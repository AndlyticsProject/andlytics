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
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.RemoteViews;

import java.util.ArrayList;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;

public class NotificationCompat2 {
	/** @deprecated */
	@Deprecated
	public static final int FLAG_HIGH_PRIORITY = Notification.FLAG_HIGH_PRIORITY;

	public static final int PRIORITY_DEFAULT = Notification.PRIORITY_DEFAULT;
	public static final int PRIORITY_HIGH = Notification.PRIORITY_HIGH;
	public static final int PRIORITY_LOW = Notification.PRIORITY_LOW;
	public static final int PRIORITY_MAX = Notification.PRIORITY_MAX;
	public static final int PRIORITY_MIN = Notification.PRIORITY_MIN;

	interface NotificationCompatImpl {
		public Notification build(Builder builder);
	}

	private static final NotificationCompatImpl IMPL;

	static {
		if (SDK_INT >= JELLY_BEAN) {
			IMPL = new NotificationCompatJB();
		} else if (SDK_INT >= ICE_CREAM_SANDWICH) {
			IMPL = new NotificationCompatICS();
		} else if (SDK_INT >= HONEYCOMB) {
			IMPL = new NotificationCompatHC();
		} else {
			IMPL = new NotificationCompatBase();
		}
	}

	/**
	 * Builder class for {@link android.app.Notification} objects.  Allows easier control over
	 * all the flags, as well as help constructing the typical notification layouts.
	 */
	public static class Builder {
		final Context mContext;

		CharSequence mContentTitle;
		CharSequence mContentText;
		PendingIntent mContentIntent;
		PendingIntent mFullScreenIntent;
		RemoteViews mTickerView;
		Bitmap mLargeIcon;
		CharSequence mContentInfo;
		int mNumber;
		int mPriority;
		CharSequence mSubText;
		boolean mUsesChronometer;
		Style mStyle;
		ArrayList<Integer> mActionIcons;
		ArrayList<CharSequence> mActionTitles;
		ArrayList<PendingIntent> mActionIntents;
		int mProgress;
		int mProgressMax;
		boolean mProgressIndeterminate;
		boolean mProgressSet;

		Notification mNotification = new Notification();

		/**
		 * Constructor.
		 *
		 * Automatically sets the when field to {@link System#currentTimeMillis()
		 * System.currentTimeMillis()} and the audio stream to the
		 * {@link android.app.Notification#STREAM_DEFAULT}.
		 *
		 * @param context A {@link android.content.Context} that will be used to construct the
		 *      RemoteViews. The Context will not be held past the lifetime of this
		 *      Builder object.
		 */
		public Builder(Context context) {
			mContext = context;

			// Set defaults to match the defaults of a Notification
			mNotification.when = System.currentTimeMillis();
			mNotification.audioStreamType = Notification.STREAM_DEFAULT;
			mPriority = PRIORITY_DEFAULT;
		}

		/**
		 * Set the time that the event occurred.  Notifications in the panel are
		 * sorted by this time.
		 */
		public Builder setWhen(long when) {
			mNotification.when = when;
			return this;
		}

		/**
		 * Set the small icon to use in the notification layouts.  Different classes of devices
		 * may return different sizes.  See the UX guidelines for more information on how to
		 * design these icons.
		 *
		 * @param icon A resource ID in the application's package of the drawble to use.
		 */
		public Builder setSmallIcon(int icon) {
			mNotification.icon = icon;
			return this;
		}

		/**
		 * A variant of {@link #setSmallIcon(int) setSmallIcon(int)} that takes an additional
		 * level parameter for when the icon is a {@link android.graphics.drawable.LevelListDrawable
		 * LevelListDrawable}.
		 *
		 * @param icon A resource ID in the application's package of the drawble to use.
		 * @param level The level to use for the icon.
		 *
		 * @see android.graphics.drawable.LevelListDrawable
		 */
		public Builder setSmallIcon(int icon, int level) {
			mNotification.icon = icon;
			mNotification.iconLevel = level;
			return this;
		}

		/**
		 * Set the title (first row) of the notification, in a standard notification.
		 */
		public Builder setContentTitle(CharSequence title) {
			mContentTitle = title;
			return this;
		}

		/**
		 * Set the text (second row) of the notification, in a standard notification.
		 */
		public Builder setContentText(CharSequence text) {
			mContentText = text;
			return this;
		}

