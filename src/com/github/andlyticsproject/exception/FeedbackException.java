package com.github.andlyticsproject.exception;

public class FeedbackException extends Exception {

	private static final long serialVersionUID = 1L;

	public FeedbackException(String message) {
		super("Feedback:: " + message);
	}
	
}
