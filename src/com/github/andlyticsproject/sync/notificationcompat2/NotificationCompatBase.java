package com.github.andlyticsproject.sync.notificationcompat2;

import android.app.Notification;

class NotificationCompatBase implements NotificationCompat2.NotificationCompatImpl {
    @SuppressWarnings("deprecation")
    public Notification build(NotificationCompat2.Builder b) {
        Notification result = b.mNotification;
        result.setLatestEventInfo(b.mContext, b.mContentTitle,
                b.mContentText, b.mContentIntent);
        return result;
  }
}
