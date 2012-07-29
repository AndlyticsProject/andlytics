
package com.github.andlyticsproject.exception;

public class NetworkException extends Exception {

	private static final long serialVersionUID = 1L;

	public NetworkException(Exception e) {
		super(e);
	}

	public NetworkException(Exception e, int statusCode) {
		super("Status-Code: " + statusCode, e);
	}

}
