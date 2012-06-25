package com.github.andlyticsproject.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.github.andlyticsproject.R;
import com.github.andlyticsproject.cache.AppIconInMemoryCache;
import com.github.andlyticsproject.io.ExportService;
import com.github.andlyticsproject.model.AppInfo;

public class ExportDialog extends Dialog implements OnClickListener {

	public static final int TAG_IMAGE_REF = R.id.tag_mainlist_image_reference;

	Button okButton;

	private ExportListAdapter adapter;

	private List<AppInfo> appInfos;

	private LayoutInflater layoutInflater;

	private AppIconInMemoryCache inMemoryCache;

	private File cachDir;

	private Activity context;

	private Drawable spacerIcon;

	private List<String> exportPackageNames = new ArrayList<String>();

    public ExportDialog(final Activity context, List<AppInfo> appInfos, final String accountName) {


        super(context, R.style.Dialog);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        layoutInflater = context.getLayoutInflater();

        setContentView(R.layout.export_dialog);

        adapter = new ExportListAdapter();
        this.setAppInfos(appInfos);
        View closeButton = (View) this.findViewById(R.id.export_dialog_close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});


        View exportButton = (View) this.findViewById(R.id.export_dialog_export_button);
        exportButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                if (!android.os.Environment.getExternalStorageState().equals(
                                android.os.Environment.MEDIA_MOUNTED)) {
                    Toast.makeText(context, "SD-Card not mounted, can't export!", Toast.LENGTH_LONG).show();

                } else {

                    if (exportPackageNames.size() == 0) {

                        Toast.makeText(context, "No app selected, can't export!", Toast.LENGTH_LONG).show();

                    } else {

                        Intent intent = new Intent(context,ExportService.class);
                        intent.putExtra(ExportService.PACKAGE_NAMES, exportPackageNames.toArray(new String[exportPackageNames.size()]));
                        intent.putExtra(ExportService.ACCOUNT_NAME, accountName);
                        context.startService(intent);

                        dismiss();
                    }

                }


            }
        });
        ListView lv = (ListView) this.findViewById(R.id.list_view_id);
        lv.addHeaderView(layoutInflater.inflate(R.layout.export_list_header,null));
        lv.setAdapter(adapter);

		this.inMemoryCache = AppIconInMemoryCache.getInstance();
		this.cachDir = context.getCacheDir();
		this.context = context;
		this.spacerIcon = context.getResources().getDrawable(R.drawable.app_icon_spacer);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void onClick(View v) {
        /** When OK Button is clicked, dismiss the dialog */
        if (v == okButton)
            dismiss();
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

				convertView = layoutInflater.inflate(R.layout.ghost_list_item, null);

				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.main_app_name);
				holder.packageName = (TextView) convertView.findViewById(R.id.main_package_name);
				holder.icon = (ImageView) convertView.findViewById(R.id.main_app_icon);
				holder.row = (RelativeLayout) convertView.findViewById(R.id.main_app_row);
				holder.checkbox = (CheckBox)convertView.findViewById(R.id.ghost_checkbox);
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			final AppInfo appDownloadInfo = getItem(position);
			holder.name.setText(appDownloadInfo.getName());
			holder.packageName.setText(appDownloadInfo.getPackageName());

			final String packageName = appDownloadInfo.getPackageName();

			holder.checkbox.setChecked(exportPackageNames.contains(packageName));

			final File iconFile = new File(cachDir + "/" + appDownloadInfo.getIconName());

			holder.icon.setTag(TAG_IMAGE_REF, packageName );
			if(inMemoryCache.contains(packageName)) {

				holder.icon.setImageBitmap(inMemoryCache.get(packageName));
				holder.icon.clearAnimation();

			} else {
				holder.icon.setImageDrawable(null);
				holder.icon.clearAnimation();
				if(appDownloadInfo.getPackageName().startsWith("com.github.andlyticsproject.demo")) {

					holder.icon.setImageDrawable(context.getResources().getDrawable(R.drawable.default_app_icon));

				} else {

					new GetCachedImageTask(holder.icon, appDownloadInfo.getPackageName()).execute(new File[]{ iconFile });
				}
			}


			holder.row.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {

					CheckBox checkbox = ((CheckBox)(((ViewGroup)v).findViewById(R.id.ghost_checkbox)));
					checkbox.setChecked(!checkbox.isChecked());

                    if(checkbox.isChecked()) {
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
                    boolean checked = ((CheckBox)v).isChecked();
                    if(checked) {
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

				if(result == null) {
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

			if(iconFile.exists()) {
				Bitmap bm = BitmapFactory.decodeFile(iconFile.getAbsolutePath());
				return bm;
			}
			return null;
		}
	}



	public void updateMainImage(ImageView imageView, int animationId, Bitmap result) {
		imageView.setImageBitmap(result);
		imageView.clearAnimation();
		Animation fadeInAnimation = AnimationUtils.loadAnimation(context.getApplicationContext(), animationId);
		imageView.startAnimation(fadeInAnimation);
	}

	public void setAppInfos(List<AppInfo> appInfos) {
		this.appInfos = appInfos;
		if(adapter != null) {
			this.adapter.notifyDataSetChanged();
		}
	}

	public List<AppInfo> getAppInfos() {
		return appInfos;
	}
}
