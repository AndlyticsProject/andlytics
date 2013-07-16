package com.github.andlyticsproject.legacy;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.github.andlyticsproject.ChartListAdapter;
import com.github.andlyticsproject.ContentAdapter;
import com.github.andlyticsproject.DetailsActivity;
import com.github.andlyticsproject.Preferences;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.R;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.AppStatsSummary;
import com.github.andlyticsproject.util.DetachableAsyncTask;
import com.github.andlyticsproject.util.Utils;

public class ChartActivity extends BaseChartActivity {

	private static String TAG = ChartActivity.class.getSimpleName();
	private static final boolean DEBUG = false;

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
		// reload since time frame has changed
		dataUpdateRequested = true;
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
			String chartSet = b.getString(DetailsActivity.EXTRA_CHART_SET);
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
			String chartSet = b.getString(DetailsActivity.EXTRA_CHART_SET);
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

		if (getLastCustomNonConfigurationInstance() != null) {
			loadChartData = (LoadChartData) getLastCustomNonConfigurationInstance();
			loadChartData.attach(this);
			if (loadChartData.statsForApp != null) {
				updateView(loadChartData.statsForApp);
				dataUpdateRequested = false;
			}
		}
		// first load is handled in onResume()
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
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

		private ContentAdapter db;

		LoadChartData(ChartActivity activity) {
			super(activity);
			db = ContentAdapter.getInstance(activity.getApplication());
		}

		private AppStatsSummary statsForApp;

		@Override
		protected void onPreExecute() {
			if (activity == null) {
				return;
			}

			activity.refreshStarted();
		}

		@Override
		protected Boolean doInBackground(Timeframe... params) {
			if (activity == null) {
				return null;
			}

			if (activity.dataUpdateRequested
					|| activity.historyListAdapter.getDownloadInfos() == null
					|| activity.historyListAdapter.isEmpty()) {
				statsForApp = db.getStatsForApp(activity.packageName, params[0],
						activity.smoothEnabled);

				if (DEBUG) {
					Log.d(TAG,
							"statsForApp::highestRatingChange "
									+ statsForApp.getHighestRatingChange());
					Log.d(TAG,
							"statsForApp::lowestRatingChanage "
									+ statsForApp.getLowestRatingChange());
					Log.d(TAG, "statsForApp::appStats " + statsForApp.getAppStats().size());
					Log.d(TAG, "statsForApps::overall " + statsForApp.getOverallStats());
				}

				activity.dataUpdateRequested = false;

				return true;
			}

			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (activity == null) {
				return;
			}

			if (result && statsForApp != null) {
				activity.updateView(statsForApp);
			}
			activity.refreshFinished();
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

	private void updateView(AppStatsSummary appStatsList) {
		List<AppStats> statsForApp = appStatsList.getAppStats();
		if (statsForApp != null && statsForApp.size() > 0) {
			boolean smoothedValues = applySmoothedValues(statsForApp);
			historyListAdapter.setOverallStats(appStatsList.getOverallStats());
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
