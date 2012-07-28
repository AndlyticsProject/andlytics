
package com.github.andlyticsproject;

import java.util.List;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.AsyncTasks.LoadAppListTask;
import com.github.andlyticsproject.AsyncTasks.LoadAppListTaskCompleteListener;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.sync.AutosyncHandler;
import com.github.andlyticsproject.sync.AutosyncHandlerFactory;

public class AccountSpecificPreferenceActivity extends SherlockPreferenceActivity implements
		LoadAppListTaskCompleteListener {

	private String accountName;
	private Preference dummyApp;
	private PreferenceCategory notificationAppList;
	private PreferenceCategory hiddenAppList;
	private LoadAppListTask task;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		accountName = getIntent().getExtras().getString(Constants.AUTH_ACCOUNT_NAME);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setTitle(accountName);
		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(Preferences.PREF);
		addPreferencesFromResource(R.xml.account_specific_preferences);

		PreferenceCategory autoSyncCat = (PreferenceCategory) getPreferenceScreen().findPreference(
				"prefCatAutoSync");
		CheckBoxPreference autoSync = new CheckBoxPreference(this);
		autoSync.setKey(Preferences.AUTOSYNC_ENABLE + accountName);
		autoSync.setDefaultValue(true);
		autoSync.setTitle(R.string.auto_sync);
		autoSync.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				AutosyncHandler autosyncHandler = AutosyncHandlerFactory
						.getInstance(AccountSpecificPreferenceActivity.this);
				// Toogle auto sync between the sync period and 0
				autosyncHandler.setAutosyncPeriod(accountName, ((Boolean) newValue) ? 
						Preferences.getAutoSyncPeriod(AccountSpecificPreferenceActivity.this, 
								accountName) : 0);
				return true;
			}
		});
		autoSyncCat.addPreference(autoSync);

		dummyApp = new Preference(this);
		dummyApp.setTitle(R.string.loading_app_list);

		// Notifications list
		notificationAppList = (PreferenceCategory) getPreferenceScreen().findPreference(
				"prefCatNotificationApps");
		// Set Enabled state
		boolean commentsEnabled = Preferences.getNotificationPerf(this,
				Preferences.NOTIFICATION_CHANGES_COMMENTS);
		boolean ratingsEnabled = Preferences.getNotificationPerf(this,
				Preferences.NOTIFICATION_CHANGES_RATING);
		boolean downloadsEnabled = Preferences.getNotificationPerf(this,
				Preferences.NOTIFICATION_CHANGES_DOWNLOADS);
		Boolean notificationsEnabled = commentsEnabled || ratingsEnabled || downloadsEnabled;
		notificationAppList.addPreference(dummyApp);
		notificationAppList.setEnabled(notificationsEnabled);

		// Hidden apps list
		hiddenAppList = (PreferenceCategory) getPreferenceScreen().findPreference(
				"prefCatHiddenApps");
		hiddenAppList.addPreference(dummyApp);

		// Load apps
		task = (LoadAppListTask) getLastNonConfigurationInstance();
		if (task == null) {
			task = new LoadAppListTask(this);
			task.execute(accountName);
		} else {
			task.attach(this);
			List<AppInfo> apps = task.getResult();
			if (apps != null) {
				onLoadAppListTaskComplete(apps);
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (task != null) {
			task.detach();
		}
		return (task);
	}

	@Override
	public void onLoadAppListTaskComplete(List<AppInfo> apps) {
		task.detach();
		task = null;
		if (apps != null && apps.size() > 0) {
			notificationAppList.removePreference(dummyApp);
			hiddenAppList.removePreference(dummyApp);
			for (AppInfo app : apps) {
				CheckBoxPreference pref = new CheckBoxPreference(this);
				pref.setTitle(app.getName());
				pref.setSummary(app.getPackageName());
				pref.setChecked(!app.isSkipNotification());
				pref.setOnPreferenceChangeListener(notificationAppPrefChangedListener);
				notificationAppList.addPreference(pref);

				pref = new CheckBoxPreference(this);
				pref.setTitle(app.getName());
				pref.setSummary(app.getPackageName());
				pref.setChecked(app.isGhost());
				pref.setOnPreferenceChangeListener(hiddenAppPrefChangedListener);
				hiddenAppList.addPreference(pref);
			}
		} else {
			dummyApp.setTitle(R.string.no_published_apps);
		}
	}

	OnPreferenceChangeListener notificationAppPrefChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			AndlyticsApp.getInstance().getDbAdapter()
					.setSkipNotification((String) preference.getSummary(), !(Boolean) newValue);
			return true;
		}
	};

	OnPreferenceChangeListener hiddenAppPrefChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			AndlyticsApp.getInstance().getDbAdapter()
					.setGhost(accountName, (String) preference.getSummary(), (Boolean) newValue);
			return true;
		}
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}
