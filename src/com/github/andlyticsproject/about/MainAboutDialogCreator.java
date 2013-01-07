package com.github.andlyticsproject.about;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.github.andlyticsproject.R;

/**
 * Creator implementation for main about dialog.
 */
public final class MainAboutDialogCreator {
	/** Private Constructor. */
	private MainAboutDialogCreator() {
	}

	/**
	 * creates the main 'About' dialog with the necessary listeners already set.
	 * 
	 * @param context
	 *            The context
	 * @return the main 'About' dialog.
	 */
	public static Dialog createMainAboutDialog(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		// TODO: Items should be inflated by XML
		// TODO: Add FAQ
		String[] aboutItems = { context.getString(R.string.about_credits),
				// context.getString(R.string.mainabout_faq),
				context.getString(R.string.changelog_title),
				context.getString(R.string.feedback)};
		builder.setTitle(R.string.about_title);
		ListAdapter adapter = new ArrayAdapter<String>(context,
				R.layout.about_list_row, aboutItems);

		builder.setAdapter(adapter, new MainAboutOnItemListener(context,
				builder.create()));
		return builder.create();
	}
}
