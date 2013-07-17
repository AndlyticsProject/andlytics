package com.github.andlyticsproject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.AppStatsSummary;
import com.github.andlyticsproject.util.LoaderBase;
import com.github.andlyticsproject.util.LoaderResult;


public abstract class ChartFragment extends ChartFragmentBase implements StatsView {


	static class AppStatsSummaryLoader extends LoaderBase<AppStatsSummary> {

		static final String ARG_PACKAGE_NAME = "packageName";
		static final String ARG_TIMEFRAME = "timeframe";
		static final String ARG_SMOOTH_ENABLED = "smoothEnabled";

		private ContentAdapter db;
		private String packageName;
		private Timeframe timeframe;
		private boolean smoothEnabled;

		public AppStatsSummaryLoader(Activity context, String packageName, Timeframe timeframe,
				boolean smoothEnabled) {
			super(context);
			db = ContentAdapter.getInstance(AndlyticsApp.getInstance());
			this.packageName = packageName;
			this.timeframe = timeframe;
			this.smoothEnabled = smoothEnabled;
		}

		@Override
		protected AppStatsSummary load() throws Exception {
			if (packageName == null) {
				return null;
			}

			return db.getStatsForApp(packageName, timeframe, smoothEnabled);
		}

		@Override
		protected void releaseResult(LoaderResult<AppStatsSummary> result) {
			// just a string, nothing to do
		}

		@Override
		protected boolean isActive(LoaderResult<AppStatsSummary> result) {
			return false;
		}
	}

	protected DetailedStatsActivity statsActivity;

	private ListView historyList;
	private ChartListAdapter historyListAdapter;
	private TextView historyListFooter;
	private View oneEntryHint;
	private boolean dataUpdateRequested;

	protected ChartSet currentChartSet;
	private boolean smoothEnabled;

	public ChartFragment() {
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSherlockActivity().getSupportActionBar().setTitle(getTitle());

		Bundle b = getArguments();
		if (b != null) {
			String chartSet = b.getString(DetailsActivity.EXTRA_CHART_SET);
			if (chartSet != null) {
				currentChartSet = ChartSet.valueOf(chartSet);
			}
		}

		currentChartSet = getChartSet();
		smoothEnabled = Preferences.getChartSmooth(getActivity());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// just init don't try to load
		initLoader();
	}

	public abstract void initLoader();

	public abstract void restartLoader(Bundle args);

	public abstract ChartSet getChartSet();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		oneEntryHint = (View) view.findViewById(R.id.base_chart_one_entry_hint);

		historyList = (ListView) view.findViewById(R.id.base_chart_list);
		View inflate = getActivity().getLayoutInflater().inflate(R.layout.chart_list_footer, null);
		historyListFooter = (TextView) inflate.findViewById(R.id.chart_footer_text);
		historyList.addFooterView(inflate, null, false);

		historyListAdapter = new ChartListAdapter(getActivity());
		setAdapter(historyListAdapter);

