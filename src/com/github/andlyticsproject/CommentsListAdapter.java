
package com.github.andlyticsproject;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.RatingBar;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

import com.github.andlyticsproject.model.Comment;
import com.github.andlyticsproject.model.CommentGroup;

public class CommentsListAdapter extends BaseExpandableListAdapter {

	private LayoutInflater layoutInflater;

	private ArrayList<CommentGroup> commentGroups;

	private CommentsActivity context;

	public CommentsListAdapter(CommentsActivity activity) {
		this.setCommentGroups(new ArrayList<CommentGroup>());
		this.layoutInflater = activity.getLayoutInflater();
		this.context = activity;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
			View convertView, ViewGroup parent) {

		ViewHolderChild holder;

		if (convertView == null) {
			convertView = layoutInflater.inflate(R.layout.comments_list_item_child, null);

			holder = new ViewHolderChild();
			holder.text = (TextView) convertView.findViewById(R.id.comments_list_item_text);
			holder.user = (TextView) convertView.findViewById(R.id.comments_list_item_username);
			holder.device = (TextView) convertView.findViewById(R.id.comments_list_item_device);
			holder.rating = (RatingBar) convertView
					.findViewById(R.id.comments_list_item_app_ratingbar);

			convertView.setTag(holder);
		} else {

			holder = (ViewHolderChild) convertView.getTag();
		}

		final Comment comment = getChild(groupPosition, childPosition);
		holder.text.setText(comment.getText());
		holder.user.setText(comment.getUser());

		String deviceText = "";
		// building string: version X on device: XYZ
		if (comment.getAppVersion() != null && comment.getAppVersion().length() > 0) {
			deviceText += context.getString(R.string.comments_version) + " " + comment.getAppVersion() + " ";
		}

		if (comment.getDevice() != null && comment.getDevice().length() > 0) {

			if (deviceText.length() > 0) {
				deviceText += context.getString(R.string.comments_on) + " ";
			}

			deviceText += context.getString(R.string.device)+ ": " + comment.getDevice();
		}

		holder.device.setText(deviceText);

		int rating = comment.getRating();
		switch (rating) {
			case 1:
				holder.rating.setRating(1f);
				break;
			case 2:
				holder.rating.setRating(2f);
				break;
			case 3:
				holder.rating.setRating(3f);
				break;
			case 4:
				holder.rating.setRating(4f);
				break;
			case 5:
				holder.rating.setRating(5f);
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
					url = url.replaceAll("<<lang>>", URLEncoder.encode(displayLanguage, "UTF-8"));
					url = url.replaceAll("<<text>>", URLEncoder.encode(text, "UTF-8"));
					Log.d("CommentsTranslate", "lang: " + displayLanguage + " url: " + url);

					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					context.startActivity(i);

				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return true;
			}
		});
		return convertView;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
			ViewGroup parent) {

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
		RatingBar rating;
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
