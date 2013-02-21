package com.github.andlyticsproject.console.v2;

import org.apache.http.cookie.Cookie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SessionCredentials {

	private String xsrfToken;
	// 20 digit developer account ID
	private String[] developerAccountIds;
	// authentication session cookies, including AD
	private List<Cookie> cookies = new ArrayList<Cookie>();

	public SessionCredentials(String xsrfToken, String[] developerAccountIds) {
		this.xsrfToken = xsrfToken;
		this.developerAccountIds = developerAccountIds;
	}

	public String getXsrfToken() {
		return xsrfToken;
	}

	public String[] getDeveloperAccountIds() {
		return developerAccountIds;
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
