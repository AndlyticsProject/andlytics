package com.github.andlyticsproject.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.github.andlyticsproject.Preferences;
import com.github.andlyticsproject.model.AppDetails;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.DeveloperAccount;
import com.github.andlyticsproject.model.Link;
import com.github.andlyticsproject.model.Revenue;
import com.github.andlyticsproject.model.RevenueSummary;
import com.github.andlyticsproject.sync.AutosyncHandler;
import com.github.andlyticsproject.util.Utils;

public class AndlyticsDb extends SQLiteOpenHelper {

	private static final String TAG = AndlyticsDb.class.getSimpleName();

	private static final int DATABASE_VERSION = 24;

	private static final String DATABASE_NAME = "andlytics";

	private static AndlyticsDb instance;

	private Context context;

	public static synchronized AndlyticsDb getInstance(Context context) {
		if (instance == null) {
			instance = new AndlyticsDb(context);
		}

		return instance;
	}

	private AndlyticsDb(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "Creating databse");
		db.execSQL(AppInfoTable.TABLE_CREATE_APPINFO);
		db.execSQL(AppStatsTable.TABLE_CREATE_STATS);
		db.execSQL(CommentsTable.TABLE_CREATE_COMMENTS);
		db.execSQL(AdmobTable.TABLE_CREATE_ADMOB);
		db.execSQL(DeveloperAccountsTable.TABLE_CREATE_DEVELOPER_ACCOUNT);
		db.execSQL(LinksTable.TABLE_CREATE_LINKS);
		db.execSQL(AppDetailsTable.TABLE_CREATE_APP_DETAILS);
		db.execSQL(RevenueSummaryTable.TABLE_CREATE_REVENUE_SUMMARY);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ".");
		if (oldVersion < 9) {
			Log.w(TAG, "Old version < 9 - drop all tables & recreate");
			db.execSQL("DROP TABLE IF EXISTS " + AppInfoTable.DATABASE_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + AppStatsTable.DATABASE_TABLE_NAME);
			db.execSQL(AppInfoTable.TABLE_CREATE_APPINFO);
			db.execSQL(AppStatsTable.TABLE_CREATE_STATS);
		}

		if (oldVersion == 9) {
			Log.w(TAG, "Old version = 9 - add ghost column");
			db.execSQL("ALTER table " + AppInfoTable.DATABASE_TABLE_NAME + " add "
					+ AppInfoTable.KEY_APP_GHOST + " integer");
		}
		if (oldVersion > 8 && oldVersion < 11) {
			Log.w(TAG, "Old version = 10 or 9 - add rating expand column");
			db.execSQL("ALTER table " + AppInfoTable.DATABASE_TABLE_NAME + " add "
					+ AppInfoTable.KEY_APP_RATINGS_EXPANDED + " integer");
		}
		if (oldVersion < 12) {
			Log.w(TAG, "Old version < 12 - add comments table");
			db.execSQL(CommentsTable.TABLE_CREATE_COMMENTS);
		}
		if (oldVersion < 13) {
			Log.w(TAG, "Old version < 13 - add skip notification");
			db.execSQL("ALTER table " + AppInfoTable.DATABASE_TABLE_NAME + " add "
					+ AppInfoTable.KEY_APP_SKIP_NOTIFICATION + " integer");
		}
		if (oldVersion < 14) {
			Log.w(TAG, "Old version < 14 - add admob table");
			db.execSQL(AdmobTable.TABLE_CREATE_ADMOB);
		}
		if (oldVersion < 15) {
			Log.w(TAG, "Old version < 15 - add version name");
			db.execSQL("ALTER table " + AppInfoTable.DATABASE_TABLE_NAME + " add "
					+ AppInfoTable.KEY_APP_VERSION_NAME + " text");

		}

		if (oldVersion < 16) {
			Log.w(TAG, "Old version < 16 - add new comments colums");
			db.execSQL("ALTER table " + CommentsTable.DATABASE_TABLE_NAME + " add "
					+ CommentsTable.KEY_COMMENT_APP_VERSION + " text");
			db.execSQL("ALTER table " + CommentsTable.DATABASE_TABLE_NAME + " add "
					+ CommentsTable.KEY_COMMENT_DEVICE + " text");
		}

		if (oldVersion < 17) {
			Log.w(TAG, "Old version < 17 - changing comments date format");
			db.execSQL("DROP TABLE IF EXISTS " + CommentsTable.DATABASE_TABLE_NAME);
			db.execSQL(CommentsTable.TABLE_CREATE_COMMENTS);
		}

		if (oldVersion < 18) {
			Log.w(TAG, "Old version < 18 - adding replies to comments");
			db.execSQL("DROP TABLE IF EXISTS " + CommentsTable.DATABASE_TABLE_NAME);
			db.execSQL(CommentsTable.TABLE_CREATE_COMMENTS);
		}

		if (oldVersion < 19) {
			Log.w(TAG, "Old version < 19 - adding developer_accounts table");
			db.execSQL("DROP TABLE IF EXISTS " + DeveloperAccountsTable.DATABASE_TABLE_NAME);
			db.execSQL(DeveloperAccountsTable.TABLE_CREATE_DEVELOPER_ACCOUNT);

			migrateAccountsFromPrefs(db);

			Log.d(TAG, "Old version < 19 - adding new appinfo columns");
			db.execSQL("ALTER table " + AppInfoTable.DATABASE_TABLE_NAME + " add "
					+ AppInfoTable.KEY_APP_ADMOB_ACCOUNT + " text");
			db.execSQL("ALTER table " + AppInfoTable.DATABASE_TABLE_NAME + " add "
					+ AppInfoTable.KEY_APP_ADMOB_SITE_ID + " text");
			db.execSQL("ALTER table " + AppInfoTable.DATABASE_TABLE_NAME + " add "
					+ AppInfoTable.KEY_APP_LAST_COMMENTS_UPDATE + " date");

			migrateAppInfoPrefs(db);

			Log.d(TAG, "Old version < 19 - adding new appstats columns");
			db.execSQL("ALTER table " + AppStatsTable.DATABASE_TABLE_NAME + " add "
					+ AppStatsTable.KEY_STATS_NUM_ERRORS + " integer");
		}

		if (oldVersion < 20) {
			Log.w(TAG, "Old version < 20 - add new comments colums");
			db.execSQL("ALTER table " + CommentsTable.DATABASE_TABLE_NAME + " add "
					+ CommentsTable.KEY_COMMENT_LANGUAGE + " text");
			db.execSQL("ALTER table " + CommentsTable.DATABASE_TABLE_NAME + " add "
					+ CommentsTable.KEY_COMMENT_ORIGINAL_TEXT + " text");
			db.execSQL("ALTER table " + CommentsTable.DATABASE_TABLE_NAME + " add "
					+ CommentsTable.KEY_COMMENT_UNIQUE_ID + " text");

			Log.w(TAG, "Old version < 20 - adding links table");
			db.execSQL("DROP TABLE IF EXISTS " + LinksTable.DATABASE_TABLE_NAME);
			db.execSQL(LinksTable.TABLE_CREATE_LINKS);

			Log.d(TAG, "Old version < 20 - adding new app_details table");
			db.execSQL("DROP TABLE IF EXISTS " + AppDetailsTable.DATABASE_TABLE_NAME);
			db.execSQL(AppDetailsTable.TABLE_CREATE_APP_DETAILS);

			Log.d(TAG, "Old version < 20 - adding new appinfo columns");
			db.execSQL("ALTER table " + AppInfoTable.DATABASE_TABLE_NAME + " add "
					+ AppInfoTable.KEY_APP_DEVELOPER_ID + " text");
			db.execSQL("ALTER table " + AppInfoTable.DATABASE_TABLE_NAME + " add "
					+ AppInfoTable.KEY_APP_DEVELOPER_NAME + " text");
			// XXX
			//			db.execSQL("ALTER table " + DeveloperAccountsTable.DATABASE_TABLE_NAME + " add "
			//					+ DeveloperAccountsTable.DEVELOPER_ID + " text");
		}
		if (oldVersion < 21) {
			Log.w(TAG, "Old version < 21 - adding revenue_summary table");
			db.execSQL("DROP TABLE IF EXISTS " + RevenueSummaryTable.DATABASE_TABLE_NAME);
			db.execSQL(RevenueSummaryTable.TABLE_CREATE_REVENUE_SUMMARY);

			Log.w(TAG, "Old version < 21 - add new stats colums");
			db.execSQL("ALTER table " + AppStatsTable.DATABASE_TABLE_NAME + " add "
					+ AppStatsTable.KEY_STATS_TOTAL_REVENUE + " double");
			db.execSQL("ALTER table " + AppStatsTable.DATABASE_TABLE_NAME + " add "
					+ AppStatsTable.KEY_STATS_CURRENCY + " text");
		}

		// only add this if migrating from 21
		if (oldVersion == 21) {
			Log.w(TAG, "Old version < 22 - adding revenue_summary.date column");
			db.execSQL("ALTER table " + RevenueSummaryTable.DATABASE_TABLE_NAME + " add "
					+ RevenueSummaryTable.DATE + " date");
			// set all 2013-01-01 00:00:00
			db.execSQL("UPDATE " + RevenueSummaryTable.DATABASE_TABLE_NAME + " SET "
					+ RevenueSummaryTable.DATE + "= '1356998400'");
		}

		if (oldVersion < 23) {
			Log.w(TAG, "Old version < 23 - adding appinfo.ad_unit_id column");
			db.execSQL("ALTER table " + AppInfoTable.DATABASE_TABLE_NAME + " add "
					+ AppInfoTable.KEY_APP_ADMOB_AD_UNIT_ID + " text");

			Log.w(TAG, "Old version < 23 - adding admob.currency column");
			db.execSQL("ALTER table " + AdmobTable.DATABASE_TABLE_NAME + " add "
					+ AdmobTable.KEY_CURRENCY + " text");
		}
		
		if (oldVersion < 24) {
			Log.w(TAG, "Old version < 24 - adding comment.title column");
			db.execSQL("ALTER table " + CommentsTable.DATABASE_TABLE_NAME + " add "
					+ CommentsTable.KEY_COMMENT_TITLE + " text");
			db.execSQL("ALTER table " + CommentsTable.DATABASE_TABLE_NAME + " add "
					+ CommentsTable.KEY_COMMENT_ORIGINAL_TITLE + " text");
		}
	}

	@SuppressWarnings("deprecation")
	private void migrateAppInfoPrefs(SQLiteDatabase db) {
		Log.d(TAG, "Migrating app info settings from preferences...");
		int migrated = 0;

		db.beginTransaction();
		try {
			Cursor c = null;
			class Package {
				long id;
				String name;
			}
			List<Package> packages = new ArrayList<Package>();
			try {
				c = db.query(AppInfoTable.DATABASE_TABLE_NAME, new String[] {
						AppInfoTable.KEY_ROWID, AppInfoTable.KEY_APP_PACKAGENAME }, null, null,
						null, null, "_id asc", null);
				while (c.moveToNext()) {
					Package p = new Package();
					p.id = c.getLong(0);
					p.name = c.getString(1);
					packages.add(p);
				}
			} finally {
				if (c != null) {
					c.close();
				}
			}
			for (Package p : packages) {
				Log.d(TAG, "Migrating package: " + p.name);
				String admobSiteId = Preferences.getAdmobSiteId(context, p.name);
				if (admobSiteId != null) {
					String admobAccount = Preferences.getAdmobAccount(context, admobSiteId);
					ContentValues values = new ContentValues();
					values.put(AppInfoTable.KEY_APP_ADMOB_SITE_ID, admobSiteId);
					values.put(AppInfoTable.KEY_APP_ADMOB_ACCOUNT, admobAccount);
					db.update(AppInfoTable.DATABASE_TABLE_NAME, values, "_id = ?",
							new String[] { Long.toString(p.id) });
				}
				long lastCommentsUpdate = Preferences.getLastCommentsRemoteUpdateTime(context,
						p.name);
				if (lastCommentsUpdate != 0) {
					ContentValues values = new ContentValues();
					values.put(AppInfoTable.KEY_APP_LAST_COMMENTS_UPDATE, lastCommentsUpdate);
					db.update(AppInfoTable.DATABASE_TABLE_NAME, values, "_id = ?",
							new String[] { Long.toString(p.id) });
				}
				migrated++;
			}

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		Log.d(TAG,
				String.format("Successfully migrated app info settings for %d packages", migrated));
	}

	@SuppressWarnings("deprecation")
	private void migrateAccountsFromPrefs(SQLiteDatabase db) {
		Log.d(TAG, "Migrating developer accounts from preferences...");
		int migrated = 0;

		db.beginTransaction();
		try {
			AccountManager am = AccountManager.get(context);
			Account[] accounts = am.getAccountsByType(AutosyncHandler.ACCOUNT_TYPE_GOOGLE);
			String activeAccount = Preferences.getAccountName(context);
			for (Account account : accounts) {
				boolean isHidden = Preferences.getIsHiddenAccount(context, account.name);
				long lastStatsUpdate = Preferences.getLastStatsRemoteUpdateTime(context,
						account.name);
				DeveloperAccount.State state = DeveloperAccount.State.ACTIVE;
				if (isHidden) {
					state = DeveloperAccount.State.HIDDEN;
				}
				if (account.name.equals(activeAccount)) {
					state = DeveloperAccount.State.SELECTED;
				}
				DeveloperAccount developerAccount = new DeveloperAccount(account.name, state);
				if (lastStatsUpdate != 0) {
					developerAccount.setLastStatsUpdate(new Date(lastStatsUpdate));
				}

				Log.d(TAG, "Adding account: " + developerAccount);
				addDeveloperAccount(db, developerAccount);
				migrated++;
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		Log.d(TAG, String.format("Successfully migrated %d developer accounts", migrated));
	}

	private long addDeveloperAccount(SQLiteDatabase db, DeveloperAccount account) {
		ContentValues values = toValues(account);

		return db.insertOrThrow(DeveloperAccountsTable.DATABASE_TABLE_NAME, null, values);
	}

	public synchronized long addDeveloperAccount(DeveloperAccount account) {
		return addDeveloperAccount(getWritableDatabase(), account);
	}

	public static ContentValues toValues(DeveloperAccount account) {
		ContentValues result = new ContentValues();
		result.put(DeveloperAccountsTable.NAME, account.getName());
		result.put(DeveloperAccountsTable.STATE, account.getState().ordinal());
		long updateTime = account.getLastStatsUpdate() == null ? 0 : account.getLastStatsUpdate()
				.getTime();
		result.put(DeveloperAccountsTable.LAST_STATS_UPDATE, updateTime);
		// XXX
		//		result.put(DeveloperAccountsTable.DEVELOPER_ID, account.getDeveloperId());

		return result;
	}

	// account, site ID, ad unit ID (only if migrated to 'new Admob')
	public String[] getAdmobDetails(String packageName) {
		SQLiteDatabase db = getWritableDatabase();
		Cursor c = null;
		try {
			c = db.query(AppInfoTable.DATABASE_TABLE_NAME, new String[] {
					AppInfoTable.KEY_APP_ADMOB_ACCOUNT, AppInfoTable.KEY_APP_ADMOB_SITE_ID,
					AppInfoTable.KEY_APP_ADMOB_AD_UNIT_ID }, AppInfoTable.KEY_APP_PACKAGENAME
					+ "=?", new String[] { packageName }, null, null, null);
			if (!c.moveToNext()) {
				return null;
			}

			String[] result = new String[3];
			result[0] = c.getString(c.getColumnIndex(AppInfoTable.KEY_APP_ADMOB_ACCOUNT));
			result[1] = c.getString(c.getColumnIndex(AppInfoTable.KEY_APP_ADMOB_SITE_ID));
			result[2] = c.getString(c.getColumnIndex(AppInfoTable.KEY_APP_ADMOB_AD_UNIT_ID));
			if (result[0] == null && result[1] == null) {
				return null;
			}

			return result;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public synchronized void saveAdmobDetails(String packageName, String admobAccount,
			String admobSiteId) {
		saveAdmobDetails(packageName, admobAccount, admobSiteId, null);
	}

	public synchronized void saveAdmobDetails(String packageName, String admobAccount,
			String admobSiteId, String admobAdUnitId) {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
			long id = findPackageId(db, packageName);

			ContentValues values = new ContentValues();
			values.put(AppInfoTable.KEY_APP_ADMOB_ACCOUNT, admobAccount);
			values.put(AppInfoTable.KEY_APP_ADMOB_SITE_ID, admobSiteId);
			values.put(AppInfoTable.KEY_APP_ADMOB_AD_UNIT_ID, admobAdUnitId);

			db.update(AppInfoTable.DATABASE_TABLE_NAME, values, "_id = ?",
					new String[] { Long.toString(id) });

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public synchronized void saveAdmobAdUnitId(String packageName, String admobAccount,
			String admobAdUnitId) {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
			long id = findPackageId(db, packageName);

			ContentValues values = new ContentValues();
			values.put(AppInfoTable.KEY_APP_ADMOB_ACCOUNT, admobAccount);
			values.put(AppInfoTable.KEY_APP_ADMOB_AD_UNIT_ID, admobAdUnitId);

			db.update(AppInfoTable.DATABASE_TABLE_NAME, values, "_id = ?",
					new String[] { Long.toString(id) });

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public synchronized long getLastCommentsRemoteUpdateTime(String packageName) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = null;
		try {
			c = db.query(AppInfoTable.DATABASE_TABLE_NAME,
					new String[] { AppInfoTable.KEY_APP_LAST_COMMENTS_UPDATE },
					AppInfoTable.KEY_APP_PACKAGENAME + "=?", new String[] { packageName }, null,
					null, null);
			if (c.getCount() != 1) {
				Log.w(TAG,
						String.format("Unexpected package count for %s: %d", packageName,
								c.getCount()));
			}
			if (c.getCount() < 1 || !c.moveToNext()) {
				throw new IllegalStateException(String.format(
						"Package name not found in AppInfo table: %s. count=%d", packageName,
						c.getCount()));
			}

			int idx = c.getColumnIndex(AppInfoTable.KEY_APP_LAST_COMMENTS_UPDATE);
			if (c.isNull(idx)) {
				return 0;
			}

			return c.getLong(idx);
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public synchronized void saveLastCommentsRemoteUpdateTime(String packageName, long updateTime) {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
			long id = findPackageId(db, packageName);

			ContentValues values = new ContentValues();
			values.put(AppInfoTable.KEY_APP_LAST_COMMENTS_UPDATE, updateTime);

			db.update(AppInfoTable.DATABASE_TABLE_NAME, values, "_id = ?",
					new String[] { Long.toString(id) });

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private long findPackageId(SQLiteDatabase db, String packageName) {
		Cursor c = null;
		try {
			c = db.query(AppInfoTable.DATABASE_TABLE_NAME, new String[] { AppInfoTable.KEY_ROWID },
					AppInfoTable.KEY_APP_PACKAGENAME + "=?", new String[] { packageName }, null,
					null, null);
			if (c.getCount() != 1) {
				Log.w(TAG,
						String.format("Unexpected package count for %s: %d", packageName,
								c.getCount()));
			}
			if (c.getCount() < 1 || !c.moveToNext()) {
				throw new IllegalStateException(String.format(
						"Package name not found in AppInfo table: %s. count=%d", packageName,
						c.getCount()));
			}

			return c.getLong(c.getColumnIndex(AppInfoTable.KEY_ROWID));
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public synchronized AppInfo findAppByPackageName(String packageName) {
		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = null;
		try {
			cursor = db.query(AppInfoTable.DATABASE_TABLE_NAME, new String[] {
					AppInfoTable.KEY_ROWID, AppInfoTable.KEY_APP_VERSION_NAME,
					AppInfoTable.KEY_APP_PACKAGENAME, AppInfoTable.KEY_APP_LASTUPDATE,
					AppInfoTable.KEY_APP_NAME, AppInfoTable.KEY_APP_GHOST,
					AppInfoTable.KEY_APP_SKIP_NOTIFICATION, AppInfoTable.KEY_APP_RATINGS_EXPANDED,
					AppInfoTable.KEY_APP_ICONURL, AppInfoTable.KEY_APP_ADMOB_ACCOUNT,
					AppInfoTable.KEY_APP_ADMOB_SITE_ID, AppInfoTable.KEY_APP_LAST_COMMENTS_UPDATE,
					AppInfoTable.KEY_APP_ACCOUNT, AppInfoTable.KEY_APP_DEVELOPER_ID,
					AppInfoTable.KEY_APP_DEVELOPER_NAME }, AppInfoTable.KEY_APP_PACKAGENAME + "=?",
					new String[] { packageName }, null, null, null);

			if (cursor.getCount() < 1 || !cursor.moveToNext()) {
				return null;
			}

			AppInfo appInfo = new AppInfo();
			appInfo.setId(cursor.getLong(cursor.getColumnIndex(AppInfoTable.KEY_ROWID)));
			appInfo.setAccount(cursor.getString(cursor.getColumnIndex(AppInfoTable.KEY_APP_ACCOUNT)));
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

			fetchAppDetails(appInfo);

			return appInfo;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public synchronized void fetchAppDetails(AppInfo appInfo) {
		if (appInfo.getId() == null) {
			// not persistent
			return;
		}

		SQLiteDatabase db = getReadableDatabase();
		Cursor c = null;
		try {
			c = db.query(AppDetailsTable.DATABASE_TABLE_NAME, AppDetailsTable.ALL_COLUMNS,
					AppDetailsTable.APPINFO_ID + "=?",
					new String[] { Long.toString(appInfo.getId()) }, null, null, null);
			if (c.getCount() < 1 || !c.moveToNext()) {
				return;
			}

			Long id = c.getLong(c.getColumnIndex(AppDetailsTable.ROWID));
			String description = c.getString(c.getColumnIndex(AppDetailsTable.DESCRIPTION));
			AppDetails details = new AppDetails(description);
			details.setId(id);
			int idx = c.getColumnIndex(AppDetailsTable.CHANGELOG);
			if (!c.isNull(idx)) {
				details.setChangelog(c.getString(idx));
			}
			idx = c.getColumnIndex(AppDetailsTable.LAST_STORE_UPDATE);
			if (!c.isNull(idx)) {
				details.setLastStoreUpdate(new Date(c.getLong(idx)));
			}
			List<Link> links = getLinksForApp(id);
			details.setLinks(links);

			appInfo.setDetails(details);
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	private long saveAppDetails(SQLiteDatabase db, AppInfo appInfo) {
		ContentValues values = toValues(appInfo);

		return db.insertOrThrow(AppDetailsTable.DATABASE_TABLE_NAME, null, values);
	}

	public synchronized long saveAppDetails(AppInfo appInfo) {
		return saveAppDetails(getWritableDatabase(), appInfo);
	}

	public synchronized void updateAppDetails(AppDetails details) {
		ContentValues values = new ContentValues();
		values.put(AppDetailsTable.DESCRIPTION, details.getDescription());
		values.put(AppDetailsTable.CHANGELOG, details.getChangelog());
		long updateTime = details.getLastStoreUpdate() == null ? 0 : details.getLastStoreUpdate()
				.getTime();
		values.put(AppDetailsTable.LAST_STORE_UPDATE, updateTime);

		getWritableDatabase().update(AppDetailsTable.DATABASE_TABLE_NAME, values, "_id = ?",
				new String[] { Long.toString(details.getId()) });
	}

	public synchronized void insertOrUpdateAppDetails(AppInfo appInfo) {
		SQLiteDatabase db = getWritableDatabase();

		Cursor c = null;
		try {
			c = db.query(AppDetailsTable.DATABASE_TABLE_NAME,
					new String[] { AppDetailsTable.ROWID }, AppDetailsTable.APPINFO_ID + "=?",
					new String[] { Long.toString(appInfo.getId()) }, null, null, null);
			if (c.getCount() < 1 || !c.moveToNext()) {
				saveAppDetails(appInfo);
			} else {
				long id = c.getLong(c.getColumnIndex(AppDetailsTable.ROWID));
				appInfo.getDetails().setId(id);
				updateAppDetails(appInfo.getDetails());
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public static ContentValues toValues(AppInfo appInfo) {
		AppDetails details = appInfo.getDetails();

		ContentValues result = new ContentValues();
		result.put(AppDetailsTable.DESCRIPTION, details.getDescription());
		result.put(AppDetailsTable.CHANGELOG, details.getChangelog());
		long updateTime = details.getLastStoreUpdate() == null ? 0 : details.getLastStoreUpdate()
				.getTime();
		result.put(AppDetailsTable.LAST_STORE_UPDATE, updateTime);
		result.put(AppDetailsTable.APPINFO_ID, appInfo.getId());

		return result;
	}

	public synchronized ArrayList<Link> getLinksForApp(long appDetailsId) {
		SQLiteDatabase db = getReadableDatabase();

		ArrayList<Link> result = new ArrayList<Link>();

		Cursor cursor = null;
		try {
			cursor = db.query(LinksTable.DATABASE_TABLE_NAME, LinksTable.ALL_COLUMNS,
					LinksTable.APP_DETAILS_ID + " = ?",
					new String[] { Long.toString(appDetailsId) }, null, null, LinksTable.ROWID);
			if (cursor == null) {
				return result;
			}

			while (cursor.moveToNext()) {
				Link link = new Link();
				link.setId(cursor.getLong(cursor.getColumnIndex(LinksTable.ROWID)));
				link.setName(cursor.getString(cursor.getColumnIndex(LinksTable.LINK_NAME)));
				link.setURL(cursor.getString(cursor.getColumnIndex(LinksTable.LINK_URL)));

				result.add(link);
			}

			return result;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public synchronized void deleteLink(long id) {
		getWritableDatabase().delete(LinksTable.DATABASE_TABLE_NAME, LinksTable.ROWID + "=?",
				new String[] { Long.toString(id) });
	}

	public synchronized void addLink(AppDetails appDetails, String url, String name) {
		ContentValues values = new ContentValues();
		values.put(LinksTable.APP_DETAILS_ID, appDetails.getId());
		values.put(LinksTable.LINK_URL, url);
		values.put(LinksTable.LINK_NAME, name);
		getWritableDatabase().insertOrThrow(LinksTable.DATABASE_TABLE_NAME, null, values);
	}

	public synchronized void editLink(Long id, String url, String name) {
		ContentValues values = new ContentValues();
		values.put(LinksTable.LINK_URL, url);
		values.put(LinksTable.LINK_NAME, name);

		getWritableDatabase().update(LinksTable.DATABASE_TABLE_NAME, values,
				LinksTable.ROWID + " = ?", new String[] { Long.toString(id) });
	}

	public synchronized void fetchRevenueSummary(AppInfo appInfo) {
		if (appInfo.getId() == null) {
			// not persistent
			return;
		}

		SQLiteDatabase db = getReadableDatabase();
		Cursor c = null;
		try {
			c = db.query(RevenueSummaryTable.DATABASE_TABLE_NAME, RevenueSummaryTable.ALL_COLUMNS,
					RevenueSummaryTable.APPINFO_ID + "=?",
					new String[] { Long.toString(appInfo.getId()) }, null, null,
					RevenueSummaryTable.DATE + " desc", "1");
			if (c.getCount() < 1 || !c.moveToNext()) {
				return;
			}

			Long id = c.getLong(c.getColumnIndex(RevenueSummaryTable.ROWID));
			int typeIdx = c.getInt(c.getColumnIndex(RevenueSummaryTable.TYPE));
			String currency = c.getString(c.getColumnIndex(RevenueSummaryTable.CURRENCY));
			Date date = Utils.parseDbDate("2013-01-01 00:00:00");
			int idx = c.getColumnIndex(RevenueSummaryTable.DATE);
			if (!c.isNull(idx)) {
				date = new Date(c.getLong(idx));
			}
			double lastDayTotal = c.getDouble(c.getColumnIndex(RevenueSummaryTable.LAST_DAY_TOTAL));
			double last7DaysTotal = c.getDouble(c
					.getColumnIndex(RevenueSummaryTable.LAST_7DAYS_TOTAL));
			double last30DaysTotal = c.getDouble(c
					.getColumnIndex(RevenueSummaryTable.LAST_30DAYS_TOTAL));
			double overallTotal = c.getDouble(c.getColumnIndex(RevenueSummaryTable.OVERALL_TOTAL));

			Revenue.Type type = Revenue.Type.values()[typeIdx];
			RevenueSummary revenue = new RevenueSummary(type, currency, date, lastDayTotal,
					last7DaysTotal, last30DaysTotal, overallTotal);
			revenue.setId(id);

			appInfo.setTotalRevenueSummary(revenue);
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

}
