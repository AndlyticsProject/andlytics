package com.github.andlyticsproject.v2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpVersion;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;

import com.github.andlyticsproject.exception.AuthenticationException;
import com.github.andlyticsproject.exception.DeveloperConsoleException;
import com.github.andlyticsproject.exception.MultiAccountAcception;
import com.github.andlyticsproject.exception.NetworkException;
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
public class DeveloperConsoleV2 {

	private static final String TAG = DeveloperConsoleV2.class.getSimpleName();

	// Parameters used in string substitution
	private static final String PARAM_PACKAGENAME = "<<packageName>>";
	private static final String PARAM_XSRFTOKEN = "<<xsrfToken>>";
	private static final String PARAM_STATS_TYPE = "<<statsType>>";
	private static final String PARAM_STATS_BY = "<<statsBy>>";
	private static final String PARAM_START = "<<start>>";
	private static final String PARAM_COUNT = "<<count>>";

	// Base urls
	private static final String URL_DEVELOPER_CONSOLE = "https://play.google.com/apps/publish/v2/";
	private static final String URL_APPS = "https://play.google.com/apps/publish/v2/androidapps";
	private static final String URL_STATISTICS = "https://play.google.com/apps/publish/v2/statistics";
	private static final String URL_REVIEWS = "https://play.google.com/apps/publish/v2/reviews";

	// Payloads used in POST requests
	private static final String PAYLOAD_APPS = "{\"method\":\"fetch\",\"params\":[,,1,1],\"xsrf\":"
			+ PARAM_XSRFTOKEN + "}";
	private static final String PAYLOAD_RATINGS = "{\"method\":\"getRatings\",\"params\":[,[\""
			+ PARAM_PACKAGENAME + "\"]],\"xsrf\":" + PARAM_XSRFTOKEN + "}";
	private static final String PAYLOAD_COMMENTS = "{\"method\":\"getReviews\",\"params\":[,\""
			+ PARAM_PACKAGENAME + "," + PARAM_START + "," + PARAM_COUNT + "],\"xsrf\":"
			+ PARAM_XSRFTOKEN + "}";
	private static final String PAYLOAD_STATISTICS = "{\"method\":\"getCombinedStats\",\"params\":[,\""
			+ PARAM_PACKAGENAME
			+ "\",1,"
			+ PARAM_STATS_TYPE
			+ ",["
			+ PARAM_STATS_BY
			+ "]],\"xsrf\":" + PARAM_XSRFTOKEN + "}";

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

	private AuthInfo authInfo;
	private DevConsoleAuthenticator authenticator;

	public DeveloperConsoleV2(DevConsoleAuthenticator authenticator) {
		this.authenticator = authenticator;
	}

	// TODO Decide on which exceptions should actually be thrown and by which
	// methods, and what data we should include in them

