package com.github.andlyticsproject;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.andlyticsproject.console.v2.DevConsoleRegistry;
import com.github.andlyticsproject.console.v2.DevConsoleV2;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.model.Comment;
import com.github.andlyticsproject.util.DetachableAsyncTask;
import com.github.andlyticsproject.util.Utils;

public class DetailsActivity extends BaseActivity implements DetailedStatsActivity, CommentReplier,
		ChartSwitcher {

	private static final String TAG = DetailsActivity.class.getSimpleName();

	public static final String EXTRA_CHART_SET = "com.github.andlyticsproject.chartset";
	public static final String EXTRA_CHART_NAME = "com.github.andlyticsproject.chartname";

	private static final String REPLY_DIALOG_FRAGMENT = "reply_dialog_fragment";

	private static final String[] TAB_TAGS = { "comments_tab", "ratings_tab", "downloads_tab",
			"revenue_tab", "admob_tab" };

	public static String EXTRA_SELECTED_TAB_IDX = "selectedTabIdx";
	public static int TAB_IDX_COMMENTS = 0;
	public static int TAB_IDX_RATINGS = 1;
	public static int TAB_IDX_DOWNLOADS = 2;
	public static int TAB_IDX_REVENUE = 3;
	public static int TAB_IDX_ADMOB = 4;

	public static String EXTRA_HAS_REVENUE = "com.github.andlyticsproject.hasRevenue";

	private String appName;
	private boolean hasRevenue;

	public static class TabListener<T extends StatsView<?>> implements ActionBar.TabListener {

		private Fragment fragment;
		private DetailsActivity activity;
		private String tag;
		private Class<T> clazz;

		public TabListener(DetailsActivity activity, String tag, Class<T> clz) {
			this.activity = activity;
			this.tag = tag;
			this.clazz = clz;

			fragment = activity.getSupportFragmentManager().findFragmentByTag(tag);
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			if (fragment == null) {
				fragment = Fragment.instantiate(activity, clazz.getName());
				ft.add(android.R.id.content, fragment, tag);
			} else {
				ft.attach(fragment);
			}

			activity.getSupportActionBar().setTitle(((StatsView<?>) fragment).getTitle());
			if (activity.appName != null) {
				activity.getSupportActionBar().setSubtitle(activity.appName);
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (fragment != null) {
				ft.detach(fragment);
			}
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		//		FragmentManager.enableDebugLogging(true);

		super.onCreate(savedInstanceState);

		appName = getDbAdapter().getAppName(packageName);
		hasRevenue = getIntent().getBooleanExtra(EXTRA_HAS_REVENUE, true);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		Tab tab = actionBar
				.newTab()
				.setText(R.string.comments)
				.setTabListener(
						new TabListener<CommentsFragment>(this, "comments_tab",
								CommentsFragment.class));
		actionBar.addTab(tab);

		tab = actionBar
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

		if (hasRevenue) {
			tab = actionBar
					.newTab()
					.setText(R.string.revenue)
					.setTabListener(
							new TabListener<RevenueFragment>(this, "revenue_tab",
									RevenueFragment.class));
			actionBar.addTab(tab);
		}

		// Check if AdMob is configured for this app
		String[] admobDetails = AndlyticsDb.getInstance(this).getAdmobDetails(packageName);
		boolean admobConfigured = admobDetails != null;
		if (admobConfigured || !Preferences.getHideAdmobForUnconfiguredApps(this)) {
			tab = actionBar
					.newTab()
					.setText(R.string.admob)
					.setTabListener(
							new TabListener<AdmobFragment>(this, "admob_tab", AdmobFragment.class));
			actionBar.addTab(tab);
		}

		int selectedTabIdx = getIntent().getExtras().getInt(EXTRA_SELECTED_TAB_IDX, 0);
		// FIXME This is a hack to select AdMob in the case that the revenue tab isn't enabled
		if (!hasRevenue && selectedTabIdx == TAB_IDX_ADMOB) {
			selectedTabIdx = TAB_IDX_REVENUE;
		}
		if (savedInstanceState != null) {
			selectedTabIdx = savedInstanceState.getInt(EXTRA_SELECTED_TAB_IDX, 0);
		}
		if (selectedTabIdx < actionBar.getTabCount()) {
			actionBar.setSelectedNavigationItem(selectedTabIdx);
		} else {
			actionBar.setSelectedNavigationItem(0);
		}
	}

    @Override
    public void setActionBarTitle(String title) {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(title);
    }

	@Override
	protected void onResume() {
		super.onResume();
	}

	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putInt(EXTRA_SELECTED_TAB_IDX, getSupportActionBar().getSelectedNavigationIndex());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// XXX finish?!
			finish();
			overridePendingTransition(R.anim.activity_prev_in, R.anim.activity_prev_out);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void showReplyDialog(Comment comment) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(REPLY_DIALOG_FRAGMENT);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		ReplyDialog replyDialog = new ReplyDialog();

		Bundle args = new Bundle();
		args.putString(ReplyDialog.ARG_UNIQUE_ID, comment.getUniqueId());
		args.putString(ReplyDialog.ARG_REPLY, comment.getReply() == null ? "" : comment.getReply()
				.getText());

		replyDialog.setArguments(args);

		replyDialog.show(ft, REPLY_DIALOG_FRAGMENT);
	}

	public void hideReplyDialog() {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment dialog = getSupportFragmentManager().findFragmentByTag(REPLY_DIALOG_FRAGMENT);
		if (dialog != null) {
			ft.remove(dialog);
			ft.commit();
		}
	}

	public void replyToComment(final String commentUniqueId, final String replyText) {
		Utils.execute(new DetachableAsyncTask<Void, Void, Comment, DetailsActivity>(this) {

			Exception error;

			@Override
			protected void onPreExecute() {
				if (activity == null) {
					return;
				}

				activity.refreshStarted();
			}

			@Override
			protected Comment doInBackground(Void... arg0) {
				if (activity == null) {
					return null;
				}

				try {
					DevConsoleV2 devConsole = DevConsoleRegistry.getInstance().get(accountName);

					return devConsole.replyToComment(DetailsActivity.this, packageName,
							developerId, commentUniqueId, replyText);
				} catch (Exception e) {
					error = e;
					return null;
				}
			}

			@Override
			protected void onPostExecute(Comment reply) {
				if (activity == null) {
					return;
				}

				activity.refreshFinished();

				if (error != null) {
					Log.e(TAG, "Error replying to comment: " + error.getMessage(), error);
					activity.hideReplyDialog();
					activity.handleUserVisibleException(error);

					return;
				}

				Toast.makeText(activity, R.string.reply_sent, Toast.LENGTH_LONG).show();

				CommentsFragment commentsFargment = (CommentsFragment) getSupportFragmentManager()
						.findFragmentByTag("comments_tab");
				if (commentsFargment != null) {
					commentsFargment.refreshComments();
				}
			}
		});
	}

	@Override
	public void setCurrentChart(int currentPage, int column) {
		String tabTag = TAB_TAGS[getSupportActionBar().getSelectedNavigationIndex()];
		StatsView<?> chartFargment = (StatsView<?>) getSupportFragmentManager().findFragmentByTag(
				tabTag);
		if (chartFargment != null) {
			chartFargment.setCurrentChart(currentPage, column);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
			if (resultCode != Activity.RESULT_OK) {
				checkGooglePlayServicesAvailable();
			}
		} else if (requestCode == REQUEST_AUTHORIZATION) {
			if (resultCode == Activity.RESULT_OK) {
				if (getSupportActionBar().getSelectedNavigationIndex() == TAB_IDX_ADMOB) {
					String tabTag = TAB_TAGS[getSupportActionBar().getSelectedNavigationIndex()];
					AdmobFragment admobFragment = (AdmobFragment) getSupportFragmentManager()
							.findFragmentByTag(tabTag);
					admobFragment.loadAdUnits();
				}
			} else {
				Toast.makeText(this, getString(R.string.account_authorization_denied, accountName),
						Toast.LENGTH_LONG).show();
			}
		}
	}
}
