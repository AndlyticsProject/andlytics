
package de.betaapps.andlytics.chart;

import de.betaapps.andlytics.Preferences;
import de.betaapps.andlytics.model.Admob;
import de.betaapps.andlytics.model.AppStats;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer.Orientation;

public class Chart extends AbstractChart {

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
	
	public enum ChartSet {
		DOWNLOADS, RATINGS, ADMOB
	}
	
	public enum AdmobChartType {
        REVENUE, EPC,REQUESTS, CLICKS, FILL_RATE, ECPM, IMPRESSIONS, CTR, HOUSEAD_CLICKS
    }

	public enum DownloadChartType {
		TOTAL_DOWNLAODS, ACTIVE_INSTALLS_TOTAL, TOTAL_DOWNLAODS_BY_DAY, ACTIVE_INSTALLS_PERCENT  
	}
	
	public enum RatingChartType {
		AVG_RATING, RATINGS_5, RATINGS_4, RATINGS_3, RATINGS_2, RATINGS_1  
	}	

	public interface ValueCallbackHander {
		double getValue(Object appInfo);
		Date getDate(Object appInfo);
        boolean isHeilightValue(Object appInfo, Object object);
	} 
	
	public abstract class DevConValueCallbackHander implements ValueCallbackHander{
	    @Override
	    public Date getDate(Object appInfo) {
	        return ((AppStats)appInfo).getRequestDate();
	    }
	    

        @Override
        public boolean isHeilightValue(Object current, Object previouse) {

            if(previouse == null) {
                return false;
            }
            
            AppStats cstats = ((AppStats)current);
            
            if(cstats.getVersionCode() == 0) {
                return false;
            }
            
            if(cstats.getVersionCode() < ((AppStats)previouse).getVersionCode()) {
                return true;
            }
            
            return false;
        }
	}
	
   public abstract class AdmobValueCallbackHander implements ValueCallbackHander{
        @Override
        public Date getDate(Object appInfo) {
            return ((Admob)appInfo).getDate();
        }
        

        @Override
        public boolean isHeilightValue(Object current, Object previouse) {
   
            return false;
        }
    }

	private static final int MAX_BAR_VALUES = Integer.MAX_VALUE;

