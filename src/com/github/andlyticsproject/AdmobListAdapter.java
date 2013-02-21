
package com.github.andlyticsproject;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.github.andlyticsproject.chart.Chart;
import com.github.andlyticsproject.chart.Chart.ValueCallbackHander;
import com.github.andlyticsproject.model.Admob;

@SuppressLint("SimpleDateFormat")
public class AdmobListAdapter extends BaseChartListAdapter {
	private static final int DATE = 0;
	private static final int REVENUE = 1;
	private static final int EPC = 2;
	private static final int REQUESTS = 3;
	private static final int CLICKS = 4;
	private static final int FILL_RATE = 5;
	private static final int ECPM = 1;
	private static final int IMPRESSIONS = 2;
	private static final int CTR = 3;
	private static final int HOUSEAD_CLICKS = 4;

	private NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);
	private List<Admob> stats;

	//	private LayoutInflater layoutInflater;

	private BaseActivity activity;

	//	private Object currentChart;

	private SimpleDateFormat dateFormat;

	//	private List<AdmobChartType> secondPageCharts;

	private Admob overallStats;

	public AdmobListAdapter(BaseChartActivity activity) {
		super(activity);
		this.stats = new ArrayList<Admob>();
		//		this.layoutInflater = activity.getLayoutInflater();
		this.activity = activity;
		this.dateFormat = new SimpleDateFormat(Preferences.getDateFormatStringShort(activity));
	}

	@Override
	public int getCount() {
		return stats.size();
	}

	@Override
	public Admob getItem(int position) {
		return stats.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	/*public Object getCurrentChart() {
		return currentChart;
	}

	public void setCurrentChart(Object currentChart) {
		this.currentChart = currentChart;
	}*/

	public void setOverallStats(Admob overallStats) {
		this.overallStats = overallStats;
	}

	@Override
	public void notifyDataSetChanged() {
		this.dateFormat = new SimpleDateFormat(Preferences.getDateFormatStringShort(activity));
		super.notifyDataSetChanged();
	}

	public void setStats(List<Admob> stats) {
		this.stats = stats;
	}

	public List<Admob> getStats() {
		return stats;
	}

	/*    public void setSecondPageCharts(List<AdmobChartType> secondPageCharts) {
	        this.secondPageCharts = secondPageCharts;
	    }

	    public List<AdmobChartType> getSecondPageCharts() {
	        return secondPageCharts;
	    }*/

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
			// XXX Workaround for #306 -- don't show House ad click graph
			//				return 5;
			return 4;
			default:
				throw new IndexOutOfBoundsException("page=" + page);
		}
	}

	@Override
	public String getChartTitle(int page, int column) throws IndexOutOfBoundsException {
		if (column == DATE)
			return "";

		switch (page) {
			case 0: {
				switch (column) {
					case REVENUE:
						return activity.getString(R.string.admob__revenue);
					case EPC:
						return activity.getString(R.string.admob__epc);
					case REQUESTS:
						return activity.getString(R.string.admob__requests);
					case CLICKS:
						return activity.getString(R.string.admob__clicks);
					case FILL_RATE:
						return activity.getString(R.string.admob__fill_rate);

				}
			}
			case 1: {
				switch (column) {
					case ECPM:
						return activity.getString(R.string.admob__eCPM);
					case IMPRESSIONS:
						return activity.getString(R.string.admob__impressions);
					case CTR:
						return activity.getString(R.string.admob__CTR);
					case HOUSEAD_CLICKS:
						return activity.getString(R.string.admob__house_ad_clicks);

				}
			}
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

	@Override
	public void updateChartValue(int position, int page, int column, TextView tv)
			throws IndexOutOfBoundsException {
		Admob admob = getItem(position);
		if (column == DATE) {
			tv.setText(dateFormat.format(admob.getDate()));
			return;
		}

		switch (page) {
			case 0: {
				switch (column) {
					case REVENUE:
						tv.setText(numberFormat.format(admob.getRevenue()));
						return;
					case EPC:
						tv.setText(admob.getEpcCents());
						return;
					case REQUESTS:
						tv.setText(admob.getRequests() + "");
						return;
					case CLICKS:
						tv.setText(admob.getClicks() + "");
						return;
					case FILL_RATE:
						BigDecimal fillrate = new BigDecimal(admob.getFillRate() * 100);
						fillrate = fillrate.setScale(2, BigDecimal.ROUND_HALF_EVEN);
						tv.setText(fillrate.toPlainString() + "%");
						return;

				}
			}
			case 1: {
				switch (column) {
					case ECPM:
						tv.setText(numberFormat.format(admob.getEcpm()));
						return;
					case IMPRESSIONS:
						tv.setText(admob.getImpressions() + "");
						return;
					case CTR:
						BigDecimal ctr = new BigDecimal(admob.getCtr() * 100);
						ctr = ctr.setScale(2, BigDecimal.ROUND_HALF_EVEN);
						tv.setText(ctr.toPlainString() + "%");
						return;
					case HOUSEAD_CLICKS:
						tv.setText(admob.getHouseAdClicks() + "");
						return;

				}
			}
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

	@Override
	protected View buildChart(Context context, Chart baseChart, List<?> statsForApp, int page,
			int column) throws IndexOutOfBoundsException {
		ValueCallbackHander handler = null;

		switch (page) {
			case 0: {
				switch (column) {
					case REVENUE:

						handler = new AdmobValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((Admob) appInfo).getRevenue();
							}
						};
						return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
					case EPC:

						handler = new AdmobValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((Admob) appInfo).getEpc();
							}
						};
						return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
					case REQUESTS:
						handler = new AdmobValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((Admob) appInfo).getRequests();
							}
						};
						return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
					case CLICKS:

						handler = new AdmobValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((Admob) appInfo).getClicks();
							}
						};
						return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
					case FILL_RATE:
						handler = new AdmobValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((Admob) appInfo).getFillRate();
							}
						};
						return baseChart.buildLineChart(context, statsForApp.toArray(), handler);

				}
			}
			case 1: {
				switch (column) {
					case ECPM:
						handler = new AdmobValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((Admob) appInfo).getEcpm();
							}
						};
						return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
					case IMPRESSIONS:

						handler = new AdmobValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((Admob) appInfo).getImpressions();
							}
						};
						return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
					case CTR:
						handler = new AdmobValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((Admob) appInfo).getCtr();
							}
						};
						return baseChart.buildLineChart(context, statsForApp.toArray(), handler);
					case HOUSEAD_CLICKS:
						handler = new AdmobValueCallbackHander() {
							@Override
							public double getValue(Object appInfo) {
								return ((Admob) appInfo).getHouseAdClicks();
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
			return ((Admob) appInfo).getDate();
		}

		@Override
		public boolean isHeilightValue(Object current, Object previouse) {

			return false;
		}
	}

	@Override
	public String getSubHeadLine(int page, int column) throws IndexOutOfBoundsException {
		if (column == DATE) {
			return "";
		}

		switch (page) {
			case 0: {
				switch (column) {
					case REVENUE:
						return (overallStats != null) ? numberFormat.format(overallStats
								.getRevenue()) : "";

					case EPC:
						return (overallStats != null) ? overallStats.getEpcCents() : "";

					case REQUESTS:
						return (overallStats != null) ? overallStats.getRequests() + "" : "";

					case CLICKS:
						return (overallStats != null) ? overallStats.getClicks() + "" : "";

					case FILL_RATE:
						return (overallStats != null) ? (new BigDecimal(
								overallStats.getFillRate() * 100)).setScale(2,
								BigDecimal.ROUND_HALF_UP).toPlainString()
								+ "%" : "";
				}
			}
				break;
			case 1: {
				switch (column) {
					case ECPM:
						return (overallStats != null) ? numberFormat.format(overallStats.getEcpm())
								: "";

					case IMPRESSIONS:
						return (overallStats != null) ? overallStats.getImpressions() + "" : "";

					case CTR:
						return (overallStats != null) ? (new BigDecimal(overallStats.getCtr() * 100))
								.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "%"
								: "";

					case HOUSEAD_CLICKS:
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
