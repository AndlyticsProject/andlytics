package com.github.andlyticsproject;

import org.acra.ACRA;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.github.andlyticsproject.admob.AdmobAccountRemovedException;
import com.github.andlyticsproject.admob.AdmobAskForPasswordException;
import com.github.andlyticsproject.admob.AdmobGenericException;
import com.github.andlyticsproject.admob.AdmobInvalidRequestException;
import com.github.andlyticsproject.admob.AdmobInvalidTokenException;
import com.github.andlyticsproject.admob.AdmobRateLimitExceededException;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.console.AuthenticationException;
import com.github.andlyticsproject.console.DevConsoleProtocolException;
import com.github.andlyticsproject.console.MultiAccountException;
import com.github.andlyticsproject.console.NetworkException;
import com.github.andlyticsproject.dialog.CrashDialog;
import com.github.andlyticsproject.dialog.CrashDialog.CrashDialogBuilder;
import com.github.andlyticsproject.legacy.ChartActivity;
import com.github.andlyticsproject.util.Utils;

public class BaseActivity extends SherlockFragmentActivity {

	private static final String TAG = BaseActivity.class.getSimpleName();

	protected static final int REQUEST_AUTHENTICATE = 42;

	protected String packageName;
	protected String developerId;
	protected String iconFilePath;
	protected String accountName;

	private boolean refreshing;

	private boolean skipMainReload;

	protected DeveloperAccountManager developerAccountManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		developerAccountManager = DeveloperAccountManager.getInstance(getApplication());

