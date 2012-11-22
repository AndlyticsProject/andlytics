
package com.github.andlyticsproject.console.v2;

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
 * This class contains static methods used to parse JSON from {@link DevConsoleV2}
 * 
 * See {@link https://github.com/AndlyticsProject/andlytics/wiki/Developer-Console-v2} for some more documentation
 *
 */
public class JsonParser {
	
	private JsonParser() {
		
	}

	/**
	 * Parses the supplied JSON string and adds the extracted ratings to the supplied {@link AppStats} object
	 * @param json
	 * @param stats
	 * @throws JSONException
	 */
	static void parseRatings(String json, AppStats stats) throws JSONException {
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
	 * Not used at the moment
	 * @param json
	 * @param stats
	 * @param statsType
	 * @throws JSONException
	 */
	static void parseStatistics(String json, AppStats stats, int statsType)
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
		 * Date
		 * [null, value]
		 */
		int latestValue = latestData.getJSONArray(2).getInt(1);

		switch (statsType) {
		case DevConsoleV2Protocol.STATS_TYPE_TOTAL_USER_INSTALLS:
				stats.setTotalDownloads(latestValue);
				break;
		case DevConsoleV2Protocol.STATS_TYPE_ACTIVE_DEVICE_INSTALLS:
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
	static List<AppInfo> parseAppInfos(String json, String accountName)
			throws JSONException {

		Date now = new Date();
		List<AppInfo> apps = new ArrayList<AppInfo>();
		// Extract the base array containing apps
		JSONArray jsonApps = new JSONObject(json).getJSONArray("result").getJSONArray(1);

		int numberOfApps = jsonApps.length();
		for (int i = 0; i < numberOfApps; i++) {
			AppInfo app = new AppInfo();
			app.setAccount(accountName);
			app.setLastUpdate(now);
			/* 
			 * Per app:
			 * null
			 * [ APP_INFO_ARRAY
			 ** null
			 ** packageName
			 ** Nested array with details
			 ** null
			 ** Nested array with version details
			 ** Nested array with price details
			 ** Last update Date
			 ** Number [1=published, 5 = draft?]
			 * ]
			 * null
			 * [ APP_STATS_ARRAY
			 ** null,
			 ** Active installs
			 ** Total ratings
			 ** Average rating
			 ** Errors
			 ** Total installs
			 * ]
			 */
			JSONArray jsonApp = jsonApps.getJSONArray(i);
			JSONArray jsonAppInfo = jsonApp.getJSONArray(1);
			String packageName = jsonAppInfo.getString(1);
			// Look for "tmp.7238057230750432756094760456.235728507238057230542"
			if (packageName == null || (packageName.startsWith("tmp.")
					&& Character.isDigit(packageName.charAt(4)))) {
				break;
				// Draft app
			}
			// Check number code and last updated date
			if (jsonAppInfo.getInt(7) == 5 || jsonAppInfo.optInt(6) == 0) {
				break;
				// Probably a draft app
			}
			app.setPackageName(packageName);

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
			JSONArray appDetails = jsonAppInfo.getJSONArray(2).getJSONArray(1).getJSONArray(0);
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
			JSONArray appVersions = jsonAppInfo.getJSONArray(4);
			JSONArray lastAppVersionDetails = appVersions.getJSONArray(appVersions.length() - 1)
					.getJSONArray(2);
			app.setVersionName(lastAppVersionDetails.getString(4));
			app.setIconUrl(lastAppVersionDetails.getJSONArray(6).getString(3));

			// App stats
			/*
			 * null,
			 * Active installs
			 * Total ratings
			 * Average rating
			 * Errors
			 * Total installs
			 */
			JSONArray jsonAppStats = jsonApp.getJSONArray(3);
			AppStats stats = new AppStats();
			stats.setRequestDate(now);
			stats.setActiveInstalls(jsonAppStats.getInt(1));
			stats.setTotalDownloads(jsonAppStats.getInt(5));
			app.setLatestStats(stats);

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
	static int parseCommentsCount(String json) throws JSONException {
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
	static List<Comment> parseComments(String json) throws JSONException {
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
			// Example with developer reply
			/*
			[
			   null,
			   "gaia:12824185113034449316:1:vm:18363775304595766012",
			   "Micka�l",
			   "1350333837326",
			   1,
			   "",
			   "Nul\tNul!! N'arrive pas a scanner le moindre code barre!",
			   73,
			   "3.2.5",
			   [
			      null,
			      "X10i",
			      "SEMC",
			      "Xperia X10"
			   ],
			   "fr_FR",
			   [
			      null,
			      "Prixing fonctionne pourtant bien sur Xperia X10. Essayez de prendre un minimum de recul, au moins 20 � 30cm, �vitez les ombres et les reflets. N'h�sitez pas � nous �crire sur contact@prixing.fr pour une assistance personnalis�e.",
			      null,
			      "1350393460968"
			   ],
			   1
			]
			 */
			comment.setUser(jsonComment.getString(2));
			comment.setDate(parseDate(jsonComment.getLong(3)));
			comment.setRating(jsonComment.getInt(4));
			String version = jsonComment.getString(8);
			if (version != null && !version.equals("null")) {
				comment.setAppVersion(version);
			}
			comment.setText(jsonComment.getString(6));
			JSONArray jsonDevice = jsonComment.optJSONArray(9);
			if (jsonDevice != null) {
				String device = jsonDevice.optString(2) + " " + jsonDevice.optString(3);
				comment.setDevice(device.trim());
			}

			JSONArray jsonReply = jsonComment.optJSONArray(11);
			if (jsonReply != null) {
				Comment reply = new Comment(true);
				reply.setText(jsonReply.getString(1));
				reply.setReplyDate(parseDate(jsonReply.getLong(3)));
				reply.setDate(comment.getDate());
				comment.setReply(reply);
			}

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
