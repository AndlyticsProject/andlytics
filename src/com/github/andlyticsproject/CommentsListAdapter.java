package com.github.andlyticsproject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

import com.github.andlyticsproject.model.Comment;
import com.github.andlyticsproject.model.CommentGroup;

public class CommentsListAdapter extends BaseExpandableListAdapter {

	private LayoutInflater layoutInflater;

	private Bitmap ratingImage1;

	private Bitmap ratingImage2;

	private Bitmap ratingImage3;

	private Bitmap ratingImage4;

	private Bitmap ratingImage5;

	private ArrayList<CommentGroup> commentGroups;

    private CommentsActivity context;

	public CommentsListAdapter(CommentsActivity activity) {
		this.setCommentGroups(new ArrayList<CommentGroup>());
		this.layoutInflater = activity.getLayoutInflater();
		this.ratingImage1 = ((BitmapDrawable) activity.getResources().getDrawable(R.drawable.rating_1)).getBitmap();
		this.ratingImage2 = ((BitmapDrawable) activity.getResources().getDrawable(R.drawable.rating_2)).getBitmap();
		this.ratingImage3 = ((BitmapDrawable) activity.getResources().getDrawable(R.drawable.rating_3)).getBitmap();
		this.ratingImage4 = ((BitmapDrawable) activity.getResources().getDrawable(R.drawable.rating_4)).getBitmap();
		this.ratingImage5 = ((BitmapDrawable) activity.getResources().getDrawable(R.drawable.rating_5)).getBitmap();
		this.context = activity;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
			ViewGroup parent) {

		ViewHolderChild holder;

		if (convertView == null) {
			convertView = layoutInflater.inflate(R.layout.comments_list_item_child, null);

			holder = new ViewHolderChild();
			holder.text = (TextView) convertView.findViewById(R.id.comments_list_item_text);
			holder.user = (TextView) convertView.findViewById(R.id.comments_list_item_username);
			holder.device = (TextView) convertView.findViewById(R.id.comments_list_item_device);
			holder.rating = (ImageView) convertView.findViewById(R.id.comments_list_item_app_ratingbar);

			convertView.setTag(holder);
		} else {

			holder = (ViewHolderChild) convertView.getTag();
		}


		final Comment comment = getChild(groupPosition, childPosition);
		holder.text.setText(comment.getText());
		holder.user.setText(comment.getUser());

		String deviceText = "";

		if(comment.getAppVersion() != null && comment.getAppVersion().length() > 0) {
		    deviceText += "version " + comment.getAppVersion() + " ";
		}

		if(comment.getDevice() != null && comment.getDevice().length() > 0) {

		    if(deviceText.length() > 0) {
		        deviceText += "on ";
		    }

		    deviceText += "device: " + comment.getDevice();
		}

		holder.device.setText(deviceText);

		int rating = comment.getRating();
		switch (rating) {
		case 1:
			holder.rating.setImageBitmap(ratingImage1);
			break;
		case 2:
			holder.rating.setImageBitmap(ratingImage2);
			break;
		case 3:
			holder.rating.setImageBitmap(ratingImage3);
			break;
		case 4:
			holder.rating.setImageBitmap(ratingImage4);
			break;
		case 5:
			holder.rating.setImageBitmap(ratingImage5);
			break;

		default:
			break;
		}


        convertView.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {

                String text = comment.getText();

                String displayLanguage = Locale.getDefault().getLanguage();

                String url = "http://translate.google.de/m/translate?hl=<<lang>>&vi=m&text=<<text>>&langpair=auto|<<lang>>";

                try {
                    url = url.replaceAll("<<lang>>", URLEncoder.encode(displayLanguage,"UTF-8"));
                    url = url.replaceAll("<<text>>", URLEncoder.encode(text,"UTF-8"));
                    Log.d("CommentsTranslate", "lang: " + displayLanguage + " url: " + url);


                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    context.startActivity(i);

//                    TranslateDialog dialog = new TranslateDialog(context, text);
//                    dialog.show();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }


                return true;
            }
        });

		return convertView;

	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

		ViewHolderGroup holder;

		if (convertView == null) {
			convertView = layoutInflater.inflate(R.layout.comments_list_item, null);

			holder = new ViewHolderGroup();
			holder.date = (TextView) convertView.findViewById(R.id.comments_list_item_date);
			convertView.setTag(holder);
		} else {

			holder = (ViewHolderGroup) convertView.getTag();
		}

		CommentGroup commentGroup = getGroup(groupPosition);
		holder.date.setText(commentGroup.getDateString());

		convertView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

			}
		});

		return convertView;
	}

	static class ViewHolderGroup {
		TextView date;
	}

	static class ViewHolderChild {
		TextView text;
		ImageView rating;
		TextView user;
		TextView device;
	}

	@Override
	public int getGroupCount() {
		return getCommentGroups().size();
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return getCommentGroups().get(groupPosition).getComments().size();
	}

	@Override
	public CommentGroup getGroup(int groupPosition) {
		return getCommentGroups().get(groupPosition);
	}

	@Override
	public Comment getChild(int groupPosition, int childPosition) {
		return getCommentGroups().get(groupPosition).getComments().get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

	public void setCommentGroups(ArrayList<CommentGroup> commentGroups) {
		this.commentGroups = commentGroups;
	}

	public ArrayList<CommentGroup> getCommentGroups() {
		return commentGroups;
	}

}
