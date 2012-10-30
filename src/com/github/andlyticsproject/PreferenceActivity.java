
package com.github.andlyticsproject;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.sync.AutosyncHandler;
import com.github.andlyticsproject.sync.AutosyncHandlerFactory;

// Suppressing warnings as there is no SherlockPreferenceFragment
// for us to use instead of a PreferencesActivity
@SuppressWarnings("deprecation")
public class PreferenceActivity extends SherlockPreferenceActivity implements
		OnPreferenceChangeListener, OnSharedPreferenceChangeListener {

	private PreferenceCategory accountListPrefCat;
	private List<String> accountsList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(Preferences.PREF);
		addPreferencesFromResource(R.xml.preferences);

		// Find and setup a listener for auto sync as we have had to adjust the sync handler
		getPreferenceScreen().findPreference(Preferences.AUTOSYNC_PERIOD)
				.setOnPreferenceChangeListener(this);
		
		// We have to clear cached date formats when they change
		getPreferenceScreen().findPreference(Preferences.DATE_FORMAT_LONG)
				.setOnPreferenceChangeListener(this);

		// Find the preference category used to list all the accounts
		accountListPrefCat = (PreferenceCategory) getPreferenceScreen().findPreference(
				"prefCatAccountSpecific");

		buildAccountsList();

		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
			initSummary(getPreferenceScreen().getPreference(i));
		}
	}

	private void buildAccountsList() {
		final AccountManager manager = AccountManager.get(this);
		Account[] accounts = manager.getAccountsByType(Constants.ACCOUNT_TYPE_GOOGLE);
		accountsList = new ArrayList<String>();
		for (Account account : accounts) {
			if (!Preferences.getIsHiddenAccount(this, account.name)) {
				// Add all non hidden accounts to the list for use with the auto sync preference
				accountsList.add(account.name);
				// Create a preference representing the account and add it to the screen
				Preference pref = new Preference(this);
				pref.setTitle(account.name);
				pref.setOnPreferenceClickListener(accountPrefrenceClickedListener);
				accountListPrefCat.addPreference(pref);
			}
		}

	}

	OnPreferenceClickListener accountPrefrenceClickedListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			String accountName = (String) preference.getTitle();
			Intent i = new Intent(PreferenceActivity.this, AccountSpecificPreferenceActivity.class);
			i.putExtra(Constants.AUTH_ACCOUNT_NAME, accountName);
			startActivity(i);
			return true;
		}
	};

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals(Preferences.AUTOSYNC_PERIOD)) {
			Integer newPeriod = Integer.parseInt((String) newValue);
			newPeriod = newPeriod * 60; // Convert from minutes to seconds
			AutosyncHandler autosyncHandler = AutosyncHandlerFactory.getInstance(this);
			for (String account : accountsList) {
				// Setup auto sync for every account that has it enabled
				// Note: accountsList does not contain hidden accounts, so we don't need to check
				if (Preferences.isAutoSyncEnabled(PreferenceActivity.this, account)) {
					int autosyncPeriod = autosyncHandler.getAutosyncPeriod(account);
					if (autosyncPeriod != newPeriod) {
						autosyncHandler.setAutosyncPeriod(account, newPeriod);
					}
				}
			}
		} else if (preference.getKey().equals(Preferences.DATE_FORMAT_LONG)) {
			Preferences.clearCachedDateFormats();
		}
		return true;
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

	// Generic code to provide summaries for preferences based on their values
	// Overkill at the moment, but may be useful in the future as we add more options

	private void initSummary(Preference p) {
		if (p instanceof PreferenceCategory) {
			PreferenceCategory pCat = (PreferenceCategory) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				initSummary(pCat.getPreference(i));
			}
		} else {
			updatePrefSummary(p);
		}

	}

	private void updatePrefSummary(Preference p) {
		if (p instanceof ListPreference) {
			ListPreference listPref = (ListPreference) p;
			p.setSummary(listPref.getEntry());
		} else if (p instanceof EditTextPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p;
			p.setSummary(editTextPref.getText());
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePrefSummary(findPreference(key));

	}

	@Override
	protected void onResume() {
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
				this);
	}

}
