package com.github.andlyticsproject.console.v2;

import com.github.andlyticsproject.console.AuthenticationException;

public interface DevConsoleAuthenticator {

	String getAccountName();

	// XXX uglish
	// Get rid of the whole exception package thing
	AuthInfo authenticate() throws AuthenticationException;
}
