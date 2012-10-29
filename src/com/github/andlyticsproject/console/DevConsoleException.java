package com.github.andlyticsproject.console;

public class DevConsoleException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4251549102914653087L;

	public DevConsoleException() {
		super();
	}

	public DevConsoleException(String message) {
		super(message);
	}

	public DevConsoleException(Throwable cause) {
		super(cause);
	}

	public DevConsoleException(String message, Throwable cause) {
		super(message, cause);
	}

}
