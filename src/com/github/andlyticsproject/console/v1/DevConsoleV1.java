package com.github.andlyticsproject.console.v1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.github.andlyticsproject.AndlyticsApp;
import com.github.andlyticsproject.Preferences;
import com.github.andlyticsproject.console.AuthenticationException;
import com.github.andlyticsproject.console.DevConsoleProtocolException;
import com.github.andlyticsproject.console.MultiAccountException;
import com.github.andlyticsproject.console.NetworkException;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.Comment;

public class DevConsoleV1 {
	// private static String LOG_TAG=DeveloperConsole.class.toString();
	private static final String GWT_PERMUTATION = "6D75CBE66FE85272BB1AD2C64A98B720";

	// "com.github.andlyticsproject", but it seems "Andlytics"
	private static final String PARAM_APPNAME = "<<appname>>";
	private static final String PARAM_XSRFTOKEN = "<<xsrftoken>>";
	private static final String PARAM_PACKAGELIST = "<<packagelist>>";
	private static final String PARAM_STARTINDEX = "<<start>>";
	private static final String PARAM_LENGTH = "<<length>>";

	public static final String ACCOUNT_TYPE = "GOOGLE";
	public static final String SERVICE = "androiddeveloper";

	private static final String URL_GOOGLE_LOGIN = "https://www.google.com/accounts/ClientLogin";
	private static final String URL_DEVELOPER_CONSOLE = "https://play.google.com/apps/publish";
	private static final String URL_DEVELOPER_EDIT_APP = "https://play.google.com/apps/publish/editapp";
	private static final String URL_COMMENTS = "https://play.google.com/apps/publish/comments";
	private static final String URL_FEEDBACK = "https://play.google.com/apps/publish/feedback";

	private static final String GET_ASSET_INDEX_FOR_USER = "7|2|7|https://play.google.com/apps/publish/gwt/|2BCF4D167ACB0F4BCB3EBA75DDAA0575|com.google.gwt.user.client.rpc.XsrfToken/4254043109|"
			+ PARAM_XSRFTOKEN
			+ "|com.google.wireless.android.vending.developer.shared.AppEditorService|getAssetIndexForUser|I|1|2|3|4|5|6|1|7|100|";
	private static final String GET_FULL_ASSET_INFO_FOR_USER_REQUEST = "7|2|9|https://play.google.com/apps/publish/gwt/|2BCF4D167ACB0F4BCB3EBA75DDAA0575|com.google.gwt.user.client.rpc.XsrfToken/4254043109|"
			+ PARAM_XSRFTOKEN
			+ "|com.google.wireless.android.vending.developer.shared.AppEditorService|getProductInfosForUser|java.lang.String/2004016611|I|"
			+ PARAM_APPNAME + "|1|2|3|4|5|6|4|7|8|8|7|9|0|" + PARAM_LENGTH + "|0|";
	private static final String GET_ASSET_FOR_USER_COUNT_REQUEST = "7|0|4|https://play.google.com/apps/publish/gwt/|11B29A336607683DE538737452FFF924|com.google.wireless.android.vending.developer.shared.AppEditorService|getAssetForUserCount|1|2|3|4|0|";
	private static final String GET_USER_COMMENTS_REQUEST = "7|2|11|https://play.google.com/apps/publish/gwt/|3B4252B1EA6FFDBEAC02B41B3975C468|com.google.gwt.user.client.rpc.XsrfToken/4254043109|"
			+ PARAM_XSRFTOKEN
			+ "|com.google.wireless.android.vending.developer.shared.CommentsService|getUserComments|java.lang.String/2004016611|J|java.lang.Iterable|"
			+ PARAM_APPNAME
			+ "|java.util.ArrayList/4159755760|1|2|3|4|5|6|7|7|8|8|9|9|9|7|10|"
			+ PARAM_STARTINDEX + "|" + PARAM_LENGTH + "|11|0|11|0|11|0|0|";
	private static final String GET_FEEDBACK_OVERVIEW = "7|0|6|https://play.google.com/apps/publish/gwt/|8A88A8C8E8E60107C7E013322C6CE8F2|com.google.wireless.android.vending.developer.shared.FeedbackService|getOverviewsForPackages|com.google.protos.userfeedback.gwt.AndroidFrontend$AndroidPackageListRequest$Json/4146859527|[,[<<packagelist>>] ] |1|2|3|4|1|5|5|6|";
	// private static final String GET_FEEDBACK_REQUEST =
	// "7|0|6|https://play.google.com/apps/publish/gwt/|8A88A8C8E8E60107C7E013322C6CE8F2|com.google.wireless.android.vending.developer.shared.FeedbackService|getDetailForPackage|com.google.protos.userfeedback.gwt.AndroidFrontend$AndroidPackageRequest$Json/2133352596|[,<<appname>>] |1|2|3|4|1|5|5|6|";

