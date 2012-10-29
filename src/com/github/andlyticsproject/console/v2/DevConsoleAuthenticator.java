package com.github.andlyticsproject.console.v2;

import android.app.Activity;

import com.github.andlyticsproject.console.AuthenticationException;

public interface DevConsoleAuthenticator {

	String getAccountName();

	// Activity may be needed to start authentication sub activity (password or
	// approval prompt, etc). It needs to override onActivityResult() and
	// retry if RESULT_OK
	SessionCredentials authenticate(Activity activity, boolean invalidate) throws AuthenticationException;

	// Use this when calling from a service. Won't launch any UIs
	SessionCredentials authenticateSilently(boolean invalidate) throws AuthenticationException;
}