		Bundle b = getIntent().getExtras();
		if (b != null) {
			// TODO move packageName and iconFilePath assignments to
			// BaseDetailsActivity
			// Will this effect startActivity etc with regard to null behaviour?
			// Might be best to leave them here
			packageName = b.getString(Constants.PACKAGE_NAME_PARCEL);
			developerId = b.getString(Constants.DEVELOPER_ID_PARCEL);
			iconFilePath = b.getString(Constants.ICON_FILE_PARCEL);
			accountName = b.getString(Constants.AUTH_ACCOUNT_NAME);
			developerAccountManager.selectDeveloperAccount(accountName);
		}

	}

	public void startActivity(Class<?> clazz, boolean disableAnimation, boolean skipDataReload) {
		Intent intent = new Intent(BaseActivity.this, clazz);
		intent.putExtra(Constants.PACKAGE_NAME_PARCEL, packageName);
		intent.putExtra(Constants.DEVELOPER_ID_PARCEL, developerId);
		intent.putExtra(Constants.ICON_FILE_PARCEL, iconFilePath);
		intent.putExtra(Constants.AUTH_ACCOUNT_NAME, accountName);
		if (clazz.equals(Main.class)) {
			// Main does not have singleTask set in the manifest
			// in order to facilitate easy switching between accounts using list
			// navigation
			// We therefore need to clear activity we came from before had in
			// order
			// to avoid duplicates
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		if (disableAnimation) {
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		}
		if (skipDataReload) {
			setSkipMainReload(true);
		}

		startActivity(intent);
	}

	public void startChartActivity(ChartSet set) {
		Intent intent = new Intent(BaseActivity.this, ChartActivity.class);
		intent.putExtra(Constants.PACKAGE_NAME_PARCEL, packageName);
		intent.putExtra(Constants.DEVELOPER_ID_PARCEL, developerId);
		intent.putExtra(Constants.ICON_FILE_PARCEL, iconFilePath);
		intent.putExtra(Constants.AUTH_ACCOUNT_NAME, accountName);
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		intent.putExtra(Constants.CHART_SET, set.name());

		startActivity(intent);
	}

	public void handleUserVisibleException(Exception e) {
		if (e instanceof NetworkException) {
			Toast.makeText(BaseActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG)
					.show();
		} else if (e instanceof AuthenticationException) {
			Toast.makeText(BaseActivity.this, getString(R.string.auth_error, accountName),
					Toast.LENGTH_LONG).show();
		} else if (e instanceof AdmobRateLimitExceededException) {
			Toast.makeText(BaseActivity.this, getString(R.string.admob_ratelimit_error),
					Toast.LENGTH_LONG).show();
		} else if (e instanceof AdmobAskForPasswordException) {
			Log.w(TAG, "ask for admob credentials");
			setSkipMainReload(true);
		} else if (e instanceof AdmobAccountRemovedException) {
			String wrongAccount = ((AdmobAccountRemovedException) e).getAccountName();
			Toast.makeText(BaseActivity.this,
					getString(R.string.admob_missing_error, wrongAccount), Toast.LENGTH_LONG)
					.show();
		} else if (e instanceof AdmobInvalidRequestException) {
			Toast.makeText(BaseActivity.this, getString(R.string.admob_invalid_request_error),
					Toast.LENGTH_LONG).show();
		} else if (e instanceof AdmobInvalidTokenException) {
			Toast.makeText(BaseActivity.this, getString(R.string.admob_invalid_token_error),
					Toast.LENGTH_LONG).show();
		} else if (e instanceof AdmobGenericException) {
			Log.w(TAG, e.getMessage(), e);
			Toast.makeText(BaseActivity.this, getString(R.string.admob_generic_error),
					Toast.LENGTH_LONG).show();
		} else if (e instanceof DevConsoleProtocolException) {
			int appVersionCode = Utils.getAppVersionCode(this);
			if (Preferences.getLatestVersionCode(this) > appVersionCode) {
				showNewVersionDialog(e);
			} else {
				showGoogleErrorDialog(e);
			}
		} else if (e instanceof MultiAccountException) {
			showAspErrorDialog(e);
		} else {
			showCrashDialog(e);
		}

	}

	private void showNewVersionDialog(Exception e) {
		if (!isFinishing()) {

			CrashDialog.CrashDialogBuilder builder = new CrashDialogBuilder(this);
			builder.setTitle(getString(R.string.update_required_title));
			builder.setMessage(R.string.newversion_desc);
			builder.setPositiveButton(getString(R.string.update_button),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {

							Intent goToMarket = null;
							goToMarket = new Intent(Intent.ACTION_VIEW, Uri
									.parse(getString(R.string.market_uri)));
							startActivity(goToMarket);

							dialog.dismiss();
						}

					});
			builder.setNegativeButton(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {

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
			builder.setTitle(getString(R.string.crash_dialog_title));
			builder.setMessage(R.string.crash_desc);
			builder.setPositiveButton(getString(R.string.send_report_button),
					new DialogInterface.OnClickListener() {

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
			builder.setNegativeButton(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {

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
			builder.setTitle(getString(R.string.crash_dialog_title));
			builder.setMessage(getString(R.string.remote_interface_changed_error));
			builder.setPositiveButton(getString(R.string.send_report_button),
					new DialogInterface.OnClickListener() {

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
			builder.setNegativeButton(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {

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
			builder.setTitle(getString(R.string.multiple_dev_accounts_title));
			builder.setMessage(getString(R.string.multiple_dev_accounts_error));
			builder.setPositiveButton(getString(R.string.logout),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							developerAccountManager.unselectDeveloperAccount();
							Preferences.saveSkipAutoLogin(BaseActivity.this, true);
							Intent intent = new Intent(BaseActivity.this, LoginActivity.class);
							startActivity(intent);
							overridePendingTransition(R.anim.activity_fade_in,
									R.anim.activity_fade_out);

						}

					});
			builder.setNegativeButton(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							developerAccountManager.unselectDeveloperAccount();
							Preferences.saveSkipAutoLogin(BaseActivity.this, true);
							Intent intent = new Intent(BaseActivity.this, LoginActivity.class);
							startActivity(intent);
							overridePendingTransition(R.anim.activity_fade_in,
									R.anim.activity_fade_out);
						}

					});

			builder.create().show();

		}

	}

	public ContentAdapter getDbAdapter() {
		return getAndlyticsApplication().getDbAdapter();
	}

	protected void showLoadingIndicator(ViewSwitcher switcher) {
		Animation loadingAnim = AnimationUtils.loadAnimation(this, R.anim.loading);
		loadingAnim.setInterpolator(new LinearInterpolator());

		switcher.showNext();
	}

	protected void hideLoadingIndicator(ViewSwitcher switcher) {
		switcher.showPrevious();
	}

	public AndlyticsApp getAndlyticsApplication() {
		return (AndlyticsApp) getApplication();
	}

	public boolean shouldRemoteUpdateStats() {
		long now = System.currentTimeMillis();
		long lastUpdate = developerAccountManager.getLastStatsRemoteUpdateTime(accountName);
		// never updated
		if (lastUpdate == 0) {
			return true;
		}

		return (now - lastUpdate) >= Preferences.STATS_REMOTE_UPDATE_INTERVAL;
	}

	public boolean isRefreshing() {
		return refreshing;
	}

	public void refreshStarted() {
		ensureMainThread();

		refreshing = true;
		supportInvalidateOptionsMenu();
	}

	public void refreshFinished() {
		ensureMainThread();

		refreshing = false;
		supportInvalidateOptionsMenu();
	}

	private void ensureMainThread() {
		Looper looper = Looper.myLooper();
		if (looper != null && looper != getMainLooper()) {
			throw new IllegalStateException("Only call this from your main thread.");
		}
	}

	protected void setSkipMainReload(boolean skipMainReload) {
		this.skipMainReload = skipMainReload;
	}

	protected boolean isSkipMainReload() {
		return skipMainReload;
	}

	public String getPackage() {
		return packageName;
	}

	public String getDeveloperId() {
		return developerId;
	}

	public String getAccountName() {
		return accountName;
	}

}
