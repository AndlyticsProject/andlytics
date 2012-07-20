package com.github.andlyticsproject;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Context;

@ReportsCrashes(
		formKey = "dHBKcnZqTHMyMHlfLTB0RjhMejZfbkE6MQ",
		sharedPreferencesMode=Context.MODE_PRIVATE,
		sharedPreferencesName=Preferences.PREF)
public class AndlyticsApp extends Application {

	private String authToken;

	private String xsrfToken;

	private ContentAdapter db;

	private boolean skipMainReload;

	private String feedbackMessage;
	
	private static AndlyticsApp sInstance;

	@Override
	public void onCreate() {
		//ACRA.init(this);
		super.onCreate();
		Preferences.disableCrashReports(this);
		setDbAdapter(new ContentAdapter(this));
		sInstance = this;
	}
	
	public static AndlyticsApp getInstance(){
		return sInstance;
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
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
