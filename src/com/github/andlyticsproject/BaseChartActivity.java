package com.github.andlyticsproject;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.widget.ViewSwitcher.ViewFactory;

import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.chart.Chart;
import com.github.andlyticsproject.chart.ChartTextSwitcher;
import com.github.andlyticsproject.view.ChartGallery;
import com.github.andlyticsproject.view.ChartGalleryAdapter;
import com.github.andlyticsproject.view.ViewSwitcher3D;
import com.github.andlyticsproject.view.ViewSwitcher3D.ViewSwitcherListener;

public abstract class BaseChartActivity extends BaseActivity implements ViewSwitcherListener {
	 private static String LOG_TAG=BaseChartActivity.class.toString();
	private ChartTextSwitcher titleTextSwitcher;
	private Animation inNegative;
	private Animation outNegative;
	private Animation inPositive;
	private Animation outPositive;

	private ChartGalleryAdapter chartGalleryAdapter;
	private ChartGallery chartGallery;
	private int currentChartPosition;
	private ListView dataList;
	private TextView timeframeText;
	protected String timetext;
	private Timeframe currentTimeFrame;
	private RadioButton radioDmy;
	private RadioButton radioYmd;
	private RadioButton radioMdy;
	private ViewSwitcher3D listViewSwitcher;

	private RadioButton radioLastThrity,radioUnlimited,radioLastSeven;
	
