package com.github.andlyticsproject.console.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.cookie.Cookie;

public class SessionCredentials {

	private String accountName;
	private String xsrfToken;
	// 20 digit developer account ID
	private String[] developerAccountIds;
	// authentication session cookies, including AD
	private List<Cookie> cookies = new ArrayList<Cookie>();

	public SessionCredentials(String accountName, String xsrfToken, String[] developerAccountIds) {
		this.accountName = accountName;
		this.xsrfToken = xsrfToken;
		this.developerAccountIds = developerAccountIds.clone();
	}

	public String getAccountName() {
		return accountName;
	}

	public String getXsrfToken() {
		return xsrfToken;
	}

	public String[] getDeveloperAccountIds() {
		return developerAccountIds.clone();
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
