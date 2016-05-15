package com.github.andlyticsproject.db;


public class DeveloperAccountsTable {

	public static final String DATABASE_TABLE_NAME = "developer_accounts";

	public static final String ROWID = "_id";
	public static final String NAME = "name";
	public static final String STATE = "state";
	public static final String LAST_STATS_UPDATE = "last_stats_update";
	public static final String DEVELOPER_ID = "developerid";

	public static final String TABLE_CREATE_DEVELOPER_ACCOUNT = "create table "
			+ DATABASE_TABLE_NAME + " (_id integer primary key autoincrement, " + NAME
			+ " text unique not null, " + STATE + " integer not null," + LAST_STATS_UPDATE
			+ " date)";//, " + DEVELOPER_ID + "text)";

	public static final String[] ALL_COLUMNS = { ROWID, NAME, STATE, LAST_STATS_UPDATE, };
	//
	//			DEVELOPER_ID };
}
