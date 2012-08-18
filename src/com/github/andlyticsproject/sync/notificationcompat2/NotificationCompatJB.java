
package com.github.andlyticsproject.sync.notificationcompat2;

import android.annotation.TargetApi;
import android.app.Notification;

@TargetApi(16)
class NotificationCompatJB implements NotificationCompat2.NotificationCompatImpl {
	static Notification.Builder createBuilder(NotificationCompat2.Builder b) {
		final Notification.Builder builder = NotificationCompatICS.createBuilder(b);

		if (b.mActionIcons != null) {
			final int size = b.mActionIcons.size();
			for (int i = 0; i < size; i++) {
				builder.addAction(b.mActionIcons.get(i), b.mActionTitles.get(i),
						b.mActionIntents.get(i));
			}
		}

		return builder.setPriority(b.mPriority).setSubText(b.mSubText)
				.setUsesChronometer(b.mUsesChronometer);
	}

	@Override
	public Notification build(NotificationCompat2.Builder b) {
		if (b.mStyle != null) {
			NotificationCompat2.Style style = b.mStyle;
			b.mStyle = null; // Avoid infinite recursion
			style.setBuilder(b);
			return style.build();
		}
		return createBuilder(b).build();
	}

	static Notification buildBigPictureStyle(NotificationCompat2.BigPictureStyle s) {
		return new Notification.BigPictureStyle(createBuilder(s.mBuilder))
				.bigLargeIcon(s.mBigLargeIcon).bigPicture(s.mBigPicture)
				.setBigContentTitle(s.mBigContentTitle).setSummaryText(s.mSummaryText).build();
	}

	static Notification buildBigTextStyle(NotificationCompat2.BigTextStyle s) {
		return new Notification.BigTextStyle(createBuilder(s.mBuilder)).bigText(s.mBigText)
				.setBigContentTitle(s.mBigContentTitle).setSummaryText(s.mSummaryText).build();
	}

	static Notification buildInboxStyle(NotificationCompat2.InboxStyle s) {
		Notification.InboxStyle style = new Notification.InboxStyle(createBuilder(s.mBuilder))
				.setBigContentTitle(s.mBigContentTitle).setSummaryText(s.mSummaryText);

		if (s.mLines != null) {
			for (CharSequence line : s.mLines) {
				style.addLine(line);
			}
		}

		return style.build();
	}
}
