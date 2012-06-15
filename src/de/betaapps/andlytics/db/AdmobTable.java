package de.betaapps.andlytics.db;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.HashMap;

public final class AdmobTable implements BaseColumns {

    
    public static final String DATABASE_TABLE_NAME = "admob";
    
    public static final Uri CONTENT_URI = Uri.parse("content://" + AndlyticsContentProvider.AUTHORITY + "/"
                    + DATABASE_TABLE_NAME);

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.andlytics." + DATABASE_TABLE_NAME;

    public static final String KEY_ROWID = "_id";
    
    public static final String KEY_SITE_ID = "site_id";
    public static final String KEY_REQUESTS = "requests";
    public static final String KEY_HOUSEAD_REQUESTS = "housead_requests";
    public static final String KEY_INTERSTITIAL_REQUESTS = "interstitial_requests";
    public static final String KEY_IMPRESSIONS = "impressions";
    public static final String KEY_FILL_RATE = "fill_rate";
    public static final String KEY_HOUSEAD_FILL_RATE = "housead_fill_rate";
    public static final String KEY_OVERALL_FILL_RATE = "overall_fill_rate";
    public static final String KEY_CLICKS = "clicks";
    public static final String KEY_HOUSEAD_CLICKS= "housead_clicks";
    public static final String KEY_CTR = "ctr";
    public static final String KEY_ECPM = "ecpm";
    public static final String KEY_REVENUE = "revenue";
    public static final String KEY_CPC_REVENUE = "cpc_revenue";
    public static final String KEY_CPM_REVENUE = "cpm_revenue";
    public static final String KEY_EXCHANGE_DOWNLOADS = "exchange_downloads";
    public static final String KEY_DATE = "date";


    public static final String TABLE_CREATE_ADMOB = "create table "  + AdmobTable.DATABASE_TABLE_NAME + " (_id integer primary key autoincrement, "
        + AdmobTable.KEY_SITE_ID + " text not null,"
        + AdmobTable.KEY_REQUESTS + " integer,"
        + AdmobTable.KEY_HOUSEAD_REQUESTS + " integer,"
        + AdmobTable.KEY_INTERSTITIAL_REQUESTS + " integer,"
        + AdmobTable.KEY_IMPRESSIONS + " integer,"
        + AdmobTable.KEY_FILL_RATE + " float,"
        + AdmobTable.KEY_HOUSEAD_FILL_RATE + " float,"
        + AdmobTable.KEY_OVERALL_FILL_RATE + " float,"
        + AdmobTable.KEY_CLICKS + " integer,"
        + AdmobTable.KEY_HOUSEAD_CLICKS + " integer,"
        + AdmobTable.KEY_CTR + " float,"
        + AdmobTable.KEY_ECPM + " float,"
        + AdmobTable.KEY_REVENUE + " float,"
        + AdmobTable.KEY_CPC_REVENUE + " float,"
        + AdmobTable.KEY_CPM_REVENUE + " float,"
        + AdmobTable.KEY_EXCHANGE_DOWNLOADS + " integer,"
        + AdmobTable.KEY_DATE + " date not null)";  
        
            
    public static HashMap<String, String> PROJECTION_MAP;
    
    static {
        PROJECTION_MAP = new HashMap<String, String>();
        
        PROJECTION_MAP.put(KEY_ROWID,KEY_ROWID);
        PROJECTION_MAP.put(KEY_SITE_ID,KEY_SITE_ID);
        PROJECTION_MAP.put(KEY_REQUESTS,KEY_REQUESTS);
        PROJECTION_MAP.put(KEY_HOUSEAD_REQUESTS,KEY_HOUSEAD_REQUESTS);
        PROJECTION_MAP.put(KEY_INTERSTITIAL_REQUESTS,KEY_INTERSTITIAL_REQUESTS);
        PROJECTION_MAP.put(KEY_IMPRESSIONS,KEY_IMPRESSIONS);
        PROJECTION_MAP.put(KEY_FILL_RATE,KEY_FILL_RATE);
        PROJECTION_MAP.put(KEY_HOUSEAD_FILL_RATE,KEY_HOUSEAD_FILL_RATE);
        PROJECTION_MAP.put(KEY_OVERALL_FILL_RATE,KEY_OVERALL_FILL_RATE);
        PROJECTION_MAP.put(KEY_CLICKS,KEY_CLICKS);
        PROJECTION_MAP.put(KEY_HOUSEAD_CLICKS,KEY_HOUSEAD_CLICKS);
        PROJECTION_MAP.put(KEY_CTR,KEY_CTR);
        PROJECTION_MAP.put(KEY_ECPM,KEY_ECPM);
        PROJECTION_MAP.put(KEY_REVENUE,KEY_REVENUE);
        PROJECTION_MAP.put(KEY_CPC_REVENUE,KEY_CPC_REVENUE);
        PROJECTION_MAP.put(KEY_CPM_REVENUE,KEY_CPM_REVENUE);
        PROJECTION_MAP.put(KEY_EXCHANGE_DOWNLOADS,KEY_EXCHANGE_DOWNLOADS);
        PROJECTION_MAP.put(KEY_DATE,KEY_DATE);
    }
    
    
}