		/**
		 * Set the large number at the right-hand side of the notification.  This is
		 * equivalent to setContentInfo, although it might show the number in a different
		 * font size for readability.
		 */
		public Builder setNumber(int number) {
			mNumber = number;
			return this;
		}

		/**
		 * Set the large text at the right-hand side of the notification.
		 */
		public Builder setContentInfo(CharSequence info) {
			mContentInfo = info;
			return this;
		}

		/**
		 * Set the progress this notification represents, which may be
		 * represented as a {@link android.widget.ProgressBar}.
		 */
		public Builder setProgress(int max, int progress, boolean indeterminate) {
			mProgressSet = true;
			mProgressMax = max;
			mProgress = progress;
			mProgressIndeterminate = indeterminate;
			return this;
		}

		/**
		 * Supply a custom RemoteViews to use instead of the standard one.
		 */
		public Builder setContent(RemoteViews views) {
			mNotification.contentView = views;
			return this;
		}

		/**
		 * Supply a {@link android.app.PendingIntent} to send when the notification is clicked.
		 * If you do not supply an intent, you can now add PendingIntents to individual
		 * views to be launched when clicked by calling {@link android.widget.RemoteViews#setOnClickPendingIntent
		 * RemoteViews.setOnClickPendingIntent(int,PendingIntent)}.  Be sure to
		 * read {@link android.app.Notification#contentIntent Notification.contentIntent} for
		 * how to correctly use this.
		 */
		public Builder setContentIntent(PendingIntent intent) {
			mContentIntent = intent;
			return this;
		}

		/**
		 * Supply a {@link android.app.PendingIntent} to send when the notification is cleared by the user
		 * directly from the notification panel.  For example, this intent is sent when the user
		 * clicks the "Clear all" button, or the individual "X" buttons on notifications.  This
		 * intent is not sent when the application calls {@link android.app.NotificationManager#cancel
		 * NotificationManager.cancel(int)}.
		 */
		public Builder setDeleteIntent(PendingIntent intent) {
			mNotification.deleteIntent = intent;
			return this;
		}

		/**
		 * An intent to launch instead of posting the notification to the status bar.
		 * Only for use with extremely high-priority notifications demanding the user's
		 * <strong>immediate</strong> attention, such as an incoming phone call or
		 * alarm clock that the user has explicitly set to a particular time.
		 * If this facility is used for something else, please give the user an option
		 * to turn it off and use a normal notification, as this can be extremely
		 * disruptive.
		 *
		 * @param intent The pending intent to launch.
		 * @param highPriority Passing true will cause this notification to be sent
		 *          even if other notifications are suppressed.
		 */
		public Builder setFullScreenIntent(PendingIntent intent, boolean highPriority) {
			mFullScreenIntent = intent;
			setFlag(FLAG_HIGH_PRIORITY, highPriority);
			return this;
		}

		/**
		 * Set the text that is displayed in the status bar when the notification first
		 * arrives.
		 */
		public Builder setTicker(CharSequence tickerText) {
			mNotification.tickerText = tickerText;
			return this;
		}

		/**
		 * Set the text that is displayed in the status bar when the notification first
		 * arrives, and also a RemoteViews object that may be displayed instead on some
		 * devices.
		 */
		public Builder setTicker(CharSequence tickerText, RemoteViews views) {
			mNotification.tickerText = tickerText;
			mTickerView = views;
			return this;
		}

		/**
		 * Set the large icon that is shown in the ticker and notification.
		 */
		public Builder setLargeIcon(Bitmap icon) {
			mLargeIcon = icon;
			return this;
		}

		/**
		 * Set the sound to play.  It will play on the default stream.
		 */
		public Builder setSound(Uri sound) {
			mNotification.sound = sound;
			mNotification.audioStreamType = Notification.STREAM_DEFAULT;
			return this;
		}

		/**
		 * Set the sound to play.  It will play on the stream you supply.
		 *
		 * @see Notification#STREAM_DEFAULT
		 * @see android.media.AudioManager for the <code>STREAM_</code> constants.
		 */
		public Builder setSound(Uri sound, int streamType) {
			mNotification.sound = sound;
			mNotification.audioStreamType = streamType;
			return this;
		}

