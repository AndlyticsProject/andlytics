
package com.github.andlyticsproject.v2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.StringTokenizer;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.util.Log;

import com.github.andlyticsproject.exception.AuthenticationException;
import com.github.andlyticsproject.exception.DeveloperConsoleException;
import com.github.andlyticsproject.exception.MultiAccountAcception;
import com.github.andlyticsproject.exception.NetworkException;

/**
 * This is a WIP class representing the new v2 version of the developer console.
 * The aim is to build it from scratch to make it a light weight and as well documented at the end as possible.
 * Once it is done and available to all users, we will rip out the old code and replace it with this.
 * 
 * Once v2 is available to all users, there is scope for better utilising the available statistics data,
 * so keep that in mind when developing this class. For now though, keep it simple and get it working.
 * 
 * See https://github.com/AndlyticsProject/andlytics/wiki/Developer-Console-v2 for some more documentation
 *
 */
public class DeveloperConsoleV2 {

	private static final String TAG = "Andlytics";

	// Parameters used in string substitution
	private static final String PARAM_PACKAGENAME = "<<packagename>>";
	private static final String PARAM_XSRFTOKEN = "<<xsrftoken>>";
	private static final String PARAM_STATS_TYPE = "<<statstype>>";
	private static final String PARAM_STATS_BY = "<<statsby>>";
	private static final String PARAM_START = "<<start>>";
	private static final String PARAM_COUNT = "<<count>>";

	// Account types
	public static final String ACCOUNT_TYPE = "GOOGLE";
	public static final String SERVICE = "androiddeveloper";

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
			+ PARAM_PACKAGENAME	+ "\",1," + PARAM_STATS_TYPE + ",["	+ PARAM_STATS_BY
			+ "]],\"xsrf\":" + PARAM_XSRFTOKEN + "}";

	//Represents the different ways to break down statistics by e.g. by android version
	public static final int STATS_BY_ANDROID_VERSION = 1;
	public static final int STATS_BY_DEVICE = 2;
	public static final int STATS_BY_COUNTRY = 3;
	public static final int STATS_BY_LANGUAGE = 4;
	public static final int STATS_BY_APP_VERSION = 5;
	public static final int STATS_BY_CARRIER = 6;

	// Represents the different types of statistics e.g. active device installs
	public static final int STATS_TYPE_ACTIVE_DEVICE_INSTALLS = 1;
	public static final int STATS_TTPE_TOTAL_USER_INSTALLS = 8;

	private String cookie;
	private String devacc;
	private Context context;
	private String xsrfToken;

	public DeveloperConsoleV2(Context context) {
		this.context = context;
	}

	/**
	 * Fake login using data collected from v1 login (not even sure if the cookie will allow this?)
	 * @param cookie
	 * @param devacc
	 */
	public void login(String cookie, String devacc, String xsrfToken) {
		this.cookie = cookie;
		this.devacc = devacc;
		this.xsrfToken = xsrfToken;
	}

