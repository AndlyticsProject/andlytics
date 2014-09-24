package com.github.andlyticsproject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.andlyticsproject.io.ImportService;
import com.github.andlyticsproject.io.ServiceException;
import com.github.andlyticsproject.io.StatsCsvReaderWriter;
import com.github.andlyticsproject.util.DetachableAsyncTask;
import com.github.andlyticsproject.util.FileUtils;
import com.github.andlyticsproject.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportActivity extends Activity {

	private static final String TAG = ImportActivity.class.getSimpleName();

	public static final int TAG_IMAGE_REF = R.id.tag_mainlist_image_reference;

	private static final String EXTRA_IMPORT_FILENAMES = "importFilenames";

	private ImportListAdapter adapter;

	private LayoutInflater layoutInflater;

	private ArrayList<String> importFilenames = new ArrayList<String>();

	private String accountName;

	private ListView listView;

	private LoadImportDialogTask loadTask;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.import_stats);
		setProgressBarIndeterminateVisibility(false);

		layoutInflater = getLayoutInflater();

		accountName = getAccountName();
		getActionBar().setSubtitle(accountName);

		if (state != null) {
			importFilenames = (ArrayList<String>) state.getSerializable(EXTRA_IMPORT_FILENAMES);
		}

		setupViews();

		if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			Uri data = getIntent().getData();
			if (data == null) {
				Toast.makeText(this, getString(R.string.stats_file_not_specified_error),
						Toast.LENGTH_LONG).show();
				finish();
			}

			if (getLastNonConfigurationInstance() != null) {
				loadTask = (LoadImportDialogTask) getLastNonConfigurationInstance();
				loadTask.attach(this);
				setFilenames(loadTask.getFilenames());
			} else {
				loadTask = new LoadImportDialogTask(this, accountName);
				Utils.execute(loadTask, data);
			}
		} else {
			Log.w(TAG, "Don't know how to handle this action: " + getIntent().getAction());
			finish();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putSerializable(EXTRA_IMPORT_FILENAMES, importFilenames);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return loadTask == null ? null : loadTask.detach();
	}

	private void setupViews() {
		View closeButton = (View) this.findViewById(R.id.import_dialog_close_button);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		View importButton = (View) this.findViewById(R.id.import_dialog_import_button);
		importButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (importFilenames.isEmpty()) {
					Toast.makeText(ImportActivity.this, getString(R.string.import_no_app),
							Toast.LENGTH_LONG).show();
					return;
				}

				ConfirmImportDialogFragment.newInstance(adapter.getCount()).show(
						getFragmentManager(), "confirmImportDialog");
			}
		});

		listView = (ListView) this.findViewById(R.id.list_view_id);
		listView.addHeaderView(layoutInflater.inflate(R.layout.import_list_header, null), null,
				false);
		setFilenames(new ArrayList<String>());
	}

	void setFilenames(List<String> filenames) {
		adapter = new ImportListAdapter(filenames);
		listView.setAdapter(adapter);
	}

	private String getAccountName() {
		String ownerAccount = StatsCsvReaderWriter.getAccountNameForExport(new File(getIntent()
				.getData().getPath()).getName());
		if (ownerAccount == null) {
			// fall back to value from preferences
			// XXX should we give a choice instead?
			ownerAccount = DeveloperAccountManager.getInstance(this).getSelectedDeveloperAccount()
					.getName();
		}
		return ownerAccount;
	}

	private void startImport() {
		Intent intent = new Intent(ImportActivity.this, ImportService.class);
		intent.setData(getIntent().getData());
		intent.putExtra(ImportService.FILE_NAMES,
				importFilenames.toArray(new String[importFilenames.size()]));
		intent.putExtra(ImportService.ACCOUNT_NAME, accountName);
		startService(intent);
		finish();
	}

	public static class ConfirmImportDialogFragment extends DialogFragment {

		public static final String ARG_NUM_APPS = "numApps";

		public static ConfirmImportDialogFragment newInstance(int numExistingApps) {
			ConfirmImportDialogFragment frag = new ConfirmImportDialogFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_NUM_APPS, numExistingApps);
			frag.setArguments(args);
			return frag;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final int numExistingApps = getArguments().getInt(ARG_NUM_APPS);

			return new AlertDialog.Builder(getActivity())
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.import_confirm_dialog_title)
					.setMessage(
							getResources().getString(R.string.import_confirm_dialog_message,
									numExistingApps))
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							((ImportActivity) getActivity()).startImport();
						}
					})
					.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							dismiss();
						}
					}).create();
		}
	}


	private static class LoadImportDialogTask extends
			DetachableAsyncTask<Uri, Void, Boolean, ImportActivity> {

		ContentAdapter db;
		String accountName;

		LoadImportDialogTask(ImportActivity parent, String accountName) {
			super(parent);
			this.accountName = accountName;
			db = ContentAdapter.getInstance(parent.getApplication());
		}

		private List<String> filenames = new ArrayList<String>();
		private Uri zipFileUri;

		@Override
		protected void onPreExecute() {
			activity.setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected Boolean doInBackground(Uri... params) {
			if (activity == null) {
				return false;
			}

			zipFileUri = params[0];
			List<String> pacakgeNames = db.getPackagesForAccount(accountName);
			try {
				String zipFilePath = zipFileUri.getPath();
				File tempZip = null;
				try {
					if (!zipFileUri.getScheme().equalsIgnoreCase("file")) {
						tempZip = copyToTempFile();
						zipFilePath = tempZip.getAbsolutePath();
					}
					filenames = StatsCsvReaderWriter.getImportFileNamesFromZip(
							activity.getAccountName(), pacakgeNames, zipFilePath);
				} catch (IOException e) {
					Log.e(TAG, "Error reading import zip file: " + e.getMessage());
					return false;
				} finally {
					if (tempZip != null) {
						tempZip.delete();
					}
				}

				return true;
			} catch (ServiceException e) {
				Log.e(TAG, "Error reading import zip file: " + e.getMessage());
				return false;
			}
		}

		private File copyToTempFile() throws IOException {
			File tempZip = File.createTempFile("andlytics-import", ".zip");
			FileOutputStream out = new FileOutputStream(tempZip);
			byte[] zipBytes = FileUtils.readFromUri(activity, zipFileUri);
			out.write(zipBytes);
			out.flush();
			out.close();

			return tempZip;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (activity == null) {
				return;
			}

			activity.setProgressBarIndeterminateVisibility(false);

			if (!activity.isFinishing()) {
				if (result) {
					activity.setFilenames(filenames);
				} else {
					Toast.makeText(activity, activity.getString(R.string.import_no_sdcard_or_file),
							Toast.LENGTH_LONG).show();
					activity.finish();
				}
			}
		}

		List<String> getFilenames() {
			return filenames;
		}

	}

	class ImportListAdapter extends BaseAdapter {

		List<String> files;

		ImportListAdapter(List<String> files) {
			this.files = files;
		}

		@Override
		public int getCount() {
			return files.size();
		}

		@Override
		public String getItem(int position) {
			return files.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder;

			if (convertView == null) {

				convertView = layoutInflater.inflate(R.layout.import_list_item, null);

				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.import_file_name);
				holder.row = (RelativeLayout) convertView.findViewById(R.id.import_app_row);
				holder.checkbox = (CheckBox) convertView.findViewById(R.id.import_file_checkbox);
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			final String fileName = getItem(position);
			holder.name.setText(StatsCsvReaderWriter.getPackageName(fileName));

			holder.checkbox.setChecked(importFilenames.contains(fileName));

			holder.row.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {

					CheckBox checkbox = ((CheckBox) (((ViewGroup) v)
							.findViewById(R.id.import_file_checkbox)));
					checkbox.setChecked(!checkbox.isChecked());

					if (checkbox.isChecked()) {
						importFilenames.add(fileName);
					} else {
						importFilenames.remove(fileName);
					}
				}
			});

			holder.checkbox.setTag(fileName);

			holder.checkbox.setOnClickListener(new CheckBox.OnClickListener() {

				@Override
				public void onClick(View v) {
					boolean isChecked = ((CheckBox) v).isChecked();
					if (isChecked) {
						importFilenames.add(fileName);
					} else {
						importFilenames.remove(fileName);
					}

				}
			});

			return convertView;
		}

		private class ViewHolder {
			public RelativeLayout row;
			public TextView name;
			public CheckBox checkbox;
		}
	}

}