		historyListAdapter.setCurrentChart(currentChartSet.ordinal(), 1);
		setAllowChangePageSliding(false);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		executeLoadData(currentTimeFrame);
	}

	public void updateView(AppStatsSummary appStatsList) {
		if (appStatsList == null) {
			return;
		}

		if (historyListAdapter == null) {
			return;
		}

		List<AppStats> statsForApp = appStatsList.getAppStats();
		if (statsForApp != null && statsForApp.size() > 0) {
			boolean smoothedValues = applySmoothedValues(statsForApp);
			historyListAdapter.setOverallStats(appStatsList.getOverallStats());
			historyListAdapter.setHeighestRatingChange(appStatsList.getHighestRatingChange());
			historyListAdapter.setLowestRatingChange(appStatsList.getLowestRatingChange());

			updateCharts(statsForApp);

			DateFormat dateFormat = Preferences.getDateFormatLong(getActivity());
			timetext = dateFormat.format(statsForApp.get(0).getRequestDate()) + " - "
					+ dateFormat.format(statsForApp.get(statsForApp.size() - 1).getRequestDate());

			updateChartHeadline();

			// make a shallow copy, otherwise original data can't be used to
			// restore state
			List<AppStats> statsForAppReversed = new ArrayList<AppStats>();
			statsForAppReversed.addAll(statsForApp);
			Collections.reverse(statsForAppReversed);
			historyListAdapter.setDownloadInfos(statsForAppReversed);
			/*
			 * int page=historyListAdapter.getCurrentPage(); int
			 * column=historyListAdapter.getCurrentColumn();
			 * historyListAdapter.setCurrentChart(page, column);
			 */
			historyListAdapter.notifyDataSetChanged();

			if (smoothedValues && currentChartSet.equals(ChartSet.DOWNLOADS)) {
				historyListFooter.setVisibility(View.VISIBLE);
			} else {
				historyListFooter.setVisibility(View.INVISIBLE);
			}

			if (oneEntryHint != null) {
				if (statsForApp.size() == 1) {
					oneEntryHint.setVisibility(View.VISIBLE);
				} else {
					oneEntryHint.setVisibility(View.INVISIBLE);
				}
			}
		}
	}

	private static boolean applySmoothedValues(List<AppStats> statsForApp) {
		for (AppStats appInfo : statsForApp) {
			if (appInfo.isSmoothingApplied()) {
				return true;
			}
		}

		return false;
	}

	protected String getChartHint() {
		return getString(R.string.revenue);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			statsActivity = (DetailedStatsActivity) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		statsActivity = null;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		menu.clear();
		inflater.inflate(R.menu.charts_menu, menu);
		MenuItem activeTimeFrame = null;
		switch (currentTimeFrame) {
		case LAST_SEVEN_DAYS:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframe7);
			break;
		case LAST_THIRTY_DAYS:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframe30);
			break;
		case LAST_NINETY_DAYS:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframe90);
			break;
		case UNLIMITED:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframeUnlimited);
			break;
		case MONTH_TO_DATE:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframeMonthToDate);
			break;
		}
		activeTimeFrame.setChecked(true);

		if (statsActivity.isRefreshing()) {
			menu.findItem(R.id.itemChartsmenuRefresh).setActionView(
					R.layout.action_bar_indeterminate_progress);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Context ctx = getActivity();

		switch (item.getItemId()) {
		case R.id.itemChartsmenuRefresh:
			setChartIgnoreCallLayouts(true);
			executeLoadData(currentTimeFrame);
			return true;
		case R.id.itemChartsmenuToggle:
			toggleChartData(item);
			return true;
		case R.id.itemChartsmenuTimeframe7:
			currentTimeFrame = Timeframe.LAST_SEVEN_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_SEVEN_DAYS, ctx);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframe30:
			currentTimeFrame = Timeframe.LAST_THIRTY_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_THIRTY_DAYS, ctx);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframe90:
			currentTimeFrame = Timeframe.LAST_NINETY_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_NINETY_DAYS, ctx);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframeUnlimited:
			currentTimeFrame = Timeframe.UNLIMITED;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.UNLIMITED, ctx);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframeMonthToDate:
			currentTimeFrame = Timeframe.MONTH_TO_DATE;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.MONTH_TO_DATE, ctx);
			item.setChecked(true);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void notifyChangedDataformat() {
		dataUpdateRequested = true;
		executeLoadData(currentTimeFrame);
	}

	@Override
	protected void executeLoadData(Timeframe currentTimeFrame) {
		Bundle args = new Bundle();
		args.putString(AppStatsSummaryLoader.ARG_PACKAGE_NAME, statsActivity.getPackage());
		args.putBoolean(AppStatsSummaryLoader.ARG_SMOOTH_ENABLED, smoothEnabled);
		args.putSerializable(AppStatsSummaryLoader.ARG_TIMEFRAME, currentTimeFrame);

		statsActivity.refreshStarted();

		restartLoader(args);
	}

	public void setCurrentChartSet(ChartSet currentChartSet) {
		this.currentChartSet = currentChartSet;
	}

	public ChartSet getCurrentChartSet() {
		return currentChartSet;
	}

	@Override
	public void setCurrentChart(int page, int column) {
		int pos = 0;
		for (View view : chartGalleryAdapter.getViews()) {
			int pageColumn[] = (int[]) view.getTag();
			if (page == pageColumn[0] && column == pageColumn[1]) {
				chartGallery.setSelection(pos, false);
				return;
			}
			pos++;
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

}
