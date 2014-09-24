package com.github.andlyticsproject.dialog;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.andlyticsproject.R;

import java.net.MalformedURLException;
import java.net.URL;

public class AddEditLinkDialog extends DialogFragment {
	private EditText urlInput;
	private EditText nameInput;

	private Long id = null;
	private String name = null;
	private String url = null;

	private OnFinishAddEditLinkDialogListener onFinishAddEditLinkDialogListener;

	public interface OnFinishAddEditLinkDialogListener {
		void onFinishAddEditLink(String url, String name, Long id);
	}

	public AddEditLinkDialog() {
		setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Dialog);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Bundle arguments = getArguments();

		View view = inflater.inflate(R.layout.appinfo_link_addedit_dialog,
				container);

		if (arguments.containsKey("id")) {
			id = Long.valueOf(arguments.getLong("id"));
			name = arguments.getString("name");
			url = arguments.getString("url");
		}

		if (id != null) {
			TextView title = (TextView) view
					.findViewById(R.id.appinfo_link_addedit_dialog_title);
			title.setText(R.string.appinfo_link_addedit_dialog_title_edit);
		}

		urlInput = (EditText) view
				.findViewById(R.id.appinfo_link_addedit_dialog_url_input);
		nameInput = (EditText) view
				.findViewById(R.id.appinfo_link_addedit_dialog_name_input);

		if (url != null) {
			urlInput.setText(url);
		}

		if (name != null) {
			nameInput.setText(name);
		}

		view.findViewById(R.id.appinfo_link_addedit_dialog_positive_button)
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						String urlString = urlInput.getText().toString();
						String nameString = nameInput.getText().toString();

						urlString = urlString.toString();

						try {
							URL url = new URL(urlString);
							urlString = url.toString();

							if (nameString.trim().length() == 0) {
								nameString = url.getHost();
							}

							onFinishAddEditLinkDialogListener
									.onFinishAddEditLink(urlString, nameString,
											id);
							dismiss();
						} catch (MalformedURLException e) {
							Toast.makeText(
									AddEditLinkDialog.this.getActivity(),
									getString(R.string.appinfo_link_addedit_dialog_not_url),
									Toast.LENGTH_LONG).show();
						}
					}
				});

		view.findViewById(R.id.appinfo_link_addedit_dialog_negative_button)
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						dismiss();
					}
				});

		return view;
	}

	public void setOnFinishAddEditLinkDialogListener(
			OnFinishAddEditLinkDialogListener listener) {
		onFinishAddEditLinkDialogListener = listener;
	}
}