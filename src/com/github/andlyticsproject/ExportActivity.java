package com.github.andlyticsproject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.github.andlyticsproject.cache.AppIconInMemoryCache;
import com.github.andlyticsproject.io.ExportService;
import com.github.andlyticsproject.io.StatsCsvReaderWriter;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.util.DetachableAsyncTask;
import com.github.andlyticsproject.util.Utils;

public class ExportActivity extends SherlockFragmentActivity {

	private static final String TAG = ExportActivity.class.getSimpleName();

	public static final int TAG_IMAGE_REF = R.id.tag_mainlist_image_reference;

	public static final String EXTRA_ACCOUNT_NAME = "accountName";
	private static final String EXTRA_EXPORT_PACKAGE_NAMES = "exportPackageNames";

	private LayoutInflater layoutInflater;

	private ContentAdapter db;

	private AppIconInMemoryCache inMemoryCache;

	private File cacheDir;

	private Drawable spacerIcon;

	private ExportListAdapter adapter;
	private List<AppInfo> appInfos = new ArrayList<AppInfo>();
	private ArrayList<String> exportPackageNames = new ArrayList<String>();
	private String accountName;

	private LoadExportTask loadTask;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.export_stats);
		setProgressBarIndeterminateVisibility(false);

		layoutInflater = getLayoutInflater();
		db = ((AndlyticsApp) getApplication()).getDbAdapter();

		accountName = getIntent().getExtras().getString(EXTRA_ACCOUNT_NAME);
		getSupportActionBar().setSubtitle(accountName);

		adapter = new ExportListAdapter();
		setAppInfos(appInfos);

		if (state != null) {
			exportPackageNames = (ArrayList<String>) state
					.getSerializable(EXTRA_EXPORT_PACKAGE_NAMES);
		}

		setupView();

		this.inMemoryCache = AppIconInMemoryCache.getInstance();
		this.cacheDir = getCacheDir();
		this.spacerIcon = getResources().getDrawable(R.drawable.app_icon_spacer);

		if (getLastCustomNonConfigurationInstance() != null) {
			loadTask = (LoadExportTask) getLastCustomNonConfigurationInstance();
			loadTask.attach(this);
			setAppInfos(loadTask.getAppInfos());
		} else {
			loadTask = new LoadExportTask(this);
			Utils.execute(loadTask);
		}
	}

	private void setupView() {
		View closeButton = (View) this.findViewById(R.id.export_dialog_close_button);
		closeButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

		final Context context = this;
		View exportButton = (View) this.findViewById(R.id.export_dialog_export_button);
		exportButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!android.os.Environment.getExternalStorageState().equals(
						android.os.Environment.MEDIA_MOUNTED)) {
					Toast.makeText(context, context.getString(R.string.export_no_sdcard),
							Toast.LENGTH_LONG).show();

					return;
				}

				if (exportPackageNames.isEmpty()) {
					Toast.makeText(context, context.getString(R.string.export_no_app),
							Toast.LENGTH_LONG).show();

					return;
				}

				File exportFile = StatsCsvReaderWriter.getExportFileForAccount(Preferences
						.getAccountName(ExportActivity.this));
				if (exportFile.exists()) {
					ConfirmExportDialogFragment.newInstance(exportFile.getName()).show(
							getSupportFragmentManager(), "confirmExportDialog");
				} else {
					startExport();
				}
			}
		});

		ListView lv = (ListView) this.findViewById(R.id.list_view_id);
		lv.addHeaderView(layoutInflater.inflate(R.layout.export_list_header, null));
		lv.setAdapter(adapter);
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putSerializable(EXTRA_EXPORT_PACKAGE_NAMES, exportPackageNames);
	}


	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		return loadTask == null ? null : loadTask.detach();
	}

	ContentAdapter getDb() {
		return db;
	}

	String getAccountName() {
		return accountName;
	}

	public static class ConfirmExportDialogFragment extends DialogFragment {

		public static final String ARG_FILENAME = "filename";

		public static ConfirmExportDialogFragment newInstance(String filename) {
			ConfirmExportDialogFragment frag = new ConfirmExportDialogFragment();
			Bundle args = new Bundle();
			args.putString(ARG_FILENAME, filename);
			frag.setArguments(args);
			return frag;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final String filename = getArguments().getString(ARG_FILENAME);

			return new AlertDialog.Builder(getActivity())
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.export_confirm_dialog_title)
					.setMessage(
							getResources().getString(R.string.export_confirm_dialog_message,
									filename))
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							((ExportActivity) getActivity()).startExport();
						}
					})
					.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							dismiss();
						}
					}).create();
		}
	}


	private static class LoadExportTask extends
			DetachableAsyncTask<Void, Void, List<AppInfo>, ExportActivity> {

		List<AppInfo> appInfos = new ArrayList<AppInfo>();

		LoadExportTask(ExportActivity parent) {
			super(parent);
		}

		@Override
		protected void onPreExecute() {
			activity.setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected List<AppInfo> doInBackground(Void... params) {
			if (activity == null) {
				return null;
			}

			try {
				appInfos = activity.getDb().getAllAppsLatestStats(activity.getAccountName());

				return appInfos;
			} catch (Exception e) {
				Log.e(TAG, "Failed to get app stats: " + e.getMessage(), e);

				return null;
			}
		}

		@Override
		protected void onPostExecute(List<AppInfo> result) {
			if (activity == null) {
				return;
			}

			activity.setProgressBarIndeterminateVisibility(false);

			if (!activity.isFinishing()) {
				if (result != null) {
					activity.setAppInfos(result);
				} else {
					Toast.makeText(activity, R.string.export_failed_to_load_stats,
							Toast.LENGTH_LONG).show();
					activity.finish();
				}
			}
		}

		List<AppInfo> getAppInfos() {
			return appInfos;
		}

	}

	class ExportListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return getAppInfos().size();
		}

		@Override
		public AppInfo getItem(int position) {
			return getAppInfos().get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder;

			if (convertView == null) {

				convertView = layoutInflater.inflate(R.layout.export_list_item, null);

				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.main_app_name);
				holder.packageName = (TextView) convertView.findViewById(R.id.main_package_name);
				holder.icon = (ImageView) convertView.findViewById(R.id.main_app_icon);
				holder.row = (RelativeLayout) convertView.findViewById(R.id.main_app_row);
				holder.checkbox = (CheckBox) convertView.findViewById(R.id.ghost_checkbox);
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			final AppInfo appDownloadInfo = getItem(position);
			holder.name.setText(appDownloadInfo.getName());
			holder.packageName.setText(appDownloadInfo.getPackageName());

			final String packageName = appDownloadInfo.getPackageName();

			holder.checkbox.setChecked(exportPackageNames.contains(packageName));

			final File iconFile = new File(cacheDir + "/" + appDownloadInfo.getIconName());

			holder.icon.setTag(TAG_IMAGE_REF, packageName);
			if (inMemoryCache.contains(packageName)) {

				holder.icon.setImageBitmap(inMemoryCache.get(packageName));
				holder.icon.clearAnimation();

			} else {
				holder.icon.setImageDrawable(null);
				holder.icon.clearAnimation();
				if (appDownloadInfo.getPackageName().startsWith("com.github.andlyticsproject.demo")) {

					holder.icon.setImageDrawable(getResources().getDrawable(
							R.drawable.default_app_icon));

				} else {

					new GetCachedImageTask(holder.icon, appDownloadInfo.getPackageName())
							.execute(new File[] { iconFile });
				}
			}

			holder.row.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {

					CheckBox checkbox = ((CheckBox) (((ViewGroup) v)
							.findViewById(R.id.ghost_checkbox)));
					checkbox.setChecked(!checkbox.isChecked());

					if (checkbox.isChecked()) {
						exportPackageNames.add(appDownloadInfo.getPackageName());
					} else {
						exportPackageNames.remove(appDownloadInfo.getPackageName());
					}

				}
			});

			holder.checkbox.setTag(packageName);
			holder.checkbox.setOnClickListener(new CheckBox.OnClickListener() {

				@Override
				public void onClick(View v) {
					boolean checked = ((CheckBox) v).isChecked();
					if (checked) {
						exportPackageNames.add(appDownloadInfo.getPackageName());
					} else {
						exportPackageNames.remove(appDownloadInfo.getPackageName());
					}

				}
			});

			return convertView;
		}

		private class ViewHolder {
			public RelativeLayout row;
			public TextView name;
			public TextView packageName;
			public ImageView icon;
			public CheckBox checkbox;
		}
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

	public void updateMainImage(ImageView imageView, int animationId, Bitmap result) {
		imageView.setImageBitmap(result);
		imageView.clearAnimation();
		Animation fadeInAnimation = AnimationUtils.loadAnimation(getApplicationContext(),
				animationId);
		imageView.startAnimation(fadeInAnimation);
	}

	public void setAppInfos(List<AppInfo> appInfos) {
		this.appInfos = appInfos;
		if (adapter != null) {
			this.adapter.notifyDataSetChanged();
		}
	}

	public List<AppInfo> getAppInfos() {
		return appInfos;
	}

	private void startExport() {
		Intent exportIntent = new Intent(this, ExportService.class);
		exportIntent.putExtra(ExportService.EXTRA_PACKAGE_NAMES,
				exportPackageNames.toArray(new String[exportPackageNames.size()]));
		exportIntent.putExtra(ExportService.EXTRA_ACCOUNT_NAME, accountName);
		startService(exportIntent);

		finish();
	}
}
