package com.github.andlyticsproject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.github.andlyticsproject.model.Comment;
import com.github.andlyticsproject.model.CommentGroup;
import com.github.andlyticsproject.util.Utils;

public class CommentsListAdapter extends BaseExpandableListAdapter {

	private static final int TYPE_COMMENT = 0;
	private static final int TYPE_REPLY = 1;

	private LayoutInflater layoutInflater;

	private List<CommentGroup> commentGroups;

	private Activity context;

	private DateFormat commentDateFormat = DateFormat.getDateInstance(DateFormat.FULL);

	private boolean canReplyToComments;

	public CommentsListAdapter(Activity activity) {
		// XXX no pretty, is there a better way?
		if (!(activity instanceof CommentReplier)) {
			throw new ClassCastException("Activity must implement CommentReplier.");
		}
		this.setCommentGroups(new ArrayList<CommentGroup>());
		this.layoutInflater = activity.getLayoutInflater();
		this.context = activity;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
			View convertView, ViewGroup parent) {

		final Comment comment = getChild(groupPosition, childPosition);
		ViewHolderChild holder;

		if (convertView == null) {
			convertView = layoutInflater.inflate(
					comment.isReply() ? R.layout.comments_list_item_reply
							: R.layout.comments_list_item_comment, null);

			holder = new ViewHolderChild();
			holder.text = (TextView) convertView.findViewById(R.id.comments_list_item_text);
			holder.title = (TextView) convertView.findViewById(R.id.comments_list_item_title);
			holder.user = (TextView) convertView.findViewById(R.id.comments_list_item_username);
			holder.date = (TextView) convertView.findViewById(R.id.comments_list_item_date);
			holder.device = (TextView) convertView.findViewById(R.id.comments_list_item_device);
			holder.version = (TextView) convertView.findViewById(R.id.comments_list_item_version);
			holder.rating = (RatingBar) convertView
					.findViewById(R.id.comments_list_item_app_ratingbar);
			holder.deviceVersionContainer = (LinearLayout) convertView
					.findViewById(R.id.comments_list_item_device_container);
			holder.language = (TextView) convertView.findViewById(R.id.comments_list_item_language);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolderChild) convertView.getTag();
		}

