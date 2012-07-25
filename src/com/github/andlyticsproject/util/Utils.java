package com.github.andlyticsproject.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;

import com.github.andlyticsproject.io.MediaScannerWrapper;

/**
 * Utility class for simple helper methods.
 */
public final class Utils {

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
			return context.getPackageManager().getPackageInfo(context.getPackageName(),
					PackageManager.GET_ACTIVITIES).versionCode;
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
			return context.getPackageManager().getPackageInfo(context.getPackageName(),
					PackageManager.GET_ACTIVITIES).versionName;
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	@SuppressLint("NewApi")
	public static <P, T extends AsyncTask<P, ?, ?>> void execute(T task, P... params) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		} else {
			task.execute(params);
		}
	}

	public static boolean isFroyo() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
	}

	public static void scanFile(Context ctx, String filename) {
		if (isFroyo()) {
			MediaScannerWrapper.scanFile(ctx, filename);
		}
	}
}
