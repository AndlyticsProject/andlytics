package com.github.andlyticsproject.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdmobStatsSummary {

	private List<AdmobStats> admobStats = new ArrayList<AdmobStats>();
	private AdmobStats overallStats = new AdmobStats();

	public void addStats(AdmobStats stats) {
		admobStats.add(stats);

		overallStats.setClicks(overallStats.getClicks() + stats.getClicks());
		overallStats.setCpcRevenue(overallStats.getCpcRevenue() + stats.getCpcRevenue());
		overallStats.setCpmRevenue(overallStats.getCpmRevenue() + stats.getCpmRevenue());
		overallStats.setCtr(overallStats.getCtr() + stats.getCtr());
		overallStats.setEcpm(overallStats.getEcpm() + stats.getEcpm());
		overallStats.setExchangeDownloads(overallStats.getExchangeDownloads() + stats.getExchangeDownloads());
		overallStats.setFillRate(overallStats.getFillRate() + stats.getFillRate());
		overallStats.setHouseAdClicks(overallStats.getHouseAdClicks() + stats.getHouseAdClicks());
		overallStats.setHouseadFillRate(overallStats.getHouseadFillRate() + stats.getHouseadFillRate());
		overallStats.setHouseadRequests(overallStats.getHouseadRequests() + stats.getHouseadRequests());
		overallStats.setImpressions(overallStats.getImpressions() + stats.getImpressions());
		overallStats.setInterstitialRequests(overallStats.getInterstitialRequests()
				+ stats.getInterstitialRequests());
		overallStats.setOverallFillRate(overallStats.getOverallFillRate() + stats.getOverallFillRate());
		overallStats.setRequests(overallStats.getRequests() + stats.getRequests());
		overallStats.setRevenue(overallStats.getRevenue() + stats.getRevenue());
	}

	public List<AdmobStats> getAdmobs() {
		return Collections.unmodifiableList(admobStats);
	}

	public void calculateOverallStats() {
		Collections.reverse(admobStats);
		int count = admobStats.size();
		if (count > 0) {
			overallStats.setCtr(overallStats.getCtr() / count);
			overallStats.setFillRate(overallStats.getFillRate() / count);
			overallStats.setEcpm(overallStats.getEcpm() / count);
		}
	}

	public AdmobStats getOverallStats() {
		return overallStats;
	}


}
