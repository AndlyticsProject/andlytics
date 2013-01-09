package com.github.andlyticsproject.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.R;

public class AboutActivity extends SherlockFragmentActivity implements
		ActionBar.TabListener {
	private static final String BUNDLE_KEY_TABINDEX = "tabindex";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		setContentView(R.layout.about_navigation);

		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		ActionBar.Tab tab1 = getSupportActionBar().newTab();
		tab1.setText(R.string.about_credits);
		tab1.setTabListener(this);

		ActionBar.Tab tab2 = getSupportActionBar().newTab();
		tab2.setText(R.string.changelog_title);
		tab2.setTabListener(this);

		getSupportActionBar().addTab(tab1);
		getSupportActionBar().addTab(tab2);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt(BUNDLE_KEY_TABINDEX, getSupportActionBar()
				.getSelectedTab().getPosition());
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		getSupportActionBar().setSelectedNavigationItem(
				savedInstanceState.getInt(BUNDLE_KEY_TABINDEX));
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction transaction) {
		Log.i("Tab Reselected", tab.getText().toString());
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction transaction) {
		if (0 == tab.getPosition()) {
			AboutFragment fragment = new AboutFragment();
			transaction.replace(android.R.id.content, fragment);
		} else {
			ChangelogFragment fragment = new ChangelogFragment();
			transaction.replace(android.R.id.content, fragment);
		}
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
		Log.i("Tab Unselected", tab.getText().toString());
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onOpenGoogleplusClick(View view) {
		openBrowser(this.getString(R.string.googleplus_url));
	}

	public void onOpenGithubClick(View view) {
		openBrowser(this.getString(R.string.github_url));
	}
	
	public void onOpenFeedbackClick(View view) {
		openBrowser(this.getString(R.string.github_issues_url));
	}

	public void onOpenFacebookClick(View view) {
		openBrowser(this.getString(R.string.facebook_url));
	}

	public void onOpenTwitterClick(View view) {
		openBrowser(this.getString(R.string.twitter_url));
	}

	private void openBrowser(String url) {
		startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));
	}
}
