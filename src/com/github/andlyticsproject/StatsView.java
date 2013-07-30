package com.github.andlyticsproject;

import com.github.andlyticsproject.model.Statistic;
import com.github.andlyticsproject.model.StatsSummary;

public interface StatsView<T extends Statistic> {

	public void updateView(StatsSummary<T> appStatsList);

	public String getTitle();

	public void setCurrentChart(int currentPage, int column);

	public int getCurrentChart();
}
