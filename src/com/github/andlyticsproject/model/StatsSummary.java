package com.github.andlyticsproject.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class StatsSummary<T extends Statistic> {

	protected List<T> stats = new ArrayList<T>();
	protected T overallStats;

	public abstract void addStat(T stat);

	public List<T> getStats() {
		return Collections.unmodifiableList(stats);
	}

	public abstract void calculateOverallStats(int limit, boolean smoothEnabled);

	public T getOverallStats() {
		return overallStats;
	}

	public abstract boolean applySmoothedValues();

}
