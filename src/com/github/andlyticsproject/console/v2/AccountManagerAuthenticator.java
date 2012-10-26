package com.github.andlyticsproject.console.v2;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.github.andlyticsproject.AndlyticsApp;
import com.github.andlyticsproject.console.AuthenticationException;

public class AccountManagerAuthenticator extends BaseAuthenticator {

	private static final String TAG = AccountManagerAuthenticator.class.getSimpleName();

	private static final String DEVELOPER_CONSOLE_URL = "https://play.google.com/apps/publish/v2/";

	private static final int REQUEST_AUTHENTICATE = 42;

	private static final boolean DEBUG = false;

	private AccountManager accountManager;

	// includes one-time token
	private String webloginUrl;
	private boolean reuseAuthentication;
	private DefaultHttpClient httpClient;

	public AccountManagerAuthenticator(String accountName, boolean reuseAuthentication,
			DefaultHttpClient httpClient) {
		super(accountName);
		this.accountManager = AccountManager.get(AndlyticsApp.getInstance());
		this.reuseAuthentication = reuseAuthentication;
		this.httpClient = httpClient;
	}

	// as described here: http://www.slideshare.net/pickerweng/chromium-os-login
	// http://www.chromium.org/chromium-os/chromiumos-design-docs/login
	// and implemented by the Android Browser:
	// packages/apps/Browser/src/com/android/browser/GoogleAccountLogin.java
	// packages/apps/Browser/src/com/android/browser/DeviceAccountLogin.java
	@Override
	public AuthInfo authenticate(Activity activity, boolean invalidate)
			throws AuthenticationException {
		return authenticateInternal(activity, invalidate);
	}

	@Override
	public AuthInfo authenticateSilently(boolean invalidate) throws AuthenticationException {
		return authenticateInternal(null, invalidate);
	}

	@SuppressWarnings("deprecation")
	private AuthInfo authenticateInternal(Activity activity, boolean invalidate)
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
				throw new AuthenticationException(String.format("Account %s not found on device?"));
			}

			if (invalidate && webloginUrl != null) {
				// probably not needed, since what we are getting is a very
				// short-lived token
				accountManager.invalidateAuthToken(account.type, webloginUrl);
			}

			Bundle authResult = accountManager.getAuthToken(account,
					"weblogin:service=androiddeveloper&continue=" + DEVELOPER_CONSOLE_URL, false,
					null, null).getResult();
			if (authResult.containsKey(AccountManager.KEY_INTENT)) {
				Intent authIntent = authResult.getParcelable(AccountManager.KEY_INTENT);
				if (DEBUG) Log.w(TAG, "Got a reauthenticate intent: " + authIntent);

				// silent mode
				if (activity == null) {
					// TODO the right way is to create a notification and
					// handle the LOGIN_ACCOUNTS_CHANGED_ACTION if it ever
					// gets clicked. Just fail for now
					throw new AuthenticationException(
							"Silent authentication failed. User input required");
				}

				authIntent.setFlags(authIntent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
				activity.startActivityForResult(authIntent, REQUEST_AUTHENTICATE);
				return null;
			}
			webloginUrl = authResult.getString(AccountManager.KEY_AUTHTOKEN);
			if (webloginUrl == null) {
				throw new AuthenticationException(
						"Unexpected authentication error: weblogin URL = null");
			}
			if (DEBUG) Log.d(TAG, "Weblogin URL: " + webloginUrl);

			HttpGet getConsole = new HttpGet(webloginUrl);
			HttpResponse response = httpClient.execute(getConsole);
			int status = response.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				throw new IllegalStateException("Authentication error: " + response.getStatusLine());
			}
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				throw new AuthenticationException("Authentication error: null result?");
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

			AuthInfo result = new AuthInfo(xsrfToken, developerAccountId);
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
