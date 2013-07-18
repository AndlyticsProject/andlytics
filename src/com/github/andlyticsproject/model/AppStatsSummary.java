package com.github.andlyticsproject.model;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;

public class AppStatsSummary extends StatsSummary<AppStats> {

	private Integer highestRatingChange = 0;
	private Integer lowestRatingChange = 0;

	public AppStatsSummary() {
		overallStats = new AppStats();
	}

	@Override
	public void addStat(AppStats stat) {
		stat.init();
		stats.add(stat);
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	public void calculateOverallStats(int limit, boolean smoothEnabled) {
		Collections.reverse(stats);

		List<AppStats> missingAppStats = new ArrayList<AppStats>();
		List<Integer> missingAppStatsPositionOffest = new ArrayList<Integer>();

		int positionInsertOffset = 0;

		// add missing sync days
		if (stats.size() > 1) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

			for (int currentIndex = 1; currentIndex < stats.size(); currentIndex++) {

				String olderEntryDate = stats.get(currentIndex - 1).getRequestDateString();
				String newerEntryDate = stats.get(currentIndex).getRequestDateString();

				try {
					Date olderDate = dateFormat.parse(olderEntryDate);
					Date newerDate = dateFormat.parse(newerEntryDate);

					long daysDistance = ((newerDate.getTime() - olderDate.getTime()) / 1000 / 60 / 60 / 24);

					for (int i = 1; i < daysDistance; i++) {
						AppStats missingEntry = new AppStats(stats.get(currentIndex - 1));
						missingEntry.setDate(new Date(missingEntry.getDate()
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
			stats.add(missingAppStatsPositionOffest.get(i), missingAppStats.get(i));
		}

		// calculate daily downloads
		int nullStartIndex = -1;
		boolean greaterNullDetected = false;

		for (int currentIndex = 1; currentIndex < stats.size(); currentIndex++) {

			// normalize daily, total & active
			int olderTotalValue = stats.get(currentIndex - 1).getTotalDownloads();
			int newerTotalValue = stats.get(currentIndex).getTotalDownloads();
			int totalValueDiff = newerTotalValue - olderTotalValue;

			int olderActiveValue = stats.get(currentIndex - 1).getActiveInstalls();
			int newerActiveValue = stats.get(currentIndex).getActiveInstalls();
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

					int totalDownload = stats.get(nullStartIndex - 1).getTotalDownloads();
					int activeInstall = stats.get(nullStartIndex - 1).getActiveInstalls();

					for (int j = nullStartIndex; j < currentIndex + 1; j++) {

						totalDownload += totalSmoothvalue;
						activeInstall += activeSmoothvalue;

						// for the last value, take rounding error in account
						if (currentIndex == j) {
							stats.get(j)
									.setDailyDownloads(totalSmoothvalue + roundingErrorTotal);
							stats.get(j).setTotalDownloads(totalDownload + roundingErrorTotal);
							stats.get(j).setActiveInstalls(activeInstall + roundingErrorActive);
						} else {
							stats.get(j).setDailyDownloads(totalSmoothvalue);
							stats.get(j).setTotalDownloads(totalDownload);
							stats.get(j).setActiveInstalls(activeInstall);
						}

						stats.get(j).setSmoothingApplied(true);
					}

					nullStartIndex = -1;
					greaterNullDetected = false;
				} else {

					stats.get(currentIndex).setDailyDownloads(totalValueDiff);
				}
			}
		}

		// reduce if limit exceeded (only if sync < 24h)
		if (stats.size() > limit) {
			stats = stats.subList(stats.size() - limit, stats.size());
		}

		float overallActiveInstallPercent = 0;
		float overallAvgRating = 0;

		// create rating diff
		AppStats prevStats = null;
		int value = 0;

		for (int i = 0; i < stats.size(); i++) {

			AppStats stat = stats.get(i);
			if (prevStats != null) {
				value = stat.getRating1() - prevStats.getRating1();
				if (value > highestRatingChange)
					highestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;
				stat.setRating1Diff(value);

				value = stat.getRating2() - prevStats.getRating2();
				if (value > highestRatingChange)
					highestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;
				stat.setRating2Diff(value);

				value = stat.getRating3() - prevStats.getRating3();
				if (value > highestRatingChange)
					highestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;
				stat.setRating3Diff(value);

				value = stat.getRating4() - prevStats.getRating4();
				if (value > highestRatingChange)
					highestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;
				stat.setRating4Diff(value);

				value = stat.getRating5() - prevStats.getRating5();
				if (value > highestRatingChange)
					highestRatingChange = value;
				if (value < lowestRatingChange)
					lowestRatingChange = value;

				stat.setRating5Diff(value);

				stat.setAvgRatingDiff(stat.getAvgRating() - prevStats.getAvgRating());
				stat.setRatingCountDiff(stat.getRatingCount() - prevStats.getRatingCount());
				stat.setNumberOfCommentsDiff(stat.getNumberOfComments()
						- prevStats.getNumberOfComments());
				stat.setActiveInstallsDiff(stat.getActiveInstalls()
						- prevStats.getActiveInstalls());
			}
			prevStats = stat;

			overallActiveInstallPercent += stat.getActiveInstallsPercent();
			overallAvgRating += stat.getAvgRating();

		}

		if (stats.size() > 0) {

			AppStats first = stats.get(0);
			AppStats last = stats.get(stats.size() - 1);

			overallStats.setActiveInstalls(last.getActiveInstalls() - first.getActiveInstalls());
			overallStats.setTotalDownloads(last.getTotalDownloads() - first.getTotalDownloads());
			overallStats.setRating1(last.getRating1() - first.getRating1());
			overallStats.setRating2(last.getRating2() - first.getRating2());
			overallStats.setRating3(last.getRating3() - first.getRating3());
			overallStats.setRating4(last.getRating4() - first.getRating4());
			overallStats.setRating5(last.getRating5() - first.getRating5());
			overallStats.init();
			overallStats.setDailyDownloads((last.getTotalDownloads() - first.getTotalDownloads())
					/ stats.size());

			BigDecimal avgBigDecimal = new BigDecimal(overallAvgRating / stats.size());
			avgBigDecimal = avgBigDecimal.setScale(3, BigDecimal.ROUND_HALF_UP);
			overallStats.setAvgRatingString(avgBigDecimal.toPlainString() + "");

			BigDecimal percentBigDecimal = new BigDecimal(overallActiveInstallPercent
					/ stats.size());
			percentBigDecimal = percentBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);

			overallStats.setActiveInstallsPercentString(percentBigDecimal.toPlainString() + "");

			double totalRevenue = 0;
			String currency = null;
			for (AppStats as : stats) {
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

	@Override
	public boolean applySmoothedValues() {
		for (AppStats stat : stats) {
			if (stat.isSmoothingApplied()) {
				return true;
			}
		}

		return false;
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