	BaseChartListAdapter myAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basechart);
		currentChartPosition = -1;
		List<View> extras;
		extras = getExtraConfig();
		if (extras != null) {
			LinearLayout container = (LinearLayout) findViewById(R.id.base_extra_config);
			for (View v : extras)
				container.addView(v);

		}

		extras = getExtraFullViews();
		if (extras != null) {
			ViewSwitcher vs = (ViewSwitcher) findViewById(R.id.base_chart_viewswitcher_config);
			for (View v : extras)
				vs.addView(v);

		}

		currentTimeFrame = Preferences.getChartTimeframe(this);
		
		View backButton = findViewById(R.id.base_chart_button_back);
		backButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(Main.class, false, true);
				overridePendingTransition(R.anim.activity_prev_in, R.anim.activity_prev_out);
			}
		});

		View configButton = findViewById(R.id.base_chart_button_config);
		if (configButton != null) {
			listViewSwitcher = new ViewSwitcher3D((ViewGroup) findViewById(R.id.base_chart_bottom_frame));
			listViewSwitcher.setListener(this);

			String dateFormatLong = Preferences.getDateFormatLong(this);
			radioDmy = (RadioButton) findViewById(R.id.base_chart_config_ratio_dmy);
			if ("dd/MM/yyyy".equals(dateFormatLong)) {
				radioDmy.setChecked(true);
			}
			radioDmy.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						Preferences.saveDateFormatLong(BaseChartActivity.this, "dd/MM/yyyy");
						Preferences.saveDateFormatShort(BaseChartActivity.this, "dd/MM");
						notifyChangedDataformat();
					}
				}
			});

			radioYmd = (RadioButton) findViewById(R.id.base_chart_config_ratio_ymd);
			if ("yyyy/MM/dd".equals(dateFormatLong)) {
				radioYmd.setChecked(true);
			}
			radioYmd.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						Preferences.saveDateFormatLong(BaseChartActivity.this, "yyyy/MM/dd");
						Preferences.saveDateFormatShort(BaseChartActivity.this, "MM/dd");
						notifyChangedDataformat();
					}
				}
			});
			radioMdy = (RadioButton) findViewById(R.id.base_chart_config_ratio_mdy);
			if ("MM/dd/yyyy".equals(dateFormatLong)) {
				radioMdy.setChecked(true);
			}
			radioMdy.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						Preferences.saveDateFormatLong(BaseChartActivity.this, "MM/dd/yyyy");
						Preferences.saveDateFormatShort(BaseChartActivity.this, "MM/dd");
						notifyChangedDataformat();
					}
				}
			});
			View configDoneButton = (View) findViewById(R.id.base_chart_config_done_button);
			configDoneButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					listViewSwitcher.swap();
				}
			});

			radioLastThrity = (RadioButton) findViewById(R.id.base_chart_config_ratio_last_thrity_days);
			radioUnlimited = (RadioButton) findViewById(R.id.base_chart_config_ratio_last_unlimited);
			radioLastSeven = (RadioButton) findViewById(R.id.base_chart_config_ratio_last_seven_days);

			if (Timeframe.LAST_SEVEN_DAYS.equals(currentTimeFrame)) {
				radioLastSeven.setChecked(true);
			}
			radioLastSeven.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						currentTimeFrame = Timeframe.LAST_SEVEN_DAYS;
						executeLoadData(currentTimeFrame);
						Preferences.saveChartTimeframe(Timeframe.LAST_SEVEN_DAYS, BaseChartActivity.this);
					}
				}
			});

			if (Timeframe.LAST_THIRTY_DAYS.equals(currentTimeFrame)) {
				radioLastThrity.setChecked(true);
			}
			radioLastThrity.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						currentTimeFrame = Timeframe.LAST_THIRTY_DAYS;
						executeLoadData(currentTimeFrame);
						Preferences.saveChartTimeframe(Timeframe.LAST_THIRTY_DAYS, BaseChartActivity.this);
					}
				}
			});
			if (Timeframe.UNLIMITED.equals(currentTimeFrame)) {
				radioUnlimited.setChecked(true);
			}
			radioUnlimited.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						currentTimeFrame = Timeframe.UNLIMITED;
						executeLoadData(currentTimeFrame);
						Preferences.saveChartTimeframe(Timeframe.UNLIMITED, BaseChartActivity.this);
					}
				}
			});
			
		}// End if(configButton!=null)

		titleTextSwitcher = (ChartTextSwitcher) findViewById(R.id.base_chart_base_chart_type);
		titleTextSwitcher.setFactory(new ViewFactory() {

			public View makeView() {

				return getLayoutInflater().inflate(R.layout.base_chart_headline, null);
			}
		});
		inNegative = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
		outNegative = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
		inPositive = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
		outPositive = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);

		chartGallery = (ChartGallery) findViewById(R.id.base_chart_gallery);
		chartGalleryAdapter = new ChartGalleryAdapter(new ArrayList<View>());
		chartGallery.setAdapter(chartGalleryAdapter);
		chartGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				chartGallery.setIgnoreLayoutCalls(true);

				if (currentChartPosition < position) {
					titleTextSwitcher.setInAnimation(inNegative);
					titleTextSwitcher.setOutAnimation(outNegative);
				} else {
					titleTextSwitcher.setInAnimation(inPositive);
					titleTextSwitcher.setOutAnimation(outPositive);
				}
				currentChartPosition = position;

				if (view.getTag() != null) {
					int pageColumn[] = (int[]) view.getTag();
					myAdapter.setCurrentChart(pageColumn[0], pageColumn[1]);
					updateChartHeadline();
					myAdapter.notifyDataSetChanged();
					onChartSelected(pageColumn[0], pageColumn[1]);

				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		dataList = (ListView) findViewById(R.id.base_chart_list);
		timeframeText = (TextView) findViewById(R.id.base_chart_timeframe);


		if (iconFilePath != null) {

			ImageView appIcon = (ImageView) findViewById(R.id.base_chart_app_icon);
			Bitmap bm = BitmapFactory.decodeFile(iconFilePath);
			appIcon.setImageBitmap(bm);
		}

	}
	/**
	 * Called when chart is selected
	 * @param page
	 * @param column
	 */
