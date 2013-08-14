package com.github.andlyticsproject.console.v2;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.JsonReader;
import android.util.Log;

import com.github.andlyticsproject.console.DevConsoleException;
import com.github.andlyticsproject.model.AppDetails;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.Comment;
import com.github.andlyticsproject.model.Revenue;
import com.github.andlyticsproject.model.RevenueSummary;
import com.github.andlyticsproject.util.FileUtils;

/**
 * This class contains static methods used to parse JSON from {@link DevConsoleV2}
 * 
 * See {@link https://github.com/AndlyticsProject/andlytics/wiki/Developer-Console-v2} for some more
 * documentation
 * 
 */
public class JsonParser {

	private static final String TAG = JsonParser.class.getSimpleName();

	private static final boolean DEBUG = false;

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
		// Extract just the array with the values
		JSONObject values = new JSONObject(json).getJSONObject("result").getJSONArray("1")
				.getJSONObject(0);

		// Ratings are at index 2 - 6
		stats.setRating(values.getInt("2"), values.getInt("3"), values.getInt("4"),
				values.getInt("5"), values.getInt("6"));

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
			// Per app:
			// 1 : { 1: package name,
			// 2 : { 1: [{1 : lang, 2: name, 3: description, 4: ??, 5: what's new}], 2 : ?? },
			// 3 : ??,
			// 4 : update history,
			// 5 : price,
			// 6 : update date,
			// 7 : state?
			// }
			// 2 : {}
			// 3 : { 1: active dnd, 2: # ratings, 3: avg rating, 4: ???, 5: total dnd }

			// arrays have changed to objects, with the index as the key
			/*
			 * Per app:
			 * null
			 * [ APP_INFO_ARRAY
			 * * null
			 * * packageName
			 * * Nested array with details
			 * * null
			 * * Nested array with version details
			 * * Nested array with price details
			 * * Last update Date
			 * * Number [1=published, 5 = draft?]
			 * ]
			 * null
			 * [ APP_STATS_ARRAY
			 * * null,
			 * * Active installs
			 * * Total ratings
			 * * Average rating
			 * * Errors
			 * * Total installs
			 * ]
			 */
			JSONObject jsonApp = jsonApps.getJSONObject(i);
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

