package com.github.andlyticsproject.console.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.cookie.Cookie;

import com.github.andlyticsproject.model.DeveloperConsoleAccount;

public class SessionCredentials {

	private String accountName;
	private String xsrfToken;
	// 20 digit developer account ID
	private DeveloperConsoleAccount[] consoleAccounts;
	// authentication session cookies, including AD
	private List<Cookie> cookies = new ArrayList<Cookie>();

	public SessionCredentials(String accountName, String xsrfToken,
			DeveloperConsoleAccount[] consoleAccounts) {
		this.accountName = accountName;
		this.xsrfToken = xsrfToken;
		this.consoleAccounts = consoleAccounts.clone();
	}

	public String getAccountName() {
		return accountName;
	}

	public String getXsrfToken() {
		return xsrfToken;
	}

	public DeveloperConsoleAccount[] getDeveloperConsoleAccounts() {
		return consoleAccounts.clone();
	}

	public void addCookie(Cookie c) {
		cookies.add(c);
	}

	public void addCookies(List<Cookie> c) {
		cookies.addAll(c);
	}

	public List<Cookie> getCookies() {
		return Collections.unmodifiableList(cookies);
	}

}
