package com.github.andlyticsproject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.Preferences.StatsMode;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.admob.AdmobRequest;
import com.github.andlyticsproject.console.AuthenticationException;
import com.github.andlyticsproject.console.DevConsoleProtocolException;
import com.github.andlyticsproject.console.v2.DevConsoleRegistry;
import com.github.andlyticsproject.console.v2.DevConsoleV2;
import com.github.andlyticsproject.console.v2.HttpClientFactory;
import com.github.andlyticsproject.io.StatsCsvReaderWriter;
import com.github.andlyticsproject.model.Admob;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.sync.AutosyncHandler;
import com.github.andlyticsproject.sync.NotificationHandler;
import com.github.andlyticsproject.util.ChangelogBuilder;
import com.github.andlyticsproject.util.Utils;

public class Main extends BaseActivity implements AuthenticationCallback, OnNavigationListener {

	/** Key for latest version code preference. */
	private static final String LAST_VERSION_CODE_KEY = "last_version_code";

	public static final String TAG = Main.class.getSimpleName();

	private boolean cancelRequested;
	private ListView mainListView;
	private ContentAdapter db;
	private TextView statusText;
	private ViewSwitcher mainViewSwitcher;
	private MainListAdapter adapter;
	public boolean dotracking;
	private View footer;

	private boolean isAuthenticationRetry;
	public Animation aniPrevIn;
	private StatsMode currentStatsMode;
	private boolean refreshing;

	private MenuItem statsModeMenuItem;

	private List<String> accountsList;
	
