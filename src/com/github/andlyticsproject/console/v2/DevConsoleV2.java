package com.github.andlyticsproject.console.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;

import com.github.andlyticsproject.console.AuthenticationException;
import com.github.andlyticsproject.console.DevConsole;
import com.github.andlyticsproject.console.DevConsoleException;
import com.github.andlyticsproject.console.DevConsoleProtocolException;
import com.github.andlyticsproject.console.NetworkException;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.Comment;

/**
 * This is a WIP class representing the new v2 version of the developer console.
 * The aim is to build it from scratch to make it a light weight and as well
 * documented at the end as possible. Once it is done and available to all
 * users, we will rip out the old code and replace it with this.
 * 
 * Once v2 is available to all users, there is scope for better utilising the
 * available statistics data, so keep that in mind when developing this class.
 * For now though, keep it simple and get it working.
 * 
 * See https://github.com/AndlyticsProject/andlytics/wiki/Developer-Console-v2
 * for some more documentation
 * 
 * This class fetches the data, which is then passed using {@link JsonParser}
 * 
 */
@SuppressLint("DefaultLocale")
public class DevConsoleV2 implements DevConsole {

	// 30 seconds -- for both socket and connection
	public static final int TIMEOUT = 30 * 1000;

	private static final String TAG = DevConsoleV2.class.getSimpleName();

	private static final boolean DEBUG = false;

	// Base urls
	private static final String URL_DEVELOPER_CONSOLE = "https://play.google.com/apps/publish/v2";
	private static final String URL_APPS = URL_DEVELOPER_CONSOLE + "/androidapps";
	private static final String URL_STATISTICS = URL_DEVELOPER_CONSOLE + "/statistics";
	private static final String URL_REVIEWS = URL_DEVELOPER_CONSOLE + "/reviews";

	// Templates for payloads used in POST requests
	private static final String FETCH_APPS_TEMPLATE = "{\"method\":\"fetch\","
			+ "\"params\":{\"2\":1,\"3\":7},\"xsrf\":\"%s\"}";
	// 1$: package name, 2$: XSRF
	private static final String GET_RATINGS_TEMPLATE = "{\"method\":\"getRatings\","
			+ "\"params\":{\"1\":[\"%1$s\"]},\"xsrf\":\"%2$s\"}";
	// 1$: package name, 2$: start, 3$: num comments to fetch, 4$ XSRF
	private static final String GET_REVIEWS_TEMPLATE = "{\"method\":\"getReviews\","
			+ "\"params\":{\"1\":\"%1$s\",\"2\":%2$d,\"3\":%3$d},\"xsrf\":\"%4$s\"}";
	// 1$: package name, 2$: stats type, 3$: stats by, 4$: XSRF
	private static final String GET_COMBINED_STATS_TEMPLATE = "{\"method\":\"getCombinedStats\","
			+ "\"params\":{\"1\":\"%1$s\",\"2\":1,\"3\":%2$d,\"4\":[%3$d]},\"xsrf\":\"%4$s\"}";

	// Represents the different ways to break down statistics by e.g. by android
	// version
	protected static final int STATS_BY_ANDROID_VERSION = 1;
	protected static final int STATS_BY_DEVICE = 2;
	protected static final int STATS_BY_COUNTRY = 3;
	protected static final int STATS_BY_LANGUAGE = 4;
	protected static final int STATS_BY_APP_VERSION = 5;
	protected static final int STATS_BY_CARRIER = 6;

	// Represents the different types of statistics e.g. active device installs
	protected static final int STATS_TYPE_ACTIVE_DEVICE_INSTALLS = 1;
	protected static final int STATS_TYPE_TOTAL_USER_INSTALLS = 8;

	private DefaultHttpClient httpClient;
	private SessionCredentials sessionCredentials;
	private DevConsoleAuthenticator authenticator;
	private String accountName;

	public static DevConsoleV2 createForAccount(String accountName, DefaultHttpClient httpClient) {
		DevConsoleAuthenticator authenticator = new AccountManagerAuthenticator(accountName,
				httpClient);

		return new DevConsoleV2(httpClient, authenticator);
	}

	public static DevConsoleV2 createForAccountAndPassword(String accountName, String password,
			DefaultHttpClient httpClient) {
		DevConsoleAuthenticator authenticator = new PasswordAuthenticator(accountName, password,
				httpClient);

		return new DevConsoleV2(httpClient, authenticator);
	}

