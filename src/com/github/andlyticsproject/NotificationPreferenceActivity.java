
package com.github.andlyticsproject;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

// See PreferenceActivity for warning suppression justification
@SuppressWarnings("deprecation")
public class NotificationPreferenceActivity extends SherlockPreferenceActivity {

	private CheckBoxPreference downloadsPref;
	private CheckBoxPreference ratingsPref;
	private CheckBoxPreference commentsPref;
	private PreferenceCategory notificationSignalPrefCat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(Preferences.PREF);
		addPreferencesFromResource(R.xml.notification_preferences);

		// Notification trigger
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
		// Set initial enabled state
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
}