		/**
		 * Set the vibration pattern to use.
		 *
		 * @see android.os.Vibrator for a discussion of the <code>pattern</code>
		 * parameter.
		 */
		public Builder setVibrate(long[] pattern) {
			mNotification.vibrate = pattern;
			return this;
		}

		/**
		 * Set the argb value that you would like the LED on the device to blnk, as well as the
		 * rate.  The rate is specified in terms of the number of milliseconds to be on
		 * and then the number of milliseconds to be off.
		 */
		public Builder setLights(int argb, int onMs, int offMs) {
			mNotification.ledARGB = argb;
			mNotification.ledOnMS = onMs;
			mNotification.ledOffMS = offMs;
			boolean showLights = mNotification.ledOnMS != 0 && mNotification.ledOffMS != 0;
			mNotification.flags = (mNotification.flags & ~Notification.FLAG_SHOW_LIGHTS) |
					(showLights ? Notification.FLAG_SHOW_LIGHTS : 0);
			return this;
		}

		/**
		 * Set whether this is an ongoing notification.
		 *
		 * <p>Ongoing notifications differ from regular notifications in the following ways:
		 * <ul>
		 *   <li>Ongoing notifications are sorted above the regular notifications in the
		 *   notification panel.</li>
		 *   <li>Ongoing notifications do not have an 'X' close button, and are not affected
		 *   by the "Clear all" button.
		 * </ul>
		 */
		public Builder setOngoing(boolean ongoing) {
			setFlag(Notification.FLAG_ONGOING_EVENT, ongoing);
			return this;
		}

		/**
		 * Set this flag if you would only like the sound, vibrate
		 * and ticker to be played if the notification is not already showing.
		 */
		public Builder setOnlyAlertOnce(boolean onlyAlertOnce) {
			setFlag(Notification.FLAG_ONLY_ALERT_ONCE, onlyAlertOnce);
			return this;
		}

		/**
		 * Setting this flag will make it so the notification is automatically
		 * canceled when the user clicks it in the panel.  The PendingIntent
		 * set with {@link #setDeleteIntent} will be broadcast when the notification
		 * is canceled.
		 */
		public Builder setAutoCancel(boolean autoCancel) {
			setFlag(Notification.FLAG_AUTO_CANCEL, autoCancel);
			return this;
		}

		/**
		 * Set the default notification options that will be used.
		 * <p>
		 * The value should be one or more of the following fields combined with
		 * bitwise-or:
		 * {@link android.app.Notification#DEFAULT_SOUND}, {@link android.app.Notification#DEFAULT_VIBRATE},
		 * {@link android.app.Notification#DEFAULT_LIGHTS}.
		 * <p>
		 * For all default values, use {@link android.app.Notification#DEFAULT_ALL}.
		 */
		public Builder setDefaults(int defaults) {
			mNotification.defaults = defaults;
			if ((defaults & Notification.DEFAULT_LIGHTS) != 0) {
				mNotification.flags |= Notification.FLAG_SHOW_LIGHTS;
			}
			return this;
		}

		private void setFlag(int mask, boolean value) {
			if (value) {
				mNotification.flags |= mask;
			} else {
				mNotification.flags &= ~mask;
			}
		}

		/**
		 * Add an action to this notification. Actions are typically displayed
		 * by the system as a button adjacent to the notification content.
		 *
		 * @param icon Resource ID of a drawable that represents the action.
		 * @param title Text describing the action.
		 * @param intent PendingIntent to be fired when the action is invoked.
		 */
		public Builder addAction(int icon, CharSequence title, PendingIntent intent) {
			if (mActionIcons == null) {
				mActionIcons = new ArrayList<Integer>();
				mActionTitles = new ArrayList<CharSequence>();
				mActionIntents = new ArrayList<PendingIntent>();
			}
			mActionIcons.add(icon);
			mActionTitles.add(title);
			mActionIntents.add(intent);
			return this;
		}

		/**
		 * Set the priority of this notification.
		 */
		public Builder setPriority(int priority) {
			mPriority = priority;
			return this;
		}

		/**
		 * Add a rich notification style to be applied at build time.
		 */
		public Builder setStyle(Style style) {
			mStyle = style;
			return this;
		}

