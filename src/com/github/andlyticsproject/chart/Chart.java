package com.github.andlyticsproject.chart;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer.Orientation;
import org.achartengine.renderer.XYSeriesRenderer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;

import com.github.andlyticsproject.Preferences;
import com.github.andlyticsproject.R;

public class Chart extends AbstractChart {

	private static final String TAG = Chart.class.getSimpleName();
	private static final boolean DEBUG = false;

	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public enum ChartSet {
		RATINGS, DOWNLOADS, REVENUE, ADMOB
	}

	/*
	 * public enum AdmobChartType { REVENUE, EPC,REQUESTS, CLICKS, FILL_RATE,
	 * ECPM, IMPRESSIONS, CTR, HOUSEAD_CLICKS }
	 * 
	 * public enum DownloadChartType { TOTAL_DOWNLAODS, ACTIVE_INSTALLS_TOTAL,
	 * TOTAL_DOWNLAODS_BY_DAY, ACTIVE_INSTALLS_PERCENT }
	 * 
	 * public enum RatingChartType { AVG_RATING, RATINGS_5, RATINGS_4,
	 * RATINGS_3, RATINGS_2, RATINGS_1 }
	 */
	public interface ValueCallbackHander {
		double getValue(Object appInfo);

		Date getDate(Object appInfo);

		boolean isHeilightValue(Object appInfo, Object object);
	}

	private static final int MAX_BAR_VALUES = Integer.MAX_VALUE;

	@SuppressLint("SimpleDateFormat")
	public View buildBarChart(Context context, Object[] appstats, ValueCallbackHander handler,
			double heighestValue, double lowestValue) {

		String[] titles = new String[] { "" };

		List<Object> statsForApp = Arrays.asList(appstats);
		if (statsForApp.size() > MAX_BAR_VALUES) {
			statsForApp = statsForApp.subList(statsForApp.size() - MAX_BAR_VALUES,
					statsForApp.size());
		}

		// styling
		int[] colors = new int[] { context.getResources().getColor(R.color.lightBlue) };
		XYMultipleSeriesRenderer renderer = buildBarRenderer(colors);
		renderer.setOrientation(Orientation.HORIZONTAL);

		// get x values (dates) at least 10
		List<String> dates = new ArrayList<String>();

		int xLabelDistance = 0;
		if (statsForApp.size() > 0) {
			xLabelDistance = statsForApp.size() / 6;
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				Preferences.getDateFormatStringShort(context));
		int nextXLabelPrint = 1;
		for (int i = 1; i < statsForApp.size(); i++) {
			Object appInfo = statsForApp.get(i);
			dates.add(getDateString(handler.getDate(appInfo)));

			if (i == nextXLabelPrint) {
				renderer.addXTextLabel(i, dateFormat.format(handler.getDate(appInfo)));
				nextXLabelPrint += xLabelDistance;
			}
		}

		double[] valuesArray = new double[dates.size()];

		for (int i = 1; i < statsForApp.size(); i++) {
			Object appInfoPrev = statsForApp.get(i);
			double value = handler.getValue(appInfoPrev);
			valuesArray[i - 1] = value;

			if (value > heighestValue) {
				heighestValue = value;
			}
			if (value < lowestValue) {
				lowestValue = value;
			}

		}

		List<double[]> values = new ArrayList<double[]>();

		values.add(valuesArray);
		// values.add(activeArray);

		// long dateDistance = datesArray[datesArray.length - 1].getTime() -
		// datesArray[0].getTime();
		// dateDistance = (long) (dateDistance * .1f);

		double valueDistance = heighestValue - lowestValue;
		double valueDistanceTop = heighestValue + (valueDistance * .2f);
		double valueDistanceBottom = lowestValue - (valueDistance * .1f);

		if (heighestValue == lowestValue) {

			valueDistanceTop = lowestValue + (lowestValue / 2);
			valueDistanceBottom = lowestValue / 2;
		}

		// settings
		setChartSettings(context.getResources(), renderer, "", "", "", 0, statsForApp.size(),
				valueDistanceBottom, valueDistanceTop, Color.LTGRAY, Color.BLACK);

		renderer.setYLabels(7);
		renderer.setXLabels(-1);
		renderer.setShowLegend(false);
		renderer.setShowAxes(false);
		renderer.setShowGrid(true);
		renderer.setAntialiasing(true);

		return ChartFactory.getBarChartView(context, buildBarDataset(titles, values), renderer,
				Type.DEFAULT);

	}

