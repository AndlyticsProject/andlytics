package com.github.andlyticsproject.console.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.cookie.Cookie;

import com.github.andlyticsproject.model.DeveloperConsoleAccount;

public abstract class BaseAuthenticator implements DevConsoleAuthenticator {

	protected static final Pattern DEV_ACC_PATTERN = Pattern
			.compile("\"DeveloperConsoleAccounts\":\"\\{\\\\\"1\\\\\":\\[\\{\\\\\"1\\\\\":\\\\\"(\\d{20})\\\\\"");
	protected static final Pattern DEV_ACCS_PATTERN = Pattern
			.compile("\\\\\"1\\\\\":\\\\\"(\\d{20})\\\\\",\\\\\"2\\\\\":\\\\\"(.+?)\\\\\",");
	protected static final Pattern XSRF_TOKEN_PATTERN = Pattern
			.compile("\"XsrfToken\":\"\\{\\\\\"1\\\\\":\\\\\"(\\S+)\\\\\"\\}\"");

	protected static final Pattern WHITELISTED_FEATURES_PATTERN = Pattern
			.compile("\"WhitelistedFeatures\":\"\\{\\\\\"1\\\\\":\\[(\\S+?)\\]\\}");

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

	protected DeveloperConsoleAccount[] findDeveloperAccounts(String responseStr) {
		List<DeveloperConsoleAccount> devAccounts = new ArrayList<DeveloperConsoleAccount>();
		Matcher m = DEV_ACCS_PATTERN.matcher(responseStr);
		while (m.find()) {
			String developerId = m.group(1);
			String developerName = m.group(2);
			if (developerName.contains("\\\\u")) {
				developerName = developerName.replace("\\\\u", "\\u");
				developerName = StringEscapeUtils.unescapeJava(developerName);
			} else if (developerName.contains("\\u")) {
				developerName = StringEscapeUtils.unescapeJava(developerName);
			}
			devAccounts.add(new DeveloperConsoleAccount(developerId, developerName));
		}
		return devAccounts.isEmpty() ? null : devAccounts
				.toArray(new DeveloperConsoleAccount[devAccounts.size()]);
	}

	protected List<String> findWhitelistedFeatures(String responseStr) {
		List<String> result = new ArrayList<String>();
		Matcher m = WHITELISTED_FEATURES_PATTERN.matcher(responseStr);
		if (m.find()) {
			String featuresStr = m.group(1);
			String[] features = featuresStr.split(",");
			for (String feature : features) {
				result.add(feature.replaceAll("\\\\\"", ""));
			}
		}

		return Collections.unmodifiableList(result);
	}

	public String getAccountName() {
		return accountName;
	}

}
