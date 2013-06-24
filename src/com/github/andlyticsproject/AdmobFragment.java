package com.github.andlyticsproject;


import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.model.Admob;
import com.github.andlyticsproject.model.AdmobList;
import com.github.andlyticsproject.util.LoaderBase;
import com.github.andlyticsproject.util.LoaderResult;
import com.github.andlyticsproject.view.ViewSwitcher3D;

public class AdmobFragment extends ChartFragment implements
		LoaderManager.LoaderCallbacks<LoaderResult<AdmobList>> {

	private static final String TAG = AdmobActivity.class.getSimpleName();

	private AdmobListAdapter admobListAdapter;
	public Integer heighestRatingChange;
	public Integer lowestRatingChange;
	private ViewSwitcher3D mainViewSwitcher;
	private ViewGroup accountList;

	private View addAccountButton;

	private ViewSwitcher configSwitcher;

	protected String admobToken;

	private ViewGroup siteList;

	static class AdmobDbLoader extends LoaderBase<AdmobList> {

		private ContentAdapter db;
		private String packageName;
		private Timeframe timeframe;

		public AdmobDbLoader(Context context, String packageName, Timeframe timeframe) {
			super(context);
			db = ContentAdapter.getInstance(AndlyticsApp.getInstance());
			this.packageName = packageName;
			this.timeframe = timeframe;
		}

		@Override
		protected AdmobList load() throws Exception {
			if (packageName == null || timeframe == null) {
				return null;
			}

			String[] admobDetails = AndlyticsDb.getInstance(getContext()).getAdmobDetails(
					packageName);
			if (admobDetails == null) {
				Log.w(TAG, "Admob account and site ID not founf for " + packageName);
				return null;
			}

			String currentSiteId = admobDetails[1];

			return db.getAdmobStats(currentSiteId, timeframe);
		}

		@Override
		protected void releaseResult(LoaderResult<AdmobList> result) {
			// just a string, nothing to do
		}

		@Override
		protected boolean isActive(LoaderResult<AdmobList> result) {
			return false;
		}
	}

	public AdmobFragment() {
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// just init don't try to load
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		mainViewSwitcher = new ViewSwitcher3D(
				(ViewGroup) view.findViewById(R.id.base_chart_main_frame));
		mainViewSwitcher.setListener(this);

		admobListAdapter = new AdmobListAdapter(getActivity());

		setAdapter(admobListAdapter);

		String[] admobDetails = AndlyticsDb.getInstance(getActivity()).getAdmobDetails(
				statsActivity.getPackageName());
		if (admobDetails == null) {
			mainViewSwitcher.swap();
			if (configSwitcher.getCurrentView().getId() != R.id.base_chart_config) {
				configSwitcher.showPrevious();
			}
			showAccountList();
		} else {
			// XXX
			//			if (getLastCustomNonConfigurationInstance() != null) {
			//				state = (State) getLastCustomNonConfigurationInstance();
			//				state.attachAll(this);
			//				if (state.loadDbEntries.admobList != null) {
			//					showStats(state.loadDbEntries.admobList);
			//				}
			//			} else {
			//				executeLoadDataDefault(false);
			//			}
		}

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		Bundle args = new Bundle();
		args.putString("packageName", statsActivity.getPackageName());
		// XXX
		args.putSerializable("timeframe", Timeframe.MONTH_TO_DATE);
		statsActivity.refreshStarted();

		getLoaderManager().restartLoader(0, args, this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		// The CursorLoader example doesn't do this, but if we get an update
		// while the UI is
		// destroyed, it will crash. Why is this necessary?
		getLoaderManager().destroyLoader(0);
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.admob_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
		String[] admobDetails = AndlyticsDb.getInstance(getActivity()).getAdmobDetails(
				statsActivity.getPackageName());

		if (statsActivity.isRefreshing()) {
			menu.findItem(R.id.itemChartsmenuRefresh).setActionView(
					R.layout.action_bar_indeterminate_progress);
		}
		if (admobDetails == null) {
			menu.findItem(R.id.itemAdmobsmenuRemove).setVisible(false);
			menu.findItem(R.id.itemChartsmenuTimeframe).setVisible(false);
			menu.findItem(R.id.itemChartsmenuRefresh).setVisible(statsActivity.isRefreshing());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemChartsmenuRefresh:
			setChartIgnoreCallLayouts(true);
			loadRemoteEntries();
			return true;
		case R.id.itemAdmobsmenuRemove:
			AndlyticsDb.getInstance(getActivity()).saveAdmobDetails(statsActivity.getPackageName(),
					null, null);
			showAccountList();
			if (configSwitcher.getCurrentView().getId() != R.id.base_chart_config) {
				configSwitcher.showPrevious();
			}
			mainViewSwitcher.swap();
			getActivity().supportInvalidateOptionsMenu();
			return true;
		default:
			return (super.onOptionsItemSelected(item));
		}
	}

	private void loadRemoteEntries() {
		// XXX
	}

	@Override
	protected String getChartHint() {
		return "8 " + this.getString(R.string.admob__charts_available) + " ->";
	}

	protected void showAccountList() {
		final AccountManager manager = AccountManager.get(getActivity());
		final Account[] accounts = manager.getAccountsByType(Constants.ACCOUNT_TYPE_ADMOB);
		final int size = accounts.length;
		String[] names = new String[size];
		accountList.removeAllViews();
		for (int i = 0; i < size; i++) {
			names[i] = accounts[i].name;

			View inflate = getActivity().getLayoutInflater().inflate(
					R.layout.admob_account_list_item, null);
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
		// XXX
		//		state.setLoadRemoteSiteList(new LoadRemoteSiteListTask(this, currentAdmobAccount));
		//		Utils.execute(state.loadRemoteSiteList);
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

		Activity activity = getActivity();
		AccountManager.get(activity)
				.addAccount(Constants.ACCOUNT_TYPE_ADMOB, Constants.AUTHTOKEN_TYPE_ADMOB, null,
						null /* options */, activity, callback, null /* handler */);
	}

	@Override
	public ChartSet getChartSet() {
		return ChartSet.ADMOB;
	}

	@Override
	public String getTitle() {
		return getString(R.string.admob);
	}

	private void showStats(AdmobList admobList) {
		admobListAdapter.setOverallStats(admobList.getOverallStats());

		List<Admob> admobStats = admobList.getAdmobs();
		loadChartData(admobStats);
		// make shallow copy
		List<Admob> reversedAdmobStats = new ArrayList<Admob>();
		reversedAdmobStats.addAll(admobStats);
		Collections.reverse(reversedAdmobStats);

		admobListAdapter.setStats(reversedAdmobStats);
		// admobListAdapter.setCurrentChart(currentChart);
		admobListAdapter.notifyDataSetChanged();
	}

	private void loadChartData(List<Admob> statsForApp) {
		/*
		 * if(radioLastThrity != null) { radioLastThrity.setEnabled(false);
		 * radioUnlimited.setEnabled(false); checkSmooth.setEnabled(false); }
		 */

		if (statsForApp != null && statsForApp.size() > 0) {
			updateCharts(statsForApp);

			DateFormat dateFormat = Preferences.getDateFormatLong(getActivity());
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
		// XXX
		//		executeLoadDataDefault(false);
	}

	@Override
	protected List<View> getExtraFullViews() {
		return new ArrayList<View>();
		// XXX
		//		configSwitcher = (ViewSwitcher) findViewById(R.id.base_chart_viewswitcher_config);
		//		configSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
		//		configSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_left));
		//		List<View> ret = new ArrayList<View>();
		//		RelativeLayout ll;
		//
		//		ll = (RelativeLayout) getLayoutInflater().inflate(R.layout.admob_config_selectapp, null);
		//		siteList = (ViewGroup) ll.findViewById(R.id.admob_sitelist);
		//		ret.add(ll);
		//
		//		ll = (RelativeLayout) getLayoutInflater().inflate(R.layout.admob_config_addaccount, null);
		//		accountList = (ViewGroup) ll.findViewById(R.id.admob_accountlist);
		//		addAccountButton = (View) ll.findViewById(R.id.admob_addaccount_button);
		//		ret.add(ll);
		//
		//		addAccountButton.setOnClickListener(new OnClickListener() {
		//
		//			@Override
		//			public void onClick(View v) {
		//				addNewAdmobAccount();
		//			}
		//		});
		//		return ret;

	}

	@Override
	public Loader<LoaderResult<AdmobList>> onCreateLoader(int id, Bundle args) {
		String packageName = null;
		Timeframe timeframe = null;
		if (args != null) {
			packageName = args.getString("packageName");
			timeframe = (Timeframe) args.getSerializable("timeframe");
		}

		return new AdmobDbLoader(getActivity(), packageName, timeframe);
	}

	@Override
	public void onLoadFinished(Loader<LoaderResult<AdmobList>> loader,
			LoaderResult<AdmobList> result) {
		statsActivity.refreshFinished();

		if (result.isFailed()) {
			statsActivity.handleUserVisibleException(result.getError());

			return;
		}

		if (result.getData() == null) {
			return;
		}

		showStats(result.getData());

		// XXX
		//		if (executeRemoteCall) {
		//			new LoadRemoteEntriesTask(activity).execute();
		//		}
	}

	@Override
	public void onLoaderReset(Loader<LoaderResult<AdmobList>> arg0) {
	};

}
