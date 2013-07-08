package com.github.andlyticsproject;


import com.github.andlyticsproject.chart.Chart.ChartSet;

public class RatingsFragment extends ChartFragment {

	public RatingsFragment() {
	}

	@Override
	public ChartSet getChartSet() {
		return ChartSet.RATINGS;
	}

	@Override
	public String getTitle() {
		return getString(R.string.ratings);
	}

}
