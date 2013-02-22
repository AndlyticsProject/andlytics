package com.github.andlyticsproject;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.andlyticsproject.Preferences.StatsMode;
import com.github.andlyticsproject.cache.AppIconInMemoryCache;
import com.github.andlyticsproject.chart.Chart.ChartSet;
import com.github.andlyticsproject.model.Admob;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;

public class MainListAdapter extends BaseAdapter {

	private NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);

	private static int BLACK_TEXT;

	private static final int RED_TEXT = Color.RED;

	private static int BLUE_TEXT;

	public static final int TAG_IMAGE_REF = R.id.tag_mainlist_image_reference;

	public static final int TAG_IS_EXPANDED = R.id.tag_mainlist_expanded;

	private static final int TAG_ROW_ID = R.id.tag_mainlist_rowid;

	private static final int ANIMATION_DURATION = 1200;

	private List<AppInfo> appInfos;

	private LayoutInflater layoutInflater;

	private Activity activity;

	private Drawable spacerIcon;

	protected String accountname;

	private File cachDir;

	private AppIconInMemoryCache inMemoryCache;

	private List<Integer> animationList;

	private int expandViewHeight;

	private Drawable iconDown;

	private Drawable iconUp;

	private int expandMargin;

	private AccelerateInterpolator upInterpolator;

	private BounceInterpolator downInterpolator;

	private StatsMode statsMode;

	private int expandViewHeightAdmob;

	public MainListAdapter(Activity activity, String accountname, StatsMode statsMode) {
		BLACK_TEXT = activity.getResources().getColor(R.color.blackText);
		BLUE_TEXT = activity.getResources().getColor(R.color.lightBlue);
		this.setAppInfos(new ArrayList<AppInfo>());
		this.layoutInflater = activity.getLayoutInflater();
		this.activity = activity;
		this.spacerIcon = activity.getResources().getDrawable(R.drawable.app_icon_spacer);
		this.accountname = accountname;
		this.cachDir = activity.getCacheDir();
		this.inMemoryCache = AppIconInMemoryCache.getInstance();
		this.animationList = Collections.synchronizedList(new ArrayList<Integer>());
		this.upInterpolator = new AccelerateInterpolator(1.7f);
		this.downInterpolator = new BounceInterpolator();

		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		this.expandViewHeight = Math.round(metrics.scaledDensity * 150);
		this.expandViewHeightAdmob = Math.round(metrics.scaledDensity * 200);
		this.expandMargin = Math.round(metrics.scaledDensity * 5);
		this.iconDown = activity.getResources().getDrawable(R.drawable.icon_down);
		this.iconUp = activity.getResources().getDrawable(R.drawable.icon_up);

		this.setStatsMode(statsMode);
	}

	@Override
	public int getCount() {
		return appInfos.size();
	}

	@Override
	public AppInfo getItem(int position) {
		return appInfos.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

		final ViewHolder holder;

		if (convertView == null) {

			convertView = layoutInflater.inflate(R.layout.main_list_item, null);

			holder = new ViewHolder();
			holder.avgrating = (TextView) convertView.findViewById(R.id.main_app_avgrating);
			holder.avgratingPercent = (TextView) convertView
					.findViewById(R.id.main_app_avgratingPercent);
			holder.name = (TextView) convertView.findViewById(R.id.main_app_name);
			holder.packageName = (TextView) convertView.findViewById(R.id.main_package_name);

			holder.activeInstalls = (TextView) convertView
					.findViewById(R.id.main_app_activeinstalls);
			holder.activeInstallsPercent = (TextView) convertView
					.findViewById(R.id.main_app_activeinstallsPercent);

			holder.admobFrame = (View) convertView.findViewById(R.id.main_app_admob_frame);
			holder.admobRequests = (TextView) convertView
					.findViewById(R.id.main_app_admob_requests);
			holder.admobRevenue = (TextView) convertView.findViewById(R.id.main_app_admob_revenue);

			holder.ratingCount = (TextView) convertView.findViewById(R.id.main_app_rating);
			holder.ratingCountPercent = (TextView) convertView
					.findViewById(R.id.main_app_ratingPercent);
			holder.downloadsCount = (TextView) convertView.findViewById(R.id.main_app_downloads);
			holder.downloadsCountPercent = (TextView) convertView
					.findViewById(R.id.main_app_downloadsPercent);
			holder.commentsCount = (TextView) convertView.findViewById(R.id.main_app_commentscount);
			holder.commentsCountPercent = (TextView) convertView
					.findViewById(R.id.main_app_commentscountPercent);
			holder.icon = (ImageView) convertView.findViewById(R.id.main_app_icon);
			holder.expand = (ImageView) convertView.findViewById(R.id.main_app_expand_image);
			holder.ratingbar = (RatingBar) convertView.findViewById(R.id.main_app_ratingbar);
			holder.row = (RelativeLayout) convertView.findViewById(R.id.main_app_row);

			holder.ratingtext1 = (TextView) convertView.findViewById(R.id.main_app_rating_1_text);
			holder.ratingtext2 = (TextView) convertView.findViewById(R.id.main_app_rating_2_text);
			holder.ratingtext3 = (TextView) convertView.findViewById(R.id.main_app_rating_3_text);
			holder.ratingtext4 = (TextView) convertView.findViewById(R.id.main_app_rating_4_text);
			holder.ratingtext5 = (TextView) convertView.findViewById(R.id.main_app_rating_5_text);

			holder.ratings1 = (ProgressBar) convertView
					.findViewById(R.id.main_app_rating_1_rating_progressbar);
			holder.ratings2 = (ProgressBar) convertView
					.findViewById(R.id.main_app_rating_2_rating_progressbar);
			holder.ratings3 = (ProgressBar) convertView
					.findViewById(R.id.main_app_rating_3_rating_progressbar);
			holder.ratings4 = (ProgressBar) convertView
					.findViewById(R.id.main_app_rating_4_rating_progressbar);
			holder.ratings5 = (ProgressBar) convertView
					.findViewById(R.id.main_app_rating_5_rating_progressbar);

			holder.ratingpercent1 = (TextView) convertView
					.findViewById(R.id.main_app_rating_1_percent);
			holder.ratingpercent2 = (TextView) convertView
					.findViewById(R.id.main_app_rating_2_percent);
			holder.ratingpercent3 = (TextView) convertView
					.findViewById(R.id.main_app_rating_3_percent);
			holder.ratingpercent4 = (TextView) convertView
					.findViewById(R.id.main_app_rating_4_percent);
			holder.ratingpercent5 = (TextView) convertView
					.findViewById(R.id.main_app_rating_5_percent);

			holder.buttonHistory = (View) convertView.findViewById(R.id.main_app_button_history);
			holder.ratingFrame = (View) convertView.findViewById(R.id.main_app_ratingdetain_frame);
			holder.downloadFrame = (View) convertView.findViewById(R.id.main_app_download_frame);

			convertView.setTag(holder);

		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		final AppInfo appDownloadInfo = getItem(position);
		final AppStats appStats = appDownloadInfo.getLatestStats();

		int ai = appStats.getActiveInstalls();

		int td = appStats.getTotalDownloads();
		holder.avgrating.setText(appStats.getAvgRatingString());

		holder.ratingbar.setRating(appStats.getAvgRating());

		holder.activeInstalls.setText(ai + "");
		holder.downloadsCount.setText(td + "");
		holder.name.setText(appDownloadInfo.getName());

		String packageNameText = appDownloadInfo.getPackageName();
		//        if(appStats.getVersionCode() != null) {
		//            packageNameText += " - " + appStats.getVersionCode();
		//        }
		holder.packageName.setText(packageNameText);

		// Map<Integer, Integer> ratings = appDownloadInfo.getRatings();
		holder.ratingCount.setText(appStats.getRatingCount() + "");
		holder.commentsCount.setText(appStats.getNumberOfComments() + "");

		holder.ratingtext1.setText(appStats.getRating1().toString());
		holder.ratingtext2.setText(appStats.getRating2().toString());
		holder.ratingtext3.setText(appStats.getRating3().toString());
		holder.ratingtext4.setText(appStats.getRating4().toString());
		holder.ratingtext5.setText(appStats.getRating5().toString());

		int numRatings = appStats.getRatingCount();
		holder.ratings1.setMax(numRatings);
		holder.ratings1.setProgress(appStats.getRating1());
		holder.ratings2.setMax(numRatings);
		holder.ratings2.setProgress(appStats.getRating2());
		holder.ratings3.setMax(numRatings);
		holder.ratings3.setProgress(appStats.getRating3());
		holder.ratings4.setMax(numRatings);
		holder.ratings4.setProgress(appStats.getRating4());
		holder.ratings5.setMax(numRatings);
		holder.ratings5.setProgress(appStats.getRating5());

		if (statsMode.equals(StatsMode.DAY_CHANGES)) {
			setupValueDiff(holder.ratingpercent1, appStats.getRating1Diff());
			setupValueDiff(holder.ratingpercent2, appStats.getRating2Diff());
			setupValueDiff(holder.ratingpercent3, appStats.getRating3Diff());
			setupValueDiff(holder.ratingpercent4, appStats.getRating4Diff());
			setupValueDiff(holder.ratingpercent5, appStats.getRating5Diff());
			setupValueDiff(holder.ratingCountPercent, appStats.getRatingCountDiff());
			setupFloatValueDiff(holder.avgratingPercent, appStats.getAvgRatingDiff(),
					appStats.getAvgRatingDiffString());
			setupValueDiff(holder.commentsCountPercent, appStats.getNumberOfCommentsDiff());
			setupValueDiff(holder.activeInstallsPercent, appStats.getActiveInstallsDiff());
			setupValueDiff(holder.downloadsCountPercent, appStats.getDailyDownloads());

		} else {

			holder.downloadsCountPercent.setText("");
			holder.avgratingPercent.setText("");
			holder.ratingCountPercent.setText(appStats.getRatingCountPercentString() + "%");
			holder.ratingCountPercent.setTextColor(BLACK_TEXT);

			holder.ratingpercent1.setText(appStats.getRatingPercentString(1));
			holder.ratingpercent2.setText(appStats.getRatingPercentString(2));
			holder.ratingpercent3.setText(appStats.getRatingPercentString(3));
			holder.ratingpercent4.setText(appStats.getRatingPercentString(4));
			holder.ratingpercent5.setText(appStats.getRatingPercentString(5));

			holder.ratingpercent1.setTextColor(BLACK_TEXT);
			holder.ratingpercent2.setTextColor(BLACK_TEXT);
			holder.ratingpercent3.setTextColor(BLACK_TEXT);
			holder.ratingpercent4.setTextColor(BLACK_TEXT);
			holder.ratingpercent5.setTextColor(BLACK_TEXT);

			holder.commentsCountPercent.setText(appStats.getNumberOfCommentsPercentString() + "%");
			holder.commentsCountPercent.setTextColor(BLACK_TEXT);
			holder.activeInstallsPercent.setText(appStats.getActiveInstallsPercentString() + "%");
			holder.activeInstallsPercent.setTextColor(BLACK_TEXT);

		}

		int height = expandViewHeight;
		Admob admobStats = appDownloadInfo.getAdmobStats();
		if (admobStats != null) {
			height = expandViewHeightAdmob;
			holder.admobFrame.setVisibility(View.VISIBLE);
			holder.admobRevenue.setText(numberFormat.format(admobStats.getRevenue()));
			holder.admobRequests.setText(admobStats.getRequests() + "");
		} else {
			holder.admobFrame.setVisibility(View.GONE);
		}
		final int expandHeight = height;

		final String packageName = appDownloadInfo.getPackageName();

		final File iconFile = new File(cachDir + "/" + appDownloadInfo.getIconName());

		if (inMemoryCache.contains(packageName)) {

			holder.icon.setImageBitmap(inMemoryCache.get(packageName));
			holder.icon.setTag(TAG_IMAGE_REF, packageName);
			holder.icon.clearAnimation();

		} else {
			holder.icon.setTag(TAG_IMAGE_REF, packageName);
			holder.icon.setImageDrawable(null);
			holder.icon.clearAnimation();
			new GetCachedImageTask(holder.icon, appDownloadInfo.getPackageName())
					.execute(new File[] { iconFile });
		}

		holder.icon.setTag(TAG_IMAGE_REF, packageName);
		holder.row.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent intent = new Intent(activity, CommentsActivity.class);
				intent.putExtra(Constants.PACKAGE_NAME_PARCEL, packageName);
				if (iconFile.exists()) {
					intent.putExtra(Constants.ICON_FILE_PARCEL, iconFile.getAbsolutePath());
				}
				intent.putExtra(Constants.AUTH_ACCOUNT_NAME, accountname);
				intent.putExtra(Constants.DEVELOPER_ID_PARCEL, appDownloadInfo.getDeveloperId());

				activity.startActivity(intent);
				activity.overridePendingTransition(R.anim.activity_next_in,
						R.anim.activity_next_out);

			}
		});

		holder.downloadFrame.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(activity, ChartActivity.class);
				intent.putExtra(Constants.PACKAGE_NAME_PARCEL, packageName);
				intent.putExtra(Constants.CHART_NAME, R.string.downloads);
				if (iconFile.exists()) {
					intent.putExtra(Constants.ICON_FILE_PARCEL, iconFile.getAbsolutePath());
				}
				intent.putExtra(Constants.AUTH_ACCOUNT_NAME, accountname);
				intent.putExtra(Constants.CHART_SET, ChartSet.DOWNLOADS.name());
				activity.startActivity(intent);
				activity.overridePendingTransition(R.anim.activity_next_in,
						R.anim.activity_next_out);
			}
		});

		holder.admobFrame.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(activity, AdmobActivity.class);
				intent.putExtra(Constants.PACKAGE_NAME_PARCEL, packageName);
				if (iconFile.exists()) {
					intent.putExtra(Constants.ICON_FILE_PARCEL, iconFile.getAbsolutePath());
				}
				intent.putExtra(Constants.AUTH_ACCOUNT_NAME, accountname);
				activity.startActivity(intent);
				activity.overridePendingTransition(R.anim.activity_next_in,
						R.anim.activity_next_out);
			}
		});

		holder.ratingFrame.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(activity, ChartActivity.class);
				intent.putExtra(Constants.PACKAGE_NAME_PARCEL, packageName);
				intent.putExtra(Constants.CHART_NAME, R.string.ratings);
				if (iconFile.exists()) {
					intent.putExtra(Constants.ICON_FILE_PARCEL, iconFile.getAbsolutePath());
				}
				intent.putExtra(Constants.AUTH_ACCOUNT_NAME, accountname);
				intent.putExtra(Constants.CHART_SET, ChartSet.RATINGS.name());

				activity.startActivity(intent);
				activity.overridePendingTransition(R.anim.activity_next_in,
						R.anim.activity_next_out);
			}
		});

		android.widget.RelativeLayout.LayoutParams layout = ((RelativeLayout.LayoutParams) holder.ratingFrame
				.getLayoutParams());
		if (appDownloadInfo.isRatingDetailsExpanded()) {
			holder.ratingFrame.setTag(TAG_IS_EXPANDED, true);
			layout.topMargin = expandHeight;
			holder.expand.clearAnimation();
			holder.expand.setImageDrawable(iconUp);
			layout.leftMargin = 0;
			layout.rightMargin = expandMargin;
			changeBackgroundDrawable(holder.buttonHistory,
					activity.getResources().getDrawable(R.drawable.row_background_inner_borderless));
			holder.buttonHistory.setSelected(false);
			holder.buttonHistory.setPressed(false);

			// holder.expand.setVisibility(View.INVISIBLE);
		} else {
			holder.ratingFrame.setTag(TAG_IS_EXPANDED, false);
			((RelativeLayout.LayoutParams) holder.ratingFrame.getLayoutParams()).topMargin = 0;
			holder.expand.clearAnimation();
			holder.expand.setImageDrawable(iconDown);
			layout.rightMargin = 0;
			layout.leftMargin = expandMargin;
			changeBackgroundDrawable(
					holder.buttonHistory,
					activity.getResources().getDrawable(
							R.drawable.row_background_inner_borderless_bottom));
			holder.buttonHistory.setSelected(false);
			holder.buttonHistory.setPressed(false);
			// holder.expand.setVisibility(View.VISIBLE);
		}
		holder.ratingFrame.setTag(TAG_ROW_ID, position);
		holder.buttonHistory.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {

				if (!animationList.contains(position)) {
					animationList.add(position);

					boolean isExpanded = (Boolean) holder.ratingFrame.getTag(TAG_IS_EXPANDED);
					ContentAdapter db = ContentAdapter.getInstance(AndlyticsApp.getInstance());
					db.setRatingExpanded(accountname, packageName, !isExpanded);
					appDownloadInfo.setRatingDetailsExpanded(!isExpanded);

					int margin = expandMargin;

					holder.expand.setVisibility(View.VISIBLE);

					if (isExpanded) {
						holder.expand.setImageDrawable(iconDown);
						Animation rotateUp = AnimationUtils.loadAnimation(
								activity.getApplicationContext(), R.anim.rotate_up);
						rotateUp.setInterpolator(upInterpolator);

						changeBackgroundDrawable(
								v,
								activity.getResources().getDrawable(
										R.drawable.row_background_inner_borderless_bottom));
						holder.expand.startAnimation(rotateUp);

						(new ExpandAnimation(holder.ratingFrame, true, expandHeight, margin,
								position)).execute();

					} else {
						holder.expand.setImageDrawable(iconUp);
						Animation rotateDown = AnimationUtils.loadAnimation(
								activity.getApplicationContext(), R.anim.rotate_down);
						rotateDown.setFillAfter(true);
						rotateDown.setAnimationListener(new AnimationListener() {

							@Override
							public void onAnimationStart(Animation animation) {
							}

							@Override
							public void onAnimationRepeat(Animation animation) {
							}

							@Override
							public void onAnimationEnd(Animation animation) {
								TransitionDrawable out = (TransitionDrawable) activity
										.getResources().getDrawable(
												R.drawable.background_border_transition_out);
								changeBackgroundDrawable(v, out);
								out.startTransition(2000);

							}
						});
						holder.expand.startAnimation(rotateDown);

						(new ExpandAnimation(holder.ratingFrame, false, expandHeight, margin,
								position)).execute();

					}

				}

			}
		});

		return convertView;
	}

	private void setupValueDiff(TextView view, Integer diff) {

		String value = diff.toString();

		if (diff > 0) {
			view.setTextColor(BLUE_TEXT);
			value = "+" + value;
		} else if (diff < 0) {
			view.setTextColor(RED_TEXT);
		} else {
			view.setTextColor(BLACK_TEXT);
		}
		view.setText(value);

	}

	private void setupFloatValueDiff(TextView view, float diff, String diffvalue) {

		String value = diffvalue;
		if ("0.000".equals(diffvalue)) {
			value = "0";
			view.setTextColor(BLACK_TEXT);
		} else {

			if (diff > 0) {
				value = diffvalue;
				view.setTextColor(BLUE_TEXT);
				value = "+" + value;
			} else if (diff < 0) {
				value = diffvalue;
				view.setTextColor(RED_TEXT);
			}
		}

		view.setText(value);

	}

	static class ViewHolder {
		public TextView admobRevenue;
		public TextView admobRequests;
		public View admobFrame;
		public TextView downloadsCountPercent;
		public View downloadFrame;
		public ScrollView scrollview;
		public TextView ratingCountPercent;
		public TextView commentsCountPercent;
		public RelativeLayout row;
		public TextView commentsCount;
		public RatingBar ratingbar;
		public ImageView icon;
		public ImageView expand;
		public TextView ratingtext1;
		public TextView ratingtext2;
		public TextView ratingtext3;
		public TextView ratingtext4;
		public TextView ratingtext5;
		public TextView ratingpercent1;
		public TextView ratingpercent2;
		public TextView ratingpercent3;
		public TextView ratingpercent4;
		public TextView ratingpercent5;
		public ProgressBar ratings1;
		public ProgressBar ratings2;
		public ProgressBar ratings3;
		public ProgressBar ratings4;
		public ProgressBar ratings5;
		public TextView name;
		public TextView packageName;
		public TextView downloadsCount;
		public TextView ratingCount;
		public TextView activeInstallsPercent;
		public TextView activeInstalls;
		public TextView avgrating;
		public TextView avgratingPercent;
		public View buttonHistory;
		public View ratingFrame;
	}

	private class GetCachedImageTask extends AsyncTask<File, Void, Bitmap> {

		private ImageView imageView;
		private String reference;

		public GetCachedImageTask(ImageView imageView, String reference) {
			this.imageView = imageView;
			this.reference = reference;
		}

		protected void onPostExecute(Bitmap result) {

			// only update the image if tag==reference
			// (view may have been reused as convertView)
			if (imageView.getTag(TAG_IMAGE_REF).equals(reference)) {

				if (result == null) {
					imageView.setImageDrawable(spacerIcon);
				} else {
					inMemoryCache.put(reference, result);
					updateMainImage(imageView, R.anim.fade_in_fast, result);
				}
			}
		}

		@Override
		protected Bitmap doInBackground(File... params) {

			File iconFile = params[0];

			if (iconFile.exists()) {
				Bitmap bm = BitmapFactory.decodeFile(iconFile.getAbsolutePath());
				return bm;
			}
			return null;
		}
	}

	public void setAppInfos(List<AppInfo> appInfos) {
		this.appInfos = appInfos;
	}

	public List<AppInfo> getAppInfos() {
		return appInfos;
	}

	public void updateMainImage(ImageView imageView, int animationId, Bitmap result) {
		imageView.setImageBitmap(result);
		imageView.clearAnimation();
		Animation fadeInAnimation = AnimationUtils.loadAnimation(activity.getApplicationContext(),
				animationId);
		imageView.startAnimation(fadeInAnimation);
	}

	@SuppressWarnings("deprecation")
	private void changeBackgroundDrawable(final View v, Drawable drawable) {
		LayoutParams l = v.getLayoutParams();
		int paddingBottom = v.getPaddingBottom();
		int paddingLeft = v.getPaddingLeft();
		int paddingRight = v.getPaddingRight();
		int paddingTop = v.getPaddingTop();
		v.setBackgroundDrawable(drawable);
		v.setLayoutParams(l);
		v.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
	}

	public void setStatsMode(StatsMode statsMode) {
		this.statsMode = statsMode;
	}

	public StatsMode getStatsMode() {
		return statsMode;
	}

	class ExpandAnimation extends AsyncTask<Void, LayoutParams, Void> {

		private static final int SLEEP_TIME = 20;
		private View view;
		private boolean up;
		private int height;
		private int margin;
		private Integer rowId;

		public ExpandAnimation(View viewToExpand, boolean up, int height, int margin, int rowid) {
			this.view = viewToExpand;
			this.up = up;
			this.height = height;
			this.margin = margin;
			this.rowId = rowid;
		}

		@Override
		protected Void doInBackground(Void... params) {

			long startAnimation = System.currentTimeMillis() - 1;
			float animationtime = 1;

			Interpolator inter = null;
			if (up) {
				inter = upInterpolator;
			} else {
				inter = downInterpolator;
			}

			final RelativeLayout.LayoutParams layoutParams = (android.widget.RelativeLayout.LayoutParams) view
					.getLayoutParams();
			while (animationtime < ANIMATION_DURATION && (Integer) view.getTag(TAG_ROW_ID) == rowId) {

				int diffHeight = 0;
				if (up) {
					diffHeight = height
							- ((int) (height * inter.getInterpolation(animationtime
									/ ANIMATION_DURATION)));
				} else {
					diffHeight = (int) (height * inter.getInterpolation(animationtime
							/ ANIMATION_DURATION));
				}

				int diffLeft = 0;
				int diffRight = 0;
				int diffMargin = (int) (margin * inter.getInterpolation(animationtime
						/ ANIMATION_DURATION));
				if (up) {
					diffLeft = diffMargin;
					diffRight = margin - diffMargin;
				} else {
					diffLeft = margin - diffMargin;
					diffRight = diffMargin;
				}

				layoutParams.topMargin = diffHeight;
				layoutParams.leftMargin = diffLeft;
				layoutParams.rightMargin = diffRight;
				publishProgress(layoutParams);
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				animationtime = (System.currentTimeMillis()) - startAnimation;

			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			if ((Integer) view.getTag(TAG_ROW_ID) == rowId) {
				if (up) {
					final RelativeLayout.LayoutParams layoutParams = (android.widget.RelativeLayout.LayoutParams) view
							.getLayoutParams();
					layoutParams.topMargin = 0;
					layoutParams.leftMargin = margin;
					layoutParams.rightMargin = 0;

					view.setLayoutParams(layoutParams);
					view.setTag(TAG_IS_EXPANDED, false);
				} else {
					final RelativeLayout.LayoutParams layoutParams = (android.widget.RelativeLayout.LayoutParams) view
							.getLayoutParams();
					layoutParams.topMargin = height;
					layoutParams.leftMargin = 0;
					layoutParams.rightMargin = margin;
					view.setLayoutParams(layoutParams);
					view.setTag(TAG_IS_EXPANDED, true);
				}
			}
			Iterator<Integer> iterator = animationList.iterator();
			while (iterator.hasNext()) {
				if (iterator.next().equals(rowId)) {
					iterator.remove();
					break;
				}
			}
		}

		@Override
		protected void onProgressUpdate(LayoutParams... values) {
			view.setLayoutParams(values[0]);
		}
	}

}
