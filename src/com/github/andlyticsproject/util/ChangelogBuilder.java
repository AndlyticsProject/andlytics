
package com.github.andlyticsproject.util;

import java.io.IOException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import com.github.andlyticsproject.R;

/**
 * Changelog builder to create the changelog screen.
 */
public final class ChangelogBuilder {
	/** LOG Constant. **/
	private static final String TAG = "ChangelogBuilder";

	/** Private constructor. */
	private ChangelogBuilder() {
	}

	/**
	 * Show the dialog only if not already shown for this version of the
	 * application.
	 * 
	 * @param context
	 *            the context
	 * @param listener
	 *            the listener to be set for the clickevent of the 'OK' button
	 * @return the 'Changelog' dialog
	 */
	public static AlertDialog create(final Context context, final Dialog.OnClickListener listener) {

		View view = LayoutInflater.from(context).inflate(R.layout.changelog, null);
		WebView webView = (WebView) view.findViewById(R.id.changelogcontent);

		try {
			webView.loadData(DataLoader.loadData(context, "changelog"), "text/html", "UTF-8");
		} catch (IOException ioe) {
			Log.e(TAG, "Error reading changelog file!", ioe);
		}

		return new AlertDialog.Builder(context)
				.setTitle(
						context.getString(R.string.changelog_title) + "\n"
								+ context.getString(R.string.app_name) + " v"
								+ Utils.getActualVersionName(context)).setIcon(R.drawable.icon)
				.setView(view).setPositiveButton(android.R.string.ok, listener).create();
	}
}
