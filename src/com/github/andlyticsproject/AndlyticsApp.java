package com.github.andlyticsproject;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpPostSender;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

@ReportsCrashes(formKey = "dHBKcnZqTHMyMHlfLTB0RjhMejZfbkE6MQ", sharedPreferencesMode = Context.MODE_PRIVATE, sharedPreferencesName = Preferences.PREF, mode = ReportingInteractionMode.TOAST)
public class AndlyticsApp extends Application {

	private static final String TAG = AndlyticsApp.class.getSimpleName();

	private String authToken;

	private String xsrfToken;

	private ContentAdapter db;

	private boolean skipMainReload;

	private String feedbackMessage;

	private static AndlyticsApp sInstance;

	private boolean isAppVisible = false;

	@Override
	public void onCreate() {
		super.onCreate();

		initAcra();

		setDbAdapter(new ContentAdapter(this));
		sInstance = this;
	}

	private void initAcra() {
		try {
			ACRA.getConfig().setResToastText(R.string.crash_toast);
			ACRA.getConfig().setSendReportsInDevMode(false);
			ACRA.init(this);
			String bugsenseUrl = getResources().getString(R.string.bugsense_url);
			ACRA.getErrorReporter().addReportSender(new HttpPostSender(bugsenseUrl, null));
		} catch (IllegalStateException e) {
			Log.w(TAG, "ACRA.init() called more than once?: " + e.getMessage(), e);
		}
	}

	public boolean isDebug() {
		return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
	}

	public static AndlyticsApp getInstance() {
		return sInstance;
	}

	public boolean isAppVisible() {
		// TODO This is a bit of a hack, could it be improved
		return isAppVisible;
	}

	public void setIsAppVisible(boolean isVisible) {
		isAppVisible = isVisible;
	}

	public void setDbAdapter(ContentAdapter db) {
		this.db = db;
	}

	public ContentAdapter getDbAdapter() {
		return db;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setXsrfToken(String xsrfToken) {
		this.xsrfToken = xsrfToken;
	}

	public String getXsrfToken() {
		return xsrfToken;
	}

	public void setSkipMainReload(boolean skipMainReload) {
		this.skipMainReload = skipMainReload;
	}

	public boolean isSkipMainReload() {
		return skipMainReload;
	}

	public void setFeedbackMessage(String feedbackMessage) {
		this.feedbackMessage = feedbackMessage;
	}

	public String getFeedbackMessage() {
		return feedbackMessage;
	}

}