	private String cookie;
	private String devacc;
	private Context context;
	private String cookieAuthtoken;
	private String postData;

	public DevConsoleV1(Context context) {
		this.context = context;
	}

	public List<AppInfo> getAppDownloadInfos(String authtoken, String accountName)
			throws NetworkException, DevConsoleProtocolException, AuthenticationException,
			MultiAccountException {

		return getFullAssetListRequest(accountName, authtoken, false);
	}

	private List<AppInfo> getFullAssetListRequest(String accountName, String authtoken,
			boolean reuseAuthentication) throws NetworkException, DevConsoleProtocolException,
			AuthenticationException, MultiAccountException {

		developerConsoleAuthentication(authtoken, reuseAuthentication);

		String json = grapGetAssetIndexForUser();
		json = grapAppStatistics("", parseGetAssetForUserCount(json));
		List<AppInfo> result = parseAppStatisticsResponse(json, accountName);

		List<String> packageNames = new ArrayList<String>();

		// remove drafts from result
		Iterator<AppInfo> iterator = result.iterator();
		while (iterator.hasNext()) {
			AppInfo appInfo = (AppInfo) iterator.next();
			if (appInfo.isDraftOnly()) {
				iterator.remove();
			} else {
				packageNames.add(appInfo.getPackageName());
			}
		}

		// get feedback
		/*
		 * if(packageNames.size() > 0) { String feedbackJson =
		 * grapFeedbackOverview(packageNames);
		 * 
		 * Map<String, Integer> errorMap =
		 * parseFeedbackOverviewResponse(feedbackJson);
		 * 
		 * for (AppInfo appInfo: result) {
		 * 
		 * Integer errors = errorMap.get(appInfo.getPackageName()); if(errors !=
		 * null) { appInfo.setNumberOfErrors(errors); } } }
		 */

		return result;
	}

	public List<Comment> getAppComments(String authtoken, String accountName, String packageName,
			int startIndex, int lenght) throws NetworkException, DevConsoleProtocolException,
			AuthenticationException, MultiAccountException {

		try {

			developerConsoleAuthentication(authtoken, true);
			String json = grapComments(packageName, startIndex, lenght);
			// for testing
			// String json = Utils.readFileAsString(new File(
			// Environment.getExternalStorageDirectory(),
			// "comments.json").getAbsolutePath());

			return parseCommentsResponse(json, accountName);

		} catch (DevConsoleProtocolException e) {
			developerConsoleAuthentication(authtoken, false);
			String json = grapComments(packageName, startIndex, lenght);

			return parseCommentsResponse(json, accountName);
		}
	}

	public List<AppInfo> parseAppStatisticsResponse(String json, String accountName)
			throws DevConsoleProtocolException, AuthenticationException {

		List<AppInfo> result = new ArrayList<AppInfo>();

		testIfJsonIsValid(json);

		GwtParser parser = new GwtParser(json);
		result = parser.getAppInfos(accountName);

		return result;
	}

	public Map<String, Integer> parseFeedbackOverviewResponse(String json)
			throws DevConsoleProtocolException, AuthenticationException {

		Map<String, Integer> result = new HashMap<String, Integer>();

		testIfJsonIsValid(json);

		GwtParser parser = new GwtParser(json);
		result = parser.getFeedbackOverview();

		return result;
	}

	private long parseGetAssetForUserCount(String json) throws DevConsoleProtocolException,
			AuthenticationException {

		long result = 0;

		testIfJsonIsValid(json);

		GwtParser parser = new GwtParser(json);
		result = parser.getAppInfoSize();

		return result;
	}

	private void testIfJsonIsValid(String json) throws DevConsoleProtocolException,
			AuthenticationException {
		if (json == null || !json.startsWith("//OK")) {

			if (json != null
					&& (json.indexOf("NewServiceAccount") > -1 || json
							.indexOf("WrongUserException") > -1)) {
				throw new AuthenticationException("Authentication error. Console response: " + json);
			}

			throw new DevConsoleProtocolException("Invalid JSON response", postData, json);
		}
	}

	public List<Comment> parseCommentsResponse(String json, String accountName)
			throws DevConsoleProtocolException, AuthenticationException {
		testIfJsonIsValid(json);

		List<Comment> result = new ArrayList<Comment>();
		try {

			GwtParser parser = new GwtParser(json);
			result = parser.getComments();

		} catch (Exception e) {
			throw new RuntimeException(postData + "response::" + json, e);
		}

		return result;
	}

