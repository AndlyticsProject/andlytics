package com.github.andlyticsproject;


import com.github.andlyticsproject.chart.Chart.ChartSet;

public class RevenueFragment extends ChartFragment {

	public RevenueFragment() {
	}

	@Override
	public ChartSet getChartSet() {
		return ChartSet.REVENUE;
	}

	@Override
	public String getTitle() {
		return getString(R.string.revenue);
	}

}
