package com.github.andlyticsproject.sync;

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

import com.github.andlyticsproject.AndlyticsApp;
import com.github.andlyticsproject.AppStatsDiff;
import com.github.andlyticsproject.ContentAdapter;
import com.github.andlyticsproject.DeveloperAccountManager;
import com.github.andlyticsproject.admob.AdmobException;
import com.github.andlyticsproject.adsense.AdSenseClient;
import com.github.andlyticsproject.console.DevConsoleException;
import com.github.andlyticsproject.console.v2.DevConsoleRegistry;
import com.github.andlyticsproject.console.v2.DevConsoleV2;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.DeveloperAccount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
			// If the account is hidden and the user enables syncing for it via system
			// then this could get called. Check account state and only sync
			// if not hidden.
			DeveloperAccount developerAccount = DeveloperAccountManager.getInstance(mContext)
					.findDeveloperAccountByName(account.name);
			if (developerAccount != null && developerAccount.isVisible()) {
				try {
					SyncAdapterService.performSync(mContext, account, extras, authority, provider,
							syncResult);
				} catch (OperationCanceledException e) {
					Log.w(TAG, "operation canceled", e);
				}
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

			if (console != null) {
				List<AppInfo> appDownloadInfos = console.getAppInfo(null);
				// this can also happen if authentication fails and the user
				// need to click on a notification to confirm or re-enter
				// password (e.g., if password changed or 2FA enabled)
				if (appDownloadInfos.isEmpty()) {
					return;
				}

				Log.d(TAG, "andlytics from sync adapter, size: " + appDownloadInfos.size());

				List<AppStatsDiff> diffs = new ArrayList<AppStatsDiff>();
				Map<String, List<String>> admobAccountSiteMap = new HashMap<String, List<String>>();

				db = ContentAdapter.getInstance(AndlyticsApp.getInstance());
				for (AppInfo appDownloadInfo : appDownloadInfos) {
					// update in database
					diffs.add(db.insertOrUpdateStats(appDownloadInfo));
					String[] admobDetails = AndlyticsDb.getInstance(context).getAdmobDetails(
							appDownloadInfo.getPackageName());
					if (admobDetails != null) {
						String admobAccount = admobDetails[0];
						String admobSiteId = admobDetails[1];
						String adUnitId = admobDetails[2];
						// only sync legacy data if not migrated
						if (admobAccount != null && adUnitId == null) {
							List<String> siteList = admobAccountSiteMap.get(admobAccount);
							if (siteList == null) {
								siteList = new ArrayList<String>();
							}
							siteList.add(admobSiteId);
							admobAccountSiteMap.put(admobAccount, siteList);
						} else {
							List<String> siteList = admobAccountSiteMap.get(admobAccount);
							if (siteList == null) {
								siteList = new ArrayList<String>();
							}
							siteList.add(adUnitId);
							admobAccountSiteMap.put(admobAccount, siteList);
						}
					}
					// update app details
					AndlyticsDb.getInstance(context).insertOrUpdateAppDetails(appDownloadInfo);
				}
				Log.d(TAG, "sucessfully synced andlytics");

				// check for notifications
				NotificationHandler.handleNotificaions(context, diffs, account.name);

				if (!admobAccountSiteMap.isEmpty()) {
					Log.d(TAG, "Syncing AdMob stats");
					// sync admob accounts
					Set<String> admobAccuntKeySet = admobAccountSiteMap.keySet();
					for (String admobAccount : admobAccuntKeySet) {
						AdSenseClient.backgroundSyncStats(context, admobAccount,
								admobAccountSiteMap.get(admobAccount), extras, authority, null);
					}
					Log.d(TAG, "Sucessfully synced AdMob stats");
				}

				DeveloperAccountManager.getInstance(context).saveLastStatsRemoteUpdateTime(
						account.name, System.currentTimeMillis());
			}
		} catch (DevConsoleException e) {
			Log.e(TAG, "error during dev console stats sync", e);
		} catch (AdmobException e) {
			Log.e(TAG, "error during Admob sync", e);
		} catch (Exception e) {
			Log.e(TAG, "error during sync", e);
		}

	}
}
