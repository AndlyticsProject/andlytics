package com.github.andlyticsproject;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.sync.AutosyncHandler;
import com.github.andlyticsproject.sync.AutosyncHandlerFactory;


//Suppressing warnings as there is no SherlockPreferenceFragment
// for us to use instead of a PreferencesActivity
@SuppressWarnings("deprecation")
public class PreferenceActivity extends SherlockPreferenceActivity
		implements OnPreferenceChangeListener, OnSharedPreferenceChangeListener {
	
	private String mAccountName;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		mAccountName = getIntent().getExtras().getString(Constants.AUTH_ACCOUNT_NAME);
		getSupportActionBar().setSubtitle(mAccountName);

		addPreferencesFromResource(R.xml.preferences);
		
		getPreferenceScreen().findPreference(Preferences.PREF_AUTO_SYNC_PERIOD)
				.setOnPreferenceChangeListener(this);

		for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
			initSummary(getPreferenceScreen().getPreference(i));
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if(preference.getKey().equals(Preferences.PREF_AUTO_SYNC_PERIOD)){
			Integer newPeriod = Integer.parseInt((String) newValue);
			newPeriod = newPeriod * 60; // Convert from minutes to seconds
			AutosyncHandler autosyncHandler = AutosyncHandlerFactory.getInstance(this);
	        int autosyncPeriod = autosyncHandler.getAutosyncPeriod(mAccountName);
			if(autosyncPeriod != newPeriod) {
			    autosyncHandler.setAutosyncPeriod(mAccountName, newPeriod);
			}
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

	private void initSummary(Preference p){
		if (p instanceof PreferenceCategory){
			PreferenceCategory pCat = (PreferenceCategory)p;
			for(int i=0;i<pCat.getPreferenceCount();i++){
				initSummary(pCat.getPreference(i));
			}
		} else{
			updatePrefSummary(p);
		}

	}

	private void updatePrefSummary(Preference p){
		if (p instanceof ListPreference) {
			ListPreference listPref = (ListPreference) p; 
			p.setSummary(listPref.getEntry()); 
		} else 	if (p instanceof EditTextPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p; 
			p.setSummary(editTextPref.getText()); 
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePrefSummary(findPreference(key));
		
	}
	
	@Override 
    protected void onResume(){
        super.onResume();
        // Set up a listener whenever a key changes             
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override 
    protected void onPause() { 
        super.onPause();
        // Unregister the listener whenever a key changes             
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);     
    }

}
