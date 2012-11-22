package com.github.andlyticsproject.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import com.github.andlyticsproject.ContentAdapter;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.R;
import com.github.andlyticsproject.model.AppStatsList;
import com.github.andlyticsproject.util.Utils;

public class ExportService extends IntentService {

	private static final String TAG = ExportService.class.getSimpleName();

	public static final int NOTIFICATION_ID_PROGRESS = 1;
	public static final int NOTIFICATION_ID_FINISHED = 1;

	public static final String EXTRA_PACKAGE_NAMES = "packageNames";
	public static final String EXTRA_ACCOUNT_NAME = "accountName";

	private boolean errors = false;

	private String[] packageNames;

	private String accountName;

	private NotificationManager notificationManager;

	public ExportService() {
		super("andlytics ExportService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "export service onStartCommand");

		this.packageNames = intent.getStringArrayExtra(EXTRA_PACKAGE_NAMES);
		Log.d(TAG, "package names:: " + packageNames);

		this.accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME);
		Log.d(TAG, "account name:: " + accountName);

		boolean success = exportStats();
		notifyExportFinished(success);
	}

	private boolean exportStats() {
		String message = getApplicationContext().getString(R.string.export_started);
		sendNotification(message);

		File dir = StatsCsvReaderWriter.getExportDir();
		if (!dir.exists()) {
			dir.mkdirs();
		}

		try {
			File zipFile = StatsCsvReaderWriter.getExportFileForAccount(accountName);
			ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile));
			StatsCsvReaderWriter statsWriter = new StatsCsvReaderWriter(this);
			ContentAdapter db = new ContentAdapter(this);
			try {
				for (int i = 0; i < packageNames.length; i++) {
					AppStatsList statsForApp = db.getStatsForApp(packageNames[i],
							Timeframe.UNLIMITED, false);
					statsWriter.writeStats(packageNames[i], statsForApp.getAppStats(), zip);
				}
			} catch (IOException e) {
				Log.d(TAG, "Zip error, deleting incomplete file.");
				zipFile.delete();
			} finally {
				zip.close();
			}

			Utils.scanFile(this, zipFile.getAbsolutePath());
		} catch (IOException e) {
			Log.e(TAG, "Error zipping CSV files: " + e.getMessage(), e);

			return false;
		}

		return !errors;
	}

	private void notifyExportFinished(boolean success) {
		// clear progress notification
		notificationManager.cancel(NOTIFICATION_ID_PROGRESS);

		Intent shareIntent = createShareIntent();
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
				shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		String message = getApplicationContext().getString(R.string.export_saved_to) + ": "
				+ StatsCsvReaderWriter.getExportDirPath();
		String title = getResources().getString(R.string.app_name) + ": "
				+ getApplicationContext().getString(R.string.export_finished);

		Builder builder = new NotificationCompat.Builder(getApplicationContext());
		builder.setSmallIcon(R.drawable.statusbar_andlytics);
		builder.setContentTitle(title);
		builder.setContentText(message);
		BigTextStyle style = new BigTextStyle(builder);
		style.bigText(message);
		style.setBigContentTitle(title);
		style.setSummaryText(accountName);
		builder.setStyle(style);
		builder.setContentIntent(pendingIntent);
		builder.setWhen(System.currentTimeMillis());
		builder.setDefaults(Notification.DEFAULT_ALL);
		builder.setAutoCancel(true);
		builder.setOngoing(false);
		builder.addAction(android.R.drawable.ic_menu_share,
				getApplicationContext().getString(R.string.share), pendingIntent);

		notificationManager.notify(NOTIFICATION_ID_FINISHED, builder.build());
	}

	private Intent createShareIntent() {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("application/zip");
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		File zipFile = StatsCsvReaderWriter.getExportFileForAccount(accountName);
		intent.putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(zipFile.getAbsolutePath()));

		return Intent.createChooser(intent, (getString(R.string.share)));
	}

	/**
	 * Send a notification to the progress bar.
	 */
	protected void sendNotification(String message) {
		Intent startActivityIntent = new Intent();
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
				startActivityIntent, 0);

		Builder builder = new NotificationCompat.Builder(getApplicationContext());
		builder.setSmallIcon(R.drawable.statusbar_andlytics);
		builder.setContentTitle(getResources().getString(R.string.app_name) + ": "
				+ getApplicationContext().getString(R.string.export_));
		builder.setContentText(message);
		builder.setContentIntent(pendingIntent);
		builder.setDefaults(0);
		builder.setAutoCancel(true);
		builder.setOngoing(true);

		notificationManager.notify(NOTIFICATION_ID_PROGRESS, builder.build());
	}

}
