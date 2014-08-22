package com.github.andlyticsproject.console.v2;

import android.annotation.SuppressLint;

import com.github.andlyticsproject.console.DevConsoleProtocolException;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.Comment;
import com.github.andlyticsproject.model.Revenue;
import com.github.andlyticsproject.model.RevenueSummary;
import com.github.andlyticsproject.util.FileUtils;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;

@SuppressLint("DefaultLocale")
public class DevConsoleV2Protocol {

	// Base urls
	static final String URL_DEVELOPER_CONSOLE = "https://play.google.com/apps/publish";
	static final String URL_APPS = DevConsoleV2Protocol.URL_DEVELOPER_CONSOLE + "/androidapps";
	static final String URL_STATISTICS = DevConsoleV2Protocol.URL_DEVELOPER_CONSOLE + "/statistics";
	static final String URL_REVIEWS = DevConsoleV2Protocol.URL_DEVELOPER_CONSOLE + "/reviews";
	static final String URL_REVENUE = DevConsoleV2Protocol.URL_DEVELOPER_CONSOLE + "/revenue";

	// Templates for payloads used in POST requests
	static final String FETCH_APPS_TEMPLATE = "{\"method\":\"fetch\","
			+ "\"params\":{\"2\":1,\"3\":7},\"xsrf\":\"%s\"}";
	// 1$: comma separated list of package names
	static final String FETCH_APPS_BY_PACKAGES_TEMPLATE = "{\"method\":\"fetch\","
			+ "\"params\":{\"1\":[%1$s],\"3\":1},\"xsrf\":\"%2$s\"}";
	// 1$: package name, 2$: XSRF
	static final String FETCH_APP_TEMPLATE = "{\"method\":\"fetch\","
			+ "\"params\":{\"1\":[\"%1$s\"],\"3\":0},\"xsrf\":\"%2$s\"}";
	// 1$: package name, 2$: XSRF
	static final String GET_RATINGS_TEMPLATE = "{\"method\":\"getRatings\","
			+ "\"params\":{\"1\":[\"%1$s\"]},\"xsrf\":\"%2$s\"}";
	// 1$: package name, 2$: start, 3$: num comments to fetch, 4$: display locale, 5$ XSRF
	static final String GET_REVIEWS_TEMPLATE = "{\"method\":\"getReviews\","
			+ "\"params\":{\"1\":\"%1$s\",\"2\":%2$d,\"3\":%3$d,\"8\":\"%4$s\"},\"xsrf\":\"%5$s\"}";
	// 1$: package name, 2$: stats type, 3$: stats by, 4$: XSRF
	static final String GET_COMBINED_STATS_TEMPLATE = "{\"method\":\"getCombinedStats\","
			+ "\"params\":{\"1\":\"%1$s\",\"2\":1,\"3\":%2$d,\"4\":[%3$d]},\"xsrf\":\"%4$s\"}";
	// %1$s: package name, %2$s: comment ID, %3$s: reply text, %4$s: XSRF
	static final String REPLY_TO_COMMENT_TEMPLATE = "{\"method\":\"sendReply\","
			+ "\"params\":{\"1\":\"%1$s\",\"2\":\"%2$s\",\"3\":\"%3$s\"},\"xsrf\":\"%4$s\"}";
	// %1$s: package name, %2$s: XSRF
	static final String REVENUE_SUMMARY_TEMPLATE = "{\"method\":\"revenueSummary\",\"params\":{\"1\":\"%1$s\",\"2\":\"\"},\"xsrf\":\"%2$s\"}";
	// %1$s: package name, %2$s: XSRF
	static final String REVENUE_HISTORICAL_DATA = "{\"method\":\"historicalData\",\"params\":{\"1\":\"%1$s\",\"2\":\"\"},\"xsrf\":\"%2$s\"}";

	static final String REPLY_TO_COMMENTS_FEATURE = "REPLY_TO_COMMENTS";

