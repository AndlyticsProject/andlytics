package com.github.andlyticsproject.db;

import java.util.HashMap;

import android.net.Uri;

public class RevenueSummaryTable {

	public static final String DATABASE_TABLE_NAME = "revenue_summary";

	public static final Uri CONTENT_URI = Uri.parse("content://"
			+ AndlyticsContentProvider.AUTHORITY + "/" + DATABASE_TABLE_NAME);

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.andlytics."
			+ DATABASE_TABLE_NAME;

	public static final String ROWID = "_id";
	public static final String TYPE = "type";
	public static final String CURRENCY = "currency";
	public static final String DATE = "date";
	public static final String LAST_DAY_TOTAL = "last_day_total";
	public static final String LAST_7DAYS_TOTAL = "last_7days_total";
	public static final String LAST_30DAYS_TOTAL = "last_30days_total";
	public static final String OVERALL_TOTAL = "overall_total";
	public static final String APPINFO_ID = "appinfo_id";

	public static final String TABLE_CREATE_REVENUE_SUMMARY = "create table " + DATABASE_TABLE_NAME
			+ " (_id integer primary key autoincrement, " + TYPE + " integer not null, " + CURRENCY
			+ " text not null, " + DATE + " date, " + LAST_DAY_TOTAL + " double not null, "
			+ LAST_7DAYS_TOTAL + " double not null, " + LAST_30DAYS_TOTAL + " double not null, "
			+ OVERALL_TOTAL + " double not null, " + APPINFO_ID
			+ " integer not null, foreign key(appinfo_id) references appinfo(_id))";

	public static final String[] ALL_COLUMNS = { ROWID, TYPE, CURRENCY, DATE, LAST_DAY_TOTAL,
			LAST_7DAYS_TOTAL, LAST_30DAYS_TOTAL, OVERALL_TOTAL, APPINFO_ID };

	public static HashMap<String, String> PROJECTION_MAP;

	static {
		PROJECTION_MAP = new HashMap<String, String>();

		PROJECTION_MAP.put(RevenueSummaryTable.ROWID, RevenueSummaryTable.ROWID);
		PROJECTION_MAP.put(RevenueSummaryTable.TYPE, RevenueSummaryTable.TYPE);
		PROJECTION_MAP.put(RevenueSummaryTable.CURRENCY, RevenueSummaryTable.CURRENCY);
		PROJECTION_MAP.put(RevenueSummaryTable.DATE, RevenueSummaryTable.DATE);
		PROJECTION_MAP.put(RevenueSummaryTable.LAST_DAY_TOTAL, RevenueSummaryTable.LAST_DAY_TOTAL);
		PROJECTION_MAP.put(RevenueSummaryTable.LAST_7DAYS_TOTAL,
				RevenueSummaryTable.LAST_7DAYS_TOTAL);
		PROJECTION_MAP.put(RevenueSummaryTable.LAST_30DAYS_TOTAL,
				RevenueSummaryTable.LAST_30DAYS_TOTAL);
		PROJECTION_MAP.put(RevenueSummaryTable.OVERALL_TOTAL, RevenueSummaryTable.OVERALL_TOTAL);
		PROJECTION_MAP.put(RevenueSummaryTable.APPINFO_ID, RevenueSummaryTable.APPINFO_ID);
	}
}
