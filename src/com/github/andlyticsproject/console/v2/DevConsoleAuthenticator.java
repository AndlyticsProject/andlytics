package com.github.andlyticsproject.console.v2;

import com.github.andlyticsproject.console.AuthenticationException;

public interface DevConsoleAuthenticator {

	String getAccountName();

	// XXX uglish
	AuthInfo authenticate() throws AuthenticationException;
}
