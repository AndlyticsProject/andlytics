package com.github.andlyticsproject.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import com.github.andlyticsproject.AndlyticsApp;

/**
 * Utility class for simple helper methods.
 */
public final class Utils {

	private static final int MAX_STACKTRACE_CAUSE_DEPTH = 5;

	/** Private constructor. */
	private Utils() {
	}

	public static String stackTraceToString(Throwable e) {
		return stackTraceToString(e, 0);
	}

	public static String stackTraceToString(Throwable e, int depth) {
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement element : e.getStackTrace()) {
			sb.append(element.toString());
			sb.append("\n");
		}
		if (depth < MAX_STACKTRACE_CAUSE_DEPTH && e.getCause() != null) {
			// While there is an underlying cause below the max depth, append it
			return sb.toString() + stackTraceToString(e.getCause(), ++depth);
		}
		return sb.toString();
	}


	/**
	 * get the code of the actual version.
	 * 
	 * @param context
	 * the context
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
	 * the context
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

	public static <P, T extends AsyncTask<P, ?, ?>> void execute(T task) {
		execute(task, (P[]) null);
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

	public static void getAndSaveToFile(URL url, File file) throws IOException {
		InputStream is = null;
		FileOutputStream fos = null;

		try {
			HttpURLConnection c = (HttpURLConnection) url.openConnection();
			c.setRequestMethod("GET");
			c.connect();

			is = c.getInputStream();
			fos = new FileOutputStream(file);

			byte[] buffer = new byte[1024];
			int read = 0;
			while ((read = is.read(buffer)) != -1) {
				fos.write(buffer, 0, read);
			}
		} finally {
			if (is != null) {
				is.close();
			}
			if (fos != null) {
				fos.close();
			}
		}
	}

	public static int getAppVersionCode(Context context) {
		try {
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return pinfo.versionCode;
		} catch (NameNotFoundException e) {
			Log.e(AndlyticsApp.class.getSimpleName(), "unable to read version code", e);
		}
		return 0;
	}

	public static long timestampWithoutMillis(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.MILLISECOND, 0);

		return cal.getTimeInMillis();
	}

	@SuppressLint("InlinedApi")
	public static boolean isPackageInstalled(Context ctx, String packageName) {
		try {
			ApplicationInfo info = ctx.getPackageManager().getApplicationInfo(packageName, 0);

			// need this to cover multi-user env (4.2 tablets, etc.)
			// previous version don't set FLAG_INSTALLED
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
				return true;
			}

			return (info.flags & ApplicationInfo.FLAG_INSTALLED) == ApplicationInfo.FLAG_INSTALLED;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	// the console uses the 'en-US' format
	public static String getDisplayLocale() {
		return String.format("%s-%s", Locale.getDefault().getLanguage(), Locale.getDefault()
				.getCountry());
	}

	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	public static synchronized Date parseDbDate(String string) {
		try {
			return DB_DATE_FORMAT.parse(string);
		} catch (ParseException e) {
			return null;
		}
	}

	public static synchronized String formatDbDate(Date date) {
		return DB_DATE_FORMAT.format(date);
	}

	public static void ensureMainThread(Context ctx) {
		Looper looper = Looper.myLooper();
		if (looper != null && looper != ctx.getMainLooper()) {
			throw new IllegalStateException("Only call this from your main thread.");
		}
	}

	public static String safeToString(Object val) {
		if (val == null) {
			return "";
		}

		return val.toString();
	}

	public static Integer tryParseInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static Float tryParseFloat(String str) {
		try {
			return Float.parseFloat(str);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
