package com.github.andlyticsproject.console.v2;

import android.util.Log;

import com.github.andlyticsproject.console.DevConsoleException;
import com.github.andlyticsproject.model.AppDetails;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.Comment;
import com.github.andlyticsproject.model.RevenueSummary;
import com.github.andlyticsproject.util.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class contains static methods used to parse JSON from {@link DevConsoleV2}
 * 
 * See {@link https://github.com/AndlyticsProject/andlytics/wiki/Developer-Console-v2} for some more
 * documentation
 * 
 */
public class JsonParser {

	private static final int REVENUE_LAST_30DAYS = 30;

	private static final int REVENUE_LAST_7DAYS = 7;

	private static final int REVENUE_LAST_DAY = 1;

	private static final int REVENUE_OVERALL = -1;

	private static final String TAG = JsonParser.class.getSimpleName();

	private static final boolean DEBUG = true;

	private JsonParser() {

	}

	/**
	 * Parses the supplied JSON string and adds the extracted ratings to the supplied
	 * {@link AppStats} object
	 * 
	 * @param json
	 * @param stats
	 * @throws JSONException
	 */
	static void parseRatings(String json, AppStats stats) throws JSONException {
		JSONObject data = new JSONObject(json).getJSONObject("result").getJSONArray("1")
				.getJSONObject(0);

		// Ratings values is in 8
		JSONObject values = data.getJSONObject("8");
		// Ratings are at index 1 - 5
		stats.setRating(values.getInt("1"), values.getInt("2"), values.getInt("3"),
				values.getInt("4"), values.getInt("5"));

		// Comment count is in 7
		stats.setNumberOfComments(data.getInt("7"));
	}

	/**
	 * Parses the supplied JSON string and adds the extracted statistics to the supplied
	 * {@link AppStats} object
	 * based on the supplied statsType
	 * Not used at the moment
	 * 
	 * @param json
	 * @param stats
	 * @param statsType
	 * @throws JSONException
	 */
	static void parseStatistics(String json, AppStats stats, int statsType) throws JSONException {
		// Extract the top level values array
		JSONObject values = new JSONObject(json).getJSONObject("result").getJSONObject("1");
		/*
		 * null
		 * Nested array [null, [null, Array containing historical data]]
		 * null
		 * null
		 * null
		 * Nested arrays containing summary and historical data broken down by dimension e.g.
		 * Android version
		 * null
		 * null
		 * App name
		 */
		// For now we just care about todays value, later we may delve into the historical and
		// dimensioned data
		JSONArray historicalData = values.getJSONObject("1").getJSONArray("1");
		JSONObject latestData = historicalData.getJSONObject(historicalData.length() - 1);
		/*
		 * null
		 * Date
		 * [null, value]
		 */
		int latestValue = latestData.getJSONObject("2").getInt("1");

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
	 * 
	 * @param json
	 * @param accountName
	 * @param skipIncomplete
	 * @return List of apps
	 * @throws JSONException
	 */
	static List<AppInfo> parseAppInfos(String json, String accountName, boolean skipIncomplete)
			throws JSONException {

		Date now = new Date();
		List<AppInfo> apps = new ArrayList<AppInfo>();
		// Extract the base array containing apps
		JSONObject result = new JSONObject(json).getJSONObject("result");
		if (DEBUG) {
			pp("result", result);
		}

		JSONArray jsonApps = result.optJSONArray("1");
		if (DEBUG) {
			pp("jsonApps", jsonApps);
		}
		if (jsonApps == null) {
			// no apps yet?
			return apps;
		}

		int numberOfApps = jsonApps.length();
		Log.d(TAG, String.format("Found %d apps in JSON", numberOfApps));
		for (int i = 0; i < numberOfApps; i++) {
			AppInfo app = new AppInfo();
			app.setAccount(accountName);
			app.setLastUpdate(now);
			/*
			 * Per app key indexed objects:
			 * "1" -> [ APP_INFO
			 * * packageName
			 * * publishState
			 * * ?
			 * ]
			 * "3" -> [ APP_STATS
			 * * Active installs
			 * * Total ratings
			 * * Average rating
			 * * Errors
			 * * Total installs
			 * ]
			 * "6" -> [ APP_DETAILS
			 * * App name
			 * * Low res icon
			 * * High res icon
			 * * Version
			 * * Price
			 * * ...
			 * * Last update
			 * ]
			 */
			JSONObject jsonApp = jsonApps.getJSONObject(i);
			/*
			 * [ APP_INFO
			 * * "1" -> packageName
			 * * "7" -> publishState
			 * * "11" -> ?
			 * ]
			 */
			JSONObject jsonAppInfo = jsonApp.getJSONObject("1");
			if (DEBUG) {
				pp("jsonAppInfo", jsonAppInfo);
			}
			String packageName = jsonAppInfo.getString("1");
			// Look for "tmp.7238057230750432756094760456.235728507238057230542"
			if (packageName == null
					|| (packageName.startsWith("tmp.") && Character.isDigit(packageName.charAt(4)))) {
				Log.d(TAG, String.format("Skipping draft app %d, package name=%s", i, packageName));
				continue;
				// Draft app
			}

			// Check number code and last updated date
			// Published: 1
			// Unpublished: 2
			// Draft: 5
			// Draft w/ in-app items?: 6
			// TODO figure out the rest and add don't just skip, filter, etc. Cf. #223
			int publishState = jsonAppInfo.optInt("7");
			Log.d(TAG, String.format("%s: publishState=%d", packageName, publishState));
			if (publishState != 1) {
				// Not a published app, skipping
				Log.d(TAG, String.format(
						"Skipping app %d with state != 1: package name=%s: state=%d", i,
						packageName, publishState));
				continue;
			}
			app.setPublishState(publishState);
			app.setPackageName(packageName);

			/* App details
			 * * App name
			 * * Low res icon
			 * * High res icon
			 * * Version
			 * * Price?
			 * * ...
			 * * Last update
			 */
			// skip if we can't get all the data
			// XXX should we just let this crash so we know there is a problem?
			if (!jsonApp.has("6")) {
				if (skipIncomplete) {
					Log.d(TAG, String.format(
							"Skipping app %d because no app details found: package name=%s", i,
							packageName));
				} else {
					Log.d(TAG, "Adding incomplete app: " + packageName);
					apps.add(app);
				}
				continue;
			}
			JSONObject jsonAppDetails = jsonApp.getJSONObject("6");
			if (DEBUG) {
				pp("jsonAppDetails", jsonAppDetails);
			}
			app.setName(jsonAppDetails.getString("1"));

			String description = ""; //appDetails.optString("3");
			String changelog = ""; //appDetails.optString("5");
			Long lastPlayStoreUpdate = jsonAppDetails.getLong("8");
			AppDetails details = new AppDetails(description, changelog, lastPlayStoreUpdate);
			app.setDetails(details);

			app.setVersionName(jsonAppDetails.optString("4", ""));
			if (jsonAppDetails.has("3")) {
				app.setIconUrl(jsonAppDetails.getString("3"));
			}



			// App stats
			/*
			 * Active installs
			 * Total ratings
			 * Average rating
			 * Errors
			 * Total installs
			 */
			if (!jsonApp.has("3")) {
				if (skipIncomplete) {
					Log.d(TAG, String.format(
							"Skipping app %d because no app stats found: package name=%s", i,
							packageName));
				} else {
					Log.d(TAG, "Adding incomplete app: " + packageName);
					apps.add(app);
				}
				continue;
			}
			JSONObject jsonAppStats = jsonApp.getJSONObject("3");
			if (DEBUG) {
				pp("jsonAppStats", jsonAppStats);
			}
			AppStats stats = new AppStats();
			stats.setDate(now);
			if (jsonAppStats.length() < 4) {
				// no statistics (yet?) or weird format
				// TODO do we need differentiate?
				stats.setActiveInstalls(0);
				stats.setTotalDownloads(0);
				stats.setNumberOfErrors(0);
			} else {
				//stats.setActiveInstalls(jsonAppStats.optInt("1", 0));
				stats.setTotalDownloads(jsonAppStats.getInt("5"));
				stats.setNumberOfErrors(jsonAppStats.optInt("4"));
				stats.setActiveInstalls(jsonAppStats.optInt("7", 0));
			}
			app.setLatestStats(stats);

			apps.add(app);
		}

		return apps;
	}

	private static void pp(String name, JSONArray jsonArr) {
		try {
			String pp = jsonArr == null ? "null" : jsonArr.toString(2);
			Log.d(TAG, String.format("%s: %s", name, pp));
			FileUtils.writeToDebugDir(name + "-pp.json", pp);
		} catch (JSONException e) {
			Log.w(TAG, "Error printing JSON: " + e.getMessage(), e);
		}
	}

	private static void pp(String name, JSONObject jsonObj) {
		try {
			String pp = jsonObj == null ? "null" : jsonObj.toString(2);
			Log.d(TAG, String.format("%s: %s", name, pp));
			FileUtils.writeToDebugDir(name + "-pp.json", pp);
		} catch (JSONException e) {
			Log.w(TAG, "Error printing JSON: " + e.getMessage(), e);
		}
	}

	/**
	 * Parses the supplied JSON string and returns a list of comments.
	 * 
	 * @param json
	 * @return
	 * @throws JSONException
	 */
	static List<Comment> parseComments(String json) throws JSONException {
		List<Comment> comments = new ArrayList<Comment>();
		/*
		 * null
		 * Array containing arrays of comments
		 * numberOfComments
		 */
		JSONArray jsonComments = new JSONObject(json).getJSONObject("result").getJSONArray("1");
		int count = jsonComments.length();
		for (int i = 0; i < count; i++) {
			Comment comment = new Comment();
			JSONObject jsonComment = jsonComments.getJSONObject(i);
			// TODO These examples are out of date and need updating
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
			 * "DEVICE_CODE_NAME",
			 * "DEVICE_MANFACTURER",
			 * "DEVICE_MODEL"
			 * ],
			 * "LOCALE",
			 * null,
			 * 0
			 */
			// Example with developer reply
			/*
			 * [
			 * null,
			 * "gaia:12824185113034449316:1:vm:18363775304595766012",
			 * "Micka�l",
			 * "1350333837326",
			 * 1,
			 * "",
			 * "Nul\tNul!! N'arrive pas a scanner le moindre code barre!",
			 * 73,
			 * "3.2.5",
			 * [
			 * null,
			 * "X10i",
			 * "SEMC",
			 * "Xperia X10"
			 * ],
			 * "fr_FR",
			 * [
			 * null,
			 * "Prixing fonctionne pourtant bien sur Xperia X10. Essayez de prendre un minimum de recul, au moins 20 � 30cm, �vitez les ombres et les reflets. N'h�sitez pas � nous �crire sur contact@prixing.fr pour une assistance personnalis�e."
			 * ,
			 * null,
			 * "1350393460968"
			 * ],
			 * 1
			 * ]
			 */
			String uniqueId = jsonComment.getString("1");
			comment.setUniqueId(uniqueId);
			String user = jsonComment.optString("2");
			if (user != null && !"".equals(user) && !"null".equals(user)) {
				comment.setUser(user);
			}
			comment.setDate(parseDate(jsonComment.getLong("3")));
			comment.setRating(jsonComment.getInt("4"));
			String version = jsonComment.optString("7");
			if (version != null && !"".equals(version) && !version.equals("null")) {
				comment.setAppVersion(version);
			}

			JSONObject jsonCommentReview = jsonComment.optJSONObject("5");
			String commentLang = jsonCommentReview.getString("1");
			String commentText = jsonCommentReview.getString("3");
			String commentTitle = jsonCommentReview.getString("2");

			if (commentTitle.length() == 0) {
				// Title field is empty, see if the title is part of the comment text
				String originalTitleAndComment[] = commentText.split("\\t");
				if (originalTitleAndComment.length == 2) {
					commentTitle = originalTitleAndComment[0];
					commentText = originalTitleAndComment[1];
				}
			}

			comment.setLanguage(commentLang);
			comment.setOriginalText(commentText);
			// overwritten if translation is available
			comment.setText(commentText);
			comment.setOriginalTitle(commentTitle);
			comment.setTitle(commentTitle);

			JSONObject translation = jsonComment.optJSONObject("11");
			if (translation != null) {
				String displayLanguage = Locale.getDefault().getLanguage();
				String translationLang = translation.getString("1");

				if (translation.has("2")) {
					String translationTitle = translation.getString("2");
					if (translationLang.contains(displayLanguage)) {
						comment.setTitle(translationTitle);
					}
				}
				// Apparently, a translation body is not always provided
				// Possibly happens if the translation fails or equals the original
				if (translation.has("3")) {
					String translationText = translation.getString("3");
					if (translationLang.contains(displayLanguage)) {
						comment.setText(translationText);
					}
				}
			}

			JSONObject jsonDevice = jsonComment.optJSONObject("16");
			if (jsonDevice != null) {
				String device = jsonDevice.optString("3");
				JSONArray extraInfo = jsonDevice.optJSONArray("2");
				if (extraInfo != null) {
					device += " " + extraInfo.optString(0);
				}
				comment.setDevice(device.trim());
			}

			JSONObject jsonReply = jsonComment.optJSONObject("9");
			if (jsonReply != null) {
				Comment reply = new Comment(true);
				reply.setText(jsonReply.getString("1"));
				reply.setDate(parseDate(jsonReply.getLong("3")));
				reply.setOriginalCommentDate(comment.getDate());
				comment.setReply(reply);
			}

			comments.add(comment);
		}

		return comments;
	}

	static Comment parseCommentReplyResponse(String json) throws JSONException {
		// {"result":{"1":{"1":"REPLY","3":"TIME_STAMP"},"2":true},"xsrf":"XSRF_TOKEN"}
		// or
		// {"error":{"data":{"1":ERROR_CODE},"code":ERROR_CODE}}
		JSONObject jsonObj = new JSONObject(json);
		if (jsonObj.has("error")) {
			throw parseError(jsonObj, "replying to comments");
		}
		JSONObject replyObj = jsonObj.getJSONObject("result").getJSONObject("1");

		Comment result = new Comment(true);
		result.setText(replyObj.getString("1"));
		result.setDate(parseDate(Long.parseLong(replyObj.getString("3"))));

		return result;
	}

	private static DevConsoleException parseError(JSONObject jsonObj, String message)
			throws JSONException {
		JSONObject errorObj = jsonObj.getJSONObject("error");
		String data = errorObj.getJSONObject("data").optString("1");
		String errorCode = errorObj.optString("code");

		return new DevConsoleException(String.format("Error %s: %s, errorCode=%s", message, data,
				errorCode));
	}

	static RevenueSummary parseRevenueResponse(String json, String currency) throws JSONException {
		JSONObject jsonObj = new JSONObject(json);
		if (jsonObj.has("error")) {
			throw parseError(jsonObj, "fetch revenue summary");
		}

		JSONArray arr = jsonObj.getJSONObject("result").getJSONArray("1");
		if (arr.length() == 0) {
			return null;
		}

		JSONObject summaryObj = arr.getJSONObject(0);
		// free app or no revenue available
		if (!summaryObj.has("1")) {
			return null;
		}

		JSONArray revenueArr = summaryObj.getJSONArray("1");

		double overall = 0;
		double lastDay = 0;
		double last30Days = 0;
		double last7Days = 0;

		for (int i = 0; i < revenueArr.length(); i++) {
			JSONObject revenueObj = revenueArr.getJSONObject(i);
			int period = revenueObj.getJSONArray("1").getInt(0);
			double value = revenueObj.getJSONArray("2").optJSONObject(0).optDouble("1", 0.0) / 1000000;

			switch (period) {
			case REVENUE_OVERALL:
				overall = value;
				break;
			case REVENUE_LAST_DAY:
				lastDay = value;
				break;
			case REVENUE_LAST_7DAYS:
				last7Days = value;
				break;
			case REVENUE_LAST_30DAYS:
				last30Days = value;
				break;
			default:
				throw new IllegalArgumentException("Unknown revenue period: " + period);
			}
		}

		long timeInMillis = summaryObj.getLong("2");
		Calendar cal = Calendar.getInstance();
/*
		// TODO Work out timezone
		String tzStr = summaryObj.optString("3");
		TimeZone tz = TimeZone.getTimeZone(tzStr);
		cal = Calendar.getInstance(tz);
*/
		cal.setTimeInMillis(timeInMillis);

		return RevenueSummary.createTotal(currency, cal.getTime(), lastDay, last7Days, last30Days,
				overall);
	}

	/**
	 * Parses the given date
	 * 
	 * @param unixDateCode
	 * @return
	 */
	private static Date parseDate(long unixDateCode) {
		return new Date(unixDateCode);
	}

}
