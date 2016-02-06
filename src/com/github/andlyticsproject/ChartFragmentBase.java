package com.github.andlyticsproject;


import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.chart.Chart;
import com.github.andlyticsproject.view.ChartGallery;
import com.github.andlyticsproject.view.ChartGalleryAdapter;
import com.github.andlyticsproject.view.ViewSwitcher3D;
import com.github.andlyticsproject.view.ViewSwitcher3D.ViewSwitcherListener;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public abstract class ChartFragmentBase extends Fragment implements ViewSwitcherListener {

	protected static final String SELECTED_CHART_PAGE = "selected_chart_page";
	protected static final String SELECTED_CHART_COLUMN = "selected_chart_position";

	protected ChartGalleryAdapter chartGalleryAdapter;
	protected ChartGallery chartGallery;
	protected ListView dataList;
	protected TextView timeframeText;
	protected String timetext;
	protected Timeframe currentTimeFrame;
	protected ViewSwitcher3D listViewSwitcher;
	protected ViewGroup dataframe;
	protected ViewGroup chartframe;
	protected BaseChartListAdapter<?> myAdapter;

	protected int currentChartPage = -1;
	protected int currentChartColumn = -1;

	public ChartFragmentBase() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.chart_fragment, container, false);

		List<View> extras = getExtraFullViews(view);
		if (extras != null) {
			ViewSwitcher vs = (ViewSwitcher) view.findViewById(R.id.base_chart_viewswitcher_config);
			for (View v : extras)
				vs.addView(v);
		}

		currentTimeFrame = Preferences.getChartTimeframe(getActivity());

		listViewSwitcher = new ViewSwitcher3D(
				(ViewGroup) view.findViewById(R.id.base_chart_bottom_frame));
		listViewSwitcher.setListener(this);

		chartGallery = (ChartGallery) view.findViewById(R.id.base_chart_gallery);
		chartGalleryAdapter = new ChartGalleryAdapter(new ArrayList<View>());
		chartGallery.setAdapter(chartGalleryAdapter);
		chartGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				chartGallery.setIgnoreLayoutCalls(true);

				if (view.getTag() != null) {
					// (page, column, num columns)?
					int pageColumn[] = (int[]) view.getTag();
					int page = pageColumn[0];
					int column = pageColumn[1];
					myAdapter.setCurrentChart(page, column);
					updateChartHeadline();
					myAdapter.notifyDataSetChanged();
					onChartSelected(page, column);
				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		dataList = (ListView) view.findViewById(R.id.base_chart_list);
		timeframeText = (TextView) view.findViewById(R.id.base_chart_timeframe);
		dataframe = (ViewGroup) view.findViewById(R.id.base_chart_datacontainer);
		chartframe = (ViewGroup) view.findViewById(R.id.base_chart_chartframe);

		return view;
	}


	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		if (chartGallery != null && myAdapter != null) {
			state.putInt(SELECTED_CHART_PAGE, myAdapter.getCurrentPage());
			state.putInt(SELECTED_CHART_COLUMN, myAdapter.getCurrentColumn());
		}
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState != null) {
			currentChartPage = savedInstanceState.getInt(SELECTED_CHART_PAGE, -1);
			currentChartColumn = savedInstanceState.getInt(SELECTED_CHART_COLUMN, -1);
		}

		restoreChartSelection();
	}

	/**
	 * Toggles visibility of chart/data UI parts.
	 */
	protected void toggleChartData(MenuItem item) {
		if (View.VISIBLE == chartframe.getVisibility()) {
			chartframe.setVisibility(View.GONE);
			dataframe.setVisibility(View.VISIBLE);
			item.setIcon(this.getResources().getDrawable(R.drawable.icon_graph));
		} else {
			chartframe.setVisibility(View.VISIBLE);
			dataframe.setVisibility(View.GONE);
			item.setIcon(this.getResources().getDrawable(R.drawable.icon_data));
		}
	}

	/**
	 * Called when chart is selected
	 * 
	 * @param page
	 * @param column
	 */
	protected void onChartSelected(int page, int column) {
		currentChartPage = page;
		currentChartColumn = column;
	}

	public void setCurrentChart(int page, int column) {
		if (chartGalleryAdapter.getViews().isEmpty()) {
			// chart not initialized yet
			return;
		}

		int pos = 0;
		for (View view : chartGalleryAdapter.getViews()) {
			int pageColumn[] = (int[]) view.getTag();
			if (page == pageColumn[0] && column == pageColumn[1]) {
				chartGallery.setSelection(pos, false);
				myAdapter.setCurrentChart(page, column);
				return;
			}
			pos++;
		}
		throw new IndexOutOfBoundsException("page=" + page + " column=" + column);
	}

	public int getCurrentChart() {
		return chartGallery.getSelectedItemPosition();
	}


	protected abstract void notifyChangedDataformat();

	protected List<View> getExtraFullViews(View root) {
		return new ArrayList<View>();
	}

	protected abstract void executeLoadData(Timeframe currentTimeFrame);

	protected final void setAdapter(BaseChartListAdapter<?> adapter) {
		myAdapter = adapter;
		dataList.setAdapter(adapter);
	}

	protected final void updateTitleTextSwitcher(String string) {
		if (getActivity() != null) {
			getActivity().getActionBar().setTitle(string);
		}
	}

	@Override
	public void onResume() {
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

	public Timeframe getCurrentTimeFrame() {
		return currentTimeFrame;
	}

	public ViewSwitcher3D getListViewSwitcher() {
		return listViewSwitcher;
	}

	protected final void updateChartHeadline() {

		String subHeadlineText = "";
		String title = myAdapter.getCurrentChartTitle();
		String ret = myAdapter.getCurrentSubHeadLine();
		if (ret != null)
			subHeadlineText = ret;

		updateTitleTextSwitcher(title);

		if (Preferences.getShowChartHint(getActivity())) {
			timeframeText.setText(Html.fromHtml(getChartHint()));
		} else {
			if (timetext != null) {
				timeframeText.setText(Html.fromHtml(timetext + ": <b>" + subHeadlineText + "</b>"));
			}
		}

	}

	protected abstract String getChartHint();

	public void setAllowChangePageSliding(boolean allowChangePageSliding) {
		chartGallery.setAllowChangePageSliding(allowChangePageSliding);
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
				View chartView = myAdapter.buildChart(getActivity(), chart, statsForApp, i, j);
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

	protected final void setChartIgnoreCallLayouts(boolean ignoreLayoutCalls) {
		chartGallery.setIgnoreLayoutCalls(ignoreLayoutCalls);
	}

	protected void restoreChartSelection() {
		if (currentChartPage != -1 && currentChartColumn != -1) {
			setCurrentChart(currentChartPage, currentChartColumn);
		}
	}

}