	/**
	 * Logs into the Android Developer Console using the authtoken
	 * 
	 * FIXME Not working (Cannot find ANDROID_DEV cookie needed for the initial request. Also have a new AD cookie
	 * which we need to do something with)
	 * 
	 * @throws AuthenticationException 
	 * @throws MultiAccountAcception 
	 * @throws NetworkException 
	 */
	public void login(String authtoken, boolean reuseAuthentication)
			throws AuthenticationException, MultiAccountAcception, NetworkException {
		// Login to Google play which this results in a 302 and is necessary for a cookie to be set

		// This is now broken down into (i think) 3 steps, although there seems to be a missing first step to get ANDROID_DEV

		/*
		 * Need ANDROID_DEV cookie for this one, but don't know how to get it cookies?
		 * GET https://play.google.com/apps/publish/v2/?auth=AUTH_TOKEN
		 * Returns 302 and has AD value in cookie
		 * 
		 * Need AD and ANDROID_DEV cookie for this one
		 * GET https://play.google.com/apps/publish/v2/
		 * Returns 302 and gives dev_acc in location
		 * 
		 * GET https://play.google.com/apps/publish/v2/?dev_acc=DEV_ACC
		 * Entity contains XSRF Token
		 * 
		 */

		HttpClient httpclient = null;
		boolean asp = false;

		try {

			if (!reuseAuthentication) {
				cookie = null;
			}

			// reuse cookie for performance
			if (cookie == null || authtoken == null) {

				String cookieAndroidDev = null;
				String cookieAD = null;

				// TODO Need ANDROID_DEV cookie now, how do we get it?
				//
				//	allHeaders = httpResponse.getHeaders("Set-Cookie");
				//	if (allHeaders != null && allHeaders.length > 0) {
				//		if (allHeaders[0].getValue().startsWith("ANDROID_DEV")) {
				//			cookieAndroidDev = allHeaders[0].getValue();
				//		}
				//	}

				// GET https://play.google.com/apps/publish/v2/?auth=AUTH_TOKEN
				HttpGet httpget = new HttpGet(URL_DEVELOPER_CONSOLE + "?auth=" + authtoken);
				HttpParams params = new BasicHttpParams();
				HttpClientParams.setRedirecting(params, true);
				HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
				HttpProtocolParams.setUseExpectContinue(params, true);

				SSLSocketFactory sf = SSLSocketFactory.getSocketFactory();
				sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

				SchemeRegistry schReg = new SchemeRegistry();
				schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				schReg.register(new Scheme("https", sf, 443));

				ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);

				int timeoutSocket = 30 * 1000;
				HttpConnectionParams.setSoTimeout(params, timeoutSocket);

				HttpContext context = new BasicHttpContext();
				httpclient = new DefaultHttpClient(conMgr, params);

				HttpResponse httpResponse = httpclient.execute(httpget, context);

				final int statusCode = httpResponse.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK) {
					throw new AuthenticationException("Got HTTP " + statusCode + " ("
							+ httpResponse.getStatusLine().getReasonPhrase() + ')');
				}

				// Get AD from cookie
				Header[] allHeaders = httpResponse.getHeaders("Set-Cookie");
				if (allHeaders != null && allHeaders.length > 0) {
					if (allHeaders[0].getValue().startsWith("AD")) {
						cookieAD = allHeaders[0].getValue();
					}
				}

				if (cookieAndroidDev == null) {
					throw new AuthenticationException();
				}

				this.cookie = cookieAndroidDev + cookieAD;

				// TODO Do second request to get dev_acc
				// GET https://play.google.com/apps/publish/v2/

				Object obj = context.getAttribute("http.protocol.redirect-locations");
				if (obj != null) {
					RedirectLocations locs = (RedirectLocations) obj;

					try {
						Field privateStringField = RedirectLocations.class.getDeclaredField("uris");
						privateStringField.setAccessible(true);
						HashSet<URI> uris = (HashSet<URI>) privateStringField.get(locs);

						for (URI uri : uris) {
							String string = uri.toASCIIString();

							// TODO get dev_acc
							if (string.indexOf("dev_acc=") > -1) {
								devacc = string.substring(string.indexOf("=") + 1, string.length());
								break;
							} else if (string.indexOf("asp=1") > -1) {
								asp = true;
							}

						}

					} catch (NoSuchFieldException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}

				}

				if (devacc == null && asp) {
					throw new MultiAccountAcception();
				}

				if (devacc == null) {
					Log.e(TAG, "missing devacc");
					throw new AuthenticationException();
				}

				// TODO Do third request and read the entity for XSRF Token

			}
		} catch (SocketException e) {
			throw new NetworkException(e);
		} catch (UnknownHostException e) {
			throw new NetworkException(e);
		} catch (IOException e) {
			throw new NetworkException(e);
		} finally {
			if (httpclient != null) {
				httpclient.getConnectionManager().shutdown();
				httpclient = null;
			}
		}

	}

	public void fetchRatings(String packageName) throws DeveloperConsoleException {
		// Setup the request
		String postData = PAYLOAD_RATINGS;
		postData = postData.replace(PARAM_PACKAGENAME, packageName);
		postData = postData.replace(PARAM_XSRFTOKEN, xsrfToken);

		// Perform the request
		String jsonResult = null;
		try {
			URL url = new URL(URL_REVIEWS + "?dev_acc=" + devacc);
			jsonResult = performHttpPost(postData, url);
		} catch (Exception ex) {
			ex.printStackTrace();
			//throw new DeveloperConsoleException(jsonResult, ex);
			// Dummy data
			jsonResult = "{\"result\":[null,[[null,\"" + packageName + "\",\"2\",\"0\",\"3\",\"27\",\"206\"]]]," +
					"\"xsrf\":\"AMtNNDEXXXXXXXXXXXXXX:1344165266000\"}";
		}
		
		// TODO Can this be automatically done using Gson?

		// Find the start and end
		int ratingsStartIndex = jsonResult.indexOf(packageName) + packageName.length() + 3;
		int ratingsEndIndex = jsonResult.indexOf("]]],\"xsrf");
		String ratingsString = jsonResult.substring(ratingsStartIndex, ratingsEndIndex);
		// Strip out extra quotes and split based on ,
		ratingsString = ratingsString.replace("\"", "");
		StringTokenizer st = new StringTokenizer(ratingsString, ",");
		int totalRatings = 0;
		int[] ratings = new int[6]; // Index 5 = Total, 0 = # 1 star, 1 = # 2 star ...
		int index = 0;
		while (st.hasMoreElements()) {
			int rating = Integer.parseInt(st.nextToken());
			totalRatings += rating;
			ratings[index] = rating;
			index++;
		}
		ratings[5] = totalRatings;

		// TODO return something e.g. AppData or an int[] with the data

	}

	/**
	 * Performs a HTTP POST request using the provided data to the given url
	 * 
	 * FIXME Doesn't work yet, probably because of authentication due to login not working
	 * 
	 * @param developerPostData The data to send
	 * @param url The url to send it to
	 * 
	 * @return A JSON string
	 * 
	 */
	private String performHttpPost(String developerPostData, URL url) throws IOException,
	ProtocolException {

		String result = null;
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
		connection.setRequestProperty("Cookie", cookie); // TODO Need to double check what needs to be in this cookie
		connection.setRequestProperty("Connection", "keep-alive");
		connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
		connection.setRequestProperty("X-GWT-Permutation", "72A1C129AEDE44C1A7A3EE2CA737B409");
		connection.setRequestProperty("X-GWT-Module-Base",
				"https://play.google.com/apps/publish/v2/gwt/");
		connection.setRequestProperty("Referer",
				"https://play.google.com/apps/publish/v2/?dev_acc=" + devacc);

		OutputStreamWriter streamToAuthorize = new java.io.OutputStreamWriter(
				connection.getOutputStream());

		streamToAuthorize.write(developerPostData);
		streamToAuthorize.flush();
		streamToAuthorize.close();

		// FIXME Not working due to 404, possibly due to invalid cookie (V2 login doesn't work, and reusing V1 data doesn't appear to)
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