	private DevConsoleV2(DefaultHttpClient httpClient, DevConsoleAuthenticator authenticator) {
		this.httpClient = httpClient;
		this.authenticator = authenticator;
		this.accountName = authenticator.getAccountName();
	}

	/**
	 * Gets a list of available apps for the given account
	 * 
	 * @param activity
	 * @return
	 * @throws DevConsoleException
	 */
	public synchronized List<AppInfo> getAppInfo(Activity activity) throws DevConsoleException {
		try {
			// the authenticator launched a sub-activity, bail out for now
			if (!authenticateWithCachedCredentialas(activity)) {
				return new ArrayList<AppInfo>();
			}

			return fetchAppInfosAndStatistics();
		} catch (AuthenticationException ex) {
			if (!authenticateFromScratch(activity)) {
				return new ArrayList<AppInfo>();
			}

			return fetchAppInfosAndStatistics();
		}
	}

	private List<AppInfo> fetchAppInfosAndStatistics() {
		// Fetch a list of available apps
		List<AppInfo> apps = fetchAppInfos();

		for (AppInfo app : apps) {
			// Fetch remaining app statistics
			// Latest stats object, and active/total installs is fetched
			// in fetchAppInfos
			AppStats stats = app.getLatestStats();
			fetchRatings(app.getPackageName(), stats);
			stats.setNumberOfComments(fetchCommentsCount(app.getPackageName()));
		}

		return apps;
	}

	/**
	 * Gets a list of comments for the given app based on the startIndex and
	 * count
	 * 
	 * @param accountName
	 * @param packageName
	 * @param startIndex
	 * @param count
	 * @return
	 * @throws DevConsoleException
	 */
	public synchronized List<Comment> getComments(Activity activity, String packageName,
			int startIndex, int count) throws DevConsoleException {
		try {
			if (!authenticateWithCachedCredentialas(activity)) {
				return new ArrayList<Comment>();
			}

			return fetchComments(packageName, startIndex, count);
		} catch (AuthenticationException ex) {
			if (!authenticateFromScratch(activity)) {
				return new ArrayList<Comment>();
			}

			return fetchComments(packageName, startIndex, count);
		}
	}

