package com.github.andlyticsproject.io;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class AndlyticsBackupAgent extends BackupAgentHelper {

	private static final String PREFS = "andlytics_pref";
	private static final String PREFS_BACKUP_KEY = "prefs";
	private static final String STATS_BACKUP_KEY = "stats";

	@Override
	public void onCreate() {
		SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, PREFS);
		addHelper(PREFS_BACKUP_KEY, helper);
		addHelper(STATS_BACKUP_KEY, new StatsDbBackupHelper(this));
	}

}
