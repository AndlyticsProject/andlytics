package com.github.andlyticsproject.admob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.github.andlyticsproject.AndlyticsApp;
import com.github.andlyticsproject.ContentAdapter;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.console.NetworkException;
import com.github.andlyticsproject.model.Admob;

public class AdmobRequest {

	private static final String TAG = AdmobRequest.class.getSimpleName();

	public static final String ERROR_TOKEN_INVALID = "token_invalid";
	public static final String ERROR_INVALID_SITE_ID = "site_id_invalid";
	public static final String ERROR_RATE_LIMIT_EXCEEDED = "rate_limit_exceeded";
	public static final String ERROR_NETWORK_ERROR = "andlytics_network_error";
	public static final String ERROR_ACCOUNT_REMOVED = "andlytics_account_removed";
	public static final String ERROR_ASK_USER_PASSWORD = "andlytics_account_ask_for_password";
	public static final String ERROR_REQUEST_INVALID = "request_invalid";

	public static final String KEY_SITE_ID = "site_id";
	public static final String KEY_REQUESTS = "requests";
	public static final String KEY_HOUSEAD_REQUESTS = "housead_requests";
	public static final String KEY_INTERSTITIAL_REQUESTS = "interstitial_requests";
	public static final String KEY_IMPRESSIONS = "impressions";
	public static final String KEY_FILL_RATE = "fill_rate";
	public static final String KEY_HOUSEAD_FILL_RATE = "housead_fill_rate";
	public static final String KEY_OVERALL_FILL_RATE = "overall_fill_rate";
	public static final String KEY_CLICKS = "clicks";
	public static final String KEY_HOUSEAD_CLICKS = "housead_clicks";
	public static final String KEY_CTR = "ctr";
	public static final String KEY_ECPM = "ecpm";
	public static final String KEY_REVENUE = "revenue";
	public static final String KEY_CPC_REVENUE = "cpc_revenue";
	public static final String KEY_CPM_REVENUE = "cpm_revenue";
	public static final String KEY_EXCHANGE_DOWNLOADS = "exchange_downloads";
	public static final String KEY_DATE = "date";

	@SuppressLint("SimpleDateFormat")
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private static final boolean DEBUG = false;

	//base access url to admob api
	private static final String BASE_URL = "https://api.admob.com/v2/";
	private static final String clientKey = "k1541c79203abfb693edb05ede83891b";

	public static final long RETRY_TIME = 4000;

	//construct
	public AdmobRequest() {
	}

	//user login
	public static String login(String email, String password) throws NetworkException,
			AdmobInvalidTokenException, AdmobGenericException, AdmobInvalidRequestException,
			AdmobRateLimitExceededException {

		String token = null;

		//create the parameters
		@SuppressWarnings("deprecation")
		String params = "client_key=" + clientKey + "&email=" + URLEncoder.encode(email)
				+ "&password=" + URLEncoder.encode(password);

		try {
			//try to login
			if (DEBUG)
				Log.d(TAG, email + " admob login request");
			JSONArray data = getResponse("auth", "login", params, true, true);

			//decode the json response and assign token for future use in this session
			token = data.getJSONObject(0).getString("token");
		} catch (JSONException e) {
			throw new AdmobGenericException(e);
		}

		return token;
	}

	private static void handleNonJsonException(Exception e, String sb) throws NetworkException,
			AdmobGenericException, AdmobInvalidTokenException {

		if (e instanceof SocketException || e instanceof UnknownHostException
				|| e instanceof IOException || e instanceof NetworkException) {

			throw new NetworkException(e);
		} else if (e instanceof AdmobInvalidTokenException) {
			throw (AdmobInvalidTokenException) e;
		} else {
			throw new AdmobGenericException(e, sb);
		}
	}

	public static JSONArray getData(Context context, String account, String token, String object,
			String method, String[] requestData) throws AdmobAccountRemovedException,
			NetworkException, AdmobRateLimitExceededException, AdmobInvalidTokenException,
			AdmobGenericException, AdmobAskForPasswordException, AdmobInvalidRequestException {

		String params = createRequestParams(token, requestData);

		//pass everything to getResponse and return it
		JSONArray result = null;
		try {
			result = getResponse(object, method, params, false, true);

		} catch (AdmobInvalidTokenException e) {

			// token invalid, maybe too old, retry with new token
			invalidateAdmobToken(account, token, context);
			String newToken = authenticateAdmobAccount(account, context);
			params = createRequestParams(newToken, requestData);
			result = getResponse(object, method, params, false, true);
		}

		return result;

	}

