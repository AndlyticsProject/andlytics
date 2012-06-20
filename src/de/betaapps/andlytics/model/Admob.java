package de.betaapps.andlytics.model;

import java.text.DecimalFormat;
import java.util.Date;

public class Admob  {
    
    private String siteId;
    private Integer requests = 0; 
    private Integer houseadRequests = 0; 
    private Integer interstitialRequests = 0; 
    private Integer impressions = 0;  
    private Float fillRate = .0f; 
    private Float houseadFillRate = .0f; 
    private Float overallFillRate = .0f; 
    private Integer clicks = 0; 
    private Integer houseAdClicks = 0; 
    private Float ctr = .0f; 
    private Float ecpm = .0f; 
    private Float revenue = .0f; 
    private Float cpcRevenue = .0f;
    private Float cpmRevenue = .0f;
    private Integer exchangeDownloads = 0;
    private Date date;
    private static final int XK_cent                           = 0x00a2;  /* U+00A2 CENT SIGN */
    private static final DecimalFormat centsFormatter = new DecimalFormat("0.00"+((char)XK_cent));
    
    public String getSiteId() {
        return siteId;
    }
    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }
    public Integer getRequests() {
        return requests;
    }
    public void setRequests(Integer requests) {
        this.requests = requests;
    }
    public Integer getHouseadRequests() {
        return houseadRequests;
    }
    public void setHouseadRequests(Integer houseadRequests) {
        this.houseadRequests = houseadRequests;
    }
    public Integer getInterstitialRequests() {
        return interstitialRequests;
    }
    public void setInterstitialRequests(Integer interstitialRequests) {
        this.interstitialRequests = interstitialRequests;
    }
    public Integer getImpressions() {
        return impressions;
    }
    public void setImpressions(Integer impressions) {
        this.impressions = impressions;
    }
    public Float getFillRate() {
        return fillRate;
    }
    public void setFillRate(Float fillRate) {
        this.fillRate = fillRate;
    }
    public Float getHouseadFillRate() {
        return houseadFillRate;
    }
    public void setHouseadFillRate(Float houseadFillRate) {
        this.houseadFillRate = houseadFillRate;
    }
    public Float getOverallFillRate() {
        return overallFillRate;
    }
    public void setOverallFillRate(Float overallFillRate) {
        this.overallFillRate = overallFillRate;
    }
    public Integer getClicks() {
        return clicks;
    }
    public void setClicks(Integer clicks) {
        this.clicks = clicks;
    }
    public Integer getHouseAdClicks() {
        return houseAdClicks;
    }
    public void setHouseAdClicks(Integer houseAdClicks) {
        this.houseAdClicks = houseAdClicks;
    }
    public Float getCtr() {
        return ctr;
    }
    public void setCtr(Float ctr) {
        this.ctr = ctr;
    }
    public Float getEcpm() {
        return ecpm;
    }
    public String getEpcCents() {
      return centsFormatter.format(getEpc());
  }
    public Float getEpc() {
    return clicks>0?(revenue*100.f/clicks):-1;
  }
    public void setEcpm(Float ecpm) {
        this.ecpm = ecpm;
    }
    public Float getRevenue() {
        return revenue;
    }
    public void setRevenue(Float revenue) {
        this.revenue = revenue;
    }
    public Float getCpcRevenue() {
        return cpcRevenue;
    }
    public void setCpcRevenue(Float cpcRevenue) {
        this.cpcRevenue = cpcRevenue;
    }
    public Float getCpmRevenue() {
        return cpmRevenue;
    }
    public void setCpmRevenue(Float cpmRevenue) {
        this.cpmRevenue = cpmRevenue;
    }

    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
    public void setExchangeDownloads(Integer exchangeDownloads) {
        this.exchangeDownloads = exchangeDownloads;
    }
    public Integer getExchangeDownloads() {
        return exchangeDownloads;
    }


}
