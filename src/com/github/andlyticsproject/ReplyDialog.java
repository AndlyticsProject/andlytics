package com.github.andlyticsproject;

import android.support.v4.app.DialogFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

public class ReplyDialog extends DialogFragment {

	public static final int DEVELOPER_REPLY_MAX_CHARACTERS = 350;

	static final String ARG_REPLY = "reply";
	static final String ARG_UNIQUE_ID = "uniqueId";

	private String commentUniqueId;

	public ReplyDialog() {
		setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.comment_reply_dialog,
				container);

		final EditText replyText = (EditText) view
				.findViewById(R.id.comment_reply_dialog_text);
		final TextView comment_reply_dialog_counter = (TextView) view
				.findViewById(R.id.comment_reply_dialog_counter);
		final TextView comment_reply_dialog_max_characters = (TextView) view
				.findViewById(R.id.comment_reply_dialog_max_characters);

		final View okButton = view
				.findViewById(R.id.comment_reply_dialog_positive_button);

		// show keyboard
		replyText.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboard();
				}
			}
		});

		// live count comment reply characters
		replyText.addTextChangedListener(new TextWatcher() {
			public void beforeTextChanged(CharSequence reply, int start,
					int count, int after) {
			}

			public void onTextChanged(CharSequence reply, int start,
					int before, int count) {
				// set counter view to current comment length
				comment_reply_dialog_counter.setText(String.valueOf(reply
						.length()));

				// enable/disable button and add color-coding
				if (ReplyDialog.DEVELOPER_REPLY_MAX_CHARACTERS < reply
						.length()) {
					okButton.setEnabled(false);
					comment_reply_dialog_counter.setTextColor(Color.RED);
					comment_reply_dialog_max_characters
							.setTextColor(Color.RED);
				} else if (1 > reply.length()) {
					okButton.setEnabled(false);
				} else {
					okButton.setEnabled(true);
					comment_reply_dialog_counter
							.setTextColor(getResources().getColor(
									R.color.greyText));
					comment_reply_dialog_max_characters
							.setTextColor(getResources().getColor(
									R.color.greyText));
				}
			}

			public void afterTextChanged(Editable e) {
			}
		});

		Bundle args = getArguments();
		if (args.containsKey(ARG_UNIQUE_ID)) {
			commentUniqueId = args.getString(ARG_UNIQUE_ID);
		}
		if (args.containsKey(ARG_REPLY)) {
			replyText.setText(args.getString(ARG_REPLY));
		}

		view.findViewById(R.id.comment_reply_dialog_negative_button)
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						dismiss();
					}
				});
		view.findViewById(R.id.comment_reply_dialog_positive_button)
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						String reply = replyText.getText().toString();
						CommentReplier activity = (CommentReplier) getActivity();
						if (activity != null) {
							activity.replyToComment(commentUniqueId, reply);
						}
						dismiss();
					}
				});

		replyText.requestFocus();
		showKeyboard();

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		getDialog().getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		super.onViewCreated(view, savedInstanceState);
	}

	private void showKeyboard() {
		getDialog()
				.getWindow()
				.setSoftInputMode(
						WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
								| WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}
}