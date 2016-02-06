package com.github.andlyticsproject;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.adsense.AdSenseClient;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.model.AdmobStats;
import com.github.andlyticsproject.model.AdmobStatsSummary;
import com.github.andlyticsproject.model.StatsSummary;
import com.github.andlyticsproject.util.DetachableAsyncTask;
import com.github.andlyticsproject.util.LoaderBase;
import com.github.andlyticsproject.util.LoaderResult;
import com.github.andlyticsproject.util.Utils;
import com.github.andlyticsproject.view.ViewSwitcher3D;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdmobFragment extends ChartFragment<AdmobStats> implements
		LoaderManager.LoaderCallbacks<LoaderResult<AdmobStatsSummary>> {

	static final String ARG_PACKAGE_NAME = "packageName";
	static final String ARG_TIMEFRAME = "timeframe";
	static final String ARG_LOAD_REMOTE = "loadRemote";

	private static final String TAG = AdmobFragment.class.getSimpleName();

	private AdmobListAdapter admobListAdapter;
	public Integer heighestRatingChange;
	public Integer lowestRatingChange;
	private ViewSwitcher3D mainViewSwitcher;
	private ViewGroup accountList;

	private ViewSwitcher configSwitcher;

	private ViewGroup siteList;

	private LoadAdUnitsTask loadAdUnitsTask;

	private String selectedAdmobAccount;

	static class AdmobDbLoader extends LoaderBase<AdmobStatsSummary> {

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
		protected AdmobStatsSummary load() throws Exception {
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
			String adUnitId = admobDetails[2];

			Log.d(TAG, "Loading Admob stats...");
			if (loadRemote) {
				AdSenseClient.foregroundSyncStats(getContext(), currentAdmobAccount,
						Arrays.asList(adUnitId));
			}

			Log.d(TAG, "Loading Admob stats from DB...");
			return db.getAdmobStats(currentSiteId, adUnitId, timeframe);
		}

		@Override
		protected void releaseResult(LoaderResult<AdmobStatsSummary> result) {
			// just a string, nothing to do
		}

		@Override
		protected boolean isActive(LoaderResult<AdmobStatsSummary> result) {
			return false;
		}
	}

	public AdmobFragment() {
		setHasOptionsMenu(true);
		setRetainInstance(true);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// init loader
		loadCurrentData();
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

		Log.d(TAG, "onResume()");

		if (statsActivity.shouldRemoteUpdateStats()) {
			loadRemoteData();
		} else {
			loadCurrentData();
		}
	}


	private void loadRemoteData() {
		loadData(getCurrentTimeFrame(), true);
	}

	private void loadData(Timeframe timeframe, boolean loadRemote) {
		Bundle args = new Bundle();
		args.putString(ARG_PACKAGE_NAME, statsActivity.getPackage());
		args.putSerializable(ARG_TIMEFRAME, timeframe);
		args.putBoolean(ARG_LOAD_REMOTE, loadRemote);
		statsActivity.refreshStarted();

		Log.d(TAG, "Restarting loader");
		getLoaderManager().restartLoader(0, args, this);
	}

	@Override
	protected void loadCurrentData() {
		Bundle args = new Bundle();
		args.putString(ARG_PACKAGE_NAME, statsActivity.getPackage());
		args.putSerializable(ARG_TIMEFRAME, getCurrentTimeFrame());
		args.putBoolean(ARG_LOAD_REMOTE, false);
		statsActivity.refreshStarted();

		getLoaderManager().initLoader(0, args, this);
	}

	@Override
	public void updateView(StatsSummary<AdmobStats> statsSummary) {
		admobListAdapter.setOverallStats(statsSummary.getOverallStats());

		List<AdmobStats> admobStats = statsSummary.getStats();
		loadChartData(admobStats);
		// make shallow copy
		List<AdmobStats> reversedAdmobStats = new ArrayList<AdmobStats>();
		reversedAdmobStats.addAll(admobStats);
		Collections.reverse(reversedAdmobStats);

		admobListAdapter.setStats(reversedAdmobStats);
		// admobListAdapter.setCurrentChart(currentChart);
		admobListAdapter.notifyDataSetChanged();

		restoreChartSelection();
	}

	@Override
	public void setupListAdapter(ChartListAdapter<AdmobStats> listAdapter,
			StatsSummary<AdmobStats> statsSummary) {
		// nothing to do
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
			getActivity().invalidateOptionsMenu();
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

	private void showAccountList() {
		final AccountManager manager = AccountManager.get(getActivity());
		final Account[] accounts = manager.getAccountsByType("com.google");
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
					selectedAdmobAccount = currentAdmobAccount;
					configSwitcher.showNext();
					loadAdUnits();

				}
			});
			accountList.addView(inflate);
		}
	}

	void loadAdUnits() {
		if (getActivity() == null) {
			return;
		}

		// can't have two loaders with different interface, use 
		// AsyncTask and retained fragment
		if (loadAdUnitsTask != null) {
			loadAdUnitsTask.cancel(true);
			loadAdUnitsTask = null;
		}

		loadAdUnitsTask = new LoadAdUnitsTask(getActivity(), this, selectedAdmobAccount);
		Utils.execute(loadAdUnitsTask);
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

	private void loadChartData(List<AdmobStats> statsForApp) {
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

		RelativeLayout container = (RelativeLayout) getActivity().getLayoutInflater().inflate(
				R.layout.admob_config_selectapp, null);
		siteList = (ViewGroup) container.findViewById(R.id.admob_sitelist);
		ret.add(container);

		container = (RelativeLayout) getActivity().getLayoutInflater().inflate(
				R.layout.admob_config_addaccount, null);
		accountList = (ViewGroup) container.findViewById(R.id.admob_accountlist);
		ret.add(container);

		return ret;

	}

	@Override
	public Loader<LoaderResult<AdmobStatsSummary>> onCreateLoader(int id, Bundle args) {
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
	public void onLoadFinished(Loader<LoaderResult<AdmobStatsSummary>> loader,
			LoaderResult<AdmobStatsSummary> result) {
		statsActivity.refreshFinished();

		if (result.isFailed()) {
			statsActivity.handleUserVisibleException(result.getError());

			return;
		}

		if (result.getData() == null) {
			return;
		}

		updateView(result.getData());
	}

	@Override
	public void onLoaderReset(Loader<LoaderResult<AdmobStatsSummary>> arg0) {
	}

	@Override
	public void initLoader(Bundle args) {
		// NOOP, to fulfill ChartFragment interface
	}

	@Override
	public void restartLoader(Bundle args) {
		// NOOP, to fulfill ChartFragment interface
	}

	private static class LoadAdUnitsTask extends
			DetachableAsyncTask<Void, Void, Exception, Activity> {

		// ad unit -> name
		private Map<String, String> adUnits;

		private AdmobFragment admobFragment;
		private DetailedStatsActivity statsActivity;
		private String admobAccount;

		public LoadAdUnitsTask(Activity activity, AdmobFragment admobFragment, String admobAccount) {
			super(activity);
			this.statsActivity = (DetailedStatsActivity) activity;
			this.admobFragment = admobFragment;
			this.admobAccount = admobAccount;
		}

		@Override
		protected void onPreExecute() {
			if (activity == null) {
				return;
			}

			statsActivity.refreshStarted();
		}

		@Override
		protected Exception doInBackground(Void... params) {
			if (activity == null) {
				return null;
			}

			try {
				adUnits = AdSenseClient.getAdUnits(activity, admobAccount);
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

			statsActivity.refreshFinished();

			if (error != null) {
				if (error instanceof UserRecoverableAuthIOException) {
					activity.startActivityForResult(
							((UserRecoverableAuthIOException) error).getIntent(),
							BaseActivity.REQUEST_AUTHORIZATION);

					return;
				} else if (error instanceof GoogleJsonResponseException) {
					GoogleJsonError details = ((GoogleJsonResponseException) error).getDetails();
					String message = error.getMessage();
					if (details != null) {
						message = details.getMessage();
					}
					Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
					admobFragment.configSwitcher.showPrevious();

					return;
				} else if (error.getCause() instanceof GoogleAuthException) {
					String message = ((GoogleAuthException) error.getCause()).getMessage();
					Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
					admobFragment.configSwitcher.showPrevious();

					return;
				}


				statsActivity.handleUserVisibleException(error);
				return;
			}

			if (adUnits != null && adUnits.size() > 0) {
				admobFragment.siteList.removeAllViews();

				Set<String> keySet = adUnits.keySet();
				for (String siteId : keySet) {

					String siteName = adUnits.get(siteId);

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
							String admobAdUnitId = (String) view.getTag();
							AndlyticsDb.getInstance(activity).saveAdmobAdUnitId(
									statsActivity.getPackage(), admobAccount, admobAdUnitId);

							admobFragment.mainViewSwitcher.swap();
							admobFragment.loadRemoteData();
							activity.invalidateOptionsMenu();
						}
					});
					admobFragment.siteList.addView(inflate);

				}
			}
		}
	}

	@Override
	public ChartListAdapter<AdmobStats> createChartAdapter() {
		return new AdmobListAdapter(getActivity());
	}

}
