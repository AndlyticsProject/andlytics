package com.github.andlyticsproject.console.v2;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.net.Uri;
import android.util.Log;

import com.github.andlyticsproject.console.AuthenticationException;

public class AccountManagerAuthenticator extends BaseAuthenticator {

	private static final String TAG = AccountManagerAuthenticator.class.getSimpleName();

	private static final String DEVELOPER_CONSOLE_URL = "https://play.google.com/apps/publish/v2/";
	private static final Uri ISSUE_AUTH_TOKEN_URL = Uri
			.parse("https://www.google.com/accounts/IssueAuthToken?service=gaia&Session=false");
	private static final Uri TOKEN_AUTH_URL = Uri
			.parse("https://www.google.com/accounts/TokenAuth");

	private static final boolean DEBUG = true;

	private AccountManager accountManager;

	private String authToken;
	private boolean reuseAuthentication;
	// XXX use callback so this work in the sync adapter
	private Activity activity;
	private DefaultHttpClient httpClient;

	private String cookie;
	private String devacc;
	private String xsrfToken;

	public AccountManagerAuthenticator(String accountName, AccountManager accountManager,
			boolean reuseAuthentication, Activity activity, DefaultHttpClient httpClient) {
		super(accountName);
		this.accountManager = accountManager;
		this.reuseAuthentication = reuseAuthentication;
		this.activity = activity;
		this.httpClient = httpClient;
	}

	// as described here: http://www.slideshare.net/pickerweng/chromium-os-login
	// http://www.chromium.org/chromium-os/chromiumos-design-docs/login
	// and implemented by the Android Browser: 
	// packages/apps/Browser/src/com/android/browser/GoogleAccountLogin.java
	@Override
	public AuthInfo authenticate() throws AuthenticationException {
		try {
			Account[] accounts = accountManager.getAccountsByType("com.google");
			Account account = null;
			for (Account acc : accounts) {
				if (acc.name.equals(accountName)) {
					account = acc;
					break;
				}
			}
			if (account == null) {
				throw new AuthenticationException(String.format("Account %s not found on device?"));
			}

			// XXX use callbacks
			String sid = accountManager.getAuthToken(account, "SID", null, activity, null, null)
					.getResult().getString(AccountManager.KEY_AUTHTOKEN);
			if (DEBUG) {
				Log.d(TAG, "**** SID token: " + sid);
			}
			String lsid = accountManager.getAuthToken(account, "LSID", null, activity, null, null)
					.getResult().getString(AccountManager.KEY_AUTHTOKEN);
			if (DEBUG) {
				Log.d(TAG, "***** LSID token: " + lsid);
			}

			String url = ISSUE_AUTH_TOKEN_URL.buildUpon().appendQueryParameter("SID", sid)
					.appendQueryParameter("LSID", lsid).build().toString();
			HttpPost getGaiaToken = new HttpPost(url);

			HttpResponse response = httpClient.execute(getGaiaToken);
			int status = response.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				throw new IllegalStateException("Invalid token?");
			}
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				throw new IllegalStateException("null result?");
			}

			authToken = EntityUtils.toString(entity, "UTF-8");

			final String getCookiesUrl = TOKEN_AUTH_URL.buildUpon()
					.appendQueryParameter("source", "android-browser")
					.appendQueryParameter("auth", authToken)
					.appendQueryParameter("continue", DEVELOPER_CONSOLE_URL).build().toString();
			HttpGet getConsole = new HttpGet(getCookiesUrl);
			response = httpClient.execute(getConsole);
			status = response.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				throw new IllegalStateException("Invalid token?");
			}
			entity = response.getEntity();
			if (entity == null) {
				throw new IllegalStateException("null result?");
			}

			CookieStore cookieStore = httpClient.getCookieStore();
			List<Cookie> cookies = cookieStore.getCookies();
			cookies = cookieStore.getCookies();
			String adCookie = findAdCookie(cookies);
			if (DEBUG) {
				Log.d(TAG, "AD cookie " + adCookie);
			}
			if (adCookie == null) {
				throw new AuthenticationException("Couldn't get AD cookie.");
			}

			String responseStr = EntityUtils.toString(entity, "UTF-8");
			if (DEBUG) {
				Log.d(TAG, "Response: " + responseStr);
			}
			String developerAccountId = findDeveloperAccountId(responseStr);
			if (developerAccountId == null) {
				throw new AuthenticationException("Couldn't get developer account ID.");
			}

			String xsrfToken = findXsrfToken(responseStr);
			if (xsrfToken == null) {
				throw new AuthenticationException("Couldn't get XSRF token.");
			}

			AuthInfo result = new AuthInfo(adCookie, xsrfToken, developerAccountId);
			result.addCookies(cookies);

			return result;
		} catch (IOException e) {
			// throw new NetworkException(e);
			throw new RuntimeException(e);
		} catch (OperationCanceledException e) {
			throw new AuthenticationException(e);
		} catch (AuthenticatorException e) {
			throw new AuthenticationException(e);
		}
	}
}
