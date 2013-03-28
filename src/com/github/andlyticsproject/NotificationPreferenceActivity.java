package com.github.andlyticsproject;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.util.UiUtils;

// See PreferenceActivity for warning suppression justification
@SuppressWarnings("deprecation")
public class NotificationPreferenceActivity extends SherlockPreferenceActivity {

	private Preference downloadsPref;
	private Preference ratingsPref;
	private Preference commentsPref;
	private PreferenceCategory notificationSignalPrefCat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(Preferences.PREF);
		addPreferencesFromResource(R.xml.notification_preferences);

		// Get a reference to the notification triggers so that we can visually disable the other
		// notification preferences when all the triggers are disabled
		// TODO: Can we do this all using one generic listener?
		ratingsPref = getPreferenceScreen().findPreference(Preferences.NOTIFICATION_CHANGES_RATING);
		ratingsPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean notificationsEnabled = (Boolean) newValue
						|| UiUtils.isChecked(commentsPref) || UiUtils.isChecked(downloadsPref);
				notificationSignalPrefCat.setEnabled(notificationsEnabled);
				return true;
			}
		});

		commentsPref = getPreferenceScreen().findPreference(
				Preferences.NOTIFICATION_CHANGES_COMMENTS);
		commentsPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean notificationsEnabled = (Boolean) newValue || UiUtils.isChecked(ratingsPref)
						|| UiUtils.isChecked(downloadsPref);
				notificationSignalPrefCat.setEnabled(notificationsEnabled);
				return true;
			}
		});

		downloadsPref = getPreferenceScreen().findPreference(
				Preferences.NOTIFICATION_CHANGES_DOWNLOADS);
		downloadsPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Boolean notificationsEnabled = (Boolean) newValue || UiUtils.isChecked(ratingsPref)
						|| UiUtils.isChecked(commentsPref);
				notificationSignalPrefCat.setEnabled(notificationsEnabled);
				return true;
			}
		});

		// Notification signal
		notificationSignalPrefCat = (PreferenceCategory) getPreferenceScreen().findPreference(
				"prefCatNotificationSignal");
		// Set initial enabled state based on the triggers
		Boolean notificationsEnabled = UiUtils.isChecked(commentsPref)
				|| UiUtils.isChecked(ratingsPref) || UiUtils.isChecked(downloadsPref);
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

}
