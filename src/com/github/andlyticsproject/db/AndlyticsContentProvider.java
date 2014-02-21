package com.github.andlyticsproject.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

@SuppressLint("SimpleDateFormat")
public class AndlyticsContentProvider extends ContentProvider {

	private static final int ID_TABLE_STATS = 0;
	private static final int ID_TABLE_APPINFO = 1;
	private static final int ID_TABLE_COMMENTS = 2;
	private static final int ID_TABLE_ADMOB = 3;
	private static final int ID_APP_VERSION_CHANGE = 4;
	private static final int ID_UNIQUE_PACKAGES = 5;
	private static final int ID_TABLE_LINKS = 6;
	private static final int ID_TABLE_REVENUE_SUMMARY = 7;

	public static final String AUTHORITY = "com.github.andlyticsproject.db.AndlyticsContentProvider";

	private static final UriMatcher sUriMatcher;

	public static final String APP_VERSION_CHANGE = "appVersionChange";

	private AndlyticsDb dbHelper;

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case ID_TABLE_STATS:
			count = db.delete(AppStatsTable.DATABASE_TABLE_NAME, where, whereArgs);
			break;
		case ID_TABLE_APPINFO:
			count = db.delete(AppInfoTable.DATABASE_TABLE_NAME, where, whereArgs);
			break;
		case ID_TABLE_COMMENTS:
			count = db.delete(CommentsTable.DATABASE_TABLE_NAME, where, whereArgs);
			break;
		case ID_TABLE_ADMOB:
			count = db.delete(AdmobTable.DATABASE_TABLE_NAME, where, whereArgs);
			break;
		case ID_TABLE_LINKS:
			count = db.delete(LinksTable.DATABASE_TABLE_NAME, where, whereArgs);
			break;
		case ID_TABLE_REVENUE_SUMMARY:
			count = db.delete(RevenueSummaryTable.DATABASE_TABLE_NAME, where, whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case ID_TABLE_STATS:
			return AppStatsTable.CONTENT_TYPE;
		case ID_TABLE_APPINFO:
			return AppInfoTable.CONTENT_TYPE;
		case ID_TABLE_COMMENTS:
			return CommentsTable.CONTENT_TYPE;
		case ID_TABLE_ADMOB:
			return AdmobTable.CONTENT_TYPE;
		case ID_APP_VERSION_CHANGE:
			return APP_VERSION_CHANGE;
		case ID_TABLE_REVENUE_SUMMARY:
			return RevenueSummaryTable.CONTENT_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = -1;

		switch (sUriMatcher.match(uri)) {
		case ID_TABLE_STATS:

			rowId = getAppStatsIdByDate(values.getAsString(AppStatsTable.KEY_STATS_PACKAGENAME),
					parseDate(values.getAsString(AppStatsTable.KEY_STATS_REQUESTDATE)), db);
			if (rowId > -1) {

				db.update(AppStatsTable.DATABASE_TABLE_NAME, values, AppStatsTable.KEY_ROWID + "="
						+ rowId, null);

			} else {

				rowId = db.insert(AppStatsTable.DATABASE_TABLE_NAME, null, values);
			}
			if (rowId > 0) {
				Uri noteUri = ContentUris.withAppendedId(AppStatsTable.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(noteUri, null);
				return noteUri;
			}

		case ID_TABLE_APPINFO:

			Cursor mCursor = db.query(AppInfoTable.DATABASE_TABLE_NAME, new String[] {
					AppInfoTable.KEY_ROWID, AppInfoTable.KEY_APP_ACCOUNT,
					AppInfoTable.KEY_APP_PACKAGENAME }, AppInfoTable.KEY_APP_PACKAGENAME + "='"
					+ values.getAsString(AppInfoTable.KEY_APP_PACKAGENAME) + "'", null, null, null,
					null);

			if (mCursor != null && mCursor.moveToFirst()) {
				rowId = mCursor.getInt(mCursor.getColumnIndex(AppInfoTable.KEY_ROWID));
			}
			mCursor.close();

			if (rowId > -1) {
				db.update(AppInfoTable.DATABASE_TABLE_NAME, initialValues, AppInfoTable.KEY_ROWID
						+ "=" + rowId, null);

			} else {

				rowId = db.insert(AppInfoTable.DATABASE_TABLE_NAME, null, values);
			}

			if (rowId > 0) {
				Uri noteUri = ContentUris.withAppendedId(AppInfoTable.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(noteUri, null);
				return noteUri;
			}

		case ID_TABLE_COMMENTS:

			rowId = db.insert(CommentsTable.DATABASE_TABLE_NAME, null, values);
			if (rowId > 0) {
				Uri noteUri = ContentUris.withAppendedId(CommentsTable.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(noteUri, null);
				return noteUri;
			}

		case ID_TABLE_ADMOB:

			rowId = db.insert(AdmobTable.DATABASE_TABLE_NAME, null, values);
			if (rowId > 0) {
				Uri noteUri = ContentUris.withAppendedId(AdmobTable.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(noteUri, null);
				return noteUri;
			}
		case ID_TABLE_REVENUE_SUMMARY:
			rowId = db.insert(RevenueSummaryTable.DATABASE_TABLE_NAME, null, values);
			if (rowId > 0) {
				Uri noteUri = ContentUris.withAppendedId(RevenueSummaryTable.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(noteUri, null);
				return noteUri;
			}
		}


		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public boolean onCreate() {
		dbHelper = AndlyticsDb.getInstance(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		String groupBy = null;

		switch (sUriMatcher.match(uri)) {
		case ID_TABLE_STATS:
			qb.setTables(AppStatsTable.DATABASE_TABLE_NAME);
			groupBy = AppStatsTable.KEY_STATS_REQUESTDATE; // fix for duplicate entries
			qb.setProjectionMap(AppStatsTable.PROJECTION_MAP);
			break;
		case ID_TABLE_APPINFO:
			qb.setTables(AppInfoTable.DATABASE_TABLE_NAME);
			groupBy = AppInfoTable.KEY_APP_PACKAGENAME; // fix for duplicate entries
			qb.setProjectionMap(AppInfoTable.PROJECTION_MAP);
			break;
		case ID_TABLE_COMMENTS:
			qb.setTables(CommentsTable.DATABASE_TABLE_NAME);
			qb.setProjectionMap(CommentsTable.PROJECTION_MAP);
			break;
		case ID_TABLE_ADMOB:
			qb.setTables(AdmobTable.DATABASE_TABLE_NAME);
			qb.setProjectionMap(AdmobTable.PROJECTION_MAP);
			break;
		case ID_APP_VERSION_CHANGE:
			qb.setTables(AppStatsTable.DATABASE_TABLE_NAME);
			qb.setProjectionMap(AppStatsTable.PROJECTION_MAP);
			break;
		case ID_UNIQUE_PACKAGES:
			qb.setTables(AppInfoTable.DATABASE_TABLE_NAME);
			qb.setProjectionMap(AppInfoTable.PACKAGE_NAMES_MAP);
			qb.setDistinct(true);
			break;
		case ID_TABLE_REVENUE_SUMMARY:
			qb.setTables(RevenueSummaryTable.DATABASE_TABLE_NAME);
			qb.setProjectionMap(RevenueSummaryTable.PROJECTION_MAP);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// TODO What is the best way to deal with upgrades while still
		// only getting write access if we really need it?
		SQLiteDatabase db = null;
		try {
			db = dbHelper.getReadableDatabase();
		} catch (SQLException ex) {
			if (ex.getMessage().startsWith("Can't upgrade read-only database")) {
				db = dbHelper.getWritableDatabase();
			} else {
				throw ex;
			}
		}

		Cursor c = null;

		if (sUriMatcher.match(uri) == ID_APP_VERSION_CHANGE) {
			c = qb.query(db, projection, selection, selectionArgs,
					AppStatsTable.KEY_STATS_VERSIONCODE, null, sortOrder);

		} else {
			c = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder);
		}

		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case ID_TABLE_STATS:
			count = db.update(AppStatsTable.DATABASE_TABLE_NAME, values, where, whereArgs);
			break;
		case ID_TABLE_APPINFO:
			count = db.update(AppInfoTable.DATABASE_TABLE_NAME, values, where, whereArgs);
			break;
		case ID_TABLE_COMMENTS:
			count = db.update(CommentsTable.DATABASE_TABLE_NAME, values, where, whereArgs);
			break;
		case ID_TABLE_LINKS:
			count = db.update(LinksTable.DATABASE_TABLE_NAME, values, where, whereArgs);
			break;
		case ID_TABLE_ADMOB:
			count = db.update(AdmobTable.DATABASE_TABLE_NAME, values, where, whereArgs);
			break;
		case ID_TABLE_REVENUE_SUMMARY:
			count = db.update(RevenueSummaryTable.DATABASE_TABLE_NAME, values, where, whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, AppStatsTable.DATABASE_TABLE_NAME, ID_TABLE_STATS);
		sUriMatcher.addURI(AUTHORITY, AppInfoTable.DATABASE_TABLE_NAME, ID_TABLE_APPINFO);
		sUriMatcher.addURI(AUTHORITY, AppInfoTable.UNIQUE_PACKAGE_NAMES, ID_UNIQUE_PACKAGES);
		sUriMatcher.addURI(AUTHORITY, CommentsTable.DATABASE_TABLE_NAME, ID_TABLE_COMMENTS);
		sUriMatcher.addURI(AUTHORITY, LinksTable.DATABASE_TABLE_NAME, ID_TABLE_LINKS);
		sUriMatcher.addURI(AUTHORITY, AdmobTable.DATABASE_TABLE_NAME, ID_TABLE_ADMOB);
		sUriMatcher.addURI(AUTHORITY, APP_VERSION_CHANGE, ID_APP_VERSION_CHANGE);
		sUriMatcher.addURI(AUTHORITY, RevenueSummaryTable.DATABASE_TABLE_NAME,
				ID_TABLE_REVENUE_SUMMARY);
	}

	public long getAppStatsIdByDate(String packagename, Date date, SQLiteDatabase db)
			throws SQLException {

		long result = -1;

		// make sure there is only one entry / day
		SimpleDateFormat dateFormatStart = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
		SimpleDateFormat dateFormatEnd = new SimpleDateFormat("yyyy-MM-dd 23:59:59");

		Cursor mCursor = db.query(
				AppStatsTable.DATABASE_TABLE_NAME,
				new String[] { AppStatsTable.KEY_ROWID, AppStatsTable.KEY_STATS_REQUESTDATE },
				AppStatsTable.KEY_STATS_PACKAGENAME + "='" + packagename + "' and "
						+ AppStatsTable.KEY_STATS_REQUESTDATE + " BETWEEN '"
						+ dateFormatStart.format(date) + "' and '" + dateFormatEnd.format(date)
						+ "'", null, null, null, null);
		if (mCursor != null && mCursor.moveToFirst()) {

			result = mCursor.getInt(mCursor.getColumnIndex(AppStatsTable.KEY_ROWID));
		}

		mCursor.close();

		return result;
	}

	private Date parseDate(String string) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			return dateFormat.parse(string);
		} catch (ParseException e) {
			return null;
		}
	}

}