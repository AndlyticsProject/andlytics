/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.andlyticsproject.sync.notificationcompat2;

import android.app.Notification;

import static android.app.Notification.FLAG_AUTO_CANCEL;
import static android.app.Notification.FLAG_ONGOING_EVENT;
import static android.app.Notification.FLAG_ONLY_ALERT_ONCE;

class NotificationCompatHC implements NotificationCompat2.NotificationCompatImpl {
	@SuppressWarnings("deprecation")
	static Notification.Builder createBuilder(NotificationCompat2.Builder b) {
		final Notification n = b.mNotification;
		return new Notification.Builder(b.mContext)
				.setWhen(n.when)
				.setSmallIcon(n.icon, n.iconLevel)
				.setContent(n.contentView)
				.setTicker(n.tickerText, b.mTickerView)
				.setSound(n.sound, n.audioStreamType)
				.setVibrate(n.vibrate)
				.setLights(n.ledARGB, n.ledOnMS, n.ledOffMS)
				.setOngoing((n.flags & FLAG_ONGOING_EVENT) != 0)
				.setOnlyAlertOnce((n.flags & FLAG_ONLY_ALERT_ONCE) != 0)
				.setAutoCancel((n.flags & FLAG_AUTO_CANCEL) != 0)
				.setDefaults(n.defaults)
				.setContentTitle(b.mContentTitle)
				.setContentText(b.mContentText)
				.setContentInfo(b.mContentInfo)
				.setContentIntent(b.mContentIntent)
				.setDeleteIntent(n.deleteIntent)
				.setFullScreenIntent(b.mFullScreenIntent,
						(n.flags & Notification.FLAG_HIGH_PRIORITY) != 0)
				.setLargeIcon(b.mLargeIcon)
				.setNumber(b.mNumber);
	}

	@SuppressWarnings("deprecation")
	public Notification build(NotificationCompat2.Builder b) {
		return createBuilder(b).getNotification();
	}
}
