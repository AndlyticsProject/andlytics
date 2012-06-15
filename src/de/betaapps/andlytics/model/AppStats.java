package de.betaapps.andlytics.model;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AppStats {
    
	private int totalDownloads;
	
	private int activeInstalls;

	private int activeInstallsDiff;
	
	private int numberOfComments;

	private int numberOfCommentsDiff;
	
	private Date requestDate;
	
	private int dailyDownloads;
	
	private boolean smoothingApplied;

	private Integer versionCode;
	
	private Integer rating1 = 0;

	private Integer rating2 = 0;
	
	private Integer rating3 = 0;
	
	private Integer rating4 = 0;
	
	private Integer rating5 = 0;

	private Integer rating1Diff = 0;

	private Integer rating2Diff = 0;
	
	private Integer rating3Diff = 0;
	
	private Integer rating4Diff = 0;
	
	private Integer rating5Diff = 0;
	
	private float avgRating;
	
    private float avgRatingDiff;
	
	private int ratingCount;

	private int ratingCountDiff;

	private String avgRatingString;

	private String numberOfCommentsPercentString;

	private String ratingCountPercentString;

	private HashMap<Integer, String> ratingPercentStringMap;

	private String activeInstallsPercentString;

    private String avgRatingDiffString;
	
	public AppStats(){}
	
	/**
	 * Copy Constructor
	 *
	 * @param appStats a <code>AppStats</code> object
	 */
	public AppStats(AppStats appStats) 
	{
	    this.totalDownloads = appStats.totalDownloads;
	    this.activeInstalls = appStats.activeInstalls;
	    this.numberOfComments = appStats.numberOfComments;
	    this.requestDate = appStats.requestDate;
	    this.dailyDownloads = appStats.dailyDownloads;
	    this.smoothingApplied = appStats.smoothingApplied;
	    this.rating1 = appStats.rating1;
	    this.rating2 = appStats.rating2;
	    this.rating3 = appStats.rating3;
	    this.rating4 = appStats.rating4;
	    this.rating5 = appStats.rating5;
	    this.rating1Diff = appStats.rating1Diff;
	    this.rating2Diff = appStats.rating2Diff;
	    this.rating3Diff = appStats.rating3Diff;
	    this.rating4Diff = appStats.rating4Diff;
	    this.rating5Diff = appStats.rating5Diff;
	    this.avgRating = appStats.avgRating;
	    this.ratingCount = appStats.ratingCount;
	    this.setAvgRatingString(appStats.getAvgRatingString());
	    this.numberOfCommentsPercentString = appStats.numberOfCommentsPercentString;
	    this.ratingCountPercentString = appStats.ratingCountPercentString;
	    this.ratingPercentStringMap = appStats.ratingPercentStringMap;
	    this.setActiveInstallsPercentString(appStats.getActiveInstallsPercentString());
	    this.versionCode = appStats.versionCode;
	}
	
	public void init() {
		calcAvgRating();
		calcAvgRatingString();
		calcNumberOfCommentsPercentString();
		calcRatingCount();
		calcRatingCountPercentString();
		calsRatingPercentStrings();
		calcActiveInstallsPercentString();
		calcAvgRatingDiffString();
	}
	
	public int getTotalDownloads() {
		return totalDownloads;
	}

	public void setTotalDownloads(int totalDownloads) {
		this.totalDownloads = totalDownloads;
	}

	public int getActiveInstalls() {
		return activeInstalls;
	}

	public void setActiveInstalls(int activeInstalls) {
		this.activeInstalls = activeInstalls;
	}

	public int getNumberOfComments() {
		return numberOfComments;
	}

	public void setNumberOfComments(int numberOfComments) {
		this.numberOfComments = numberOfComments;
	}

	public Date getRequestDate() {
		return requestDate;
	}

	public void setRequestDate(Date requestDate) {
		this.requestDate = requestDate;
	}

	public int getDailyDownloads() {
		return dailyDownloads;
	}

	public void setDailyDownloads(int dailyDownloads) {
		this.dailyDownloads = dailyDownloads;
	}

	public boolean isSmoothingApplied() {
		return smoothingApplied;
	}

	public void setSmoothingApplied(boolean smoothingApplied) {
		this.smoothingApplied = smoothingApplied;
	}


	public void setRating(Map<Integer, Integer> rating) {
		
		if(rating.size() > 0) {
			this.rating1 = rating.get(1);
			this.rating2 = rating.get(2);
			this.rating3 = rating.get(3);
			this.rating4 = rating.get(4);
			this.rating5 = rating.get(5);
		}
	}

	public void addRating(int i, int value) {
		
		switch (i) {
		case 1:
			this.rating1 = value;
			break;
		case 2:
			this.rating2 = value;
			break;
		case 3:
			this.rating3 = value;
			break;
		case 4:
			this.rating4 = value;
			break;
		case 5:
			this.rating5 = value;
			break;

		default:
			break;
		}

	}

	public float getAvgRating() {
		
		return avgRating;
		
	}
	
	public void calcAvgRating() {
		
		float ratings = 0;
		float count = 0;
		
		for (int i = 1; i < 6; i++) {
		
			int value = 0;
			
			switch (i) {
			case 1:
				value = rating1;
				break;
			case 2:
				value = rating2;
				break;
			case 3:
				value = rating3;
				break;
			case 4:
				value = rating4;
				break;
			case 5:
				value = rating5;
				break;

			default:
				break;
			}
			
			ratings += i * value;
			count += value;
		}

		if(count < 1) {
			this.avgRating =  0;
		} else {
			this.avgRating = ratings / count;
		}
	}

	public int getRatingCount() {
		
		return ratingCount;
		
	}
	

	public void calcRatingCount() {
		this.ratingCount = rating1 + rating2 + rating3 + rating4 + rating5;
		
	}
	
	public void calcAvgRatingString() {
		BigDecimal ratingBigDecimal = new BigDecimal(getAvgRating());
		ratingBigDecimal = ratingBigDecimal.setScale(3, BigDecimal.ROUND_HALF_UP);
		this.setAvgRatingString(ratingBigDecimal.toPlainString());
	}
	
	public void calcAvgRatingDiffString() {
        BigDecimal ratingBigDecimal = new BigDecimal(getAvgRatingDiff());
        ratingBigDecimal = ratingBigDecimal.setScale(3, BigDecimal.ROUND_HALF_UP);
        this.setAvgRatingDiffString(ratingBigDecimal.toPlainString());
    }
	
	public String getAvgRatingString() {
		return this.avgRatingString;
	}

	public String getRatingPercentString(int rating) {
		return ratingPercentStringMap.get(rating);
	}

	public void calsRatingPercentStrings() {

		this.ratingPercentStringMap = new HashMap<Integer, String>();
		int sum = getRatingCount();

		for (int i = 1; i < 6; i++) {
			
			BigDecimal ratingBigDecimal = new BigDecimal(20);
			if(sum != 0) {
				
				Integer rate = null;
				
				switch (i) {
				case 1:
					rate = rating1;
					break;
				case 2:
					rate = rating2;
					break;
				case 3:
					rate = rating3;
					break;
				case 4:
					rate = rating4;
					break;
				case 5:
					rate = rating5;
					break;

				default:
					break;
				}
				
				if(rate == null || rate < 1) {
					rate = 0;
				}
				ratingBigDecimal = new BigDecimal(100.f/sum*rate);
			}
			ratingBigDecimal = ratingBigDecimal.setScale(2, BigDecimal.ROUND_HALF_EVEN);
			ratingPercentStringMap.put(i, ratingBigDecimal.toPlainString() + "%");
		}
	}

	
	public String getActiveInstallsPercentString() {
		return activeInstallsPercentString;
	}

	public void calcActiveInstallsPercentString() {
		BigDecimal percentBigDecimal = new BigDecimal(getActiveInstallsPercent());
		percentBigDecimal = percentBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
		this.setActiveInstallsPercentString(percentBigDecimal.toPlainString());
	}
	
	public String getRequestDateString() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		return dateFormat.format(getRequestDate());
	}
	
	public double getActiveInstallsPercent(){
		
		if(totalDownloads < 1) {
			return 0;
		}
		
		return (activeInstalls * 100.0) / totalDownloads;

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + activeInstalls;
		result = prime * result + dailyDownloads;
		result = prime * result + numberOfComments;
		result = prime * result + ((requestDate == null) ? 0 : requestDate.hashCode());
		result = prime * result + (smoothingApplied ? 1231 : 1237);
		result = prime * result + totalDownloads;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AppStats other = (AppStats) obj;
		if (activeInstalls != other.activeInstalls)
			return false;
		if (dailyDownloads != other.dailyDownloads)
			return false;
		if (numberOfComments != other.numberOfComments)
			return false;
		if (requestDate == null) {
			if (other.requestDate != null)
				return false;
		} else if (!requestDate.equals(other.requestDate))
			return false;
		if (smoothingApplied != other.smoothingApplied)
			return false;
		if (totalDownloads != other.totalDownloads)
			return false;
		return true;
	}

	public String getNumberOfCommentsPercentString() {
		return numberOfCommentsPercentString;
	}
	

	public void calcNumberOfCommentsPercentString() {
		
		int numberOfComments = getNumberOfComments();
		float percent = 0.0f;
		if(totalDownloads > 0) {
			percent = 100.0f / totalDownloads * numberOfComments;
		}
		BigDecimal percentBigDecimal = new BigDecimal(percent);
		percentBigDecimal = percentBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
		this.numberOfCommentsPercentString =  percentBigDecimal.toPlainString();
	}

	public String getRatingCountPercentString() {
		return ratingCountPercentString;
	}
	
	public void calcRatingCountPercentString() {
		int numberOfComments = getRatingCount();
		float percent = 0.0f;
		if(totalDownloads > 0) {
			percent = 100.0f / totalDownloads *  numberOfComments;
		}
		BigDecimal percentBigDecimal = new BigDecimal(percent);
		percentBigDecimal = percentBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
		this.ratingCountPercentString = percentBigDecimal.toPlainString();
	}
	

	public Integer getRating1() {
		return rating1;
	}

	public void setRating1(Integer rating1) {
		this.rating1 = rating1;
	}

	public Integer getRating2() {
		return rating2;
	}

	public void setRating2(Integer rating2) {
		this.rating2 = rating2;
	}

	public Integer getRating3() {
		return rating3;
	}

	public void setRating3(Integer rating3) {
		this.rating3 = rating3;
	}

	public Integer getRating4() {
		return rating4;
	}

	public void setRating4(Integer rating4) {
		this.rating4 = rating4;
	}

	public Integer getRating5() {
		return rating5;
	}

	public Integer getRating1Diff() {
		return rating1Diff;
	}

	public void setRating1Diff(Integer rating1Diff) {
		this.rating1Diff = rating1Diff;
	}

	public Integer getRating2Diff() {
		return rating2Diff;
	}

	public void setRating2Diff(Integer rating2Diff) {
		this.rating2Diff = rating2Diff;
	}

	public Integer getRating3Diff() {
		return rating3Diff;
	}

	public void setRating3Diff(Integer rating3Diff) {
		this.rating3Diff = rating3Diff;
	}

	public Integer getRating4Diff() {
		return rating4Diff;
	}

	public void setRating4Diff(Integer rating4Diff) {
		this.rating4Diff = rating4Diff;
	}

	public Integer getRating5Diff() {
		return rating5Diff;
	}

	public void setRating5Diff(Integer rating5Diff) {
		this.rating5Diff = rating5Diff;
	}

	public void setRating5(Integer rating5) {
		this.rating5 = rating5;
	}

    public void setAvgRatingDiff(float avgRatingDiff) {
        this.avgRatingDiff = avgRatingDiff;
    }

    public float getAvgRatingDiff() {
        return avgRatingDiff;
    }

    public void setRatingCountDiff(int ratingCountDiff) {
        this.ratingCountDiff = ratingCountDiff;
    }

    public int getRatingCountDiff() {
        return ratingCountDiff;
    }

    public void setAvgRatingDiffString(String avgRatingDiffString) {
        this.avgRatingDiffString = avgRatingDiffString;
    }

    public String getAvgRatingDiffString() {
        return avgRatingDiffString;
    }

    public void setNumberOfCommentsDiff(int numberOfCommentsDiff) {
        this.numberOfCommentsDiff = numberOfCommentsDiff;
    }

    public int getNumberOfCommentsDiff() {
        return numberOfCommentsDiff;
    }

    public void setActiveInstallsDiff(int activeInstallsDiff) {
        this.activeInstallsDiff = activeInstallsDiff;
    }

    public int getActiveInstallsDiff() {
        return activeInstallsDiff;
    }

    public void setVersionCode(Integer versionCode) {
        this.versionCode = versionCode;
    }

    public Integer getVersionCode() {
        return versionCode;
    }

    public void setActiveInstallsPercentString(String activeInstallsPercentString) {
        this.activeInstallsPercentString = activeInstallsPercentString;
    }

    public void setAvgRatingString(String avgRatingString) {
        this.avgRatingString = avgRatingString;
    }

}
