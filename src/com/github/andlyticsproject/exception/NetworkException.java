
package com.github.andlyticsproject.exception;

public class NetworkException extends AndlyticsException {

	private static final long serialVersionUID = 1L;

	public NetworkException(Throwable cause) {
		super(cause);
	}

	public NetworkException(Throwable cause, int statusCode) {
		super("Status-Code: " + statusCode, cause);
	}

}
