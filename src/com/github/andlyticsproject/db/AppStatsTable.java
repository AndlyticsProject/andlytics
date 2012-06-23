package com.github.andlyticsproject.db;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.HashMap;

public final class AppStatsTable implements BaseColumns {

    
    public static final String DATABASE_TABLE_NAME = "appstats";
    
    public static final Uri CONTENT_URI = Uri.parse("content://" + AndlyticsContentProvider.AUTHORITY + "/"
                    + DATABASE_TABLE_NAME);

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.andlytics." + DATABASE_TABLE_NAME;

    public static final String KEY_ROWID = "_id";
    public static final String KEY_STATS_PACKAGENAME = "packagename";
    public static final String KEY_STATS_REQUESTDATE = "requestdate";
    public static final String KEY_STATS_DOWNLOADS = "downloads";
    public static final String KEY_STATS_INSTALLS = "installs";
    public static final String KEY_STATS_COMMENTS = "comments";
    public static final String KEY_STATS_MARKETERANKING = "marketranking";
    public static final String KEY_STATS_CATEGORYRANKING = "categoryranking";
    public static final String KEY_STATS_5STARS = "starsfive";
    public static final String KEY_STATS_4STARS = "starsfour";
    public static final String KEY_STATS_3STARS = "starsthree";
    public static final String KEY_STATS_2STARS = "starstwo";
    public static final String KEY_STATS_1STARS = "starsone";
    public static final String KEY_STATS_VERSIONCODE = "versioncode";


    public static final String TABLE_CREATE_STATS = "create table "  + AppStatsTable.DATABASE_TABLE_NAME + " (_id integer primary key autoincrement, "
        + AppStatsTable.KEY_STATS_PACKAGENAME + " text not null,"
        + AppStatsTable.KEY_STATS_REQUESTDATE + " date not null,"
        + AppStatsTable.KEY_STATS_DOWNLOADS + " integer,"
        + AppStatsTable.KEY_STATS_INSTALLS  + " integer,"
        + AppStatsTable.KEY_STATS_COMMENTS  + " integer,"
        + AppStatsTable.KEY_STATS_MARKETERANKING  + " integer,"
        + AppStatsTable.KEY_STATS_CATEGORYRANKING  + " integer,"
        + AppStatsTable.KEY_STATS_5STARS  + " integer,"
        + AppStatsTable.KEY_STATS_4STARS  + " integer,"
        + AppStatsTable.KEY_STATS_3STARS  + " integer,"
        + AppStatsTable.KEY_STATS_2STARS  + " integer,"
        + AppStatsTable.KEY_STATS_1STARS  + " integer,"
        + AppStatsTable.KEY_STATS_VERSIONCODE  + " integer);";   

    
    public static HashMap<String, String> PROJECTION_MAP;
    
    static {
        PROJECTION_MAP = new HashMap<String, String>();
        
        PROJECTION_MAP.put(AppStatsTable.KEY_ROWID,AppStatsTable.KEY_ROWID);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_PACKAGENAME,AppStatsTable.KEY_STATS_PACKAGENAME);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_REQUESTDATE,AppStatsTable.KEY_STATS_REQUESTDATE);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_DOWNLOADS,AppStatsTable.KEY_STATS_DOWNLOADS);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_INSTALLS,AppStatsTable.KEY_STATS_INSTALLS);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_COMMENTS,AppStatsTable.KEY_STATS_COMMENTS);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_MARKETERANKING,AppStatsTable.KEY_STATS_MARKETERANKING);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_CATEGORYRANKING,AppStatsTable.KEY_STATS_CATEGORYRANKING);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_5STARS,AppStatsTable.KEY_STATS_5STARS);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_4STARS,AppStatsTable.KEY_STATS_4STARS);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_3STARS,AppStatsTable.KEY_STATS_3STARS);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_2STARS,AppStatsTable.KEY_STATS_2STARS);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_1STARS,AppStatsTable.KEY_STATS_1STARS);
        PROJECTION_MAP.put(AppStatsTable.KEY_STATS_VERSIONCODE,AppStatsTable.KEY_STATS_VERSIONCODE);
    }
    
    
}