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

	public static RevenueSummary createTotal(String currency, double lastDay, double last7Days,
			double last30Days) {
		return new RevenueSummary(Type.TOTAL, currency, lastDay, last7Days, last30Days);
	}

	public static RevenueSummary createSales(String currency, double lastDay, double last7Days,
			double last30Days) {
		return new RevenueSummary(Type.SALES, currency, lastDay, last7Days, last30Days);
	}

	public static RevenueSummary createInApp(String currency, double lastDay, double last7Days,
			double last30Days) {
		return new RevenueSummary(Type.IN_APP, currency, lastDay, last7Days, last30Days);
	}

	public RevenueSummary(Type type, String currency, double lastDay, double last7Days,
			double last30Days) {
		this.type = type;
		this.currency = currency;
		this.lastDay = lastDay;
		this.last7Days = last7Days;
		this.last30Days = last30Days;
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

	public boolean hasRevenue() {
		return last30Days > 0;
	}
}
