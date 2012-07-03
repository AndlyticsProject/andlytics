package com.github.andlyticsproject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.AppStatsList;

public class ChartActivity extends BaseChartActivity {
//	private static String LOG_TAG=ChartActivity.class.toString();
	private ContentAdapter db;
	private ListView historyList;
	private ChartListAdapter historyListAdapter;
	private TextView historyListFooter;
	private View oneEntryHint;
	private boolean dataUpdateRequested;

	private ChartSet currentChartSet;
	private CheckBox checkSmooth;
	private Boolean smoothEnabled;
    public List<Date> versionUpdateDates;

  
  @Override
  protected void executeLoadData(Timeframe timeFrame)
  {
    new LoadChartData().execute(timeFrame);
    
  }
  private void executeLoadDataDefault()
  {
    new LoadChartData().execute(getCurrentTimeFrame());
    
  }
  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
		Bundle b = intent.getExtras();
		if (b != null) {
			String chartSet = b.getString(Constants.CHART_SET);
			if(chartSet != null) {
				currentChartSet = ChartSet.valueOf(chartSet);
			}
		}

		if(currentChartSet == null) {
			currentChartSet = ChartSet.DOWNLOADS;
		}
		setCurrentChart(currentChartSet.ordinal(),1);
    
  }
  @Override
	public void onCreate(Bundle savedInstanceState) {
		smoothEnabled = Preferences.getChartSmooth(this);
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		setCurrentChartSet(ChartSet.RATINGS);
		
		Bundle b = getIntent().getExtras();
		if (b != null) {
			String chartSet = b.getString(Constants.CHART_SET);
			if(chartSet != null) {
				currentChartSet = ChartSet.valueOf(chartSet);
			}
		}

		if(currentChartSet == null) {
			currentChartSet = ChartSet.DOWNLOADS;
		}
		
		if (ChartSet.RATINGS.equals(currentChartSet)) {
			getSupportActionBar().setTitle(R.string.ratings);
		} else {
			getSupportActionBar().setTitle(R.string.downloads);
		}
				
		db = getDbAdapter();
		//chartFrame = (ViewSwitcher) ;

		oneEntryHint = (View)findViewById(R.id.base_chart_one_entry_hint);

		historyList = (ListView) findViewById(R.id.base_chart_list);
		View inflate = getLayoutInflater().inflate(R.layout.chart_list_footer, null);
		historyListFooter = (TextView) inflate.findViewById(R.id.chart_footer_text);
		historyList.addFooterView(inflate, null, false);

		historyListAdapter = new ChartListAdapter(this);
		setAdapter(historyListAdapter);
		
		historyListAdapter.setCurrentChart(currentChartSet.ordinal(),1);
		setAllowChangePageSliding(false);
	}

  @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.charts_menu, menu);
		return true;
	}
	
	/**
	 * Called if item in option menu is selected.
	 * 
	 * @param item
	 *            The chosen menu item
	 * @return boolean true/false
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			startActivityAfterCleanup(Main.class);
			return true;
		case R.id.itemChartsmenuSettings:
			setChartIgnoreCallLayouts(true);
			getListViewSwitcher().swap();
			return true;
		default:
			return (super.onOptionsItemSelected(item));
		}
	}
	
	/**
	 * starts a given activity with a clear flag.
	 * 
	 * @param activity
	 *            Activity to be started
	 */
	private void startActivityAfterCleanup(Class<?> activity) {
		Intent intent = new Intent(getApplicationContext(), activity);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}