		if (holder.language != null) {
			final TextView commentText = holder.text;
			holder.language.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					boolean showTranslations = Preferences.isShowCommentAutoTranslations(context);
					if (!showTranslations || !comment.isTranslated()) {
						return;
					}

					if (comment.getText().equals(commentText.getText().toString())) {
						commentText.setText(comment.getOriginalText());
						commentText.setTextAppearance(context, R.style.normalText);
					} else {
						commentText.setText(comment.getText());
						commentText.setTextAppearance(context, R.style.italicText);
					}
				}
			});
		}
		holder.replyIcon = (ImageView) convertView.findViewById(R.id.comments_list_icon_reply);
		if (holder.replyIcon != null) {
			holder.replyIcon.setVisibility(canReplyToComments ? View.VISIBLE : View.INVISIBLE);
			holder.replyIcon.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					CommentReplier replier = (CommentReplier) context;
					replier.showReplyDialog(comment);
				}
			});
		}

		if (comment.isReply()) {
			holder.date.setText(formatCommentDate(comment.getDate()));
			holder.text.setText(comment.getText());
		} else {
			boolean showTranslations = Preferences.isShowCommentAutoTranslations(context);
			String commentText = comment.getText();
			if (!showTranslations && comment.getOriginalText() != null) {
				commentText = comment.getOriginalText();
			}

			// XXX translations replace the tab delimiter with space, so
			// no way to separate the title. Show title in original language
			// for now
			String originalTitleAndComment[] = comment.getOriginalText() == null ? null : comment
					.getOriginalText().split("\\t");
			if (originalTitleAndComment != null && originalTitleAndComment.length > 1) {
				holder.title.setText(originalTitleAndComment[0]);
				if (showTranslations) {
					holder.text.setText(commentText);
				} else {
					holder.text.setText(originalTitleAndComment[1]);
				}
				holder.text.setVisibility(View.VISIBLE);
			} else {
				holder.text.setText(commentText);
				holder.title.setText(null);
				holder.title.setVisibility(View.GONE);
			}
			// italic for translated text
			boolean translated = showTranslations && comment.isTranslated();
			holder.text.setTextAppearance(context, translated ? R.style.italicText
					: R.style.normalText);

			holder.user.setText(comment.getUser() == null ? context
					.getString(R.string.comment_no_user_info) : comment.getUser());
			String version = comment.getAppVersion();
			String device = comment.getDevice();
			String language = comment.getLanguage();
			holder.version.setVisibility(View.GONE);
			holder.device.setVisibility(View.GONE);
			holder.language.setVisibility(View.GONE);
			boolean showInfoBox = false;

			// building version/device
			if (isNotEmptyOrNull(version)) {
				holder.version.setText(version);
				holder.version.setVisibility(View.VISIBLE);
				showInfoBox = true;
			}
			if (isNotEmptyOrNull(device)) {
				holder.device.setText(device);
				holder.device.setVisibility(View.VISIBLE);
				showInfoBox = true;
			}
			// TODO better UI for language, option to show original text?
			if (isNotEmptyOrNull(language)) {
				holder.language.setText(formatLanguageString(comment.getLanguage()));
				holder.language.setVisibility(View.VISIBLE);
				showInfoBox = true;
			}

			if (showInfoBox) {
				holder.deviceVersionContainer.setVisibility(View.VISIBLE);
			} else {
				holder.deviceVersionContainer.setVisibility(View.GONE);
			}

			int rating = comment.getRating();
			if (rating > 0 && rating <= 5) {
				holder.rating.setRating((float) rating);
			}
		}

		convertView.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				String text = comment.getText();
				String displayLanguage = Locale.getDefault().getLanguage();

				if (Preferences.isUseGoogleTranslateApp(context) && isGoogleTranslateInstalled()) {
					sendToGoogleTranslate(text, displayLanguage);
					return true;
				}

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

	private boolean isGoogleTranslateInstalled() {
		return Utils.isPackageInstalled(context, "com.google.android.apps.translate");
	}

	private String formatLanguageString(String language) {
		if (language == null || language.indexOf("_") == -1) {
			return language;
		}

		String[] parts = language.split("_");
		if (parts.length > 1
				&& parts[0].toUpperCase(Locale.ENGLISH)
						.equals(parts[1].toUpperCase(Locale.ENGLISH))) {
			return parts[1].toUpperCase(Locale.ENGLISH);
		}

		return language.replaceAll("_", "/");

	}

	private void sendToGoogleTranslate(String text, String displayLanguage) {
		Intent i = new Intent();
		i.setAction(Intent.ACTION_VIEW);
		i.putExtra("key_text_input", text);
		i.putExtra("key_text_output", "");
		i.putExtra("key_language_from", "auto");
		i.putExtra("key_language_to", displayLanguage);
		i.putExtra("key_suggest_translation", "");
		i.putExtra("key_from_floating_window", false);
		i.setComponent(new ComponentName("com.google.android.apps.translate",
				"com.google.android.apps.translate.translation.TranslateActivity"));
		context.startActivity(i);
	}

	@Override
	public int getChildType(int groupPosition, int childPosition) {
		return getChild(groupPosition, childPosition).isReply() ? TYPE_REPLY : TYPE_COMMENT;
	}

	@Override
	public int getChildTypeCount() {
		return 2;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
			ViewGroup parent) {

		ViewHolderGroup holder;

		if (convertView == null) {
			convertView = layoutInflater.inflate(R.layout.comments_list_item_group_header, null);
			holder = new ViewHolderGroup();
			holder.date = (TextView) convertView.findViewById(R.id.comments_list_item_date);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolderGroup) convertView.getTag();
		}

		CommentGroup commentGroup = getGroup(groupPosition);
		holder.date.setText(formatCommentDate(commentGroup.getDate()));

		return convertView;
	}

	private boolean isNotEmptyOrNull(String str) {
		return str != null && str.length() > 0;
	}

	static class ViewHolderGroup {
		TextView date;
	}

	static class ViewHolderChild {
		RatingBar rating;
		TextView text;
		TextView title;
		TextView user;
		TextView date;
		LinearLayout deviceVersionContainer;
		TextView device;
		TextView version;
		TextView language;
		ImageView replyIcon;
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

	public void setCommentGroups(List<CommentGroup> commentGroups) {
		this.commentGroups = commentGroups;
	}

	public List<CommentGroup> getCommentGroups() {
		return commentGroups;
	}

	private String formatCommentDate(Date date) {
		return commentDateFormat.format(date);
	}

	public boolean isCanReplyToComments() {
		return canReplyToComments;
	}

	public void setCanReplyToComments(boolean canReplyToComments) {
		this.canReplyToComments = canReplyToComments;
	}

}
