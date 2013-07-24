package com.github.andlyticsproject.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;

@SuppressLint("SimpleDateFormat")
public class CommentGroup {

	private SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyyMMdd");

	// both non-null
	private Date date;
	// comments are grouped by date, we ignore HH:mm:ss and use 
	// this for comparison/searches
	private String yyyymmddDate;

	private List<Comment> comments = new ArrayList<Comment>();

	public CommentGroup(Date date) {
		setDate(date);
	}

	public CommentGroup(Comment comment) {
		Date date = comment.isReply() ? comment.getOriginalCommentDate() : comment.getDate();
		setDate(date);
		comments.add(comment);
	}

	public Date getDate() {
		return date = (Date) date.clone();
	}

	public void setDate(Date date) {
		this.date = (Date) date.clone();
		this.yyyymmddDate = DATE_FMT.format(date);
	}

	public String getFormattedDate() {
		return yyyymmddDate;
	}

	public List<Comment> getComments() {
		return comments;
	}

	public void addComment(Comment comment) {
		comments.add(comment);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + yyyymmddDate.hashCode();

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CommentGroup)) {
			return false;
		}

		CommentGroup rhs = (CommentGroup) obj;
		if (date == null && rhs.date != null) {
			return false;
		}

		return yyyymmddDate.equals(rhs.yyyymmddDate);
	}

	@Override
	public String toString() {
		return "CommentGroup " + yyyymmddDate;
	}

}
