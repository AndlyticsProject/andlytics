package com.github.andlyticsproject.sync;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.github.andlyticsproject.AppStatsDiff;
import com.github.andlyticsproject.ContentAdapter;
import com.github.andlyticsproject.console.DevConsoleException;
import com.github.andlyticsproject.console.v2.DevConsoleRegistry;
import com.github.andlyticsproject.console.v2.DevConsoleV2;
import com.github.andlyticsproject.console.v2.HttpClientFactory;
import com.github.andlyticsproject.model.AppInfo;

public class SyncAdapterService extends Service {

	private static final String TAG = SyncAdapterService.class.getSimpleName();

	private static SyncAdapterImpl sSyncAdapter = null;

	private static ContentAdapter db;

	public SyncAdapterService() {
		super();
	}

	private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
		private Context mContext;

		public SyncAdapterImpl(Context context) {
			super(context, true);
			mContext = context;
		}

		@Override
		public void onPerformSync(Account account, Bundle extras, String authority,
				ContentProviderClient provider, SyncResult syncResult) {
			try {
				SyncAdapterService.performSync(mContext, account, extras, authority, provider,
						syncResult);
			} catch (OperationCanceledException e) {
				Log.w(TAG, "operation canceled", e);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		IBinder ret = null;
		ret = getSyncAdapter().getSyncAdapterBinder();
		return ret;
	}

	private SyncAdapterImpl getSyncAdapter() {
		if (sSyncAdapter == null)
			sSyncAdapter = new SyncAdapterImpl(this);
		return sSyncAdapter;
	}

	private static void performSync(Context context, Account account, Bundle extras,
			String authority, ContentProviderClient provider, SyncResult syncResult)
			throws OperationCanceledException {
		try {
			DevConsoleV2 console = DevConsoleRegistry.getInstance().get(account.name);
			if (console == null) {
				DefaultHttpClient httpClient = HttpClientFactory
						.createDevConsoleHttpClient(DevConsoleV2.TIMEOUT);
				console = DevConsoleV2.createForAccount(account.name, httpClient);
				DevConsoleRegistry.getInstance().put(account.name, console);
			}

			if (console != null) {
				List<AppInfo> appDownloadInfos = console.getAppInfo(null);
				// this can also happen if authentication fails and the user 
				// need to click on a notification to confir or re-enter 
				// password
				if (appDownloadInfos.isEmpty()) {
					return;
				}

				Log.d(TAG, "andlytics from sync adapter, size: " + appDownloadInfos.size());

				List<AppStatsDiff> diffs = new ArrayList<AppStatsDiff>();

				db = new ContentAdapter(context);
				for (AppInfo appDownloadInfo : appDownloadInfos) {
					// update in database
					diffs.add(db.insertOrUpdateStats(appDownloadInfo));
				}
				Log.d(TAG, "sucessfully synced andlytics");

				// check for notifications
				NotificationHandler.handleNotificaions(context, diffs, account.name);

				// } else {
				// Log.e(TAG, "error during sync auth, no token found");
				// }
			}
		} catch (DevConsoleException e) {
			Log.e(TAG, "error during sync", e);
		}

	}
}
