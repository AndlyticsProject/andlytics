
package com.github.andlyticsproject.v2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.Comment;

/**
 * This class contains static methods used to parse JSON from {@link DeveloperConsoleV2}
 * 
 * See {@link https://github.com/AndlyticsProject/andlytics/wiki/Developer-Console-v2} for some more documentation
 *
 */
public class JsonParser {

	/**
	 * Parses the supplied JSON string and adds the extracted ratings to the supplied {@link AppStats} object
	 * @param json
	 * @param stats
	 * @throws JSONException
	 */
	protected static void parseRatings(String json, AppStats stats) throws JSONException {
		// Extract just the array with the values
		JSONArray values = new JSONObject(json).getJSONArray("result").getJSONArray(1)
				.getJSONArray(0);

		// Ratings are at index 2 - 6
		stats.setRating(values.getInt(2), values.getInt(3), values.getInt(4), values.getInt(5),
				values.getInt(6));

	}

	/**
	 * Parses the supplied JSON string and adds the extracted statistics to the supplied {@link AppStats} object
	 * based on the supplied statsType
	 * @param json
	 * @param stats
	 * @param statsType
	 * @throws JSONException
	 */
	protected static void parseStatistics(String json, AppStats stats, int statsType)
			throws JSONException {
		// Extract the top level values array
		JSONArray values = new JSONObject(json).getJSONArray("result").getJSONArray(1);
		/*
		 * null
		 * Nested array [null, [null, Array containing historical data]]
		 * null
		 * null
		 * null
		 * Nested arrays containing summary and historical data broken down by dimension e.g. Android version
		 * null
		 * null
		 * App name
		 * 
		 */
		// For now we just care about todays value, later we may delve into the historical and dimensioned data		
		JSONArray historicalData = values.getJSONArray(1).getJSONArray(1);
		JSONArray latestData = historicalData.getJSONArray(historicalData.length() - 1);
		/*
		 * null
		 * Date?
		 * value
		 */
		int latestValue = latestData.getInt(2);

		switch (statsType) {
			case DeveloperConsoleV2.STATS_TYPE_TOTAL_USER_INSTALLS:
				stats.setTotalDownloads(latestValue);
				break;
			case DeveloperConsoleV2.STATS_TYPE_ACTIVE_DEVICE_INSTALLS:
				stats.setActiveInstalls(latestValue);
				break;
			default:
				break;
		}

	}

	/**
	 * Parses the supplied JSON string and builds a list of apps from it
	 * @param json
	 * @param accountName
	 * @return List of apps
	 * @throws JSONException
	 */
	protected static List<AppInfo> parseAppInfos(String json, String accountName)
			throws JSONException {

		List<AppInfo> apps = new ArrayList<AppInfo>();
		// Extract the base array containing apps
		JSONArray jsonApps = new JSONObject(json).getJSONArray("result").getJSONArray(1);

		int numberOfApps = jsonApps.length();
		for (int i = 0; i < numberOfApps; i++) {
			AppInfo app = new AppInfo();
			app.setAccount(accountName);
			/* 
			 * Per app:
			 * null
			 * packageName
			 * Nested array with details
			 * null
			 * Nested array with version details
			 * Nested array with price details
			 * Date?
			 * Number? Always is 1, but might change for multi-consoles or people with loads of apps
			 */
			JSONArray jsonApp = jsonApps.getJSONArray(i).getJSONArray(1);
			String packageName = jsonApp.getString(1);
			if (packageName == null) {
				break; // Draft app
				// FIXME Check for half finished apps in the later sections as Google lets you setup apps bit by bit now
			}
			app.setPackageName(jsonApp.getString(1));

			/* 
			 * Per app details:
			 * null
			 * Country code
			 * App Name
			 * Description
			 * Unknown
			 * Last what's new
			 * 
			 */
			JSONArray appDetails = jsonApp.getJSONArray(2).getJSONArray(1).getJSONArray(0);
			app.setName(appDetails.getString(2));

			/*
			 * Per app version details:
			 * null
			 * null
			 * packageName
			 * versionNumber
			 * versionName
			 * null
			 * Array with app icon [null,null,null,icon]
			 */
			JSONArray appVersions = jsonApp.getJSONArray(4);
			JSONArray lastAppVersionDetails = appVersions.getJSONArray(appVersions.length() - 1)
					.getJSONArray(2);
			app.setVersionName(lastAppVersionDetails.getString(4));
			app.setIconUrl(lastAppVersionDetails.getJSONArray(6).getString(3));

			apps.add(app);

		}

		return apps;
	}

	/**
	 * Parses the supplied JSON string and returns the number of comments. 
	 * @param json
	 * @return
	 * @throws JSONException
	 */
	protected static int parseCommentsCount(String json) throws JSONException {
		// Just extract the number of comments
		/*
		 * null
		 * Array containing arrays of comments
		 * numberOfComments
		 */
		return new JSONObject(json).getJSONArray("result").getInt(2);
	}
	
	/**
	 * Parses the supplied JSON string and returns a list of comments.
	 * @param json
	 * @return
	 * @throws JSONException
	 */
	protected static List<Comment> parseComments(String json) throws JSONException {
		List<Comment> comments  = new ArrayList<Comment>();
		/*
		 * null
		 * Array containing arrays of comments
		 * numberOfComments
		 */
		JSONArray jsonComments = new JSONObject(json).getJSONArray("result").getJSONArray(1);
		int count = jsonComments.length();
		for (int i = 0; i < count; i++) {
			Comment comment = new Comment();
			JSONArray jsonComment = jsonComments.getJSONArray(i);
			/*
			 * null
			 * "gaia:17919762185957048423:1:vm:11887109942373535891", -- ID?
			* "REVIEWERS_NAME",
			* "1343652956570", -- DATE?
			* RATING,
			* null
			* "COMMENT",
			* null,
			* "VERSION_NAME",
			* [ null,
			*   "DEVICE_CODE_NAME",
			*   "DEVICE_MANFACTURER",
			*   "DEVICE_MODEL"
			* ],
			* "LOCALE",
			* null,
			* 0
			 */
			comment.setUser(jsonComment.getString(2));
			comment.setDate(parseDate(jsonComment.getLong(3)));
			comment.setRating(jsonComment.getInt(4));
			comment.setAppVersion(jsonComment.getString(8));
			comment.setText(jsonComment.getString(6));
			JSONArray jsonDevice = jsonComment.getJSONArray(9);
			comment.setDevice(jsonDevice.getString(2) + " " + jsonDevice.getString(3));
			
			comments.add(comment);			
		}		
		
		return comments;
	}
	
	/**
	 * Parses the given date
	 * @param unixDateCode
	 * @return
	 */
	private static Date parseDate(long unixDateCode){
		return new Date(unixDateCode);
	}

}
