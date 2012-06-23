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

import java.io.IOException;

import com.github.andlyticsproject.ContentAdapter;
import com.github.andlyticsproject.R;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.model.AppStatsList;


public class ExportService extends Service {

    private static final String TAG = ExportService.class.getSimpleName();

    public static final int NOTIFICATION_ID_PROGRESS = 1;

    public static final int NOTIFICATION_ID_FINISHED = 1;

    public static final String PACKAGE_NAMES = "packageNames";

    public static final String ACCOUNT_NAME = "accountName";
    
    private Notification notification;

    private String message;

    private boolean errors = false;

    private String[] packageNames;

    private String accountName;

    @Override
    public void onCreate() {

        this.notification = new Notification(R.drawable.statusbar_andlytics, "Andlytics export started", System.currentTimeMillis());
        this.notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_AUTO_CANCEL;
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "export service onStartCommand");
        
        this.packageNames = intent.getStringArrayExtra(PACKAGE_NAMES);
        Log.d(TAG, "package names:: " + packageNames);

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

      
            message = "export started";
            sendNotification();
            
            for (int i = 0; i < packageNames.length; i++) {
                
                StatsCsvReaderWriter statsWriter = new StatsCsvReaderWriter(ExportService.this);
                
                ContentAdapter db = new ContentAdapter(ExportService.this);
                AppStatsList statsForApp = db.getStatsForApp(packageNames[i], Timeframe.UNLIMITED, false);
                
                try {
                    statsWriter.writeStats(packageNames[i], statsForApp.getAppStats());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                
                publishProgress(i);

            }
            
            message = "export finished";
            sendNotification();

            return !errors;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //message = "Exporting: " + packageNames[values[0]];
            //sendNotification();
        }

        @Override
        protected void onPostExecute(Boolean success) {

            // clear progress notification
            NotificationManager notificationManager = (NotificationManager) 
                    getSystemService(Context.NOTIFICATION_SERVICE);
            
            notificationManager.cancel(NOTIFICATION_ID_PROGRESS);


            notification = new Notification(R.drawable.statusbar_andlytics, message, System.currentTimeMillis());

            Intent startActivityIntent = new Intent(ExportService.this, ExportService.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, startActivityIntent, 0);
            notification.contentIntent = pendingIntent;

            message = "Files saved to: " + StatsCsvReaderWriter.getDefaultDirectory(); 

            notification.setLatestEventInfo(getApplicationContext(), "Andlytics export finished", message, pendingIntent);
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
        Intent startActivityIntent = new Intent(ExportService.this, ExportService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, startActivityIntent, 0);

        notification.setLatestEventInfo(this, "Andlytics export", message , pendingIntent);        
        notificationManager.notify(NOTIFICATION_ID_PROGRESS, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

   
}
