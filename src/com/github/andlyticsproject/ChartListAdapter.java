package com.github.andlyticsproject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;

import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.chart.Chart.ValueCallbackHander;
import com.github.andlyticsproject.model.AppStats;

@SuppressLint("SimpleDateFormat")
public abstract class ChartListAdapter<T> extends BaseChartListAdapter<T> {

	protected static int BLACK_TEXT;

	protected static final int COL_DATE = 0;

	protected Activity activity;

	protected SimpleDateFormat dateFormat;

	protected List<T> stats;
	protected T overallStats;

	@SuppressLint("SimpleDateFormat")
	public ChartListAdapter(Activity activity) {
		super(activity);
		BLACK_TEXT = activity.getResources().getColor(R.color.blackText);
		this.stats = new ArrayList<T>();
		this.activity = activity;
		this.dateFormat = new SimpleDateFormat(Preferences.getDateFormatStringShort(activity));
	}

	@Override
	public int getCount() {
		return stats.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public void setStats(List<T> stats) {
		this.stats = stats;
	}

	public List<T> getStats() {
		return stats;
	}

	public void setOverallStats(T overallStats) {
		this.overallStats = overallStats;
	}

	@Override
	public void notifyDataSetChanged() {
		this.dateFormat = new SimpleDateFormat(Preferences.getDateFormatStringShort(activity));
		super.notifyDataSetChanged();
	}

	@Override
	public int getNumPages() {
		return 3;
	}

	@Override
	public int getNumCharts(int page) throws IndexOutOfBoundsException {
		switch (ChartSet.values()[page]) {
		case DOWNLOADS:
			return 5;
		case RATINGS:
			return 7;
		case REVENUE:
			return 2;
		}
		throw new IndexOutOfBoundsException("page=" + page);
	}

	@Override
	protected boolean useSmothColumn(int page) {
		return page == 0;
	}

	public static abstract class DevConValueCallbackHander implements ValueCallbackHander {
		@Override
		public Date getDate(Object appInfo) {
			return ((AppStats) appInfo).getDate();
		}

		@Override
		public boolean isHeilightValue(Object current, Object previouse) {

			if (previouse == null) {
				return false;
			}

			AppStats cstats = ((AppStats) current);

			if (cstats.getVersionCode() == 0) {
				return false;
			}

			if (cstats.getVersionCode() < ((AppStats) previouse).getVersionCode()) {
				return true;
			}

			return false;
		}
	}



}
