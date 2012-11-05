
package com.github.andlyticsproject.sync;

import java.util.List;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.PeriodicSync;
import android.os.Bundle;

import com.github.andlyticsproject.Constants;

public class AutosyncHandler {	

	public static int DEFAULT_PERIOD = 60 * 3; // 3 hours (in minutes)

	public boolean isAutosyncEnabled(String accountName) {
		Account account = new Account(accountName, Constants.ACCOUNT_TYPE_GOOGLE);
		return ContentResolver.getSyncAutomatically(account, Constants.ACCOUNT_AUTHORITY);
	}
	
	public void setAutosyncEnabled(String accountName, boolean enabled) {
		Account account = new Account(accountName, Constants.ACCOUNT_TYPE_GOOGLE);
		ContentResolver.setSyncAutomatically(account, Constants.ACCOUNT_AUTHORITY, enabled);
	}

	/**
	 * Gets the sync period in minutes for the given account
	 * @param accountName
	 * @return The sync period in minutes
	 */
	public int getAutosyncPeriod(String accountName) {

		int result = 0;

		Account account = new Account(accountName, Constants.ACCOUNT_TYPE_GOOGLE);
		if (ContentResolver.getSyncAutomatically(account, Constants.ACCOUNT_AUTHORITY)) {
			List<PeriodicSync> periodicSyncs = ContentResolver.getPeriodicSyncs(account,
					Constants.ACCOUNT_AUTHORITY);
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
		Account account = new Account(accountName, Constants.ACCOUNT_TYPE_GOOGLE);

		if (periodInMins == 0) {
			ContentResolver.setSyncAutomatically(account, Constants.ACCOUNT_AUTHORITY, false);
		} else {
			ContentResolver.setSyncAutomatically(account, Constants.ACCOUNT_AUTHORITY, true);
			ContentResolver.addPeriodicSync(account, Constants.ACCOUNT_AUTHORITY, extras, periodInMins * 60);
		}
		
	}

}
