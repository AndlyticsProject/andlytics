package com.github.andlyticsproject.console;

public class NetworkException extends DevConsoleException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4549798608972299810L;

	public NetworkException(String message) {
		super(message);
	}

	public NetworkException(Throwable cause) {
		super(cause);
	}

	public NetworkException(String message, Throwable cause) {
		super(message, cause);
	}

	public NetworkException(Throwable cause, int statusCode) {
		super("Status-Code: " + statusCode, cause);
	}

}
