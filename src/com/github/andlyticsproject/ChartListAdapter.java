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

public class ChartListAdapter extends BaseChartListAdapter {
	// private static String LOG_TAG=ChartListAdapter.class.toString();

	private static int BLACK_TEXT;

	private static final int RED_TEXT = Color.RED;

	private static final int DATE = 0;
	private static final int TOTAL_DOWNLAODS = 1;
	private static final int ACTIVE_INSTALLS_TOTAL = 2;
	private static final int TOTAL_DOWNLAODS_BY_DAY = 3;
	private static final int ACTIVE_INSTALLS_PERCENT = 4;

	private static final int AVG_RATING = 1;
	private static final int RATINGS_5 = 2;
	private static final int RATINGS_4 = 3;
	private static final int RATINGS_3 = 4;
	private static final int RATINGS_2 = 5;
	private static final int RATINGS_1 = 6;

	private Integer heighestRatingChange;
	private Integer lowestRatingChange;

	private List<AppStats> downloadInfos;

	private List<Date> versionUpdateDates;

	private Activity activity;

	private SimpleDateFormat dateFormat;
	private AppStats overallStats;

	public ChartListAdapter(BaseChartActivity activity) {
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
		return 2;
	}

	@Override
	public int getNumCharts(int page) throws IndexOutOfBoundsException {
		switch (ChartSet.values()[page]) {
			case DOWNLOADS:
				return 5;
			case RATINGS:
				return 7;
		}
		throw new IndexOutOfBoundsException("page=" + page);
	}

	public void setOverallStats(AppStats overallStats) {
		this.overallStats = overallStats;
	}

