package de.betaapps.andlytics.model;

import java.util.List;

public class AdmobList {
    
    private List<Admob> admobs;
    
    private Admob overallStats;

    public void setOverallStats(Admob overallStats) {
        this.overallStats = overallStats;
    }

    public Admob getOverallStats() {
        return overallStats;
    }

    public void setAdmobs(List<Admob> admobs) {
        this.admobs = admobs;
    }

    public List<Admob> getAdmobs() {
        return admobs;
    }

}
