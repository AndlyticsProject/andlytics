package com.github.andlyticsproject;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.andlyticsproject.admob.AdmobAccountAuthenticator;
import com.github.andlyticsproject.admob.AdmobAuthenticationUtilities;
import com.github.andlyticsproject.admob.AdmobRequest;

public class AdmobAuthenticatorActivity extends AccountAuthenticatorActivity {

	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";
	public static final String PARAM_PASSWORD = "password";

	private AccountManager mAccountManager;
	private Thread mAuthThread;
	private String mAuthtoken;
	private String mAuthtokenType;
	private Boolean mConfirmCredentials = false;

	private final Handler mHandler = new Handler();

	private TextView mMessageView;
	private String mPassword;
	private String mUsername;
	private EditText mPasswordEdit;
	private EditText mUsernameEdit;

	private boolean mRequestNewAccount = false;
	private View mOkButton;

	@Override
	public void onCreate(Bundle neato) {

		super.onCreate(neato);
		mAccountManager = AccountManager.get(this);

		final Intent intent = getIntent();
		mUsername = intent.getStringExtra(PARAM_USERNAME);
		mAuthtokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);
		mRequestNewAccount = mUsername == null;
		mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRM_CREDENTIALS, false);
		initLayout();
	}

	private void initLayout() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.admob_login);

		mMessageView = (TextView) findViewById(R.id.admob_login_message);
		mUsernameEdit = (EditText) findViewById(R.id.admob_login_username_edit);
		mPasswordEdit = (EditText) findViewById(R.id.admob_login_password_edit);
		if (mUsername == null) {
			mUsername = DeveloperAccountManager.getInstance(this).getSelectedDeveloperAccount()
					.getName();
		}
		mUsernameEdit.setText(mUsername);
		if (mUsername != null) {
			mPasswordEdit.requestFocusFromTouch();
		}
		if (getMessage() != null) {
			mMessageView.setText(getMessage());
		}
		mOkButton = findViewById(R.id.admob_login_ok_button);

		mOkButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				handleLogin(v);
			}
		});
	}

	private CharSequence getMessage() {
		if (TextUtils.isEmpty(mUsername)) {
			CharSequence msg = getString(R.string.admob_data_prompt);
			return msg;
		}
		if (TextUtils.isEmpty(mPassword)) {
			return getString(R.string.admob_password_prompt);
		}
		return null;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		ProgressDialog dialog = new ProgressDialog(this);
		dialog.setMessage(getString(R.string.admob_authenticating));
		dialog.setIndeterminate(true);
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				if (mAuthThread != null) {
					mAuthThread.interrupt();
					finish();
				}
			}

		});

		return dialog;
	}

	public void handleLogin(View v) {
		if (mRequestNewAccount) {
			mUsername = mUsernameEdit.getText().toString();
		}
		mPassword = mPasswordEdit.getText().toString();
		if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)) {
			mMessageView.setText(getMessage());
		} else {
			showProgress();
			mAuthThread = AdmobAuthenticationUtilities.attemptAuth(mUsername, mPassword, mHandler,
					AdmobAuthenticatorActivity.this);

		}
	}

	private void finishConfirmCredentials(boolean result) {
		Account account = new Account(mUsername, AdmobAccountAuthenticator.ACCOUNT_TYPE_ADMOB);
		mAccountManager.setPassword(account, mPassword);
		Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	private void finishLogin() {
		Account account = new Account(mUsername, AdmobAccountAuthenticator.ACCOUNT_TYPE_ADMOB);
		if (mRequestNewAccount) {
			mAccountManager.addAccountExplicitly(account, mPassword, null);
			ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
		} else {
			mAccountManager.setPassword(account, mPassword);
		}
		Intent intent = new Intent();
		mAuthtoken = mPassword;
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AdmobAccountAuthenticator.ACCOUNT_TYPE_ADMOB);
		if (mAuthtokenType != null && mAuthtokenType.equals(AdmobAccountAuthenticator.AUTHTOKEN_TYPE_ADMOB)) {
			intent.putExtra(AccountManager.KEY_AUTHTOKEN, mAuthtoken);
		}
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	@SuppressWarnings("deprecation")
	private void showProgress() {
		showDialog(0);
	}

	@SuppressWarnings("deprecation")
	private void hideProgress() {
		try {
			dismissDialog(0);
		} catch (IllegalArgumentException e) {
			Log.e("AdMobAuthenticator", "dismissDialog without open", e);
		}
	}

	public void onAuthenticationResult(String result) {
		hideProgress();
		if ("true".equalsIgnoreCase(result)) {
			if (!mConfirmCredentials) {
				finishLogin();
			} else {
				finishConfirmCredentials(true);
			}
		} else {
			if (AdmobRequest.ERROR_NETWORK_ERROR.equals(result)) {
				Toast.makeText(AdmobAuthenticatorActivity.this, getString(R.string.network_error),
						Toast.LENGTH_SHORT).show();
			}
			if (AdmobRequest.ERROR_RATE_LIMIT_EXCEEDED.equals(result)) {
				Toast.makeText(AdmobAuthenticatorActivity.this,
						getString(R.string.admob_ratelimit_error), Toast.LENGTH_LONG).show();
			}
			if (AdmobRequest.ERROR_REQUEST_INVALID.equals(result)) {
				Toast.makeText(AdmobAuthenticatorActivity.this,
						getString(R.string.admob_linked_accounts_error), Toast.LENGTH_LONG).show();
			}

			if (mRequestNewAccount) {
				mMessageView.setText(getString(R.string.admob_auth_failed));
			} else {
				mMessageView.setText(getString(R.string.admob_wrong_password));
			}
		}
	}
}
