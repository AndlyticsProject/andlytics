package com.github.andlyticsproject;


import com.github.andlyticsproject.chart.Chart.ChartSet;

public class DownloadsFragment extends ChartFragment {

	public DownloadsFragment() {
	}

	@Override
	public ChartSet getChartSet() {
		return ChartSet.DOWNLOADS;
	}

	@Override
	public String getTitle() {
		return getString(R.string.downloads);
	}

}
