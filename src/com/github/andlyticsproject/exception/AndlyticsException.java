package com.github.andlyticsproject.exception;

// TODO -- make this a RE
// maybe come up with a better name
public class AndlyticsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4251549102914653087L;

	public AndlyticsException() {
		super();
	}

	public AndlyticsException(String message) {
		super(message);
	}

	public AndlyticsException(Throwable cause) {
		super(cause);
	}

	public AndlyticsException(String message, Throwable cause) {
		super(message, cause);
	}

}
