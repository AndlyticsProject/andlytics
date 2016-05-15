package com.github.andlyticsproject.model;

import java.util.Date;

public abstract class Statistic {

	protected Date date;

	public Date getDate() {
		return date == null ? null : (Date) date.clone();
	}

	public void setDate(Date date) {
		this.date = date == null ? null : (Date) date.clone();
	}
}