	public View buildLineChart(Context context, Object[] stats, ValueCallbackHander handler) {

		String[] titles = new String[] { "" };

		List<Object> statsForApp = Arrays.asList(stats);

		// get x values (dates) at least 10
		List<Date> dates = new ArrayList<Date>();
		List<Date> highlightDates = new ArrayList<Date>();

		for (int i = 0; i < statsForApp.size(); i++) {
			Object appInfo = statsForApp.get(i);
			Calendar calendar = Calendar.getInstance(); 
			calendar.setTime(handler.getDate(appInfo));
			calendar.set(Calendar.HOUR_OF_DAY, 0);			
			dates.add(calendar.getTime());
			if (i > 0) {
				boolean highlight = handler.isHeilightValue(appInfo, statsForApp.get(i - 1));
				if (highlight) {
					highlightDates.add(calendar.getTime());
				}
			}
		}

		Date[] datesArray = dates.toArray(new Date[dates.size()]);
		double[] valuesArray = new double[dates.size()];

		double[] highlightValuesArray = new double[highlightDates.size()];

		// This could break something, but we don't (?) use negative 
		// stats, so should be OK. Passing Double.MIN_VALUE to achartengine 
		// crashes Android, so not a good idea to use it. Cf. #464
		double highestValue = 0;//Double.MIN_VALUE;
		double lowestValue = Double.MAX_VALUE;

		for (int i = 0; i < statsForApp.size(); i++) {
			Object appInfo = statsForApp.get(i);
			double value = handler.getValue(appInfo);
			valuesArray[i] = value;

			if (value > highestValue) {
				highestValue = value;
			}
			if (value < lowestValue) {
				lowestValue = value;
			}

			int indexOf = highlightDates.indexOf(handler.getDate(appInfo));
			if (indexOf > -1) {
				highlightValuesArray[indexOf] = value;
			}

		}

		if (DEBUG) {
			for (int i = 0; i < datesArray.length; i++) {
				Log.d(TAG, String.format("%s->%f", datesArray[i].toString(), valuesArray[i]));
			}
			Log.d(TAG, "*********************************************************");
			Log.d(TAG, String.format("high=%f, low=%f", highestValue, lowestValue));
			Log.d(TAG, "*********************************************************");
		}

		List<Date[]> dateArrayList = new ArrayList<Date[]>();
		dateArrayList.add(datesArray);
		// dateArrayList.add(datesArray);

		List<double[]> values = new ArrayList<double[]>();

		values.add(valuesArray);
		// values.add(activeArray);

		// styling
		int[] colors = new int[] { context.getResources().getColor(R.color.lightBlue) };
		PointStyle pointStye = valuesArray.length > 30 ? PointStyle.POINT : PointStyle.CIRCLE;
		PointStyle[] styles = new PointStyle[] { pointStye };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
		int length = renderer.getSeriesRendererCount();
		for (int i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}

		long dateDistance = datesArray[datesArray.length - 1].getTime() - datesArray[0].getTime();
		dateDistance = (long) (dateDistance * .1f);

		double valueDistance = highestValue - lowestValue;
		double valueDistanceTop = highestValue + (valueDistance * .2f);
		double valueDistanceBottom = lowestValue - (valueDistance * .1f);

		if (highestValue == lowestValue) {

			valueDistanceTop = lowestValue + (lowestValue / 2);
			valueDistanceBottom = lowestValue / 2;
		}

		// settings
		setChartSettings(context.getResources(), renderer, "", "", "", datesArray[0].getTime()
				- dateDistance, datesArray[datesArray.length - 1].getTime() + dateDistance,
				valueDistanceBottom, valueDistanceTop, Color.LTGRAY, Color.BLACK);

		renderer.setYLabels(5);
		renderer.setXLabels(10);
		renderer.setShowLegend(false);
		renderer.setShowAxes(false);
		renderer.setShowGrid(true);
		renderer.setAntialiasing(true);

		return ChartFactory.getTimeChartView(context,
				buildDateDataset(titles, dateArrayList, values), renderer,
				Preferences.getDateFormatStringShort(context));

	}

	public String getDateString(Date date) {
		return dateFormat.format(date);
	}
}
