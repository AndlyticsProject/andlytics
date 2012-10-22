package com.github.andlyticsproject.v2;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

public abstract class BaseAuthenticator implements DevConsoleAuthenticator {

	// 30 seconds -- for both socket and connection 
	private static final int TIMEOUT = 30 * 1000;
	protected static final Pattern DEV_ACC_PATTERN = Pattern
				.compile("\"DeveloperConsoleAccounts\":\"\\[null,\\[\\[null,\\\\\\\"(\\d{20})\\\\\\\",\\\\\\\"(.+?)\\\\\\\",\\d]");
	protected static final Pattern XSRF_TOKEN_PATTERN = Pattern
				.compile("\"XsrfToken\":\"\\[null,\\\\\"(\\S+)\\\\\"\\]");

	protected DefaultHttpClient createHttpClient() {
		return HttpClientFactory.createDevConsoleHttpClient(TIMEOUT);
	}

	protected String findAdCookie(List<Cookie> cookies) {
		for (Cookie c : cookies) {
			if ("AD".equals(c.getName())) {
				return c.getValue();
			}
		}
		return null;
	}

	protected String findXsrfToken(String responseStr) {
		Matcher m = XSRF_TOKEN_PATTERN.matcher(responseStr);
		if (m.find()) {
			return m.group(1);
		}
		return null;
	}

	protected String findDeveloperAccountId(String responseStr) {
		Matcher m = DEV_ACC_PATTERN.matcher(responseStr);
		if (m.find()) {
			return m.group(1);
		}
	
		return null;
	}
}
