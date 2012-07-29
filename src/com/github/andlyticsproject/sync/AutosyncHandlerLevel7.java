
package com.github.andlyticsproject.sync;

import com.github.andlyticsproject.Constants;
import com.github.andlyticsproject.Preferences;

import android.accounts.Account;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class AutosyncHandlerLevel7 implements AutosyncHandler {

	private static final String TAG = AutosyncHandlerLevel7.class.getSimpleName();

	private Context context;

	public AutosyncHandlerLevel7(Context context) {
		this.context = context;
	}

	public boolean isAutosyncEnabled(String accountname) {

		Account account = new Account(accountname, Constants.ACCOUNT_TYPE_GOOGLE);
		return ContentResolver.getSyncAutomatically(account, Constants.ACCOUNT_AUTHORITY);
	}

	public int getAutosyncPeriod(String accountname) {

		if (!isAutosyncEnabled(accountname)) {
			return 0;
		} else {
			return Preferences.getLevel7AlarmManagerPeriod(context);
		}

	}

	/**
	 * Periodic sync for level7 can not be configured for different periods per account.
	 */
	@Override
	public void setAutosyncPeriod(String accountName, Integer periodInSeconds) {

		Account account = new Account(accountName, Constants.ACCOUNT_TYPE_GOOGLE);

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, i, 0);

		Log.d(TAG, "setting alarm to:: " + periodInSeconds);

		int previousPeriod = Preferences.getLevel7AlarmManagerPeriod(context);

		Preferences.saveLevel7AlarmManagerPeriod(periodInSeconds, context);

		if (periodInSeconds == 0) {
			Log.d(TAG, "cancel alarm for:: " + pendingIntent);
			alarmManager.cancel(pendingIntent);
			ContentResolver.setSyncAutomatically(account, Constants.ACCOUNT_AUTHORITY, false);

		} else {
			Log.d(TAG, "create alarm for:: " + pendingIntent);
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), periodInSeconds * 1000, pendingIntent);
			if (previousPeriod == 0) {
				ContentResolver.setSyncAutomatically(account, Constants.ACCOUNT_AUTHORITY, true);
			}

		}

	}

}
