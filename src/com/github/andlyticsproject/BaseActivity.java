
package com.github.andlyticsproject;

import org.acra.ACRA;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.app.SherlockActivity;
import com.github.andlyticsproject.admob.AdmobAccountRemovedException;
import com.github.andlyticsproject.admob.AdmobAskForPasswordException;
import com.github.andlyticsproject.admob.AdmobGenericException;
import com.github.andlyticsproject.admob.AdmobInvalidRequestException;
import com.github.andlyticsproject.admob.AdmobInvalidTokenException;
import com.github.andlyticsproject.admob.AdmobRateLimitExceededException;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.dialog.CrashDialog;
import com.github.andlyticsproject.dialog.CrashDialog.CrashDialogBuilder;
import com.github.andlyticsproject.exception.AuthenticationException;
import com.github.andlyticsproject.exception.DeveloperConsoleException;
import com.github.andlyticsproject.exception.InvalidJSONResponseException;
import com.github.andlyticsproject.exception.MultiAccountAcception;
import com.github.andlyticsproject.exception.NetworkException;
import com.github.andlyticsproject.exception.NoCookieSetException;
import com.github.andlyticsproject.exception.SignupException;

public class BaseActivity extends SherlockActivity {

	private static final String TAG = BaseActivity.class.getSimpleName();

