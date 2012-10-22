package com.github.andlyticsproject.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.util.Log;

import com.github.andlyticsproject.exception.AuthenticationException;

public class PasswordAuthenticator extends BaseAuthenticator {

	private static final String TAG = PasswordAuthenticator.class.getSimpleName();

	private static final String LOGIN_PAGE_URL = "https://accounts.google.com/ServiceLogin?service=androiddeveloper";
	private static final String AUTHENTICATE_URL = "https://accounts.google.com/ServiceLoginAuth?service=androiddeveloper";
	private static final String DEV_CONSOLE_URL = "https://play.google.com/apps/publish/v2/";

	// this may not work for all accounts
	// "DeveloperConsoleAccounts":"[null,[[null,\"XXXXXXXXXXXXXXXXXXXX\",\"Developer Name\",1]\n]\n]"
	private static final Pattern DEV_ACC_PATTERN = Pattern
			.compile("\"DeveloperConsoleAccounts\":\"\\[null,\\[\\[null,\\\\\\\"(\\d{20})\\\\\\\",\\\\\\\"(.+?)\\\\\\\",\\d]");
	// "XsrfToken":"[null,\"AM...:1350659413000\"]\n"
	private static final Pattern XSRF_TOKEN_PATTERN = Pattern
			.compile("\"XsrfToken\":\"\\[null,\\\\\"(\\S+)\\\\\"\\]");

	private String emailAddress;
	private String password;

	public PasswordAuthenticator(String emailAddress, String password) {
		this.emailAddress = emailAddress;
		this.password = password;
	}

	/*
	 * To login, perform the following steps
	 * 
	 * 
	 * GET https://play.google.com/apps/publish/v2/?auth=AUTH_TOKEN Returns 302
	 * and has AD value in cookie
	 * 
	 * GET https://play.google.com/apps/publish/v2/ Need AD cookie for this one
	 * Returns 302 and gives dev_acc in location
	 * 
	 * GET https://play.google.com/apps/publish/v2/?dev_acc=DEV_ACC Need AD
	 * cookie for this one Entity contains XSRF Token
	 */

	@Override
	public AuthInfo authenticate() throws AuthenticationException {
		DefaultHttpClient httpClient = createHttpClient();
		try {
			HttpGet get = new HttpGet(LOGIN_PAGE_URL);
			HttpResponse response = httpClient.execute(get);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw new AuthenticationException("Auth error: " + response.getStatusLine());
			}

			String galxValue = null;
			CookieStore cookieStore = httpClient.getCookieStore();
			List<Cookie> cookies = cookieStore.getCookies();
			for (Cookie c : cookies) {
				if ("GALX".equals(c.getName())) {
					galxValue = c.getValue();
				}
			}
			Log.d(TAG, "GALX: " + galxValue);

			HttpPost post = new HttpPost(AUTHENTICATE_URL);
			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			NameValuePair email = new BasicNameValuePair("Email", emailAddress);
			pairs.add(email);
			NameValuePair passwd = new BasicNameValuePair("Passwd", password);
			pairs.add(passwd);
			NameValuePair galx = new BasicNameValuePair("GALX", galxValue);
			pairs.add(galx);
			NameValuePair cont = new BasicNameValuePair("continue", DEV_CONSOLE_URL);
			pairs.add(cont);
			UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(pairs, "UTF-8");
			post.setEntity(formEntity);

			response = httpClient.execute(post);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw new AuthenticationException("Auth error: " + response.getStatusLine());
			}

			String adCookie = null;
			String xsrfToken = null;
			String developerAccountId = null;
			cookies = cookieStore.getCookies();
			for (Cookie c : cookies) {
				if ("AD".equals(c.getName())) {
					adCookie = c.getValue();
				}
			}
			Log.d(TAG, "AD cookie " + adCookie);

			if (adCookie == null) {
				throw new AuthenticationException("Couldn't get AD cookie.");
			}

			String responseStr = EntityUtils.toString(response.getEntity());
			Log.d(TAG, "Response: " + responseStr);
			Matcher m = DEV_ACC_PATTERN.matcher(responseStr);
			if (m.find()) {
				developerAccountId = m.group(1);
			}
			if (developerAccountId == null) {
				throw new AuthenticationException("Couldn't get developer account ID.");
			}

			m = XSRF_TOKEN_PATTERN.matcher(responseStr);
			if (m.find()) {
				xsrfToken = m.group(1);
			}
			if (xsrfToken == null) {
				throw new AuthenticationException("Couldn't get XSRF token.");
			}

			AuthInfo result = new AuthInfo(adCookie, xsrfToken, developerAccountId);
			result.addCookies(cookies);

			return result;
		} catch (ClientProtocolException e) {
			throw new AuthenticationException(e);
		} catch (IOException e) {
			throw new AuthenticationException(e);
		}
	}
}
