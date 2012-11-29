package com.github.andlyticsproject.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AccountsChangedReceiver extends BroadcastReceiver {

	private static final String TAG = AccountsChangedReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "System Google accounts changed. Will sync accounts and data.");

		Intent syncServiceIntent = new Intent(context, SyncDeveloperAccountsService.class);
		context.startService(syncServiceIntent);
	}

}
