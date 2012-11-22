package com.github.andlyticsproject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.AppStatsList;
import com.github.andlyticsproject.util.DetachableAsyncTask;
import com.github.andlyticsproject.util.Utils;

public class ChartActivity extends BaseChartActivity {

	private static String TAG = ChartActivity.class.getSimpleName();

	private ContentAdapter db;
	private ListView historyList;
	private ChartListAdapter historyListAdapter;
	private TextView historyListFooter;
	private View oneEntryHint;
	private boolean dataUpdateRequested;

	private ChartSet currentChartSet;
	private Boolean smoothEnabled;

	private LoadChartData loadChartData;

	@Override
	protected void executeLoadData(Timeframe timeFrame) {
		if (loadChartData != null) {
			loadChartData.detach();
		}
		loadChartData = new LoadChartData(this);
		Utils.execute(loadChartData, timeFrame);

	}

	private void executeLoadDataDefault() {
		if (loadChartData != null) {
			loadChartData.detach();
		}
		loadChartData = new LoadChartData(this);
		Utils.execute(loadChartData, getCurrentTimeFrame());

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		Bundle b = intent.getExtras();
		if (b != null) {
			String chartSet = b.getString(Constants.CHART_SET);
			if (chartSet != null) {
				currentChartSet = ChartSet.valueOf(chartSet);
			}
		}

		if (currentChartSet == null) {
			currentChartSet = ChartSet.DOWNLOADS;
		}
		setCurrentChart(currentChartSet.ordinal(), 1);

	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		smoothEnabled = Preferences.getChartSmooth(this);
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		setCurrentChartSet(ChartSet.RATINGS);

		Bundle b = getIntent().getExtras();
		if (b != null) {
			String chartSet = b.getString(Constants.CHART_SET);
			if (chartSet != null) {
				currentChartSet = ChartSet.valueOf(chartSet);
			}
		}

		if (currentChartSet == null) {
			currentChartSet = ChartSet.DOWNLOADS;
		}

		if (ChartSet.RATINGS.equals(currentChartSet)) {
			getSupportActionBar().setTitle(R.string.ratings);
		} else {
			getSupportActionBar().setTitle(R.string.downloads);
		}

		db = getDbAdapter();
		// chartFrame = (ViewSwitcher) ;

		oneEntryHint = (View) findViewById(R.id.base_chart_one_entry_hint);

		historyList = (ListView) findViewById(R.id.base_chart_list);
		View inflate = getLayoutInflater().inflate(R.layout.chart_list_footer, null);
		historyListFooter = (TextView) inflate.findViewById(R.id.chart_footer_text);
		historyList.addFooterView(inflate, null, false);

		historyListAdapter = new ChartListAdapter(this);
		setAdapter(historyListAdapter);

		historyListAdapter.setCurrentChart(currentChartSet.ordinal(), 1);
		setAllowChangePageSliding(false);

		if (getLastNonConfigurationInstance() != null) {
			loadChartData = (LoadChartData) getLastNonConfigurationInstance();
			loadChartData.attach(this);
			if (loadChartData.statsForApp != null && loadChartData.versionUpdateDates != null) {
				updateView(loadChartData.statsForApp, loadChartData.versionUpdateDates);
				dataUpdateRequested = false;
			}
		}
		// first load is handled in onResume()
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return loadChartData == null ? null : loadChartData.detach();
	}

	@Override
	protected String getChartHint() {
		return this.getString(R.string.swipe);
	}

	@Override
	protected void onResume() {
		super.onResume();

		dataUpdateRequested = shouldRemoteUpdateStats();

		executeLoadDataDefault();
	}

	public void setCurrentChartSet(ChartSet currentChartSet) {
		this.currentChartSet = currentChartSet;
	}

	public ChartSet getCurrentChartSet() {
		return currentChartSet;
	}

	private static class LoadChartData extends
			DetachableAsyncTask<Timeframe, Void, Boolean, ChartActivity> {

		LoadChartData(ChartActivity activity) {
			super(activity);
		}

		private AppStatsList statsForApp;
		private List<Date> versionUpdateDates;

		@Override
		protected Boolean doInBackground(Timeframe... params) {
			if (activity == null) {
				return null;
			}

			if (activity.dataUpdateRequested || statsForApp == null
					|| statsForApp.getAppStats().size() == 0) {
				statsForApp = activity.db.getStatsForApp(activity.packageName, params[0],
						activity.smoothEnabled);
				versionUpdateDates = activity.db.getVersionUpdateDates(activity.packageName);

				Log.d(TAG,
						"statsForApp::highestRatingChange " + statsForApp.getHighestRatingChange());
				Log.d(TAG,
						"statsForApp::lowestRatingChanage " + statsForApp.getLowestRatingChange());
				Log.d(TAG, "statsForApp::appStats " + statsForApp.getAppStats().size());
				Log.d(TAG, "statsForApps::overall " + statsForApp.getOverall());
				Log.d(TAG, "versionUpdateDates " + versionUpdateDates.size());

				activity.dataUpdateRequested = false;
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (activity == null) {
				return;
			}

			activity.updateView(statsForApp, versionUpdateDates);
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

	private void updateView(AppStatsList appStatsList, List<Date> versionUpdateDates) {
		List<AppStats> statsForApp = appStatsList.getAppStats();
		if (statsForApp != null && statsForApp.size() > 0) {
			boolean smoothedValues = applySmoothedValues(statsForApp);
			historyListAdapter.setOverallStats(appStatsList.getOverall());
			historyListAdapter.setHeighestRatingChange(appStatsList.getHighestRatingChange());
			historyListAdapter.setLowestRatingChange(appStatsList.getLowestRatingChange());

			updateCharts(statsForApp);

			DateFormat dateFormat = Preferences.getDateFormatLong(this);
			timetext = dateFormat.format(statsForApp.get(0).getRequestDate()) + " - "
					+ dateFormat.format(statsForApp.get(statsForApp.size() - 1).getRequestDate());

			updateChartHeadline();

			// make a shallow copy, otherwise original data can't be used to
			// restore state
			List<AppStats> statsForAppReversed = new ArrayList<AppStats>();
			statsForAppReversed.addAll(statsForApp);
			Collections.reverse(statsForAppReversed);
			historyListAdapter.setDownloadInfos(statsForAppReversed);
			historyListAdapter.setVersionUpdateDates(versionUpdateDates);
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

			// chartFrame.showNext();

		}
	}

	@Override
	protected void notifyChangedDataformat() {
		dataUpdateRequested = true;
		executeLoadDataDefault();

	}

	@Override
	protected void onChartSelected(int page, int column) {
		super.onChartSelected(page, column);
		if (page != currentChartSet.ordinal()) {
			currentChartSet = ChartSet.values()[page];
			updateTabbarButtons();
		}

	}

}
