
package com.github.andlyticsproject.sync;

import java.util.List;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.PeriodicSync;
import android.os.Bundle;


public class AutosyncHandler {	

	public static int DEFAULT_PERIOD = 60 * 3; // 3 hours (in minutes)
	public static final String ACCOUNT_AUTHORITY = "com.github.andlyticsproject";
	public static final String ACCOUNT_TYPE_GOOGLE = "com.google";

	public boolean isAutosyncEnabled(String accountName) {
		Account account = new Account(accountName, AutosyncHandler.ACCOUNT_TYPE_GOOGLE);
		return ContentResolver.getSyncAutomatically(account, AutosyncHandler.ACCOUNT_AUTHORITY);
	}
	
	public void setAutosyncEnabled(String accountName, boolean enabled) {
		Account account = new Account(accountName, AutosyncHandler.ACCOUNT_TYPE_GOOGLE);
		ContentResolver.setSyncAutomatically(account, AutosyncHandler.ACCOUNT_AUTHORITY, enabled);
	}

	/**
	 * Gets the sync period in minutes for the given account
	 * @param accountName
	 * @return The sync period in minutes
	 */
	public int getAutosyncPeriod(String accountName) {

		int result = 0;

		Account account = new Account(accountName, AutosyncHandler.ACCOUNT_TYPE_GOOGLE);
		if (ContentResolver.getSyncAutomatically(account, AutosyncHandler.ACCOUNT_AUTHORITY)) {
			List<PeriodicSync> periodicSyncs = ContentResolver.getPeriodicSyncs(account,
					AutosyncHandler.ACCOUNT_AUTHORITY);
			for (PeriodicSync periodicSync : periodicSyncs) {
				result = 60 * (int) periodicSync.period;
				break;
			}
		}
		return result;

	}

	/**
	 * Sets the sync period in minutes for the given account
	 * @param accountName
	 * @param periodInMins
	 */
	public void setAutosyncPeriod(String accountName, Integer periodInMins) {

		Bundle extras = new Bundle();
		Account account = new Account(accountName, AutosyncHandler.ACCOUNT_TYPE_GOOGLE);

		if (periodInMins == 0) {
			ContentResolver.setSyncAutomatically(account, AutosyncHandler.ACCOUNT_AUTHORITY, false);
		} else {
			ContentResolver.setSyncAutomatically(account, AutosyncHandler.ACCOUNT_AUTHORITY, true);
			ContentResolver.addPeriodicSync(account, AutosyncHandler.ACCOUNT_AUTHORITY, extras, periodInMins * 60);
		}
		
	}

}
