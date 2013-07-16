package com.github.andlyticsproject;

import java.util.Date;
import java.util.List;

import com.github.andlyticsproject.model.AppStatsSummary;

public interface StatsView {

	public void updateView(AppStatsSummary appStatsList, List<Date> versionUpdateDates);

	public String getTitle();

	public void setCurrentChart(int currentPage, int column);
}
