package de.betaapps.andlytics.dialog;

import de.betaapps.andlytics.ContentAdapter;
import de.betaapps.andlytics.Preferences;
import de.betaapps.andlytics.R;
import de.betaapps.andlytics.cache.AppIconInMemoryCache;
import de.betaapps.andlytics.model.AppInfo;

import android.app.Activity;
import android.app.Dialog;
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

import java.io.File;
import java.util.List;

public class NotificationsDialog extends Dialog implements OnClickListener {

	public static final int TAG_IMAGE_REF = R.id.tag_mainlist_image_reference;

	Button okButton;
    
	private GhostListAdapter adapter;

	private List<AppInfo> appInfos;

	private LayoutInflater layoutInflater;

	private AppIconInMemoryCache inMemoryCache;

	private File cachDir;

	private Activity context;

	private Drawable spacerIcon;
	
    private ViewGroup ratingChangeButton;

    private ViewGroup commentsChangeButton;

    private ViewGroup downloadsChangeButton;

    private ViewGroup soundButton;

    private ContentAdapter db;

    private ViewGroup lightButton;

	public interface GhostSelectonChangeListener {
		
		public void onGhostSelectionChanged(String packageName, boolean isGhost);

		public void onGhostDialogClose();
		
	} 
	
    public NotificationsDialog(final Activity context, List<AppInfo> appInfos, final String accountName, final ContentAdapter db) {
        
        super(context, R.style.Dialog);
        this.db = db;
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        layoutInflater = context.getLayoutInflater();
        
        setContentView(R.layout.notification_dialog);
        
        adapter = new GhostListAdapter();
        this.setAppInfos(appInfos);
        View closeButton = (View) this.findViewById(R.id.notification_dialog_close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
        ListView lv = (ListView) this.findViewById(R.id.list_view_id);
        lv.addHeaderView(layoutInflater.inflate(R.layout.notification_list_header, null), null, false);

        lv.setAdapter(adapter);
        
		this.inMemoryCache = AppIconInMemoryCache.getInstance();
		this.cachDir = context.getCacheDir();
		this.context = context;
		this.spacerIcon = context.getResources().getDrawable(R.drawable.app_icon_spacer);
		
        View.OnClickListener ratingOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePrefValue(context, Preferences.NOTIFICATION_CHANGES_RATING, accountName);
                updateButtons(accountName);
            }
        };
        ratingChangeButton = (ViewGroup) this.findViewById(R.id.notification_dialog_rating_change);
        ratingChangeButton.setOnClickListener(ratingOnClick);
        ((CheckBox)ratingChangeButton.getChildAt(0)).setOnClickListener(ratingOnClick);
        
