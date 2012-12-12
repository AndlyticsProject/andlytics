package com.github.andlyticsproject.console.v2;

import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;

import android.annotation.SuppressLint;

import com.github.andlyticsproject.console.DevConsoleProtocolException;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.Comment;

@SuppressLint("DefaultLocale")
public class DevConsoleV2Protocol {

	// Base urls
	static final String URL_DEVELOPER_CONSOLE = "https://play.google.com/apps/publish/v2";
	static final String URL_APPS = DevConsoleV2Protocol.URL_DEVELOPER_CONSOLE + "/androidapps";
	static final String URL_STATISTICS = DevConsoleV2Protocol.URL_DEVELOPER_CONSOLE + "/statistics";
	static final String URL_REVIEWS = DevConsoleV2Protocol.URL_DEVELOPER_CONSOLE + "/reviews";

	// Templates for payloads used in POST requests
	static final String FETCH_APPS_TEMPLATE = "{\"method\":\"fetch\","
			+ "\"params\":{\"2\":1,\"3\":7},\"xsrf\":\"%s\"}";
	// 1$: package name, 2$: XSRF
	static final String FETCH_APP_TEMPLATE = "{\"method\":\"fetch\","
			+ "\"params\":{\"1\":[\"%1$s\"],\"3\":0},\"xsrf\":\"%2$s\"}";
	// 1$: package name, 2$: XSRF
	static final String GET_RATINGS_TEMPLATE = "{\"method\":\"getRatings\","
			+ "\"params\":{\"1\":[\"%1$s\"]},\"xsrf\":\"%2$s\"}";
	// 1$: package name, 2$: start, 3$: num comments to fetch, 4$ XSRF
	static final String GET_REVIEWS_TEMPLATE = "{\"method\":\"getReviews\","
			+ "\"params\":{\"1\":\"%1$s\",\"2\":%2$d,\"3\":%3$d},\"xsrf\":\"%4$s\"}";
	// 1$: package name, 2$: stats type, 3$: stats by, 4$: XSRF
	static final String GET_COMBINED_STATS_TEMPLATE = "{\"method\":\"getCombinedStats\","
			+ "\"params\":{\"1\":\"%1$s\",\"2\":1,\"3\":%2$d,\"4\":[%3$d]},\"xsrf\":\"%4$s\"}";

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

	void addHeaders(HttpPost post) {
		checkState();

		post.addHeader("Host", "play.google.com");
		post.addHeader("Connection", "keep-alive");
		post.addHeader("Content-Type", "application/json; charset=utf-8");
		// XXX get this dynamically by fetching and executing the nocache.js file: 
		// https://play.google.com/apps/publish/v2/gwt/com.google.wireless.android.vending.developer.fox.Fox.nocache.js
		post.addHeader("X-GWT-Permutation", "C96CABBAF6CC3B517113CC559C9BCF67");
		post.addHeader("Origin", "https://play.google.com");
		post.addHeader("X-GWT-Module-Base", "https://play.google.com/apps/publish/v2/gwt/");
		post.addHeader("Referer", "https://play.google.com/apps/publish/v2/?dev_acc="
				+ getSessionCredentials().getDeveloperAccountId());
	}

	String createDeveloperUrl(String baseUrl) {
		checkState();

		return String.format("%s?dev_acc=%s", baseUrl, sessionCredentials.getDeveloperAccountId());
	}

	String createFetchAppsUrl() {
		return createDeveloperUrl(URL_APPS);
	}

	String createFetchStatisticsUrl() {
		return createDeveloperUrl(URL_STATISTICS);
	}

	String createFetchCommentsUrl() {
		return createDeveloperUrl(URL_REVIEWS);
	}

	String createFetchAppInfosRequest() {
		checkState();

		// TODO Check the remaining possible parameters to see if they are
		// needed for large numbers of apps
		return String.format(FETCH_APPS_TEMPLATE, sessionCredentials.getXsrfToken());
	}


	List<AppInfo> parseAppInfosResponse(String json, String accountName) {
		try {
			return JsonParser.parseAppInfos(json, accountName);
		} catch (JSONException ex) {
			throw new DevConsoleProtocolException(json, ex);
		}
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
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	String createFetchCommentsRequest(String packageName, int start, int pageSize) {
		checkState();

		return String.format(GET_REVIEWS_TEMPLATE, packageName, start, pageSize,
				sessionCredentials.getXsrfToken());
	}

	int extractCommentsCount(String json) {
		try {
			return JsonParser.parseCommentsCount(json);
		} catch (JSONException ex) {
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	List<Comment> parseCommentsResponse(String json) {
		try {
			return JsonParser.parseComments(json);
		} catch (JSONException ex) {
			throw new DevConsoleProtocolException(json, ex);
		}
	}

}
