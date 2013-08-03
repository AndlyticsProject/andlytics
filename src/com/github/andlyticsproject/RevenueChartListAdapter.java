package com.github.andlyticsproject;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.github.andlyticsproject.chart.Chart;
import com.github.andlyticsproject.chart.Chart.ValueCallbackHander;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.util.Utils;

public class RevenueChartListAdapter extends ChartListAdapter<AppStats> {

	private static final int COL_REVENUE = 1;
	private static final int COL_DEVELOPER_CUT = 2;

	public RevenueChartListAdapter(Activity activity) {
		super(activity);
	}

	@Override
	public int getNumPages() {
		return 1;
	}

	@Override
	public int getNumCharts(int page) throws IndexOutOfBoundsException {
		switch (page) {
			case 0:
				return 3;
		}
		throw new IndexOutOfBoundsException("page=" + page);
	}

	@Override
	public String getChartTitle(int page, int column) throws IndexOutOfBoundsException {
		if (column == COL_DATE) {
			return "";
		}

		switch (page) {
			case 0:
				switch (column) {
					case COL_REVENUE:
						return activity.getString(R.string.total_revenue);
					case COL_DEVELOPER_CUT:
						return "Developer cut";
				}
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);

	}

	@Override
	public void updateChartValue(int position, int page, int column, TextView tv)
			throws IndexOutOfBoundsException {
		AppStats appInfo = getItem(position);
		if (column == COL_DATE) {
			tv.setText(dateFormat.format(appInfo.getDate()));
			return;
		}
		int textColor = BLACK_TEXT;
		switch (page) {
			case 0: {

				switch (column) {
					case COL_REVENUE:
						tv.setText(Utils.safeToString(appInfo.getTotalRevenue()));
						tv.setTextColor(textColor);
						return;
					case COL_DEVELOPER_CUT:
						if (appInfo.getTotalRevenue() == null) {
							tv.setText("");
						} else {
							tv.setText(appInfo.getTotalRevenue().developerCutAsString());
						}
						tv.setTextColor(textColor);
						return;
				}

			}
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);

	}

	public View buildChart(Context context, Chart baseChart, List<?> statsForApp, int page,
			int column) throws IndexOutOfBoundsException {
		ValueCallbackHander handler = null;
		switch (page) {
			case 0:
				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						AppStats stats = (AppStats) appInfo;
						return stats.getTotalRevenue() == null ? 0 : stats.getTotalRevenue()
								.getAmount();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
		}

		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

	@Override
	public String getSubHeadLine(int page, int column) throws IndexOutOfBoundsException {
		if (column == COL_DATE) {
			return "";
		}
		switch (page) {
			case 0:
				Preferences.saveShowChartHint(activity, false);
				if (overallStats == null) {
					return "";
				}

				switch (column) {
					case COL_REVENUE:
						return overallStats.getTotalRevenue() == null ? "unknown" : overallStats
								.getTotalRevenue().asString();
					case COL_DEVELOPER_CUT:
						return overallStats.getTotalRevenue() == null ? "unknown" : overallStats
								.getTotalRevenue().developerCutAsString();
				}
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

	@Override
	public AppStats getItem(int position) {
		return getStats().get(position);
	}

	@Override
	protected boolean isSmothValue(int page, int position) {
		return page == 0 ? getItem(position).isSmoothingApplied() : false;
	}

}
