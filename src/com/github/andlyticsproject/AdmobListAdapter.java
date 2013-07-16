package com.github.andlyticsproject;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.github.andlyticsproject.chart.Chart;
import com.github.andlyticsproject.chart.Chart.ValueCallbackHander;
import com.github.andlyticsproject.model.AdmobStats;

@SuppressLint("SimpleDateFormat")
public class AdmobListAdapter extends BaseChartListAdapter {

	private static final int COL_DATE = 0;
	private static final int COL_REVENUE = 1;
	private static final int COL_EPC = 2;
	private static final int COL_REQUESTS = 3;
	private static final int COL_CLICKS = 4;
	private static final int COL_FILL_RATE = 5;
	private static final int COL_ECPM = 1;
	private static final int COL_IMPRESSIONS = 2;
	private static final int COL_CTR = 3;
	private static final int COL_HOUSEAD_CLICKS = 4;

	private NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);
	private List<AdmobStats> stats;

	//	private LayoutInflater layoutInflater;

	private Activity activity;

	//	private Object currentChart;

	private SimpleDateFormat dateFormat;

	//	private List<AdmobChartType> secondPageCharts;

	private AdmobStats overallStats;

	public AdmobListAdapter(Activity activity) {
		super(activity);
		this.stats = new ArrayList<AdmobStats>();
		//		this.layoutInflater = activity.getLayoutInflater();
		this.activity = activity;
		this.dateFormat = new SimpleDateFormat(Preferences.getDateFormatStringShort(activity));
	}

	@Override
	public int getCount() {
		return stats.size();
	}

	@Override
	public AdmobStats getItem(int position) {
		return stats.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	/*
	 * public Object getCurrentChart() {
	 * return currentChart;
	 * }
	 * 
	 * public void setCurrentChart(Object currentChart) {
	 * this.currentChart = currentChart;
	 * }
	 */

	public void setOverallStats(AdmobStats overallStats) {
		this.overallStats = overallStats;
	}

	@Override
	public void notifyDataSetChanged() {
		this.dateFormat = new SimpleDateFormat(Preferences.getDateFormatStringShort(activity));
		super.notifyDataSetChanged();
	}

	public void setStats(List<AdmobStats> stats) {
		this.stats = stats;
	}

	public List<AdmobStats> getStats() {
		return stats;
	}

	/*
	 * public void setSecondPageCharts(List<AdmobChartType> secondPageCharts) {
	 * this.secondPageCharts = secondPageCharts;
	 * }
	 * 
	 * public List<AdmobChartType> getSecondPageCharts() {
	 * return secondPageCharts;
	 * }
	 */

	@Override
	public int getNumPages() {
		return 2;
	}

	@Override
	public int getNumCharts(int page) throws IndexOutOfBoundsException {
		switch (page) {
		case 0:
			return 6;
		case 1:
			return 5;
		default:
			throw new IndexOutOfBoundsException("page=" + page);
		}
	}

	@Override
	public String getChartTitle(int page, int column) throws IndexOutOfBoundsException {
		if (column == COL_DATE)
			return "";

		switch (page) {
		case 0: {
			switch (column) {
			case COL_REVENUE:
				return activity.getString(R.string.admob__revenue);
			case COL_EPC:
				return activity.getString(R.string.admob__epc);
			case COL_REQUESTS:
				return activity.getString(R.string.admob__requests);
			case COL_CLICKS:
				return activity.getString(R.string.admob__clicks);
			case COL_FILL_RATE:
				return activity.getString(R.string.admob__fill_rate);

			}
		}
		case 1: {
			switch (column) {
			case COL_ECPM:
				return activity.getString(R.string.admob__eCPM);
			case COL_IMPRESSIONS:
				return activity.getString(R.string.admob__impressions);
			case COL_CTR:
				return activity.getString(R.string.admob__CTR);
			case COL_HOUSEAD_CLICKS:
				return activity.getString(R.string.admob__house_ad_clicks);

			}
		}
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

	@Override
	public void updateChartValue(int position, int page, int column, TextView tv)
			throws IndexOutOfBoundsException {
		AdmobStats admob = getItem(position);
		if (column == COL_DATE) {
			tv.setText(dateFormat.format(admob.getDate()));
			return;
		}

		switch (page) {
		case 0: {
			switch (column) {
			case COL_REVENUE:
				tv.setText(numberFormat.format(admob.getRevenue()));
				return;
			case COL_EPC:
				tv.setText(admob.getEpcCents());
				return;
			case COL_REQUESTS:
				tv.setText(admob.getRequests() + "");
				return;
			case COL_CLICKS:
				tv.setText(admob.getClicks() + "");
				return;
			case COL_FILL_RATE:
				BigDecimal fillrate = new BigDecimal(admob.getFillRate() * 100);
				fillrate = fillrate.setScale(2, BigDecimal.ROUND_HALF_EVEN);
				tv.setText(fillrate.toPlainString() + "%");
				return;

			}
		}
		case 1: {
			switch (column) {
			case COL_ECPM:
				tv.setText(numberFormat.format(admob.getEcpm()));
				return;
			case COL_IMPRESSIONS:
				tv.setText(admob.getImpressions() + "");
				return;
			case COL_CTR:
				BigDecimal ctr = new BigDecimal(admob.getCtr() * 100);
				ctr = ctr.setScale(2, BigDecimal.ROUND_HALF_EVEN);
				tv.setText(ctr.toPlainString() + "%");
				return;
			case COL_HOUSEAD_CLICKS:
				tv.setText(admob.getHouseAdClicks() + "");
				return;

			}
		}
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
			case COL_REVENUE:

				handler = new AdmobValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AdmobStats) appInfo).getRevenue();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
			case COL_EPC:

				handler = new AdmobValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AdmobStats) appInfo).getEpc();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
			case COL_REQUESTS:
				handler = new AdmobValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AdmobStats) appInfo).getRequests();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
			case COL_CLICKS:

				handler = new AdmobValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AdmobStats) appInfo).getClicks();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
			case COL_FILL_RATE:
				handler = new AdmobValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AdmobStats) appInfo).getFillRate();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);

			}
		}
		case 1: {
			switch (column) {
			case COL_ECPM:
				handler = new AdmobValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AdmobStats) appInfo).getEcpm();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
			case COL_IMPRESSIONS:

				handler = new AdmobValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AdmobStats) appInfo).getImpressions();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
			case COL_CTR:
				handler = new AdmobValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AdmobStats) appInfo).getCtr();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
			case COL_HOUSEAD_CLICKS:
				handler = new AdmobValueCallbackHander() {
					@Override
					public double getValue(Object appInfo) {
						return ((AdmobStats) appInfo).getHouseAdClicks();
					}
				};
				return baseChart.buildLineChart(context, statsForApp.toArray(), handler);

			}
		}
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

	public abstract class AdmobValueCallbackHander implements ValueCallbackHander {
		@Override
		public Date getDate(Object appInfo) {
			return ((AdmobStats) appInfo).getDate();
		}

		@Override
		public boolean isHeilightValue(Object current, Object previouse) {

			return false;
		}
	}

	@Override
	public String getSubHeadLine(int page, int column) throws IndexOutOfBoundsException {
		if (column == COL_DATE) {
			return "";
		}

		switch (page) {
		case 0: {
			switch (column) {
			case COL_REVENUE:
				return (overallStats != null) ? numberFormat.format(overallStats.getRevenue()) : "";

			case COL_EPC:
				return (overallStats != null) ? overallStats.getEpcCents() : "";

			case COL_REQUESTS:
				return (overallStats != null) ? overallStats.getRequests() + "" : "";

			case COL_CLICKS:
				return (overallStats != null) ? overallStats.getClicks() + "" : "";

			case COL_FILL_RATE:
				return (overallStats != null) ? (new BigDecimal(overallStats.getFillRate() * 100))
						.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "%" : "";
			}
		}
			break;
		case 1: {
			switch (column) {
			case COL_ECPM:
				return (overallStats != null) ? numberFormat.format(overallStats.getEcpm()) : "";

			case COL_IMPRESSIONS:
				return (overallStats != null) ? overallStats.getImpressions() + "" : "";

			case COL_CTR:
				return (overallStats != null) ? (new BigDecimal(overallStats.getCtr() * 100))
						.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "%" : "";

			case COL_HOUSEAD_CLICKS:
				return (overallStats != null) ? overallStats.getHouseAdClicks() + "" : "";
			}
		}
			break;
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

	@Override
	protected boolean isSmothValue(int page, int position) {
		return false;
	}

	@Override
	protected boolean useSmothColumn(int page) {
		return false;
	}

}
