
package com.github.andlyticsproject.console;

public class NetworkException extends DevConsoleException {

	private static final long serialVersionUID = 1L;

	public NetworkException(Throwable cause) {
		super(cause);
	}

	public NetworkException(Throwable cause, int statusCode) {
		super("Status-Code: " + statusCode, cause);
	}

}
