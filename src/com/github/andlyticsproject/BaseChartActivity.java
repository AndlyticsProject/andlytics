package com.github.andlyticsproject;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Looper;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.chart.Chart;
import com.github.andlyticsproject.view.ChartGallery;
import com.github.andlyticsproject.view.ChartGalleryAdapter;
import com.github.andlyticsproject.view.ViewSwitcher3D;
import com.github.andlyticsproject.view.ViewSwitcher3D.ViewSwitcherListener;

public abstract class BaseChartActivity extends BaseDetailsActivity implements ViewSwitcherListener {

	private ChartGalleryAdapter chartGalleryAdapter;
	private ChartGallery chartGallery;
	private ListView dataList;
	private TextView timeframeText;
	protected String timetext;
	private Timeframe currentTimeFrame;
	private ViewSwitcher3D listViewSwitcher;

	BaseChartListAdapter myAdapter;

	private boolean refreshing;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basechart);
		List<View> extras;

		extras = getExtraFullViews();
		if (extras != null) {
			ViewSwitcher vs = (ViewSwitcher) findViewById(R.id.base_chart_viewswitcher_config);
			for (View v : extras)
				vs.addView(v);

		}

		currentTimeFrame = Preferences.getChartTimeframe(this);

		listViewSwitcher = new ViewSwitcher3D(
				(ViewGroup) findViewById(R.id.base_chart_bottom_frame));
		listViewSwitcher.setListener(this);

		chartGallery = (ChartGallery) findViewById(R.id.base_chart_gallery);
		chartGalleryAdapter = new ChartGalleryAdapter(new ArrayList<View>());
		chartGallery.setAdapter(chartGalleryAdapter);
		chartGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				chartGallery.setIgnoreLayoutCalls(true);

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
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.charts_menu, menu);
		MenuItem activeTimeFrame = null;
		switch (currentTimeFrame) {
		case LAST_SEVEN_DAYS:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframe7);
			break;
		case LAST_THIRTY_DAYS:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframe30);
			break;
		case LAST_NINETY_DAYS:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframe90);
			break;
		case UNLIMITED:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframeUnlimited);
			break;
		}
		activeTimeFrame.setChecked(true);

		if (refreshing) {
			menu.findItem(R.id.itemChartsmenuRefresh).setActionView(
					R.layout.action_bar_indeterminate_progress);
		}

		return true;
	}


	/**
	 * Called if item in option menu is selected.
	 * 
	 * @param item The chosen menu item
	 * @return boolean true/false
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemChartsmenuRefresh:
			setChartIgnoreCallLayouts(true);
			executeLoadData(currentTimeFrame);
			return true;
		case R.id.itemChartsmenuTimeframe7:
			currentTimeFrame = Timeframe.LAST_SEVEN_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_SEVEN_DAYS, BaseChartActivity.this);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframe30:
			currentTimeFrame = Timeframe.LAST_THIRTY_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_THIRTY_DAYS, BaseChartActivity.this);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframe90:
			currentTimeFrame = Timeframe.LAST_NINETY_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_NINETY_DAYS, BaseChartActivity.this);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframeUnlimited:
			currentTimeFrame = Timeframe.UNLIMITED;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.UNLIMITED, BaseChartActivity.this);
			item.setChecked(true);
			return true;
		default:
			return (super.onOptionsItemSelected(item));
		}
	}


	/**
	 * Called when chart is selected
	 * 
	 * @param page
	 * @param column
	 */
	protected void onChartSelected(int page, int column) {
	}

	protected void setCurrentChart(int page, int column) {
		int pos = 0;
		for (View view : chartGalleryAdapter.getViews()) {
			int pageColumn[] = (int[]) view.getTag();
			if (page == pageColumn[0] && column == pageColumn[1]) {
				chartGallery.setSelection(pos, false);
				return;
			}
			pos++;
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

	protected abstract void notifyChangedDataformat();

	protected List<View> getExtraFullViews() {
		return null;
	}

	protected abstract void executeLoadData(Timeframe currentTimeFrame);

	protected final void setAdapter(BaseChartListAdapter adapter) {
		myAdapter = adapter;
		dataList.setAdapter(adapter);
	}

	private final void updateTitleTextSwitcher(String string) {
		getSupportActionBar().setTitle(string);
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
				 * if(chartView==null) { Log.i(LOG_TAG,"Ignoring chart p="+i+" c="+j+"for class="
				 * +this.getClass().toString()); continue; }
				 */
				@SuppressWarnings("deprecation")
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
		String ret = myAdapter.getCurrentSubHeadLine();
		if (ret != null)
			subHeadlineText = ret;

		updateTitleTextSwitcher(title);

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

	public void setAllowChangePageSliding(boolean allowChangePageSliding) {
		chartGallery.setAllowChangePageSliding(allowChangePageSliding);
	}


	public synchronized boolean isRefreshing() {
		return refreshing;
	}

	public void refreshStarted() {
		ensureMainThread();

		refreshing = true;
		supportInvalidateOptionsMenu();
	}

	public void refreshFinished() {
		ensureMainThread();

		refreshing = false;
		supportInvalidateOptionsMenu();
	}

	private void ensureMainThread() {
		Looper looper = Looper.myLooper();
		if (looper != null && looper != getMainLooper()) {
			throw new IllegalStateException("Only call this from your main thread.");
		}
	}

}
