package com.github.andlyticsproject.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Utility class for simple helper methods.
 */
public final class Utils {
	private static final String TAG = "Utils";
	
	/** Key for latest version code preference. */
	private static final String LAST_VERSION_CODE_KEY = "last_version_code";
    /** Private constructor. */
    private Utils() {
    }

    /**
     * get the code of the actual version.
     *
     * @param context
     *            the context
     * @return the code of the actual version
     */
    public static int getActualVersionCode(final Context context) {
        // Get the versionCode of the Package, which must be different
        // (incremented) in each release on the market in the
        // AndroidManifest.xml
        try {
            return context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_ACTIVITIES).versionCode;
        } catch (NameNotFoundException e) {
            return 0;
        }
    }

    /**
     * get the name of the actual version.
     *
     * @param context
     *            the context
     * @return the name of the actual version
     */
    public static String getActualVersionName(final Context context) {
        // Get the versionCode of the Package, which must be different
        // (incremented) in each release on the market in the
        // AndroidManifest.xml
        try {
            return context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
        } catch (NameNotFoundException e) {
            return null;
        }
    }
}
