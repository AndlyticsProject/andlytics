package com.github.andlyticsproject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.admob.AdmobRequest;
import com.github.andlyticsproject.admob.AdmobRequest.SyncCallback;
import com.github.andlyticsproject.console.NetworkException;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.model.Admob;
import com.github.andlyticsproject.model.AdmobList;
import com.github.andlyticsproject.util.DetachableAsyncTask;
import com.github.andlyticsproject.util.Utils;
import com.github.andlyticsproject.view.ViewSwitcher3D;

public class AdmobActivity extends BaseChartActivity {

	public static final String TAG = AdmobActivity.class.getSimpleName();

	protected ContentAdapter db;
	private AdmobListAdapter admobListAdapter;
	public Integer heighestRatingChange;
	public Integer lowestRatingChange;
	private ViewSwitcher3D mainViewSwitcher;
	private ViewGroup accountList;

	private View addAccountButton;

	private ViewSwitcher configSwitcher;

	protected String admobToken;

	private ViewGroup siteList;

	private static class State {
		LoadDbEntriesTask loadDbEntries;
		LoadRemoteEntriesTask loadRemoteEntries;
		LoadRemoteSiteListTask loadRemoteSiteList;

		State detachAll() {
			if (loadDbEntries != null) {
				loadDbEntries.detach();
			}

			if (loadRemoteEntries != null) {
				loadRemoteEntries.detach();
			}

			if (loadRemoteSiteList != null) {
				loadRemoteSiteList.detach();
			}

			return this;
		}

		void attachAll(AdmobActivity activity) {
			if (loadDbEntries != null) {
				loadDbEntries.attach(activity);
			}

			if (loadRemoteEntries != null) {
				loadRemoteEntries.attach(activity);
			}

			if (loadRemoteSiteList != null) {
				loadRemoteSiteList.attach(activity);
			}
		}

		void setLoadDbEntries(LoadDbEntriesTask task) {
			if (loadDbEntries != null) {
				loadDbEntries.detach();
			}
			loadDbEntries = task;
		}

		void setLoadRemoteEntries(LoadRemoteEntriesTask task) {
			if (loadRemoteEntries != null) {
				loadRemoteEntries.detach();
			}
			loadRemoteEntries = task;
		}

		void setLoadRemoteSiteList(LoadRemoteSiteListTask task) {
			if (loadRemoteSiteList != null) {
				loadRemoteSiteList.detach();
			}
			loadRemoteSiteList = task;
		}
	}

	private State state = new State();

	@Override
	protected void executeLoadData(Timeframe timeFrame) {
		state.setLoadDbEntries(new LoadDbEntriesTask(this));
		Utils.execute(state.loadDbEntries, new Object[] { false, timeFrame });

	}

