
package com.github.andlyticsproject.exception;

public class InvalidJSONResponseException extends AndlyticsException {

	private static final long serialVersionUID = 1L;

	public InvalidJSONResponseException(String message, String json) {
		super(message + ",json:" + json);
	}

}
