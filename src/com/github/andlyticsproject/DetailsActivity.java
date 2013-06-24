package com.github.andlyticsproject;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.model.AppStatsList;
import com.github.andlyticsproject.util.DetachableAsyncTask;
import com.github.andlyticsproject.util.Utils;

public class DetailsActivity extends BaseActivity implements ChartFragment.DetailedStatsActivity {

	private static final String TAG = DetailsActivity.class.getSimpleName();
	private static final boolean DEBUG = true;

	private ViewGroup content;

	private boolean smoothEnabled;
	private Timeframe currentTimeFrame = Timeframe.MONTH_TO_DATE;

	private AppStatsList statsForApp;
	private List<Date> versionUpdateDates;

	private String appName;

	public static class TabListener<T extends ChartFragmentBase> implements ActionBar.TabListener {

		private ChartFragment fragment;
		private DetailsActivity activity;
		private String tag;
		private Class<T> clazz;

		public TabListener(DetailsActivity activity, String tag, Class<T> clz) {
			this.activity = activity;
			this.tag = tag;
			this.clazz = clz;
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			if (fragment == null) {
				fragment = (ChartFragment) Fragment.instantiate(activity, clazz.getName());
				ft.add(android.R.id.content, fragment, tag);
			} else {
				ft.show(fragment);
				activity.setTitle(fragment.getTitle());
			}
			if (activity.statsForApp != null && activity.versionUpdateDates != null) {
				fragment.updateView(activity.statsForApp, activity.versionUpdateDates);
			}
			if (activity.appName != null) {
				activity.getSupportActionBar().setSubtitle(activity.appName);
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (fragment != null) {
				ft.hide(fragment);
			}
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		smoothEnabled = Preferences.getChartSmooth(this);
		appName = getDbAdapter().getAppName(packageName);

		content = (ViewGroup) findViewById(R.id.details_content);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		Tab tab = actionBar
				.newTab()
				.setText(R.string.ratings)
				.setTabListener(
						new TabListener<RatingsFragment>(this, "ratings_tab", RatingsFragment.class));
		actionBar.addTab(tab);

		tab = actionBar
				.newTab()
				.setText(R.string.downloads)
				.setTabListener(
						new TabListener<DownloadsFragment>(this, "downloads_tab",
								DownloadsFragment.class));
		actionBar.addTab(tab);

		tab = actionBar
				.newTab()
				.setText(R.string.revenue)
				.setTabListener(
						new TabListener<RevenueFragment>(this, "revenue_tab", RevenueFragment.class));
		actionBar.addTab(tab);

		tab = actionBar
				.newTab()
				.setText(R.string.admob)
				.setTabListener(
						new TabListener<AdmobFragment>(this, "admob_tab", AdmobFragment.class));
		actionBar.addTab(tab);

		if (getLastCustomNonConfigurationInstance() != null) {
			loadChartData = (LoadChartData) getLastCustomNonConfigurationInstance();
			loadChartData.attach(this);
			if (loadChartData.statsForApp != null && loadChartData.versionUpdateDates != null) {
				updateView(loadChartData.statsForApp, loadChartData.versionUpdateDates);
				dataUpdateRequested = false;
			}
		}
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		return loadChartData == null ? null : loadChartData.detach();
	}

	@Override
	protected void onResume() {
		super.onResume();

		dataUpdateRequested = shouldRemoteUpdateStats();

		executeLoadDataDefault();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Context ctx = this;

		switch (item.getItemId()) {
		case android.R.id.home:
			// XXX finish?!
			finish();
			overridePendingTransition(R.anim.activity_prev_in, R.anim.activity_prev_out);
			return true;
		case R.id.itemChartsmenuRefresh:
			//			setChartIgnoreCallLayouts(true);
			executeLoadData(currentTimeFrame);
			return true;
		case R.id.itemChartsmenuToggle:
			//			toggleChartData(item);
			return true;
		case R.id.itemChartsmenuTimeframe7:
			currentTimeFrame = Timeframe.LAST_SEVEN_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_SEVEN_DAYS, ctx);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframe30:
			currentTimeFrame = Timeframe.LAST_THIRTY_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_THIRTY_DAYS, ctx);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframe90:
			currentTimeFrame = Timeframe.LAST_NINETY_DAYS;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.LAST_NINETY_DAYS, ctx);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframeUnlimited:
			currentTimeFrame = Timeframe.UNLIMITED;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.UNLIMITED, ctx);
			item.setChecked(true);
			return true;
		case R.id.itemChartsmenuTimeframeMonthToDate:
			currentTimeFrame = Timeframe.MONTH_TO_DATE;
			executeLoadData(currentTimeFrame);
			Preferences.saveChartTimeframe(Timeframe.MONTH_TO_DATE, ctx);
			item.setChecked(true);
			return true;
		default:
			return (super.onOptionsItemSelected(item));
		}
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
		case MONTH_TO_DATE:
			activeTimeFrame = menu.findItem(R.id.itemChartsmenuTimeframeMonthToDate);
			break;
		}
		activeTimeFrame.setChecked(true);

		if (isRefreshing()) {
			menu.findItem(R.id.itemChartsmenuRefresh).setActionView(
					R.layout.action_bar_indeterminate_progress);
		}

		return true;
	}

	private LoadChartData loadChartData;
	private boolean dataUpdateRequested;

	protected void executeLoadData(Timeframe timeFrame) {
		if (loadChartData != null) {
			loadChartData.detach();
		}
		// reload since time frame has changed
		dataUpdateRequested = true;
		loadChartData = new LoadChartData(this);
		Utils.execute(loadChartData, timeFrame);

	}

	private void executeLoadDataDefault() {
		if (loadChartData != null) {
			loadChartData.detach();
		}
		loadChartData = new LoadChartData(this);
		Utils.execute(loadChartData, getCurrentTimeFrame());

	}

	public Timeframe getCurrentTimeFrame() {
		return currentTimeFrame;
	}

	public void setCurrentTimeFrame(Timeframe currentTimeFrame) {
		this.currentTimeFrame = currentTimeFrame;
	}


	private static class LoadChartData extends
			DetachableAsyncTask<Timeframe, Void, Boolean, DetailsActivity> {

		private ContentAdapter db;

		LoadChartData(DetailsActivity activity) {
			super(activity);
			db = ContentAdapter.getInstance(activity.getApplication());
		}

		private AppStatsList statsForApp;
		private List<Date> versionUpdateDates;

		@Override
		protected void onPreExecute() {
			if (activity == null) {
				return;
			}

			activity.refreshStarted();
		}

		@Override
		protected Boolean doInBackground(Timeframe... params) {
			if (activity == null) {
				return null;
			}

			if (activity.dataUpdateRequested || activity.statsForApp == null
					|| activity.versionUpdateDates.isEmpty()) {
				statsForApp = db.getStatsForApp(activity.packageName, params[0],
						activity.smoothEnabled);
				versionUpdateDates = db.getVersionUpdateDates(activity.packageName);
				// XXX
				activity.statsForApp = statsForApp;
				activity.versionUpdateDates = versionUpdateDates;

				if (DEBUG) {
					Log.d(TAG,
							"statsForApp::highestRatingChange "
									+ statsForApp.getHighestRatingChange());
					Log.d(TAG,
							"statsForApp::lowestRatingChanage "
									+ statsForApp.getLowestRatingChange());
					Log.d(TAG, "statsForApp::appStats " + statsForApp.getAppStats().size());
					Log.d(TAG, "statsForApps::overall " + statsForApp.getOverall());
					Log.d(TAG, "versionUpdateDates " + versionUpdateDates.size());
				}

				activity.dataUpdateRequested = false;

				return true;
			}

			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (activity == null) {
				return;
			}

			if (result && statsForApp != null && versionUpdateDates != null) {
				activity.updateView(statsForApp, versionUpdateDates);
			}
			activity.refreshFinished();
		}

	}

	public void updateView(AppStatsList statsForApp, List<Date> versionUpdateDates) {
		// XXX is there a better way?
		String[] tabTags = { "ratings_tab", "downloads_tab", "revenue_tab" };
		String tabTag = tabTags[getSupportActionBar().getSelectedNavigationIndex()];
		ChartFragment chartFargment = (ChartFragment) getSupportFragmentManager()
				.findFragmentByTag(tabTag);
		if (chartFargment != null) {
			chartFargment.updateView(statsForApp, versionUpdateDates);
		}
	}

	@Override
	public AppStatsList getStatsForApp() {
		return statsForApp;
	}

	@Override
	public List<Date> getVersionUpdateDates() {
		return versionUpdateDates;
	}


}