	private void executeLoadDataDefault(boolean executeRemoteCall) {
		state.setLoadDbEntries(new LoadDbEntriesTask(this));
		Utils.execute(state.loadDbEntries,
				new Object[] { executeRemoteCall, getCurrentTimeFrame() });

	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		db = getDbAdapter();

		mainViewSwitcher = new ViewSwitcher3D((ViewGroup) findViewById(R.id.base_chart_main_frame));
		mainViewSwitcher.setListener(this);

		admobListAdapter = new AdmobListAdapter(this);

		setAdapter(admobListAdapter);

		String[] admobDetails = AndlyticsDb.getInstance(this).getAdmobDetails(packageName);
		if (admobDetails == null) {
			mainViewSwitcher.swap();
			if (configSwitcher.getCurrentView().getId() != R.id.base_chart_config) {
				configSwitcher.showPrevious();
			}
			showAccountList();
		} else {
			if (getLastNonConfigurationInstance() != null) {
				state = (State) getLastNonConfigurationInstance();
				state.attachAll(this);
				if (state.loadDbEntries.admobStats != null) {
					showStats(state.loadDbEntries.admobStats);
				}
			} else {
				executeLoadDataDefault(false);
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return state.detachAll();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		getSupportMenuInflater().inflate(R.menu.admob_menu, menu);
		super.onCreateOptionsMenu(menu);
		String[] admobDetails = AndlyticsDb.getInstance(this).getAdmobDetails(packageName);

		if (isRefreshing()) {
			menu.findItem(R.id.itemChartsmenuRefresh).setActionView(
					R.layout.action_bar_indeterminate_progress);
		}
		if (admobDetails == null) {
			menu.findItem(R.id.itemAdmobsmenuRemove).setVisible(false);
			menu.findItem(R.id.itemChartsmenuTimeframe).setVisible(false);
			menu.findItem(R.id.itemChartsmenuRefresh).setVisible(isRefreshing());
		}
		return true;
	}

	/**
	 * Called if item in option menu is selected.
	 * 
	 * @param item The chosen menu item
	 * @return boolean true/false
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemChartsmenuRefresh:
			setChartIgnoreCallLayouts(true);
			loadRemoteEntries();
			return true;
		case R.id.itemAdmobsmenuRemove:
			AndlyticsDb.getInstance(this).saveAdmobDetails(TAG, null, null);
			showAccountList();
			if (configSwitcher.getCurrentView().getId() != R.id.base_chart_config) {
				configSwitcher.showPrevious();
			}
			mainViewSwitcher.swap();
			supportInvalidateOptionsMenu();
			return true;
		default:
			return (super.onOptionsItemSelected(item));
		}
	}

	private void loadRemoteEntries() {
		state.setLoadRemoteEntries(new LoadRemoteEntriesTask(this));
		Utils.execute(state.loadRemoteEntries);
	}

	@Override
	protected String getChartHint() {
		return "8 " + this.getString(R.string.admob__charts_available) + " ->";
	}

	protected void showAccountList() {

		final AccountManager manager = AccountManager.get(this);
		final Account[] accounts = manager.getAccountsByType(Constants.ACCOUNT_TYPE_ADMOB);
		final int size = accounts.length;
		String[] names = new String[size];
		accountList.removeAllViews();
		for (int i = 0; i < size; i++) {
			names[i] = accounts[i].name;

			View inflate = getLayoutInflater().inflate(R.layout.admob_account_list_item, null);
			TextView accountName = (TextView) inflate
					.findViewById(R.id.admob_account_list_item_text);
			accountName.setText(accounts[i].name);
			inflate.setTag(accounts[i].name);
			inflate.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {

					String currentAdmobAccount = (String) view.getTag();

					configSwitcher.showNext();
					loadRemoteSiteList(currentAdmobAccount);

				}
			});
			accountList.addView(inflate);
		}
	}

	private void loadRemoteSiteList(String currentAdmobAccount) {
		state.setLoadRemoteSiteList(new LoadRemoteSiteListTask(this, currentAdmobAccount));
		Utils.execute(state.loadRemoteSiteList);
	}

	private void addNewAdmobAccount() {

		AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
			public void run(AccountManagerFuture<Bundle> future) {
				try {
					Bundle bundle = future.getResult();
					bundle.keySet();
					Log.d(TAG, "account added: " + bundle);

					showAccountList();

				} catch (OperationCanceledException e) {
					Log.d(TAG, "addAccount was canceled");
				} catch (IOException e) {
					Log.d(TAG, "addAccount failed: " + e);
				} catch (AuthenticatorException e) {
					Log.d(TAG, "addAccount failed: " + e);
				}
				// gotAccount(false);
			}
		};

		AccountManager.get(AdmobActivity.this).addAccount(Constants.ACCOUNT_TYPE_ADMOB,
				Constants.AUTHTOKEN_TYPE_ADMOB, null, null /* options */, AdmobActivity.this,
				callback, null /* handler */);
	}

	private static class LoadDbEntriesTask extends
			DetachableAsyncTask<Object, Void, Exception, AdmobActivity> {

		private List<Admob> admobStats;
		private Boolean executeRemoteCall = false;

		LoadDbEntriesTask(AdmobActivity activity) {
			super(activity);
		}

		@Override
		protected void onPreExecute() {
			if (activity == null) {
				return;
			}

			activity.refreshStarted();
		}

		@Override
		protected Exception doInBackground(Object... params) {
			if (activity == null) {
				return null;
			}

			String[] admobDetails = AndlyticsDb.getInstance(activity).getAdmobDetails(
					activity.packageName);
			if (admobDetails == null) {
				Log.w(TAG, "Admob account and site ID not founf for " + activity.packageName);
				return null;
			}

			String currentSiteId = admobDetails[1];
			AdmobList admobList = activity.db.getAdmobStats(currentSiteId, (Timeframe) params[1]);
			admobStats = admobList.getAdmobs();
			activity.admobListAdapter.setOverallStats(admobList.getOverallStats());
			executeRemoteCall = (Boolean) params[0];

			return null;
		}

		@Override
		protected void onPostExecute(Exception error) {
			if (activity == null) {
				return;
			}

			activity.refreshFinished();

			if (error == null && admobStats == null) {
				return;
			}

			if (error != null) {
				activity.handleUserVisibleException(error);
				return;
			}

			activity.showStats(admobStats);

			if (executeRemoteCall) {
				new LoadRemoteEntriesTask(activity).execute();
			}
		}
	};

	private void showStats(List<Admob> admobStats) {
		loadChartData(admobStats);
		// make shallow copy
		List<Admob> reversedAdmobStats = new ArrayList<Admob>();
		reversedAdmobStats.addAll(admobStats);
		Collections.reverse(reversedAdmobStats);

		admobListAdapter.setStats(reversedAdmobStats);
		// admobListAdapter.setCurrentChart(currentChart);
		admobListAdapter.notifyDataSetChanged();
	}

