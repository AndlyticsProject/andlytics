package com.github.andlyticsproject;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.backup.BackupManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.db.AdmobTable;
import com.github.andlyticsproject.db.AppInfoTable;
import com.github.andlyticsproject.db.AppStatsTable;
import com.github.andlyticsproject.db.CommentsTable;
import com.github.andlyticsproject.db.RevenueSummaryTable;
import com.github.andlyticsproject.model.AdmobStats;
import com.github.andlyticsproject.model.AdmobStatsSummary;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.AppStatsSummary;
import com.github.andlyticsproject.model.Comment;
import com.github.andlyticsproject.model.Revenue;
import com.github.andlyticsproject.model.RevenueSummary;
import com.github.andlyticsproject.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ContentAdapter {

	private final Context context;

	private static ContentAdapter instance;

	private BackupManager backupManager;

	private ContentAdapter(Context ctx) {
		this.context = ctx;
		this.backupManager = new BackupManager(ctx);
	}

	public static synchronized ContentAdapter getInstance(Application appCtx) {
		if (instance == null) {
			instance = new ContentAdapter(appCtx);
		}

		return instance;
	}

	public AdmobStatsSummary getAdmobStats(String siteId, Timeframe currentTimeFrame) {
		return getAdmobStats(siteId, null, currentTimeFrame);
	}

	public AdmobStatsSummary getAdmobStats(String siteId, String adUnitId,
			Timeframe currentTimeFrame) {
		AdmobStatsSummary statsSummary = new AdmobStatsSummary();

		int limit = Integer.MAX_VALUE;
		if (currentTimeFrame.equals(Timeframe.LAST_NINETY_DAYS)) {
			limit = 90;
		} else if (currentTimeFrame.equals(Timeframe.LAST_THIRTY_DAYS)) {
			limit = 30;
		} else if (currentTimeFrame.equals(Timeframe.LAST_SEVEN_DAYS)) {
			limit = 7;
		} else if (currentTimeFrame.equals(Timeframe.LAST_TWO_DAYS)) {
			limit = 2;
		} else if (currentTimeFrame.equals(Timeframe.LATEST_VALUE)) {
			limit = 1;
		} else if (currentTimeFrame.equals(Timeframe.MONTH_TO_DATE)) {
			limit = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		}

		Cursor cursor = null;

		try {
			if (adUnitId == null) {
				cursor = context.getContentResolver().query(
						AdmobTable.CONTENT_URI,
						new String[] {

						AdmobTable.KEY_ROWID, AdmobTable.KEY_SITE_ID, AdmobTable.KEY_REQUESTS,
								AdmobTable.KEY_HOUSEAD_REQUESTS,
								AdmobTable.KEY_INTERSTITIAL_REQUESTS, AdmobTable.KEY_IMPRESSIONS,
								AdmobTable.KEY_FILL_RATE, AdmobTable.KEY_HOUSEAD_FILL_RATE,
								AdmobTable.KEY_OVERALL_FILL_RATE, AdmobTable.KEY_CLICKS,
								AdmobTable.KEY_HOUSEAD_CLICKS, AdmobTable.KEY_CTR,
								AdmobTable.KEY_ECPM, AdmobTable.KEY_REVENUE,
								AdmobTable.KEY_CPC_REVENUE, AdmobTable.KEY_CPM_REVENUE,
								AdmobTable.KEY_EXCHANGE_DOWNLOADS, AdmobTable.KEY_DATE,
								AdmobTable.KEY_CURRENCY
						}, AdmobTable.KEY_SITE_ID + "='" + siteId + "'", null,
						AdmobTable.KEY_DATE + " desc LIMIT " + limit + ""); // sort
																			// order ->
																			// new to
																			// old

				while (cursor.moveToNext()) {
					AdmobStats admob = readAdmobStats(cursor);
					statsSummary.addStat(admob);
				}
				cursor.close();
			} else {
				cursor = context.getContentResolver().query(
						AdmobTable.CONTENT_URI,
						new String[] {

						AdmobTable.KEY_ROWID, AdmobTable.KEY_SITE_ID, AdmobTable.KEY_REQUESTS,
								AdmobTable.KEY_HOUSEAD_REQUESTS,
								AdmobTable.KEY_INTERSTITIAL_REQUESTS, AdmobTable.KEY_IMPRESSIONS,
								AdmobTable.KEY_FILL_RATE, AdmobTable.KEY_HOUSEAD_FILL_RATE,
								AdmobTable.KEY_OVERALL_FILL_RATE, AdmobTable.KEY_CLICKS,
								AdmobTable.KEY_HOUSEAD_CLICKS, AdmobTable.KEY_CTR,
								AdmobTable.KEY_ECPM, AdmobTable.KEY_REVENUE,
								AdmobTable.KEY_CPC_REVENUE, AdmobTable.KEY_CPM_REVENUE,
								AdmobTable.KEY_EXCHANGE_DOWNLOADS, AdmobTable.KEY_DATE,
								AdmobTable.KEY_CURRENCY
						}, AdmobTable.KEY_SITE_ID + "=?", new String[] { adUnitId },
						AdmobTable.KEY_DATE + " desc LIMIT " + limit + ""); // sort
																			// order ->
																			// new to
																			// old

				while (cursor.moveToNext()) {
					AdmobStats admob = readAdmobStats(cursor);
					statsSummary.addStat(admob);
				}
				cursor.close();
			}

			statsSummary.calculateOverallStats(0, false);

			return statsSummary;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private AdmobStats readAdmobStats(Cursor cursor) {
		AdmobStats admob = new AdmobStats();

		admob.setSiteId(cursor.getString(cursor.getColumnIndex(AdmobTable.KEY_ROWID)));
		admob.setClicks(cursor.getInt(cursor.getColumnIndex(AdmobTable.KEY_CLICKS)));
		admob.setCpcRevenue(cursor.getFloat(cursor.getColumnIndex(AdmobTable.KEY_CPC_REVENUE)));
		admob.setCpmRevenue(cursor.getFloat(cursor.getColumnIndex(AdmobTable.KEY_CPM_REVENUE)));
		admob.setCtr(cursor.getFloat(cursor.getColumnIndex(AdmobTable.KEY_CTR)));
		admob.setEcpm(cursor.getFloat(cursor.getColumnIndex(AdmobTable.KEY_ECPM)));
		admob.setExchangeDownloads(cursor.getInt(cursor
				.getColumnIndex(AdmobTable.KEY_EXCHANGE_DOWNLOADS)));
		admob.setFillRate(cursor.getFloat(cursor.getColumnIndex(AdmobTable.KEY_FILL_RATE)));
		admob.setHouseAdClicks(cursor.getInt(cursor.getColumnIndex(AdmobTable.KEY_HOUSEAD_CLICKS)));
		admob.setHouseadFillRate(cursor.getFloat(cursor
				.getColumnIndex(AdmobTable.KEY_HOUSEAD_FILL_RATE)));
		admob.setHouseadRequests(cursor.getInt(cursor
				.getColumnIndex(AdmobTable.KEY_HOUSEAD_REQUESTS)));
		admob.setImpressions(cursor.getInt(cursor.getColumnIndex(AdmobTable.KEY_IMPRESSIONS)));
		admob.setInterstitialRequests(cursor.getInt(cursor
				.getColumnIndex(AdmobTable.KEY_INTERSTITIAL_REQUESTS)));
		admob.setOverallFillRate(cursor.getFloat(cursor
				.getColumnIndex(AdmobTable.KEY_OVERALL_FILL_RATE)));
		admob.setRequests(cursor.getInt(cursor.getColumnIndex(AdmobTable.KEY_REQUESTS)));
		admob.setRevenue(cursor.getFloat(cursor.getColumnIndex(AdmobTable.KEY_REVENUE)));
		String dateString = cursor.getString(cursor.getColumnIndex(AdmobTable.KEY_DATE));
		admob.setDate(Utils.parseDbDate(dateString.substring(0, 10) + " 12:00:00"));
		int idx = cursor.getColumnIndex(AdmobTable.KEY_CURRENCY);
		if (!cursor.isNull(idx)) {
			admob.setCurrencyCode(cursor.getString(idx));
		}

		return admob;
	}

	public void insertOrUpdateAdmobStats(AdmobStats admob) {
		ContentValues value = createAdmobContentValues(admob);

		long existingId = getAdmobStatsIdForDate(admob.getDate(), admob.getSiteId());
		if (existingId > -1) {
			// update
			context.getContentResolver().update(AdmobTable.CONTENT_URI, value,
					AdmobTable.KEY_ROWID + "=" + existingId, null);
		} else {
			// insert
			context.getContentResolver().insert(AdmobTable.CONTENT_URI, value);
		}

		backupManager.dataChanged();
	}

	@SuppressLint("SimpleDateFormat")
	private long getAdmobStatsIdForDate(Date date, String siteId) {

		long result = -1;

		// make sure there is only one entry / day
		SimpleDateFormat dateFormatStart = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
		SimpleDateFormat dateFormatEnd = new SimpleDateFormat("yyyy-MM-dd 23:59:59");

		Cursor mCursor = context.getContentResolver().query(
				AdmobTable.CONTENT_URI,
				new String[] { AdmobTable.KEY_ROWID, AdmobTable.KEY_DATE },
				AdmobTable.KEY_SITE_ID + "='" + siteId + "' and " + AdmobTable.KEY_DATE
						+ " BETWEEN '" + dateFormatStart.format(date) + "' and '"
						+ dateFormatEnd.format(date) + "'", null, null);
		if (mCursor != null && mCursor.moveToFirst()) {

			result = mCursor.getInt(mCursor.getColumnIndex(AdmobTable.KEY_ROWID));
		}

		mCursor.close();

		return result;
	}

	public void bulkInsertAdmobStats(List<AdmobStats> stats) {
		List<ContentValues> values = new ArrayList<ContentValues>();
		for (AdmobStats admob : stats) {
			ContentValues value = createAdmobContentValues(admob);
			values.add(value);
		}

		context.getContentResolver().bulkInsert(AdmobTable.CONTENT_URI,
				values.toArray(new ContentValues[values.size()]));
		backupManager.dataChanged();
	}

	private ContentValues createAdmobContentValues(AdmobStats admob) {
		ContentValues values = new ContentValues();

		values.put(AdmobTable.KEY_CLICKS, admob.getClicks());
		values.put(AdmobTable.KEY_CPC_REVENUE, admob.getCpcRevenue());
		values.put(AdmobTable.KEY_CPM_REVENUE, admob.getCpmRevenue());
		values.put(AdmobTable.KEY_CTR, admob.getCtr());
		values.put(AdmobTable.KEY_DATE, Utils.formatDbDate(admob.getDate()));
		values.put(AdmobTable.KEY_ECPM, admob.getEcpm());
		values.put(AdmobTable.KEY_EXCHANGE_DOWNLOADS, admob.getExchangeDownloads());
		values.put(AdmobTable.KEY_FILL_RATE, admob.getFillRate());
		values.put(AdmobTable.KEY_HOUSEAD_CLICKS, admob.getHouseAdClicks());
		values.put(AdmobTable.KEY_HOUSEAD_FILL_RATE, admob.getHouseadFillRate());
		values.put(AdmobTable.KEY_HOUSEAD_REQUESTS, admob.getHouseadRequests());
		values.put(AdmobTable.KEY_IMPRESSIONS, admob.getImpressions());
		values.put(AdmobTable.KEY_INTERSTITIAL_REQUESTS, admob.getInterstitialRequests());
		values.put(AdmobTable.KEY_OVERALL_FILL_RATE, admob.getOverallFillRate());
		values.put(AdmobTable.KEY_REQUESTS, admob.getRequests());
		values.put(AdmobTable.KEY_REVENUE, admob.getRevenue());
		values.put(AdmobTable.KEY_SITE_ID, admob.getSiteId());
		values.put(AdmobTable.KEY_CURRENCY, admob.getCurrencyCode());

		return values;
	}

	// ---insert a title into the database---
	public AppStatsDiff insertOrUpdateStats(AppInfo appInfo) {
		// do not insert draft apps
		if (appInfo == null || appInfo.isDraftOnly()) {
			return null;
		}

		// diff report
		AppStats previousStats = getLatestForApp(appInfo.getPackageName());

		insertOrUpdateApp(appInfo);

		// quickfix
		appInfo.setSkipNotification(getSkipNotification(appInfo.getPackageName()));

		AppStats downloadInfo = appInfo.getLatestStats();

		insertOrUpdateAppStats(downloadInfo, appInfo.getPackageName());

		return downloadInfo.createDiff(previousStats, appInfo);
	}

	public void insertOrUpdateAppStats(AppStats appStats, String packageName) {
		ContentValues values = new ContentValues();

		values.put(AppStatsTable.KEY_STATS_REQUESTDATE, Utils.formatDbDate(appStats.getDate()));
		values.put(AppStatsTable.KEY_STATS_PACKAGENAME, packageName);
		values.put(AppStatsTable.KEY_STATS_DOWNLOADS, appStats.getTotalDownloads());
		values.put(AppStatsTable.KEY_STATS_INSTALLS, appStats.getActiveInstalls());
		values.put(AppStatsTable.KEY_STATS_COMMENTS, appStats.getNumberOfComments());
		values.put(AppStatsTable.KEY_STATS_MARKETERANKING, -1);
		values.put(AppStatsTable.KEY_STATS_CATEGORYRANKING, -1);

		if (appStats.getRating5() != null) {
			values.put(AppStatsTable.KEY_STATS_5STARS, appStats.getRating5());
		}
		if (appStats.getRating4() != null) {
			values.put(AppStatsTable.KEY_STATS_4STARS, appStats.getRating4());
		}
		if (appStats.getRating3() != null) {
			values.put(AppStatsTable.KEY_STATS_3STARS, appStats.getRating3());
		}
		if (appStats.getRating2() != null) {
			values.put(AppStatsTable.KEY_STATS_2STARS, appStats.getRating2());
		}
		if (appStats.getRating1() != null) {
			values.put(AppStatsTable.KEY_STATS_1STARS, appStats.getRating1());
		}
		if (appStats.getVersionCode() != null) {
			values.put(AppStatsTable.KEY_STATS_VERSIONCODE, appStats.getVersionCode());
		}
		if (appStats.getNumberOfErrors() != null) {
			values.put(AppStatsTable.KEY_STATS_NUM_ERRORS, appStats.getNumberOfErrors());
		}
		if (appStats.getTotalRevenue() != null) {
			values.put(AppStatsTable.KEY_STATS_TOTAL_REVENUE, appStats.getTotalRevenue()
					.getAmount());
			values.put(AppStatsTable.KEY_STATS_CURRENCY, appStats.getTotalRevenue()
					.getCurrencyCode());
		}

		context.getContentResolver().insert(AppStatsTable.CONTENT_URI, values);

		backupManager.dataChanged();
	}

	private void insertOrUpdateApp(AppInfo appInfo) {
		// do not insert draft apps
		if (appInfo == null || appInfo.isDraftOnly()) {
			return;
		}

		ContentValues initialValues = new ContentValues();
		initialValues.put(AppInfoTable.KEY_APP_LASTUPDATE,
				Utils.formatDbDate(appInfo.getLastUpdate()));
		initialValues.put(AppInfoTable.KEY_APP_PACKAGENAME, appInfo.getPackageName());
		initialValues.put(AppInfoTable.KEY_APP_ACCOUNT, appInfo.getAccount());
		initialValues.put(AppInfoTable.KEY_APP_DEVELOPER_ID, appInfo.getDeveloperId());
		initialValues.put(AppInfoTable.KEY_APP_DEVELOPER_NAME, appInfo.getDeveloperName());
		initialValues.put(AppInfoTable.KEY_APP_NAME, appInfo.getName());
		initialValues.put(AppInfoTable.KEY_APP_ICONURL, appInfo.getIconUrl());
		initialValues.put(AppInfoTable.KEY_APP_PUBLISHSTATE, appInfo.getPublishState());
		initialValues.put(AppInfoTable.KEY_APP_CATEGORY, -1);
		initialValues.put(AppInfoTable.KEY_APP_VERSION_NAME, appInfo.getVersionName());

		Uri uri = context.getContentResolver().insert(AppInfoTable.CONTENT_URI, initialValues);
		long id = Long.parseLong(uri.getPathSegments().get(1));
		appInfo.setId(id);

		// XXX here?
		RevenueSummary revenue = appInfo.getTotalRevenueSummary();
		if (revenue != null) {
			ContentValues values = new ContentValues();
			values.put(RevenueSummaryTable.TYPE, revenue.getType().ordinal());
			values.put(RevenueSummaryTable.CURRENCY, revenue.getCurrency());
			values.put(RevenueSummaryTable.DATE, revenue.getDate().getTime());
			values.put(RevenueSummaryTable.LAST_DAY_TOTAL, revenue.getLastDay().getAmount());
			values.put(RevenueSummaryTable.LAST_7DAYS_TOTAL, revenue.getLast7Days().getAmount());
			values.put(RevenueSummaryTable.LAST_30DAYS_TOTAL, revenue.getLast30Days().getAmount());
			values.put(RevenueSummaryTable.OVERALL_TOTAL, revenue.getOverall().getAmount());
			values.put(RevenueSummaryTable.APPINFO_ID, appInfo.getId());

			Cursor c = null;
			try {
				c = context.getContentResolver().query(
						RevenueSummaryTable.CONTENT_URI,
						RevenueSummaryTable.ALL_COLUMNS,
						RevenueSummaryTable.APPINFO_ID + " =?  and " + RevenueSummaryTable.DATE
								+ " = ?",
						new String[] { Long.toString(appInfo.getId()),
								Long.toString(revenue.getDate().getTime()) },
						RevenueSummaryTable.DATE + " desc");
				if (!c.moveToNext()) {
					uri = context.getContentResolver().insert(RevenueSummaryTable.CONTENT_URI,
							values);
					id = Long.parseLong(uri.getPathSegments().get(1));
					revenue.setId(id);
				} else {
					long revenueId = c.getLong(c.getColumnIndex(RevenueSummaryTable.ROWID));
					context.getContentResolver().update(RevenueSummaryTable.CONTENT_URI, values,
							RevenueSummaryTable.ROWID + " = ?",
							new String[] { Long.toString(revenueId) });
				}
			} finally {
				if (c != null) {
					c.close();
				}
			}
		}

		backupManager.dataChanged();
	}

	public String getAppName(String packageName) {
		if (packageName == null) {
			return null;
		}

		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(AppInfoTable.CONTENT_URI,
					new String[] { AppInfoTable.KEY_APP_NAME },
					AppInfoTable.KEY_APP_PACKAGENAME + "='" + packageName + "'", null, null);

			if (cursor.moveToFirst()) {
				return cursor.getString(cursor.getColumnIndex(AppInfoTable.KEY_APP_NAME));
			}

			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	// XXX add dev ID filter
	public List<String> getPackagesForAccount(String account) {
		List<String> result = new ArrayList<String>();
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(AppInfoTable.UNIQUE_PACAKGES_CONTENT_URI,
					new String[] { AppInfoTable.KEY_APP_PACKAGENAME },
					AppInfoTable.KEY_APP_ACCOUNT + "=?", new String[] { account },
					AppInfoTable.KEY_APP_PACKAGENAME);
			while (cursor.moveToNext()) {
				result.add(cursor.getString(0));
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return result;
	}

	// XXX filter by dev ID
	public List<AppInfo> getAllAppsLatestStats(String account) {

		List<AppInfo> appInfos = new ArrayList<AppInfo>();

		Cursor cursor = null;
		try {
			cursor = context.getContentResolver()
					.query(AppInfoTable.CONTENT_URI,
							new String[] { AppInfoTable.KEY_ROWID,
									AppInfoTable.KEY_APP_VERSION_NAME,
									AppInfoTable.KEY_APP_PACKAGENAME,
									AppInfoTable.KEY_APP_LASTUPDATE, AppInfoTable.KEY_APP_NAME,
									AppInfoTable.KEY_APP_GHOST,
									AppInfoTable.KEY_APP_SKIP_NOTIFICATION,
									AppInfoTable.KEY_APP_RATINGS_EXPANDED,
									AppInfoTable.KEY_APP_ICONURL,
									AppInfoTable.KEY_APP_ADMOB_ACCOUNT,
									AppInfoTable.KEY_APP_ADMOB_SITE_ID,
									AppInfoTable.KEY_APP_ADMOB_AD_UNIT_ID,
									AppInfoTable.KEY_APP_LAST_COMMENTS_UPDATE,
									AppInfoTable.KEY_APP_DEVELOPER_ID,
									AppInfoTable.KEY_APP_DEVELOPER_NAME },
							AppInfoTable.KEY_APP_ACCOUNT + "='" + account + "'", null,
							AppInfoTable.KEY_APP_NAME + "");

			while (cursor.moveToNext()) {
				AppInfo appInfo = new AppInfo();
				appInfo.setId(cursor.getLong(cursor.getColumnIndex(AppInfoTable.KEY_ROWID)));
				appInfo.setAccount(account);
				appInfo.setLastUpdate(Utils.parseDbDate(cursor.getString(cursor
						.getColumnIndex(AppInfoTable.KEY_APP_LASTUPDATE))));
				appInfo.setPackageName(cursor.getString(cursor
						.getColumnIndex(AppInfoTable.KEY_APP_PACKAGENAME)));
				appInfo.setName(cursor.getString(cursor.getColumnIndex(AppInfoTable.KEY_APP_NAME)));
				appInfo.setGhost(cursor.getInt(cursor.getColumnIndex(AppInfoTable.KEY_APP_GHOST)) == 0 ? false
						: true);
				appInfo.setSkipNotification(cursor.getInt(cursor
						.getColumnIndex(AppInfoTable.KEY_APP_SKIP_NOTIFICATION)) == 0 ? false
						: true);
				appInfo.setRatingDetailsExpanded(cursor.getInt(cursor
						.getColumnIndex(AppInfoTable.KEY_APP_RATINGS_EXPANDED)) == 0 ? false : true);
				appInfo.setIconUrl(cursor.getString(cursor
						.getColumnIndex(AppInfoTable.KEY_APP_ICONURL)));
				appInfo.setVersionName(cursor.getString(cursor
						.getColumnIndex(AppInfoTable.KEY_APP_VERSION_NAME)));

				int idx = cursor.getColumnIndex(AppInfoTable.KEY_APP_ADMOB_ACCOUNT);
				if (!cursor.isNull(idx)) {
					appInfo.setAdmobAccount(cursor.getString(idx));
				}
				idx = cursor.getColumnIndex(AppInfoTable.KEY_APP_ADMOB_SITE_ID);
				if (!cursor.isNull(idx)) {
					appInfo.setAdmobSiteId(cursor.getString(idx));
				}
				idx = cursor.getColumnIndex(AppInfoTable.KEY_APP_ADMOB_AD_UNIT_ID);
				if (!cursor.isNull(idx)) {
					appInfo.setAdmobAdUnitId(cursor.getString(idx));
				}
				idx = cursor.getColumnIndex(AppInfoTable.KEY_APP_LAST_COMMENTS_UPDATE);
				if (!cursor.isNull(idx)) {
					appInfo.setLastCommentsUpdate(new Date(cursor.getLong(idx)));
				}
				idx = cursor.getColumnIndex(AppInfoTable.KEY_APP_DEVELOPER_ID);
				if (!cursor.isNull(idx)) {
					appInfo.setDeveloperId(cursor.getString(idx));
				}
				idx = cursor.getColumnIndex(AppInfoTable.KEY_APP_DEVELOPER_NAME);
				if (!cursor.isNull(idx)) {
					appInfo.setDeveloperName(cursor.getString(idx));
				}

				appInfos.add(appInfo);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		for (AppInfo appInfo : appInfos) {
			String packageName = appInfo.getPackageName();

			AppStatsSummary statsForApp = getStatsForApp(packageName, Timeframe.LAST_TWO_DAYS,
					false);

			AppStats stats = new AppStats();

			if (statsForApp.getStats().size() > 1) {
				stats = statsForApp.getStats().get(1);
			} else {
				if (statsForApp.getStats().size() > 0) {
					stats = statsForApp.getStats().get(0);

				}
			}

			stats.init();
			appInfo.setLatestStats(stats);

			//
			RevenueSummary revenueSummary = getRevenueSummaryForApp(appInfo);
			appInfo.setTotalRevenueSummary(revenueSummary);
		}

		return appInfos;
	}

	public long setGhost(String account, String packageName, boolean value) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(AppInfoTable.KEY_APP_GHOST, value == true ? 1 : 0);

		long result = -1;
		long exisitingId = getAppInfoByPackageName(packageName);
		if (exisitingId > -1) {

			context.getContentResolver().update(AppInfoTable.CONTENT_URI, initialValues,
					AppInfoTable.KEY_ROWID + "=" + exisitingId, null);
			result = exisitingId;

			backupManager.dataChanged();
		}

		return result;
	}

	public long setRatingExpanded(String account, String packageName, boolean value) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(AppInfoTable.KEY_APP_RATINGS_EXPANDED, value == true ? 1 : 0);

		long result = -1;
		long exisitingId = getAppInfoByPackageName(packageName);
		if (exisitingId > -1) {
			context.getContentResolver().update(AppInfoTable.CONTENT_URI, initialValues,
					AppInfoTable.KEY_ROWID + "=" + exisitingId, null);

			result = exisitingId;

			backupManager.dataChanged();
		}

		return result;
	}

	private long getAppInfoByPackageName(String packageName) {
		long result = -1;

		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(
					AppInfoTable.CONTENT_URI,
					new String[] { AppInfoTable.KEY_ROWID, AppInfoTable.KEY_APP_ACCOUNT,
							AppInfoTable.KEY_APP_PACKAGENAME },
					AppInfoTable.KEY_APP_PACKAGENAME + "='" + packageName + "'", null, null);
			if (cursor != null && cursor.moveToFirst()) {
				result = cursor.getInt(cursor.getColumnIndex(AppInfoTable.KEY_ROWID));
			}

			return result;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private boolean getSkipNotification(String packageName) {
		boolean result = false;

		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(
					AppInfoTable.CONTENT_URI,
					new String[] { AppInfoTable.KEY_APP_SKIP_NOTIFICATION,
							AppInfoTable.KEY_APP_ACCOUNT, AppInfoTable.KEY_APP_PACKAGENAME },
					AppInfoTable.KEY_APP_PACKAGENAME + "='" + packageName + "'", null, null);
			if (cursor != null && cursor.moveToFirst()) {
				result = cursor.getInt(cursor
						.getColumnIndex(AppInfoTable.KEY_APP_SKIP_NOTIFICATION)) == 0 ? false
						: true;
			}

			return result;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@SuppressLint("SimpleDateFormat")
	public AppStatsSummary getStatsForApp(String packageName, Timeframe currentTimeFrame,
			Boolean smoothEnabled) {
		AppStatsSummary result = new AppStatsSummary();

		int limit = Integer.MAX_VALUE;
		if (currentTimeFrame.equals(Timeframe.LAST_NINETY_DAYS)) {
			limit = 90;
		} else if (currentTimeFrame.equals(Timeframe.LAST_THIRTY_DAYS)) {
			limit = 30;
		} else if (currentTimeFrame.equals(Timeframe.LAST_SEVEN_DAYS)) {
			limit = 7;
		} else if (currentTimeFrame.equals(Timeframe.LAST_TWO_DAYS)) {
			limit = 2;
		} else if (currentTimeFrame.equals(Timeframe.LATEST_VALUE)) {
			limit = 1;
		} else if (currentTimeFrame.equals(Timeframe.MONTH_TO_DATE)) {
			limit = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		}

		Cursor cursor = null;
		try {
			cursor = context.getContentResolver()
					.query(AppStatsTable.CONTENT_URI,
							new String[] { AppStatsTable.KEY_ROWID,
									AppStatsTable.KEY_STATS_PACKAGENAME,
									AppStatsTable.KEY_STATS_DOWNLOADS,
									AppStatsTable.KEY_STATS_INSTALLS,
									AppStatsTable.KEY_STATS_COMMENTS,
									AppStatsTable.KEY_STATS_MARKETERANKING,
									AppStatsTable.KEY_STATS_CATEGORYRANKING,
									AppStatsTable.KEY_STATS_5STARS, AppStatsTable.KEY_STATS_4STARS,
									AppStatsTable.KEY_STATS_3STARS, AppStatsTable.KEY_STATS_2STARS,
									AppStatsTable.KEY_STATS_1STARS,
									AppStatsTable.KEY_STATS_REQUESTDATE,
									AppStatsTable.KEY_STATS_VERSIONCODE,
									AppStatsTable.KEY_STATS_NUM_ERRORS,
									AppStatsTable.KEY_STATS_TOTAL_REVENUE,
									AppStatsTable.KEY_STATS_CURRENCY },
							AppStatsTable.KEY_STATS_PACKAGENAME + "='" + packageName + "'", null,
							AppStatsTable.KEY_STATS_REQUESTDATE + " desc LIMIT " + limit + ""); // sort
																								// order
																								// ->
																								// new
																								// to
																								// old

			if (cursor.moveToFirst()) {
				do {
					AppStats stats = new AppStats();
					stats.setActiveInstalls(cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_INSTALLS)));
					stats.setTotalDownloads(cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_DOWNLOADS)));

					String dateString = cursor.getString(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_REQUESTDATE));
					stats.setDate(Utils.parseDbDate(dateString.substring(0, 10) + " 12:00:00"));

					stats.setNumberOfComments(cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_COMMENTS)));
					stats.setVersionCode(cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_VERSIONCODE)));

					stats.setRating(
							cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_1STARS)),
							cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_2STARS)),
							cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_3STARS)),
							cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_4STARS)),
							cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_5STARS)));

					int idx = cursor.getColumnIndex(AppStatsTable.KEY_STATS_NUM_ERRORS);
					if (!cursor.isNull(idx)) {
						stats.setNumberOfErrors(cursor.getInt(idx));
					}
					idx = cursor.getColumnIndex(AppStatsTable.KEY_STATS_TOTAL_REVENUE);
					if (!cursor.isNull(idx)) {
						double amount = cursor.getDouble(idx);
						String currency = cursor.getString(cursor
								.getColumnIndex(AppStatsTable.KEY_STATS_CURRENCY));
						stats.setTotalRevenue(new Revenue(Revenue.Type.TOTAL, amount, currency));
					}

					result.addStat(stats);

				} while (cursor.moveToNext());

			}

			cursor.close();

			result.calculateOverallStats(limit, smoothEnabled);

			return result;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public AppStats getLatestForApp(String packageName) {
		AppStats stats = null;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver()
					.query(AppStatsTable.CONTENT_URI,
							new String[] { AppStatsTable.KEY_ROWID,
									AppStatsTable.KEY_STATS_PACKAGENAME,
									AppStatsTable.KEY_STATS_DOWNLOADS,
									AppStatsTable.KEY_STATS_VERSIONCODE,
									AppStatsTable.KEY_STATS_INSTALLS,
									AppStatsTable.KEY_STATS_COMMENTS,
									AppStatsTable.KEY_STATS_MARKETERANKING,
									AppStatsTable.KEY_STATS_CATEGORYRANKING,
									AppStatsTable.KEY_STATS_5STARS, AppStatsTable.KEY_STATS_4STARS,
									AppStatsTable.KEY_STATS_3STARS, AppStatsTable.KEY_STATS_2STARS,
									AppStatsTable.KEY_STATS_1STARS,
									AppStatsTable.KEY_STATS_REQUESTDATE,
									AppStatsTable.KEY_STATS_TOTAL_REVENUE,
									AppStatsTable.KEY_STATS_CURRENCY },
							AppStatsTable.KEY_STATS_PACKAGENAME + " = ?",
							new String[] { packageName },
							AppStatsTable.KEY_STATS_REQUESTDATE + " desc limit 1");

			if (cursor.moveToFirst()) {
				do {
					stats = new AppStats();
					stats.setActiveInstalls(cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_INSTALLS)));
					stats.setTotalDownloads(cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_DOWNLOADS)));
					stats.setDate(Utils.parseDbDate(cursor.getString(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_REQUESTDATE))));
					stats.setNumberOfComments(cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_COMMENTS)));
					stats.setVersionCode(cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_VERSIONCODE)));

					stats.setRating(
							cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_1STARS)),
							cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_2STARS)),
							cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_3STARS)),
							cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_4STARS)),
							cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_5STARS)));
					int idx = cursor.getColumnIndex(AppStatsTable.KEY_STATS_TOTAL_REVENUE);
					if (!cursor.isNull(idx)) {
						double amount = cursor.getDouble(idx);
						String currency = cursor.getString(cursor
								.getColumnIndex(AppStatsTable.KEY_STATS_CURRENCY));
						stats.setTotalRevenue(new Revenue(Revenue.Type.TOTAL, amount, currency));
					}

					stats.init();

				} while (cursor.moveToNext());

			}

			return stats;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public RevenueSummary getRevenueSummaryForApp(AppInfo app) {
		Cursor cursor = null;

		try {
			// limit clause not supported, so we might get quite a few results
			cursor = context.getContentResolver()
					.query(RevenueSummaryTable.CONTENT_URI, RevenueSummaryTable.ALL_COLUMNS,
							RevenueSummaryTable.APPINFO_ID + "=?",
							new String[] { Long.toString(app.getId()) },
							RevenueSummaryTable.DATE + " desc");

			if (cursor.getCount() == 0) {
				return null;
			}

			if (!cursor.moveToFirst()) {
				return null;
			}

			int typeIdx = cursor.getInt(cursor.getColumnIndex(RevenueSummaryTable.TYPE));
			String currency = cursor.getString(cursor.getColumnIndex(RevenueSummaryTable.CURRENCY));
			// XXX hack -- make sure date is not null
			Date date = Utils.parseDbDate("2013-01-01 00:00:00");
			int idx = cursor.getColumnIndex(RevenueSummaryTable.DATE);
			if (!cursor.isNull(idx)) {
				date = new Date(cursor.getLong(idx));
			}
			double lastDay = cursor.getDouble(cursor
					.getColumnIndex(RevenueSummaryTable.LAST_DAY_TOTAL));
			double last7Days = cursor.getDouble(cursor
					.getColumnIndex(RevenueSummaryTable.LAST_7DAYS_TOTAL));
			double last30Days = cursor.getDouble(cursor
					.getColumnIndex(RevenueSummaryTable.LAST_30DAYS_TOTAL));
			double overall = cursor.getDouble(cursor
					.getColumnIndex(RevenueSummaryTable.OVERALL_TOTAL));
			Revenue.Type type = Revenue.Type.values()[typeIdx];
			RevenueSummary result = new RevenueSummary(type, currency, date, lastDay, last7Days,
					last30Days, overall);

			return result;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@SuppressLint("SimpleDateFormat")
	public Map<Date, Map<Integer, Integer>> getDailyRatings(Date maxDate, String packagename) {
		Map<Date, Map<Integer, Integer>> result = new TreeMap<Date, Map<Integer, Integer>>(
				new Comparator<Date>() {
					// reverse order
					@Override
					public int compare(Date object1, Date object2) {
						return object2.compareTo(object1);
					}
				});

		// make sure there is only one entry / day
		SimpleDateFormat dateFormatEnd = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
		SimpleDateFormat dateFormatStart = new SimpleDateFormat("yyyy-MM-dd 23:59:59");

		String queryString = AppStatsTable.KEY_STATS_PACKAGENAME + "='" + packagename + "'";

		if (maxDate != null) {
			queryString += " and " + AppStatsTable.KEY_STATS_REQUESTDATE + " BETWEEN '"
					+ dateFormatStart.format(new Date()) + "' and '"
					+ dateFormatEnd.format(maxDate) + "'";
		}

		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(
					AppStatsTable.CONTENT_URI,
					new String[] { AppStatsTable.KEY_ROWID, AppStatsTable.KEY_STATS_PACKAGENAME,
							AppStatsTable.KEY_STATS_DOWNLOADS, AppStatsTable.KEY_STATS_INSTALLS,
							AppStatsTable.KEY_STATS_COMMENTS,
							AppStatsTable.KEY_STATS_MARKETERANKING,
							AppStatsTable.KEY_STATS_CATEGORYRANKING,
							AppStatsTable.KEY_STATS_VERSIONCODE, AppStatsTable.KEY_STATS_5STARS,
							AppStatsTable.KEY_STATS_4STARS, AppStatsTable.KEY_STATS_3STARS,
							AppStatsTable.KEY_STATS_2STARS, AppStatsTable.KEY_STATS_1STARS,
							AppStatsTable.KEY_STATS_REQUESTDATE }, queryString, null,
					AppStatsTable.KEY_STATS_REQUESTDATE + " asc");

			Integer prev1 = null;
			Integer prev2 = null;
			Integer prev3 = null;
			Integer prev4 = null;
			Integer prev5 = null;

			if (cursor != null && cursor.moveToFirst()) {
				do {
					Map<Integer, Integer> ratings = new TreeMap<Integer, Integer>();

					int value1 = cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_1STARS));
					ratings.put(1, getDiff(prev1, value1));
					prev1 = value1;

					int value2 = cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_1STARS));
					ratings.put(2, getDiff(prev2, value2));
					prev2 = value2;

					int value3 = cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_3STARS));
					ratings.put(3, getDiff(prev3, value3));
					prev3 = value3;

					int value4 = cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_4STARS));
					ratings.put(4, getDiff(prev4, value4));
					prev4 = value4;

					int value5 = cursor.getInt(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_5STARS));
					ratings.put(5, getDiff(prev5, value5));
					prev5 = value5;

					String dateString = cursor.getString(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_REQUESTDATE));
					Date date = Utils.parseDbDate(dateString.substring(0, 10) + " 00:00:00");

					result.put(date, ratings);

				} while (cursor.moveToNext());
			}

			return result;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private Integer getDiff(Integer prev1, int value1) {
		if (prev1 == null) {
			return 0;
		} else {
			return value1 - prev1;
		}
	}

	public void updateCommentsCache(List<Comment> comments, String packageName) {
		// TODO Do not drop the table each time

		// clear table
		context.getContentResolver().delete(CommentsTable.CONTENT_URI,
				CommentsTable.KEY_COMMENT_PACKAGENAME + "='" + packageName + "'", null);

		// insert new values
		for (Comment comment : comments) {
			ContentValues initialValues = new ContentValues();
			initialValues.put(CommentsTable.KEY_COMMENT_PACKAGENAME, packageName);
			initialValues
					.put(CommentsTable.KEY_COMMENT_DATE, Utils.formatDbDate(comment.getDate()));
			initialValues.put(CommentsTable.KEY_COMMENT_RATING, comment.getRating());
			initialValues.put(CommentsTable.KEY_COMMENT_TITLE, comment.getTitle());
			initialValues.put(CommentsTable.KEY_COMMENT_TEXT, comment.getText());
			initialValues.put(CommentsTable.KEY_COMMENT_USER, comment.getUser());
			initialValues.put(CommentsTable.KEY_COMMENT_APP_VERSION, comment.getAppVersion());
			initialValues.put(CommentsTable.KEY_COMMENT_DEVICE, comment.getDevice());
			initialValues.put(CommentsTable.KEY_COMMENT_DEVICE_API_LEVEL,comment.getAndroidAPILevel());
			Comment reply = comment.getReply();
			String replyText = null;
			String replyDate = null;
			if (reply != null) {
				replyText = reply.getText();
				replyDate = Utils.formatDbDate(reply.getDate());
			}
			initialValues.put(CommentsTable.KEY_COMMENT_REPLY_TEXT, replyText);
			initialValues.put(CommentsTable.KEY_COMMENT_REPLY_DATE, replyDate);
			initialValues.put(CommentsTable.KEY_COMMENT_LANGUAGE, comment.getLanguage());
			initialValues.put(CommentsTable.KEY_COMMENT_ORIGINAL_TITLE, comment.getOriginalTitle());
			initialValues.put(CommentsTable.KEY_COMMENT_ORIGINAL_TEXT, comment.getOriginalText());
			initialValues.put(CommentsTable.KEY_COMMENT_UNIQUE_ID, comment.getUniqueId());

			context.getContentResolver().insert(CommentsTable.CONTENT_URI, initialValues);

		}

		backupManager.dataChanged();
	}

	public List<Comment> getCommentsFromCache(String packageName) {
		List<Comment> result = new ArrayList<Comment>();

		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(
					CommentsTable.CONTENT_URI,
					new String[] { CommentsTable.KEY_COMMENT_DATE,
							CommentsTable.KEY_COMMENT_PACKAGENAME,
							CommentsTable.KEY_COMMENT_RATING,
							CommentsTable.KEY_COMMENT_TITLE,
							CommentsTable.KEY_COMMENT_TEXT,
							CommentsTable.KEY_COMMENT_USER,
							CommentsTable.KEY_COMMENT_DEVICE,
							CommentsTable.KEY_COMMENT_APP_VERSION,
							CommentsTable.KEY_COMMENT_REPLY_TEXT,
							CommentsTable.KEY_COMMENT_REPLY_DATE,
							CommentsTable.KEY_COMMENT_LANGUAGE,
							CommentsTable.KEY_COMMENT_ORIGINAL_TITLE,
							CommentsTable.KEY_COMMENT_ORIGINAL_TEXT,
							CommentsTable.KEY_COMMENT_UNIQUE_ID,
					        CommentsTable.KEY_COMMENT_DEVICE_API_LEVEL},
					AppInfoTable.KEY_APP_PACKAGENAME + " = ?", new String[] { packageName },
					CommentsTable.KEY_COMMENT_DATE + " desc");
			if (cursor == null) {
				return result;
			}

			while (cursor.moveToNext()) {
				Comment comment = new Comment();
				String dateString = cursor.getString(cursor
						.getColumnIndex(CommentsTable.KEY_COMMENT_DATE));
				comment.setDate(Utils.parseDbDate(dateString));
				comment.setUser(cursor.getString(cursor
						.getColumnIndex(CommentsTable.KEY_COMMENT_USER)));
				comment.setTitle(cursor.getString(cursor
						.getColumnIndex(CommentsTable.KEY_COMMENT_TITLE)));
				comment.setText(cursor.getString(cursor
						.getColumnIndex(CommentsTable.KEY_COMMENT_TEXT)));
				comment.setDevice(cursor.getString(cursor
						.getColumnIndex(CommentsTable.KEY_COMMENT_DEVICE)));
				comment.setAppVersion(cursor.getString(cursor
						.getColumnIndex(CommentsTable.KEY_COMMENT_APP_VERSION)));
				comment.setRating(cursor.getInt(cursor
						.getColumnIndex(CommentsTable.KEY_COMMENT_RATING)));
				comment.setAndroidAPILevel(cursor.getString(cursor.getColumnIndex(CommentsTable.KEY_COMMENT_DEVICE_API_LEVEL)));
				String replyText = cursor.getString(cursor
						.getColumnIndex(CommentsTable.KEY_COMMENT_REPLY_TEXT));

				if (replyText != null) {
					Comment reply = new Comment(true);
					reply.setText(replyText);
					reply.setDate(Utils.parseDbDate(cursor.getString(cursor
							.getColumnIndex(CommentsTable.KEY_COMMENT_REPLY_DATE))));
					reply.setOriginalCommentDate(comment.getDate());
					comment.setReply(reply);
				}
				int idx = cursor.getColumnIndex(CommentsTable.KEY_COMMENT_LANGUAGE);
				if (!cursor.isNull(idx)) {
					comment.setLanguage(cursor.getString(idx));
				}
				idx = cursor.getColumnIndex(CommentsTable.KEY_COMMENT_ORIGINAL_TITLE);
				if (!cursor.isNull(idx)) {
					comment.setOriginalTitle(cursor.getString(idx));
				}
				idx = cursor.getColumnIndex(CommentsTable.KEY_COMMENT_ORIGINAL_TEXT);
				if (!cursor.isNull(idx)) {
					comment.setOriginalText(cursor.getString(idx));
				}
				idx = cursor.getColumnIndex(CommentsTable.KEY_COMMENT_UNIQUE_ID);
				if (!cursor.isNull(idx)) {
					comment.setUniqueId(cursor.getString(idx));
				}

				result.add(comment);
			}
			if (cursor != null) {
				cursor.close();
			}
			return result;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public long setSkipNotification(String packageName, boolean value) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(AppInfoTable.KEY_APP_SKIP_NOTIFICATION, value == true ? 1 : 0);

		long result = -1;
		long exisitingId = getAppInfoByPackageName(packageName);
		if (exisitingId > -1) {
			context.getContentResolver().update(AppInfoTable.CONTENT_URI, initialValues,
					AppInfoTable.KEY_ROWID + "=" + exisitingId, null);
			result = exisitingId;
		}

		return result;
	}

}
