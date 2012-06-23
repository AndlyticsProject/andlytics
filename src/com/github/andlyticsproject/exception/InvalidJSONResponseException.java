package com.github.andlyticsproject.exception;

public class InvalidJSONResponseException extends Exception {

	private static final long serialVersionUID = 1L;

	public InvalidJSONResponseException(String string, String json) {
		super(string + ",json:" + json);
	}

}
