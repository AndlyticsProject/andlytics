package com.github.andlyticsproject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
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
import com.github.andlyticsproject.about.AboutActivity;
import com.github.andlyticsproject.admob.AdmobRequest;
import com.github.andlyticsproject.adsense.AdSenseClient;
import com.github.andlyticsproject.console.v2.DevConsoleRegistry;
import com.github.andlyticsproject.console.v2.DevConsoleV2;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.io.StatsCsvReaderWriter;
import com.github.andlyticsproject.model.AdmobStats;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.DeveloperAccount;
import com.github.andlyticsproject.sync.NotificationHandler;
import com.github.andlyticsproject.util.ChangelogBuilder;
import com.github.andlyticsproject.util.DetachableAsyncTask;
import com.github.andlyticsproject.util.Utils;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main extends BaseActivity implements OnNavigationListener {

	/** Key for latest version code preference. */
	private static final String LAST_VERSION_CODE_KEY = "last_version_code";

	public static final String TAG = Main.class.getSimpleName();

	/** Dialog constant. **/
	public static final int DIALOG_ABOUT_ID = 1;

	private boolean cancelRequested;
	private ListView mainListView;
	private TextView statusText;
	private ViewSwitcher mainViewSwitcher;
	private MainListAdapter adapter;
	private View footer;

	public Animation aniPrevIn;
	private StatsMode currentStatsMode;
	private MenuItem statsModeMenuItem;

	private List<DeveloperAccount> developerAccounts;

	private DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);

	private static final int REQUEST_OPEN_DOCUMENT = 88;
	private static final int REQUEST_CODE_MANAGE_ACCOUNTS = 99;

	private static class State {
		// TODO replace with loaders
		LoadDbEntries loadDbEntries;
		LoadRemoteEntries loadRemoteEntries;
		LoadIconInCache loadIconInCache;
		List<AppInfo> lastAppList;

		void detachAll() {
			if (loadDbEntries != null) {
				loadDbEntries.detach();
			}

			if (loadRemoteEntries != null) {
				loadRemoteEntries.detach();
			}

			if (loadIconInCache != null) {
				loadIconInCache.detach();
			}
		}

		void attachAll(Main activity) {
			if (loadDbEntries != null) {
				loadDbEntries.attach(activity);
			}

			if (loadRemoteEntries != null) {
				loadRemoteEntries.attach(activity);
			}

			if (loadIconInCache != null) {
				loadIconInCache.attach(activity);
			}
		}

		void setLoadDbEntries(LoadDbEntries task) {
			if (loadDbEntries != null) {
				loadDbEntries.detach();
			}
			loadDbEntries = task;
		}

		void setLoadRemoteEntries(LoadRemoteEntries task) {
			if (loadRemoteEntries != null) {
				loadRemoteEntries.detach();
			}
			loadRemoteEntries = task;
		}

		void setLoadIconInCache(LoadIconInCache task) {
			if (loadIconInCache != null) {
				loadIconInCache.detach();
			}
			loadIconInCache = task;
		}
	}

	private State state = new State();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		LayoutInflater layoutInflater = getLayoutInflater();

		// BaseActivity has already selected the account
		updateAccountsList();

		// setup main list
		mainListView = (ListView) findViewById(R.id.main_app_list);
		mainListView.addHeaderView(layoutInflater.inflate(R.layout.main_list_header, null), null,
				false);
		footer = layoutInflater.inflate(R.layout.main_list_footer, null);
		footer.setVisibility(View.INVISIBLE);
		mainListView.addFooterView(footer, null, false);
		adapter = new MainListAdapter(this, accountName, currentStatsMode);
		mainListView.setAdapter(adapter);
		mainViewSwitcher = (ViewSwitcher) findViewById(R.id.main_viewswitcher);

		// status & progress bar
		statusText = (TextView) findViewById(R.id.main_app_status_line);
		aniPrevIn = AnimationUtils.loadAnimation(Main.this, R.anim.activity_fade_in);

		currentStatsMode = Preferences.getStatsMode(this);
		updateStatsMode();

		State lastState = (State) getLastCustomNonConfigurationInstance();
		if (lastState != null) {
			state = lastState;
			state.attachAll(this);
			if (state.lastAppList != null) {
				adapter.setAppInfos(state.lastAppList);
				setSkipMainReload(true);
			}
		}

		// show changelog
		if (isUpdate()) {
			showChangelog();
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (!developerAccounts.get(itemPosition).getName().equals(accountName)) {
			// Only switch if it is a new account
			Intent intent = new Intent(Main.this, Main.class);
			intent.putExtra(BaseActivity.EXTRA_AUTH_ACCOUNT_NAME,
					developerAccounts.get(itemPosition).getName());
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

		if (!isSkipMainReload() && shouldRemoteUpdateStats()) {
			loadLocalEntriesAndUpdate();
		} else {
			loadLocalEntriesOnly();
		}

		setSkipMainReload(false);

		AndlyticsApp.getInstance().setIsAppVisible(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		getSupportMenuInflater().inflate(R.menu.main_menu, menu);
		statsModeMenuItem = menu.findItem(R.id.itemMainmenuStatsMode);
		if (isRefreshing())
			menu.findItem(R.id.itemMainmenuRefresh).setActionView(
					R.layout.action_bar_indeterminate_progress);
		updateStatsMode();
		return true;
	}

	/**
	 * Called if item in option menu is selected.
	 * 
	 * @param item
	 * The chosen menu item
	 * @return boolean true/false
	 */
	@TargetApi(19)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemMainmenuRefresh:
			loadRemoteEntries();
			break;
		case R.id.itemMainmenuImport:
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				Intent openIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				openIntent.setType("*/*");
				openIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] { "application/zip" });
				//hidden
				openIntent.putExtra("android.content.extra.SHOW_ADVANCED", true);
				startActivityForResult(openIntent, REQUEST_OPEN_DOCUMENT);
			} else {
				File fileToImport = StatsCsvReaderWriter.getExportFileForAccount(accountName);
				if (!fileToImport.exists()) {
					Toast.makeText(
							this,
							getString(R.string.import_no_stats_file, fileToImport.getAbsolutePath()),
							Toast.LENGTH_LONG).show();
					return true;
				}

				Intent importIntent = new Intent(this, ImportActivity.class);
				importIntent.setAction(Intent.ACTION_VIEW);
				importIntent.setData(Uri.fromFile(fileToImport));
				startActivity(importIntent);
			}
			break;
		case R.id.itemMainmenuExport:
			Intent exportIntent = new Intent(this, ExportActivity.class);
			exportIntent.putExtra(ExportActivity.EXTRA_ACCOUNT_NAME, accountName);
			startActivity(exportIntent);
			break;
		case R.id.itemMainmenuAbout:
			// launch about activity				
			Intent aboutIntent = new Intent(this, AboutActivity.class);
			startActivity(aboutIntent);
			break;
		case R.id.itemMainmenuPreferences:
			Intent preferencesIntent = new Intent(this, PreferenceActivity.class);
			preferencesIntent.putExtra(BaseActivity.EXTRA_AUTH_ACCOUNT_NAME, accountName);
			startActivity(preferencesIntent);
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
			Intent accountsIntent = new Intent(this, LoginActivity.class);
			accountsIntent.putExtra(LoginActivity.EXTRA_MANAGE_ACCOUNTS_MODE, true);
			startActivityForResult(accountsIntent, REQUEST_CODE_MANAGE_ACCOUNTS);
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
				developerAccountManager.unselectDeveloperAccount();
				finish();
			}
		} else if (requestCode == REQUEST_AUTHENTICATE) {
			if (resultCode == RESULT_OK) {
				// user entered credentials, etc, try to get data again
				loadRemoteEntries();
			} else {
				Toast.makeText(this, getString(R.string.auth_error, accountName), Toast.LENGTH_LONG)
						.show();
			}
		} else if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
			if (resultCode == Activity.RESULT_OK) {
			} else {
				checkGooglePlayServicesAvailable();
			}
		} else if (requestCode == REQUEST_AUTHORIZATION) {
			if (resultCode == Activity.RESULT_OK) {
				loadRemoteEntries();
			} else {
				Toast.makeText(this, getString(R.string.account_authorization_denied, accountName),
						Toast.LENGTH_LONG).show();
			}
		} else if (requestCode == REQUEST_OPEN_DOCUMENT) {
			if (resultCode == RESULT_OK) {
				Intent importIntent = new Intent(this, ImportActivity.class);
				importIntent.setAction(Intent.ACTION_VIEW);
				Uri uri = data.getData();
				importIntent.setData(data.getData());
				startActivity(importIntent);
			} else {
				Toast.makeText(
						this,
						getString(R.string.import_no_stats_file, data == null ? "" : data.getData()),
						Toast.LENGTH_LONG).show();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		state.lastAppList = adapter.getAppInfos();
		state.detachAll();

		return state;
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
		developerAccounts = developerAccountManager.getActiveDeveloperAccounts();
		if (developerAccounts.size() > 1) {
			int selectedIndex = 0;
			int index = 0;
			for (DeveloperAccount account : developerAccounts) {
				if (account.getName().equals(accountName)) {
					selectedIndex = index;
				}
				index++;
			}
			if (developerAccounts.size() > 1) {
				// Only use the spinner if we have multiple accounts
				Context context = getSupportActionBar().getThemedContext();
				AccountSelectorAdaper accountsAdapter = new AccountSelectorAdaper(context,
						R.layout.account_selector_item, developerAccounts);
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
				statusText.setText(Preferences.getDateFormatLong(this).format(lastUpdateDate) + " "
						+ timeFormat.format(lastUpdateDate));
			}
		}

		if (!(R.id.main_app_list == mainViewSwitcher.getCurrentView().getId())) {
			mainViewSwitcher.showNext();
		}

	}

	private static class LoadRemoteEntries extends
			DetachableAsyncTask<String, Integer, Exception, Main> {

		private ContentAdapter db;

		public LoadRemoteEntries(Main activity) {
			super(activity);
			db = ContentAdapter.getInstance(activity.getApplication());
		}

		@Override
		protected void onPreExecute() {
			if (activity == null) {
				return;
			}

			activity.refreshStarted();
		}

		@SuppressLint("NewApi")
		@SuppressWarnings("unchecked")
		@Override
		protected Exception doInBackground(String... params) {
			if (activity == null) {
				return null;
			}

			Exception exception = null;

			List<AppInfo> appDownloadInfos = null;
			try {
				DevConsoleV2 v2 = DevConsoleRegistry.getInstance().get(activity.accountName);

				appDownloadInfos = v2.getAppInfo(activity);

				if (activity.cancelRequested) {
					activity.cancelRequested = false;
					return null;
				}

				boolean migratedToAdSense = false;
				Map<String, List<String>> admobAccountSiteMap = new HashMap<String, List<String>>();

				List<AppStatsDiff> diffs = new ArrayList<AppStatsDiff>();

				for (AppInfo appDownloadInfo : appDownloadInfos) {
					// update in database and check for diffs
					// sets DB ID of ApInfo
					diffs.add(db.insertOrUpdateStats(appDownloadInfo));
					String[] admobDetails = AndlyticsDb.getInstance(activity).getAdmobDetails(
							appDownloadInfo.getPackageName());
					if (admobDetails != null) {
						String admobAccount = admobDetails[0];
						String admobSiteId = admobDetails[1];
						String adUnitId = admobDetails[2];
						if (admobAccount != null && adUnitId == null) {
							List<String> siteList = admobAccountSiteMap.get(admobAccount);
							if (siteList == null) {
								siteList = new ArrayList<String>();
							}
							siteList.add(admobSiteId);
							admobAccountSiteMap.put(admobAccount, siteList);
						} else {
							migratedToAdSense = true;
							List<String> siteList = admobAccountSiteMap.get(admobAccount);
							if (siteList == null) {
								siteList = new ArrayList<String>();
							}
							siteList.add(adUnitId);
							admobAccountSiteMap.put(admobAccount, siteList);
						}
					}
					// update app details
					AndlyticsDb.getInstance(activity).insertOrUpdateAppDetails(appDownloadInfo);
				}

				// check for notifications
				NotificationHandler.handleNotificaions(activity, diffs, activity.accountName);

				// sync admob accounts
				Set<String> admobAccuntKeySet = admobAccountSiteMap.keySet();
				for (String admobAccount : admobAccuntKeySet) {
					if (migratedToAdSense) {
						AdSenseClient.foregroundSyncStats(activity, admobAccount,
								admobAccountSiteMap.get(admobAccount));
					} else {
						AdmobRequest.syncSiteStats(admobAccount, activity,
								admobAccountSiteMap.get(admobAccount), null);
					}
				}

				activity.state.setLoadIconInCache(new LoadIconInCache(activity));
				Utils.execute(activity.state.loadIconInCache, appDownloadInfos);

			} catch (UserRecoverableAuthIOException userRecoverableException) {
				activity.startActivityForResult(userRecoverableException.getIntent(),
						REQUEST_AUTHORIZATION);
			} catch (Exception e) {
				// These exceptions can contain very long JSON strings
				// Explicitly print out the root cause first
				Log.e(TAG,
						"Error while requesting developer console : " + Utils.stackTraceToString(e));
				Log.e(TAG, "Error while requesting developer console : " + e.getMessage(), e);
				exception = e;
			}

			return exception;
		}

		@Override
		protected void onPostExecute(Exception exception) {
			if (activity == null) {
				return;
			}

			activity.refreshFinished();

			if (exception == null) {
				activity.developerAccountManager.saveLastStatsRemoteUpdateTime(
						activity.accountName, System.currentTimeMillis());
				activity.loadLocalEntriesOnly();
				return;
			}

			activity.handleUserVisibleException(exception);
			activity.loadLocalEntriesOnly();
		}

	}

	private void loadLocalEntriesOnly() {
		loadDbEntries(false);
	}

	private void loadLocalEntriesAndUpdate() {
		loadDbEntries(true);
	}

	private void loadDbEntries(boolean triggerRemoteCall) {
		state.setLoadDbEntries(new LoadDbEntries(this));
		Utils.execute(state.loadDbEntries, triggerRemoteCall);
	}

	private static class LoadDbEntries extends DetachableAsyncTask<Boolean, Void, Boolean, Main> {

		private ContentAdapter db;

		LoadDbEntries(Main activity) {
			super(activity);
			db = ContentAdapter.getInstance(activity.getApplication());
		}

		private List<AppInfo> allStats = new ArrayList<AppInfo>();
		private List<AppInfo> filteredStats = new ArrayList<AppInfo>();

		private Boolean triggerRemoteCall;

		@Override
		protected Boolean doInBackground(Boolean... params) {
			if (activity == null) {
				return null;
			}

			allStats = db.getAllAppsLatestStats(activity.accountName);

			for (AppInfo appInfo : allStats) {
				if (!appInfo.isGhost()) {
					if (appInfo.getAdmobSiteId() != null || appInfo.getAdmobAdUnitId() != null) {
						List<AdmobStats> admobStats = db.getAdmobStats(appInfo.getAdmobSiteId(),
								appInfo.getAdmobAdUnitId(), Timeframe.LAST_TWO_DAYS).getStats();
						if (admobStats.size() > 0) {
							AdmobStats admob = admobStats.get(admobStats.size() - 1);
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
			if (activity == null) {
				return;
			}

			activity.updateMainList(filteredStats);

			if (triggerRemoteCall) {
				activity.loadRemoteEntries();
			} else {
				if (allStats.isEmpty()) {
					Toast.makeText(activity, R.string.no_published_apps, Toast.LENGTH_LONG).show();
				}
			}
		}

	}

	private static class LoadIconInCache extends
			DetachableAsyncTask<List<AppInfo>, Void, Boolean, Main> {

		LoadIconInCache(Main activity) {
			super(activity);
		}

		@Override
		protected Boolean doInBackground(List<AppInfo>... params) {
			if (activity == null) {
				return null;
			}

			List<AppInfo> appInfos = params[0];
			Boolean success = Boolean.FALSE;

			for (AppInfo appInfo : appInfos) {
				String iconUrl = appInfo.getIconUrl();

				if (iconUrl != null) {
					File iconFile = new File(activity.getCacheDir(), appInfo.getIconName());
					if (!iconFile.exists()) {
						try {
							if (iconFile.createNewFile()) {
								Utils.getAndSaveToFile(new URL(iconUrl), iconFile);

								success = Boolean.TRUE;
							}
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
			if (activity == null) {
				return;
			}

			if (success) {
				activity.adapter.notifyDataSetChanged();
			}
		}

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

	private void loadRemoteEntries() {
		state.setLoadRemoteEntries(new LoadRemoteEntries(this));
		Utils.execute(state.loadRemoteEntries);
	}

	/**
	 * checks if the app is started for the first time (after an update).
	 * 
	 * @return <code>true</code> if this is the first start (after an update)
	 * else <code>false</code>
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

	private static class AccountSelectorAdaper extends ArrayAdapter<DeveloperAccount> {
		private Context context;
		private List<DeveloperAccount> accounts;
		private int textViewResourceId;

		public AccountSelectorAdaper(Context context, int textViewResourceId,
				List<DeveloperAccount> objects) {
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
			subtitle.setText(accounts.get(position).getName());
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

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			View result = super.getDropDownView(position, convertView, parent);
			((TextView) result).setText(accounts.get(position).getName());

			return result;
		}

	}

}
