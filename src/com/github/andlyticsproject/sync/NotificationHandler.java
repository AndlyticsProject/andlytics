package com.github.andlyticsproject.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import com.github.andlyticsproject.AndlyticsApp;
import com.github.andlyticsproject.AppStatsDiff;
import com.github.andlyticsproject.Constants;
import com.github.andlyticsproject.Main;
import com.github.andlyticsproject.Preferences;
import com.github.andlyticsproject.R;

public class NotificationHandler {

    static final String GROWL_ACTION = "org.damazio.notifier.service.UserReceiver.USER_MESSAGE";

    static final String EXTRA_TITLE = "title";

    static final String EXTRA_DESCRIPTION = "description";

    public static void handleNotificaions(Context context, List<AppStatsDiff> diffs, String accountName) {

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);

        String contentTitle = "Andlytics change detection";
        String contentText = "";

        boolean commentsEnabled = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_CHANGES_COMMENTS, accountName);
        boolean ratingsEnabled = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_CHANGES_RATING, accountName);
        boolean downloadsEnabled = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_CHANGES_DOWNLOADS, accountName);
        boolean soundEnabled = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_SOUND, accountName);
        boolean lightEnabled = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_LIGHT, accountName);

        List<String> appNameList = new ArrayList<String>();
        for (int i = 0; i < diffs.size(); i++) {

            AppStatsDiff diff = diffs.get(i);
            if(!diff.isSkipNotification()) {

                if(diff.hasChanges()) {

                    List<String> changeProperties = new ArrayList<String>();

                    if(commentsEnabled && diff.getCommentsChange() != 0) {
                        changeProperties.add("comments");
                    }
                    if(ratingsEnabled && diff.getAvgRatingChange() != 0) {
                        changeProperties.add("ratings");
                    }
                    if(downloadsEnabled && diff.getDownloadsChange() != 0) {
                        changeProperties.add("downloads");
                    }

                    if(changeProperties.size() > 0) {
                        String name = diff.getAppName();
                        name += " (";
                        for (int j = 0; j < changeProperties.size(); j++) {
                            name += changeProperties.get(j);
                            if(j < changeProperties.size() -1) {
                                name += ", ";
                            }

                        }
                        name += ")";

                        appNameList.add(name);
                    }
                }
            }
        }

        if(appNameList.size() > 0) {

            for (int i = 0; i < appNameList.size(); i++) {
                contentText += appNameList.get(i);
                if(i < appNameList.size() -1) {
                    contentText += ", ";
                }
            }

            Notification notification = new Notification(R.drawable.statusbar_andlytics, contentTitle + ": " + contentText, System.currentTimeMillis());

            Intent notificationIntent = new Intent(context, Main.class);
            notificationIntent.putExtra(Constants.AUTH_ACCOUNT_NAME, accountName);


            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);


            if(soundEnabled) {
                notification.defaults |= Notification.DEFAULT_SOUND;
            }
            if(lightEnabled) {
                notification.defaults |= Notification.DEFAULT_LIGHTS;
            }
            notification.contentIntent = contentIntent;
            notification.flags |= Notification.FLAG_AUTO_CANCEL;

            mNotificationManager.notify(1, notification);


            Intent i = new Intent(GROWL_ACTION);
            i.putExtra(EXTRA_TITLE, contentTitle);
            i.putExtra(EXTRA_DESCRIPTION, contentText);
            context.sendBroadcast(i);

        }


    }

}
