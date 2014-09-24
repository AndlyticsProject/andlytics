package com.github.andlyticsproject.dialog;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.github.andlyticsproject.R;

public class LongTextDialog extends SherlockDialogFragment {
	public LongTextDialog() {
		setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Dialog);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Bundle arguments = getArguments();

		View view = inflater.inflate(R.layout.longtext_dialog, container);

		TextView longText = (TextView) view.findViewById(R.id.longtext_tv);

		TextView longTextTitle = (TextView) view
				.findViewById(R.id.longtext_title);

		if (arguments.containsKey("longText")) {
			longText.setText(Html.fromHtml(arguments.getString("longText")));
		}

		if (arguments.containsKey("title")) {
			longTextTitle.setText(arguments.getInt("title"));
		}

		view.findViewById(R.id.longtext_dialog_dismiss).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						dismiss();
					}
				});

		return view;
	}
}