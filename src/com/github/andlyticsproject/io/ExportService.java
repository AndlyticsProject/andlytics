package com.github.andlyticsproject.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

	private Notification notification;

	private boolean errors = false;

	private String[] packageNames;

	private String accountName;

	private Exception error;

	private NotificationManager notificationManager;

	public ExportService() {
		super("andlytics ExportService");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate() {
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		this.notification = new Notification(R.drawable.statusbar_andlytics, getResources()
				.getString(R.string.app_name)
				+ ": "
				+ getApplicationContext().getString(R.string.export_started),
				System.currentTimeMillis());
		this.notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT
				| Notification.FLAG_AUTO_CANCEL;
		super.onCreate();
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

		for (int i = 0; i < packageNames.length; i++) {
			StatsCsvReaderWriter statsWriter = new StatsCsvReaderWriter(this);
			ContentAdapter db = new ContentAdapter(this);
			AppStatsList statsForApp = db.getStatsForApp(packageNames[i], Timeframe.UNLIMITED,
					false);

			try {
				statsWriter.writeStats(packageNames[i], statsForApp.getAppStats());
			} catch (IOException e) {
				Log.e(TAG, "Error writing CSV files: " + e.getMessage(), e);
				return false;
			}
		}

		try {
			File zipFile = StatsCsvReaderWriter.getExportFileForAccount(accountName);
			ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile));

			List<File> csvFiles = new ArrayList<File>();
			byte[] buff = new byte[1024];
			try {
				for (String packageName : packageNames) {
					File csvFile = new File(StatsCsvReaderWriter.getExportDirPath(), packageName
							+ ".csv");
					InputStream in = new FileInputStream(csvFile);
					zip.putNextEntry(new ZipEntry(csvFile.getName()));

					int len = -1;
					while ((len = in.read(buff)) > 0) {
						zip.write(buff, 0, len);
					}

					zip.closeEntry();
					in.close();
				}
			} catch (IOException e) {
				Log.d(TAG, "Zip error, deleting incomplete file.");
				zipFile.delete();
			} finally {
				zip.close();
			}

			// delete temporary files
			for (File f : csvFiles) {
				f.delete();
			}

			Utils.scanFile(this, zipFile.getAbsolutePath());
		} catch (IOException e) {
			Log.e(TAG, "Error zipping CSV files: " + e.getMessage(), e);
			error = e;

			// XXX do something with the error
			return false;
		}

		message = getResources().getString(R.string.app_name) + ": "
				+ getApplicationContext().getString(R.string.export_finished);
		sendNotification(message);

		return !errors;
	}

	@SuppressWarnings("deprecation")
	private void notifyExportFinished(boolean success) {
		// clear progress notification
		notificationManager.cancel(NOTIFICATION_ID_PROGRESS);

		Intent shareIntent = createShareIntent();
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
				shareIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		notification.contentIntent = pendingIntent;

		String message = getApplicationContext().getString(R.string.export_saved_to) + ": "
				+ StatsCsvReaderWriter.getExportDirPath();
		notification = new Notification(R.drawable.statusbar_andlytics, message,
				System.currentTimeMillis());
		notification.setLatestEventInfo(getApplicationContext(),
				getResources().getString(R.string.app_name) + ": "
						+ getApplicationContext().getString(R.string.export_finished), message,
				pendingIntent);
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		notificationManager.notify(NOTIFICATION_ID_FINISHED, notification);
	}

	private Intent createShareIntent() {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("application/zip");
		File zipFile = StatsCsvReaderWriter.getExportFileForAccount(accountName);
		intent.putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(zipFile.getAbsolutePath()));

		return Intent.createChooser(intent, (getString(R.string.share)));
	}

	/**
	 * Send a notification to the progress bar.
	 */
	@SuppressWarnings("deprecation")
	protected void sendNotification(String message) {
		Intent startActivityIntent = new Intent();
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
				startActivityIntent, 0);

		notification.setLatestEventInfo(this, getResources().getString(R.string.app_name) + ": "
				+ getApplicationContext().getString(R.string.export_), message, pendingIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(NOTIFICATION_ID_PROGRESS, notification);
	}

}
