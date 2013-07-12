package com.github.andlyticsproject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.github.andlyticsproject.chart.Chart;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.chart.Chart.ValueCallbackHander;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.util.Utils;

public class ChartListAdapter extends BaseChartListAdapter {
	// private static String LOG_TAG=ChartListAdapter.class.toString();

	private static int BLACK_TEXT;

	private static final int RED_TEXT = Color.RED;

	private static final int COL_DATE = 0;
	private static final int COL_TOTAL_DOWNLAODS = 1;
	private static final int COL_ACTIVE_INSTALLS_TOTAL = 2;
	private static final int COL_TOTAL_DOWNLAODS_BY_DAY = 3;
	private static final int COL_ACTIVE_INSTALLS_PERCENT = 4;

	private static final int COL_AVG_RATING = 1;
	private static final int COL_RATINGS_5 = 2;
	private static final int COL_RATINGS_4 = 3;
	private static final int COL_RATINGS_3 = 4;
	private static final int COL_RATINGS_2 = 5;
	private static final int COL_RATINGS_1 = 6;

	private static final int COL_REVENUE = 1;

	private Integer heighestRatingChange;
	private Integer lowestRatingChange;

	private List<AppStats> downloadInfos;

	private List<Date> versionUpdateDates;

	private Activity activity;

	private SimpleDateFormat dateFormat;
	private AppStats overallStats;

	public ChartListAdapter(Activity activity) {
		super(activity);
		BLACK_TEXT = activity.getResources().getColor(R.color.blackText);
		this.setDownloadInfos(new ArrayList<AppStats>());
		this.activity = activity;
		this.dateFormat = new SimpleDateFormat(Preferences.getDateFormatStringShort(activity));
	}

	@Override
	public int getCount() {
		return getDownloadInfos().size();
	}

