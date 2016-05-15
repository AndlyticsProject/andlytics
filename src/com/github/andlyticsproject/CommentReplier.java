package com.github.andlyticsproject;

import com.github.andlyticsproject.model.Comment;

public interface CommentReplier {

	public void showReplyDialog(Comment comment);

	public void hideReplyDialog();

	public void replyToComment(String commentUniqueId, String reply);
}
