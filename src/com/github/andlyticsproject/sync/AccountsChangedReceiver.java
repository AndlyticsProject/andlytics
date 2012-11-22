package com.github.andlyticsproject.sync;

import java.util.Date;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.github.andlyticsproject.Constants;
import com.github.andlyticsproject.Preferences;

public class AccountsChangedReceiver extends BroadcastReceiver {

	private static final String TAG = AccountsChangedReceiver.class.getSimpleName();

	@SuppressWarnings("deprecation")
	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d(TAG, "onReceive called at:: " + new Date(System.currentTimeMillis()).toGMTString());

		final AccountManager manager = AccountManager.get(context);
		final Account[] accounts = manager.getAccountsByType(Constants.ACCOUNT_TYPE_GOOGLE);
		for (Account account : accounts) {
			// skip disabled/hidden accounts
			if (Preferences.getIsHiddenAccount(context, account.name)) {
				continue;
			}

			boolean syncAutomatically = ContentResolver.getSyncAutomatically(account,
					Constants.ACCOUNT_AUTHORITY);
			if (syncAutomatically) {
				Bundle extras = new Bundle();
				Log.d(TAG,
						"requesting sync for " + account.name + " now! :: "
								+ new Date(System.currentTimeMillis()).toGMTString());
				ContentResolver.requestSync(account, Constants.ACCOUNT_AUTHORITY, extras);
			} else {
				Log.d(TAG, "auto sync disabled for account :: " + account.name);
			}
		}
	}

}
