package com.github.andlyticsproject.model;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;

public class AppStatsSummary {

	private List<AppStats> appStats = new ArrayList<AppStats>();
	private AppStats overallStats = new AppStats();

	private Integer highestRatingChange = 0;
	private Integer lowestRatingChange = 0;

	public void addStats(AppStats stats) {
		stats.init();
		appStats.add(stats);
	}

	public List<AppStats> getAppStats() {
		return Collections.unmodifiableList(appStats);
	}

	@SuppressLint("SimpleDateFormat")
	public void calculateOverallStats(int limit, boolean smoothEnabled) {
		Collections.reverse(appStats);

		List<AppStats> missingAppStats = new ArrayList<AppStats>();
		List<Integer> missingAppStatsPositionOffest = new ArrayList<Integer>();

		int positionInsertOffset = 0;

		// add missing sync days
		if (appStats.size() > 1) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

			for (int currentIndex = 1; currentIndex < appStats.size(); currentIndex++) {

				String olderEntryDate = appStats.get(currentIndex - 1).getRequestDateString();
				String newerEntryDate = appStats.get(currentIndex).getRequestDateString();

				try {
					Date olderDate = dateFormat.parse(olderEntryDate);
					Date newerDate = dateFormat.parse(newerEntryDate);

					long daysDistance = ((newerDate.getTime() - olderDate.getTime()) / 1000 / 60 / 60 / 24);

					for (int i = 1; i < daysDistance; i++) {
						AppStats missingEntry = new AppStats(appStats.get(currentIndex - 1));
						missingEntry.setRequestDate(new Date(missingEntry.getRequestDate()
								.getTime() + (i * 1000 * 60 * 60 * 24)));
						missingAppStats.add(missingEntry);
						missingAppStatsPositionOffest.add(currentIndex + positionInsertOffset);
						positionInsertOffset++;
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}

		for (int i = 0; i < missingAppStatsPositionOffest.size(); i++) {
			appStats.add(missingAppStatsPositionOffest.get(i), missingAppStats.get(i));
		}

		// calculate daily downloads
		int nullStartIndex = -1;
		boolean greaterNullDetected = false;

		for (int currentIndex = 1; currentIndex < appStats.size(); currentIndex++) {

			// normalize daily, total & active
			int olderTotalValue = appStats.get(currentIndex - 1).getTotalDownloads();
			int newerTotalValue = appStats.get(currentIndex).getTotalDownloads();
			int totalValueDiff = newerTotalValue - olderTotalValue;

			int olderActiveValue = appStats.get(currentIndex - 1).getActiveInstalls();
			int newerActiveValue = appStats.get(currentIndex).getActiveInstalls();
			int activeValueDiff = newerActiveValue - olderActiveValue;

			if (nullStartIndex > -1) {
				if (totalValueDiff > 0) {
					greaterNullDetected = true;
				}
			}

			if (totalValueDiff == 0 && nullStartIndex < 0) {
				nullStartIndex = currentIndex;
			} else {
				if (nullStartIndex != -1 && greaterNullDetected && smoothEnabled) {

					// distance to fill with values
					int distance = currentIndex - nullStartIndex + 1;

					// smoothen values
					int totalSmoothvalue = Math.round(totalValueDiff / distance);
					int activeSmoothvalue = Math.round(activeValueDiff / distance);

					// rounding
					int roundingErrorTotal = newerTotalValue
							- (olderTotalValue + ((totalSmoothvalue * (distance))));
					int roundingErrorActive = newerActiveValue
							- (olderActiveValue + ((activeSmoothvalue * (distance))));
					;

					int totalDownload = appStats.get(nullStartIndex - 1).getTotalDownloads();
					int activeInstall = appStats.get(nullStartIndex - 1).getActiveInstalls();

					for (int j = nullStartIndex; j < currentIndex + 1; j++) {

						totalDownload += totalSmoothvalue;
						activeInstall += activeSmoothvalue;

						// for the last value, take rounding error in account
						if (currentIndex == j) {
							appStats.get(j)
									.setDailyDownloads(totalSmoothvalue + roundingErrorTotal);
							appStats.get(j).setTotalDownloads(totalDownload + roundingErrorTotal);
							appStats.get(j).setActiveInstalls(activeInstall + roundingErrorActive);
						} else {
							appStats.get(j).setDailyDownloads(totalSmoothvalue);
							appStats.get(j).setTotalDownloads(totalDownload);
							appStats.get(j).setActiveInstalls(activeInstall);
						}

						appStats.get(j).setSmoothingApplied(true);
					}

					nullStartIndex = -1;
					greaterNullDetected = false;
				} else {

					appStats.get(currentIndex).setDailyDownloads(totalValueDiff);
				}
			}
		}

		// reduce if limit exceeded (only if sync < 24h)
		if (appStats.size() > limit) {
			appStats = appStats.subList(appStats.size() - limit, appStats.size());
		}

		float overallActiveInstallPercent = 0;
		float overallAvgRating = 0;

		// create rating diff
		AppStats prevStats = null;
		int value = 0;

		for (int i = 0; i < appStats.size(); i++) {

			AppStats stats = appStats.get(i);
			if (prevStats != null) {
				value = stats.getRating1() - prevStats.getRating1();
				if (value > highestRatingChange)
					highestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;
				stats.setRating1Diff(value);

				value = stats.getRating2() - prevStats.getRating2();
				if (value > highestRatingChange)
					highestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;
				stats.setRating2Diff(value);

				value = stats.getRating3() - prevStats.getRating3();
				if (value > highestRatingChange)
					highestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;
				stats.setRating3Diff(value);

				value = stats.getRating4() - prevStats.getRating4();
				if (value > highestRatingChange)
					highestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;
				stats.setRating4Diff(value);

				value = stats.getRating5() - prevStats.getRating5();
				if (value > highestRatingChange)
					highestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;

				stats.setRating5Diff(value);

				stats.setAvgRatingDiff(stats.getAvgRating() - prevStats.getAvgRating());
				stats.setRatingCountDiff(stats.getRatingCount() - prevStats.getRatingCount());
				stats.setNumberOfCommentsDiff(stats.getNumberOfComments()
						- prevStats.getNumberOfComments());
				stats.setActiveInstallsDiff(stats.getActiveInstalls()
						- prevStats.getActiveInstalls());
			}
			prevStats = stats;

			overallActiveInstallPercent += stats.getActiveInstallsPercent();
			overallAvgRating += stats.getAvgRating();

		}

		if (appStats.size() > 0) {

			AppStats first = appStats.get(0);
			AppStats last = appStats.get(appStats.size() - 1);

			overallStats.setActiveInstalls(last.getActiveInstalls() - first.getActiveInstalls());
			overallStats.setTotalDownloads(last.getTotalDownloads() - first.getTotalDownloads());
			overallStats.setRating1(last.getRating1() - first.getRating1());
			overallStats.setRating2(last.getRating2() - first.getRating2());
			overallStats.setRating3(last.getRating3() - first.getRating3());
			overallStats.setRating4(last.getRating4() - first.getRating4());
			overallStats.setRating5(last.getRating5() - first.getRating5());
			overallStats.init();
			overallStats.setDailyDownloads((last.getTotalDownloads() - first.getTotalDownloads())
					/ appStats.size());

			BigDecimal avgBigDecimal = new BigDecimal(overallAvgRating / appStats.size());
			avgBigDecimal = avgBigDecimal.setScale(3, BigDecimal.ROUND_HALF_UP);
			overallStats.setAvgRatingString(avgBigDecimal.toPlainString() + "");

			BigDecimal percentBigDecimal = new BigDecimal(overallActiveInstallPercent
					/ appStats.size());
			percentBigDecimal = percentBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);

			overallStats.setActiveInstallsPercentString(percentBigDecimal.toPlainString() + "");

			double totalRevenue = 0;
			String currency = null;
			for (AppStats as : appStats) {
				if (as.getTotalRevenue() != null) {
					totalRevenue += as.getTotalRevenue().getAmount();
					if (currency == null) {
						currency = as.getTotalRevenue().getCurrencyCode();
					}
				}
			}
			if (currency != null) {
				overallStats
						.setTotalRevenue(new Revenue(Revenue.Type.TOTAL, totalRevenue, currency));
			}
		}

	}

	public AppStats getOverallStats() {
		return overallStats;
	}

	public void setLowestRatingChange(Integer lowestRatingChange) {
		this.lowestRatingChange = lowestRatingChange;
	}

	public Integer getLowestRatingChange() {
		return lowestRatingChange;
	}

	public void setHighestRatingChange(Integer highestRatingChange) {
		this.highestRatingChange = highestRatingChange;
	}

	public Integer getHighestRatingChange() {
		return highestRatingChange;
	}

}
