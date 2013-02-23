
package com.github.andlyticsproject.db;

import android.net.Uri;

import java.util.HashMap;

public class LinksTable {

	public static final String DATABASE_TABLE_NAME = "links";

	public static final Uri CONTENT_URI = Uri.parse("content://"
			+ AndlyticsContentProvider.AUTHORITY + "/" + DATABASE_TABLE_NAME);

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.andlytics."
			+ DATABASE_TABLE_NAME;

	public static final String KEY_ROWID = "_id";
	public static final String KEY_LINK_PACKAGENAME = "packagename";
	public static final String KEY_LINK_NAME = "name";
	public static final String KEY_LINK_URL = "url";

	public static final String TABLE_CREATE_LINKS = "create table " + DATABASE_TABLE_NAME
			+ " (_id integer primary key autoincrement, " + KEY_LINK_PACKAGENAME
			+ " text not null," + KEY_LINK_NAME + " text not null," + KEY_LINK_URL
			+ " text)";

	public static HashMap<String, String> PROJECTION_MAP;

	static {
		PROJECTION_MAP = new HashMap<String, String>();

		PROJECTION_MAP.put(LinksTable.KEY_ROWID, LinksTable.KEY_ROWID);
		PROJECTION_MAP.put(LinksTable.KEY_LINK_PACKAGENAME,
				LinksTable.KEY_LINK_PACKAGENAME);
		PROJECTION_MAP.put(LinksTable.KEY_LINK_NAME, LinksTable.KEY_LINK_NAME);
		PROJECTION_MAP.put(LinksTable.KEY_LINK_URL, LinksTable.KEY_LINK_URL);
	}

}
