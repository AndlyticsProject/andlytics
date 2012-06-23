package com.github.andlyticsproject.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Comment {
	
	private String text;
	
	private String date;
	
	private int rating;
	
	private String user;

	private SimpleDateFormat format;
	
	private String appVersion;
	
	private String device;
	
	public Comment() {
		format = new SimpleDateFormat("MMMMM dd, yyyy");
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public Date getDateObject(){
		Date result = null;
		if(date != null) {
			try {
				result = format.parse(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return result;
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

	
}
