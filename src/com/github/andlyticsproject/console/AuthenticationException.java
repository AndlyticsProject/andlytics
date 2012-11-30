package com.github.andlyticsproject.console;

public class AuthenticationException extends DevConsoleException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5409862768941335087L;

	public AuthenticationException() {
		super();
	}

	public AuthenticationException(String message) {
		super(message);
	}

	public AuthenticationException(Throwable cause) {
		super(cause);
	}

	public AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

}