	public View buildBarChart(Context context, Object[] appstats, ValueCallbackHander handler, double heighestValue, double lowestValue) {

		String[] titles = new String[] { "" };
		
		List<Object> statsForApp = Arrays.asList(appstats);
		if(statsForApp.size() > MAX_BAR_VALUES) {
			statsForApp = statsForApp.subList(statsForApp.size() -MAX_BAR_VALUES, statsForApp.size());
		}
		
		// styling
		int[] colors = new int[] { Color.parseColor("#84b325") };
		XYMultipleSeriesRenderer renderer = buildBarRenderer(colors);
		renderer.setOrientation(Orientation.HORIZONTAL);

		// get x values (dates) at least 10
		List<String> dates = new ArrayList<String>();

		int xLabelDistance = 0;
		if(statsForApp.size() > 0) {
		    xLabelDistance = statsForApp.size() / 6;
		}
		int nextXLabelPrint  = 1;
		for (int i = 1; i < statsForApp.size(); i++) {
			Object appInfo = statsForApp.get(i);
			dates.add(getDateString(handler.getDate(appInfo)));
			
			if(i == nextXLabelPrint) {
				SimpleDateFormat dateFormat = new SimpleDateFormat(Preferences.getDateFormatShort(context));
				renderer.addTextLabel(i, dateFormat.format(handler.getDate(appInfo)));
				nextXLabelPrint += xLabelDistance;
			}
	    }

		double[] valuesArray = new double[dates.size()];

		for (int i = 1; i < statsForApp.size(); i++) {
			Object appInfoPrev = statsForApp.get(i);
			double value  = handler.getValue(appInfoPrev);
			valuesArray[i-1] = value;

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
		setChartSettings(renderer, "", "", "", 0, statsForApp.size(), valueDistanceBottom, valueDistanceTop, Color.LTGRAY, Color.BLACK);

		
		renderer.setYLabels(7);
		renderer.setXLabels(-1);
		renderer.setShowLegend(false);
		renderer.setShowAxes(false);
		renderer.setShowGrid(true);
		renderer.setAntialiasing(true);
		renderer.setLabelsTextSize(12);

		return ChartFactory.getBarChartView(context, buildBarDataset(titles, values), renderer, Type.DEFAULT);

	}

	public View buildLineChart(Context context, Object[] stats, ValueCallbackHander handler) {

		String[] titles = new String[] { "" };
		
		List<Object> statsForApp = Arrays.asList(stats);

		// get x values (dates) at least 10
		List<Date> dates = new ArrayList<Date>();
		List<Date> highlightDates = new ArrayList<Date>();

		for (int i = 0; i < statsForApp.size(); i++) {
			Object appInfo = statsForApp.get(i);
			Date date = handler.getDate(appInfo);
			dates.add(date);
			if(i > 0) {
			    boolean highlight = handler.isHeilightValue(appInfo, statsForApp.get(i-1));
			    if(highlight) {
			        highlightDates.add(date);
			    }
			}
		}

		Date[] datesArray = dates.toArray(new Date[dates.size()]);
		double[] valuesArray = new double[dates.size()];
		
	    Date[] highlightDatesArray = highlightDates.toArray(new Date[highlightDates.size()]);
        double[] highlightValuesArray = new double[highlightDates.size()];

		
		double heighestValue = Double.MIN_VALUE;
		double lowestValue = Double.MAX_VALUE;

		for (int i = 0; i < statsForApp.size(); i++) {
			Object appInfo = statsForApp.get(i);
			double value = handler.getValue(appInfo);
			valuesArray[i] = value;

			if (value > heighestValue) {
				heighestValue = value;
			}
			if (value < lowestValue) {
				lowestValue = value;
			}
			
			int indexOf = highlightDates.indexOf(handler.getDate(appInfo));
			if(indexOf > -1) {
			    highlightValuesArray[indexOf] = value;
			}

		}

		List<Date[]> dateArrayList = new ArrayList<Date[]>();
		dateArrayList.add(datesArray);
		// dateArrayList.add(datesArray);

		List<double[]> values = new ArrayList<double[]>();

		values.add(valuesArray);
		// values.add(activeArray);

		// styling
		int[] colors = new int[] { Color.parseColor("#84b325") };
		PointStyle pointStye = valuesArray.length > 30 ? PointStyle.POINT : PointStyle.CIRCLE;
		PointStyle[] styles = new PointStyle[] { pointStye };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
		int length = renderer.getSeriesRendererCount();
		for (int i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}

		long dateDistance = datesArray[datesArray.length - 1].getTime() - datesArray[0].getTime();
		dateDistance = (long) (dateDistance * .1f);

		double valueDistance = heighestValue - lowestValue;
		double valueDistanceTop = heighestValue + (valueDistance * .2f);
		double valueDistanceBottom = lowestValue - (valueDistance * .1f);

		if (heighestValue == lowestValue) {

			valueDistanceTop = lowestValue + (lowestValue / 2);
			valueDistanceBottom = lowestValue / 2;
		}

		// settings
		setChartSettings(renderer, "", "", "", datesArray[0].getTime() - dateDistance,
				datesArray[datesArray.length - 1].getTime() + dateDistance, valueDistanceBottom, valueDistanceTop,
				Color.LTGRAY, Color.BLACK);

		renderer.setYLabels(5);
		renderer.setXLabels(10);
		renderer.setShowLegend(false);
		renderer.setShowAxes(false);
		renderer.setShowGrid(true);
		renderer.setAntialiasing(true);
		renderer.setLabelsTextSize(12);

		return ChartFactory.getTimeChartView(context, buildDateDataset(titles, dateArrayList, values), renderer,
				Preferences.getDateFormatShort(context));

	}

	public View buildDownloadChart(Context context, List<AppStats> statsForApp, DownloadChartType chartType) {

		ValueCallbackHander handler = null;
		View result = null;

		switch (chartType) {
		case TOTAL_DOWNLAODS: 

			handler = new DevConValueCallbackHander() {
				@Override 
				public double getValue(Object appInfo) {
					return ((AppStats)appInfo).getTotalDownloads();
				}
			};
			result = buildLineChart(context, statsForApp.toArray(), handler);
			break;

		case TOTAL_DOWNLAODS_BY_DAY:

			handler = new DevConValueCallbackHander() {
				@Override 
				public double getValue(Object appInfo) {
					return ((AppStats)appInfo).getDailyDownloads();
				}
			};
			result = buildBarChart(context, statsForApp.toArray(), handler, Integer.MIN_VALUE, 0);
			break;
		
		case ACTIVE_INSTALLS_TOTAL:
			handler = new DevConValueCallbackHander() {
				@Override
				public double getValue(Object appInfo) {
					return ((AppStats)appInfo).getActiveInstalls();
				}
			};
			result = buildLineChart(context, statsForApp.toArray(), handler);
			break;

		case ACTIVE_INSTALLS_PERCENT:
			handler = new DevConValueCallbackHander() {
				@Override
				public double getValue(Object appInfo) {
					return ((AppStats)appInfo).getActiveInstallsPercent();
				}
			};
			result = buildLineChart(context, statsForApp.toArray(), handler);
			break;
		default:
			break;
		}

		return result;

	}
	

    public View buildRatingChart(Context context, List<AppStats> statsForApp, RatingChartType chartType, Integer heighestRatingChange, Integer lowestRatingChange) {

		ValueCallbackHander handler = null;
		View result = null;

		switch (chartType) {
		case AVG_RATING:

			handler = new DevConValueCallbackHander() {
				@Override
				public double getValue(Object appInfo) {
					return ((AppStats)appInfo).getAvgRating();
				}
			};
			result = buildLineChart(context, statsForApp.toArray(), handler);
			break;

		case RATINGS_1:

			handler = new DevConValueCallbackHander() {
				@Override
				public double getValue(Object appInfo) {
					return ((AppStats)appInfo).getRating1Diff();
				}
			};
			result = buildBarChart(context, statsForApp.toArray(), handler, heighestRatingChange, lowestRatingChange);
			break;
		
		case RATINGS_2:

			handler = new DevConValueCallbackHander() {
				@Override
				public double getValue(Object appInfo) {
					return ((AppStats)appInfo).getRating2Diff();
				}
			};
			result = buildBarChart(context, statsForApp.toArray(), handler, heighestRatingChange, lowestRatingChange);
			break;
		case RATINGS_3:

			handler = new DevConValueCallbackHander() {
				@Override
				public double getValue(Object appInfo) {
					return ((AppStats)appInfo).getRating3Diff();
				}
			};
			result = buildBarChart(context, statsForApp.toArray(), handler, heighestRatingChange, lowestRatingChange);
			break;
		case RATINGS_4:

			handler = new DevConValueCallbackHander() {
				@Override
				public double getValue(Object appInfo) {
					return ((AppStats)appInfo).getRating4Diff();
				}
			};
			result = buildBarChart(context, statsForApp.toArray(), handler, heighestRatingChange, lowestRatingChange);
			break;
		case RATINGS_5:

			handler = new DevConValueCallbackHander() {
				@Override
				public double getValue(Object appInfo) {
					return ((AppStats)appInfo).getRating5Diff();
				}
			};
			result = buildBarChart(context, statsForApp.toArray(), handler, heighestRatingChange, lowestRatingChange);
			break;
	
		default:
			break;
		}

		return result;

	}

    
    public String getDateString(Date date) {
        return dateFormat.format(date);
    }

    public View buildAdmobChart(Context context, List<Admob> statsForApp, AdmobChartType chartType) {
        ValueCallbackHander handler = null;
        View result = null;

        switch (chartType) {
        case REVENUE:

            handler = new AdmobValueCallbackHander() {
                @Override
                public double getValue(Object appInfo) {
                    return ((Admob)appInfo).getRevenue();
                }
            };
            result = buildLineChart(context, statsForApp.toArray(), handler);
            break;
          case EPC:

            handler = new AdmobValueCallbackHander() {
                @Override
                public double getValue(Object appInfo) {
                    return ((Admob)appInfo).getEpc();
                }
            };
            result = buildLineChart(context, statsForApp.toArray(), handler);
            break;

        case IMPRESSIONS:
            
            handler = new AdmobValueCallbackHander() {
                @Override
                public double getValue(Object appInfo) {
                    return ((Admob)appInfo).getImpressions();
                }
            };
            result = buildLineChart(context, statsForApp.toArray(), handler);
            break;
        
        case CLICKS:
            
            handler = new AdmobValueCallbackHander() {
                @Override
                public double getValue(Object appInfo) {
                    return ((Admob)appInfo).getClicks();
                }
            };
            result = buildLineChart(context, statsForApp.toArray(), handler);
            break;

        case CTR:
            handler = new AdmobValueCallbackHander() {
                @Override
                public double getValue(Object appInfo) {
                    return ((Admob)appInfo).getCtr();
                }
            };
            result = buildLineChart(context, statsForApp.toArray(), handler);
            break;
    
        case ECPM:
            handler = new AdmobValueCallbackHander() {
                @Override
                public double getValue(Object appInfo) {
                    return ((Admob)appInfo).getEcpm();
                }
            };
            result = buildLineChart(context, statsForApp.toArray(), handler);
            break;
        case FILL_RATE:
            handler = new AdmobValueCallbackHander() {
                @Override
                public double getValue(Object appInfo) {
                    return ((Admob)appInfo).getFillRate();
                }
            };
            result = buildLineChart(context, statsForApp.toArray(), handler);
            break;
        case HOUSEAD_CLICKS:
            handler = new AdmobValueCallbackHander() {
                @Override
                public double getValue(Object appInfo) {
                    return ((Admob)appInfo).getHouseAdClicks();
                }
            };
            result = buildLineChart(context, statsForApp.toArray(), handler);
            break;
        case REQUESTS:
            handler = new AdmobValueCallbackHander() {
                @Override
                public double getValue(Object appInfo) {
                    return ((Admob)appInfo).getRequests();
                }
            };
            result = buildLineChart(context, statsForApp.toArray(), handler);
            break;
        default:
            break;
        }

        return result;

    }

}
