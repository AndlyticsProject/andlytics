package com.github.andlyticsproject.v2;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.accounts.AccountManager;
import android.util.Log;

import com.github.andlyticsproject.AndlyticsApp;
import com.github.andlyticsproject.Constants;
import com.github.andlyticsproject.exception.AuthenticationException;

public class AccountManagerAuthenticator extends BaseAuthenticator {

	private static final String TAG = AccountManagerAuthenticator.class.getSimpleName();

	private static final String URL_DEVELOPER_CONSOLE = "https://play.google.com/apps/publish/v2/";

	private AccountManager accountManager;

	private String authToken;
	private boolean reuseAuthentication;

	private String cookie;
	private String devacc;
	private String xsrfToken;

	public AccountManagerAuthenticator(String accountName, AccountManager accountManager,
			boolean reuseAuthentication) {
		super(accountName);
		this.accountManager = accountManager;
		this.reuseAuthentication = reuseAuthentication;
	}

	@Override
	public AuthInfo authenticate() throws AuthenticationException {

		/*
		 * To login, perform the following steps
		 * 
		 * 
		 * GET https://play.google.com/apps/publish/v2/?auth=AUTH_TOKEN Returns
		 * 302 and has AD value in cookie
		 * 
		 * GET https://play.google.com/apps/publish/v2/ Need AD cookie for this
		 * one Returns 302 and gives dev_acc in location
		 * 
		 * GET https://play.google.com/apps/publish/v2/?dev_acc=DEV_ACC Need AD
		 * cookie for this one Entity contains XSRF Token
		 */

		DefaultHttpClient httpclient = null;

		try {
			if (!reuseAuthentication) {
				cookie = null;
			}

			// reuse cookie for performance
			if (cookie != null) {
				// XXX
				return new AuthInfo(cookie, xsrfToken, devacc);
			}

			boolean asp = false;

			// Variables that we need to collect
			String cookieAD = null;
			String xsrfToken = null;
			String devacc = null;

			// Setup parameters etc..
			// TODO do we need all these parameters/are they needed for all
			// requests
			httpclient = HttpClientFactory.createDevConsoleHttpClient(DeveloperConsoleV2.TIMEOUT);

			HttpContext httpContext = new BasicHttpContext();
			RedirectHandler rd = httpclient.getRedirectHandler();
			Log.d(TAG, "redirect handler: " + rd);
			// httpclient.setRedirectHandler(new RedirectHandler() {
			//
			// @Override
			// public URI getLocationURI(HttpResponse response, HttpContext
			// ctx)
			// throws org.apache.http.ProtocolException {
			// return null;
			// }
			//
			// @Override
			// public boolean isRedirectRequested(HttpResponse response,
			// HttpContext ctx) {
			// return false;
			// }
			// });

			/*
			 * Get AD cookie
			 */
			// GET https://play.google.com/apps/publish/v2/?auth=AUTH_TOKEN
			HttpGet httpGet = new HttpGet(URL_DEVELOPER_CONSOLE + "?auth=" + authToken);

			HttpResponse httpResponse = httpclient.execute(httpGet, httpContext);

			// FIXME returns 200 along with what I think is a request to
			// re-authenticate, rather than 302 and the AD cookie

			Log.d(TAG, "Headers: ");
			Header[] headers = httpResponse.getAllHeaders();
			for (Header h : headers) {
				Log.d(TAG, h.toString());
			}

			AccountManager accountManager = AccountManager.get(AndlyticsApp.getInstance());
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			// if (statusCode != HttpStatus.SC_MOVED_TEMPORARILY) {
			if (statusCode != HttpStatus.SC_OK) {
				accountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE_GOOGLE, authToken);
				throw new AuthenticationException("Got HTTP " + statusCode + " ("
						+ httpResponse.getStatusLine().getReasonPhrase() + ')');
			}

			// Get AD cookie from headers
			Header[] allHeaders = httpResponse.getHeaders("Set-Cookie");
			if (allHeaders != null && allHeaders.length > 0) {
				if (allHeaders[0].getValue().startsWith("AD")) {
					cookieAD = allHeaders[0].getValue();
				}
			}

			if (cookieAD == null) {
				Log.e(TAG, "Missing cookie AD");
				throw new AuthenticationException();
			}

			/*
			 * Get DEV_ACC variable
			 */
			// GET https://play.google.com/apps/publish/v2/
			httpGet = new HttpGet(URL_DEVELOPER_CONSOLE);
			httpGet.addHeader("Cookie", cookieAD);

			httpResponse = httpclient.execute(httpGet, httpContext);

			statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_MOVED_TEMPORARILY) {
				throw new AuthenticationException("Got HTTP " + statusCode + " ("
						+ httpResponse.getStatusLine().getReasonPhrase() + ')');
			}

			// Get DEV_ACC from the location
			Object obj = httpContext.getAttribute("http.protocol.redirect-locations");
			if (obj != null) {
				RedirectLocations locs = (RedirectLocations) obj;

				try {
					Field privateStringField = RedirectLocations.class.getDeclaredField("uris");
					privateStringField.setAccessible(true);
					// TODO Cast this properly
					HashSet<URI> uris = (HashSet<URI>) privateStringField.get(locs);

					for (URI uri : uris) {
						String string = uri.toASCIIString();
						if (string.indexOf("dev_acc=") > -1) {
							devacc = string.substring(string.indexOf("=") + 1, string.length());
							break;
						} else if (string.indexOf("asp=1") > -1) {
							asp = true;
						}

					}

				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}

			}

			if (devacc == null && asp) {
				// TODO Support these accounts
				Log.e(TAG, "Multi linked account");
				// throw new MultiAccountAcception();
				// TODO clean this up
				throw new AuthenticationException("Multiple accounts not supported");
			}

			if (devacc == null) {
				Log.e(TAG, "Missing devacc");
				throw new AuthenticationException();
			}

			/*
			 * Get XSRF_TOKEN from entity
			 */
			// GET https://play.google.com/apps/publish/v2/?dev_acc=DEV_ACC
			httpGet = new HttpGet(URL_DEVELOPER_CONSOLE + "?dev_acc=" + devacc);
			httpGet.addHeader("Cookie", cookieAD);

			httpResponse = httpclient.execute(httpGet, httpContext);

			statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				throw new AuthenticationException("Got HTTP " + statusCode + " ("
						+ httpResponse.getStatusLine().getReasonPhrase() + ')');
			}

			String entity = EntityUtils.toString(httpResponse.getEntity());
			Matcher m1 = Pattern.compile("\"XsrfToken\".+\"").matcher(entity);
			if (m1.find()) {
				xsrfToken = m1.group(0);
				xsrfToken = xsrfToken.substring(20, xsrfToken.length() - 1);
			}

			if (xsrfToken != null) {
				// Fill in the details for use later on
				this.xsrfToken = xsrfToken;
				this.cookie = cookieAD;
				this.devacc = devacc;

				return new AuthInfo(cookie, xsrfToken, devacc);
			}

			Log.e(TAG, "Missing xsrfToken");
			throw new AuthenticationException("Couldn't get cookie.");
		} catch (SocketException e) {
			// throw new NetworkException(e);
			throw new RuntimeException(e);
		} catch (UnknownHostException e) {
			// throw new NetworkException(e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			// throw new NetworkException(e);
			throw new RuntimeException(e);
		} finally {
			if (httpclient != null) {
				httpclient.getConnectionManager().shutdown();
				httpclient = null;
			}
		}
	}

}
