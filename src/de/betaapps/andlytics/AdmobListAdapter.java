package de.betaapps.andlytics;

import de.betaapps.andlytics.chart.Chart.AdmobChartType;
import de.betaapps.andlytics.model.Admob;

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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdmobListAdapter extends BaseAdapter {
    
    private NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);
	private List<Admob> stats;

	private LayoutInflater layoutInflater;

	private Activity activity;

	private float scale;

	private Object currentChart;

	private SimpleDateFormat dateFormat;
	
	private List<AdmobChartType> secondPageCharts;

	public AdmobListAdapter(Activity activity) {
		this.stats = new ArrayList<Admob>();
		this.layoutInflater = activity.getLayoutInflater();
		this.activity = activity;
		this.scale = activity.getResources().getDisplayMetrics().density;
		this.dateFormat = new SimpleDateFormat(Preferences.getDateFormatShort(activity));
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

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Object baseHolder = null;

        Admob admob = getItem(position);

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.chart_list_item, null);

            AdmobViewHolder holder = new AdmobViewHolder();
            
            holder.date = createTextView("", false, false);

            holder.revenue = createTextView("", false, true);
            holder.fillrate = createTextView("", false, true);
            holder.requests = createTextView("", false, true);
            holder.clicks = createTextView("", false, true);
                
            ((ViewGroup) convertView).addView(holder.date);
            ((ViewGroup) convertView).addView(holder.revenue);
            ((ViewGroup) convertView).addView(holder.requests);
            ((ViewGroup) convertView).addView(holder.clicks);
            ((ViewGroup) convertView).addView(holder.fillrate);

            holder.ctr = createTextView("", false, true);
            holder.houseAdClicks = createTextView("", false, true);
            holder.impressions = createTextView("", false, true);
            holder.ecpm = createTextView("", false, true);
            ((ViewGroup) convertView).addView(holder.ecpm);
            ((ViewGroup) convertView).addView(holder.impressions);
            ((ViewGroup) convertView).addView(holder.ctr);
            ((ViewGroup) convertView).addView(holder.houseAdClicks);
            
            
            convertView.setTag(holder);
            baseHolder = holder;

        } else {

            baseHolder = (Object) convertView.getTag();
        }

        AdmobViewHolder holder = (AdmobViewHolder) baseHolder;

        holder.date.setText(dateFormat.format(admob.getDate()));

        BigDecimal fillrate = new BigDecimal(admob.getFillRate() * 100);
        fillrate = fillrate.setScale(2, BigDecimal.ROUND_HALF_EVEN);

        BigDecimal ctr = new BigDecimal(admob.getCtr() * 100);
        ctr = ctr.setScale(2, BigDecimal.ROUND_HALF_EVEN);

        if(secondPageCharts.contains(currentChart)) {
            
            holder.ecpm.setVisibility(View.VISIBLE);
            holder.impressions.setVisibility(View.VISIBLE);
            holder.ctr.setVisibility(View.VISIBLE);
            holder.houseAdClicks.setVisibility(View.VISIBLE);
            holder.revenue.setVisibility(View.GONE);
            holder.requests.setVisibility(View.GONE);
            holder.clicks.setVisibility(View.GONE);
            holder.fillrate.setVisibility(View.GONE);
            holder.ecpm.setText(numberFormat.format(admob.getEcpm()));
            holder.impressions.setText(admob.getImpressions() +"");
            holder.ctr.setText(ctr.toPlainString() + "%");
            holder.houseAdClicks.setText(admob.getHouseAdClicks() +"");
        } else {
            
            holder.revenue.setText(numberFormat.format(admob.getRevenue()));
            holder.requests.setText(admob.getRequests() + "");
            holder.clicks.setText(admob.getClicks() + "");
            holder.fillrate.setText(fillrate.toPlainString() + "%");
            holder.ecpm.setVisibility(View.GONE);
            holder.impressions.setVisibility(View.GONE);
            holder.ctr.setVisibility(View.GONE);
            holder.houseAdClicks.setVisibility(View.GONE);
            holder.revenue.setVisibility(View.VISIBLE);
            holder.requests.setVisibility(View.VISIBLE);
            holder.clicks.setVisibility(View.VISIBLE);
            holder.fillrate.setVisibility(View.VISIBLE);            
        }
			
        Typeface typeface = holder.date.getTypeface();
            
		switch ((AdmobChartType)currentChart) {
			case REVENUE:
				holder.revenue.setTypeface(typeface, Typeface.BOLD);
				holder.requests.setTypeface(typeface, Typeface.NORMAL);
				holder.clicks.setTypeface(typeface, Typeface.NORMAL);
				holder.fillrate.setTypeface(typeface, Typeface.NORMAL);
                holder.ecpm.setTypeface(typeface, Typeface.NORMAL);
                holder.impressions.setTypeface(typeface, Typeface.NORMAL);
                holder.ctr.setTypeface(typeface, Typeface.NORMAL);
                holder.houseAdClicks.setTypeface(typeface, Typeface.NORMAL);
				
				break;
			case REQUESTS:
                holder.revenue.setTypeface(typeface, Typeface.NORMAL);
                holder.requests.setTypeface(typeface, Typeface.BOLD);
                holder.clicks.setTypeface(typeface, Typeface.NORMAL);
                holder.fillrate.setTypeface(typeface, Typeface.NORMAL);
                holder.ecpm.setTypeface(typeface, Typeface.NORMAL);
                holder.impressions.setTypeface(typeface, Typeface.NORMAL);
                holder.ctr.setTypeface(typeface, Typeface.NORMAL);
                holder.houseAdClicks.setTypeface(typeface, Typeface.NORMAL);
				
				break;
			case CLICKS:
                holder.revenue.setTypeface(typeface, Typeface.NORMAL);
                holder.requests.setTypeface(typeface, Typeface.NORMAL);
                holder.clicks.setTypeface(typeface, Typeface.BOLD);
                holder.fillrate.setTypeface(typeface, Typeface.NORMAL);
                holder.ecpm.setTypeface(typeface, Typeface.NORMAL);
                holder.impressions.setTypeface(typeface, Typeface.NORMAL);
                holder.ctr.setTypeface(typeface, Typeface.NORMAL);
                holder.houseAdClicks.setTypeface(typeface, Typeface.NORMAL);
				
				break;
			case FILL_RATE:
                holder.revenue.setTypeface(typeface, Typeface.NORMAL);
                holder.requests.setTypeface(typeface, Typeface.NORMAL);
                holder.clicks.setTypeface(typeface, Typeface.NORMAL);
                holder.fillrate.setTypeface(typeface, Typeface.BOLD);
                holder.ecpm.setTypeface(typeface, Typeface.NORMAL);
                holder.impressions.setTypeface(typeface, Typeface.NORMAL);
                holder.ctr.setTypeface(typeface, Typeface.NORMAL);
                holder.houseAdClicks.setTypeface(typeface, Typeface.NORMAL);
				
				break;
            case ECPM:
                holder.revenue.setTypeface(typeface, Typeface.NORMAL);
                holder.requests.setTypeface(typeface, Typeface.NORMAL);
                holder.clicks.setTypeface(typeface, Typeface.NORMAL);
                holder.fillrate.setTypeface(typeface, Typeface.NORMAL);
                holder.ecpm.setTypeface(typeface, Typeface.BOLD);
                holder.impressions.setTypeface(typeface, Typeface.NORMAL);
                holder.ctr.setTypeface(typeface, Typeface.NORMAL);
                holder.houseAdClicks.setTypeface(typeface, Typeface.NORMAL);
                
                break;
            case IMPRESSIONS:
                holder.revenue.setTypeface(typeface, Typeface.NORMAL);
                holder.requests.setTypeface(typeface, Typeface.NORMAL);
                holder.clicks.setTypeface(typeface, Typeface.NORMAL);
                holder.fillrate.setTypeface(typeface, Typeface.NORMAL);
                holder.ecpm.setTypeface(typeface, Typeface.NORMAL);
                holder.impressions.setTypeface(typeface, Typeface.BOLD);
                holder.ctr.setTypeface(typeface, Typeface.NORMAL);
                holder.houseAdClicks.setTypeface(typeface, Typeface.NORMAL);
                
                break;
            case CTR:
                holder.revenue.setTypeface(typeface, Typeface.NORMAL);
                holder.requests.setTypeface(typeface, Typeface.NORMAL);
                holder.clicks.setTypeface(typeface, Typeface.NORMAL);
                holder.fillrate.setTypeface(typeface, Typeface.NORMAL);
                holder.ecpm.setTypeface(typeface, Typeface.NORMAL);
                holder.impressions.setTypeface(typeface, Typeface.NORMAL);
                holder.ctr.setTypeface(typeface, Typeface.BOLD);
                holder.houseAdClicks.setTypeface(typeface, Typeface.NORMAL);
                
                break;
            case HOUSEAD_CLICKS:
                holder.revenue.setTypeface(typeface, Typeface.NORMAL);
                holder.requests.setTypeface(typeface, Typeface.NORMAL);
                holder.clicks.setTypeface(typeface, Typeface.NORMAL);
                holder.fillrate.setTypeface(typeface, Typeface.NORMAL);
                holder.ecpm.setTypeface(typeface, Typeface.NORMAL);
                holder.impressions.setTypeface(typeface, Typeface.NORMAL);
                holder.ctr.setTypeface(typeface, Typeface.NORMAL);
                holder.houseAdClicks.setTypeface(typeface, Typeface.BOLD);
                
                break;

			default:
				break;
			}
			
		
		
		return convertView;
	}
    
	static class AdmobViewHolder {
	    TextView clicks;
        TextView requests;
        TextView date;
        TextView revenue;
		TextView fillrate;
		TextView ecpm;
        TextView impressions;
        TextView ctr;
        TextView houseAdClicks;
		

        
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


    @Override
    public void notifyDataSetChanged() {
        this.dateFormat = new SimpleDateFormat(Preferences.getDateFormatShort(activity));
        super.notifyDataSetChanged();
    }

    public void setStats(List<Admob> stats) {
        this.stats = stats;
    }

    public List<Admob> getStats() {
        return stats;
    }


    public void setSecondPageCharts(List<AdmobChartType> secondPageCharts) {
        this.secondPageCharts = secondPageCharts;
    }

    public List<AdmobChartType> getSecondPageCharts() {
        return secondPageCharts;
    }
	
    


}
