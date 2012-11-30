package com.github.andlyticsproject.console;

public class DevConsoleProtocolException extends DevConsoleException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6909987334134805822L;

	private String postData;
	private String consoleResponse;

	public DevConsoleProtocolException(String message) {
		super(message);
	}

	public DevConsoleProtocolException(String message, String postData, String consoleResponse) {
		super(message);
		this.postData = postData;
		this.consoleResponse = consoleResponse;
	}

	public DevConsoleProtocolException(Throwable cause) {
		super(cause);
	}

	public DevConsoleProtocolException(String message, Throwable cause) {
		super(message, cause);
	}

	public String getPostData() {
		return postData;
	}

	public String getConsoleResponse() {
		return consoleResponse;
	}

}