		/**
		 * Set the third line of text in the platform notification template.
		 * Don't use if you're also using
		 * {@link #setProgress(int, int, boolean)}; they occupy the same
		 * location in the standard template.
		 */
		public Builder setSubText(CharSequence subtext) {
			mSubText = subtext;
			return this;
		}

		/**
		 * Show the {@link Notification#when} field as a stopwatch. Instead of
		 * presenting {@code when} as a timestamp, the notification will show
		 * an automatically updating display of the minutes and seconds since
		 * {@code when}. Useful when showing an elapsed time (like an ongoing
		 * phone call).
		 */
		public Builder setUsesChronometer(boolean usesChronometer) {
			mUsesChronometer = usesChronometer;
			return this;
		}

		/** @deprecated Use {@link #build()}. */
		@Deprecated
		public Notification getNotification() {
			return build();
		}

		/**
		 * Combine all of the options that have been set and return a new {@link android.app.Notification}
		 * object.
		 */
		public Notification build() {
			return IMPL.build(this);
		}
	}

	public static abstract class Style {
		protected NotificationCompat2.Builder mBuilder;
		CharSequence mBigContentTitle;
		CharSequence mSummaryText;

		public abstract Notification build();

		public void setBuilder(NotificationCompat2.Builder builder) {
			mBuilder = builder;
		}

		protected void checkBuilder() {
			if (mBuilder == null) {
				throw new IllegalStateException("No builder.");
			}
		}

		protected RemoteViews getStandardView(int layoutId) {
			return null;
		}

		protected void internalSetBigContentTitle(CharSequence title) {
			mBigContentTitle = title;
		}

		protected void internalSetSummaryText(CharSequence cs) {
			mSummaryText = cs;
		}
	}

	public static class BigPictureStyle extends Style {
		Bitmap mBigLargeIcon;
		Bitmap mBigPicture;

		public BigPictureStyle() {
		}

		public BigPictureStyle(Builder builder) {
			setBuilder(builder);
		}

		public BigPictureStyle bigLargeIcon(Bitmap b) {
			mBigLargeIcon = b;
			return this;
		}

		public BigPictureStyle bigPicture(Bitmap b) {
			mBigPicture = b;
			return this;
		}

		@Override
		public Notification build() {
			checkBuilder();
			if (SDK_INT >= JELLY_BEAN) {
				return NotificationCompatJB.buildBigPictureStyle(this);
			}
			return mBuilder.build();
		}

		public BigPictureStyle setBigContentTitle(CharSequence title) {
			internalSetBigContentTitle(title);
			return this;
		}

		public BigPictureStyle setSummaryText(CharSequence cs) {
			internalSetSummaryText(cs);
			return this;
		}
	}

	public static class BigTextStyle extends Style {
		CharSequence mBigText;

		public BigTextStyle() {
		}

		public BigTextStyle(Builder builder) {
			setBuilder(builder);
		}

		public BigTextStyle bigText(CharSequence text) {
			mBigText = text;
			return this;
		}

		@Override
		public Notification build() {
			checkBuilder();
			if (SDK_INT >= JELLY_BEAN) {
				return NotificationCompatJB.buildBigTextStyle(this);
			}
			return mBuilder.build();
		}

		public BigTextStyle setBigContentTitle(CharSequence title) {
			internalSetBigContentTitle(title);
			return this;
		}

		public BigTextStyle setSummaryText(CharSequence cs) {
			internalSetSummaryText(cs);
			return this;
		}
	}

	public static class InboxStyle extends Style {
		ArrayList<CharSequence> mLines;

		public InboxStyle() {
		}

		public InboxStyle(Builder builder) {
			setBuilder(builder);
		}

		public InboxStyle addLine(CharSequence line) {
			if (mLines == null) {
				mLines = new ArrayList<CharSequence>();
			}
			mLines.add(line);
			return this;
		}

		@Override
		public Notification build() {
			checkBuilder();
			if (SDK_INT >= JELLY_BEAN) {
				return NotificationCompatJB.buildInboxStyle(this);
			}
			return mBuilder.build();
		}

		public InboxStyle setBigContentTitle(CharSequence title) {
			internalSetBigContentTitle(title);
			return this;
		}

		public InboxStyle setSummaryText(CharSequence cs) {
			internalSetSummaryText(cs);
			return this;
		}
	}
}
