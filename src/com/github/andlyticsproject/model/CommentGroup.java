
package com.github.andlyticsproject.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommentGroup {

	private Date date;

	private List<Comment> comments;

	public CommentGroup() {
		comments = new ArrayList<Comment>();
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public List<Comment> getComments() {
		return comments;
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommentGroup other = (CommentGroup) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else {
			// TODO do a better check
			SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
			return fmt.format(date).equals(fmt.format(other.date));
		}
		return true;
	}

	public void addComment(Comment comment) {

		comments.add(comment);

	}

}
