package com.github.andlyticsproject.db;


public class LinksTable {

	public static final String DATABASE_TABLE_NAME = "links";

	public static final String ROWID = "_id";
	public static final String LINK_NAME = "name";
	public static final String LINK_URL = "url";
	public static final String APP_DETAILS_ID = "app_details_id";

	public static final String TABLE_CREATE_LINKS = "create table " + DATABASE_TABLE_NAME
			+ " (_id integer primary key autoincrement, " + LINK_NAME + " text not null,"
			+ LINK_URL + " text, " + APP_DETAILS_ID
			+ " integer not null, foreign key(app_details_id) references app_details(_id))";

	public static final String[] ALL_COLUMNS = { ROWID, LINK_NAME, LINK_URL, APP_DETAILS_ID, };

}
