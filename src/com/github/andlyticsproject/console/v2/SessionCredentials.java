package com.github.andlyticsproject.console.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.cookie.Cookie;

public class SessionCredentials {

	private String xsrfToken;
	// 20 digit developer account ID
	private String developerAccountId;
	// authentication session cookies, including AD
	private List<Cookie> cookies = new ArrayList<Cookie>();

	public SessionCredentials(String xsrfToken, String developerAccountId) {
		this.xsrfToken = xsrfToken;
		this.developerAccountId = developerAccountId;
	}

	public String getXsrfToken() {
		return xsrfToken;
	}

	public String getDeveloperAccountId() {
		return developerAccountId;
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
