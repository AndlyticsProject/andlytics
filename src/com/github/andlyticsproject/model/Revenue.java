package com.github.andlyticsproject.model;

import java.util.Date;


public class Revenue {

	public enum Type {
		TOTAL, SALES, IN_APP
	}


	private Type type;
	private String currency;
	private Date date;
	private double amount;

	public Revenue(Type type, Date date, String currency, double amount) {
		this.type = type;
		this.date = date == null ? null : (Date) date.clone();
		this.currency = currency;
		this.amount = amount;
	}

	public Type getType() {
		return type;
	}

	public Date getDate() {
		return date == null ? null : (Date) date.clone();
	}

	public String getCurrency() {
		return currency;
	}

	public double getAmount() {
		return amount;
	}

}
