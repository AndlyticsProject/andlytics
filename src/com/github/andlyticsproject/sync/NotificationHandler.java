
package com.github.andlyticsproject.sync;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;

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

	public static void handleNotificaions(Context context, List<AppStatsDiff> diffs,
			String accountName) {
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		String contentTitle = context.getString(R.string.notification_title);
		String contentText = "";
		String iconName = null;

		boolean downloadsEnabled = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_CHANGES_DOWNLOADS);
		boolean commentsEnabled = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_CHANGES_COMMENTS);
		boolean ratingsEnabled = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_CHANGES_RATING);
		boolean lightEnabled = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_LIGHT);
		String ringtone = Preferences.getNotificationRingtone(context);

		List<String> appNameList = new ArrayList<String>();
		int number = 0;
		for (int i = 0; i < diffs.size(); i++) {

			AppStatsDiff diff = diffs.get(i);
			if (!diff.isSkipNotification()) {

				if (diff.hasChanges()) {

					List<String> changeProperties = new ArrayList<String>();

					if (commentsEnabled && diff.getCommentsChange() != 0) {
						changeProperties.add(context.getString(R.string.comments));
						number++;
					}
					if (ratingsEnabled && diff.getAvgRatingChange() != 0) {
						changeProperties.add(context.getString(R.string.ratings));
						number++;
					}
					if (downloadsEnabled && diff.getDownloadsChange() != 0) {
						changeProperties.add(context.getString(R.string.downloads));
						number++;
					}

					if (changeProperties.size() > 0) {
						String name = diff.getAppName();
						name += " (";
						for (int j = 0; j < changeProperties.size(); j++) {
							name += changeProperties.get(j);
							if (j < changeProperties.size() - 1) {
								name += ", ";
							}

						}
						name += ")";

						if (appNameList.size() == 0) {
							// Record the icon of the first app with changes that we are
							// interested in that also has notifications turned on
							iconName = diff.getIconName();
						}

						appNameList.add(name);
					}
				}
			}
		}

		if (appNameList.size() > 0) {

			for (int i = 0; i < appNameList.size(); i++) {
				contentText += appNameList.get(i);
				if (i < appNameList.size() - 1) {
					contentText += ", ";
				}
			}

			if (!AndlyticsApp.getInstance().isAppVisible()
					|| !accountName.equals(Preferences.getAccountName(context))
					|| Preferences.getNotificationPerf(context,
							Preferences.NOTIFICATION_WHEN_ACCOUNT_VISISBLE)) {
				// The user can choose not to see notifications if the current account is visible

				Builder builder = new NotificationCompat.Builder(context);
				builder.setSmallIcon(R.drawable.statusbar_andlytics);
				builder.setContentTitle(contentTitle);
				builder.setContentText(contentText);
				File iconFilePath = new File(context.getCacheDir(), iconName);
				if (iconFilePath.exists()) {
					Bitmap bm = BitmapFactory.decodeFile(iconFilePath.getAbsolutePath());
					builder.setLargeIcon(bm);
				}
				BigTextStyle style = new BigTextStyle(builder);
				style.bigText(contentText);
				style.setBigContentTitle(contentTitle);
				style.setSummaryText(accountName);
				builder.setStyle(style);
				builder.setWhen(System.currentTimeMillis());
				builder.setNumber(number);

				Intent notificationIntent = new Intent(context, Main.class);
				notificationIntent.putExtra(Constants.AUTH_ACCOUNT_NAME, accountName);
				notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

				PendingIntent contentIntent = PendingIntent.getActivity(context, accountName.hashCode(),
						notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

				builder.setContentIntent(contentIntent);
				builder.setTicker(contentTitle);

				if (ringtone != null) {
					builder.setSound(Uri.parse(ringtone));
				}
				if (lightEnabled) {
					builder.setDefaults(Notification.DEFAULT_LIGHTS);
				}
				builder.setAutoCancel(true);
				nm.notify(accountName.hashCode(), builder.build());
			}
			Intent i = new Intent(GROWL_ACTION);
			i.putExtra(EXTRA_TITLE, contentTitle);
			i.putExtra(EXTRA_DESCRIPTION, contentText);
			context.sendBroadcast(i);
		}
	}
}
