package com.github.andlyticsproject;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.backup.BackupManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.db.AdmobTable;
import com.github.andlyticsproject.db.AndlyticsContentProvider;
import com.github.andlyticsproject.db.AppInfoTable;
import com.github.andlyticsproject.db.AppStatsTable;
import com.github.andlyticsproject.db.CommentsTable;
import com.github.andlyticsproject.model.Admob;
import com.github.andlyticsproject.model.AdmobList;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.AppStatsList;
import com.github.andlyticsproject.model.Comment;
import com.github.andlyticsproject.util.Utils;

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

	public AdmobList getAdmobStats(String siteId, Timeframe currentTimeFrame) {

		AdmobList admobList = new AdmobList();

		Admob overall = new Admob();

		List<Admob> result = new ArrayList<Admob>();

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

		Cursor cursor = context.getContentResolver().query(
				AdmobTable.CONTENT_URI,
				new String[] {

				AdmobTable.KEY_ROWID, AdmobTable.KEY_SITE_ID, AdmobTable.KEY_REQUESTS,
						AdmobTable.KEY_HOUSEAD_REQUESTS, AdmobTable.KEY_INTERSTITIAL_REQUESTS,
						AdmobTable.KEY_IMPRESSIONS, AdmobTable.KEY_FILL_RATE,
						AdmobTable.KEY_HOUSEAD_FILL_RATE, AdmobTable.KEY_OVERALL_FILL_RATE,
						AdmobTable.KEY_CLICKS, AdmobTable.KEY_HOUSEAD_CLICKS, AdmobTable.KEY_CTR,
						AdmobTable.KEY_ECPM, AdmobTable.KEY_REVENUE, AdmobTable.KEY_CPC_REVENUE,
						AdmobTable.KEY_CPM_REVENUE, AdmobTable.KEY_EXCHANGE_DOWNLOADS,
						AdmobTable.KEY_DATE

				}, AdmobTable.KEY_SITE_ID + "='" + siteId + "'", null,
				AdmobTable.KEY_DATE + " desc LIMIT " + limit + ""); // sort
																	// order ->
																	// new to
																	// old

		int count = 0;

		if (cursor.moveToFirst()) {

			do {

				Admob admob = new Admob();

				admob.setSiteId(cursor.getString(cursor.getColumnIndex(AdmobTable.KEY_ROWID)));
				admob.setClicks(cursor.getInt(cursor.getColumnIndex(AdmobTable.KEY_CLICKS)));
				admob.setCpcRevenue(cursor.getFloat(cursor
						.getColumnIndex(AdmobTable.KEY_CPC_REVENUE)));
				admob.setCpmRevenue(cursor.getFloat(cursor
						.getColumnIndex(AdmobTable.KEY_CPM_REVENUE)));
				admob.setCtr(cursor.getFloat(cursor.getColumnIndex(AdmobTable.KEY_CTR)));
				admob.setEcpm(cursor.getFloat(cursor.getColumnIndex(AdmobTable.KEY_ECPM)));
				admob.setExchangeDownloads(cursor.getInt(cursor
						.getColumnIndex(AdmobTable.KEY_EXCHANGE_DOWNLOADS)));
				admob.setFillRate(cursor.getFloat(cursor.getColumnIndex(AdmobTable.KEY_FILL_RATE)));
				admob.setHouseAdClicks(cursor.getInt(cursor
						.getColumnIndex(AdmobTable.KEY_HOUSEAD_CLICKS)));
				admob.setHouseadFillRate(cursor.getFloat(cursor
						.getColumnIndex(AdmobTable.KEY_HOUSEAD_FILL_RATE)));
				admob.setHouseadRequests(cursor.getInt(cursor
						.getColumnIndex(AdmobTable.KEY_HOUSEAD_REQUESTS)));
				admob.setImpressions(cursor.getInt(cursor
						.getColumnIndex(AdmobTable.KEY_IMPRESSIONS)));
				admob.setInterstitialRequests(cursor.getInt(cursor
						.getColumnIndex(AdmobTable.KEY_INTERSTITIAL_REQUESTS)));
				admob.setOverallFillRate(cursor.getFloat(cursor
						.getColumnIndex(AdmobTable.KEY_OVERALL_FILL_RATE)));
				admob.setRequests(cursor.getInt(cursor.getColumnIndex(AdmobTable.KEY_REQUESTS)));
				admob.setRevenue(cursor.getFloat(cursor.getColumnIndex(AdmobTable.KEY_REVENUE)));
				String dateString = cursor.getString(cursor.getColumnIndex(AdmobTable.KEY_DATE));
				admob.setDate(Utils.parseDbDate(dateString.substring(0, 10) + " 12:00:00"));

				overall.setClicks(overall.getClicks() + admob.getClicks());
				overall.setCpcRevenue(overall.getCpcRevenue() + admob.getCpcRevenue());
				overall.setCpmRevenue(overall.getCpmRevenue() + admob.getCpmRevenue());
				overall.setCtr(overall.getCtr() + admob.getCtr());
				overall.setEcpm(overall.getEcpm() + admob.getEcpm());
				overall.setExchangeDownloads(overall.getExchangeDownloads()
						+ admob.getExchangeDownloads());
				overall.setFillRate(overall.getFillRate() + admob.getFillRate());
				overall.setHouseAdClicks(overall.getHouseAdClicks() + admob.getHouseAdClicks());
				overall.setHouseadFillRate(overall.getHouseadFillRate()
						+ admob.getHouseadFillRate());
				overall.setHouseadRequests(overall.getHouseadRequests()
						+ admob.getHouseadRequests());
				overall.setImpressions(overall.getImpressions() + admob.getImpressions());
				overall.setInterstitialRequests(overall.getInterstitialRequests()
						+ admob.getInterstitialRequests());
				overall.setOverallFillRate(overall.getOverallFillRate()
						+ admob.getOverallFillRate());
				overall.setRequests(overall.getRequests() + admob.getRequests());
				overall.setRevenue(overall.getRevenue() + admob.getRevenue());
				count++;
				result.add(admob);

			} while (cursor.moveToNext());

		}
		cursor.close();

		Collections.reverse(result);

		if (count > 0) {
			overall.setCtr(overall.getCtr() / count);
			overall.setFillRate(overall.getFillRate() / count);
			overall.setEcpm(overall.getEcpm() / count);
		}

		admobList.setOverallStats(overall);
		admobList.setAdmobs(result);

		return admobList;
	}

	public void insertOrUpdateAdmobStats(Admob admob) {

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

	public void bulkInsertAdmobStats(List<Admob> stats) {

		List<ContentValues> values = new ArrayList<ContentValues>();
		for (Admob admob : stats) {
			ContentValues value = createAdmobContentValues(admob);
			values.add(value);
		}

		context.getContentResolver().bulkInsert(AdmobTable.CONTENT_URI,
				values.toArray(new ContentValues[values.size()]));
		backupManager.dataChanged();
	}

	private ContentValues createAdmobContentValues(Admob admob) {

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

		String packageName = appInfo.getPackageName();

		insertOrUpdateAppStats(downloadInfo, packageName);

		return createAppStatsDiff(downloadInfo, previousStats, appInfo);
	}

	public void insertOrUpdateAppStats(AppStats downloadInfo, String packageName) {

		ContentValues values = new ContentValues();

		values.put(AppStatsTable.KEY_STATS_REQUESTDATE,
				Utils.formatDbDate(downloadInfo.getRequestDate()));
		values.put(AppStatsTable.KEY_STATS_PACKAGENAME, packageName);
		values.put(AppStatsTable.KEY_STATS_DOWNLOADS, downloadInfo.getTotalDownloads());
		values.put(AppStatsTable.KEY_STATS_INSTALLS, downloadInfo.getActiveInstalls());
		values.put(AppStatsTable.KEY_STATS_COMMENTS, downloadInfo.getNumberOfComments());
		values.put(AppStatsTable.KEY_STATS_MARKETERANKING, -1);
		values.put(AppStatsTable.KEY_STATS_CATEGORYRANKING, -1);
		values.put(AppStatsTable.KEY_STATS_5STARS, downloadInfo.getRating5());
		values.put(AppStatsTable.KEY_STATS_4STARS, downloadInfo.getRating4());
		values.put(AppStatsTable.KEY_STATS_3STARS, downloadInfo.getRating3());
		values.put(AppStatsTable.KEY_STATS_2STARS, downloadInfo.getRating2());
		values.put(AppStatsTable.KEY_STATS_1STARS, downloadInfo.getRating1());
		values.put(AppStatsTable.KEY_STATS_VERSIONCODE, downloadInfo.getVersionCode());
		values.put(AppStatsTable.KEY_STATS_NUM_ERRORS, downloadInfo.getNumberOfErrors());

		context.getContentResolver().insert(AppStatsTable.CONTENT_URI, values);

		backupManager.dataChanged();
	}

	private AppStatsDiff createAppStatsDiff(AppStats newStats, AppStats previousStats,
			AppInfo appInfo) {

		AppStatsDiff diff = new AppStatsDiff();
		diff.setAppName(appInfo.getName());
		diff.setPackageName(appInfo.getPackageName());
		diff.setIconName(appInfo.getIconName());
		diff.setSkipNotification(appInfo.isSkipNotification());
		diff.setVersionName(appInfo.getVersionName());

		if (previousStats != null) {

			newStats.init();
			previousStats.init();

			diff.setActiveInstallsChange(newStats.getActiveInstalls()
					- previousStats.getActiveInstalls());
			diff.setDownloadsChange(newStats.getTotalDownloads()
					- previousStats.getTotalDownloads());
			diff.setCommentsChange(newStats.getNumberOfComments()
					- previousStats.getNumberOfComments());
			diff.setAvgRatingChange(newStats.getAvgRating() - previousStats.getAvgRating());
			diff.setRating1Change(newStats.getRating1() - previousStats.getRating1());
			diff.setRating2Change(newStats.getRating2() - previousStats.getRating2());
			diff.setRating3Change(newStats.getRating3() - previousStats.getRating3());
			diff.setRating4Change(newStats.getRating4() - previousStats.getRating4());
			diff.setRating5Change(newStats.getRating5() - previousStats.getRating5());

		}

		return diff;
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
		initialValues.put(AppInfoTable.KEY_APP_NAME, appInfo.getName());
		initialValues.put(AppInfoTable.KEY_APP_ICONURL, appInfo.getIconUrl());
		initialValues.put(AppInfoTable.KEY_APP_PUBLISHSTATE, appInfo.getPublishState());
		initialValues.put(AppInfoTable.KEY_APP_CATEGORY, -1);
		initialValues.put(AppInfoTable.KEY_APP_VERSION_NAME, appInfo.getVersionName());

		Uri uri = context.getContentResolver().insert(AppInfoTable.CONTENT_URI, initialValues);
		long id = Long.parseLong(uri.getPathSegments().get(1));
		appInfo.setId(id);

		backupManager.dataChanged();
	}

	public String getAppName(String packageName) {
		if (packageName == null) {
			return null;
		}
		String appName = null;

		Cursor cursor = context.getContentResolver().query(AppInfoTable.CONTENT_URI,
				new String[] { AppInfoTable.KEY_APP_NAME },
				AppInfoTable.KEY_APP_PACKAGENAME + "='" + packageName + "'", null, null);

		if (cursor.moveToFirst()) {
			appName = cursor.getString(cursor.getColumnIndex(AppInfoTable.KEY_APP_NAME));
		}
		cursor.close();

		return appName;
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

		Cursor cursor = context.getContentResolver().query(
				AppInfoTable.CONTENT_URI,
				new String[] { AppInfoTable.KEY_ROWID, AppInfoTable.KEY_APP_VERSION_NAME,
						AppInfoTable.KEY_APP_PACKAGENAME, AppInfoTable.KEY_APP_LASTUPDATE,
						AppInfoTable.KEY_APP_NAME, AppInfoTable.KEY_APP_GHOST,
						AppInfoTable.KEY_APP_SKIP_NOTIFICATION,
						AppInfoTable.KEY_APP_RATINGS_EXPANDED, AppInfoTable.KEY_APP_ICONURL,
						AppInfoTable.KEY_APP_ADMOB_ACCOUNT, AppInfoTable.KEY_APP_ADMOB_SITE_ID,
						AppInfoTable.KEY_APP_LAST_COMMENTS_UPDATE,
						AppInfoTable.KEY_APP_DEVELOPER_ID },
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
					.getColumnIndex(AppInfoTable.KEY_APP_SKIP_NOTIFICATION)) == 0 ? false : true);
			appInfo.setRatingDetailsExpanded(cursor.getInt(cursor
					.getColumnIndex(AppInfoTable.KEY_APP_RATINGS_EXPANDED)) == 0 ? false : true);
			appInfo.setIconUrl(cursor.getString(cursor.getColumnIndex(AppInfoTable.KEY_APP_ICONURL)));
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
			idx = cursor.getColumnIndex(AppInfoTable.KEY_APP_LAST_COMMENTS_UPDATE);
			if (!cursor.isNull(idx)) {
				appInfo.setLastCommentsUpdate(new Date(cursor.getLong(idx)));
			}
			idx = cursor.getColumnIndex(AppInfoTable.KEY_APP_DEVELOPER_ID);
			if (!cursor.isNull(idx)) {
				appInfo.setDeveloperId(cursor.getString(idx));
			}

			appInfos.add(appInfo);
		}
		cursor.close();

		for (AppInfo appInfo : appInfos) {

			String packageName = appInfo.getPackageName();

			AppStatsList statsForApp = getStatsForApp(packageName, Timeframe.LAST_TWO_DAYS, false);

			AppStats stats = new AppStats();

			if (statsForApp.getAppStats().size() > 1) {
				stats = statsForApp.getAppStats().get(1);
			} else {

				if (statsForApp.getAppStats().size() > 0) {
					stats = statsForApp.getAppStats().get(0);

				}
			}

			stats.init();

			appInfo.setLatestStats(stats);
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
		Cursor mCursor = context.getContentResolver().query(
				AppInfoTable.CONTENT_URI,
				new String[] { AppInfoTable.KEY_ROWID, AppInfoTable.KEY_APP_ACCOUNT,
						AppInfoTable.KEY_APP_PACKAGENAME },
				AppInfoTable.KEY_APP_PACKAGENAME + "='" + packageName + "'", null, null);
		if (mCursor != null && mCursor.moveToFirst()) {

			result = mCursor.getInt(mCursor.getColumnIndex(AppInfoTable.KEY_ROWID));
		}

		mCursor.close();

		return result;
	}

	private boolean getSkipNotification(String packageName) {

		boolean result = false;
		Cursor mCursor = context.getContentResolver().query(
				AppInfoTable.CONTENT_URI,
				new String[] { AppInfoTable.KEY_APP_SKIP_NOTIFICATION,
						AppInfoTable.KEY_APP_ACCOUNT, AppInfoTable.KEY_APP_PACKAGENAME },
				AppInfoTable.KEY_APP_PACKAGENAME + "='" + packageName + "'", null, null);
		if (mCursor != null && mCursor.moveToFirst()) {

			result = mCursor.getInt(mCursor.getColumnIndex(AppInfoTable.KEY_APP_SKIP_NOTIFICATION)) == 0 ? false
					: true;
		}

		mCursor.close();

		return result;
	}

	@SuppressLint("SimpleDateFormat")
	public AppStatsList getStatsForApp(String packageName, Timeframe currentTimeFrame,
			Boolean smoothEnabled) {

		int heighestRatingChange = 0;
		int lowestRatingChange = 0;

		List<AppStats> result = new ArrayList<AppStats>();

		AppStats overall = new AppStats();

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

		Cursor cursor = context.getContentResolver().query(
				AppStatsTable.CONTENT_URI,
				new String[] { AppStatsTable.KEY_ROWID, AppStatsTable.KEY_STATS_PACKAGENAME,
						AppStatsTable.KEY_STATS_DOWNLOADS, AppStatsTable.KEY_STATS_INSTALLS,
						AppStatsTable.KEY_STATS_COMMENTS, AppStatsTable.KEY_STATS_MARKETERANKING,
						AppStatsTable.KEY_STATS_CATEGORYRANKING, AppStatsTable.KEY_STATS_5STARS,
						AppStatsTable.KEY_STATS_4STARS, AppStatsTable.KEY_STATS_3STARS,
						AppStatsTable.KEY_STATS_2STARS, AppStatsTable.KEY_STATS_1STARS,
						AppStatsTable.KEY_STATS_REQUESTDATE, AppStatsTable.KEY_STATS_VERSIONCODE,
						AppStatsTable.KEY_STATS_NUM_ERRORS },
				AppStatsTable.KEY_STATS_PACKAGENAME + "='" + packageName + "'", null,
				AppStatsTable.KEY_STATS_REQUESTDATE + " desc LIMIT " + limit + ""); // sort
																					// order
																					// ->
																					// new
																					// to
																					// old

		if (cursor.moveToFirst()) {
			do {
				AppStats info = new AppStats();
				info.setActiveInstalls(cursor.getInt(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_INSTALLS)));
				info.setTotalDownloads(cursor.getInt(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_DOWNLOADS)));

				String dateString = cursor.getString(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_REQUESTDATE));
				info.setRequestDate(Utils.parseDbDate(dateString.substring(0, 10) + " 12:00:00"));

				info.setNumberOfComments(cursor.getInt(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_COMMENTS)));
				info.setVersionCode(cursor.getInt(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_VERSIONCODE)));

				info.setRating(
						cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_1STARS)),
						cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_2STARS)),
						cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_3STARS)),
						cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_4STARS)),
						cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_5STARS)));

				int idx = cursor.getColumnIndex(AppStatsTable.KEY_STATS_NUM_ERRORS);
				if (!cursor.isNull(idx)) {
					info.setNumberOfErrors(cursor.getInt(idx));
				}
				info.init();

				result.add(info);

			} while (cursor.moveToNext());

		}

		cursor.close();

		Collections.reverse(result);

		List<AppStats> missingAppStats = new ArrayList<AppStats>();
		List<Integer> missingAppStatsPositionOffest = new ArrayList<Integer>();

		int positionInsertOffset = 0;

		// add missing sync days
		if (result.size() > 1) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

			for (int currentIndex = 1; currentIndex < result.size(); currentIndex++) {

				String olderEntryDate = result.get(currentIndex - 1).getRequestDateString();
				String newerEntryDate = result.get(currentIndex).getRequestDateString();

				try {
					Date olderDate = dateFormat.parse(olderEntryDate);
					Date newerDate = dateFormat.parse(newerEntryDate);

					long daysDistance = ((newerDate.getTime() - olderDate.getTime()) / 1000 / 60 / 60 / 24);

					for (int i = 1; i < daysDistance; i++) {
						AppStats missingEntry = new AppStats(result.get(currentIndex - 1));
						missingEntry.setRequestDate(new Date(missingEntry.getRequestDate()
								.getTime() + (i * 1000 * 60 * 60 * 24)));
						missingAppStats.add(missingEntry);
						missingAppStatsPositionOffest.add(currentIndex + positionInsertOffset);
						positionInsertOffset++;
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}

		for (int i = 0; i < missingAppStatsPositionOffest.size(); i++) {
			result.add(missingAppStatsPositionOffest.get(i), missingAppStats.get(i));
		}

		// calculate daily downloads
		int nullStartIndex = -1;
		boolean greaterNullDetected = false;

		for (int currentIndex = 1; currentIndex < result.size(); currentIndex++) {

			// normalize daily, total & active
			int olderTotalValue = result.get(currentIndex - 1).getTotalDownloads();
			int newerTotalValue = result.get(currentIndex).getTotalDownloads();
			int totalValueDiff = newerTotalValue - olderTotalValue;

			int olderActiveValue = result.get(currentIndex - 1).getActiveInstalls();
			int newerActiveValue = result.get(currentIndex).getActiveInstalls();
			int activeValueDiff = newerActiveValue - olderActiveValue;

			if (nullStartIndex > -1) {
				if (totalValueDiff > 0) {
					greaterNullDetected = true;
				}
			}

			if (totalValueDiff == 0 && nullStartIndex < 0) {
				nullStartIndex = currentIndex;
			} else {
				if (nullStartIndex != -1 && greaterNullDetected && smoothEnabled) {

					// distance to fill with values
					int distance = currentIndex - nullStartIndex + 1;

					// smoothen values
					int totalSmoothvalue = Math.round(totalValueDiff / distance);
					int activeSmoothvalue = Math.round(activeValueDiff / distance);

					// rounding
					int roundingErrorTotal = newerTotalValue
							- (olderTotalValue + ((totalSmoothvalue * (distance))));
					int roundingErrorActive = newerActiveValue
							- (olderActiveValue + ((activeSmoothvalue * (distance))));
					;

					int totalDownload = result.get(nullStartIndex - 1).getTotalDownloads();
					int activeInstall = result.get(nullStartIndex - 1).getActiveInstalls();

					for (int j = nullStartIndex; j < currentIndex + 1; j++) {

						totalDownload += totalSmoothvalue;
						activeInstall += activeSmoothvalue;

						// for the last value, take rounding error in account
						if (currentIndex == j) {
							result.get(j).setDailyDownloads(totalSmoothvalue + roundingErrorTotal);
							result.get(j).setTotalDownloads(totalDownload + roundingErrorTotal);
							result.get(j).setActiveInstalls(activeInstall + roundingErrorActive);
						} else {
							result.get(j).setDailyDownloads(totalSmoothvalue);
							result.get(j).setTotalDownloads(totalDownload);
							result.get(j).setActiveInstalls(activeInstall);
						}

						result.get(j).setSmoothingApplied(true);
					}

					nullStartIndex = -1;
					greaterNullDetected = false;
				} else {

					result.get(currentIndex).setDailyDownloads(totalValueDiff);
				}
			}
		}

		// reduce if limit exceeded (only if sync < 24h)
		if (result.size() > limit) {
			result = result.subList(result.size() - limit, result.size());
		}

		float overallActiveInstallPercent = 0;
		float overallAvgRating = 0;

		// create rating diff
		AppStats prevStats = null;
		int value = 0;

		for (int i = 0; i < result.size(); i++) {

			AppStats stats = result.get(i);
			if (prevStats != null) {
				value = stats.getRating1() - prevStats.getRating1();
				if (value > heighestRatingChange)
					heighestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;
				stats.setRating1Diff(value);

				value = stats.getRating2() - prevStats.getRating2();
				if (value > heighestRatingChange)
					heighestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;
				stats.setRating2Diff(value);

				value = stats.getRating3() - prevStats.getRating3();
				if (value > heighestRatingChange)
					heighestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;
				stats.setRating3Diff(value);

				value = stats.getRating4() - prevStats.getRating4();
				if (value > heighestRatingChange)
					heighestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;
				stats.setRating4Diff(value);

				value = stats.getRating5() - prevStats.getRating5();
				if (value > heighestRatingChange)
					heighestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;

				stats.setRating5Diff(value);

				stats.setAvgRatingDiff(stats.getAvgRating() - prevStats.getAvgRating());
				stats.setRatingCountDiff(stats.getRatingCount() - prevStats.getRatingCount());
				stats.setNumberOfCommentsDiff(stats.getNumberOfComments()
						- prevStats.getNumberOfComments());
				stats.setActiveInstallsDiff(stats.getActiveInstalls()
						- prevStats.getActiveInstalls());
			}
			prevStats = stats;

			overallActiveInstallPercent += stats.getActiveInstallsPercent();
			overallAvgRating += stats.getAvgRating();

		}

		if (result.size() > 0) {

			AppStats first = result.get(0);
			AppStats last = result.get(result.size() - 1);

			overall.setActiveInstalls(last.getActiveInstalls() - first.getActiveInstalls());
			overall.setTotalDownloads(last.getTotalDownloads() - first.getTotalDownloads());
			overall.setRating1(last.getRating1() - first.getRating1());
			overall.setRating2(last.getRating2() - first.getRating2());
			overall.setRating3(last.getRating3() - first.getRating3());
			overall.setRating4(last.getRating4() - first.getRating4());
			overall.setRating5(last.getRating5() - first.getRating5());
			overall.init();
			overall.setDailyDownloads((last.getTotalDownloads() - first.getTotalDownloads())
					/ result.size());

			BigDecimal avgBigDecimal = new BigDecimal(overallAvgRating / result.size());
			avgBigDecimal = avgBigDecimal.setScale(3, BigDecimal.ROUND_HALF_UP);
			overall.setAvgRatingString(avgBigDecimal.toPlainString() + "");

			BigDecimal percentBigDecimal = new BigDecimal(overallActiveInstallPercent
					/ result.size());
			percentBigDecimal = percentBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);

			overall.setActiveInstallsPercentString(percentBigDecimal.toPlainString() + "");

		}

		AppStatsList list = new AppStatsList();
		list.setAppStats(result);
		list.setHighestRatingChange(heighestRatingChange);
		list.setLowestRatingChange(lowestRatingChange);

		list.setOverall(overall);

		return list;

	}

	public AppStats getLatestForApp(String packageName) {

		AppStats info = null;

		Cursor cursor = context.getContentResolver().query(
				AppStatsTable.CONTENT_URI,
				new String[] { AppStatsTable.KEY_ROWID, AppStatsTable.KEY_STATS_PACKAGENAME,
						AppStatsTable.KEY_STATS_DOWNLOADS, AppStatsTable.KEY_STATS_VERSIONCODE,
						AppStatsTable.KEY_STATS_INSTALLS, AppStatsTable.KEY_STATS_COMMENTS,
						AppStatsTable.KEY_STATS_MARKETERANKING,
						AppStatsTable.KEY_STATS_CATEGORYRANKING, AppStatsTable.KEY_STATS_5STARS,
						AppStatsTable.KEY_STATS_4STARS, AppStatsTable.KEY_STATS_3STARS,
						AppStatsTable.KEY_STATS_2STARS, AppStatsTable.KEY_STATS_1STARS,
						AppStatsTable.KEY_STATS_REQUESTDATE },
				AppStatsTable.KEY_STATS_PACKAGENAME + "='" + packageName + "'", null,
				AppStatsTable.KEY_STATS_REQUESTDATE + " desc limit 1");

		if (cursor.moveToFirst()) {

			do {
				info = new AppStats();
				info.setActiveInstalls(cursor.getInt(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_INSTALLS)));
				info.setTotalDownloads(cursor.getInt(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_DOWNLOADS)));
				info.setRequestDate(Utils.parseDbDate(cursor.getString(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_REQUESTDATE))));
				info.setNumberOfComments(cursor.getInt(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_COMMENTS)));
				info.setVersionCode(cursor.getInt(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_VERSIONCODE)));

				info.setRating(
						cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_1STARS)),
						cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_2STARS)),
						cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_3STARS)),
						cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_4STARS)),
						cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_5STARS)));

				info.init();

			} while (cursor.moveToNext());

		}

		cursor.close();

		return info;
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

		Cursor cursor = context.getContentResolver().query(
				AppStatsTable.CONTENT_URI,
				new String[] { AppStatsTable.KEY_ROWID, AppStatsTable.KEY_STATS_PACKAGENAME,
						AppStatsTable.KEY_STATS_DOWNLOADS, AppStatsTable.KEY_STATS_INSTALLS,
						AppStatsTable.KEY_STATS_COMMENTS, AppStatsTable.KEY_STATS_MARKETERANKING,
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

				int value1 = cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_1STARS));
				ratings.put(1, getDiff(prev1, value1));
				prev1 = value1;

				int value2 = cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_1STARS));
				ratings.put(2, getDiff(prev2, value2));
				prev2 = value2;

				int value3 = cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_3STARS));
				ratings.put(3, getDiff(prev3, value3));
				prev3 = value3;

				int value4 = cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_4STARS));
				ratings.put(4, getDiff(prev4, value4));
				prev4 = value4;

				int value5 = cursor.getInt(cursor.getColumnIndex(AppStatsTable.KEY_STATS_5STARS));
				ratings.put(5, getDiff(prev5, value5));
				prev5 = value5;

				String dateString = cursor.getString(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_REQUESTDATE));
				Date date = Utils.parseDbDate(dateString.substring(0, 10) + " 00:00:00");

				result.put(date, ratings);

			} while (cursor.moveToNext());
		}

		cursor.close();

		return result;

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
			initialValues.put(CommentsTable.KEY_COMMENT_TEXT, comment.getText());
			initialValues.put(CommentsTable.KEY_COMMENT_USER, comment.getUser());
			initialValues.put(CommentsTable.KEY_COMMENT_APP_VERSION, comment.getAppVersion());
			initialValues.put(CommentsTable.KEY_COMMENT_DEVICE, comment.getDevice());
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
			initialValues.put(CommentsTable.KEY_COMMENT_ORIGINAL_TEXT, comment.getOriginalText());

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
							CommentsTable.KEY_COMMENT_RATING, CommentsTable.KEY_COMMENT_TEXT,
							CommentsTable.KEY_COMMENT_USER, CommentsTable.KEY_COMMENT_DEVICE,
							CommentsTable.KEY_COMMENT_APP_VERSION,
							CommentsTable.KEY_COMMENT_REPLY_TEXT,
							CommentsTable.KEY_COMMENT_REPLY_DATE,
							CommentsTable.KEY_COMMENT_LANGUAGE,
							CommentsTable.KEY_COMMENT_ORIGINAL_TEXT },
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
				comment.setText(cursor.getString(cursor
						.getColumnIndex(CommentsTable.KEY_COMMENT_TEXT)));
				comment.setDevice(cursor.getString(cursor
						.getColumnIndex(CommentsTable.KEY_COMMENT_DEVICE)));
				comment.setAppVersion(cursor.getString(cursor
						.getColumnIndex(CommentsTable.KEY_COMMENT_APP_VERSION)));
				comment.setRating(cursor.getInt(cursor
						.getColumnIndex(CommentsTable.KEY_COMMENT_RATING)));
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
				idx = cursor.getColumnIndex(CommentsTable.KEY_COMMENT_ORIGINAL_TEXT);
				if (!cursor.isNull(idx)) {
					comment.setOriginalText(cursor.getString(idx));
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

	// XXX this does nothing. Remove?
	public List<Date> getVersionUpdateDates(String packageName) {

		List<Date> result = new ArrayList<Date>();

		if (true) {
			return result;
		}

		Uri uri = Uri.parse("content://" + AndlyticsContentProvider.AUTHORITY + "/"
				+ AndlyticsContentProvider.APP_VERSION_CHANGE);

		List<String> versionCodes = new ArrayList<String>();

		Cursor cursor = context.getContentResolver().query(
				uri,
				new String[] { AppStatsTable.KEY_ROWID, AppStatsTable.KEY_STATS_PACKAGENAME,
						AppStatsTable.KEY_STATS_DOWNLOADS, AppStatsTable.KEY_STATS_INSTALLS,
						AppStatsTable.KEY_STATS_COMMENTS, AppStatsTable.KEY_STATS_MARKETERANKING,
						AppStatsTable.KEY_STATS_CATEGORYRANKING, AppStatsTable.KEY_STATS_5STARS,
						AppStatsTable.KEY_STATS_4STARS, AppStatsTable.KEY_STATS_3STARS,
						AppStatsTable.KEY_STATS_2STARS, AppStatsTable.KEY_STATS_1STARS,
						AppStatsTable.KEY_STATS_REQUESTDATE, AppStatsTable.KEY_STATS_VERSIONCODE },
				AppStatsTable.KEY_STATS_PACKAGENAME + "='" + packageName + "'", null,
				AppStatsTable.KEY_STATS_REQUESTDATE); // sort order -> new to
														// old

		if (cursor.moveToFirst()) {

			do {

				AppStats info = new AppStats();
				String dateString = cursor.getString(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_REQUESTDATE));
				info.setRequestDate(Utils.parseDbDate(dateString.substring(0, 10) + " 12:00:00"));

				info.setNumberOfComments(cursor.getInt(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_COMMENTS)));
				info.setVersionCode(cursor.getInt(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_VERSIONCODE)));

				versionCodes.add(cursor.getInt(cursor
						.getColumnIndex(AppStatsTable.KEY_STATS_VERSIONCODE)) + "");

			} while (cursor.moveToNext());
		}

		cursor.close();

		Collections.sort(versionCodes);

		for (String code : versionCodes) {

			cursor = context.getContentResolver().query(
					AppStatsTable.CONTENT_URI,
					new String[] {

					AppStatsTable.KEY_STATS_REQUESTDATE, AppStatsTable.KEY_STATS_VERSIONCODE },
					AppStatsTable.KEY_STATS_PACKAGENAME + "='" + packageName + "' and "
							+ AppStatsTable.KEY_STATS_VERSIONCODE + "=" + code, null,
					AppStatsTable.KEY_STATS_REQUESTDATE + " limit 1"); // sort
																		// order
																		// ->
																		// new
																		// to
																		// old

			if (cursor.moveToFirst()) {

				do {
					String dateString = cursor.getString(cursor
							.getColumnIndex(AppStatsTable.KEY_STATS_REQUESTDATE));
					result.add(Utils.parseDbDate(dateString.substring(0, 10) + " 12:00:00"));
				} while (cursor.moveToNext());
			}

			cursor.close();

		}

		if (result.size() > 0) {
			result = result.subList(1, result.size());
		}

		return result;
	}

}
