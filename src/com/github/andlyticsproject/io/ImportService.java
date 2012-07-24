package com.github.andlyticsproject.io;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.github.andlyticsproject.ContentAdapter;
import com.github.andlyticsproject.LoginActivity;
import com.github.andlyticsproject.R;
import com.github.andlyticsproject.model.AppStats;


public class ImportService extends IntentService {

	private static final String TAG = ImportService.class.getSimpleName();

	public static final int NOTIFICATION_ID_PROGRESS = 2;

	public static final int NOTIFICATION_ID_FINISHED = 2;

	public static final String FILE_NAMES = "fileNames";

	public static final String ACCOUNT_NAME = "accountName";

	private Notification notification;

	private boolean errors = false;

	private String accountName;

	private List<String> fileNames;

	private String zipFilename;

	private NotificationManager notificationManager;

	public ImportService() {
		super("andlytics ImportService");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate() {
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		this.notification = new Notification(R.drawable.statusbar_andlytics, getResources()
				.getString(R.string.app_name)
				+ ": "
				+ getApplicationContext().getString(R.string.import_started),
				System.currentTimeMillis());
		this.notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT
				| Notification.FLAG_AUTO_CANCEL;
		super.onCreate();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "import service onStartCommand");

		this.zipFilename = intent.getData().getPath();
		Log.d(TAG, "zip file: " + zipFilename);

		this.fileNames = Arrays.asList(intent.getStringArrayExtra(FILE_NAMES));
		Log.d(TAG, "file names:: " + fileNames);

		this.accountName = intent.getStringExtra(ACCOUNT_NAME);
		Log.d(TAG, "account name:: " + accountName);


		boolean success = importStats();
		notifyImportFinished(success);
	}

	private boolean importStats() {
		String message = getApplicationContext().getString(R.string.import_started);
		sendNotification(message);

		ContentAdapter db = new ContentAdapter(ImportService.this);

		try {
			StatsCsvReaderWriter statsWriter = new StatsCsvReaderWriter(ImportService.this);

			ZipInputStream inzip = new ZipInputStream(new FileInputStream(zipFilename));
			ZipEntry entry = null;
			while ((entry = inzip.getNextEntry()) != null) {
				String filename = entry.getName();
				if (!fileNames.contains(filename)) {
					continue;
				}

				List<AppStats> stats = statsWriter.readStats(inzip);
				if (!stats.isEmpty()) {
					String packageName = stats.get(0).getPackageName();
					message = getApplicationContext().getString(R.string.importing) + " "
							+ packageName;
					sendNotification(message);
					for (AppStats appStats : stats)
						db.insertOrUpdateAppStats(appStats, packageName);
				}


			}
		} catch (Exception e) {
			Log.e(TAG, "Error importing stats: " + e.getMessage());
			errors = true;
		}


		message = getResources().getString(R.string.app_name) + ": "
				+ getApplicationContext().getString(R.string.import_finished);
		sendNotification(message);

		return !errors;
	}

	@SuppressWarnings("deprecation")
	private void notifyImportFinished(boolean success) {
		// clear progress notification
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.cancel(NOTIFICATION_ID_PROGRESS);

		Intent startActivityIntent = new Intent(ImportService.this, LoginActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
				startActivityIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		notification.contentIntent = pendingIntent;

		if (success) {
			String message = getResources().getString(R.string.app_name) + ": "
					+ getApplicationContext().getString(R.string.import_finished);
			notification = new Notification(R.drawable.statusbar_andlytics, message,
					System.currentTimeMillis());
			notification.setLatestEventInfo(getApplicationContext(),
					getResources().getString(R.string.app_name) + ": "
							+ getApplicationContext().getString(R.string.import_finished), "",
					pendingIntent);
		} else {
			String message = getResources().getString(R.string.app_name) + ": "
					+ getApplicationContext().getString(R.string.import_error);
			notification = new Notification(R.drawable.statusbar_andlytics, message,
					System.currentTimeMillis());
			notification.setLatestEventInfo(getApplicationContext(),
					getResources().getString(R.string.app_name) + ": "
							+ getApplicationContext().getString(R.string.import_error), "",
					pendingIntent);
		}

		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		notificationManager.notify(NOTIFICATION_ID_FINISHED, notification);
	}


	/**
	 * Send a notification to the progress bar.
	 */
	@SuppressWarnings("deprecation")
	protected void sendNotification(String message) {
		Intent startActivityIntent = new Intent(ImportService.this, ImportService.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
				startActivityIntent, 0);

		notification.setLatestEventInfo(this, getResources().getString(R.string.app_name) + ": "
				+ getApplicationContext().getString(R.string.import_), message, pendingIntent);
		notificationManager.notify(NOTIFICATION_ID_PROGRESS, notification);
	}

}
