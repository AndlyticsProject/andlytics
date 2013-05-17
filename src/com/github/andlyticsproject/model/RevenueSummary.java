package com.github.andlyticsproject.model;

public class RevenueSummary {

	private String currency;

	private double lastDay;
	private double last7Days;
	private double last30Days;

	public RevenueSummary(String currency, double lastDay, double last7Days, double last30Days) {
		this.currency = currency;
		this.lastDay = lastDay;
		this.last7Days = last7Days;
		this.last30Days = last30Days;
	}

	public String getCurrency() {
		return currency;
	}

	public double getLastDay() {
		return lastDay;
	}

	public double getLast7Days() {
		return last7Days;
	}

	public double getLast30Days() {
		return last30Days;
	}

	public boolean hasRevenue() {
		return last30Days > 0;
	}
}
