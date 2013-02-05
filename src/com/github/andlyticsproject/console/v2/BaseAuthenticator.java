package com.github.andlyticsproject.console.v2;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.cookie.Cookie;

public abstract class BaseAuthenticator implements DevConsoleAuthenticator {

	protected static final Pattern DEV_ACC_PATTERN = Pattern
			.compile("\"DeveloperConsoleAccounts\":\"\\{\\\\\"1\\\\\":\\[\\{\\\\\"1\\\\\":\\\\\"(\\d{20})\\\\\"");
	protected static final Pattern XSRF_TOKEN_PATTERN = Pattern
			.compile("\"XsrfToken\":\"\\{\\\\\"1\\\\\":\\\\\"(\\S+)\\\\\"\\}\"");

	protected String accountName;

	protected BaseAuthenticator(String accountName) {
		this.accountName = accountName;
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

	public String getAccountName() {
		return accountName;
	}

}
