package com.github.andlyticsproject;


// XXX rename once legacy ChartActivity is removed?
public interface DetailedStatsActivity {

	public void handleUserVisibleException(Exception e);

	public boolean isRefreshing();

	public void refreshStarted();

	public void refreshFinished();

	public boolean shouldRemoteUpdateStats();

	// XXX do NOT name this `getPackageName()`, will override 
	// core activity method and crash the ActivityManager.
	public String getPackage();

	public String getDeveloperId();

	public String getAccountName();
}