	@Override
	public String getChartTitle(int page, int column) throws IndexOutOfBoundsException {
		if (column == DATE) {
			return "";
		}
		switch (ChartSet.values()[page]) {
			case DOWNLOADS: {
				switch (column) {
					case TOTAL_DOWNLAODS:
						return activity.getString(R.string.total_downloads);

					case TOTAL_DOWNLAODS_BY_DAY:
						return activity.getString(R.string.downloads_day);

					case ACTIVE_INSTALLS_PERCENT:
						return activity.getString(R.string.active_installs_percent);

					case ACTIVE_INSTALLS_TOTAL:
						return activity.getString(R.string.active_installs);
				}
			}
				break;
			case RATINGS: {
				switch (column) {
					case AVG_RATING:
						return activity.getString(R.string.average_rating);

					case RATINGS_1:
						return "1* " + activity.getString(R.string.num_ratings);
					case RATINGS_2:
						return "2* " + activity.getString(R.string.num_ratings);
					case RATINGS_3:
						return "3* " + activity.getString(R.string.num_ratings);
					case RATINGS_4:
						return "4* " + activity.getString(R.string.num_ratings);
					case RATINGS_5:
						return "5* " + activity.getString(R.string.num_ratings);
				}

			}
				break;
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
		if (column == DATE) {
			tv.setText(dateFormat.format(appInfo.getRequestDate()));
			return;
		}
		int textColor = versionUpdateDates.contains(appInfo.getRequestDate()) ? RED_TEXT
				: BLACK_TEXT;
		switch (ChartSet.values()[page]) {
			case DOWNLOADS: {

				switch (column) {
					case TOTAL_DOWNLAODS:
						tv.setText(appInfo.getTotalDownloads() + "");
						tv.setTextColor(textColor);
						return;
					case ACTIVE_INSTALLS_TOTAL:
						tv.setText(appInfo.getActiveInstalls() + "");
						tv.setTextColor(textColor);
						return;
					case TOTAL_DOWNLAODS_BY_DAY:
						tv.setText(appInfo.getDailyDownloads() + "");
						tv.setTextColor(textColor);
						return;
					case ACTIVE_INSTALLS_PERCENT:
						tv.setText(appInfo.getActiveInstallsPercentString());
						tv.setTextColor(textColor);
						return;
				}
			}
				break;
			case RATINGS: {

				switch (column) {
					case AVG_RATING:
						tv.setText(appInfo.getAvgRatingString());
						tv.setTextColor(textColor);
						return;
					case RATINGS_5:
						if (appInfo.getRating5Diff() > 0) {
							tv.setText("+" + appInfo.getRating5Diff());
						} else {
							tv.setText(appInfo.getRating5Diff() + "");
						}
						tv.setTextColor(textColor);
						return;
					case RATINGS_4:
						if (appInfo.getRating4Diff() > 0) {
							tv.setText("+" + appInfo.getRating4Diff());
						} else {
							tv.setText(appInfo.getRating4Diff() + "");
						}
						tv.setTextColor(textColor);
						return;
					case RATINGS_3:
						if (appInfo.getRating3Diff() > 0) {
							tv.setText("+" + appInfo.getRating3Diff());
						} else {
							tv.setText(appInfo.getRating3Diff() + "");
						}
						tv.setTextColor(textColor);
						return;
					case RATINGS_2:
						if (appInfo.getRating2Diff() > 0) {
							tv.setText("+" + appInfo.getRating2Diff());
						} else {
							tv.setText(appInfo.getRating2Diff() + "");
						}
						tv.setTextColor(textColor);
						return;
					case RATINGS_1:
						if (appInfo.getRating1Diff() > 0) {
							tv.setText("+" + appInfo.getRating1Diff());
						} else {
							tv.setText(appInfo.getRating1Diff() + "");
						}
						tv.setTextColor(textColor);
						return;
				}

			}
				break;
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
					case TOTAL_DOWNLAODS:

						handler = new DevConValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((AppStats) appInfo).getTotalDownloads();
							}
						};
						return baseChart.buildLineChart(context, statsForApp.toArray(), handler);

					case TOTAL_DOWNLAODS_BY_DAY:

						handler = new DevConValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((AppStats) appInfo).getDailyDownloads();
							}
						};
						return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
								Integer.MIN_VALUE, 0);

					case ACTIVE_INSTALLS_TOTAL:
						handler = new DevConValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((AppStats) appInfo).getActiveInstalls();
							}
						};
						return baseChart.buildLineChart(context, statsForApp.toArray(), handler);

					case ACTIVE_INSTALLS_PERCENT:
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
					case AVG_RATING:

						handler = new DevConValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((AppStats) appInfo).getAvgRating();
							}
						};
						return baseChart.buildLineChart(context, statsForApp.toArray(), handler);

					case RATINGS_1:

						handler = new DevConValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((AppStats) appInfo).getRating1Diff();
							}
						};
						return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
								heighestRatingChange, lowestRatingChange);

					case RATINGS_2:

						handler = new DevConValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((AppStats) appInfo).getRating2Diff();
							}
						};
						return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
								heighestRatingChange, lowestRatingChange);
					case RATINGS_3:

						handler = new DevConValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((AppStats) appInfo).getRating3Diff();
							}
						};
						return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
								heighestRatingChange, lowestRatingChange);
					case RATINGS_4:

						handler = new DevConValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((AppStats) appInfo).getRating4Diff();
							}
						};
						return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
								heighestRatingChange, lowestRatingChange);
					case RATINGS_5:

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
		if (column == DATE) {
			return "";
		}
		switch (ChartSet.values()[page]) {
			case DOWNLOADS: {
				switch (column) {
					case TOTAL_DOWNLAODS:
						return (overallStats != null) ? overallStats.getTotalDownloads() + "" : "";

					case TOTAL_DOWNLAODS_BY_DAY:
						return overallStats.getDailyDownloads() + "";

					case ACTIVE_INSTALLS_PERCENT:
						return overallStats.getActiveInstallsPercentString() + "%";

					case ACTIVE_INSTALLS_TOTAL:
						Preferences.saveShowChartHint(activity, false);
						return overallStats.getActiveInstalls() + "";
				}
			}
				break;
			case RATINGS: {
				switch (column) {
					case AVG_RATING:
						return overallStats.getAvgRatingString() + "";

					case RATINGS_1:
						return overallStats.getRating1() + "";
					case RATINGS_2:
						return overallStats.getRating2() + "";
					case RATINGS_3:
						return overallStats.getRating3() + "";
					case RATINGS_4:
						return overallStats.getRating4() + "";
					case RATINGS_5:
						Preferences.saveShowChartHint(activity, false);
						return overallStats.getRating5() + "";
				}
			}
		}
		throw new IndexOutOfBoundsException("page=" + page + " columnt=" + column);
	}

}
