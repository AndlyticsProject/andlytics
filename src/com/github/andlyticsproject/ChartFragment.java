package com.github.andlyticsproject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.AppStatsList;


public abstract class ChartFragment extends ChartFragmentBase {

	private OnChartUpdatedListener mListener;

	private ListView historyList;
	private ChartListAdapter historyListAdapter;
	private TextView historyListFooter;
	private View oneEntryHint;
	private boolean dataUpdateRequested;

	protected ChartSet currentChartSet;
	private Boolean smoothEnabled;


	public ChartFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Bundle b = getArguments();
		if (b != null) {
			String chartSet = b.getString(Constants.CHART_SET);
			if (chartSet != null) {
				currentChartSet = ChartSet.valueOf(chartSet);
			}
		}

		currentChartSet = getChartSet();
		getSherlockActivity().getSupportActionBar().setTitle(getTitle());
	}

	public abstract ChartSet getChartSet();

	public abstract String getTitle();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		oneEntryHint = (View) view.findViewById(R.id.base_chart_one_entry_hint);

		historyList = (ListView) view.findViewById(R.id.base_chart_list);
		// XXX
		//		View inflate = getSupportLayoutInflater().inflate(R.layout.chart_list_footer, null);
		//		historyListFooter = (TextView) inflate.findViewById(R.id.chart_footer_text);
		//		historyList.addFooterView(inflate, null, false);

		historyListAdapter = new ChartListAdapter(getActivity());
		setAdapter(historyListAdapter);

		historyListAdapter.setCurrentChart(currentChartSet.ordinal(), 1);
		setAllowChangePageSliding(false);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		updateView(mListener.getStatsForApp(), mListener.getVersionUpdateDates());
		// XXX
		//		ContentAdapter db = ContentAdapter.getInstance(getActivity().getApplication());
		//		String packageName = "org.nick.kanjirecognizer";
		//		boolean smoothEnabled = true;
		//		Timeframe currentTimeFrame = Timeframe.MONTH_TO_DATE;
		//		AppStatsList statsForApp = db.getStatsForApp(packageName, currentTimeFrame, smoothEnabled);
		//		List<Date> versionUpdateDates = db.getVersionUpdateDates(packageName);
		//		updateView(statsForApp, versionUpdateDates);
	}

	public void updateView(AppStatsList appStatsList, List<Date> versionUpdateDates) {
		if (appStatsList == null || versionUpdateDates == null) {
			return;
		}

		if (historyListAdapter == null) {
			return;
		}

		List<AppStats> statsForApp = appStatsList.getAppStats();
		if (statsForApp != null && statsForApp.size() > 0) {
			boolean smoothedValues = applySmoothedValues(statsForApp);
			historyListAdapter.setOverallStats(appStatsList.getOverall());
			historyListAdapter.setHeighestRatingChange(appStatsList.getHighestRatingChange());
			historyListAdapter.setLowestRatingChange(appStatsList.getLowestRatingChange());

			updateCharts(statsForApp);

			DateFormat dateFormat = Preferences.getDateFormatLong(getActivity());
			timetext = dateFormat.format(statsForApp.get(0).getRequestDate()) + " - "
					+ dateFormat.format(statsForApp.get(statsForApp.size() - 1).getRequestDate());

			updateChartHeadline();

			// make a shallow copy, otherwise original data can't be used to
			// restore state
			List<AppStats> statsForAppReversed = new ArrayList<AppStats>();
			statsForAppReversed.addAll(statsForApp);
			Collections.reverse(statsForAppReversed);
			historyListAdapter.setDownloadInfos(statsForAppReversed);
			historyListAdapter.setVersionUpdateDates(versionUpdateDates);
			/*
			 * int page=historyListAdapter.getCurrentPage(); int
			 * column=historyListAdapter.getCurrentColumn();
			 * historyListAdapter.setCurrentChart(page, column);
			 */
			historyListAdapter.notifyDataSetChanged();

			//			if (smoothedValues && currentChartSet.equals(ChartSet.DOWNLOADS)) {
			//				historyListFooter.setVisibility(View.VISIBLE);
			//			} else {
			//				historyListFooter.setVisibility(View.INVISIBLE);
			//			}

			if (oneEntryHint != null) {
				if (statsForApp.size() == 1) {
					oneEntryHint.setVisibility(View.VISIBLE);
				} else {
					oneEntryHint.setVisibility(View.INVISIBLE);
				}
			}

			// chartFrame.showNext();

		}
	}

	private static boolean applySmoothedValues(List<AppStats> statsForApp) {
		for (AppStats appInfo : statsForApp) {
			if (appInfo.isSmoothingApplied()) {
				return true;
			}
		}

		return false;
	}

	protected String getChartHint() {
		// XXX abstract
		return "Revenue";
	}

	// TODO: Rename method, update argument and hook method into UI event
	public void onButtonPressed(Uri uri) {
		if (mListener != null) {
			//			mListener.onFragmentInteraction(uri);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnChartUpdatedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	public interface OnChartUpdatedListener {

		public AppStatsList getStatsForApp();

		public List<Date> getVersionUpdateDates();
	}

	@Override
	protected void notifyChangedDataformat() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void executeLoadData(Timeframe currentTimeFrame) {
		// TODO Auto-generated method stub

	}

	public void setCurrentChartSet(ChartSet currentChartSet) {
		this.currentChartSet = currentChartSet;
	}

	public ChartSet getCurrentChartSet() {
		return currentChartSet;
	}


}