        View.OnClickListener commentsOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePrefValue(context, Preferences.NOTIFICATION_CHANGES_COMMENTS, accountName);
                updateButtons(accountName);
            }
        };
        commentsChangeButton = (ViewGroup) this.findViewById(R.id.notification_dialog_comments_change);
        commentsChangeButton.setOnClickListener(commentsOnClick);
        ((CheckBox)commentsChangeButton.getChildAt(0)).setOnClickListener(commentsOnClick);
        
        View.OnClickListener downloadsOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePrefValue(context, Preferences.NOTIFICATION_CHANGES_DOWNLOADS, accountName);
                updateButtons(accountName);
            }
        };
        downloadsChangeButton = (ViewGroup) this.findViewById(R.id.notification_dialog_downloads_change);
        downloadsChangeButton.setOnClickListener(downloadsOnClick);
        ((CheckBox)downloadsChangeButton.getChildAt(0)).setOnClickListener(downloadsOnClick);
        
        View.OnClickListener soundOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePrefValue(context, Preferences.NOTIFICATION_SOUND, accountName);
                updateButtons(accountName);
            }
        };		
        soundButton = (ViewGroup) this.findViewById(R.id.notification_dialog_sound);
        soundButton.setOnClickListener(soundOnClick);
        ((CheckBox)soundButton.getChildAt(0)).setOnClickListener(soundOnClick);
        
        
        View.OnClickListener lightOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePrefValue(context, Preferences.NOTIFICATION_LIGHT, accountName);
                updateButtons(accountName);
            }
        };      
        lightButton = (ViewGroup) this.findViewById(R.id.notification_dialog_led);
        lightButton.setOnClickListener(lightOnClick);
        ((CheckBox)lightButton.getChildAt(0)).setOnClickListener(lightOnClick);
        
        
        updateButtons(accountName);
        
    }

    protected void updateButtons(String accountName) {

        boolean downloads = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_CHANGES_DOWNLOADS, accountName);
        ((CheckBox)downloadsChangeButton.getChildAt(0)).setChecked(downloads);
        
        boolean comments = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_CHANGES_COMMENTS, accountName);
        ((CheckBox)commentsChangeButton.getChildAt(0)).setChecked(comments);

        boolean ratings = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_CHANGES_RATING, accountName);
        ((CheckBox)ratingChangeButton.getChildAt(0)).setChecked(ratings);

        boolean sound = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_SOUND, accountName);
        ((CheckBox)soundButton.getChildAt(0)).setChecked(sound);

        boolean light = Preferences.getNotificationPerf(context, Preferences.NOTIFICATION_LIGHT, accountName);
        ((CheckBox)lightButton.getChildAt(0)).setChecked(light);
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
    
    class GhostListAdapter extends BaseAdapter {

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

				convertView = layoutInflater.inflate(R.layout.notification_list_item, null);

				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.notification_app_name); 
				holder.icon = (ImageView) convertView.findViewById(R.id.notification_app_icon);
				holder.row = (RelativeLayout) convertView.findViewById(R.id.notification_app_row);
				holder.checkbox = (CheckBox)convertView.findViewById(R.id.notification_app_checkbox);
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			final AppInfo appDownloadInfo = getItem(position);
			holder.name.setText(appDownloadInfo.getName());
			
			final String packageName = appDownloadInfo.getPackageName();
			
			final File iconFile = new File(cachDir + "/" + appDownloadInfo.getIconName());

			holder.icon.setTag(TAG_IMAGE_REF, packageName );
			if(inMemoryCache.contains(packageName)) {

				holder.icon.setImageBitmap(inMemoryCache.get(packageName));
				holder.icon.clearAnimation();
				
			} else {
				holder.icon.setImageDrawable(null);
				holder.icon.clearAnimation();
				if(appDownloadInfo.getPackageName().startsWith("de.betaapps.demo")) {
					
					holder.icon.setImageDrawable(context.getResources().getDrawable(R.drawable.default_app_icon));
					
				} else {
					
					new GetCachedImageTask(holder.icon, appDownloadInfo.getPackageName()).execute(new File[]{ iconFile });
					
				}
			}

			
			holder.row.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {

					CheckBox checkbox = ((CheckBox)(((ViewGroup)v).getChildAt(2)));
					checkbox.setChecked(!checkbox.isChecked());
                    boolean checked = checkbox.isChecked();
                    db.setSkipNotification(packageName, !checked);
                    appDownloadInfo.setSkipNotification(!checked);
				}
			});
			
			holder.checkbox.setTag(packageName);
			holder.checkbox.setChecked(!appDownloadInfo.isSkipNotification());
			holder.checkbox.setOnClickListener(new CheckBox.OnClickListener() {
				
                @Override
                public void onClick(View v) {
                    boolean checked = !((CheckBox)v).isChecked();
                    db.setSkipNotification(packageName, checked);
                    appDownloadInfo.setSkipNotification(checked);
                    
                }
			});

			return convertView;
		}
		
		private class ViewHolder {
			public RelativeLayout row;
			public TextView name;
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

    private void togglePrefValue(final Activity context, String property, final String accountName) {
        boolean enabled = Preferences.getNotificationPerf(context, property, accountName);
        Preferences.saveNotificationPref(context, property, accountName, !enabled);
    }	
}
