package com.github.andlyticsproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ViewSwitcher.ViewFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.chart.Chart;
import com.github.andlyticsproject.chart.ChartTextSwitcher;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.chart.Chart.DownloadChartType;
import com.github.andlyticsproject.chart.Chart.RatingChartType;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.AppStatsList;
import com.github.andlyticsproject.view.ChartGallery;
import com.github.andlyticsproject.view.ChartGalleryAdapter;
import com.github.andlyticsproject.view.ViewSwitcher3D;
import com.github.andlyticsproject.view.ViewSwitcher3D.ViewSwitcherListener;

public class ChartActivity extends BaseActivity implements ViewSwitcherListener {

	private ContentAdapter db;
	private ChartGallery chartGallery;
	private Object currentChart;
//	private TextView chatTypeText;
	private ListView historyList;
	private ChartListAdapter historyListAdapter;
	private TextView historyListFooter;
	private TextView timeframeText;
	private View oneEntryHint;
	private boolean dataUpdateRequested;
	private ChartTextSwitcher titleTextSwitcher;
	private int currentChartPosition;
	private Animation inNegative;
	private Animation outNegative;
	private Animation inPositive;
	private Animation outPositive;

	public String timetext;
	private ChartSet currentChartSet;
	public Integer heighestRatingChange;
	public Integer lowestRatingChange;
	private ViewSwitcher3D viewSwitcher;
	private Timeframe currentTimeFrame;
	private RadioButton radioLastThrity;
	private RadioButton radioUnlimited;
	private ChartGalleryAdapter chartGalleryAdapter;
	private CheckBox checkSmooth;
	private Boolean smoothEnabled;
    private RadioButton radioDmy;
    private RadioButton radioYmd;
    private RadioButton radioMdy;
    private AppStats overallStats;
    private RadioButton radioLastSeven;
    public List<Date> versionUpdateDates;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.chart);

		this.setCurrentChartSet(ChartSet.RATINGS);

		currentChartPosition = -1;

        titleTextSwitcher = (ChartTextSwitcher) findViewById(R.id.chart_chart_type);
        titleTextSwitcher.setFactory(new ViewFactory() {

			public View makeView() {

				return getLayoutInflater().inflate(R.layout.chart_headline, null);
			}
		});
		inNegative = AnimationUtils.loadAnimation(ChartActivity.this, R.anim.slide_in_right);
		outNegative = AnimationUtils.loadAnimation(ChartActivity.this, R.anim.slide_out_left);
		inPositive = AnimationUtils.loadAnimation(ChartActivity.this, R.anim.slide_in_left);
		outPositive = AnimationUtils.loadAnimation(ChartActivity.this, R.anim.slide_out_right);

		db = getDbAdapter();
		//chartFrame = (ViewSwitcher) ;


		currentTimeFrame = Preferences.getChartTimeframe(this);
		smoothEnabled = Preferences.getChartSmooth(this);

		View backButton = findViewById(R.id.chart_button_back);
		backButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(Main.class, false, true);
				overridePendingTransition(R.anim.activity_prev_in, R.anim.activity_prev_out);
			}
		});


		View configButton = findViewById(R.id.chart_button_config);

		if(configButton != null) {

			viewSwitcher = new ViewSwitcher3D((ViewGroup) findViewById(R.id.chart_bottom_frame));
			viewSwitcher.setListener(this);

			configButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					chartGallery.setIgnoreLayoutCalls(true);
					viewSwitcher.swap();
				}
			});

	         radioLastSeven = (RadioButton) findViewById(R.id.chart_config_ratio_last_seven_days);
	            if(Timeframe.LAST_SEVEN_DAYS.equals(currentTimeFrame)) {
	                radioLastSeven.setChecked(true);
	            }
	            radioLastSeven.setOnCheckedChangeListener(new OnCheckedChangeListener() {

	                @Override
	                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	                    if(isChecked) {
	                        currentTimeFrame = Timeframe.LAST_SEVEN_DAYS;
	                        new LoadChartData().execute();
	                        Preferences.saveChartTimeframe(Timeframe.LAST_SEVEN_DAYS, ChartActivity.this);
	                    }
	                }
	            });

			radioLastThrity = (RadioButton) findViewById(R.id.chart_config_ratio_last_thrity_days);
			if(Timeframe.LAST_THIRTY_DAYS.equals(currentTimeFrame)) {
				radioLastThrity.setChecked(true);
			}
			radioLastThrity.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked) {
						currentTimeFrame = Timeframe.LAST_THIRTY_DAYS;
						new LoadChartData().execute();
						Preferences.saveChartTimeframe(Timeframe.LAST_THIRTY_DAYS, ChartActivity.this);
					}
				}
			});
			radioUnlimited = (RadioButton) findViewById(R.id.chart_config_ratio_last_unlimited);
			if(Timeframe.UNLIMITED.equals(currentTimeFrame)) {
				radioUnlimited.setChecked(true);
			}
			radioUnlimited.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked) {
						currentTimeFrame = Timeframe.UNLIMITED;
						new LoadChartData().execute();
						Preferences.saveChartTimeframe(Timeframe.UNLIMITED, ChartActivity.this);
					}
				}
			});

			String dateFormatLong = Preferences.getDateFormatLong(this);
            radioDmy = (RadioButton) findViewById(R.id.chart_config_ratio_dmy);
            if("dd/MM/yyyy".equals(dateFormatLong)) {
                radioDmy.setChecked(true);
            }
            radioDmy.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        Preferences.saveDateFormatLong(ChartActivity.this, "dd/MM/yyyy");
                        Preferences.saveDateFormatShort(ChartActivity.this, "dd/MM");
                        dataUpdateRequested = true;
                        new LoadChartData().execute();
                    }
                }
            });

            radioYmd = (RadioButton) findViewById(R.id.chart_config_ratio_ymd);
            if("yyyy/MM/dd".equals(dateFormatLong)) {
                radioYmd.setChecked(true);
            }
            radioYmd.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        Preferences.saveDateFormatLong(ChartActivity.this, "yyyy/MM/dd");
                        Preferences.saveDateFormatShort(ChartActivity.this, "MM/dd");
                        dataUpdateRequested = true;
                        new LoadChartData().execute();
                    }
                }
            });
            radioMdy = (RadioButton) findViewById(R.id.chart_config_ratio_mdy);
            if("MM/dd/yyyy".equals(dateFormatLong)) {
                radioMdy.setChecked(true);
            }
            radioMdy.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        Preferences.saveDateFormatLong(ChartActivity.this, "MM/dd/yyyy");
                        Preferences.saveDateFormatShort(ChartActivity.this, "MM/dd");
                        dataUpdateRequested = true;
                        new LoadChartData().execute();
                    }
                }
            });



			// smoth
			checkSmooth = (CheckBox) findViewById(R.id.chart_config_checkbox_smooth);
			if(smoothEnabled) {
				checkSmooth.setChecked(true);
			}
			checkSmooth.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked) {
						smoothEnabled = true;
						new LoadChartData().execute();
						Preferences.saveSmooth(true, ChartActivity.this);
					} else {
						smoothEnabled = false;
						new LoadChartData().execute();
						Preferences.saveSmooth(false, ChartActivity.this);
					}
				}
			});
			View configDoneButton = (View) findViewById(R.id.chart_config_done_button);
			configDoneButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					viewSwitcher.swap();
				}
			});

		}

		chartGallery = (ChartGallery) findViewById(R.id.chart_gallery);
		chartGalleryAdapter = new ChartGalleryAdapter(new ArrayList<View>());
		chartGallery.setAdapter(chartGalleryAdapter);
		chartGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				chartGallery.setIgnoreLayoutCalls(true);

				if(currentChartPosition < position) {
					titleTextSwitcher.setInAnimation(inNegative);
					titleTextSwitcher.setOutAnimation(outNegative);
				} else {
					titleTextSwitcher.setInAnimation(inPositive);
					titleTextSwitcher.setOutAnimation(outPositive);
				}
				currentChartPosition = position;


				Object tag = view.getTag();
				if (tag != null) {

					 currentChart = tag;

					updateChartHeadline();

					historyListAdapter.setCurrentChart(currentChart);
					historyListAdapter.notifyDataSetChanged();

				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		oneEntryHint = (View)findViewById(R.id.chart_one_entry_hint);


		timeframeText = (TextView) findViewById(R.id.chart_timeframe);

		historyList = (ListView) findViewById(R.id.chart_list);
		View inflate = getLayoutInflater().inflate(R.layout.chart_list_footer, null);
		historyListFooter = (TextView) inflate.findViewById(R.id.chart_footer_text);
		historyList.addFooterView(inflate, null, false);

		historyListAdapter = new ChartListAdapter(this);
		historyList.setAdapter(historyListAdapter);

		if(iconFilePath != null) {

			ImageView appIcon = (ImageView) findViewById(R.id.chart_app_icon);
			Bitmap bm = BitmapFactory.decodeFile(iconFilePath);
			appIcon.setImageBitmap(bm);
		}

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
		if(currentChartSet.equals(ChartSet.DOWNLOADS)) {
			currentChart = DownloadChartType.TOTAL_DOWNLAODS;
		} else {
			currentChart = RatingChartType.AVG_RATING;
		}


	}


	protected void updateChartHeadline() {

	    String subHeadlineText = "";

	    if(getCurrentChartSet().equals(ChartSet.DOWNLOADS)) {

            switch ((DownloadChartType) currentChart) {

            case TOTAL_DOWNLAODS:
                titleTextSwitcher.setText(this.getString(R.string.total_downloads),null);
                if(overallStats != null)
                    subHeadlineText = overallStats.getTotalDownloads() + "";

                break;

            case TOTAL_DOWNLAODS_BY_DAY:
                titleTextSwitcher.setText(this.getString(R.string.downloads_day),null);
                subHeadlineText = overallStats.getDailyDownloads() + "";
                break;

            case ACTIVE_INSTALLS_PERCENT:
                titleTextSwitcher.setText(this.getString(R.string.active_installs_percent),null);
                subHeadlineText = overallStats.getActiveInstallsPercentString() + "%";
                break;

            case ACTIVE_INSTALLS_TOTAL:
                Preferences.saveShowChartHint(ChartActivity.this, false);
                titleTextSwitcher.setText(this.getString(R.string.active_installs),null);
                subHeadlineText = overallStats.getActiveInstalls() + "";
                break;

            default:
                break;
            }

        } else if (getCurrentChartSet().equals(ChartSet.RATINGS)) {
            switch ((RatingChartType) currentChart) {

            case AVG_RATING:
                titleTextSwitcher.setText(this.getString(R.string.average_rating),null);
                subHeadlineText = overallStats.getAvgRatingString() + "";
                break;

            case RATINGS_1:
                titleTextSwitcher.setText(this.getString(R.string.ratings), getResources().getDrawable(R.drawable.rating_1));
                subHeadlineText = overallStats.getRating1() + "";
                break;
            case RATINGS_2:
                titleTextSwitcher.setText(this.getString(R.string.ratings), getResources().getDrawable(R.drawable.rating_2));
                subHeadlineText = overallStats.getRating2() + "";
                break;
            case RATINGS_3:
                titleTextSwitcher.setText(this.getString(R.string.ratings), getResources().getDrawable(R.drawable.rating_3));
                subHeadlineText = overallStats.getRating3() + "";
                break;
            case RATINGS_4:
                titleTextSwitcher.setText(this.getString(R.string.ratings), getResources().getDrawable(R.drawable.rating_4));
                subHeadlineText = overallStats.getRating4() + "";
                break;
            case RATINGS_5:
                Preferences.saveShowChartHint(ChartActivity.this, false);
                titleTextSwitcher.setText(this.getString(R.string.ratings), getResources().getDrawable(R.drawable.rating_5));
                subHeadlineText = overallStats.getRating5() + "";
                break;



            default:
                break;
            }

        }

	    if(Preferences.getShowChartHint(ChartActivity.this)) {
            timeframeText.setText(Html.fromHtml(this.getString(R.string.swipe)));
	    } else {
	        if(timetext != null) {
	            timeframeText.setText(Html.fromHtml(timetext + ": <b>" + subHeadlineText + "</b>"));
	        }
	    }


    }


    @Override
	protected void onResume() {
		super.onResume();
		chartGallery.setIgnoreLayoutCalls(false);

		dataUpdateRequested = true;

		new LoadChartData().execute();

	}


	public void setCurrentChartSet(ChartSet currentChartSet) {
		this.currentChartSet = currentChartSet;
	}


	public ChartSet getCurrentChartSet() {
		return currentChartSet;
	}


	private class LoadChartData extends AsyncTask<Void, Void, Boolean> {

		private List<AppStats> statsForApp;

		private boolean smoothedValues = false;


		@Override
		protected void onPreExecute() {
			if(radioLastThrity != null) {
				radioLastThrity.setEnabled(false);
				radioUnlimited.setEnabled(false);
				checkSmooth.setEnabled(false);
			}
		}


		@Override
		protected Boolean doInBackground(Void... params) {

			if(dataUpdateRequested || statsForApp == null || statsForApp.size() == 0) {
				AppStatsList result = db.getStatsForApp(packageName, currentTimeFrame, smoothEnabled);
				statsForApp = result.getAppStats();
				overallStats = result.getOverall();
				versionUpdateDates = db.getVersionUpdateDates(packageName);

				heighestRatingChange = result.getHighestRatingChange();
				lowestRatingChange = result.getLowestRatingChange();
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
				Chart chart = new Chart();

				List<View> charts = new ArrayList<View>();

				if(getCurrentChartSet().equals(ChartSet.DOWNLOADS)) {

					DownloadChartType[] chartTypes = DownloadChartType.values();
					for (int i = 0; i < chartTypes.length; i++) {
						View chartView = chart.buildDownloadChart(ChartActivity.this, statsForApp, chartTypes[i]);
						Gallery.LayoutParams params = new Gallery.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
						chartView.setLayoutParams(params);
						chartView.setTag(chartTypes[i]);
						charts.add(chartView);
					}

				} else if (getCurrentChartSet().equals(ChartSet.RATINGS)) {
					RatingChartType[] chartTypes = RatingChartType.values();
					for (int i = 0; i < chartTypes.length; i++) {
						View chartView = chart.buildRatingChart(ChartActivity.this, statsForApp, chartTypes[i], heighestRatingChange, lowestRatingChange);
						Gallery.LayoutParams params = new Gallery.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
						chartView.setLayoutParams(params);
						chartView.setTag(chartTypes[i]);
						charts.add(chartView);
					}
				}

				chartGallery.setIgnoreLayoutCalls(false);
				chartGalleryAdapter.setViews(charts);
				chartGalleryAdapter.notifyDataSetChanged();
				chartGallery.invalidate();
				SimpleDateFormat dateFormat = new SimpleDateFormat(Preferences.getDateFormatLong(ChartActivity.this));
				timetext = dateFormat.format(statsForApp.get(0).getRequestDate()) + " - " + dateFormat.format(statsForApp.get(statsForApp.size() -1).getRequestDate());

                updateChartHeadline();

				Collections.reverse(statsForApp);
				historyListAdapter.setDownloadInfos(statsForApp);
				historyListAdapter.setVersionUpdateDates(versionUpdateDates);
				historyListAdapter.setCurrentChart(currentChart);
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
			if(radioLastThrity != null) {
				radioLastThrity.setEnabled(true);
				radioUnlimited.setEnabled(true);
				checkSmooth.setEnabled(true);

			}

		}


	}


	@Override
	public void onViewChanged(boolean frontsideVisible) {
		chartGallery.setIgnoreLayoutCalls(true);

	}


	@Override
	public void onRender() {
		chartGallery.invalidate();

	}

}
