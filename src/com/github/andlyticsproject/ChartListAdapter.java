package com.github.andlyticsproject;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.github.andlyticsproject.chart.Chart.DownloadChartType;
import com.github.andlyticsproject.chart.Chart.RatingChartType;
import com.github.andlyticsproject.model.AppStats;

public class ChartListAdapter extends BaseAdapter {

    private static final int BLACK_TEXT = Color.parseColor("#555555");

    private static final int RED_TEXT = Color.RED;

	private List<AppStats> downloadInfos;

	private List<Date> versionUpdateDates;

	private LayoutInflater layoutInflater;

	private Activity activity;

	private float scale;

	private Object currentChart;

	private SimpleDateFormat dateFormat;

	public ChartListAdapter(Activity activity) {
		this.setDownloadInfos(new ArrayList<AppStats>());
		this.layoutInflater = activity.getLayoutInflater();
		this.activity = activity;
		this.scale = activity.getResources().getDisplayMetrics().density;
		this.dateFormat = new SimpleDateFormat(Preferences.getDateFormatShort(activity));
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

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		Object baseHolder = null;

		AppStats appInfo = getItem(position);

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.chart_list_item, null);

            if(currentChart instanceof DownloadChartType) {

            	DownloadsViewHolder holder = new DownloadsViewHolder();
            	holder.date = createTextView("", false, false);
            	holder.smooth = createTextView("*", false, false);
            	holder.total = createTextView("", false, true);
            	holder.daily = createTextView("", false, true);
            	holder.active = createTextView("", false, true);
            	holder.activeTotal =  createTextView("", false, true);

            	((ViewGroup)convertView).addView(holder.date);
            	((ViewGroup)convertView).addView(holder.smooth);
            	((ViewGroup)convertView).addView(holder.total);
            	((ViewGroup)convertView).addView(holder.activeTotal);
            	((ViewGroup)convertView).addView(holder.daily);
            	((ViewGroup)convertView).addView(holder.active);

            	convertView.setTag(holder);
            	baseHolder = holder;
            } else if (currentChart instanceof RatingChartType) {

            	RatingsViewHolder holder = new RatingsViewHolder();
            	holder.date = createTextView("", false, false);
            	holder.smooth = createTextView("*", false, false);
            	holder.avgrating = createTextView("", false, true);
            	holder.rating1 = createTextView("", false, true);
            	holder.rating2 = createTextView("", false, true);
            	holder.rating3 = createTextView("", false, true);
            	holder.rating4 = createTextView("", false, true);
            	holder.rating5 = createTextView("", false, true);


            	((ViewGroup)convertView).addView(holder.date);
            	((ViewGroup)convertView).addView(holder.smooth);
            	((ViewGroup)convertView).addView(holder.avgrating);
            	((ViewGroup)convertView).addView(holder.rating5);
            	((ViewGroup)convertView).addView(holder.rating4);
            	((ViewGroup)convertView).addView(holder.rating3);
            	((ViewGroup)convertView).addView(holder.rating2);
            	((ViewGroup)convertView).addView(holder.rating1);

            	convertView.setTag(holder);
            	baseHolder = holder;

            }

        } else {

            baseHolder = (Object) convertView.getTag();
        }

        if(currentChart instanceof DownloadChartType) {

        	DownloadsViewHolder holder = (DownloadsViewHolder) baseHolder;

			holder.total.setText("<b>" + appInfo.getTotalDownloads() + "</b>");
			holder.date.setText(dateFormat.format(appInfo.getRequestDate()));
			holder.active.setText(appInfo.getActiveInstallsPercentString());
			holder.activeTotal.setText(appInfo.getActiveInstalls() + "");
			holder.total.setText(appInfo.getTotalDownloads() + "");
			holder.daily.setText(appInfo.getDailyDownloads() + "");

			Typeface typeface = holder.date.getTypeface();

			switch ((DownloadChartType)currentChart) {
			case ACTIVE_INSTALLS_PERCENT:
				holder.active.setTypeface(typeface, Typeface.BOLD);
				holder.total.setTypeface(typeface, Typeface.NORMAL);
				holder.activeTotal.setTypeface(typeface, Typeface.NORMAL);
				holder.daily.setTypeface(typeface, Typeface.NORMAL);

				break;
			case TOTAL_DOWNLAODS:
				holder.active.setTypeface(typeface, Typeface.NORMAL);
				holder.total.setTypeface(typeface, Typeface.BOLD);
				holder.activeTotal.setTypeface(typeface, Typeface.NORMAL);
				holder.daily.setTypeface(typeface, Typeface.NORMAL);

				break;
			case TOTAL_DOWNLAODS_BY_DAY:
				holder.active.setTypeface(typeface, Typeface.NORMAL);
				holder.total.setTypeface(typeface, Typeface.NORMAL);
				holder.activeTotal.setTypeface(typeface, Typeface.NORMAL);
				holder.daily.setTypeface(typeface, Typeface.BOLD);

				break;
			case ACTIVE_INSTALLS_TOTAL:
				holder.active.setTypeface(typeface, Typeface.NORMAL);
				holder.total.setTypeface(typeface, Typeface.NORMAL);
				holder.activeTotal.setTypeface(typeface, Typeface.BOLD);
				holder.daily.setTypeface(typeface, Typeface.NORMAL);

				break;

			default:
				break;
			}

	         if(versionUpdateDates.contains(appInfo.getRequestDate())) {
	                holder.date.setTextColor(RED_TEXT);
	                holder.active.setTextColor(RED_TEXT);
	                holder.total.setTextColor(RED_TEXT);
	                holder.activeTotal.setTextColor(RED_TEXT);
	                holder.daily.setTextColor(RED_TEXT);

	            } else {
                    holder.date.setTextColor(BLACK_TEXT);
                    holder.active.setTextColor(BLACK_TEXT);
                    holder.total.setTextColor(BLACK_TEXT);
                    holder.activeTotal.setTextColor(BLACK_TEXT);
                    holder.daily.setTextColor(BLACK_TEXT);

	            }

			if(!appInfo.isSmoothingApplied()) {
				holder.smooth.setVisibility(View.INVISIBLE);
			} else {
				holder.smooth.setVisibility(View.VISIBLE);
			}

		} else if (currentChart instanceof RatingChartType) {

        	RatingsViewHolder holder = (RatingsViewHolder) baseHolder;

			holder.date.setText(dateFormat.format(appInfo.getRequestDate()));
			holder.avgrating.setText(appInfo.getAvgRatingString());
			if(appInfo.getRating1Diff() > 0) {
				holder.rating1.setText("+" + appInfo.getRating1Diff());
			} else {
				holder.rating1.setText(appInfo.getRating1Diff() + "");
			}
			if(appInfo.getRating2Diff() > 0) {
				holder.rating2.setText("+" + appInfo.getRating2Diff());
			} else {
				holder.rating2.setText(appInfo.getRating2Diff() + "");
			}
			if(appInfo.getRating3Diff() > 0) {
				holder.rating3.setText("+" + appInfo.getRating3Diff());
			} else {
				holder.rating3.setText(appInfo.getRating3Diff() + "");
			}
			if(appInfo.getRating4Diff() > 0) {
				holder.rating4.setText("+" + appInfo.getRating4Diff());
			} else {
				holder.rating4.setText(appInfo.getRating4Diff() + "");
			}
			if(appInfo.getRating5Diff() > 0) {
				holder.rating5.setText("+" + appInfo.getRating5Diff());
			} else {
				holder.rating5.setText(appInfo.getRating5Diff() + "");
			}



			Typeface typeface = holder.date.getTypeface();

            if(versionUpdateDates.contains(appInfo.getRequestDate())) {
			    holder.date.setTextColor(RED_TEXT);
			    holder.avgrating.setTextColor(RED_TEXT);
			    holder.rating1.setTextColor(RED_TEXT);
                holder.rating2.setTextColor(RED_TEXT);
                holder.rating3.setTextColor(RED_TEXT);
                holder.rating4.setTextColor(RED_TEXT);
                holder.rating5.setTextColor(RED_TEXT);

			} else {
                holder.date.setTextColor(BLACK_TEXT);
                holder.avgrating.setTextColor(BLACK_TEXT);
                holder.rating1.setTextColor(BLACK_TEXT);
                holder.rating2.setTextColor(BLACK_TEXT);
                holder.rating3.setTextColor(BLACK_TEXT);
                holder.rating4.setTextColor(BLACK_TEXT);
                holder.rating5.setTextColor(BLACK_TEXT);

			}



			switch ((RatingChartType)currentChart) {
			case AVG_RATING:
				holder.avgrating.setTypeface(typeface, Typeface.BOLD);
				holder.rating1.setTypeface(typeface, Typeface.NORMAL);
				holder.rating2.setTypeface(typeface, Typeface.NORMAL);
				holder.rating3.setTypeface(typeface, Typeface.NORMAL);
				holder.rating4.setTypeface(typeface, Typeface.NORMAL);
				holder.rating5.setTypeface(typeface, Typeface.NORMAL);

				break;
			case RATINGS_1:
				holder.avgrating.setTypeface(typeface, Typeface.NORMAL);
				holder.rating1.setTypeface(typeface, Typeface.BOLD);
				holder.rating2.setTypeface(typeface, Typeface.NORMAL);
				holder.rating3.setTypeface(typeface, Typeface.NORMAL);
				holder.rating4.setTypeface(typeface, Typeface.NORMAL);
				holder.rating5.setTypeface(typeface, Typeface.NORMAL);

				break;
			case RATINGS_2:
				holder.avgrating.setTypeface(typeface, Typeface.NORMAL);
				holder.rating1.setTypeface(typeface, Typeface.NORMAL);
				holder.rating2.setTypeface(typeface, Typeface.BOLD);
				holder.rating3.setTypeface(typeface, Typeface.NORMAL);
				holder.rating4.setTypeface(typeface, Typeface.NORMAL);
				holder.rating5.setTypeface(typeface, Typeface.NORMAL);

				break;
			case RATINGS_3:
				holder.avgrating.setTypeface(typeface, Typeface.NORMAL);
				holder.rating1.setTypeface(typeface, Typeface.NORMAL);
				holder.rating2.setTypeface(typeface, Typeface.NORMAL);
				holder.rating3.setTypeface(typeface, Typeface.BOLD);
				holder.rating4.setTypeface(typeface, Typeface.NORMAL);
				holder.rating5.setTypeface(typeface, Typeface.NORMAL);

				break;
			case RATINGS_4:
				holder.avgrating.setTypeface(typeface, Typeface.NORMAL);
				holder.rating1.setTypeface(typeface, Typeface.NORMAL);
				holder.rating2.setTypeface(typeface, Typeface.NORMAL);
				holder.rating3.setTypeface(typeface, Typeface.NORMAL);
				holder.rating4.setTypeface(typeface, Typeface.BOLD);
				holder.rating5.setTypeface(typeface, Typeface.NORMAL);

				break;
			case RATINGS_5:
				holder.avgrating.setTypeface(typeface, Typeface.NORMAL);
				holder.rating1.setTypeface(typeface, Typeface.NORMAL);
				holder.rating2.setTypeface(typeface, Typeface.NORMAL);
				holder.rating3.setTypeface(typeface, Typeface.NORMAL);
				holder.rating4.setTypeface(typeface, Typeface.NORMAL);
				holder.rating5.setTypeface(typeface, Typeface.BOLD);

				break;

			default:
				break;
			}

			holder.smooth.setVisibility(View.INVISIBLE);

		}

		return convertView;
	}

	static class DownloadsViewHolder {
        public TextView smooth;
		public TextView date;
		TextView total;
        TextView daily;
        TextView active;
        TextView activeTotal;

    }

	static class RatingsViewHolder {
        public TextView smooth;
		public TextView date;
		TextView avgrating;
		TextView rating1;
		TextView rating2;
		TextView rating3;
		TextView rating4;
		TextView rating5;

    }
	private TextView createTextView(String string, boolean bold, boolean weight) {
		TextView view = new TextView(activity);
		view.setText(string);
		int top = (int) (2 * scale);
		int left = (int) (2 * scale);
		view.setPadding(left, top, left, top);
		view.setTextColor(Color.parseColor("#555555"));
		if(weight) {
			view.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, .2f));
		} else {
			view.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
		view.setGravity(Gravity.RIGHT);
		if(bold) {
			view.setTypeface(view.getTypeface(), Typeface.BOLD);
		}

		return view;
	}


	public Object getCurrentChart() {
		return currentChart;
	}

	public void setCurrentChart(Object currentChart) {
		this.currentChart = currentChart;
	}

	public void setDownloadInfos(List<AppStats> downloadInfos) {
		this.downloadInfos = downloadInfos;
	}

	public List<AppStats> getDownloadInfos() {
		return downloadInfos;
	}

    @Override
    public void notifyDataSetChanged() {
        this.dateFormat = new SimpleDateFormat(Preferences.getDateFormatShort(activity));
        super.notifyDataSetChanged();
    }

    public void setVersionUpdateDates(List<Date> versionUpdateDates) {
        this.versionUpdateDates = versionUpdateDates;
    }

    public List<Date> getVersionUpdateDates() {
        return versionUpdateDates;
    }




}
