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

import com.github.andlyticsproject.Constants;
import com.github.andlyticsproject.Preferences;
import com.github.andlyticsproject.model.DeveloperAccount;

public class AndlyticsDb extends SQLiteOpenHelper {

	private static final String TAG = AndlyticsDb.class.getSimpleName();

	private static final int DATABASE_VERSION = 19;

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
			Account[] accounts = am.getAccountsByType(Constants.ACCOUNT_TYPE_GOOGLE);
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

		return result;
	}

	// account, site ID
	public String[] getAdmobDetails(String packageName) {
		SQLiteDatabase db = getWritableDatabase();
		Cursor c = null;
		try {
			c = db.query(AppInfoTable.DATABASE_TABLE_NAME, new String[] {
					AppInfoTable.KEY_APP_ADMOB_ACCOUNT, AppInfoTable.KEY_APP_ADMOB_SITE_ID },
					AppInfoTable.KEY_APP_PACKAGENAME + "=?", new String[] { packageName }, null,
					null, null);
			if (!c.moveToNext()) {
				return null;
			}

			String[] result = new String[2];
			result[0] = c.getString(c.getColumnIndex(AppInfoTable.KEY_APP_ADMOB_ACCOUNT));
			result[1] = c.getString(c.getColumnIndex(AppInfoTable.KEY_APP_ADMOB_SITE_ID));
			if (result[0] == null || result[1] == null) {
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
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
			long id = findPackageId(db, packageName);

			ContentValues values = new ContentValues();
			values.put(AppInfoTable.KEY_APP_ADMOB_ACCOUNT, admobAccount);
			values.put(AppInfoTable.KEY_APP_ADMOB_SITE_ID, admobSiteId);

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
			if (c.getCount() != 1 || !c.moveToNext()) {
				throw new IllegalStateException("Package name not found in AppInfo table: "
						+ packageName);
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
			if (c.getCount() != 1 || !c.moveToNext()) {
				throw new IllegalStateException("Package name not found in AppInfo table: "
						+ packageName);
			}

			return c.getLong(c.getColumnIndex(AppInfoTable.KEY_ROWID));
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}
}