	private static class LoadRemoteEntriesTask extends
			DetachableAsyncTask<Void, Void, Exception, AdmobActivity> {

		LoadRemoteEntriesTask(AdmobActivity activity) {
			super(activity);
		}

		@Override
		protected void onPreExecute() {
			if (activity == null) {
				return;
			}

			activity.refreshStarted();
		}

		@Override
		protected Exception doInBackground(Void... lastValueDate) {
			if (activity == null) {
				return null;
			}

			String[] admobDetails = AndlyticsDb.getInstance(activity).getAdmobDetails(
					activity.packageName);
			if (admobDetails == null) {
				Log.w(TAG, "Admob account and site ID not founf for " + activity.packageName);
				return null;
			}

			String currentAdmobAccount = admobDetails[0];
			String currentSiteId = admobDetails[1];
			try {
				List<String> siteList = new ArrayList<String>();
				siteList.add(currentSiteId);

				AdmobRequest.syncSiteStats(currentAdmobAccount, activity, siteList,
						new SyncCallback() {

							@Override
							public void initialImportStarted() {
								publishProgress();
							}
						});

			} catch (Exception e) {

				if (e instanceof IOException) {
					e = new NetworkException(e);
				}

				return e;
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			if (activity == null) {
				return;
			}
			Toast.makeText(activity, activity.getString(R.string.admob_initial_import),
					Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onPostExecute(Exception error) {
			if (activity == null) {
				return;
			}

			if (error != null) {
				Log.e(TAG, "admob exception", error);
				activity.handleUserVisibleException(error);
			} else {
				activity.executeLoadDataDefault(false);
			}

			activity.refreshFinished();
		}
	};

	private static class LoadRemoteSiteListTask extends
			DetachableAsyncTask<Void, Void, Exception, AdmobActivity> {

		private Map<String, String> data;
		private String currentAdmobAccount;

		public LoadRemoteSiteListTask(AdmobActivity activity, String currentAdmobAccount) {
			super(activity);
			this.currentAdmobAccount = currentAdmobAccount;
		}

		@Override
		protected void onPreExecute() {
			if (activity == null) {
				return;
			}

			activity.refreshStarted();
		}

		@Override
		protected Exception doInBackground(Void... params) {
			if (activity == null) {
				return null;
			}

			try {
				data = AdmobRequest.getSiteList(currentAdmobAccount, activity);
			} catch (Exception e) {
				return e;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Exception error) {
			if (activity == null) {
				return;
			}

			activity.refreshFinished();

			if (error != null) {
				activity.handleUserVisibleException(error);
				return;
			}

			if (data != null && data.size() > 0) {
				activity.siteList.removeAllViews();

				Set<String> keySet = data.keySet();
				for (String siteId : keySet) {

					String siteName = data.get(siteId);

					// pull the id from the data
					View inflate = activity.getLayoutInflater().inflate(
							R.layout.admob_account_list_item, null);
					TextView accountName = (TextView) inflate
							.findViewById(R.id.admob_account_list_item_text);
					accountName.setText(siteName);
					inflate.setTag(siteId);
					inflate.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View view) {
							String admobSiteId = (String) view.getTag();
							AndlyticsDb.getInstance(activity).saveAdmobDetails(
									activity.packageName, currentAdmobAccount, admobSiteId);

							activity.mainViewSwitcher.swap();
							activity.executeLoadDataDefault(true);
							activity.supportInvalidateOptionsMenu();
						}
					});
					activity.siteList.addView(inflate);

				}
			}
		}
	};

	private void loadChartData(List<Admob> statsForApp) {
		/*
		 * if(radioLastThrity != null) { radioLastThrity.setEnabled(false);
		 * radioUnlimited.setEnabled(false); checkSmooth.setEnabled(false); }
		 */

		if (statsForApp != null && statsForApp.size() > 0) {
			updateCharts(statsForApp);

			DateFormat dateFormat = Preferences.getDateFormatLong(AdmobActivity.this);
			if (statsForApp.size() > 0) {

				timetext = dateFormat.format(statsForApp.get(0).getDate()) + " - "
						+ dateFormat.format(statsForApp.get(statsForApp.size() - 1).getDate());
				updateChartHeadline();
			}

			// chartFrame.showNext();

		}
		/*
		 * if(radioLastThrity != null) { radioLastThrity.setEnabled(true);
		 * radioUnlimited.setEnabled(true); checkSmooth.setEnabled(true); }
		 */

	}

	@Override
	protected void notifyChangedDataformat() {
		executeLoadDataDefault(false);
	}

	@Override
	protected List<View> getExtraFullViews() {
		configSwitcher = (ViewSwitcher) findViewById(R.id.base_chart_viewswitcher_config);
		configSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
		configSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_left));
		List<View> ret = new ArrayList<View>();
		RelativeLayout ll;

		ll = (RelativeLayout) getLayoutInflater().inflate(R.layout.admob_config_selectapp, null);
		siteList = (ViewGroup) ll.findViewById(R.id.admob_sitelist);
		ret.add(ll);

		ll = (RelativeLayout) getLayoutInflater().inflate(R.layout.admob_config_addaccount, null);
		accountList = (ViewGroup) ll.findViewById(R.id.admob_accountlist);
		addAccountButton = (View) ll.findViewById(R.id.admob_addaccount_button);
		ret.add(ll);

		addAccountButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				addNewAdmobAccount();
			}
		});
		return ret;

	}
}
