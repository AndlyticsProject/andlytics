
package com.github.andlyticsproject;

import java.io.IOException;
import java.text.SimpleDateFormat;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.admob.AdmobRequest;
import com.github.andlyticsproject.admob.AdmobRequest.SyncCallback;
import com.github.andlyticsproject.exception.NetworkException;
import com.github.andlyticsproject.model.Admob;
import com.github.andlyticsproject.model.AdmobList;
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
	private boolean refreshing;

	@Override
	protected void executeLoadData(Timeframe timeFrame) {
		new LoadDbEntiesTask().execute(new Object[] {
				false, timeFrame
		});

	}

	private void executeLoadDataDefault(boolean executeRemoteCall) {
		new LoadDbEntiesTask().execute(new Object[] {
				executeRemoteCall, getCurrentTimeFrame()
		});

	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		db = getDbAdapter();

		mainViewSwitcher = new ViewSwitcher3D(
				(ViewGroup) findViewById(R.id.base_chart_main_frame));
		mainViewSwitcher.setListener(this);

		admobListAdapter = new AdmobListAdapter(this);

		setAdapter(admobListAdapter);

		String currentAdmobAccount = null;
		String currentSiteId = Preferences.getAdmobSiteId(AdmobActivity.this,
				packageName);
		if (currentSiteId != null) {
			currentAdmobAccount = Preferences.getAdmobAccount(this,
					currentSiteId);
		}

		if (currentAdmobAccount == null) {
			mainViewSwitcher.swap();
			if (configSwitcher.getCurrentView().getId() != R.id.base_chart_config) {
				configSwitcher.showPrevious();
			}
			showAccountList();
		} else {
			executeLoadDataDefault(false);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		String currentAdmobAccount = null;
		String currentSiteId = Preferences.getAdmobSiteId(AdmobActivity.this,
				packageName);
		if (currentSiteId != null) {
			currentAdmobAccount = Preferences.getAdmobAccount(this,
					currentSiteId);
		}
		getSupportMenuInflater().inflate(R.menu.admob_menu, menu);
		if (refreshing) {
			menu.findItem(R.id.itemAdmobsmenuRefresh).setActionView(
					R.layout.action_bar_indeterminate_progress);
		}
		if (currentAdmobAccount == null) {
			// Hide the settings (but keep the spinner) if nothing is setup
			menu.findItem(R.id.itemAdmobsmenuSettings).setVisible(false);
			menu.findItem(R.id.itemAdmobsmenuRefresh).setVisible(refreshing);
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
			case R.id.itemAdmobsmenuRefresh:
				setChartIgnoreCallLayouts(true);
				new LoadRemoteEntiesTask().execute();
				return true;
			case R.id.itemAdmobsmenuSettings:
				setChartIgnoreCallLayouts(true);

				String admobSiteId = Preferences.getAdmobSiteId(AdmobActivity.this,
						packageName);

				if (admobSiteId == null) {

					View currentView = configSwitcher.getCurrentView();
					if (currentView.getId() != R.id.base_chart_config) {
						configSwitcher.showPrevious();
					}
					mainViewSwitcher.swap();
					showAccountList();
				} else {
					getListViewSwitcher().swap();
				}
				return true;
			default:
				return (super.onOptionsItemSelected(item));
		}
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
		for (int i = 0; i < size; i++)
		{
			names[i] = accounts[i].name;

			View inflate = getLayoutInflater().inflate(R.layout.admob_account_list_item, null);
			TextView accountName = (TextView) inflate
					.findViewById(R.id.admob_account_list_item_text);
			accountName.setText(accounts[i].name);
			inflate.setTag(accounts[i].name);
			inflate.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view)
				{

					String currentAdmobAccount = (String) view.getTag();

					configSwitcher.showNext();
					new LoadRemoteSiteListTask(currentAdmobAccount).execute();

				}
			});
			accountList.addView(inflate);
		}
	}

	private void addNewAdmobAccount() {

		AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
			public void run(AccountManagerFuture<Bundle> future)
			{
				try
				{
					Bundle bundle = future.getResult();
					bundle.keySet();
					Log.d(TAG, "account added: " + bundle);

					showAccountList();

				} catch (OperationCanceledException e)
				{
					Log.d(TAG, "addAccount was canceled");
				} catch (IOException e)
				{
					Log.d(TAG, "addAccount failed: " + e);
				} catch (AuthenticatorException e)
				{
					Log.d(TAG, "addAccount failed: " + e);
				}
				// gotAccount(false);
			}
		};

		AccountManager.get(AdmobActivity.this).addAccount(Constants.ACCOUNT_TYPE_ADMOB,
				Constants.AUTHTOKEN_TYPE_ADMOB,
				null, null /* options */, AdmobActivity.this, callback, null /* handler */);
	}

	private class LoadDbEntiesTask extends AsyncTask<Object, Void, Exception> {

		private List<Admob> admobStats;
		private Boolean executeRemoteCall = false;

		@Override
		protected Exception doInBackground(Object... params) {

			String currentSiteId = Preferences.getAdmobSiteId(AdmobActivity.this, packageName);
			AdmobList admobList = db.getAdmobStats(currentSiteId, (Timeframe) params[1]);
			admobStats = admobList.getAdmobs();
			admobListAdapter.setOverallStats(admobList.getOverallStats());
			executeRemoteCall = (Boolean) params[0];
			return null;
		}

		@Override
		protected void onPostExecute(Exception result) {

			loadChartData(admobStats);
			Collections.reverse(admobStats);

			admobListAdapter.setStats(admobStats);
			// admobListAdapter.setCurrentChart(currentChart);
			admobListAdapter.notifyDataSetChanged();

			if (executeRemoteCall) {
				new LoadRemoteEntiesTask().execute();
			}

			refreshing = false;
			invalidateOptionsMenu();

		}
	};

	private class LoadRemoteEntiesTask extends AsyncTask<Void, Void, Exception> {
		@Override
		protected void onPreExecute() {
			refreshing = true;
			invalidateOptionsMenu();
		}

		@Override
		protected Exception doInBackground(Void... lastValueDate) {
			String currentAdmobAccount = null;
			String currentSiteId = Preferences.getAdmobSiteId(AdmobActivity.this, packageName);
			if (currentSiteId != null) {
				currentAdmobAccount = Preferences
						.getAdmobAccount(AdmobActivity.this, currentSiteId);
			}

			try {

				List<String> siteList = new ArrayList<String>();
				siteList.add(currentSiteId);

				AdmobRequest.syncSiteStats(currentAdmobAccount, AdmobActivity.this, siteList,
						new SyncCallback() {

							@Override
							public void initialImportStarted()
							{
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
			Toast.makeText(AdmobActivity.this, "Initial AdMob import, this may take a while...",
					Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onPostExecute(Exception result) {

			if (result != null) {
				Log.e(TAG, "admob exception", result);
				handleUserVisibleException(result);
			}
			else {
				executeLoadDataDefault(false);
			}

			refreshing = false;
			invalidateOptionsMenu();
		}
	};

	private class LoadRemoteSiteListTask extends AsyncTask<Void, Void, Exception> {

		private Map<String, String> data;
		private String currentAdmobAccount;

		public LoadRemoteSiteListTask(String currentAdmobAccount) {
			this.currentAdmobAccount = currentAdmobAccount;
		}

		@Override
		protected void onPreExecute() {
			refreshing = true;
			invalidateOptionsMenu();
		}

		@Override
		protected Exception doInBackground(Void... params) {

			try {
				data = AdmobRequest.getSiteList(currentAdmobAccount, AdmobActivity.this);
			} catch (Exception e) {
				return e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Exception result) {

			if (result != null) {
				handleUserVisibleException(result);
			}
			else {

				if (data.size() > 0)
				{

					siteList.removeAllViews();

					Set<String> keySet = data.keySet();
					for (String siteId : keySet)
					{

						String siteName = data.get(siteId);

						// pull the id from the data
						View inflate = getLayoutInflater().inflate(
								R.layout.admob_account_list_item, null);
						TextView accountName = (TextView) inflate
								.findViewById(R.id.admob_account_list_item_text);
						accountName.setText(siteName);
						inflate.setTag(siteId);
						inflate.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View view)
							{

								Preferences.saveAdmobSiteId(AdmobActivity.this, packageName,
										(String) view.getTag());
								Preferences.saveAdmobAccount(AdmobActivity.this,
										(String) view.getTag(),
										currentAdmobAccount);
								mainViewSwitcher.swap();
								executeLoadDataDefault(true);
								invalidateOptionsMenu();
							}
						});
						siteList.addView(inflate);

					}
				}
			}

			refreshing = false;
			invalidateOptionsMenu();
		}
	};

	private void loadChartData(List<Admob> statsForApp) {
		/*
		 * if(radioLastThrity != null) { radioLastThrity.setEnabled(false);
		 * radioUnlimited.setEnabled(false); checkSmooth.setEnabled(false); }
		 */

		if (statsForApp != null && statsForApp.size() > 0)
		{
			updateCharts(statsForApp);

			SimpleDateFormat dateFormat = new SimpleDateFormat(
					Preferences.getDateFormatLong(AdmobActivity.this));
			if (statsForApp.size() > 0)
			{

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
	protected List<View> getExtraConfig() {
		LinearLayout ll = (LinearLayout) getLayoutInflater().inflate(R.layout.admob_extra_config,
				null);
		View removeButton = (View) ll.findViewById(R.id.admob_config3_remove_button);
		removeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Preferences.saveAdmobSiteId(AdmobActivity.this, packageName, null);
				showAccountList();
				if (configSwitcher.getCurrentView().getId() != R.id.base_chart_config)
				{
					configSwitcher.showPrevious();
				}
				mainViewSwitcher.swap();
				invalidateOptionsMenu();
			}
		});
		List<View> ret = new ArrayList<View>();
		ret.add(ll);
		return ret;
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
			public void onClick(View v)
			{
				addNewAdmobAccount();
			}
		});
		return ret;

	}
}
