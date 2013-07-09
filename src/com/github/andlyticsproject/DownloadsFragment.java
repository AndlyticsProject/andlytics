package com.github.andlyticsproject;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.github.andlyticsproject.ChartFragment.ChartData;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.util.LoaderResult;

public class DownloadsFragment extends ChartFragment implements
		LoaderManager.LoaderCallbacks<LoaderResult<ChartData>> {

	private static final String TAG = DownloadsFragment.class.getSimpleName();

	public DownloadsFragment() {
	}

	@Override
	public ChartSet getChartSet() {
		return ChartSet.DOWNLOADS;
	}

	@Override
	public String getTitle() {
		// this can be called before the fragment is attached
		Context ctx = AndlyticsApp.getInstance();
		return ctx.getString(R.string.downloads);
	}

	@Override
	public Loader<LoaderResult<ChartData>> onCreateLoader(int id, Bundle args) {
		String packageName = null;
		Timeframe timeframe = null;
		boolean smoothEnabled = false;
		if (args != null) {
			packageName = args.getString(ChartDataLoader.ARG_PACKAGE_NAME);
			timeframe = (Timeframe) args.getSerializable(ChartDataLoader.ARG_TIMEFRAME);
			smoothEnabled = args.getBoolean(ChartDataLoader.ARG_SMOOTH_ENABLED);
		}

		return new ChartDataLoader(getActivity(), packageName, timeframe, smoothEnabled);
	}

	@Override
	public void onLoadFinished(Loader<LoaderResult<ChartData>> loader,
			LoaderResult<ChartData> result) {
		if (getActivity() == null) {
			return;
		}

		statsActivity.refreshFinished();

		if (result.isFailed()) {
			Log.e(TAG, "Error fetching chart data: " + result.getError().getMessage(),
					result.getError());
			statsActivity.handleUserVisibleException(result.getError());

			return;
		}

		if (result.getData() == null) {
			return;
		}

		ChartData chartData = result.getData();
		if (chartData.statsForApp != null && chartData.versionUpdateDates != null) {
			updateView(chartData.statsForApp, chartData.versionUpdateDates);
		}
	}

	@Override
	public void onLoaderReset(Loader<LoaderResult<ChartData>> loader) {
	}

	@Override
	public void initLoader() {
		getLoaderManager().restartLoader(0, null, this);

	}

	@Override
	public void restartLoader(Bundle args) {
		getLoaderManager().restartLoader(0, args, this);
	}

}
