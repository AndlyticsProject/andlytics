package com.github.andlyticsproject.model;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class AdmobStatsSummary extends StatsSummary<AdmobStats> {

	private Set<Date> dates = new HashSet<Date>();

	public AdmobStatsSummary() {
		overallStats = new AdmobStats();
	}

	@Override
	public void addStat(AdmobStats stat) {
		// hack to ignore empty Admob stats after migration
		if (stat.getRequests() == 0 && stat.getFillRate() == 0) {
			return;
		}

		// XXX hack to ignore duplicate dates after migration
		if (dates.contains(stat.getDate())) {
			return;
		}

		dates.add(stat.getDate());
		stats.add(stat);

		overallStats.setClicks(overallStats.getClicks() + stat.getClicks());
		overallStats.setCpcRevenue(overallStats.getCpcRevenue() + stat.getCpcRevenue());
		overallStats.setCpmRevenue(overallStats.getCpmRevenue() + stat.getCpmRevenue());
		overallStats.setCtr(overallStats.getCtr() + stat.getCtr());
		overallStats.setEcpm(overallStats.getEcpm() + stat.getEcpm());
		overallStats.setExchangeDownloads(overallStats.getExchangeDownloads()
				+ stat.getExchangeDownloads());
		overallStats.setFillRate(overallStats.getFillRate() + stat.getFillRate());
		overallStats.setHouseAdClicks(overallStats.getHouseAdClicks() + stat.getHouseAdClicks());
		overallStats.setHouseadFillRate(overallStats.getHouseadFillRate()
				+ stat.getHouseadFillRate());
		overallStats.setHouseadRequests(overallStats.getHouseadRequests()
				+ stat.getHouseadRequests());
		overallStats.setImpressions(overallStats.getImpressions() + stat.getImpressions());
		overallStats.setInterstitialRequests(overallStats.getInterstitialRequests()
				+ stat.getInterstitialRequests());
		overallStats.setOverallFillRate(overallStats.getOverallFillRate()
				+ stat.getOverallFillRate());
		overallStats.setRequests(overallStats.getRequests() + stat.getRequests());
		overallStats.setRevenue(overallStats.getRevenue() + stat.getRevenue());
	}


	@Override
	public void calculateOverallStats(int limit, boolean smoothEnabled) {
		Collections.reverse(stats);
		int count = stats.size();
		if (count > 0) {
			overallStats.setCtr(overallStats.getCtr() / count);
			overallStats.setFillRate(overallStats.getFillRate() / count);
			overallStats.setEcpm(overallStats.getEcpm() / count);
		}
	}

	@Override
	public boolean applySmoothedValues() {
		return false;
	}

}
