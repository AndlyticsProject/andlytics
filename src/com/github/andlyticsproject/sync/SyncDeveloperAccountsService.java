package com.github.andlyticsproject.sync;

import java.util.Date;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.github.andlyticsproject.DeveloperAccountManager;
import com.github.andlyticsproject.model.DeveloperAccount;

public class SyncDeveloperAccountsService extends IntentService {

	private static final String TAG = SyncDeveloperAccountsService.class.getSimpleName();

	private DeveloperAccountManager developerAccountManager;

	public SyncDeveloperAccountsService() {
		super("SyncDeveloperAccountsService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		developerAccountManager = DeveloperAccountManager.getInstance(this);
		Account[] googleAccounts = AccountManager.get(this).getAccountsByType(
				AutosyncHandler.ACCOUNT_TYPE_GOOGLE);

		Log.d(TAG, "Synchornizing Andlytics developer accounts and Google accounts...");
		addNewGoogleAccounts(googleAccounts);

		removeStaleGoogleAccounts(googleAccounts);

		syncData();
	}

	private void removeStaleGoogleAccounts(Account[] googleAccounts) {
		int removed = 0;
		List<DeveloperAccount> developerAccounts = developerAccountManager.getAllDeveloperAccounts();
		for (DeveloperAccount account : developerAccounts) {
			Account googleAccount = findMatchingAccount(account, googleAccounts);
			if (googleAccount == null) {
				Log.d(TAG, "Removing  " + account);
				developerAccountManager.deleteDeveloperAccount(account);
				removed++;
			}
		}

		Log.d(TAG, String.format("Removed %d stale Google accounts.", removed));
	}

	private void addNewGoogleAccounts(Account[] googleAccounts) {
		// add new accounts as hidden
		int added = 0;
		List<DeveloperAccount> developerAccounts = developerAccountManager.getAllDeveloperAccounts();
		for (Account googleAccount : googleAccounts) {
			DeveloperAccount account = DeveloperAccount.createHidden(googleAccount.name);
			if (!developerAccounts.contains(account)) {
				Log.d(TAG, "Adding  " + account);
				developerAccountManager.addDeveloperAccount(account);
				added++;
			}
		}

		Log.d(TAG, String.format("Added %d new Google accounts.", added));
	}

	@SuppressWarnings("deprecation")
	private void syncData() {
		List<DeveloperAccount> accounts = developerAccountManager.getActiveDeveloperAccounts();
		for (DeveloperAccount developerAccount : accounts) {
			Account googleAccount = new Account(developerAccount.getName(),
					AutosyncHandler.ACCOUNT_TYPE_GOOGLE);
			boolean syncAutomatically = ContentResolver.getSyncAutomatically(googleAccount,
					AutosyncHandler.ACCOUNT_AUTHORITY);
			if (syncAutomatically && developerAccount.isVisible()) {
				Bundle extras = new Bundle();
				Log.d(TAG, "requesting sync for " + developerAccount + " now! :: "
						+ new Date(System.currentTimeMillis()).toGMTString());
				ContentResolver.requestSync(googleAccount, AutosyncHandler.ACCOUNT_AUTHORITY, extras);
			} else {
				Log.d(TAG, "auto sync disabled for account :: " + developerAccount);
			}
		}
	}

	private Account findMatchingAccount(DeveloperAccount account, Account[] googleAccounts) {
		for (Account googleAccount : googleAccounts) {
			if (googleAccount.name.equals(account.getName())) {
				return googleAccount;
			}
		}

		return null;
	}

}