protected void onChartSelected(int page, int column) {}
protected void setCurrentChart(int page,int column)
{
	int pos = 0;
	for(View view:chartGalleryAdapter.getViews())
	{
		int pageColumn[] = (int[]) view.getTag();
		if(page==pageColumn[0] && column==pageColumn[1])
		{
			chartGallery.setSelection(pos, false);
			return;
		}
		pos++;
	}
	throw new IndexOutOfBoundsException("page="+page+" column="+column);
}
	protected abstract void notifyChangedDataformat();

	protected abstract List<View> getExtraConfig();

	protected List<View> getExtraFullViews() {
		return null;
	}
	
	protected final RadioButton getRadioLastSeven() {
		return radioLastSeven;
	}

	protected final RadioButton getRadioUnlimited() {
		return radioUnlimited;
	}

	protected final RadioButton getRadioLastThrity() {
		return radioLastThrity;
	}

	protected abstract void executeLoadData(Timeframe currentTimeFrame2);

	protected final void setAdapter(BaseChartListAdapter adapter) {
		myAdapter = adapter;
		dataList.setAdapter(adapter);
	}

	private final void updateTitleTextSwitcher(String string, Drawable image) {

		/*
		 * This commented code does not work for ranking Object tag =
		 * titleTextSwitcher.getTag();
		 * 
		 * if(tag != null && tag.equals(string)) { return; } else
		 */

		{
			titleTextSwitcher.setTag(string);
			titleTextSwitcher.setText(string, image);
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		chartGallery.setIgnoreLayoutCalls(false);
	}

	@Override
	public void onViewChanged(boolean frontsideVisible) {
		chartGallery.setIgnoreLayoutCalls(true);

	}

	@Override
	public void onRender() {
		chartGallery.invalidate();

	}

	protected final void setChartIgnoreCallLayouts(boolean ignoreLayoutCalls) {
		chartGallery.setIgnoreLayoutCalls(ignoreLayoutCalls);
	}

	public void updateCharts(List<?> statsForApp) {
		Chart chart = new Chart();
		int page = myAdapter.getCurrentPage();
		int column = myAdapter.getCurrentColumn();

		int position = -1;
		List<View> charts = new ArrayList<View>();

		int pos = 0;
		for (int i = 0; i < myAdapter.getNumPages(); i++)
			for (int j = 1; j < myAdapter.getNumCharts(i); j++) {
				int pageColumn[] = new int[3];
				View chartView = myAdapter.buildChart(this, chart, statsForApp, i, j);
				/*
				 * if(chartView==null) {
				 * Log.i(LOG_TAG,"Ignoring chart p="+i+" c="+j+"for class="
				 * +this.getClass().toString()); continue; }
				 */
				Gallery.LayoutParams params = new Gallery.LayoutParams(LayoutParams.FILL_PARENT,
				    LayoutParams.FILL_PARENT);
				chartView.setLayoutParams(params);
				pageColumn[0] = i;
				pageColumn[1] = j;
				pageColumn[2] = myAdapter.getNumCharts(i);
				if (i == page && j == column)
					position = pos;
				pos++;
				chartView.setTag(pageColumn);
				charts.add(chartView);
			}
		chartGallery.setIgnoreLayoutCalls(false);
		chartGalleryAdapter.setViews(charts);
		if (position >= 0)
			chartGallery.setSelection(position);
		chartGalleryAdapter.notifyDataSetChanged();
		chartGallery.invalidate();
	}

	protected final void updateChartHeadline() {

		String subHeadlineText = "";
		String title = myAdapter.getCurrentChartTitle();
		Drawable image = myAdapter.getCurrentChartTitleDrawable();
		String ret = myAdapter.getCurrentSubHeadLine();
		if (ret != null)
			subHeadlineText = ret;

		updateTitleTextSwitcher(title, image);

		if (Preferences.getShowChartHint(this)) {
			timeframeText.setText(Html.fromHtml(getChartHint()));
		} else {
			if (timetext != null) {
				timeframeText.setText(Html.fromHtml(timetext + ": <b>" + subHeadlineText + "</b>"));
			}
		}

	}

	public Timeframe getCurrentTimeFrame() {
		return currentTimeFrame;
	}

	public ViewSwitcher3D getListViewSwitcher() {
		return listViewSwitcher;
	}

	protected abstract String getChartHint();
	public void setAllowChangePageSliding(boolean allowChangePageSliding)
	{
	  chartGallery.setAllowChangePageSliding(allowChangePageSliding);
	}

}