	private static String createRequestParams(String token, String[] requestData) {
		//init params
		String params = "client_key=" + clientKey;
		params += "&token=" + token;

		//build the params for getResponse to use
		for (String row : requestData) {
			params = params + "&" + row;
		}
		return params;
	}

	//private method to handle data requests
	private static JSONArray getResponse(String object, String method, String urlParameters,
			boolean sendPost, boolean retry) throws NetworkException,
			AdmobRateLimitExceededException, AdmobInvalidTokenException, AdmobGenericException,
			AdmobInvalidRequestException {

		int responseCode = -1;
		JSONObject result = null;
		String sb = null;
		try {

			//create the sub url
			String subUrl = BASE_URL + object + "/" + method;

			HttpsURLConnection con = null;

			//check if it needs to be post or get
			if (sendPost) {

				con = (HttpsURLConnection) new URL(subUrl).openConnection();
				con.setHostnameVerifier(new BrowserCompatHostnameVerifier());
				// create a connection
				con.setDoOutput(true);// allow for posting data to the connection

				// pass data to the connection (POST)
				OutputStream post = con.getOutputStream();
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(post));
				out.write(urlParameters);
				out.close();

			} else {
				if (DEBUG)
					Log.d(TAG, subUrl);
				con = (HttpsURLConnection) new URL(subUrl + "?" + urlParameters).openConnection();
				con.setHostnameVerifier(new BrowserCompatHostnameVerifier());
				//System.out.println("admob request: " + subUrl + "?" + urlParameters);
				//open the link and post everything to it

			}

			//read the response

			InputStream resultStream = con.getInputStream();
			BufferedReader aReader = new java.io.BufferedReader(new java.io.InputStreamReader(
					resultStream));
			StringBuffer aResponse = new StringBuffer();
			String aLine = aReader.readLine();
			while (aLine != null) {
				aResponse.append(aLine + "\n");
				aLine = aReader.readLine();
			}
			resultStream.close();
			sb = aResponse.toString();

			if (con instanceof HttpURLConnection) {
				HttpURLConnection httpConnection = (HttpURLConnection) con;

				responseCode = httpConnection.getResponseCode();
			}

			if (sb == null || "".equals(sb.trim())) {
				Log.w(TAG, "admob empty respsonse" + " token invalid?");
				throw new AdmobInvalidTokenException(responseCode + " null response");
			} else if (!sb.startsWith("{")) {
				throw new AdmobInvalidTokenException(responseCode + " invalid response: " + sb);
			}

			result = new JSONObject(sb);

			//  } catch (IOException e) {
			//    throw new NetworkException(e);
		} catch (JSONException e) {
			handleNonJsonException(e, responseCode + " " + sb);
		} catch (Exception e) {
			handleNonJsonException(e, responseCode + " " + sb);
		}

		// check json for errors
		try {
			checkJsonErrors(result);
		} catch (AdmobRateLimitExceededException e) {
			if (retry) {
				Log.w(TAG, "admob rateLimitExceeded, retry in 5 sec...");
				try {
					Thread.sleep(AdmobRequest.RETRY_TIME);
				} catch (InterruptedException e1) {
				}
				return getResponse(object, method, urlParameters, sendPost, false);
			} else {
				throw e;
			}
		}

		JSONArray dataObject = toJsonArray(result, "data");

