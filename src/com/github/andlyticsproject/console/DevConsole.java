package com.github.andlyticsproject.console;

import java.util.List;

import android.app.Activity;

import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.Comment;

public interface DevConsole {

	// activity is needed for starting an authentication sub-activity
	// which may be returned from the AccountManager (leaky...)
	// pass null when calling from a service
	List<AppInfo> getAppInfo(Activity activity) throws DevConsoleException;

	List<Comment> getComments(Activity activity, String packageName, String developerId,
			int startIndex, int count) throws DevConsoleException;

}