	// Represents the different ways to break down statistics by e.g. by android
	// version
	static final int STATS_BY_ANDROID_VERSION = 1;
	static final int STATS_BY_DEVICE = 2;
	static final int STATS_BY_COUNTRY = 3;
	static final int STATS_BY_LANGUAGE = 4;
	static final int STATS_BY_APP_VERSION = 5;
	static final int STATS_BY_CARRIER = 6;

	// Represents the different types of statistics e.g. active device installs
	static final int STATS_TYPE_ACTIVE_DEVICE_INSTALLS = 1;
	static final int STATS_TYPE_TOTAL_USER_INSTALLS = 8;

	static final int COMMENT_REPLY_MAX_LENGTH = 350;

	private SessionCredentials sessionCredentials;

	DevConsoleV2Protocol() {
	}

	DevConsoleV2Protocol(SessionCredentials sessionCredentials) {
		this.sessionCredentials = sessionCredentials;
	}

	SessionCredentials getSessionCredentials() {
		return sessionCredentials;
	}

	void setSessionCredentials(SessionCredentials sessionCredentials) {
		this.sessionCredentials = sessionCredentials;
	}

	boolean hasSessionCredentials() {
		return sessionCredentials != null;
	}

	void invalidateSessionCredentials() {
		sessionCredentials = null;
	}

	private void checkState() {
		if (sessionCredentials == null) {
			throw new IllegalStateException("Set session credentials first.");
		}
	}

	void addHeaders(HttpPost post, String developerId) {
		checkState();

		post.addHeader("Host", "play.google.com");
		post.addHeader("Connection", "keep-alive");
		post.addHeader("Content-Type", "application/javascript; charset=UTF-8");
		// XXX get this dynamically by fetching and executing the nocache.js file:
		// https://play.google.com/apps/publish/v2/gwt/com.google.wireless.android.vending.developer.fox.Fox.nocache.js
		post.addHeader("X-GWT-Permutation", "7E419416D8BA779A68D417481802D188");
		post.addHeader("Origin", "https://play.google.com");
		post.addHeader("X-GWT-Module-Base", "https://play.google.com/apps/publish/gwt/");
		post.addHeader("Referer", "https://play.google.com/apps/publish/?dev_acc=" + developerId);
	}

	String createDeveloperUrl(String baseUrl, String developerId) {
		checkState();

		return String.format("%s?dev_acc=%s", baseUrl, developerId);
	}

	String createFetchAppsUrl(String developerId) {
		return createDeveloperUrl(URL_APPS, developerId);
	}

	String createFetchStatisticsUrl(String developerId) {
		return createDeveloperUrl(URL_STATISTICS, developerId);
	}

	String createCommentsUrl(String developerId) {
		return createDeveloperUrl(URL_REVIEWS, developerId);
	}

	String createRevenueUrl(String developerId) {
		return createDeveloperUrl(URL_REVENUE, developerId);
	}

	String createFetchAppInfosRequest() {
		checkState();

		// TODO Check the remaining possible parameters to see if they are
		// needed for large numbers of apps
		return String.format(FETCH_APPS_TEMPLATE, sessionCredentials.getXsrfToken());
	}

	String createFetchAppInfosRequest(List<String> packages) {
		checkState();

		StringBuilder buff = new StringBuilder();
		for (int i = 0; i < packages.size(); i++) {
			String packageName = packages.get(i);
			buff.append(packageName);
			if (i != packages.size() - 1) {
				buff.append(",");
			}
		}
		String packageList = buff.toString();

		return String.format(FETCH_APPS_BY_PACKAGES_TEMPLATE, packageList,
				sessionCredentials.getXsrfToken());
	}

