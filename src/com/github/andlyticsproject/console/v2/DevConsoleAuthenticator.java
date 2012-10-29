package com.github.andlyticsproject.console.v2;

import android.app.Activity;

import com.github.andlyticsproject.console.DevConsoleException;

public interface DevConsoleAuthenticator {

	String getAccountName();

	// Activity may be needed to start authentication sub activity (password or
	// approval prompt, etc). It needs to override onActivityResult() and
	// retry if RESULT_OK
	SessionCredentials authenticate(Activity activity, boolean invalidate)
			throws DevConsoleException;

	// Use this when calling from a service. Won't launch any UIs
	SessionCredentials authenticateSilently(boolean invalidate) throws DevConsoleException;
}