	/**
	 * Fetches a list of apps for the given account
	 * 
	 * @param accountName
	 * @return
	 * @throws DevConsoleException
	 */
	private List<AppInfo> fetchAppInfos() throws DevConsoleException {
		String json = null;
		try {
			json = post(createDeveloperUrl(URL_APPS), createFetchAppInfosRequest());
			return JsonParser.parseAppInfos(json, accountName);
		} catch (JSONException ex) {
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	private String createFetchAppInfosRequest() {
		// TODO Check the remaining possible parameters to see if they are
		// needed for large numbers of apps
		return String.format(FETCH_APPS_TEMPLATE, sessionCredentials.getXsrfToken());
	}

	private String createDeveloperUrl(String baseUrl) {
		return String.format("%s?dev_acc=%s", baseUrl, sessionCredentials.getDeveloperAccountId());
	}

	/**
	 * Fetches statistics for the given packageName of the given statsType and
	 * adds them to the given {@link AppStats} object
	 * 
	 * This is not used as statistics can be fetched via fetchAppInfos Can use
	 * it later to get historical etc data
	 * 
	 * @param packageName
	 * @param stats
	 * @param statsType
	 * @throws DevConsoleException
	 */
	@SuppressWarnings("unused")
	private void fetchStatistics(String packageName, AppStats stats, int statsType)
			throws DevConsoleException {
		String json = null;
		try {
			json = post(createDeveloperUrl(URL_STATISTICS),
					createFetchStatisticsRequest(packageName, statsType));
			JsonParser.parseStatistics(json, stats, statsType);
		} catch (JSONException ex) {
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	private String createFetchStatisticsRequest(String packageName, int statsType) {
		// Don't care about the breakdown at the moment:
		// STATS_BY_ANDROID_VERSION
		return String.format(GET_COMBINED_STATS_TEMPLATE, packageName, statsType,
				STATS_BY_ANDROID_VERSION, sessionCredentials.getXsrfToken());
	}

	/**
	 * Fetches ratings for the given packageName and adds them to the given
	 * {@link AppStats} object
	 * 
	 * @param packageName
	 *            The app to fetch ratings for
	 * @param stats
	 *            The AppStats object to add them to
	 * @throws DevConsoleException
	 */
	private void fetchRatings(String packageName, AppStats stats) throws DevConsoleException {
		String json = null;
		try {
			json = post(createDeveloperUrl(URL_REVIEWS), createFetchRatingsRequest(packageName));
			JsonParser.parseRatings(json, stats);
		} catch (JSONException ex) {
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	private String createFetchRatingsRequest(String packageName) {
		return String.format(GET_RATINGS_TEMPLATE, packageName, sessionCredentials.getXsrfToken());
	}

	/**
	 * Fetches the number of comments for the given packageName
	 * 
	 * @param packageName
	 * @return
	 * @throws DevConsoleException
	 */
	private int fetchCommentsCount(String packageName) throws DevConsoleException {
		// TODO -- this doesn't always produce correct results
		// emulate the console: fetch first 50, get approx num. comments,
		// fetch last 50 (or so) to get exact number.
		int pageSize = 50;
		String json = null;
		try {
			json = post(createDeveloperUrl(URL_REVIEWS),
					createFetchCommentsRequest(packageName, 0, pageSize));
			int approxNumComments = JsonParser.parseCommentsCount(json);
			if (approxNumComments <= pageSize) {
				// this has a good chance of being exact
				return approxNumComments;
			}

			json = post(createDeveloperUrl(URL_REVIEWS),
					createFetchCommentsRequest(packageName, approxNumComments - pageSize, pageSize));
			int finalNumComments = JsonParser.parseCommentsCount(json);

			return finalNumComments;
		} catch (JSONException ex) {
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	private String createFetchCommentsRequest(String packageName, int start, int pageSize) {
		return String.format(GET_REVIEWS_TEMPLATE, packageName, start, pageSize,
				sessionCredentials.getXsrfToken());
	}

	private List<Comment> fetchComments(String packageName, int startIndex, int count)
			throws DevConsoleException {
		String json = null;
		try {
			json = post(createDeveloperUrl(URL_REVIEWS),
					createFetchCommentsRequest(packageName, startIndex, count));

			return JsonParser.parseComments(json);
		} catch (JSONException ex) {
			throw new DevConsoleProtocolException(json, ex);
		}
	}

	private boolean authenticateWithCachedCredentialas(Activity activity) {
		return authenticate(activity, false);
	}

	private boolean authenticateFromScratch(Activity activity) {
		return authenticate(activity, true);
	}

	/**
	 * Logs into the Android Developer Console
	 * 
	 * @param reuseAuthentication
	 * @throws DevConsoleException
	 */
	private boolean authenticate(Activity activity, boolean invalidateCredentials)
			throws DevConsoleException {
		if (invalidateCredentials) {
			sessionCredentials = null;
		}

		if (sessionCredentials != null) {
			// nothing to do
			return true;
		}

		sessionCredentials = activity == null ? authenticator
				.authenticateSilently(invalidateCredentials) : authenticator.authenticate(activity,
				invalidateCredentials);

		return sessionCredentials != null;
	}

	private String post(String url, String postData) {
		try {
			HttpPost post = new HttpPost(url);
			addHeaders(post);
			post.setEntity(new StringEntity(postData, "UTF-8"));

			if (DEBUG) {
				CookieStore cookieStore = httpClient.getCookieStore();
				List<Cookie> cookies = cookieStore.getCookies();
				for (Cookie c : cookies) {
					Log.d(TAG, String.format("****Cookie**** %s=%s", c.getName(), c.getValue()));
				}
			}

			ResponseHandler<String> handler = HttpClientFactory.createResponseHandler();

			return httpClient.execute(post, handler);
		} catch (HttpResponseException e) {
			if (e.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				throw new AuthenticationException(e);
			}

			throw new NetworkException(e);
		} catch (IOException e) {
			throw new NetworkException(e);
		}

	}

	private void addHeaders(HttpPost post) {
		post.addHeader("Host", "play.google.com");
		post.addHeader("Connection", "keep-alive");
		post.addHeader("Content-Type", "application/json; charset=utf-8");
		post.addHeader("X-GWT-Permutation", "04C42FD45B1FCD2E3034C8A4DC5145C1");
		post.addHeader("X-GWT-Module-Base", "https://play.google.com/apps/publish/v2/gwt/");
		post.addHeader("Referer", "https://play.google.com/apps/publish/v2/?dev_acc="
				+ sessionCredentials.getDeveloperAccountId());
	}

}
