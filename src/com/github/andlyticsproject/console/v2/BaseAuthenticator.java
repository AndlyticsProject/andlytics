package com.github.andlyticsproject.console.v2;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import com.github.andlyticsproject.AndlyticsApp;
import com.github.andlyticsproject.R;
import com.github.andlyticsproject.model.DeveloperConsoleAccount;
import com.github.andlyticsproject.util.FileUtils;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseAuthenticator implements DevConsoleAuthenticator {

	private static final String TAG = BaseAuthenticator.class.getSimpleName();

	protected static final Pattern DEV_ACC_PATTERN = Pattern
			.compile("\"DeveloperConsoleAccounts\":\"\\{\\\\\"1\\\\\":\\[\\{\\\\\"1\\\\\":\\\\\"(\\d{20})\\\\\"");
	protected static final Pattern DEV_ACCS_PATTERN = Pattern
			.compile("\\\\\"1\\\\\":\\\\\"(\\d{20})\\\\\",\\\\\"2\\\\\":\\\\\"(.+?)\\\\\",");
	protected static final Pattern XSRF_TOKEN_PATTERN = Pattern
			.compile("\"XsrfToken\":\"\\{\\\\\"1\\\\\":\\\\\"(\\S+)\\\\\"\\}\"");

	protected static final Pattern WHITELISTED_FEATURES_PATTERN = Pattern
			.compile("\"WhitelistedFeatures\":\"\\{\\\\\"1\\\\\":\\[(\\S+?)\\]\\}");

	protected String accountName;

	protected BaseAuthenticator(String accountName) {
		this.accountName = accountName;
	}

	protected String findXsrfToken(String responseStr) {
		Matcher m = XSRF_TOKEN_PATTERN.matcher(responseStr);
		if (m.find()) {
			return m.group(1);
		}
		return null;
	}

	protected DeveloperConsoleAccount[] findDeveloperAccounts(String responseStr) {
		List<DeveloperConsoleAccount> devAccounts = new ArrayList<DeveloperConsoleAccount>();
		Matcher m = DEV_ACCS_PATTERN.matcher(responseStr);
		while (m.find()) {
			String developerId = m.group(1);
			String developerName = m.group(2);
			if (developerName.contains("\\\\u")) {
				developerName = developerName.replace("\\\\u", "\\u");
				developerName = StringEscapeUtils.unescapeJava(developerName);
			} else if (developerName.contains("\\u")) {
				developerName = StringEscapeUtils.unescapeJava(developerName);
			}
			devAccounts.add(new DeveloperConsoleAccount(developerId, developerName));
		}
		return devAccounts.isEmpty() ? null : devAccounts
				.toArray(new DeveloperConsoleAccount[devAccounts.size()]);
	}

	protected List<String> findWhitelistedFeatures(String responseStr) {
		List<String> result = new ArrayList<String>();
		Matcher m = WHITELISTED_FEATURES_PATTERN.matcher(responseStr);
		if (m.find()) {
			String featuresStr = m.group(1);
			String[] features = featuresStr.split(",");
			for (String feature : features) {
				result.add(feature.replaceAll("\\\\\"", ""));
			}
		}

		return Collections.unmodifiableList(result);
	}

	public String getAccountName() {
		return accountName;
	}

	protected void debugAuthFailure(Activity activity, String responseStr, String webloginUrl) {
		FileUtils.writeToAndlyticsDir("console-response.html", responseStr);
		openAuthUrlInBrowser(activity, webloginUrl);
	}

	protected void openAuthUrlInBrowser(Activity activity, String webloginUrl) {
		if (webloginUrl == null) {
			Log.d(TAG, "Null webloginUrl?");
			return;
		}

		Log.d(TAG, "Opening login URL in browser: " + webloginUrl);

		Intent viewInBrowser = new Intent(Intent.ACTION_VIEW);
		viewInBrowser.setData(Uri.parse(webloginUrl));

		// Always show the notification
		// When this occurs, it can often occur in batches, e.g. if a the user also clicks to view
		// comments which results in multiple dev consoles opening in their browser without an
		// explanation. This is even worse if they have multiple accounts and/or are currently
		// signed in via a different account
		Context ctx = AndlyticsApp.getInstance();
		Builder builder = new NotificationCompat.Builder(ctx);
		builder.setSmallIcon(R.drawable.statusbar_andlytics);
		builder.setContentTitle(ctx.getResources().getString(R.string.auth_error, accountName));
		builder.setContentText(ctx.getResources().getString(R.string.auth_error_open_browser,
				accountName));
		builder.setAutoCancel(true);
		PendingIntent contentIntent = PendingIntent.getActivity(ctx, accountName.hashCode(),
				viewInBrowser, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(contentIntent);

		NotificationManager nm = (NotificationManager) ctx
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(accountName.hashCode(), builder.build());
	}

}