	protected boolean isInteger(String string) {

		boolean result = true;

		try {
			Integer.parseInt(string);
		} catch (NumberFormatException e) {
			result = false;
		}

		return result;
	}

	private String grapAppStatistics(String startApp, long lenght)
			throws DevConsoleProtocolException, NetworkException {

		String developerPostData = Preferences.getRequestFullAssetInfo(context);

		if (developerPostData == null) {
			developerPostData = GET_FULL_ASSET_INFO_FOR_USER_REQUEST;
		}

		// String lengthString = Base64Utils.toBase64(lenght);
		String lengthString = "" + lenght;
		String xsrfToken = ((AndlyticsApp) context.getApplicationContext()).getXsrfToken();

		developerPostData = developerPostData.replace(PARAM_APPNAME, startApp != null ? startApp
				: "");
		developerPostData = developerPostData.replace(PARAM_LENGTH, lengthString);
		developerPostData = developerPostData.replace(PARAM_XSRFTOKEN,
				xsrfToken != null ? xsrfToken : "");

		String result = null;

		try {
			URL aURL = new java.net.URL(URL_DEVELOPER_EDIT_APP + "?dev_acc=" + devacc);
			result = getGwtRpcResponse(developerPostData, aURL);

		} catch (ConnectException ex) {
			throw new NetworkException(ex);
		} catch (Exception f) {
			throw new DevConsoleProtocolException(result, f);
		}
		return result;
	}

	private String grepGetAssetForUserCount() throws DevConsoleProtocolException, NetworkException {

		String result = null;

		String postData = Preferences.getRequestGetAssetForUserCount(context);

		if (postData == null) {
			postData = GET_ASSET_FOR_USER_COUNT_REQUEST;
		}

		try {
			URL aURL = new java.net.URL(URL_DEVELOPER_EDIT_APP);
			result = getGwtRpcResponse(postData, aURL);

		} catch (ConnectException ex) {
			throw new NetworkException(ex);
		} catch (Exception f) {
			throw new DevConsoleProtocolException(postData, f);
		}

		return result;
	}

	private String grapGetAssetIndexForUser() throws DevConsoleProtocolException, NetworkException {

		String result = null;

		// String developerPostData =
		// Preferences.getGetAssetIndexForUser(context);
		String developerPostData = null;

		if (postData == null) {
			developerPostData = GET_ASSET_INDEX_FOR_USER;
		}
		String xsrfToken = ((AndlyticsApp) context.getApplicationContext()).getXsrfToken();

		developerPostData = developerPostData.replace(PARAM_XSRFTOKEN,
				xsrfToken != null ? xsrfToken : "");

		try {
			URL aURL = new java.net.URL(URL_DEVELOPER_EDIT_APP + "?dev_acc=" + devacc);
			result = getGwtRpcResponse(developerPostData, aURL);

		} catch (ConnectException ex) {
			throw new NetworkException(ex);
		} catch (Exception ex) {
			throw new DevConsoleProtocolException(ex);
		}

		return result;
	}

	private String getGwtPermutation() {
		String gwtPermutation = Preferences.getGwtPermutation(context);

		if (gwtPermutation == null) {
			gwtPermutation = GWT_PERMUTATION;
		}
		return gwtPermutation;
	}

	protected String grapComments(String packageName, int startIndex, int lenght)
			throws DevConsoleProtocolException, NetworkException {

		String postData = Preferences.getRequestUserComments(context);

		if (postData == null) {
			postData = GET_USER_COMMENTS_REQUEST;
		}

		String startIndexString = Base64Utils.toBase64(startIndex);
		String lengthString = Base64Utils.toBase64(lenght);
		String xsrfToken = ((AndlyticsApp) context.getApplicationContext()).getXsrfToken();

		postData = postData.replace(PARAM_APPNAME, packageName);
		postData = postData.replace(PARAM_STARTINDEX, startIndexString);
		postData = postData.replace(PARAM_LENGTH, lengthString);
		postData = postData.replace(PARAM_XSRFTOKEN, xsrfToken != null ? xsrfToken : "");

		String result = null;

		this.postData = postData;

		try {
			URL aURL = new java.net.URL(URL_COMMENTS + "?dev_acc=" + devacc);
			result = getGwtRpcResponse(postData, aURL);

		} catch (ConnectException ex) {
			throw new NetworkException(ex);
		} catch (Exception f) {
			throw new DevConsoleProtocolException(result, f);
		}
		return result;
	}

