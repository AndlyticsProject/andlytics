package com.github.andlyticsproject.dialog;


import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.Selection;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import org.acra.ACRA;
import org.acra.ErrorReporter;

import com.github.andlyticsproject.R;
import com.github.andlyticsproject.exception.FeedbackException;

public class FeedbackDialog extends Dialog {

	private EditText inputField;

	public FeedbackDialog(Context context, int theme) {
		super(context, theme);
	}

	public FeedbackDialog(Context context) {
		super(context);
	}

	/**
	 * Helper class for creating a custom dialog
	 */
	public static class FeedbackDialogBuilder {

		private Context context;
		private String title;
		private String message;
		private String positiveViewText;
		private String negativeViewText;

		private DialogInterface.OnClickListener positiveViewClickListener, negativeViewClickListener;

		public FeedbackDialogBuilder(Context context) {
			this.context = context;
		}

		/**
		 * Set the Dialog message from String
		 * 
		 * @param title
		 * @return
		 */
		public FeedbackDialogBuilder setMessage(String message) {
			this.message = message;
			return this;
		}

		/**
		 * Set the Dialog message from resource
		 * 
		 * @param title
		 * @return
		 */
		public FeedbackDialogBuilder setMessage(int message) {
			this.message = (String) context.getText(message);
			return this;
		}

		/**
		 * Set the Dialog title from resource
		 * 
		 * @param title
		 * @return
		 */
		public FeedbackDialogBuilder setTitle(int title) {
			this.title = (String) context.getText(title);
			return this;
		}

		/**
		 * Set the Dialog title from String
		 * 
		 * @param title
		 * @return
		 */
		public FeedbackDialogBuilder setTitle(String title) {
			this.title = title;
			return this;
		}

		/**
		 * Set the positive button resource and it's listener
		 * 
		 * @param positiveViewText
		 * @param listener
		 * @return
		 */
		public FeedbackDialogBuilder setPositiveButton(int positiveViewText, DialogInterface.OnClickListener listener) {
			this.positiveViewText = (String) context.getText(positiveViewText);
			this.positiveViewClickListener = listener;
			return this;
		}

		/**
		 * Set the positive button text and it's listener
		 * 
		 * @param positiveViewText
		 * @param listener
		 * @return
		 */
		public FeedbackDialogBuilder setPositiveButton(String positiveViewText, DialogInterface.OnClickListener listener) {
			this.positiveViewText = positiveViewText;
			this.positiveViewClickListener = listener;
			return this;
		}

		/**
		 * Set the negative button resource and it's listener
		 * 
		 * @param negativeViewText
		 * @param listener
		 * @return
		 */
		public FeedbackDialogBuilder setNegativeButton(int negativeViewText, DialogInterface.OnClickListener listener) {
			this.negativeViewText = (String) context.getText(negativeViewText);
			this.negativeViewClickListener = listener;
			return this;
		}

		/**
		 * Set the negative button text and it's listener
		 * 
		 * @param negativeViewText
		 * @param listener
		 * @return
		 */
		public FeedbackDialogBuilder setNegativeButton(String negativeViewText, DialogInterface.OnClickListener listener) {
			this.negativeViewText = negativeViewText;
			this.negativeViewClickListener = listener;
			return this;
		}

		/**
		 * Create the custom dialog
		 */
		public FeedbackDialog create(final String defaultMessage, final Application application) {

			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			// instantiate the dialog with the custom Theme
			final FeedbackDialog dialog = new FeedbackDialog(context, R.style.Dialog);
			View layout = inflater.inflate(R.layout.feedback_dialog, null);
			final EditText input = (EditText) layout.findViewById(R.id.info_dialog_input);

			input.setText(defaultMessage);
			Editable etext = input.getText();
			int position = etext.length();  // end of buffer, for instance
			Selection.setSelection(etext, position);
			
			dialog.setInputField(input);
			
			dialog.addContentView(layout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

			// set the dialog title
			((TextView) layout.findViewById(R.id.info_dialog_title)).setText(title);
			// set the confirm button
			if (positiveViewText != null) {
				((TextView) layout.findViewById(R.id.info_dialog_positive_button_text)).setText(positiveViewText);
				if (positiveViewClickListener != null) {
					((View) layout.findViewById(R.id.info_dialog_positive_button))
							.setOnClickListener(new View.OnClickListener() {
								public void onClick(View v) {

								    final String msg = input.getText().toString();
								    
				                    Thread thread = new Thread(new Runnable() {

				                        @Override
				                        public void run() {
				                            ACRA.init(application);
				                            
				                            ErrorReporter.getInstance().handleSilentException(new FeedbackException(""));
				                            ErrorReporter.getInstance().disable();
				                        }
				                    });
				                    thread.run();
				                    
				                    input.setText(defaultMessage);
				                    Editable etext = input.getText();
				                    int position = etext.length();  // end of buffer, for instance
				                    Selection.setSelection(etext, position);

								    
									positiveViewClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
								}
							});
				}
			} else {
				// if no confirm button just set the visibility to GONE
				layout.findViewById(R.id.info_dialog_positive_button).setVisibility(View.GONE);
			}
			// set the cancel button
			if (negativeViewText != null) {
				((TextView) layout.findViewById(R.id.info_dialog_negative_button_text)).setText(negativeViewText);
				if (negativeViewClickListener != null) {
					((View) layout.findViewById(R.id.info_dialog_negative_button))
							.setOnClickListener(new View.OnClickListener() {
								public void onClick(View v) {
									negativeViewClickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
								}
							});
				}
			} else {
				// if no confirm button just set the visibility to GONE
				layout.findViewById(R.id.info_dialog_negative_button).setVisibility(View.GONE);
			}
			// set the content message
			if (message != null) {
				((TextView) layout.findViewById(R.id.info_dialog_text)).setText(message);
			}
			dialog.setContentView(layout);

			
			return dialog;
		}

	}

	public void setInputField(EditText inputField) {
		this.inputField = inputField;
	}

	public EditText getInputField() {
		return inputField;
	}

}