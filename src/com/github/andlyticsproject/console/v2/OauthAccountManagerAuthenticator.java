package com.github.andlyticsproject.console.v2;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import com.github.andlyticsproject.AndlyticsApp;
import com.github.andlyticsproject.R;
import com.github.andlyticsproject.console.AuthenticationException;
import com.github.andlyticsproject.console.NetworkException;
import com.github.andlyticsproject.model.DeveloperConsoleAccount;

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

	@SuppressWarnings("deprecation")
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

			Bundle authResult = accountManager.getAuthToken(account, OAUTH_LOGIN_SCOPE, false,
					null, null).getResult();
			if (authResult.containsKey(AccountManager.KEY_INTENT)) {
				Intent authIntent = authResult.getParcelable(AccountManager.KEY_INTENT);
				if (DEBUG) {
					Log.w(TAG, "Got a reauthenticate intent: " + authIntent);
				}

				// silent mode, show notification
				if (activity == null) {
					Context ctx = AndlyticsApp.getInstance();
					Builder builder = new NotificationCompat.Builder(ctx);
					builder.setSmallIcon(R.drawable.statusbar_andlytics);
					builder.setContentTitle(ctx.getResources().getString(R.string.auth_error,
							accountName));
					builder.setContentText(ctx.getResources().getString(R.string.auth_error,
							accountName));
					builder.setAutoCancel(true);
					PendingIntent contentIntent = PendingIntent.getActivity(ctx,
							accountName.hashCode(), authIntent, PendingIntent.FLAG_UPDATE_CURRENT);
					builder.setContentIntent(contentIntent);

					NotificationManager nm = (NotificationManager) ctx
							.getSystemService(Context.NOTIFICATION_SERVICE);
					nm.notify(accountName.hashCode(), builder.build());

					return null;
				}

				// activity mode, start activity
				authIntent.setFlags(authIntent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
				activity.startActivityForResult(authIntent, REQUEST_AUTHENTICATE);

				return null;
			}

			String oauthLoginToken = authResult.getString(AccountManager.KEY_AUTHTOKEN);
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
			Log.d(TAG, "uber token: " + uberToken);
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
			Log.d(TAG, "MergeSession URL: " + webloginUrl);

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

			DeveloperConsoleAccount[] developerAccounts = findDeveloperAccounts(responseStr);
			if (developerAccounts == null) {
				debugAuthFailure(activity, responseStr, webloginUrl);

				throw new AuthenticationException("Couldn't get developer account ID.");
			}

			String xsrfToken = findXsrfToken(responseStr);
			if (xsrfToken == null) {
				debugAuthFailure(activity, responseStr, webloginUrl);

				throw new AuthenticationException("Couldn't get XSRF token.");
			}

			List<String> whitelistedFeatures = findWhitelistedFeatures(responseStr);

			SessionCredentials result = new SessionCredentials(accountName, xsrfToken,
					developerAccounts);
			result.addCookies(httpClient.getCookieStore().getCookies());
			result.addWhitelistedFeatures(whitelistedFeatures);

			return result;
		} catch (IOException e) {
			throw new NetworkException(e);
		} catch (OperationCanceledException e) {
			throw new AuthenticationException(e);
		} catch (AuthenticatorException e) {
			throw new AuthenticationException(e);
		}
	}

}
