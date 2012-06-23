package com.github.andlyticsproject.model;

import java.util.ArrayList;
import java.util.List;

public class CommentGroup {
	
	private String dateString;
	
	private List<Comment> comments;
	
	public CommentGroup() {
		comments = new ArrayList<Comment>();
	}

	public String getDateString() {
		return dateString;
	}

	public void setDateString(String dateString) {
		this.dateString = dateString;
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
		result = prime * result + ((dateString == null) ? 0 : dateString.hashCode());
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
		if (dateString == null) {
			if (other.dateString != null)
				return false;
		} else if (!dateString.equals(other.dateString))
			return false;
		return true;
	}

	public void addComment(Comment comment) {
		
		comments.add(comment);
		
	}


}
