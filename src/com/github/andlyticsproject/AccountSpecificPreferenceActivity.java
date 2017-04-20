package com.github.andlyticsproject;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.github.andlyticsproject.AsyncTasks.LoadAppListTask;
import com.github.andlyticsproject.AsyncTasks.LoadAppListTaskCompleteListener;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.sync.AutosyncHandler;
import com.github.andlyticsproject.util.Utils;

import java.util.List;

// See PreferenceActivity for warning suppression justification
@SuppressWarnings("deprecation")
public class AccountSpecificPreferenceActivity extends PreferenceActivity implements
		LoadAppListTaskCompleteListener {

	private AppCompatDelegate mDelegate;
	private String accountName;
	private Preference dummyApp;
	private CheckBoxPreference autosyncPref;
	private PreferenceCategory notificationAppList;
	private PreferenceCategory hiddenAppList;
	private LoadAppListTask task;
	private AutosyncHandler autosyncHandler = new AutosyncHandler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getDelegate().installViewFactory();
		getDelegate().onCreate(savedInstanceState);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.preference_activity);
		setSupportActionBar((Toolbar) findViewById(R.id.pref_toolbar));
		findViewById(R.id.pref_toolbar).setBackgroundColor(getResources().getColor(R.color.lightBlue));

		accountName = getIntent().getExtras().getString(BaseActivity.EXTRA_AUTH_ACCOUNT_NAME);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setTitle(accountName);
		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(Preferences.PREF);
		addPreferencesFromResource(R.xml.account_specific_preferences);


		// Setup auto sync option
		PreferenceCategory autoSyncCat = (PreferenceCategory) getPreferenceScreen().findPreference(
				"prefCatAutoSync");
		autosyncPref = new CheckBoxPreference(this);
		autosyncPref.setPersistent(false);
		autosyncPref.setChecked(autosyncHandler.isAutosyncEnabled(accountName));
		autosyncPref.setTitle(R.string.auto_sync);
		autosyncPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				autosyncHandler.setAutosyncEnabled(accountName, (Boolean) newValue);
				return true;
			}
		});
		autoSyncCat.addPreference(autosyncPref);

		// Dummy app to show when loading the app list
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
			Utils.execute(task, accountName);
		} else {
			task.attach(this);
			List<AppInfo> apps = task.getResult();
			if (apps != null) {
				onLoadAppListTaskComplete(apps);
			}
		}
	}

	@Override
	protected void onResume() {
		autosyncPref.setChecked(autosyncHandler.isAutosyncEnabled(accountName));
		super.onResume();
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
			// Clear the dummy app
			notificationAppList.removePreference(dummyApp);
			hiddenAppList.removePreference(dummyApp);
			for (AppInfo app : apps) {
				// Add the notification preference
				CheckBoxPreference pref = new CheckBoxPreference(this);
				pref.setTitle(app.getName());
				pref.setSummary(app.getPackageName());
				pref.setChecked(!app.isSkipNotification());
				pref.setOnPreferenceChangeListener(notificationAppPrefChangedListener);
				notificationAppList.addPreference(pref);

				// Add the hidden app preference
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

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		getDelegate().onPostCreate(savedInstanceState);
	}

	@Override
	public void setContentView(@LayoutRes int layoutResID) {
		getDelegate().setContentView(layoutResID);
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		getDelegate().onPostResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
		getDelegate().onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getDelegate().onDestroy();
	}

	private void setSupportActionBar(@Nullable Toolbar toolbar) {
		getDelegate().setSupportActionBar(toolbar);
	}

	private ActionBar getSupportActionBar() {
		return getDelegate().getSupportActionBar();
	}

	private AppCompatDelegate getDelegate() {
		if (mDelegate == null) {
			mDelegate = AppCompatDelegate.create(this, null);
		}
		return mDelegate;
	}

}
