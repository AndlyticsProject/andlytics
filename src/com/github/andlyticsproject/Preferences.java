package com.github.andlyticsproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class Preferences {

	public static final String PREF = "andlytics_pref";
	
	// Keys used on the main preference screen
	public static final String PREF_AUTO_SYNC_PERIOD = "prefAutoSyncPeriod";
	public static final String PREF_NOTIFICATIONS = "prefNotifications";
	public static final String PREF_HIDDEN_APPS = "prefHiddenApps";

	private static final String HIDDEN_ACCOUNT = "hiddenAccount";
	private static final String ACCOUNT_NAME = "accountName";
	private static final String GWTPERMUTATION = "permutation";

	private static final String POST_REQUEST_GET_FULL_ASSET_INFOS = "post_full_asset_info";
	private static final String POST_REQUEST_GET_USER_INFO_SIZE = "post_user_info_size";
	private static final String POST_REQUEST_USER_COMMENTS = "post_user_comments";
    private static final String POST_REQUEST_FEEDBACK = "post_feedback";

	private static final String AUTOSYNC = "autosync.initial.set";
	private static final String CRASH_REPORT_DISABLE = "acra.enable";

	public static final String CHART_TIMEFRAME = "chart.timeframe";
    public static final String ADMOB_TIMEFRAME = "admob.timeframe";

	private static final String CHART_SMOOTH = "chart.smooth";
	private static final String SKIP_AUTO_LOGIN = "skip.auto.login";
    private static final String STATS_MODE = "stats.mode";

    public static final String NOTIFICATION_CHANGES_RATING = "notification.changes.rating";
    public static final String NOTIFICATION_CHANGES_COMMENTS = "notification.changes.comments";
    public static final String NOTIFICATION_CHANGES_DOWNLOADS = "notification.changes.download";
    public static final String NOTIFICATION_SOUND = "notification.sound";
    public static final String NOTIFICATION_LIGHT = "notification.light";
    public static final String NOTIFICATION_WHEN_ACCOUNT_VISISBLE = "notification.when_account_visible";

    public static final String DATE_FORMAT_SHORT = "dateformat.short1";
    public static final String DATE_FORMAT_LONG = "dateformat.long1";

    private static final String LEVEL_7_ALARM_MANAGER_PERIOD = "level7.AlarmManagerPeriod";

    private static final String ADMOB_SITE_ID = "admob.siteid";

    private static final String ADMOB_ACCOUNT = "admob.account";

    private static final String SHOW_CHART_HINT = "show.chart.hint";

    private static final String LATEST_VERSION_CODE = "latest.version.code";


    public enum Timeframe {
		LAST_THIRTY_DAYS, UNLIMITED, LAST_TWO_DAYS, LATEST_VALUE, LAST_SEVEN_DAYS
	}

	public enum StatsMode {
        PERCENT, DAY_CHANGES
    }

	public static void disableCrashReports(Context context) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.remove(CRASH_REPORT_DISABLE);
		editor.commit();
	}

	public static void saveAccountName(Context activity, String accountName) {
		SharedPreferences.Editor editor = getSettings(activity).edit();
		editor.putString(ACCOUNT_NAME, accountName);
		editor.commit();
	}

	public static void removeAccountName(Context activity) {
		SharedPreferences.Editor editor = getSettings(activity).edit();
		editor.remove(ACCOUNT_NAME);
		editor.commit();
	}

	public static String getAccountName(Context activity) {
		return getSettings(activity).getString(ACCOUNT_NAME, null);
	}

	private static SharedPreferences getSettings(Context activity) {
		return activity.getSharedPreferences(PREF, 0);
	}

	public static String getGwtPermutation(Context activity) {
		return getVersionDependingProperty(GWTPERMUTATION, activity);
	}

	public static void saveGwtPermutation(Context activity, String gwtPermutation) {
		saveVersionDependingProperty(GWTPERMUTATION, gwtPermutation, activity);
	}

	public static String getAutosyncSet(Context activity, String accountname) {
		return getSettings(activity).getString(AUTOSYNC + accountname, null);
	}

	public static void saveAutosyncSet(Context activity, String accountname) {
		SharedPreferences.Editor editor = getSettings(activity).edit();
		editor.putString(AUTOSYNC + accountname, "true");
		editor.commit();
	}

	public static String getRequestFullAssetInfo(Context activity) {
		return getVersionDependingProperty(POST_REQUEST_GET_FULL_ASSET_INFOS, activity);
	}

	public static void saveRequestFullAssetInfo(Context activity, String postdata) {
		saveVersionDependingProperty(POST_REQUEST_GET_FULL_ASSET_INFOS, postdata, activity);
	}

	public static String getRequestGetAssetForUserCount(Context context) {
		return getVersionDependingProperty(POST_REQUEST_GET_USER_INFO_SIZE, context);
	}

	public static void saveRequestGetAssetForUserCount(Context context, String string) {
		saveVersionDependingProperty(POST_REQUEST_GET_USER_INFO_SIZE,string,context);
	}

	public static String getRequestUserComments(Context context) {
		return getVersionDependingProperty(POST_REQUEST_USER_COMMENTS, context);
	}

	public static void saveRequestUserComments(Context context, String string) {
		saveVersionDependingProperty(POST_REQUEST_USER_COMMENTS, string, context);
	}

	private static String getVersionDependingProperty(String name, Context context) {
		return getSettings(context).getString(name + getAppVersionCode(context), null);
	}

	private static void saveVersionDependingProperty(String name, String value, Context context) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putString(name + getAppVersionCode(context), value);
		editor.commit();
	}

	public static int getAppVersionCode(Context context) {
		try {
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return pinfo.versionCode;
		} catch (NameNotFoundException e) {
			Log.e(AndlyticsApp.class.getSimpleName(), "unable to read version code", e);
		}
		return 0;
	}

	public static void saveChartTimeframe(Timeframe value, Context context) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putString(CHART_TIMEFRAME, value.name());
		editor.commit();
	}

	public static Timeframe getChartTimeframe(Context activity) {
		return Timeframe.valueOf(getSettings(activity).getString(CHART_TIMEFRAME, Timeframe.LAST_THIRTY_DAYS.name()));
	}

	public static Boolean getChartSmooth(Context context) {
		return getSettings(context).getBoolean(CHART_SMOOTH, true);
	}

	public static void saveSmooth(boolean value, Context context) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putBoolean(CHART_SMOOTH, value);
		editor.commit();
	}

	public static boolean getSkipAutologin(Context context) {
		return getSettings(context).getBoolean(SKIP_AUTO_LOGIN, false);
	}

	public static void saveSkipAutoLogin(Context context, Boolean value) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putBoolean(SKIP_AUTO_LOGIN, value);
		editor.commit();
	}

    public static void saveStatsMode(StatsMode value, Context context) {
        SharedPreferences.Editor editor = getSettings(context).edit();
        editor.putString(STATS_MODE, value.name());
        editor.commit();
    }

    public static StatsMode getStatsMode(Context activity) {
        return StatsMode.valueOf(getSettings(activity).getString(STATS_MODE, StatsMode.PERCENT.name()));
    }

    public static boolean getNotificationPerf(Context context, String prefName) {
        return getSettings(context).getBoolean(prefName, true);
    }

    public static void saveLevel7AlarmManagerPeriod(Integer periodInSeconds, Context context) {
        SharedPreferences.Editor editor = getSettings(context).edit();
        editor.putInt(LEVEL_7_ALARM_MANAGER_PERIOD, periodInSeconds);
        editor.commit();
    }

    public static int getLevel7AlarmManagerPeriod(Context context) {
        return getSettings(context).getInt(LEVEL_7_ALARM_MANAGER_PERIOD, 0);
    }

    public static Boolean getShowChartHint(Context context) {
        return getSettings(context).getBoolean(SHOW_CHART_HINT, true);
    }

    public static void saveShowChartHint(Context context, Boolean value) {
        SharedPreferences.Editor editor = getSettings(context).edit();
        editor.putBoolean(SHOW_CHART_HINT, value);
        editor.commit();
    }

    public static String getDateFormatShort(Context context) {
        return getSettings(context).getString(DATE_FORMAT_SHORT, "dd/MM");
    }

    public static void saveDateFormatShort(Context context, String value) {
        SharedPreferences.Editor editor = getSettings(context).edit();
        editor.putString(DATE_FORMAT_SHORT, value);
        editor.commit();
    }

    public static String getDateFormatLong(Context context) {
        return getSettings(context).getString(DATE_FORMAT_LONG, "dd/MM/yyyy");
    }

    public static void saveDateFormatLong(Context context, String value) {
        SharedPreferences.Editor editor = getSettings(context).edit();
        editor.putString(DATE_FORMAT_LONG, value);
        editor.commit();
    }

    public static void saveAdmobSiteId(Context context, String packageName, String value) {
        SharedPreferences.Editor editor = getSettings(context).edit();
        editor.putString(ADMOB_SITE_ID + packageName, value);
        editor.commit();
    }

    public static String getAdmobSiteId(Context context, String packageName) {
        return getSettings(context).getString(ADMOB_SITE_ID + packageName, null);
    }

    public static void saveAdmobAccount(AdmobActivity context, String siteId, String accountName) {
        SharedPreferences.Editor editor = getSettings(context).edit();
        editor.putString(ADMOB_ACCOUNT + siteId, accountName);
        editor.commit();
    }

    public static String getAdmobAccount(Context context, String siteId) {
        return getSettings(context).getString(ADMOB_ACCOUNT + siteId, null);
    }

    public static int getLatestVersionCode(Context context) {
        return getSettings(context).getInt(LATEST_VERSION_CODE, 0);
    }

    public static void saveLatestVersionCode(Context context, int latest) {
        SharedPreferences.Editor editor = getSettings(context).edit();
        editor.putInt(LATEST_VERSION_CODE, latest);
        editor.commit();
    }

    public static String getRequestFeedback(Context activity) {
        return getVersionDependingProperty(POST_REQUEST_FEEDBACK, activity);
    }

    public static void saveRequestFeedback(Context activity, String postdata) {
        saveVersionDependingProperty(POST_REQUEST_FEEDBACK, postdata, activity);
    }

    public static Timeframe getAdmobTimeframe(Context context) {
        return Timeframe.valueOf(getSettings(context).getString(ADMOB_TIMEFRAME, Timeframe.LAST_THIRTY_DAYS.name()));
    }

    public static void saveAdmobTimeframe(Timeframe value, Context context) {
        SharedPreferences.Editor editor = getSettings(context).edit();
        editor.putString(ADMOB_TIMEFRAME, value.name());
        editor.commit();
    }
    
    public static void saveIsHiddenAccount(Context context, String accountName, Boolean hidden){
    	SharedPreferences.Editor editor = getSettings(context).edit();
        editor.putBoolean(HIDDEN_ACCOUNT + accountName, hidden);
        editor.commit();
    }
    
    public static boolean getIsHiddenAccount(Context context, String accountName) {
        return getSettings(context).getBoolean(HIDDEN_ACCOUNT + accountName, false);
    }
}
