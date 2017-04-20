package com.github.andlyticsproject;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.github.andlyticsproject.chart.Chart;
import com.github.andlyticsproject.chart.Chart.ValueCallbackHander;
import com.github.andlyticsproject.model.AppStats;

public class DownloadsChartListAdapter extends ChartListAdapter<AppStats> {

	private static final int COL_DATE = 0;
	private static final int COL_TOTAL_DOWNLAODS = 1;
	private static final int COL_ACTIVE_INSTALLS_TOTAL = 2;
	private static final int COL_TOTAL_DOWNLAODS_BY_DAY = 3;
	private static final int COL_ACTIVE_INSTALLS_PERCENT = 4;

	public DownloadsChartListAdapter(Activity activity) {
		super(activity);
	}

	@Override
	public int getNumPages() {
		return 1;
	}

	@Override
	public int getNumCharts(int page) throws IndexOutOfBoundsException {
		if (page == 0) {
			return 5;
		}
		throw new IndexOutOfBoundsException("page=" + page);
	}

	@Override
	public String getChartTitle(int page, int column) throws IndexOutOfBoundsException {
		if (column == COL_DATE) {
			return "";
		}

		switch (page) {
		case 0: {
			switch (column) {
			case COL_TOTAL_DOWNLAODS:
				return activity.getString(R.string.total_downloads);

			case COL_TOTAL_DOWNLAODS_BY_DAY:
				return activity.getString(R.string.downloads_day);

			case COL_ACTIVE_INSTALLS_PERCENT:
				return activity.getString(R.string.active_installs_percent);

			case COL_ACTIVE_INSTALLS_TOTAL:
				return activity.getString(R.string.active_installs);
			}
		}
			break;
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
			case COL_TOTAL_DOWNLAODS:
				tv.setText(Integer.toString(appInfo.getTotalDownloads()));
				tv.setTextColor(textColor);
				return;
			case COL_ACTIVE_INSTALLS_TOTAL:
				tv.setText(Integer.toString(appInfo.getActiveInstalls()));
				tv.setTextColor(textColor);
				return;
			case COL_TOTAL_DOWNLAODS_BY_DAY:
				tv.setText(Integer.toString(appInfo.getDailyDownloads()));
				tv.setTextColor(textColor);
				return;
			case COL_ACTIVE_INSTALLS_PERCENT:
				tv.setText(appInfo.getActiveInstallsPercentString());
				tv.setTextColor(textColor);
				return;
			}
		}
			break;
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);

	}

	@Override
	public View buildChart(Context context, Chart baseChart, List<?> statsForApp, int page,
			int column) throws IndexOutOfBoundsException {
		ValueCallbackHander handler = null;
		switch (page) {
		case 0: {
			switch (column) {
			case COL_TOTAL_DOWNLAODS:

				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getTotalDownloads();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);

			case COL_TOTAL_DOWNLAODS_BY_DAY:

				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getDailyDownloads();
					}
				};
				return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
						Integer.MIN_VALUE, 0);

			case COL_ACTIVE_INSTALLS_TOTAL:
				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getActiveInstalls();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);

			case COL_ACTIVE_INSTALLS_PERCENT:
				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getActiveInstallsPercent();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
			}

		}

			break;

		}

		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

	@Override
	public String getSubHeadLine(int page, int column) throws IndexOutOfBoundsException {
		if (column == COL_DATE) {
			return "";
		}

		switch (page) {
		case 0: {
			switch (column) {
			case COL_TOTAL_DOWNLAODS:
				return Integer.toString(overallStats.getTotalDownloads());

			case COL_TOTAL_DOWNLAODS_BY_DAY:
				return Integer.toString(overallStats.getDailyDownloads());

			case COL_ACTIVE_INSTALLS_PERCENT:
				return overallStats.getActiveInstallsPercentString() + "%";

			case COL_ACTIVE_INSTALLS_TOTAL:
				Preferences.saveShowChartHint(activity, false);
				return Integer.toString(overallStats.getActiveInstalls());
			}
		}
			break;
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
