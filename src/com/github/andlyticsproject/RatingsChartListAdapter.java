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

public class RatingsChartListAdapter extends ChartListAdapter<AppStats> {

	private static final int COL_AVG_RATING = 1;
	private static final int COL_RATINGS_5 = 2;
	private static final int COL_RATINGS_4 = 3;
	private static final int COL_RATINGS_3 = 4;
	private static final int COL_RATINGS_2 = 5;
	private static final int COL_RATINGS_1 = 6;

	private Integer highestRatingChange;
	private Integer lowestRatingChange;

	public RatingsChartListAdapter(Activity activity) {
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
			return 7;
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
						highestRatingChange, lowestRatingChange);

			case COL_RATINGS_2:

				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getRating2Diff();
					}
				};
				return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
						highestRatingChange, lowestRatingChange);
			case COL_RATINGS_3:

				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getRating3Diff();
					}
				};
				return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
						highestRatingChange, lowestRatingChange);
			case COL_RATINGS_4:

				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getRating4Diff();
					}
				};
				return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
						highestRatingChange, lowestRatingChange);
			case COL_RATINGS_5:

				handler = new DevConValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AppStats) appInfo).getRating5Diff();
					}
				};
				return baseChart.buildBarChart(context, statsForApp.toArray(), handler,
						highestRatingChange, lowestRatingChange);
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

	public void setHighestRatingChange(Integer highestRatingChange) {
		this.highestRatingChange = highestRatingChange;
	}

	public void setLowestRatingChange(Integer lowestRatingChange) {
		this.lowestRatingChange = lowestRatingChange;
	}

}
