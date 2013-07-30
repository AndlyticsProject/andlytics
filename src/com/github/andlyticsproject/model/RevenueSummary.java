package com.github.andlyticsproject.model;

public class RevenueSummary {

	private Long id;

	private Revenue.Type type;
	private String currency;

	private Revenue lastDay;
	private Revenue last7Days;
	private Revenue last30Days;
	private Revenue overall;

	public static RevenueSummary createTotal(String currency, double lastDay, double last7Days,
			double last30Days, double overall) {
		return new RevenueSummary(Revenue.Type.TOTAL, currency, lastDay, last7Days, last30Days,
				overall);
	}

	public static RevenueSummary createSales(String currency, double lastDay, double last7Days,
			double last30Days, double overall) {
		return new RevenueSummary(Revenue.Type.APP_SALES, currency, lastDay, last7Days, last30Days,
				overall);
	}

	public static RevenueSummary createInApp(String currency, double lastDay, double last7Days,
			double last30Days, double overall) {
		return new RevenueSummary(Revenue.Type.IN_APP, currency, lastDay, last7Days, last30Days,
				overall);
	}

	public RevenueSummary(Revenue.Type type, String currency, double lastDay, double last7Days,
			double last30Days, double overall) {
		this.type = type;
		this.currency = currency;
		this.lastDay = new Revenue(type, lastDay, currency);
		this.last7Days = new Revenue(type, last7Days, currency);
		this.last30Days = new Revenue(type, last30Days, currency);
		this.overall = new Revenue(type, overall, currency);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Revenue.Type getType() {
		return type;
	}

	public String getCurrency() {
		return currency;
	}

	public Revenue getLastDay() {
		return lastDay;
	}

	public Revenue getLast7Days() {
		return last7Days;
	}

	public Revenue getLast30Days() {
		return last30Days;
	}

	public Revenue getOverall() {
		return overall;
	}

	public boolean hasRevenue() {
		return overall.getAmount() > 0 || last30Days.getAmount() > 0;
	}
}
