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

// See PreferenceActivity for warning suppression justification
@SuppressWarnings("deprecation")
public class NotificationPreferenceActivity extends PreferenceActivity {

	private AppCompatDelegate mDelegate;
	private CheckBoxPreference downloadsPref;
	private CheckBoxPreference ratingsPref;
	private CheckBoxPreference commentsPref;
	private PreferenceCategory notificationSignalPrefCat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getDelegate().installViewFactory();
		getDelegate().onCreate(savedInstanceState);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.preference_activity);
		setSupportActionBar((Toolbar) findViewById(R.id.pref_toolbar));
		findViewById(R.id.pref_toolbar).setBackgroundColor(getResources().getColor(R.color.lightBlue));

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(Preferences.PREF);
		addPreferencesFromResource(R.xml.notification_preferences);

		// Get a reference to the notification triggers so that we can visually disable the other
		// notification preferences when all the triggers are disabled
		// TODO: Can we do this all using one generic listener?
		ratingsPref = (CheckBoxPreference) getPreferenceScreen().findPreference(
				Preferences.NOTIFICATION_CHANGES_RATING);
		ratingsPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean notificationsEnabled = (Boolean) newValue || commentsPref.isChecked()
						|| downloadsPref.isChecked();
				notificationSignalPrefCat.setEnabled(notificationsEnabled);
				return true;
			}
		});

		commentsPref = (CheckBoxPreference) getPreferenceScreen().findPreference(
				Preferences.NOTIFICATION_CHANGES_COMMENTS);
		commentsPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean notificationsEnabled = (Boolean) newValue || ratingsPref.isChecked()
						|| downloadsPref.isChecked();
				notificationSignalPrefCat.setEnabled(notificationsEnabled);
				return true;
			}
		});

		downloadsPref = (CheckBoxPreference) getPreferenceScreen().findPreference(
				Preferences.NOTIFICATION_CHANGES_DOWNLOADS);
		downloadsPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean notificationsEnabled = (Boolean) newValue || ratingsPref.isChecked()
						|| commentsPref.isChecked();
				notificationSignalPrefCat.setEnabled(notificationsEnabled);
				return true;
			}
		});

		// Notification signal
		notificationSignalPrefCat = (PreferenceCategory) getPreferenceScreen().findPreference(
				"prefCatNotificationSignal");
		// Set initial enabled state based on the triggers
		Boolean notificationsEnabled = commentsPref.isChecked() || ratingsPref.isChecked()
				|| downloadsPref.isChecked();
		notificationSignalPrefCat.setEnabled(notificationsEnabled);
	}

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
