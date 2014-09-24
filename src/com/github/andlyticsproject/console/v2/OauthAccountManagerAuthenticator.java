package com.github.andlyticsproject.console.v2;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.github.andlyticsproject.AndlyticsApp;
import com.github.andlyticsproject.console.AuthenticationException;
import com.github.andlyticsproject.console.NetworkException;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.UserRecoverableNotifiedException;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OauthAccountManagerAuthenticator extends BaseAuthenticator {

	private static final String TAG = OauthAccountManagerAuthenticator.class.getSimpleName();

	private static final String DEVELOPER_CONSOLE_URL = "https://play.google.com/apps/publish/";

	private static final String OAUTH_LOGIN_SCOPE = "oauth2:https://www.google.com/accounts/OAuthLogin";
	private static final String OAUTH_LOGIN_URL = "https://accounts.google.com/OAuthLogin?source=ChromiumBrowser&issueuberauth=1";
	private static final String MERGE_SESSION_URL = "https://accounts.google.com/MergeSession";

	private static final int REQUEST_AUTHENTICATE = 42;

	private static final boolean DEBUG = false;

	private AccountManager accountManager;

	// includes one-time token
	private String webloginUrl;

	private DefaultHttpClient httpClient;

	public OauthAccountManagerAuthenticator(String accountName, DefaultHttpClient httpClient) {
		super(accountName);
		this.accountManager = AccountManager.get(AndlyticsApp.getInstance());
		this.httpClient = httpClient;
	}

	@Override
	public SessionCredentials authenticate(Activity activity, boolean invalidate)
			throws AuthenticationException {
		return authenticateInternal(activity, invalidate);
	}

	@Override
	public SessionCredentials authenticateSilently(boolean invalidate)
			throws AuthenticationException {
		return authenticateInternal(null, invalidate);
	}

	private SessionCredentials authenticateInternal(Activity activity, boolean invalidate)
			throws AuthenticationException {
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
				throw new AuthenticationException(String.format("Account %s not found on device?",
						accountName));
			}

			if (invalidate && webloginUrl != null) {
				// probably not needed, since what we are getting is a very
				// short-lived token
				accountManager.invalidateAuthToken(account.type, webloginUrl);
			}

			String oauthLoginToken = null;
			if (activity == null) {
				// background
				try {
					oauthLoginToken = GoogleAuthUtil.getTokenWithNotification(
							AndlyticsApp.getInstance(), accountName, OAUTH_LOGIN_SCOPE, null);
				} catch (UserRecoverableNotifiedException userNotifiedException) {
					throw new AuthenticationException(
							"Additional authentication requried, see notifications.");
				} catch (GoogleAuthException authEx) {
					// This is likely unrecoverable.
					Log.e(TAG, "Unrecoverable authentication exception: " + authEx.getMessage(),
							authEx);
					throw new AuthenticationException("Authentication error: "
							+ authEx.getMessage());
				} catch (IOException ioEx) {
					Log.w(TAG, "transient error encountered: " + ioEx.getMessage());
					throw new NetworkException(ioEx.getMessage());
				}
			} else {
				try {
					oauthLoginToken = GoogleAuthUtil.getToken(activity, accountName,
							OAUTH_LOGIN_SCOPE);
				} catch (GooglePlayServicesAvailabilityException playEx) {
					Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
							playEx.getConnectionStatusCode(), activity, REQUEST_AUTHENTICATE);
					dialog.show();

					return null;
				} catch (UserRecoverableAuthException recoverableException) {
					Intent recoveryIntent = recoverableException.getIntent();
					activity.startActivityForResult(recoveryIntent, REQUEST_AUTHENTICATE);
				} catch (GoogleAuthException authEx) {
					// This is likely unrecoverable.
					Log.e(TAG, "Unrecoverable authentication exception: " + authEx.getMessage(),
							authEx);
					throw new AuthenticationException("Authentication error: "
							+ authEx.getMessage());
				} catch (IOException ioEx) {
					Log.w(TAG, "transient error encountered: " + ioEx.getMessage());
					throw new NetworkException(ioEx.getMessage());
				}
			}

			if (oauthLoginToken == null) {
				throw new AuthenticationException(
						"Unexpected authentication error: weblogin URL = null");
			}
			if (DEBUG) {
				Log.d(TAG, "OAuth Login token: " + webloginUrl);
			}

			HttpGet getOauthUberToken = new HttpGet(OAUTH_LOGIN_URL);
			getOauthUberToken.addHeader("Authorization", "OAuth " + oauthLoginToken);
			HttpResponse response = httpClient.execute(getOauthUberToken);
			int status = response.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_UNAUTHORIZED) {
				throw new AuthenticationException("Cannot get uber token: "
						+ response.getStatusLine());
			}
			String uberToken = EntityUtils.toString(response.getEntity(), "UTF-8");
			if (DEBUG) {
				Log.d(TAG, "uber token: " + uberToken);
			}
			if (uberToken == null || "".equals(uberToken) || uberToken.contains("Error")) {
				throw new AuthenticationException("Cannot get uber token. Got: " + uberToken);
			}

			HttpPost getConsole = new HttpPost(MERGE_SESSION_URL);
			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			pairs.add(new BasicNameValuePair("uberauth", uberToken));
			pairs.add(new BasicNameValuePair("continue", DEVELOPER_CONSOLE_URL));
			pairs.add(new BasicNameValuePair("source", "ChromiumBrowser"));
			// for debugging?
			webloginUrl = Uri.parse(MERGE_SESSION_URL).buildUpon()
					.appendQueryParameter("source", "ChromiumBrowser")
					.appendQueryParameter("uberauth", uberToken)
					.appendQueryParameter("continue", DEVELOPER_CONSOLE_URL).build().toString();
			if (DEBUG) {
				Log.d(TAG, "MergeSession URL: " + webloginUrl);
			}

			UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(pairs, "UTF-8");
			getConsole.setEntity(formEntity);
			response = httpClient.execute(getConsole);
			status = response.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_UNAUTHORIZED) {
				throw new AuthenticationException("Authentication token expired: "
						+ response.getStatusLine());
			}
			if (status != HttpStatus.SC_OK) {
				throw new AuthenticationException("Authentication error: "
						+ response.getStatusLine());
			}
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				throw new AuthenticationException("Authentication error: null result?");
			}

			String responseStr = EntityUtils.toString(entity, "UTF-8");
			if (DEBUG) {
				Log.d(TAG, "Response: " + responseStr);
			}

			return createSessionCredentials(accountName, webloginUrl, responseStr,
					httpClient.getCookieStore().getCookies());
		} catch (IOException e) {
			throw new NetworkException(e);
		}
	}

}
