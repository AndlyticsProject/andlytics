
package com.github.andlyticsproject.model;

import java.util.Date;

public class Comment {
	
	private boolean isReply = false;

	private String text;

	private Date date;
	
	private Date replyDate;

	private int rating;

	private String user;

	private String appVersion;

	private String device;

	private Comment reply;
	
	public Comment(){
		
	}
	
	public Comment(boolean isReply) {
		this.isReply = isReply;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Date or the comment
	 * In the case of a reply, this is the date or the original comment
	 *  - used for displaying comments correctly in the groups
	 * @return
	 */
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
	/**
	 * Date of the reply
	 * @return
	 */
	public Date getReplyDate() {
		return replyDate;
	}
	
	public void setReplyDate(Date date) {
		this.replyDate = date;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public void setDevice(String device) {
		this.device = device;
	}

	public String getDevice() {
		return device;
	}

	public Comment getReply() {
		return reply;
	}

	public void setReply(Comment reply) {
		this.reply = reply;
	}
	
	public boolean isReply() {
		return isReply;
	}

}