			/*
			 * Per app details:
			 * 1: Country code
			 * 2: App Name
			 * 3: Description
			 * 4: Promo text
			 * 5: Last what's new
			 */
			// skip if we can't get all the data
			// XXX should we just let this crash so we know there is a problem?
			if (!jsonAppInfo.has("2")) {
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
			if (!jsonAppInfo.has("4")) {
				if (skipIncomplete) {
					Log.d(TAG, String.format(
							"Skipping app %d because no versions info found: package name=%s", i,
							packageName));
				} else {
					Log.d(TAG, "Adding incomplete app: " + packageName);
					apps.add(app);
				}
				continue;
			}

			JSONObject appDetails = jsonAppInfo.getJSONObject("2").getJSONArray("1")
					.getJSONObject(0);
			if (DEBUG) {
				pp("appDetails", appDetails);
			}
			app.setName(appDetails.getString("2"));

			String description = appDetails.getString("3");
			String changelog = appDetails.optString("5");
			Long lastPlayStoreUpdate = jsonAppInfo.optLong("6");
			AppDetails details = new AppDetails(description, changelog, lastPlayStoreUpdate);
			app.setDetails(details);

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
			// XXX
			JSONArray appVersions = jsonAppInfo.optJSONObject("4").optJSONArray("1");
			if (DEBUG) {
				pp("appVersions", appVersions);
			}
			if (appVersions == null) {
				if (skipIncomplete) {
					Log.d(TAG, String.format(
							"Skipping app %d because no versions info found: package name=%s", i,
							packageName));
				} else {
					Log.d(TAG, "Adding incomplete app: " + packageName);
					apps.add(app);
				}
				continue;
			}
			JSONObject lastAppVersionDetails = appVersions.getJSONObject(appVersions.length() - 1)
					.getJSONObject("2");
			if (DEBUG) {
				pp("lastAppVersionDetails", lastAppVersionDetails);
			}
			app.setVersionName(lastAppVersionDetails.getString("4"));
			app.setIconUrl(lastAppVersionDetails.getJSONObject("6").getString("3"));

			// App stats
			/*
			 * null,
			 * Active installs
			 * Total ratings
			 * Average rating
			 * Errors
			 * Total installs
			 */
			// XXX this index might not be correct for all apps?
			// 3 : { 1: active dnd, 2: # ratings, 3: avg rating, 4: #errors?, 5: total dnd }
			JSONObject jsonAppStats = jsonApp.optJSONObject("3");
			if (DEBUG) {
				pp("jsonAppStats", jsonAppStats);
			}
			if (jsonAppStats == null) {
				if (skipIncomplete) {
					Log.d(TAG, String.format(
							"Skipping app %d because no stats found: package name=%s", i,
							packageName));
				} else {
					Log.d(TAG, "Adding incomplete app: " + packageName);
					apps.add(app);
				}
				continue;
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
				stats.setActiveInstalls(jsonAppStats.getInt("1"));
				stats.setTotalDownloads(jsonAppStats.getInt("5"));
				stats.setNumberOfErrors(jsonAppStats.optInt("4"));
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
	 * Parses the supplied JSON string and returns the number of comments.
	 * 
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
		return new JSONObject(json).getJSONObject("result").getInt("2");
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

			String commentLang = jsonComment.optJSONObject("5").getString("1");
			String commentText = jsonComment.optJSONObject("5").getString("3");
			comment.setLanguage(commentLang);
			comment.setOriginalText(commentText);
			// overwritten if translation is available
			comment.setText(commentText);

			JSONObject translation = jsonComment.optJSONObject("11");
			if (translation != null) {
				String displayLanguage = Locale.getDefault().getLanguage();
				String translationLang = translation.getString("1");
				String translationText = translation.getString("3");
				if (translationLang.contains(displayLanguage)) {
					comment.setText(translationText);
				}
			}

			JSONObject jsonDevice = jsonComment.optJSONObject("8");
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

	static RevenueSummary parseRevenueResponse(String json) throws JSONException {
		JSONObject jsonObj = new JSONObject(json);
		if (jsonObj.has("error")) {
			throw parseError(jsonObj, "fetch revenue summary");
		}

		JSONObject resultObj = jsonObj.getJSONObject("result");
		String currency = resultObj.optString("1");
		// XXX does this really mean that the app has no revenue
		if (currency == null || "".equals(currency)) {
			return null;
		}

		//  "6": "1376352000000"
		long timestamp = resultObj.getLong("6");
		// no time info, 00:00:00 GMT(?)
		Date date = new Date(timestamp / 1000);
		// 2 -total, 3 -sales, 4- in-app products
		// we only use total (for now)
		JSONObject revenueObj = resultObj.getJSONObject("2");
		// even keys are for previous period
		double lastDay = revenueObj.getDouble("1");
		double last7Days = revenueObj.getDouble("3");
		double last30Days = revenueObj.getDouble("5");
		// NaN is treated like NULL -> DB error
		double overall = revenueObj.optDouble("7", 0.0);

		return RevenueSummary.createTotal(currency, date, lastDay, last7Days, last30Days, overall);
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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static Revenue parseLatestTotalRevenue(String json) throws IOException {
		JsonReader reader = new JsonReader(new StringReader(json));
		reader.setLenient(true);
		String currency = null;
		Date reportDate = null;
		Date revenueDate = null;
		String revenueType1 = null;
		String revenueType2 = null;
		double value = 0;

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if ("result".equals(name)) {
				reader.beginObject();
				while (reader.hasNext()) {
					name = reader.nextName();
					// 1: sales, 2: in-app, 3: subscriptions?
					// XXX this doesn't handle the case where there is more
					// than one, e.g. app sales + subscriptions
					if ("1".equals(name) || "2".equals(name) || "3".equals(name)) {
						// revenue list: date->amount
						// [{"1":"1304103600000","2":{"2":234.0}},..{"1":"1304449200000","2":{"2":123.0}},...]
						reader.beginObject();
						while (reader.hasNext()) {
							name = reader.nextName();
							if ("1".equals(name)) {
								reader.beginArray();
								while (reader.hasNext()) {
									reader.beginObject();
									double dailyRevenue = 0;
									Date date = null;
									while (reader.hasNext()) {
										name = reader.nextName();
										if ("1".equals(name)) {
											date = new Date(reader.nextLong());
											if (revenueDate == null) {
												revenueDate = (Date) date.clone();
											}
										} else if ("2".equals(name)) {
											reader.beginObject();
											while (reader.hasNext()) {
												name = reader.nextName();
												if ("2".equals(name)) {
													dailyRevenue = reader.nextDouble();
													if (date != null
															&& date.getTime() > revenueDate
																	.getTime()) {
														revenueDate = date;
														value = dailyRevenue;
													}
												}
											}
											reader.endObject();
										}
									}
									reader.endObject();
								}
								reader.endArray();
							} else if ("2".equals(name)) {
								// "APP", "IN_APP",
								revenueType1 = reader.nextString();
							} else if ("3".equals(name)) {
								revenueType2 = reader.nextString();
							}
						}
						reader.endObject();
					} else if ("4".equals(name)) {
						reportDate = new Date(reader.nextLong());
					} else if ("5".equals(name)) {
						currency = reader.nextString();
					}
				}
				reader.endObject();
			} else if ("xsrf".equals(name)) {
				// consume XSRF
				reader.nextString();
			}
		}
		reader.endObject();

		// XXX what happens when there is more than one type?
		Revenue.Type type = Revenue.Type.TOTAL;
		if ("APP".equals(revenueType1)) {
			type = Revenue.Type.APP_SALES;
		} else if ("IN_APP".equals(revenueType1)) {
			type = Revenue.Type.IN_APP;
		} else {
			type = Revenue.Type.SUBSCRIPTIONS;
		}

		// XXX do we need the date?
		// return new Revenue(type, revenueDate, currency, value);
		return new Revenue(type, value, currency);
	}
}
