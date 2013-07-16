package com.github.andlyticsproject;

import com.github.andlyticsproject.model.AppStatsSummary;

public interface StatsView {

	public void updateView(AppStatsSummary appStatsList);

	public String getTitle();

	public void setCurrentChart(int currentPage, int column);
}
