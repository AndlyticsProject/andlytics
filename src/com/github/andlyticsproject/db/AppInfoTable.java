package com.github.andlyticsproject.db;

import java.util.HashMap;

import android.net.Uri;

public class AppInfoTable {

	public static final String DATABASE_TABLE_NAME = "appinfo";
	public static final String UNIQUE_PACKAGE_NAMES = "appinfo/unique-packages";

	public static final Uri CONTENT_URI = Uri.parse("content://"
			+ AndlyticsContentProvider.AUTHORITY + "/" + DATABASE_TABLE_NAME);
	public static final Uri UNIQUE_PACAKGES_CONTENT_URI = Uri.parse("content://"
			+ AndlyticsContentProvider.AUTHORITY + "/" + UNIQUE_PACKAGE_NAMES);

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.andlytics."
			+ DATABASE_TABLE_NAME;

	public static final String KEY_ROWID = "_id";
	public static final String KEY_APP_PACKAGENAME = "packagename";
	public static final String KEY_APP_ACCOUNT = "account";
	public static final String KEY_APP_LASTUPDATE = "lastupdate";
	public static final String KEY_APP_NAME = "name";
	public static final String KEY_APP_CATEGORY = "category";
	public static final String KEY_APP_PUBLISHSTATE = "publishstate";
	public static final String KEY_APP_ICONURL = "iconurl";
	public static final String KEY_APP_GHOST = "ghost";
	public static final String KEY_APP_RATINGS_EXPANDED = "ratingsexpanded";
	public static final String KEY_APP_SKIP_NOTIFICATION = "skipnotification";
	public static final String KEY_APP_VERSION_NAME = "versionname";

	public static final String KEY_APP_ADMOB_ACCOUNT = "admobaccount";
	public static final String KEY_APP_ADMOB_SITE_ID = "admobsiteid";
	public static final String KEY_APP_LAST_COMMENTS_UPDATE = "lastcommentsupdate";

	public static final String KEY_APP_DEVELOPER_ID = "developerid";

	public static final String TABLE_CREATE_APPINFO = "create table " + DATABASE_TABLE_NAME
			+ " (_id integer primary key autoincrement, " + KEY_APP_PACKAGENAME + " text not null,"
			+ KEY_APP_ACCOUNT + " text not null," + KEY_APP_LASTUPDATE + " date," + KEY_APP_NAME
			+ " text," + KEY_APP_ICONURL + " text," + KEY_APP_CATEGORY + " text,"
			+ KEY_APP_PUBLISHSTATE + " integer," + KEY_APP_GHOST + " integer,"
			+ KEY_APP_RATINGS_EXPANDED + " integer," + KEY_APP_SKIP_NOTIFICATION + " integer,"
			+ KEY_APP_VERSION_NAME + " text, " + KEY_APP_ADMOB_ACCOUNT + " text, "
			+ KEY_APP_ADMOB_SITE_ID + " text, " + KEY_APP_LAST_COMMENTS_UPDATE + " date, "
			+ KEY_APP_DEVELOPER_ID + " text)";

	public static HashMap<String, String> PROJECTION_MAP;
	public static HashMap<String, String> PACKAGE_NAMES_MAP;

	static {
		PROJECTION_MAP = new HashMap<String, String>();

		PROJECTION_MAP.put(AppInfoTable.KEY_ROWID, AppInfoTable.KEY_ROWID);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_PACKAGENAME, AppInfoTable.KEY_APP_PACKAGENAME);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_ACCOUNT, AppInfoTable.KEY_APP_ACCOUNT);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_LASTUPDATE, AppInfoTable.KEY_APP_LASTUPDATE);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_NAME, AppInfoTable.KEY_APP_NAME);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_CATEGORY, AppInfoTable.KEY_APP_CATEGORY);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_PUBLISHSTATE, AppInfoTable.KEY_APP_PUBLISHSTATE);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_ICONURL, AppInfoTable.KEY_APP_ICONURL);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_GHOST, AppInfoTable.KEY_APP_GHOST);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_RATINGS_EXPANDED,
				AppInfoTable.KEY_APP_RATINGS_EXPANDED);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_SKIP_NOTIFICATION,
				AppInfoTable.KEY_APP_SKIP_NOTIFICATION);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_VERSION_NAME, AppInfoTable.KEY_APP_VERSION_NAME);

		PROJECTION_MAP.put(AppInfoTable.KEY_APP_ADMOB_ACCOUNT, AppInfoTable.KEY_APP_ADMOB_ACCOUNT);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_ADMOB_SITE_ID, AppInfoTable.KEY_APP_ADMOB_SITE_ID);
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_LAST_COMMENTS_UPDATE,
				AppInfoTable.KEY_APP_LAST_COMMENTS_UPDATE);
		
		PROJECTION_MAP.put(AppInfoTable.KEY_APP_DEVELOPER_ID, AppInfoTable.KEY_APP_DEVELOPER_ID);

		PACKAGE_NAMES_MAP = new HashMap<String, String>();
		PACKAGE_NAMES_MAP.put(AppInfoTable.KEY_APP_PACKAGENAME, AppInfoTable.KEY_APP_PACKAGENAME);
	}

}