	/**
	 * Gets a list of available apps for the given account
	 * 
	 * @param accountName
	 * @return
	 * @throws DeveloperConsoleException
	 * @throws AuthenticationException
	 * @throws MultiAccountAcception
	 * @throws NetworkException
	 * @throws JSONException
	 */
	public List<AppInfo> getAppInfo(String accountName)
			throws DeveloperConsoleException, AuthenticationException, MultiAccountAcception,
			NetworkException, JSONException {

		authenticate(false);
		Date now = new Date();
		// Fetch a list of available apps
		List<AppInfo> apps = fetchAppInfos(accountName);

		for (AppInfo app : apps) {
			// Fetch app statistics
			AppStats stats = new AppStats();
			stats.setRequestDate(now);
			fetchStatistics(app.getPackageName(), stats, STATS_TYPE_TOTAL_USER_INSTALLS);
			// TODO Get active device installs from fetchAppInfos
			// (until we want to collect historical data)
			fetchStatistics(app.getPackageName(), stats, STATS_TYPE_ACTIVE_DEVICE_INSTALLS);
			fetchRatings(app.getPackageName(), stats);
			stats.setNumberOfComments(fetchCommentsCount(app.getPackageName()));

			app.addToHistory(stats);

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
	 * @throws JSONException
	 * @throws NetworkException
	 * @throws MultiAccountAcception
	 * @throws AuthenticationException
	 * @throws DeveloperConsoleException
	 */
	public List<Comment> getComments(String accountName, String packageName, int startIndex,
			int count) throws JSONException, AuthenticationException, MultiAccountAcception,
			NetworkException, DeveloperConsoleException {

		try {
			// First try using existing cookies and tokens
			authenticate(true);
			return fetchComments(packageName, startIndex, count);
		} catch (DeveloperConsoleException ex) {
			authenticate(false);
			return fetchComments(packageName, startIndex, count);
		}
	}

	/**
	 * Fetches a list of apps for the given account
	 * 
	 * @param accountName
	 * @return
	 * @throws DeveloperConsoleException
	 * @throws JSONException
	 */
	private List<AppInfo> fetchAppInfos(String accountName) throws DeveloperConsoleException,
			JSONException {

		// Setup the request
		String postData = PAYLOAD_APPS;
		// TODO Check the remaining possible parameters to see if they are
		// needed for large numbers of apps
		postData = postData.replace(PARAM_XSRFTOKEN, authInfo.getXsrfToken());

		// Perform the request
		String json = null;
		try {
			URL url = new URL(URL_APPS + "?dev_acc=" + authInfo.getDeveloperAccountId());
			json = performHttpPost(postData, url);
		} catch (Exception ex) {
			throw new DeveloperConsoleException(json, ex);
		}

		return JsonParser.parseAppInfos(json, accountName);
	}

	/**
	 * Fetches statistics for the given packageName of the given statsType and
	 * adds them to the given {@link AppStats} object
	 * 
	 * @param packageName
	 * @param stats
	 * @param statsType
	 * @throws DeveloperConsoleException
	 * @throws JSONException
	 */
	private void fetchStatistics(String packageName, AppStats stats, int statsType)
			throws DeveloperConsoleException, JSONException {

		// Setup the request
		String postData = PAYLOAD_STATISTICS;
		postData = postData.replace(PARAM_PACKAGENAME, packageName);
		postData = postData.replace(PARAM_STATS_TYPE, Integer.toString(statsType));
		// Don't care about the breakdown at the moment
		postData = postData.replace(PARAM_STATS_BY, Integer.toString(STATS_BY_ANDROID_VERSION));
		postData = postData.replace(PARAM_XSRFTOKEN, authInfo.getXsrfToken());

		// Perform the request
		String json = null;
		try {
			URL url = new URL(URL_STATISTICS + "?dev_acc=" + authInfo.getDeveloperAccountId());
			json = performHttpPost(postData, url);
		} catch (Exception ex) {
			throw new DeveloperConsoleException(json, ex);
		}

		JsonParser.parseStatistics(json, stats, statsType);
	}

	/**
	 * Fetches ratings for the given packageName and adds them to the given
	 * {@link AppStats} object
	 * 
	 * @param packageName
	 *            The app to fetch ratings for
	 * @param stats
	 *            The AppStats object to add them to
	 * @throws DeveloperConsoleException
	 */
	private void fetchRatings(String packageName, AppStats stats) throws DeveloperConsoleException,
			JSONException {

		// Setup the request
		String postData = PAYLOAD_RATINGS;
		postData = postData.replace(PARAM_PACKAGENAME, packageName);
		postData = postData.replace(PARAM_XSRFTOKEN, authInfo.getXsrfToken());

		// Perform the request
		String json = null;
		try {
			URL url = new URL(URL_REVIEWS + "?dev_acc=" + authInfo.getDeveloperAccountId());
			json = performHttpPost(postData, url);
		} catch (Exception ex) {
			throw new DeveloperConsoleException(json, ex);
		}

		JsonParser.parseRatings(json, stats);
	}

	/**
	 * Fetches the number of comments for the given packageName
	 * 
	 * @param packageName
	 * @return
	 * @throws DeveloperConsoleException
	 * @throws JSONException
	 */
	private int fetchCommentsCount(String packageName) throws DeveloperConsoleException,
			JSONException {

		// Setup the request
		String postData = PAYLOAD_COMMENTS;
		postData = postData.replace(PARAM_PACKAGENAME, packageName);
		postData = postData.replace(PARAM_START, "0");
		postData = postData.replace(PARAM_COUNT, "1"); // TODO Check asking for
														// 0 comments
		postData = postData.replace(PARAM_XSRFTOKEN, authInfo.getXsrfToken());

		// Perform the request
		String json = null;
		try {
			URL url = new URL(URL_REVIEWS + "?dev_acc=" + authInfo.getDeveloperAccountId());
			json = performHttpPost(postData, url);
		} catch (Exception ex) {
			throw new DeveloperConsoleException(json, ex);
		}

		return JsonParser.parseCommentsCount(json);
	}

	private List<Comment> fetchComments(String packageName, int startIndex, int count)
			throws DeveloperConsoleException, JSONException {

		// Setup the request
		String postData = PAYLOAD_COMMENTS;
		postData = postData.replace(PARAM_PACKAGENAME, packageName);
		postData = postData.replace(PARAM_START, Integer.toString(startIndex));
		postData = postData.replace(PARAM_COUNT, Integer.toString(count));
		postData = postData.replace(PARAM_XSRFTOKEN, authInfo.getXsrfToken());

		// Perform the request
		String json = null;
		try {
			URL url = new URL(URL_REVIEWS + "?dev_acc=" + authInfo.getDeveloperAccountId());
			json = performHttpPost(postData, url);
		} catch (Exception ex) {
			throw new DeveloperConsoleException(json, ex);
		}

		return JsonParser.parseComments(json);
	}

	/**
	 * Logs into the Android Developer Console
	 * 
	 * @param reuseAuthentication
	 * @throws AuthenticationException
	 * @throws MultiAccountAcception
	 * @throws NetworkException
	 */
	// TODO revise exceptions
	private void authenticate(boolean reuseAuthentication) throws AuthenticationException,
			MultiAccountAcception, NetworkException {
		if (!reuseAuthentication) {
			authInfo = null;
		}

		if (authInfo != null) {
			// nothing to do
			return;
		}

		authInfo = authenticator.authenticate();
	}

	private DefaultHttpClient createHttpClient() {
		DefaultHttpClient httpclient;
		HttpParams params = new BasicHttpParams();
		HttpClientParams.setRedirecting(params, true);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
		HttpProtocolParams.setUseExpectContinue(params, true);

		SSLSocketFactory sf = SSLSocketFactory.getSocketFactory();
		sf.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schReg.register(new Scheme("https", sf, 443));

		ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);

		int timeoutSocket = 30 * 1000;
		HttpConnectionParams.setSoTimeout(params, timeoutSocket);
		httpclient = new DefaultHttpClient(conMgr, params);
		return httpclient;
	}

	/**
	 * Performs a HTTP POST request using the provided data to the given url
	 * 
	 * FIXME Doesn't work yet
	 * 
	 * @param developerPostData
	 *            The data to send
	 * @param url
	 *            The url to send it to
	 * 
	 * @return A JSON string
	 * 
	 */
	private String performHttpPost(String developerPostData, URL url) throws IOException,
			ProtocolException {

		String result = null;
		// XXX Standardize the whole thing on HttpClient and used a shared
		// instance. Then we don't have to mess around with HttpsURLConnection
		// ridiculous interface and set cookies each time
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setHostnameVerifier(new AllowAllHostnameVerifier());
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		connection.setConnectTimeout(4000);

		// Setup the connection properties
		connection.setRequestProperty("Host", "play.google.com");
		connection.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:15.0) Gecko/20100101 Firefox/15.0");
		connection.setRequestProperty("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
		connection.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
		connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		connection.setRequestProperty("Keep-Alive", "115");
		connection.setRequestProperty("Cookie", "AD=" + authInfo.getAdCookie()); // TODO
																					// Need
																					// to
																					// double
		// check what needs
		// to be in this
		// cookie
		connection.setRequestProperty("Connection", "keep-alive");
		connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
		connection.setRequestProperty("X-GWT-Permutation", "72A1C129AEDE44C1A7A3EE2CA737B409");
		connection.setRequestProperty("X-GWT-Module-Base",
				"https://play.google.com/apps/publish/v2/gwt/");
		connection.setRequestProperty(
				"Referer",
				"https://play.google.com/apps/publish/v2/?dev_acc="
						+ authInfo.getDeveloperAccountId());

		OutputStreamWriter streamToAuthorize = new java.io.OutputStreamWriter(
				connection.getOutputStream());

		streamToAuthorize.write(developerPostData);
		streamToAuthorize.flush();
		streamToAuthorize.close();

		// FIXME Not working due to 404, possibly due to invalid cookie even
		// when copying data from browser
		// Get the response
		InputStream resultStream = connection.getInputStream();
		BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
				resultStream));
		StringBuffer response = new StringBuffer();
		String line = reader.readLine();
		while (line != null) {
			response.append(line + "\n");
			line = reader.readLine();
		}
		resultStream.close();
		result = response.toString();
		return result;
	}
}