	protected String grapFeedbackOverview(List<String> packageNames)
			throws DevConsoleProtocolException, NetworkException {

		String postData = Preferences.getRequestFeedback(context);

		if (postData == null) {
			postData = GET_FEEDBACK_OVERVIEW;
		}

		String jsonList = "";
		for (int i = 0; i < packageNames.size(); i++) {
			if (i != 0) {
				jsonList += ",";
			}
			jsonList += "\"" + packageNames.get(i) + "\"";
		}

		postData = postData.replace(PARAM_PACKAGELIST, jsonList);

		System.out.println("feedback post request: " + postData);

		String result = null;

		this.postData = postData;

		try {
			URL aURL = new java.net.URL(URL_FEEDBACK);
			result = getGwtRpcResponse(postData, aURL);

		} catch (ConnectException ex) {
			throw new NetworkException(ex);
		} catch (Exception f) {
			throw new DevConsoleProtocolException(result, f);
		}
		return result;
	}

	private String getGwtRpcResponse(String developerPostData, URL aURL) throws IOException,
			ProtocolException, ConnectException {
		String result;
		HttpsURLConnection connection = (HttpsURLConnection) aURL.openConnection();
		connection.setHostnameVerifier(new AllowAllHostnameVerifier());
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		connection.setConnectTimeout(4000);

		connection.setRequestProperty("Host", "play.google.com");
		connection
				.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; de; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13");
		connection.setRequestProperty("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		connection.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
		connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		connection.setRequestProperty("Keep-Alive", "115");
		connection.setRequestProperty("Cookie", cookie);
		connection.setRequestProperty("Connection", "keep-alive");
		connection.setRequestProperty("Content-Type", "text/x-gwt-rpc; charset=utf-8");
		connection.setRequestProperty("X-GWT-Permutation", getGwtPermutation());
		connection.setRequestProperty("X-GWT-Module-Base",
				"https://play.google.com/apps/publish/gwt-play/");
		connection.setRequestProperty("Referer", "https://play.google.com/apps/publish/Home");

		OutputStreamWriter streamToAuthorize = new java.io.OutputStreamWriter(
				connection.getOutputStream());

		streamToAuthorize.write(developerPostData);
		streamToAuthorize.flush();
		streamToAuthorize.close();

		// Get the Response from Autorize.net
		InputStream resultStream = connection.getInputStream();
		BufferedReader aReader = new java.io.BufferedReader(new java.io.InputStreamReader(
				resultStream));
		StringBuffer aResponse = new StringBuffer();
		String aLine = aReader.readLine();
		while (aLine != null) {
			aResponse.append(aLine + "\n");
			aLine = aReader.readLine();
		}
		resultStream.close();
		result = aResponse.toString();
		return result;
	}

	protected void developerConsoleAuthentication(String authtoken, boolean reuseAuthentication)
			throws NetworkException, AuthenticationException, MultiAccountException {
		// login to Android Market
		// this results in a 302
		// and is necessary for a cookie to be set

		// authtoken =
		// "DQAAAMsAAAA6XRgg47KgvSY9AaQ32d9hOAglYRoW9oJmwd4HxZvMicVeWFciKp5MgyXVDMCxd-xSfgmEeUl-9YFGuVsbAGJI5t09gBioQBb758jCxJbHmzd8utW7tQcK1VtVS4zkRDF_eUzN7KgyDU7AYt8wDsg9Gm8YYAB7vkhIlCGTWNCcvgYnewszM2giu-mOlcaKKjgUW5yiQj3xdZo77CTaZkj5LNeVaCSYF2s_QRKqNkIgXp2jFPQtzFHaGZ76QG4SNq6vKVaD61LF2lswgnPEnSNS";

		this.cookieAuthtoken = authtoken;

		HttpClient httpclient = null;

		try {

			if (!reuseAuthentication) {
				cookie = null;
			}

			// reuse cookie for performance
			if (cookie == null || cookieAuthtoken == null || !cookieAuthtoken.equals(authtoken)) {

				String cookie1 = null;

				// https://play.google.com/apps/publish?auth=DQAAAIMAAAAryYwfh6oNf-o3F25Jkd1vMfCOgGIus7u_iNmvYefviT_yfzgVgLDAORWewa0KDN_BMvLoerHQz0gqn5m3_DBRkm0-H0fcyDGCVs8gYZJ-uDpRqTtUyHUtiMnS8FILxii8PHTArr21FY5pcmEDXFMlG0XVtN_nwzRWZUwEdQ2ffHJMN-rG-HdXEV-dI4FrBW8

				HttpGet httpget = new HttpGet(URL_DEVELOPER_CONSOLE + "?auth=" + authtoken);
				// httpget.setHeader("Authorization", "GoogleLogin auth=" +
				// auth);
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

				// 8p8 fix for xsrfToken
				Matcher m1 = Pattern.compile("userInfo = (\\{.+\\});").matcher(
						EntityUtils.toString(httpResponse.getEntity()));
				if (m1.find()) {
					String xsrfToken = new JSONObject(m1.group(1)).getString("gwtRpcXsrfToken");
					((AndlyticsApp) this.context.getApplicationContext()).setXsrfToken(xsrfToken);
				}
				// TODO this is /apps/publish authorization's header payload
				// (multi-acc)

				final int statusCode = httpResponse.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK) {
					throw new AuthenticationException("Got HTTP " + statusCode + " ("
							+ httpResponse.getStatusLine().getReasonPhrase() + ')');
				}

				boolean asp = false; // TODO get hasher here?

				Object obj = context.getAttribute("http.protocol.redirect-locations");
				if (obj != null) {
					RedirectLocations locs = (RedirectLocations) obj;

					try {
						Field privateStringField = RedirectLocations.class.getDeclaredField("uris");
						privateStringField.setAccessible(true);
						HashSet<URI> uris = (HashSet<URI>) privateStringField.get(locs);

						for (URI uri : uris) {
							String string = uri.toASCIIString();

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

				Header[] allHeaders = httpResponse.getHeaders("Set-Cookie");
				if (allHeaders != null && allHeaders.length > 0) {
					if (allHeaders[0].getValue().startsWith("ANDROID_DEV")) {
						cookie1 = allHeaders[0].getValue();
					}
				}

				if (devacc == null && asp) {
					throw new MultiAccountException("Multiple account consoles not supported.");
				}

				if (devacc == null) {
					Log.e("andlytics", "missing devacc");
					throw new AuthenticationException();
				}

				if (cookie1 == null) {
					throw new AuthenticationException();
				}

				this.cookie = cookie1;
			}
		} catch (SocketException e) {
			throw new NetworkException(e);
		} catch (UnknownHostException e) {
			throw new NetworkException(e);
		} catch (IOException e) {
			throw new NetworkException(e);
		} catch (JSONException e) {
			throw new NetworkException(e);
		} finally {
			if (httpclient != null) {
				httpclient.getConnectionManager().shutdown();
				httpclient = null;
			}
		}

	}

	private static String convertStreamToString(InputStream is) {

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append((line + "\n"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public String login(String email, String password) {
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put("Email", email);
		params.put("Passwd", password);
		params.put("service", SERVICE);
		// params.put("source", source);
		params.put("accountType", ACCOUNT_TYPE);
		String authKey = null;

		// Login at Google.com
		try {
			String data = postUrl(URL_GOOGLE_LOGIN, params);
			StringTokenizer st = new StringTokenizer(data, "\n\r=");
			while (st.hasMoreTokens()) {
				if (st.nextToken().equalsIgnoreCase("Auth")) {
					authKey = st.nextToken();
					break;
				}
			}
			if (authKey == null)
				throw new RuntimeException("authKey not found in " + data);

		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return authKey;
	}

	protected String postUrl(String url, Map<String, String> params) throws IOException {

		String data = "";
		for (String key : params.keySet()) {
			data += "&" + URLEncoder.encode(key, "UTF-8") + "="
					+ URLEncoder.encode(params.get(key), "UTF-8");
		}
		data = data.substring(1);

		// Make the connection to Authoize.net
		URL aURL = new java.net.URL(url);
		HttpsURLConnection aConnection = (HttpsURLConnection) aURL.openConnection();
		aConnection.setDoOutput(true);
		aConnection.setDoInput(true);
		aConnection.setRequestMethod("POST");
		aConnection.setHostnameVerifier(new AllowAllHostnameVerifier());

		// aConnection.setAllowUserInteraction(false);
		// POST the data
		OutputStreamWriter streamToAuthorize = new java.io.OutputStreamWriter(
				aConnection.getOutputStream());
		streamToAuthorize.write(data);
		streamToAuthorize.flush();
		streamToAuthorize.close();

		// Get the Response from Autorize.net
		InputStream resultStream = aConnection.getInputStream();
		BufferedReader aReader = new java.io.BufferedReader(new java.io.InputStreamReader(
				resultStream));
		StringBuffer aResponse = new StringBuffer();
		String aLine = aReader.readLine();
		while (aLine != null) {
			aResponse.append(aLine + "\n");
			aLine = aReader.readLine();
		}
		resultStream.close();

		return aResponse.toString();
	}
}
