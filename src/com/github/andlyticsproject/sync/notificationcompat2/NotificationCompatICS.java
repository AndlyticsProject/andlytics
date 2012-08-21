
package com.github.andlyticsproject.sync.notificationcompat2;

import android.annotation.TargetApi;
import android.app.Notification;

@TargetApi(14)
class NotificationCompatICS implements NotificationCompat2.NotificationCompatImpl {
	static Notification.Builder createBuilder(NotificationCompat2.Builder b) {
		Notification.Builder builder = NotificationCompatHC.createBuilder(b);
		if (b.mProgressSet) {
			builder.setProgress(b.mProgressMax, b.mProgress, b.mProgressIndeterminate);
		}
		return builder;
	}

	@SuppressWarnings("deprecation")
	@Override
	public Notification build(NotificationCompat2.Builder builder) {
		return createBuilder(builder).getNotification();
	}
}