	protected String packageName;
	protected String iconFilePath;
	protected String accountName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle b = getIntent().getExtras();
		if (b != null) {
			// TODO move packageName and iconFilePath assignments to BaseDetailsActivity
			// Will this effect startActivity etc with regard to null behaviour?
			// Might be best to leave them here
			packageName = b.getString(Constants.PACKAGE_NAME_PARCEL);
			iconFilePath = b.getString(Constants.ICON_FILE_PARCEL);
			accountName = b.getString(Constants.AUTH_ACCOUNT_NAME);
			Preferences.saveAccountName(this, accountName);
		}

	}

	public void startActivity(Class<?> clazz, boolean disableAnimation, boolean skipDataReload) {
		Intent intent = new Intent(BaseActivity.this, clazz);
		intent.putExtra(Constants.PACKAGE_NAME_PARCEL, packageName);
		intent.putExtra(Constants.ICON_FILE_PARCEL, iconFilePath);
		intent.putExtra(Constants.AUTH_ACCOUNT_NAME, accountName);
		if (clazz.equals(Main.class)) {
			// Main does not have singleTask set in the manifest
			// in order to facilitate easy switching between accounts using list navigation
			// We therefore need to clear activity we came from before had in order
			// to avoid duplicates
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		if (disableAnimation) {
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		}
		if (skipDataReload) {
			getAndlyticsApplication().setSkipMainReload(true);
		}

		startActivity(intent);
	}

	public void startChartActivity(ChartSet set) {
		Intent intent = new Intent(BaseActivity.this, ChartActivity.class);
		intent.putExtra(Constants.PACKAGE_NAME_PARCEL, packageName);
		intent.putExtra(Constants.ICON_FILE_PARCEL, iconFilePath);
		intent.putExtra(Constants.AUTH_ACCOUNT_NAME, accountName);
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		intent.putExtra(Constants.CHART_SET, set.name());

		startActivity(intent);
	}

	public void handleUserVisibleException(Exception e) {
		// TODO Clean these up and put them in strings.xml so that they can be translated
		if (e instanceof NetworkException) {
			Toast.makeText(BaseActivity.this,
					"A network error has occurred. Please try again later.", Toast.LENGTH_LONG)
					.show();
		} else if (e instanceof SignupException) {
			Toast.makeText(BaseActivity.this, accountName
					+ " is not an android developer account, sign up at:\n\n"
					+ e.getMessage(), Toast.LENGTH_LONG).show();
			Toast.makeText(BaseActivity.this, accountName
					+ " is not an android developer account, sign up at:\n\n"
					+ e.getMessage(), Toast.LENGTH_LONG).show();
		} else if (e instanceof AuthenticationException || e instanceof NoCookieSetException) {

			Toast.makeText(BaseActivity.this, "authentication failed for: " + accountName,
					Toast.LENGTH_LONG).show();

		} else if (e instanceof AdmobRateLimitExceededException) {

			Toast.makeText(BaseActivity.this,
					"Can't load Admob data because Admob has restricted the number of requests from this app, try again later.",
					Toast.LENGTH_LONG).show();

		} else if (e instanceof AdmobAskForPasswordException) {

			Log.w(TAG, "ask for admob credentials");
			getAndlyticsApplication().setSkipMainReload(true);

		} else if (e instanceof AdmobAccountRemovedException) {

			Toast.makeText(BaseActivity.this, "AdMob account \""
					+ "\" is missing. If this happens repeatedly try moving Andlytics from sdcard to internal storage.",
					Toast.LENGTH_LONG).show();
			Toast.makeText(BaseActivity.this, "AdMob account \""
					+ ((AdmobAccountRemovedException) e).getAccountName()
					+ "\" is missing. If this happens repeatedly try moving Andlytics from sdcard to internal storage.",
					Toast.LENGTH_LONG).show();

		} else if (e instanceof AdmobInvalidRequestException) {

			Toast.makeText(BaseActivity.this, "Error while requesting AdMob API", Toast.LENGTH_LONG).show();

		} else if (e instanceof AdmobInvalidTokenException) {

			Toast.makeText(BaseActivity.this,
					"Error while authenticating admob account. Please try again later.",
					Toast.LENGTH_LONG).show();

		} else if (e instanceof AdmobGenericException) {

			Log.w(TAG, e.getMessage(), e);

			Toast.makeText(BaseActivity.this,
					"Unabled to load Admob data, please try again later.", Toast.LENGTH_LONG)
					.show();

		} else if (e instanceof DeveloperConsoleException) {

			int appVersionCode = getAppVersionCode(this);
			if (Preferences.getLatestVersionCode(this) > appVersionCode) {
				showNewVersionDialog(e);
			} else {
				showCrashDialog(e);
			}

		} else if (e instanceof InvalidJSONResponseException) {

			int appVersionCode = getAppVersionCode(this);
			if (Preferences.getLatestVersionCode(this) > appVersionCode) {
				showNewVersionDialog(e);
			} else {
				showGoogleErrorDialog(e);
			}

		} else if (e instanceof MultiAccountAcception) {
			showAspErrorDialog(e);
		}
	}

	private void showNewVersionDialog(Exception e) {
		if (!isFinishing()) {

			CrashDialog.CrashDialogBuilder builder = new CrashDialogBuilder(this);
			builder.setTitle("Sorry, update required.");
			builder.setMessage(R.string.newversion_desc);
			builder.setPositiveButton("update", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {

					Intent goToMarket = null;
					goToMarket = new Intent(Intent.ACTION_VIEW, Uri
							.parse("market://details?id=com.github.andlyticsproject"));
					startActivity(goToMarket);

					dialog.dismiss();
				}

			});
			builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}

			});

			builder.create().show();
		}

	}

	protected void showCrashDialog(final Exception e) {

		if (!isFinishing()) {

			CrashDialog.CrashDialogBuilder builder = new CrashDialogBuilder(this);
			builder.setTitle("Sorry...");
			builder.setMessage(R.string.crash_desc);
			builder.setPositiveButton("send report", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {

					if (!isFinishing()) {

						Thread thread = new Thread(new Runnable() {

							@Override
							public void run() {
								sendAracReport(e, true);
							}

						});
						thread.run();
						dialog.dismiss();
					}

				}

			});
			builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}

			});

			builder.create().show();
		}

	}

	private void sendAracReport(Exception e, boolean userTriggered) {
		ACRA.getErrorReporter().handleSilentException(e);
	}

	protected void showGoogleErrorDialog(final Exception e) {

		if (!isFinishing()) {

			CrashDialog.CrashDialogBuilder builder = new CrashDialogBuilder(this);
			builder.setTitle("Sorry... ");
			builder.setMessage("Updates are currently not possible, probably because the remote interface changed.\n\nPlease help us to fix this by sending us error report data. Information about the error (stacktrace) and information about your device such as device manufacturer's name, device model number, operating system, etc. will be sent to help us identifying the problem.\n\nThank you!");
			builder.setPositiveButton("send report", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {

					Thread thread = new Thread(new Runnable() {

						@Override
						public void run() {
							sendAracReport(e, true);
						}
					});
					thread.run();
					dialog.dismiss();
				}

			});
			builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}

			});

			builder.create().show();

		}

	}

	protected void showAspErrorDialog(final Exception e) {

		if (!isFinishing()) {

			CrashDialog.CrashDialogBuilder builder = new CrashDialogBuilder(this);
			builder.setTitle("Multiple Developer Accounts");
			builder.setMessage("Your account is linked to multiple developer consoles. This feature is not supported in andlytics.\n\nYou can unlink additional accounts or ask Google for a public app stats API at: \n\nhttp://support.google.com/googleplay\n\nSorry :(");
			builder.setPositiveButton("logout", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					Preferences.removeAccountName(BaseActivity.this);
					Preferences.saveSkipAutoLogin(BaseActivity.this, true);
					Intent intent = new Intent(BaseActivity.this, LoginActivity.class);
					startActivity(intent);
					overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);

				}

			});
			builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					Preferences.removeAccountName(BaseActivity.this);
					Preferences.saveSkipAutoLogin(BaseActivity.this, true);
					Intent intent = new Intent(BaseActivity.this, LoginActivity.class);
					startActivity(intent);
					overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
				}

			});

			builder.create().show();

		}

	}

	public ContentAdapter getDbAdapter() {
		return getAndlyticsApplication().getDbAdapter();
	}

	protected void authenticateAccountFromPreferences(boolean invalidateToken,
			AuthenticationCallback callback) {

		String accountName = Preferences.getAccountName(this);

		if (accountName != null) {
			AccountManager manager = AccountManager.get(this);
			Account[] accounts = manager.getAccountsByType(Constants.ACCOUNT_TYPE_GOOGLE);
			int size = accounts.length;
			for (int i = 0; i < size; i++) {
				Account account = accounts[i];
				if (accountName.equals(account.name)) {
					if (invalidateToken) {
						manager.invalidateAuthToken(Constants.ACCOUNT_TYPE_GOOGLE,
								getAndlyticsApplication().getAuthToken());
					}
					getAndlyticsApplication().setAuthToken(null);
					authenticateAccount(manager, account, callback);
				}
			}
		} else {
			getAndlyticsApplication().setAuthToken(null);
		}
	}

	private void authenticateAccount(final AccountManager manager, final Account account,
			final AuthenticationCallback callback) {

		Preferences.saveAccountName(this, account.name);

		AccountManager accountManager = AccountManager.get(this.getApplicationContext());

		final AccountManagerCallback<Bundle> myCallback = new AccountManagerCallback<Bundle>() {

			public void run(final AccountManagerFuture<Bundle> arg0) {
				try {

					String authToken = arg0.getResult().getString(AccountManager.KEY_AUTHTOKEN);

					if (authToken != null) {
						//do something with the auth token
						//  got token form manager - set in application an exit
						getAndlyticsApplication().setAuthToken(authToken);

						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								callback.authenticationSuccess();
							}
						});

					} else {

						Log.e(TAG, "auth token is null, authentication failed");
						//not expected at all
					}

				} catch (Exception e) {
					getAndlyticsApplication().setSkipMainReload(true);
					Log.e(TAG, "error during authentication", e);
					//error
				}

			}

		};

		accountManager.getAuthToken(account, Constants.AUTH_TOKEN_TYPE_ANDROID_DEVLOPER, null,
				BaseActivity.this, myCallback, null);

		/*

		Bundle bundle;
		try {
			bundle = manager.getAuthToken(account, Constants.AUTH_TOKEN_TYPE_ANDROID_DEVLOPER, true, null, null).getResult();

			if (bundle.containsKey(AccountManager.KEY_INTENT)) {

				// ask user for permission - launch account manager intent
				Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
				int flags = intent.getFlags();
				flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
				intent.setFlags(flags);
				getAndlyticsApplication().setAuthToken(null);
				getAndlyticsApplication().setRunningAuthenticationRequestIntent(true);
				startActivityForResult(intent, REQUEST_AUTHENTICATE);

			} else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {

				//  got token form manager - set in application an exit
				String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
				getAndlyticsApplication().setAuthToken(authToken);

				//manager.invalidateAuthToken(Constants.ACCOUNT_TYPE_GOOGLE, authToken);


			}
		} catch (OperationCanceledException e1) {
			e1.printStackTrace();
		} catch (AuthenticatorException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}*/

	}

	/*
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_AUTHENTICATE:

			getAndlyticsApplication().setRunningAuthenticationRequestIntent(false);

			if (resultCode == RESULT_OK) {

				AsyncTask<Void, Void, Void> t = new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						authenticateAccountFromPreferences(false);
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						onPostAuthentication();
					}


				};
				t.execute();

			} else {
			    getAndlyticsApplication().setSkipMainReload(true);
				Log.e("Andlytics", "REQUEST_AUTHENTICATE, result is NOT ok");
			}
		}
	}*/

	protected void showLoadingIndecator(ViewSwitcher switcher) {
		Animation loadingAnim = AnimationUtils.loadAnimation(this, R.anim.loading);
		loadingAnim.setInterpolator(new LinearInterpolator());

		switcher.showNext();
	}

	protected void hideLoadingIndecator(ViewSwitcher switcher) {
		switcher.showPrevious();
	}

	public AndlyticsApp getAndlyticsApplication() {
		return (AndlyticsApp) getApplication();
	}

	protected void onPostAuthentication() {
	}

	public static int getAppVersionCode(Context context) {
		try {
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return pinfo.versionCode;
		} catch (NameNotFoundException e) {
			Log.e(AndlyticsApp.class.getSimpleName(), "unable to read version code", e);
		}
		return 0;
	}
}
