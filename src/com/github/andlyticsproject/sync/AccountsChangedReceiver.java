package com.github.andlyticsproject.sync;

import java.util.Date;
import java.util.List;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.github.andlyticsproject.Constants;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.model.DeveloperAccount;

public class AccountsChangedReceiver extends BroadcastReceiver {

	private static final String TAG = AccountsChangedReceiver.class.getSimpleName();

	@SuppressWarnings("deprecation")
	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d(TAG, "onReceive called at:: " + new Date(System.currentTimeMillis()).toGMTString());

		List<DeveloperAccount> accounts = AndlyticsDb.getInstance(context)
				.getActiveDeveloperAccounts();
		for (DeveloperAccount developerAccount : accounts) {
			Account googleAccount = new Account(developerAccount.getName(),
					Constants.ACCOUNT_TYPE_GOOGLE);
			boolean syncAutomatically = ContentResolver.getSyncAutomatically(googleAccount,
					Constants.ACCOUNT_AUTHORITY);
			if (syncAutomatically) {
				Bundle extras = new Bundle();
				Log.d(TAG, "requesting sync for " + developerAccount + " now! :: "
						+ new Date(System.currentTimeMillis()).toGMTString());
				ContentResolver.requestSync(googleAccount, Constants.ACCOUNT_AUTHORITY, extras);
			} else {
				Log.d(TAG, "auto sync disabled for account :: " + developerAccount);
			}
		}
	}

}