	List<AppInfo> parseAppInfosResponse(String json, String accountName, boolean skipIncomplete) {
		try {
			return JsonParser.parseAppInfos(json, accountName, skipIncomplete);
		} catch (JSONException ex) {
			saveDebugJson(json);
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	private static void saveDebugJson(String json) {
		FileUtils.tryWriteToDebugDir(
				String.format("console_reply_%d.json", System.currentTimeMillis()), json);
	}

	String createFetchAppInfoRequest(String packageName) {
		checkState();

		return String.format(FETCH_APP_TEMPLATE, packageName, sessionCredentials.getXsrfToken());
	}

	String createFetchStatisticsRequest(String packageName, int statsType) {
		checkState();

		// Don't care about the breakdown at the moment:
		// STATS_BY_ANDROID_VERSION
		return String.format(GET_COMBINED_STATS_TEMPLATE, packageName, statsType,
				STATS_BY_ANDROID_VERSION, sessionCredentials.getXsrfToken());
	}

	void parseStatisticsResponse(String json, AppStats stats, int statsType) {
		try {
			JsonParser.parseStatistics(json, stats, statsType);
		} catch (JSONException ex) {
			saveDebugJson(json);
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	String createFetchRatingsRequest(String packageName) {
		checkState();

		return String.format(GET_RATINGS_TEMPLATE, packageName, sessionCredentials.getXsrfToken());
	}

	void parseRatingsResponse(String json, AppStats stats) {
		try {
			JsonParser.parseRatings(json, stats);
		} catch (JSONException ex) {
			saveDebugJson(json);
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	String createFetchCommentsRequest(String packageName, int start, int pageSize,
			String displayLocale) {
		checkState();

		return String.format(GET_REVIEWS_TEMPLATE, packageName, start, pageSize, displayLocale,
				sessionCredentials.getXsrfToken());
	}

	String createReplyToCommentRequest(String packageName, String commentId, String reply) {
		checkState();

		if (!canReplyToComments()) {
			throw new IllegalStateException(
					"Reply to comments feature not available for this account");
		}

		// XXX we can probably do better, truncate for now
		if (reply.length() > COMMENT_REPLY_MAX_LENGTH) {
			reply = reply.substring(0, COMMENT_REPLY_MAX_LENGTH);
		}

		return String.format(REPLY_TO_COMMENT_TEMPLATE, packageName, commentId, reply,
				sessionCredentials.getXsrfToken());
	}

	boolean hasFeature(String feature) {
		checkState();

		return sessionCredentials.hasFeature(feature);
	}

	boolean canReplyToComments() {
		// this has apparently been removed because now everybody can 
		// reply to comments
		//		return hasFeature(REPLY_TO_COMMENTS_FEATURE);
		return true;
	}

	int extractCommentsCount(String json) {
		try {
			return JsonParser.parseCommentsCount(json);
		} catch (JSONException ex) {
			saveDebugJson(json);
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	List<Comment> parseCommentsResponse(String json) {
		try {
			return JsonParser.parseComments(json);
		} catch (JSONException ex) {
			saveDebugJson(json);
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	Comment parseCommentReplyResponse(String json) {
		try {
			return JsonParser.parseCommentReplyResponse(json);
		} catch (JSONException ex) {
			saveDebugJson(json);
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	String createFetchRevenueSummaryRequest(String packageName) {
		checkState();

		return String.format(REVENUE_SUMMARY_TEMPLATE, packageName,
				sessionCredentials.getXsrfToken());
	}

	RevenueSummary parseRevenueResponse(String json) {
		try {
			return JsonParser.parseRevenueResponse(json);
		} catch (JSONException ex) {
			saveDebugJson(json);
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	String createFetchHistoricalRevenueRequest(String packageName) {
		checkState();

		return String.format(REVENUE_HISTORICAL_DATA, packageName,
				sessionCredentials.getXsrfToken());
	}

	Revenue parseLatestTotalRevenue(String json) {
		try {
			return JsonParser.parseLatestTotalRevenue(json);
		} catch (IOException ex) {
			saveDebugJson(json);
			throw new DevConsoleProtocolException(json, ex);
		}
	}

}