package com.github.andlyticsproject;

import java.util.List;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.AsyncTasks.LoadAppListTask;
import com.github.andlyticsproject.AsyncTasks.LoadAppListTaskCompleteListener;
import com.github.andlyticsproject.model.AppInfo;

// See PreferenceActivity for warning suppression justification
@SuppressWarnings("deprecation")
public class NotificationPreferenceActivity extends SherlockPreferenceActivity implements LoadAppListTaskCompleteListener {

	private String mAccountName;
	private Preference mDummyAppPreference;
	private LoadAppListTask mTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAccountName = getIntent().getExtras().getString(Constants.AUTH_ACCOUNT_NAME);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(Preferences.PREF);
		addPreferencesFromResource(R.xml.notification_preferences);

		// Have to build these up dynamically as the key contains the account name		
		// Notification trigger
		PreferenceCategory notificationTrigger = (PreferenceCategory) getPreferenceScreen().findPreference("prefCatNotificationTrigger");	
		CheckBoxPreference ratings = new CheckBoxPreference(this);
		ratings.setKey(Preferences.NOTIFICATION_CHANGES_RATING + mAccountName);
		ratings.setTitle(R.string.rating_changes);
		ratings.setDefaultValue(true);
		notificationTrigger.addPreference(ratings);

		CheckBoxPreference comments = new CheckBoxPreference(this);
		comments.setKey(Preferences.NOTIFICATION_CHANGES_COMMENTS + mAccountName);
		comments.setTitle(R.string.comment_changes);
		comments.setDefaultValue(true);
		notificationTrigger.addPreference(comments);


		CheckBoxPreference downloads = new CheckBoxPreference(this);
		downloads.setKey(Preferences.NOTIFICATION_CHANGES_DOWNLOADS + mAccountName);
		downloads.setTitle(R.string.download_changes);
		downloads.setDefaultValue(true);
		notificationTrigger.addPreference(downloads);

		// Notification signal
		PreferenceCategory notificationSignal = (PreferenceCategory) getPreferenceScreen().findPreference("prefCatNotificationSignal");	
		CheckBoxPreference sound = new CheckBoxPreference(this);
		sound.setKey(Preferences.NOTIFICATION_SOUND + mAccountName);
		sound.setTitle(R.string.notification_sound);
		sound.setDefaultValue(true);
		notificationSignal.addPreference(sound);

		CheckBoxPreference light = new CheckBoxPreference(this);
		light.setKey(Preferences.NOTIFICATION_LIGHT + mAccountName);
		light.setTitle(R.string.notification_light);
		light.setDefaultValue(true);
		notificationSignal.addPreference(light);


		// App list
		PreferenceCategory appList = (PreferenceCategory) getPreferenceScreen().findPreference("prefCatNotificationApps");
		// Create a dummy preference while we load the app list as it can be blocked by other db opperations
		mDummyAppPreference = new Preference(this);
		mDummyAppPreference.setTitle(R.string.loading_app_list);
		appList.addPreference(mDummyAppPreference);

		mTask = (LoadAppListTask) getLastNonConfigurationInstance();
		if (mTask == null){
			mTask = new LoadAppListTask(this);
			mTask.execute(mAccountName);
		} else {
			mTask.attach(this);
			List<AppInfo> apps = mTask.getResult();
			if (apps != null){
				onLoadAppListTaskComplete(apps);
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mTask != null){
			mTask.detach();
		}
		return(mTask);
	}

	@Override
	public void onLoadAppListTaskComplete(List<AppInfo> apps) {
		mTask.detach();
		mTask = null;
		if (apps != null && apps.size() > 0){
			PreferenceCategory appList = (PreferenceCategory) getPreferenceScreen().findPreference("prefCatNotificationApps");
			appList.removePreference(mDummyAppPreference);
			for (AppInfo app : apps){
				CheckBoxPreference pref = new CheckBoxPreference(this);
				pref.setTitle(app.getName());
				pref.setSummary(app.getPackageName());
				pref.setChecked(!app.isSkipNotification());
				pref.setOnPreferenceChangeListener(mAppPrefChangedListener);
				appList.addPreference(pref);
				// TODO Load the app's icon from cache and add it?
			}
		} else {
			mDummyAppPreference.setTitle(R.string.no_published_apps);
		}
	}	

	OnPreferenceChangeListener mAppPrefChangedListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			// TODO Should this be done in a different thread?
			AndlyticsApp.getInstance().getDbAdapter().setSkipNotification((String) preference.getSummary(), !(Boolean)newValue);
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
