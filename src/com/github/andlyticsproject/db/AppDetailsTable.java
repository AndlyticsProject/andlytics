package com.github.andlyticsproject.db;

public class AppDetailsTable {

	public static final String DATABASE_TABLE_NAME = "app_details";

	public static final String ROWID = "_id";
	public static final String DESCRIPTION = "description";
	public static final String CHANGELOG = "changelog";
	public static final String LAST_STORE_UPDATE = "last_store_update";
	public static final String APPINFO_ID = "appinfo_id";

	public static final String TABLE_CREATE_APP_DETAILS = "create table " + DATABASE_TABLE_NAME
			+ " (_id integer primary key autoincrement, " + DESCRIPTION + " text not null, "
			+ CHANGELOG + " text," + LAST_STORE_UPDATE + " date not null, " + APPINFO_ID
			+ " integer not null, foreign key(appinfo_id) references appinfo(_id))";

	public static final String[] ALL_COLUMNS = { ROWID, DESCRIPTION, CHANGELOG, LAST_STORE_UPDATE,
			APPINFO_ID };
}
