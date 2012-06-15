package de.betaapps.andlytics.model;

import java.util.ArrayList;
import java.util.List;

public class AppStatsList {
	
	private List<AppStats> appStats = new ArrayList<AppStats>();
	
	private Integer highestRatingChange = 0;
	
	private Integer lowestRatingChange = 0;
	
	private AppStats overall;

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

	public void setAppStats(List<AppStats> appStats) {
		this.appStats = appStats;
	}

	public List<AppStats> getAppStats() {
		return appStats;
	}

    public void setOverall(AppStats overall) {
        this.overall = overall;
    }

    public AppStats getOverall() {
        return overall;
    }
	
}