		return dataObject;

	}

	private static void checkJsonErrors(JSONObject result) throws AdmobInvalidTokenException,
			AdmobRateLimitExceededException, AdmobGenericException, AdmobInvalidRequestException {

		Map<String, String> errorMap = new HashMap<String, String>();

		JSONArray errors = (result).optJSONArray("errors");

		if (errors != null && errors.length() > 0) {

			for (int i = 0; i < errors.length(); i++) {

				try {
					String code = errors.getJSONObject(i).optString("code");
					String message = errors.getJSONObject(i).optString("msg");
					errorMap.put(code, message);

				} catch (JSONException e) {
					throw new AdmobGenericException(e);
				}
			}
		}

		if (errorMap.size() > 0) {

			if (errorMap.containsKey(ERROR_RATE_LIMIT_EXCEEDED)) {

				String exMessage = ERROR_RATE_LIMIT_EXCEEDED + " "
						+ errorMap.get(ERROR_RATE_LIMIT_EXCEEDED);
				throw new AdmobRateLimitExceededException(exMessage);

			} else if (errorMap.containsKey(ERROR_REQUEST_INVALID)) {

				String exMessage = ERROR_REQUEST_INVALID + " "
						+ errorMap.get(ERROR_REQUEST_INVALID);
				throw new AdmobInvalidRequestException(exMessage);

			} else if (errorMap.containsKey(ERROR_TOKEN_INVALID)) {

				String exMessage = ERROR_TOKEN_INVALID + " " + errorMap.get(ERROR_TOKEN_INVALID);
				throw new AdmobInvalidTokenException(exMessage);
			} else {
				String code = errorMap.keySet().iterator().next();
				throw new AdmobGenericException(code + " " + errorMap.get(code));
			}
		}

	}

	private static JSONArray toJsonArray(JSONObject result, String elementName) {
		JSONArray dataObject = (result).optJSONArray(elementName);

		if (dataObject == null) {
			dataObject = new JSONArray();
			dataObject.put(result.optJSONObject(elementName));
		}
		return dataObject;
	}

	public static void syncSiteStats(String account, Context context, List<String> siteList,
			SyncCallback callback) throws AdmobRateLimitExceededException,
			AdmobAccountRemovedException, NetworkException, AdmobInvalidTokenException,
			AdmobGenericException, AdmobAskForPasswordException, AdmobInvalidRequestException {

		if (DEBUG)
			Log.d(TAG, "admob site sync request for " + siteList.size() + " sites");

		String token = authenticateAdmobAccount(account, context);

		for (int k = 0; k < siteList.size(); k++) {

			String admobSiteId = siteList.get(k);

			boolean bulkInsert = false;

			Date startDate = null;

			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DAY_OF_YEAR, 1);
			Date endDate = calendar.getTime();

			// read db for required sync period
			ContentAdapter contentAdapter = ContentAdapter.getInstance(AndlyticsApp.getInstance());
			List<Admob> admobStats = contentAdapter.getAdmobStats(admobSiteId,
					Timeframe.LATEST_VALUE).getAdmobs();

			if (admobStats.size() > 0) {
				// found previouse sync, no bulk import
				startDate = admobStats.get(0).getDate();

				Calendar startCal = Calendar.getInstance();
				startCal.setTime(startDate);
				startCal.add(Calendar.DAY_OF_YEAR, -4);
				startDate = startCal.getTime();

			} else {
				calendar.add(Calendar.MONTH, -6);
				startDate = calendar.getTime();
				bulkInsert = true;

			}

			List<Admob> result = new ArrayList<Admob>();

			//String endDate = dateFormat.format(calendar.getTime());
			JSONArray data = getData(context, account, token, "site", "stats", new String[] {
					"site_id=" + admobSiteId, "end_date=" + dateFormat.format(endDate),
					"start_date=" + dateFormat.format(startDate), "time_dimension=day",
					"order_by[date]=desc" });

			if (callback != null && bulkInsert) {
				callback.initialImportStarted();
			}

			//go through the returned array
			for (int i = 0; i < data.length(); i++) {

				try {

					//convert the line of data to a json object so we can pick specific data out of it
					JSONObject adObject = new JSONObject(data.get(i).toString());

					Admob admob = new Admob();
					admob.setSiteId(admobSiteId);
					admob.setClicks(adObject.getInt(KEY_CLICKS));
					admob.setCpcRevenue(Float.parseFloat(adObject.getString(KEY_CPC_REVENUE)));
					admob.setCpmRevenue(Float.parseFloat(adObject.getString(KEY_CPM_REVENUE)));
					admob.setCtr(Float.parseFloat(adObject.getString(KEY_CTR)));
					admob.setDate(dateFormat.parse(adObject.getString(KEY_DATE)));
					admob.setEcpm(Float.parseFloat(adObject.getString(KEY_ECPM)));
					admob.setExchangeDownloads(adObject.getInt(KEY_EXCHANGE_DOWNLOADS));
					admob.setFillRate(Float.parseFloat(adObject.getString(KEY_FILL_RATE)));
					admob.setHouseAdClicks(adObject.getInt(KEY_HOUSEAD_CLICKS));
					admob.setHouseadFillRate(Float.parseFloat(adObject
							.getString(KEY_HOUSEAD_FILL_RATE)));
					admob.setHouseadRequests(adObject.getInt(KEY_HOUSEAD_REQUESTS));
					admob.setImpressions(adObject.getInt(KEY_IMPRESSIONS));
					admob.setInterstitialRequests(adObject.getInt(KEY_INTERSTITIAL_REQUESTS));
					admob.setOverallFillRate(Float.parseFloat(adObject
							.getString(KEY_OVERALL_FILL_RATE)));
					admob.setRequests(adObject.getInt(KEY_REQUESTS));
					admob.setRevenue(Float.parseFloat(adObject.getString(KEY_REVENUE)));

					result.add(admob);

				} catch (Exception e) {
					throw new AdmobGenericException(e);
				}
			}

			if (bulkInsert) {
				if (result.size() > 6) {

					// insert first results single to avoid manual triggered doubles
					List<Admob> subList1 = result.subList(0, 5);
					for (Admob admob : subList1) {
						contentAdapter.insertOrUpdateAdmobStats(admob);
					}

					List<Admob> subList2 = result.subList(5, result.size());
					contentAdapter.bulkInsertAdmobStats(subList2);

				} else {
					contentAdapter.bulkInsertAdmobStats(result);
				}
			} else {
				for (Admob admob : result) {
					contentAdapter.insertOrUpdateAdmobStats(admob);
				}
			}

			if (k != siteList.size() - 1) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//Log.d(TAG, "wait to avoid rate limit exception");
			}

		}

	}

	private static String authenticateAdmobAccount(String currentAdmobAccount, Context context)
			throws AdmobRateLimitExceededException, AdmobInvalidTokenException,
			AdmobAccountRemovedException, AdmobAskForPasswordException,
			AdmobInvalidRequestException {

		String admobToken = AdmobAuthenticationUtilities.authenticateAccount(currentAdmobAccount,
				context);

		if (AdmobRequest.ERROR_RATE_LIMIT_EXCEEDED.equals(admobToken)) {
			throw new AdmobRateLimitExceededException(admobToken);
		} else if (AdmobRequest.ERROR_REQUEST_INVALID.equals(admobToken)) {
			throw new AdmobInvalidRequestException(admobToken);
		} else if (AdmobRequest.ERROR_ACCOUNT_REMOVED.equals(admobToken)) {
			throw new AdmobAccountRemovedException(admobToken, currentAdmobAccount);
		} else if (AdmobRequest.ERROR_ASK_USER_PASSWORD.equals(admobToken)) {
			throw new AdmobAskForPasswordException("Missing password");
		}

		return admobToken;
	}

	private static void invalidateAdmobToken(String accountName, String token, Context context) {
		if (DEBUG)
			Log.d(TAG, "invalidate admob token for " + accountName);
		AdmobAuthenticationUtilities.invalidateToken(token, context);
	}

	public interface SyncCallback {

		void initialImportStarted();

	}

	public static Map<String, String> getSiteList(String account, Context context)
			throws AdmobRateLimitExceededException, AdmobInvalidTokenException,
			AdmobAccountRemovedException, NetworkException, AdmobGenericException,
			AdmobAskForPasswordException, AdmobInvalidRequestException {

		Map<String, String> result = new HashMap<String, String>();

		String token = authenticateAdmobAccount(account, context);

		JSONArray data = AdmobRequest.getData(context, account, token, "site", "search",
				new String[] {});

		//go through the returned array
		for (int i = 0; i < data.length(); i++) {

			//convert the line of data to a json object so we can pick specific data out of it
			JSONObject adObject;
			try {
				adObject = new JSONObject(data.get(i).toString());

				String id = adObject.getString("id");
				String name = adObject.getString("name");
				result.put(id, name);
			} catch (JSONException e) {
				throw new AdmobGenericException(e);
			}
		}

		return result;

	}

}
