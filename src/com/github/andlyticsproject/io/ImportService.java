package com.github.andlyticsproject.io;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import com.github.andlyticsproject.ContentAdapter;
import com.github.andlyticsproject.R;
import com.github.andlyticsproject.model.AppStats;


public class ImportService extends Service {

    private static final String TAG = ImportService.class.getSimpleName();

    public static final int NOTIFICATION_ID_PROGRESS = 2;

    public static final int NOTIFICATION_ID_FINISHED = 2;

    public static final String FILE_NAMES = "fileNames";

    public static final String ACCOUNT_NAME = "accountName";

    private Notification notification;

    private String message;

    private boolean errors = false;

    private String accountName;

    private String[] fileNames;

    @Override
    public void onCreate() {

        this.notification = new Notification(R.drawable.statusbar_andlytics, getResources().getString(R.string.app_name) + ": " + getApplicationContext().getString(R.string.import_started), System.currentTimeMillis());
        this.notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_AUTO_CANCEL;
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "import service onStartCommand");

        this.fileNames = intent.getStringArrayExtra(FILE_NAMES);
        Log.d(TAG, "file names:: " + fileNames);

        this.accountName = intent.getStringExtra(ACCOUNT_NAME);
        Log.d(TAG, "account name:: " + accountName);


        (new StandardServiceWorker()).execute();

        return Service.START_NOT_STICKY;
    }

    /**
     * Asynchronous task.
     */
    private class StandardServiceWorker extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {


            message = getApplicationContext().getString(R.string.import_started);
            sendNotification();

            ContentAdapter db = new ContentAdapter(ImportService.this);

            for (int i = 0; i < fileNames.length; i++) {

                StatsCsvReaderWriter statsWriter = new StatsCsvReaderWriter(ImportService.this);

                String packageName;
                try {
                    packageName = statsWriter.readPackageName(fileNames[i]);
                    message = getApplicationContext().getString(R.string.importing) + " " + packageName;
                    publishProgress(i);

                    List<AppStats> stats = statsWriter.readStats(fileNames[i]);

                    for (AppStats appStats : stats)
                        db.insertOrUpdateAppStats(appStats, packageName);
                } catch (Exception e) {
                    errors = true;
                    e.printStackTrace();
                }

            }



            message = getResources().getString(R.string.app_name) + ": "+ getApplicationContext().getString(R.string.import_finished);
            sendNotification();

            return !errors;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            sendNotification();
        }

        @Override
        protected void onPostExecute(Boolean success) {

            // clear progress notification
            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.cancel(NOTIFICATION_ID_PROGRESS);


            notification = new Notification(R.drawable.statusbar_andlytics, message, System.currentTimeMillis());

            Intent startActivityIntent = new Intent(ImportService.this, ImportService.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, startActivityIntent, 0);
            notification.contentIntent = pendingIntent;

            if(success) {
                message = getResources().getString(R.string.app_name) + ": "+ getApplicationContext().getString(R.string.import_finished);
                notification.setLatestEventInfo(getApplicationContext(), getResources().getString(R.string.app_name) + ": "+ getApplicationContext().getString(R.string.import_finished), "", pendingIntent);
            } else {
                message = getResources().getString(R.string.app_name) + ": "+ getApplicationContext().getString(R.string.import_error);
                notification.setLatestEventInfo(getApplicationContext(), getResources().getString(R.string.app_name) + ": "+ getApplicationContext().getString(R.string.import_error), "", pendingIntent);
            }

            notification.defaults |= Notification.DEFAULT_SOUND;
            notification.flags |= Notification.FLAG_AUTO_CANCEL;

            notificationManager.notify(NOTIFICATION_ID_FINISHED, notification);

            stopSelf();
        }
    }


    /**
     * Send a notification to the progress bar.
     */
    protected void sendNotification() {

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent startActivityIntent = new Intent(ImportService.this, ImportService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, startActivityIntent, 0);

        notification.setLatestEventInfo(this, getResources().getString(R.string.app_name) + ": "+ getApplicationContext().getString(R.string.import_), message , pendingIntent);
        notificationManager.notify(NOTIFICATION_ID_PROGRESS, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
