package com.github.andlyticsproject.legacy;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.BaseActivity;
import com.github.andlyticsproject.Preferences;
import com.github.andlyticsproject.R;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.db.AndlyticsDb;

/**
 * A base class for the details activities (Comments, Downloads etc...)
 */
public class BaseDetailsActivity extends BaseActivity {

	private View downloadsButton;
	private View admobButton;
	private View commentsButton;
	private View ratingsButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		String appName = getDbAdapter().getAppName(packageName);
		if (appName != null) {
			getSupportActionBar().setSubtitle(appName);
		}

		if (iconFilePath != null) {
			Bitmap bm = BitmapFactory.decodeFile(iconFilePath);
			BitmapDrawable icon = new BitmapDrawable(getResources(), bm);
			getSupportActionBar().setIcon(icon);
		}

	}

	@Override
	protected void onResume() {
		setupTabbar();
		super.onResume();
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
		case android.R.id.home:
			finish();
			overridePendingTransition(R.anim.activity_prev_in, R.anim.activity_prev_out);
			return true;
		default:
			return (super.onOptionsItemSelected(item));
		}
	}

	// TODO Replace these buttons with a modern tab bar/action bar tabs
	public void updateTabbarButtons() {
		if (this instanceof ChartActivity) {
			ChartSet currentChartSet = ((ChartActivity) this).getCurrentChartSet();
			if (currentChartSet.equals(ChartSet.DOWNLOADS)) {
				downloadsButton.setSelected(true);
				commentsButton.setSelected(false);
				admobButton.setSelected(false);
				ratingsButton.setSelected(false);

			} else if (currentChartSet.equals(ChartSet.RATINGS)) {

				downloadsButton.setSelected(false);
				commentsButton.setSelected(false);
				ratingsButton.setSelected(true);
				admobButton.setSelected(false);
			}
		} else if (this instanceof CommentsActivity) {
			commentsButton.setSelected(true);
			admobButton.setSelected(false);
			downloadsButton.setSelected(false);
			ratingsButton.setSelected(false);
		} else if (this instanceof AdmobActivity) {
			commentsButton.setSelected(false);
			admobButton.setSelected(true);
			downloadsButton.setSelected(false);
			ratingsButton.setSelected(false);
		}
	}

	public void setupTabbar() {

		downloadsButton = findViewById(R.id.tabbar_button_downloads);
		ratingsButton = findViewById(R.id.tabbar_button_ratings);
		commentsButton = findViewById(R.id.tabbar_button_comments);
		admobButton = findViewById(R.id.tabbar_button_back);

		// Check if AdMob is configured for this app
		String[] admobDetails = AndlyticsDb.getInstance(this).getAdmobDetails(packageName);
		boolean admobConfigured = admobDetails != null;
		if (!admobConfigured && Preferences.getHideAdmobForUnconfiguredApps(this)) {
			admobButton.setVisibility(View.GONE);
		}

		updateTabbarButtons();

		downloadsButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (!(BaseDetailsActivity.this instanceof ChartActivity && ((ChartActivity) BaseDetailsActivity.this)
						.getCurrentChartSet().equals(ChartSet.DOWNLOADS))) {

					commentsButton.setSelected(false);
					admobButton.setSelected(false);
					ratingsButton.setSelected(false);
					downloadsButton.setSelected(true);

					startChartActivity(ChartSet.DOWNLOADS);
					finish();
				}

			}
		});

		ratingsButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (!(BaseDetailsActivity.this instanceof ChartActivity && ((ChartActivity) BaseDetailsActivity.this)
						.getCurrentChartSet().equals(ChartSet.RATINGS))) {

					commentsButton.setSelected(false);
					admobButton.setSelected(false);
					ratingsButton.setSelected(true);
					downloadsButton.setSelected(false);
					startChartActivity(ChartSet.RATINGS);
					finish();
				}

			}
		});

		commentsButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!(BaseDetailsActivity.this instanceof CommentsActivity)) {

					downloadsButton.setSelected(false);
					admobButton.setSelected(false);
					ratingsButton.setSelected(false);
					commentsButton.setSelected(true);
					startActivity(CommentsActivity.class, true, false);
					finish();
				}

			}
		});

		admobButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!(BaseDetailsActivity.this instanceof AdmobActivity)) {

					downloadsButton.setSelected(false);
					admobButton.setSelected(true);
					ratingsButton.setSelected(false);
					commentsButton.setSelected(false);
					startActivity(AdmobActivity.class, true, false);
					finish();
				}
			}
		});
	}

	@Override
	public void onBackPressed() {
		finish();
		overridePendingTransition(R.anim.activity_prev_in, R.anim.activity_prev_out);
	}

}