@Override
protected String getChartHint() {
  return this.getString(R.string.swipe);
}
    @Override
	protected void onResume() {
		super.onResume();

		dataUpdateRequested = true;

		executeLoadDataDefault();

	}



  public void setCurrentChartSet(ChartSet currentChartSet) {
		this.currentChartSet = currentChartSet;
	}


	public ChartSet getCurrentChartSet() {
		return currentChartSet;
	}


	private class LoadChartData extends AsyncTask<Timeframe, Void, Boolean> {

		private List<AppStats> statsForApp;

		private boolean smoothedValues = false;


		@Override
		protected void onPreExecute() {
			if(getRadioLastThrity() != null) {
			  getRadioLastThrity().setEnabled(false);
				getRadioUnlimited().setEnabled(false);
				checkSmooth.setEnabled(false);
			}
		}


		@Override
		protected Boolean doInBackground(Timeframe... params) {

			if(dataUpdateRequested || statsForApp == null || statsForApp.size() == 0) {
				AppStatsList result = db.getStatsForApp(packageName, params[0], smoothEnabled);
				statsForApp = result.getAppStats();
				historyListAdapter.setOverallStats(result.getOverall());
				versionUpdateDates = db.getVersionUpdateDates(packageName);

				historyListAdapter.setHeighestRatingChange(result.getHighestRatingChange());
				historyListAdapter.setLowestRatingChange(result.getLowestRatingChange());

				dataUpdateRequested = false;

				smoothedValues = false;

				for (AppStats appInfo : statsForApp) {
					if(appInfo.isSmoothingApplied()) {
						smoothedValues = true;
						break;
					}
				}

			}


			return true;
		}


		@Override
		protected void onPostExecute(Boolean result) {

			if(statsForApp != null && statsForApp.size() > 0) {
			  updateCharts(statsForApp);
				
				SimpleDateFormat dateFormat = new SimpleDateFormat(Preferences.getDateFormatLong(ChartActivity.this));
				timetext = dateFormat.format(statsForApp.get(0).getRequestDate()) + " - " + dateFormat.format(statsForApp.get(statsForApp.size() -1).getRequestDate());

                updateChartHeadline();

				Collections.reverse(statsForApp);
				historyListAdapter.setDownloadInfos(statsForApp);
				historyListAdapter.setVersionUpdateDates(versionUpdateDates);
/*				int page=historyListAdapter.getCurrentPage();
				int column=historyListAdapter.getCurrentColumn();
				historyListAdapter.setCurrentChart(page, column);*/
				historyListAdapter.notifyDataSetChanged();

				if(smoothedValues && currentChartSet.equals(ChartSet.DOWNLOADS)) {
					historyListFooter.setVisibility(View.VISIBLE);
				} else {
					historyListFooter.setVisibility(View.INVISIBLE);
				}

				if(oneEntryHint != null) {
					if(statsForApp.size() == 1) {
						oneEntryHint.setVisibility(View.VISIBLE);
					} else {
						oneEntryHint.setVisibility(View.INVISIBLE);
					}
				}


				//chartFrame.showNext();

			}
			if(getRadioLastThrity() != null) {
			  getRadioLastThrity().setEnabled(true);
				getRadioUnlimited().setEnabled(true);
				checkSmooth.setEnabled(true);

			}

		}


	}


  @Override
  protected void notifyChangedDataformat()
  {
    dataUpdateRequested = true;
    executeLoadDataDefault();
    
  }
  @Override
  protected List<View> getExtraConfig()
  {
    LinearLayout ll = (LinearLayout) getLayoutInflater().inflate(R.layout.chart_extra_config, null);

    // smoth
    checkSmooth = (CheckBox) ll.findViewById(R.id.chart_config_checkbox_smooth);
    if(smoothEnabled) {
      checkSmooth.setChecked(true);
    }
    checkSmooth.setOnCheckedChangeListener(new OnCheckedChangeListener() {

      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked) {
          smoothEnabled = true;
          executeLoadDataDefault();
          Preferences.saveSmooth(true, ChartActivity.this);
        } else {
          smoothEnabled = false;
          executeLoadDataDefault();
          Preferences.saveSmooth(false, ChartActivity.this);
        }
      }
    });
    
    List<View> ret = new ArrayList<View>();
    ret.add(ll);
    return ret;
  }

@Override
protected void onChartSelected(int page, int column) {
  super.onChartSelected(page, column);
  if(page!=currentChartSet.ordinal())
  {
  	currentChartSet=ChartSet.values()[page];
  	updateTabbarButtons();
  }
  	
}



}
