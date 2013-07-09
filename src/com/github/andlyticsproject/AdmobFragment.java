package com.github.andlyticsproject;


import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.admob.AdmobRequest;
import com.github.andlyticsproject.admob.AdmobRequest.SyncCallback;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.model.Admob;
import com.github.andlyticsproject.model.AdmobList;
import com.github.andlyticsproject.model.AppStatsList;
import com.github.andlyticsproject.util.LoaderBase;
import com.github.andlyticsproject.util.LoaderResult;
import com.github.andlyticsproject.view.ViewSwitcher3D;

public class AdmobFragment extends ChartFragment implements
		LoaderManager.LoaderCallbacks<LoaderResult<AdmobList>> {

	static final String ARG_PACKAGE_NAME = "packageName";
	static final String ARG_TIMEFRAME = "timeframe";
	static final String ARG_LOAD_REMOTE = "loadRemote";

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

		static final String ARG_PACKAGE_NAME = "packageName";
		static final String ARG_TIMEFRAME = "timeframe";
		static final String ARG_LOAD_REMOTE = "loadRemote";

		private ContentAdapter db;
		private String packageName;
		private Timeframe timeframe;
		private boolean loadRemote;

		public AdmobDbLoader(Context context, String packageName, Timeframe timeframe,
				boolean loadRemote) {
			super(context);
			db = ContentAdapter.getInstance(AndlyticsApp.getInstance());
			this.packageName = packageName;
			this.timeframe = timeframe;
			this.loadRemote = loadRemote;
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
			//
			String currentAdmobAccount = admobDetails[0];
			String currentSiteId = admobDetails[1];

			if (loadRemote) {
				List<String> siteList = new ArrayList<String>();
				siteList.add(currentSiteId);

				AdmobRequest.syncSiteStats(currentAdmobAccount, getContext(), siteList,
						new SyncCallback() {

							@Override
							public void initialImportStarted() {
							}
						});
			}

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
				statsActivity.getPackage());
		if (admobDetails == null) {
			mainViewSwitcher.swap();
			if (configSwitcher.getCurrentView().getId() != R.id.base_chart_config) {
				configSwitcher.showPrevious();
			}
			showAccountList();
		}

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		loadData(getCurrentTimeFrame(), statsActivity.shouldRemoteUpdateStats());
	}

	private void loadData(Timeframe timeframe, boolean loadRemote) {
		Bundle args = new Bundle();
		args.putString(ARG_PACKAGE_NAME, statsActivity.getPackage());
		args.putSerializable(ARG_TIMEFRAME, timeframe);
		args.putBoolean(ARG_LOAD_REMOTE, loadRemote);
		statsActivity.refreshStarted();

		getLoaderManager().restartLoader(0, args, this);
	}

	@Override
	public void updateView(AppStatsList appStatsList, List<Date> versionUpdateDates) {
		// XXX do nothing, need to redesign super class
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
		inflater.inflate(R.menu.admob_fragment_menu, menu);
		//
		MenuItem activeTimeFrame = null;
		switch (currentTimeFrame) {
		case LAST_SEVEN_DAYS:
			activeTimeFrame = menu.findItem(R.id.itemAdmobsmenuTimeframe7);
			break;
		case LAST_THIRTY_DAYS:
			activeTimeFrame = menu.findItem(R.id.itemAdmobsmenuTimeframe30);
			break;
		case LAST_NINETY_DAYS:
			activeTimeFrame = menu.findItem(R.id.itemAdmobsmenuTimeframe90);
			break;
		case UNLIMITED:
			activeTimeFrame = menu.findItem(R.id.itemAdmobsmenuTimeframeUnlimited);
			break;
		case MONTH_TO_DATE:
			activeTimeFrame = menu.findItem(R.id.itemAdmobsmenuTimeframeMonthToDate);
			break;
		}
		activeTimeFrame.setChecked(true);

		String[] admobDetails = AndlyticsDb.getInstance(getActivity()).getAdmobDetails(
				statsActivity.getPackage());

		if (statsActivity.isRefreshing()) {
			menu.findItem(R.id.itemAdmobsmenuRefresh).setActionView(
					R.layout.action_bar_indeterminate_progress);
		}
		if (admobDetails == null) {
			menu.findItem(R.id.itemAdmobsmenuRemove).setVisible(false);
			menu.findItem(R.id.itemAdmobsmenuTimeframe).setVisible(false);
			menu.findItem(R.id.itemAdmobsmenuRefresh).setVisible(statsActivity.isRefreshing());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Context ctx = getActivity();
		if (ctx == null) {
			return false;
		}

		switch (item.getItemId()) {
		case R.id.itemAdmobsmenuRefresh:
			setChartIgnoreCallLayouts(true);
			loadRemoteEntries();
			return true;
		case R.id.itemAdmobsmenuRemove:
			AndlyticsDb.getInstance(getActivity()).saveAdmobDetails(statsActivity.getPackage(),
					null, null);
			showAccountList();
			if (configSwitcher.getCurrentView().getId() != R.id.base_chart_config) {
				configSwitcher.showPrevious();
			}
			mainViewSwitcher.swap();
			getActivity().supportInvalidateOptionsMenu();
			return true;
		case R.id.itemAdmobsmenuTimeframe7:
			currentTimeFrame = Timeframe.LAST_SEVEN_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_SEVEN_DAYS, ctx);
			item.setChecked(true);
			return true;
		case R.id.itemAdmobsmenuTimeframe30:
			currentTimeFrame = Timeframe.LAST_THIRTY_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_THIRTY_DAYS, ctx);
			item.setChecked(true);
			return true;
		case R.id.itemAdmobsmenuTimeframe90:
			currentTimeFrame = Timeframe.LAST_NINETY_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_NINETY_DAYS, ctx);
			item.setChecked(true);
			return true;
		case R.id.itemAdmobsmenuTimeframeUnlimited:
			currentTimeFrame = Timeframe.UNLIMITED;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.UNLIMITED, ctx);
			item.setChecked(true);
			return true;
		case R.id.itemAdmobsmenuTimeframeMonthToDate:
			currentTimeFrame = Timeframe.MONTH_TO_DATE;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.MONTH_TO_DATE, ctx);
			item.setChecked(true);
			return true;
		default:
			return (super.onOptionsItemSelected(item));
		}
	}

	private void loadRemoteEntries() {
		loadData(currentTimeFrame, true);
	}

	@Override
	protected void executeLoadData(Timeframe currentTimeFrame) {
		loadData(currentTimeFrame, false);
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
		// XXX implement, new loader?
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
		// this can be called before the fragment is attached
		Context ctx = AndlyticsApp.getInstance();
		return ctx.getString(R.string.admob);
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
	}

	@Override
	protected void notifyChangedDataformat() {
		loadData(getCurrentTimeFrame(), false);
	}

	@Override
	protected List<View> getExtraFullViews(View view) {
		configSwitcher = (ViewSwitcher) view.findViewById(R.id.base_chart_viewswitcher_config);
		configSwitcher.setInAnimation(AnimationUtils.loadAnimation(getActivity(),
				R.anim.slide_in_right));
		configSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getActivity(),
				R.anim.slide_out_left));
		List<View> ret = new ArrayList<View>();
		RelativeLayout ll;

		ll = (RelativeLayout) getActivity().getLayoutInflater().inflate(
				R.layout.admob_config_selectapp, null);
		siteList = (ViewGroup) ll.findViewById(R.id.admob_sitelist);
		ret.add(ll);

		ll = (RelativeLayout) getActivity().getLayoutInflater().inflate(
				R.layout.admob_config_addaccount, null);
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

	@Override
	public Loader<LoaderResult<AdmobList>> onCreateLoader(int id, Bundle args) {
		String packageName = null;
		Timeframe timeframe = null;
		boolean loadRemote = false;
		if (args != null) {
			packageName = args.getString(ARG_PACKAGE_NAME);
			timeframe = (Timeframe) args.getSerializable(ARG_TIMEFRAME);
			loadRemote = args.getBoolean(ARG_LOAD_REMOTE);
		}

		return new AdmobDbLoader(getActivity(), packageName, timeframe, loadRemote);
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
	}

	@Override
	public void onLoaderReset(Loader<LoaderResult<AdmobList>> arg0) {
	}

	@Override
	public void initLoader() {
		// NOOP, to fulfill ChartFragment interface
	}

	@Override
	public void restartLoader(Bundle args) {
		// NOOP, to fulfill ChartFragment interface
	}

}
