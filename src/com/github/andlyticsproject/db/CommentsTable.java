
package com.github.andlyticsproject.db;

import java.util.HashMap;

import android.net.Uri;

public class CommentsTable {

	public static final String DATABASE_TABLE_NAME = "comments";

	public static final Uri CONTENT_URI = Uri.parse("content://"
			+ AndlyticsContentProvider.AUTHORITY + "/" + DATABASE_TABLE_NAME);

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.andlytics."
			+ DATABASE_TABLE_NAME;

	public static final String KEY_ROWID = "_id";
	public static final String KEY_COMMENT_PACKAGENAME = "packagename";
	public static final String KEY_COMMENT_TEXT = "text";
	public static final String KEY_COMMENT_DATE = "date";
	public static final String KEY_COMMENT_USER = "user";
	public static final String KEY_COMMENT_RATING = "rating";
	public static final String KEY_COMMENT_DEVICE = "device";
	public static final String KEY_COMMENT_APP_VERSION = "app_version";
	public static final String KEY_COMMENT_REPLY_TEXT = "reply_text";
	public static final String KEY_COMMENT_REPLY_DATE = "reply_date";
	public static final String KEY_COMMENT_LANGUAGE = "language";
	public static final String KEY_COMMENT_ORIGINAL_TEXT = "original_text";

	public static final String TABLE_CREATE_COMMENTS = "create table " + DATABASE_TABLE_NAME
			+ " (_id integer primary key autoincrement, " + KEY_COMMENT_PACKAGENAME
			+ " text not null," + KEY_COMMENT_DATE + " text not null," + KEY_COMMENT_USER
			+ " text," + KEY_COMMENT_DEVICE + " text," + KEY_COMMENT_APP_VERSION + " text,"
			+ KEY_COMMENT_TEXT + " text not null," + KEY_COMMENT_RATING + " integer,"
			+ KEY_COMMENT_REPLY_TEXT + " text," + KEY_COMMENT_REPLY_DATE + " text, "
			+ KEY_COMMENT_LANGUAGE + " text," + KEY_COMMENT_ORIGINAL_TEXT + " text)";

	public static HashMap<String, String> PROJECTION_MAP;

	static {
		PROJECTION_MAP = new HashMap<String, String>();

		PROJECTION_MAP.put(CommentsTable.KEY_ROWID, CommentsTable.KEY_ROWID);
		PROJECTION_MAP.put(CommentsTable.KEY_COMMENT_PACKAGENAME,
				CommentsTable.KEY_COMMENT_PACKAGENAME);
		PROJECTION_MAP.put(CommentsTable.KEY_COMMENT_TEXT, CommentsTable.KEY_COMMENT_TEXT);
		PROJECTION_MAP.put(CommentsTable.KEY_COMMENT_DATE, CommentsTable.KEY_COMMENT_DATE);
		PROJECTION_MAP.put(CommentsTable.KEY_COMMENT_USER, CommentsTable.KEY_COMMENT_USER);
		PROJECTION_MAP.put(CommentsTable.KEY_COMMENT_RATING, CommentsTable.KEY_COMMENT_RATING);
		PROJECTION_MAP.put(CommentsTable.KEY_COMMENT_APP_VERSION,
				CommentsTable.KEY_COMMENT_APP_VERSION);
		PROJECTION_MAP.put(CommentsTable.KEY_COMMENT_DEVICE, CommentsTable.KEY_COMMENT_DEVICE);
		PROJECTION_MAP.put(CommentsTable.KEY_COMMENT_REPLY_TEXT, CommentsTable.KEY_COMMENT_REPLY_TEXT);
		PROJECTION_MAP.put(CommentsTable.KEY_COMMENT_REPLY_DATE, CommentsTable.KEY_COMMENT_REPLY_DATE);
		PROJECTION_MAP.put(CommentsTable.KEY_COMMENT_LANGUAGE, CommentsTable.KEY_COMMENT_LANGUAGE);
		PROJECTION_MAP.put(CommentsTable.KEY_COMMENT_ORIGINAL_TEXT,
				CommentsTable.KEY_COMMENT_ORIGINAL_TEXT);

	}

}
