package com.github.andlyticsproject.about;

import java.io.IOException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import com.github.andlyticsproject.R;
import com.github.andlyticsproject.util.ChangelogBuilder;
import com.github.andlyticsproject.util.DataLoader;
import com.github.andlyticsproject.util.Utils;

/**
 * Listener implementation for the main about dialog.
 */
public class MainAboutOnItemListener implements DialogInterface.OnClickListener {
	private static final String TAG = "MainAboutOnItemListener";

	/** Position of the 'credits' menue item'. */
	private static final int POSITION_CREDITS = 0;

	/** Position of the 'changelog' menue item'. */
	private static final int POSITION_CHANGELOG = 1;

	/** Position of the 'feedback' menue item'. */
	private static final int POSITION_FEEDBACK = 2;

	private Context mContext;

	/** The dialog which is showing/calling the 'About' dialog. */
	private Dialog mCallingDialog;

	/**
	 * creates a listener for the main about dialog.
	 * 
	 * @param context
	 *            The context
	 * @param callingDialog
	 *            the Dialog calling the Dialog on which this listener should be
	 *            set.
	 */
	public MainAboutOnItemListener(Context context, Dialog callingDialog) {
		mContext = context;
		mCallingDialog = callingDialog;
	}

	/**
	 * This method will be invoked when a button in the dialog is clicked.
	 * 
	 * @param dialog
	 *            The dialog that received the click.
	 * @param which
	 *            The button that was clicked (e.g.
	 *            android.content.DialogInterface.BUTTON1 ) or the position of
	 *            the item clicked.
	 */
	public void onClick(DialogInterface dialog, int which) {
		openItem(which);
	}

	/**
	 * executes the action corresponding to the clicked item based on the
	 * position.
	 * 
	 * @param position
	 *            die position of the clicked item.
	 */
	private void openItem(int position) {
		Dialog dialog = null;
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		LayoutInflater factory = LayoutInflater.from(mContext);
		View view = null;

		switch (position) {
		case POSITION_CREDITS:
			// Credits
			view = factory.inflate(R.layout.about_content, null);
			WebView creditsWebView = (WebView) view
					.findViewById(R.id.about_thirdsparty_credits);
			try {
				creditsWebView.loadData(
						DataLoader.loadData(mContext, "credits_thirdparty"),
						"text/html", "UTF-8");
			} catch (IOException ioe) {
				Log.e(TAG, "Error reading changelog file!", ioe);
			}
			builder.setCancelable(true)
					.setTitle(
							mContext.getString(R.string.app_name) + " v"
									+ Utils.getActualVersionName(mContext))
					.setIcon(R.drawable.icon)
					.setView(view)
					.setPositiveButton(mContext.getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							});
			dialog = builder.create();

			break;
		case POSITION_CHANGELOG:
			// Changelog
			dialog = ChangelogBuilder.create(mContext,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					});
			break;
		case POSITION_FEEDBACK:
			mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri
					.parse(mContext.getString(R.string.github_issues_url))));
			break;
		default:
			dialog = null;
		}
		if (dialog != null) {
			mCallingDialog.dismiss();
			dialog.show();
		}
	}
}
