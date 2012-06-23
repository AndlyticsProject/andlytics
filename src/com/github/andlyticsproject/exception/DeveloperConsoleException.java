package com.github.andlyticsproject.exception;



public class DeveloperConsoleException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public DeveloperConsoleException(String string) {
		super(string);
	}

	public DeveloperConsoleException(Exception f) {
		super(f);
	}

	public DeveloperConsoleException(String result, Exception f) {
		super(result, f);
	}

}