	@Override
	public AppStats getItem(int position) {
		return getDownloadInfos().get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	/*
	 * public Object getCurrentChart() { return currentChart; }
	 * 
	 * public void setCurrentChart(Object currentChart) { this.currentChart =
	 * currentChart; }
	 */
	public void setDownloadInfos(List<AppStats> downloadInfos) {
		this.downloadInfos = downloadInfos;
	}

	public List<AppStats> getDownloadInfos() {
		return downloadInfos;
	}

	@Override
	public void notifyDataSetChanged() {
		this.dateFormat = new SimpleDateFormat(Preferences.getDateFormatStringShort(activity));
		super.notifyDataSetChanged();
	}

	public void setVersionUpdateDates(List<Date> versionUpdateDates) {
		this.versionUpdateDates = versionUpdateDates;
	}

	public List<Date> getVersionUpdateDates() {
		return versionUpdateDates;
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

	public void setOverallStats(AppStats overallStats) {
		this.overallStats = overallStats;
	}

	@Override
	public String getChartTitle(int page, int column) throws IndexOutOfBoundsException {
		if (column == COL_DATE) {
			return "";
		}
		switch (ChartSet.values()[page]) {
		case DOWNLOADS: {
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
		case RATINGS: {
			switch (column) {
			case COL_AVG_RATING:
				return activity.getString(R.string.average_rating);

			case COL_RATINGS_1:
				return "1* " + activity.getString(R.string.num_ratings);
			case COL_RATINGS_2:
				return "2* " + activity.getString(R.string.num_ratings);
			case COL_RATINGS_3:
				return "3* " + activity.getString(R.string.num_ratings);
			case COL_RATINGS_4:
				return "4* " + activity.getString(R.string.num_ratings);
			case COL_RATINGS_5:
				return "5* " + activity.getString(R.string.num_ratings);
			}

		}
			break;
		case REVENUE:
			return activity.getString(R.string.daily_revenue);
		}
		throw new IndexOutOfBoundsException("page=" + page + " columnt=" + column);

	}

	@Override
	protected boolean isSmothValue(int page, int position) {

		return page == 0 ? getItem(position).isSmoothingApplied() : false;
	}

	@Override
	protected boolean useSmothColumn(int page) {
		return page == 0;
	}

	@Override
	public void updateChartValue(int position, int page, int column, TextView tv)
			throws IndexOutOfBoundsException {
		AppStats appInfo = getItem(position);
		if (column == COL_DATE) {
			tv.setText(dateFormat.format(appInfo.getRequestDate()));
			return;
		}
		int textColor = versionUpdateDates.contains(appInfo.getRequestDate()) ? RED_TEXT
				: BLACK_TEXT;
		switch (ChartSet.values()[page]) {
		case DOWNLOADS: {

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
		case RATINGS: {

			switch (column) {
			case COL_AVG_RATING:
				tv.setText(appInfo.getAvgRatingString());
				tv.setTextColor(textColor);
				return;
			case COL_RATINGS_5:
				if (appInfo.getRating5Diff() > 0) {
					tv.setText("+" + appInfo.getRating5Diff());
				} else {
					tv.setText(appInfo.getRating5Diff().toString());
				}
				tv.setTextColor(textColor);
				return;
			case COL_RATINGS_4:
				if (appInfo.getRating4Diff() > 0) {
					tv.setText("+" + appInfo.getRating4Diff());
				} else {
					tv.setText(appInfo.getRating4Diff().toString());
				}
				tv.setTextColor(textColor);
				return;
			case COL_RATINGS_3:
				if (appInfo.getRating3Diff() > 0) {
					tv.setText("+" + appInfo.getRating3Diff());
				} else {
					tv.setText(appInfo.getRating3Diff().toString());
				}
				tv.setTextColor(textColor);
				return;
			case COL_RATINGS_2:
				if (appInfo.getRating2Diff() > 0) {
					tv.setText("+" + appInfo.getRating2Diff());
				} else {
					tv.setText(appInfo.getRating2Diff().toString());
				}
				tv.setTextColor(textColor);
				return;
			case COL_RATINGS_1:
				if (appInfo.getRating1Diff() > 0) {
					tv.setText("+" + appInfo.getRating1Diff());
				} else {
					tv.setText(appInfo.getRating1Diff().toString());
				}
				tv.setTextColor(textColor);
				return;
			}

		}
			break;
		case REVENUE: {

			switch (column) {
			case COL_REVENUE:
				tv.setText(Utils.safeToString(appInfo.getTotalRevenue()));
				tv.setTextColor(textColor);
				return;
			}
		}
		}
		throw new IndexOutOfBoundsException("page=" + page + " columnt=" + column);

	}

	public static abstract class DevConValueCallbackHander implements ValueCallbackHander {
		@Override
		public Date getDate(Object appInfo) {
			return ((AppStats) appInfo).getRequestDate();
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

	@Override
	protected View buildChart(Context context, Chart baseChart, List<?> statsForApp, int page,
			int column) throws IndexOutOfBoundsException {
		// Log.i(LOG_TAG,"buildChart p="+page+" c="+column);
		ValueCallbackHander handler = null;
		switch (ChartSet.values()[page]) {
		case DOWNLOADS: {
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

		case RATINGS: {
			switch (column) {
			case COL_AVG_RATING:

				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getAvgRating();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);

			case COL_RATINGS_1:

				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getRating1Diff();
					}
				};
				return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
						heighestRatingChange, lowestRatingChange);

			case COL_RATINGS_2:

				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getRating2Diff();
					}
				};
				return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
						heighestRatingChange, lowestRatingChange);
			case COL_RATINGS_3:

				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getRating3Diff();
					}
				};
				return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
						heighestRatingChange, lowestRatingChange);
			case COL_RATINGS_4:

				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getRating4Diff();
					}
				};
				return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
						heighestRatingChange, lowestRatingChange);
			case COL_RATINGS_5:

				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getRating5Diff();
					}
				};
				return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
						heighestRatingChange, lowestRatingChange);
			}

		}
			break;
		case REVENUE:
			handler = new DevConValueCallbackHander() {
				@Override
				public double getValue(Object appInfo) {
					AppStats stats = (AppStats) appInfo;
					return stats.getTotalRevenue() == null ? 0 : stats.getTotalRevenue();
				}
			};
			return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
		}

		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

	public void setHeighestRatingChange(Integer heighestRatingChange) {
		this.heighestRatingChange = heighestRatingChange;
	}

	public void setLowestRatingChange(Integer lowestRatingChange) {
		this.lowestRatingChange = lowestRatingChange;
	}

	@Override
	public String getSubHeadLine(int page, int column) throws IndexOutOfBoundsException {
		if (column == COL_DATE) {
			return "";
		}
		switch (ChartSet.values()[page]) {
		case DOWNLOADS: {
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
		case RATINGS: {
			switch (column) {
			case COL_AVG_RATING:
				return overallStats.getAvgRatingString();

			case COL_RATINGS_1:
				return Utils.safeToString(overallStats.getRating1());
			case COL_RATINGS_2:
				return Utils.safeToString(overallStats.getRating2());
			case COL_RATINGS_3:
				return Utils.safeToString(overallStats.getRating3());
			case COL_RATINGS_4:
				return Utils.safeToString(overallStats.getRating4());
			case COL_RATINGS_5:
				Preferences.saveShowChartHint(activity, false);
				return Utils.safeToString(overallStats.getRating5());
			}
		}
		case REVENUE:
			Preferences.saveShowChartHint(activity, false);
			if (overallStats == null) {
				return "";
			}

			return "Total "
					+ (overallStats.getTotalRevenue() == null ? "unknown" : overallStats
							.getTotalRevenue().toString());
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

}
