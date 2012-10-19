package com.github.andlyticsproject.v2;

public class AuthInfo {

	// AD session cookie
	private String adCookie;
	private String xsrfToken;
	// 20 digit developer account ID
	private String developerAccountId;

	public AuthInfo(String adCookie, String xsrfToken, String developerAccountId) {
		this.adCookie = adCookie;
		this.xsrfToken = xsrfToken;
		this.developerAccountId = developerAccountId;
	}

	public String getAdCookie() {
		return adCookie;
	}

	public String getXsrfToken() {
		return xsrfToken;
	}

	public String getDeveloperAccountId() {
		return developerAccountId;
	}

}
