package com.github.andlyticsproject.model;

public class RevenueSummary {

	public enum Type {
		TOTAL, SALES, IN_APP
	}

	private Long id;

	private Type type;
	private String currency;

	private double lastDay;
	private double last7Days;
	private double last30Days;
	private double overall;

	public static RevenueSummary createTotal(String currency, double lastDay, double last7Days,
			double last30Days, double overall) {
		return new RevenueSummary(Type.TOTAL, currency, lastDay, last7Days, last30Days, overall);
	}

	public static RevenueSummary createSales(String currency, double lastDay, double last7Days,
			double last30Days, double overall) {
		return new RevenueSummary(Type.SALES, currency, lastDay, last7Days, last30Days, overall);
	}

	public static RevenueSummary createInApp(String currency, double lastDay, double last7Days,
			double last30Days, double overall) {
		return new RevenueSummary(Type.IN_APP, currency, lastDay, last7Days, last30Days, overall);
	}

	public RevenueSummary(Type type, String currency, double lastDay, double last7Days,
			double last30Days, double overall) {
		this.type = type;
		this.currency = currency;
		this.lastDay = lastDay;
		this.last7Days = last7Days;
		this.last30Days = last30Days;
		this.overall = overall;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Type getType() {
		return type;
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
	
	public double getOverall() {
		return overall;
	}

	public boolean hasRevenue() {
		return overall > 0;
	}
}