	private DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);

	private static final int REQUEST_CODE_MANAGE_ACCOUNTS = 99;

	/** Called when the activity is first created. */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		db = getDbAdapter();
		LayoutInflater layoutInflater = getLayoutInflater();
		
		// Hack in case the account is hidden and then the app is killed
		// which means when it starts up next, it goes straight to the account
		// even though it shouldn't. To work around this, just mark it as not
		// hidden
		// in the sense that that change they made never got applied
		// TODO Do something clever in login activity to prevent this while
		// keeping the ability
		// to block going 'back'
		Preferences.saveIsHiddenAccount(this, accountName, false);

		updateAccountsList();

		// setup main list
		mainListView = (ListView) findViewById(R.id.main_app_list);
		mainListView.addHeaderView(layoutInflater.inflate(R.layout.main_list_header, null), null,
				false);
		footer = layoutInflater.inflate(R.layout.main_list_footer, null);
		footer.setVisibility(View.INVISIBLE);
		mainListView.addFooterView(footer, null, false);
		adapter = new MainListAdapter(this, accountName, db, currentStatsMode);
		mainListView.setAdapter(adapter);
		mainViewSwitcher = (ViewSwitcher) findViewById(R.id.main_viewswitcher);

		// status & progess bar
		statusText = (TextView) findViewById(R.id.main_app_status_line);

		aniPrevIn = AnimationUtils.loadAnimation(Main.this, R.anim.activity_fade_in);

		dotracking = true;
		isAuthenticationRetry = false;

		currentStatsMode = Preferences.getStatsMode(this);
		updateStatsMode();

		final List<AppInfo> lastAppList = (List<AppInfo>) getLastNonConfigurationInstance();
		if (lastAppList != null) {
			getAndlyticsApplication().setSkipMainReload(true);

		}

		// show changelog
		if (isUpdate()) {
			showChangelog();
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (!accountsList.get(itemPosition).equals(accountName)) {
			// Only switch if it is a new account
			Preferences.removeAccountName(Main.this);
			Intent intent = new Intent(Main.this, Main.class);
			intent.putExtra(Constants.AUTH_ACCOUNT_NAME, accountsList.get(itemPosition));
			startActivity(intent);
			overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
			// Call finish to ensure we don't get multiple activities running
			finish();
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		boolean mainSkipDataReload = getAndlyticsApplication().isSkipMainReload();

		// TODO We shouldn't be reloading in every onResume
		// When we move this, make sure we move to using startActivityForResult
		// for the preferences
		// to ensure that we do update if hidden apps are changed

		// more TODO Should always show data from DB first, and then
		// trigger remote call if necessary
		// Revise the whole application global flag thing
		if (!mainSkipDataReload) {
			Utils.execute(new LoadDbEntries(), true);
		} else {
			Utils.execute(new LoadDbEntries(), false);
		}

		getAndlyticsApplication().setSkipMainReload(false);

		AndlyticsApp.getInstance().setIsAppVisible(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		getSupportMenuInflater().inflate(R.menu.main_menu, menu);
		statsModeMenuItem = menu.findItem(R.id.itemMainmenuStatsMode);
		if (refreshing)
			menu.findItem(R.id.itemMainmenuRefresh).setActionView(
					R.layout.action_bar_indeterminate_progress);
		updateStatsMode();
		return true;
	}

	/**
	 * Called if item in option menu is selected.
	 * 
	 * @param item
	 *            The chosen menu item
	 * @return boolean true/false
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i = null;
		switch (item.getItemId()) {
		case R.id.itemMainmenuRefresh:
			authenticateAccountFromPreferences(false, Main.this);
			break;
		case R.id.itemMainmenuImport:
			File fileToImport = StatsCsvReaderWriter.getExportFileForAccount(accountName);
			if (!fileToImport.exists()) {
				Toast.makeText(this,
						getString(R.string.import_no_stats_file, fileToImport.getAbsolutePath()),
						Toast.LENGTH_LONG).show();
				return true;
			}

			Intent importIntent = new Intent(this, ImportActivity.class);
			importIntent.setAction(Intent.ACTION_VIEW);
			importIntent.setData(Uri.fromFile(fileToImport));
			startActivity(importIntent);
			break;
		case R.id.itemMainmenuExport:
			Intent exportIntent = new Intent(this, ExportActivity.class);
			exportIntent.putExtra(ExportActivity.EXTRA_ACCOUNT_NAME, accountName);
			startActivity(exportIntent);
			break;
		case R.id.itemMainmenuFeedback:
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse(getString(R.string.github_issues_url))));
			break;
		case R.id.itemMainmenuPreferences:
			i = new Intent(this, PreferenceActivity.class);
			i.putExtra(Constants.AUTH_ACCOUNT_NAME, accountName);
			startActivity(i);
			break;
		case R.id.itemMainmenuStatsMode:
			if (currentStatsMode.equals(StatsMode.PERCENT)) {
				currentStatsMode = StatsMode.DAY_CHANGES;
			} else {
				currentStatsMode = StatsMode.PERCENT;
			}
			updateStatsMode();
			break;
		case R.id.itemMainmenuAccounts:
			i = new Intent(this, LoginActivity.class);
			i.putExtra(Constants.MANAGE_ACCOUNTS_MODE, true);
			startActivityForResult(i, REQUEST_CODE_MANAGE_ACCOUNTS);
			break;
		default:
			return false;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// NOTE startActivityForResult does not work when singleTask is set in
		// the manifiest
		// Therefore, FLAG_ACTIVITY_CLEAR_TOP is used on any intents instead.
		if (requestCode == REQUEST_CODE_MANAGE_ACCOUNTS) {
			if (resultCode == RESULT_OK) {
				// Went to manage accounts, didn't do anything to the current
				// account,
				// but might have added/removed other accounts
				updateAccountsList();
			} else if (resultCode == RESULT_CANCELED) {
				// The user removed the current account, remove it from
				// preferences and finish
				// so that the user has to choose an account when they next
				// start the app
				Preferences.removeAccountName(this);
				finish();
			}
		} else if (requestCode == REQUEST_AUTHENTICATE) {
			if (resultCode == RESULT_OK) {
				// user entered credentials, etc, try to get data again
				Utils.execute(new LoadRemoteEntries());
			} else {
				Toast.makeText(this, getString(R.string.auth_error, accountName), Toast.LENGTH_LONG)
						.show();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return adapter.getAppInfos();
	}

	@Override
	protected void onPause() {
		super.onPause();
		AndlyticsApp.getInstance().setIsAppVisible(false);
	}

	@Override
	protected void onDestroy() {
		statsModeMenuItem = null;
		super.onDestroy();
	}

	private void updateAccountsList() {
		final AccountManager manager = AccountManager.get(this);
		final Account[] accounts = manager.getAccountsByType(Constants.ACCOUNT_TYPE_GOOGLE);
		if (accounts.length > 1) {
			accountsList = new ArrayList<String>();
			int selectedIndex = 0;
			int index = 0;
			for (Account account : accounts) {
				if (!Preferences.getIsHiddenAccount(this, account.name)) {
					accountsList.add(account.name);
					if (account.name.equals(accountName)) {
						selectedIndex = index;
					}
					index++;
				}
			}
			if (accountsList.size() > 1) {
				// Only use the spinner if we have multiple accounts
				Context context = getSupportActionBar().getThemedContext();
				AccountSelectorAdaper accountsAdapter = new AccountSelectorAdaper(context,
						R.layout.account_selector_item, accountsList);
				accountsAdapter
						.setDropDownViewResource(com.actionbarsherlock.R.layout.sherlock_spinner_dropdown_item);

				// Hide the title to avoid duplicated info on tablets/landscape
				// & setup the spinner
				getSupportActionBar().setDisplayShowTitleEnabled(false);
				getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
				getSupportActionBar().setListNavigationCallbacks(accountsAdapter, this);
				getSupportActionBar().setSelectedNavigationItem(selectedIndex);
			} else {
				// Just one account so use the standard title/subtitle
				getSupportActionBar().setDisplayShowTitleEnabled(true);
				getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
				getSupportActionBar().setTitle(R.string.app_name);
				getSupportActionBar().setSubtitle(accountName);
			}
		} else {
			// Just one account so use the standard title/subtitle
			getSupportActionBar().setDisplayShowTitleEnabled(true);
			getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			getSupportActionBar().setTitle(R.string.app_name);
			getSupportActionBar().setSubtitle(accountName);
		}
	}

	private void updateMainList(List<AppInfo> apps) {

		if (apps != null) {

			if (apps.size() > 0) {
				footer.setVisibility(View.VISIBLE);

				String autosyncSet = Preferences.getAutosyncSet(Main.this, accountName);
				if (autosyncSet == null) {
					// Setup auto sync for the first time
					AutosyncHandler syncHandler = new AutosyncHandler();
					// Ensure it matches the sync period (excluding disabled state)
					syncHandler.setAutosyncPeriod(accountName,
							Preferences.getLastNonZeroAutosyncPeriod(Main.this));
					// Now make it match the master sync (including disabled state)
					syncHandler.setAutosyncPeriod(accountName,
							Preferences.getAutosyncPeriod(Main.this));
					Preferences.saveAutoSyncSet(Main.this, accountName);
				}
			}

			adapter.setAppInfos(apps);
			adapter.notifyDataSetChanged();

			Date lastUpdateDate = null;

			for (int i = 0; i < apps.size(); i++) {
				Date dateObject = apps.get(i).getLastUpdate();
				if (lastUpdateDate == null || lastUpdateDate.before(dateObject)) {
					lastUpdateDate = dateObject;
				}
			}

			if (lastUpdateDate != null) {
				// TODO Let the user configure this, or at least make it the
				// locale's default
				statusText.setText(this.getString(R.string.last_update) + ": "
						+ Preferences.getDateFormatLong(this).format(lastUpdateDate) + " "
						+ timeFormat.format(lastUpdateDate));
			}

		}

		if (!(R.id.main_app_list == mainViewSwitcher.getCurrentView().getId())) {
			mainViewSwitcher.showNext();
		}

	}

	// TODO Make this a static class and use a callback for UI updates, or move
	// to fragments with savedInstanceState
	private class LoadRemoteEntries extends AsyncTask<String, Integer, Exception> {

		@SuppressLint("NewApi")
		@SuppressWarnings("unchecked")
		@Override
		protected Exception doInBackground(String... params) {
			Exception exception = null;

			List<AppInfo> appDownloadInfos = null;
			try {
				DevConsoleV2 v2 = DevConsoleRegistry.getInstance().get(accountName);
				if (v2 == null) {
					// this is pre-configured with needed headers and keeps
					// track
					// of cookies, etc.
					DefaultHttpClient httpClient = HttpClientFactory
							.createDevConsoleHttpClient(DevConsoleV2.TIMEOUT);
					v2 = DevConsoleV2.createForAccount(accountName, httpClient);
					DevConsoleRegistry.getInstance().put(accountName, v2);
				}

				appDownloadInfos = v2.getAppInfo(Main.this);

				if (cancelRequested) {
					cancelRequested = false;
					return null;
				}

				Map<String, List<String>> admobAccountSiteMap = new HashMap<String, List<String>>();

				List<AppStatsDiff> diffs = new ArrayList<AppStatsDiff>();

				for (AppInfo appDownloadInfo : appDownloadInfos) {
					// update in database and check for diffs
					diffs.add(db.insertOrUpdateStats(appDownloadInfo));
					String admobSiteId = Preferences.getAdmobSiteId(Main.this,
							appDownloadInfo.getPackageName());
					if (admobSiteId != null) {
						String admobAccount = Preferences.getAdmobAccount(Main.this, admobSiteId);
						if (admobAccount != null) {
							List<String> siteList = admobAccountSiteMap.get(admobAccount);
							if (siteList == null) {
								siteList = new ArrayList<String>();
							}
							siteList.add(admobSiteId);
							admobAccountSiteMap.put(admobAccount, siteList);
						}
					}
				}

				// check for notifications
				NotificationHandler.handleNotificaions(Main.this, diffs, accountName);

				// sync admob accounts
				Set<String> admobAccuntKeySet = admobAccountSiteMap.keySet();
				for (String admobAccount : admobAccuntKeySet) {

					AdmobRequest.syncSiteStats(admobAccount, Main.this,
							admobAccountSiteMap.get(admobAccount), null);
				}

				Utils.execute(new LoadIconInCache(), appDownloadInfos);

			} catch (Exception e) {
				Log.e(TAG, "Error while requesting developer console : " + e.getMessage(), e);
				exception = e;
			}

			if (dotracking == true) {
				int size = 0;
				if (appDownloadInfos != null) {
					size = appDownloadInfos.size();
				}
				// TODO endless loop in case of exception!!!
				if (exception == null) {
					Map<String, String> map = new HashMap<String, String>();
					map.put("num", size + "");
				} else {
				}
				dotracking = false;
			}

			return exception;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Exception exception) {

			refreshing = false;
			supportInvalidateOptionsMenu();

			if (exception == null) {
				new LoadDbEntries().execute(false);
				return;
			}

			// TODO -- is this needed? DevConsoleV2 already retries
			// in the case of AuthenticationException
			if ((exception instanceof DevConsoleProtocolException || exception instanceof AuthenticationException)
					&& !isAuthenticationRetry) {
				Log.w("Andlytics", "authentication faild, retry with new token");
				isAuthenticationRetry = true;
				authenticateAccountFromPreferences(true, Main.this);

			} else {
				handleUserVisibleException(exception);
				new LoadDbEntries().execute(false);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			refreshing = true;
			supportInvalidateOptionsMenu();
		}

	}

	private class LoadDbEntries extends AsyncTask<Boolean, Void, Boolean> {

		private List<AppInfo> allStats = new ArrayList<AppInfo>();
		private List<AppInfo> filteredStats = new ArrayList<AppInfo>();

		private Boolean triggerRemoteCall;

		@Override
		protected Boolean doInBackground(Boolean... params) {

			allStats = db.getAllAppsLatestStats(accountName);

			for (AppInfo appInfo : allStats) {

				if (!appInfo.isGhost()) {
					String admobSiteId = Preferences.getAdmobSiteId(Main.this,
							appInfo.getPackageName());
					if (admobSiteId != null) {
						List<Admob> admobStats = db.getAdmobStats(admobSiteId,
								Timeframe.LAST_TWO_DAYS).getAdmobs();
						if (admobStats.size() > 0) {
							Admob admob = admobStats.get(admobStats.size() - 1);
							appInfo.setAdmobStats(admob);
						}
					}
					filteredStats.add(appInfo);
				}

			}

			triggerRemoteCall = params[0];

			return null;
		}

		@Override
		protected void onPostExecute(Boolean result) {

			updateMainList(filteredStats);

			if (triggerRemoteCall) {
				authenticateAccountFromPreferences(false, Main.this);

			} else {

				if (allStats.size() == 0) {
					Toast.makeText(Main.this, R.string.no_published_apps, Toast.LENGTH_LONG).show();
				}
			}

		}

	}

	private class LoadIconInCache extends AsyncTask<List<AppInfo>, Void, Boolean> {

		@Override
		protected Boolean doInBackground(List<AppInfo>... params) {

			List<AppInfo> appInfos = params[0];

			Boolean success = false;

			for (AppInfo appInfo : appInfos) {

				String iconUrl = appInfo.getIconUrl();

				if (iconUrl != null) {

					File iconFile = new File(getCacheDir() + "/" + appInfo.getIconName());
					if (!iconFile.exists()) {

						try {
							iconFile.createNewFile();
							URL url = new URL(iconUrl);
							HttpURLConnection c = (HttpURLConnection) url.openConnection();
							c.setRequestMethod("GET");
							// c.setDoOutput(true);
							c.connect();

							FileOutputStream fos = new FileOutputStream(iconFile);

							InputStream is = c.getInputStream();

							byte[] buffer = new byte[1024];
							int len1 = 0;
							while ((len1 = is.read(buffer)) != -1) {
								fos.write(buffer, 0, len1);
							}
							fos.close();
							is.close();

							success = true;
						} catch (IOException e) {

							if (iconFile.exists()) {
								iconFile.delete();
							}

							Log.d("log_tag", "Error: " + e);
						}

					}
				}

			}

			return success;

		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				adapter.notifyDataSetChanged();
			}
		}

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void updateStatsMode() {
		if (statsModeMenuItem != null) {
			switch (currentStatsMode) {
			case PERCENT:
				statsModeMenuItem.setTitle(R.string.daily);
				statsModeMenuItem.setIcon(R.drawable.icon_plusminus);
				break;

			case DAY_CHANGES:
				statsModeMenuItem.setTitle(R.string.percentage);
				statsModeMenuItem.setIcon(R.drawable.icon_percent);
				break;

			default:
				break;
			}
		}
		adapter.setStatsMode(currentStatsMode);
		adapter.notifyDataSetChanged();
		Preferences.saveStatsMode(currentStatsMode, Main.this);
	}

	@Override
	public void authenticationSuccess() {
		Utils.execute(new LoadRemoteEntries());
	}

	// FIXME isUpdate

	/**
	 * checks if the app is started for the first time (after an update).
	 * 
	 * @return <code>true</code> if this is the first start (after an update)
	 *         else <code>false</code>
	 */
	private boolean isUpdate() {
		// Get the versionCode of the Package, which must be different
		// (incremented) in each release on the market in the
		// AndroidManifest.xml
		final int versionCode = Utils.getActualVersionCode(this);

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final long lastVersionCode = prefs.getLong(LAST_VERSION_CODE_KEY, 0);

		if (versionCode != lastVersionCode) {
			Log.i(TAG, "versionCode " + versionCode + " is different from the last known version "
					+ lastVersionCode);
			return true;
		} else {
			Log.i(TAG, "versionCode " + versionCode + " is already known");
			return false;
		}
	}

	private void showChangelog() {
		final int versionCode = Utils.getActualVersionCode(this);
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		ChangelogBuilder.create(this, new Dialog.OnClickListener() {

			public void onClick(DialogInterface dialogInterface, int i) {
				// Mark this version as read
				sp.edit().putLong(LAST_VERSION_CODE_KEY, versionCode).commit();

				dialogInterface.dismiss();
			}
		}).show();
	}

	private static class AccountSelectorAdaper extends ArrayAdapter<String> {
		private Context context;
		private List<String> accounts;
		private int textViewResourceId;

		public AccountSelectorAdaper(Context context, int textViewResourceId, List<String> objects) {
			super(context, textViewResourceId, objects);
			this.context = context;
			this.accounts = objects;
			this.textViewResourceId = textViewResourceId;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = convertView;
			if (rowView == null) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(textViewResourceId, parent, false);
			}

			TextView subtitle = (TextView) rowView.findViewById(android.R.id.text1);
			subtitle.setText(accounts.get(position));
			Resources res = context.getResources();
			if (res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				// Scale the text down slightly to fit on landscape due to the
				// shorter Action Bar
				// and additional padding due to the drop down
				// We don't use predefined values as it saves duplicating all of
				// the different display
				// configuration values from the ABS library
				float px = subtitle.getTextSize() * 0.9f;
				subtitle.setTextSize(px / (res.getDisplayMetrics().densityDpi / 160f));

			}

			// TODO In the future when accounts linked to multiple developer
			// consoles are supported
			// we can either merge all the apps together, or extend this adapter
			// to let the user select
			// account/console E.g:
			// account1
			// account2/console1
			// account2/console2
			// ...

			return rowView;
		}

	}

}
