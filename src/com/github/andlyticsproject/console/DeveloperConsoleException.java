package com.github.andlyticsproject.console;

public class DeveloperConsoleException extends AndlyticsException {

	private static final long serialVersionUID = 1L;

	private String postData;
	private String consoleResponse;

	public DeveloperConsoleException(String message) {
		super(message);
	}

	public DeveloperConsoleException(String message, String postData, String consoleResponse) {
		super(message);
		this.postData = postData;
		this.consoleResponse = consoleResponse;
	}

	public DeveloperConsoleException(Throwable cause) {
		super(cause);
	}

	public DeveloperConsoleException(String message, Throwable cause) {
		super(message, cause);
	}

	public String getPostData() {
		return postData;
	}

	public String getConsoleResponse() {
		return consoleResponse;
	}

}
