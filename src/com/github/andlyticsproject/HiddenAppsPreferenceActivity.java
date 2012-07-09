package com.github.andlyticsproject;

import java.util.List;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.AsyncTasks.LoadAppListTask;
import com.github.andlyticsproject.AsyncTasks.LoadAppListTaskCompleteListener;
import com.github.andlyticsproject.model.AppInfo;

// See PreferenceActivity for warning suppression justification
@SuppressWarnings("deprecation")
public class HiddenAppsPreferenceActivity extends SherlockPreferenceActivity implements LoadAppListTaskCompleteListener {

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
		addPreferencesFromResource(R.xml.hidden_app_preferences);

		// App list
		PreferenceScreen screen = getPreferenceScreen();
		// Create a dummy preference while we load the app list as it can be blocked by other db opperations
		mDummyAppPreference = new Preference(this);
		mDummyAppPreference.setTitle(R.string.loading_app_list);
		screen.addPreference(mDummyAppPreference);

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
			PreferenceScreen screen = getPreferenceScreen();
			screen.removePreference(mDummyAppPreference);
			for (AppInfo app : apps){
				CheckBoxPreference pref = new CheckBoxPreference(this);
				pref.setTitle(app.getName());
				pref.setSummary(app.getPackageName());
				pref.setChecked(app.isGhost());
				pref.setOnPreferenceChangeListener(mAppPrefChangedListener);
				screen.addPreference(pref);
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
			AndlyticsApp.getInstance().getDbAdapter().setGhost(mAccountName, (String) preference.getSummary(), (Boolean) newValue);
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
