package com.github.andlyticsproject.adsense;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.github.andlyticsproject.AndlyticsApp;
import com.github.andlyticsproject.ContentAdapter;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.model.AdmobStats;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.adsense.AdSense;
import com.google.api.services.adsense.AdSense.Reports.Generate;
import com.google.api.services.adsense.AdSenseScopes;
import com.google.api.services.adsense.model.AdClients;
import com.google.api.services.adsense.model.AdsenseReportsGenerateResponse;

@SuppressLint("SimpleDateFormat")
public class AdSenseClient {

	private static final String TAG = AdSenseClient.class.getSimpleName();

	private static final boolean DEBUG = false;

	private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
	private static final String APPLICATION_NAME = "Andlytics/2.6.0";
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final int MAX_LIST_PAGE_SIZE = 50;

	private static final long MILLIES_IN_DAY = 60 * 60 * 24 * 1000L;

	private AdSenseClient() {
	}

	public static void foregroundSyncStats(Context context, String admobAccount,
			List<String> adUnits) throws Exception {
		AdSense adsense = createForegroundSyncClient(context, admobAccount);
		syncStats(context, adsense, adUnits);
	}

	private static AdSense createForegroundSyncClient(Context context, String admobAccount) {
		GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context,
				Collections.singleton(AdSenseScopes.ADSENSE_READONLY));
		credential.setSelectedAccountName(admobAccount);
		AdSense adsense = new AdSense.Builder(AndroidHttp.newCompatibleTransport(), JSON_FACTORY,
				credential).setApplicationName(APPLICATION_NAME).build();

		return adsense;
	}

	public static void backgroundSyncStats(Context context, String admobAccount,
			List<String> adUnits, Bundle extras, String authority, Bundle syncBundle)
			throws Exception {
		AdSense adsense = createBackgroundSyncClient(context, admobAccount, extras, authority,
				syncBundle);
		syncStats(context, adsense, adUnits);
	}

	private static void syncStats(Context context, AdSense adsense, List<String> adUnits)
			throws Exception {
		Calendar[] syncPeriod = getSyncPeriod(adUnits);
		boolean bulkInsert = false;
		Date startDate = syncPeriod[0].getTime();
		Date endDate = syncPeriod[1].getTime();
		if ((endDate.getTime() - startDate.getTime()) > 7 * MILLIES_IN_DAY) {
			bulkInsert = true;
		}

		// we assume there is only one(?)
		String adClientId = getClientId(adsense);
		if (adClientId == null) {
			// XXX throw?
			return;
		}

		List<AdmobStats> result = generateReport(adsense, adClientId, startDate, endDate);

		updateStats(context, bulkInsert, result);
	}

	private static Calendar[] getSyncPeriod(List<String> adUnits) {
		Calendar startDateCal = null;
		Calendar endDateCal = Calendar.getInstance();
		endDateCal.add(Calendar.DAY_OF_YEAR, 1);

		for (String adUnit : adUnits) {
			// read db for required sync period
			ContentAdapter contentAdapter = ContentAdapter.getInstance(AndlyticsApp.getInstance());
			List<AdmobStats> admobStats = contentAdapter.getAdmobStats(adUnit,
					Timeframe.LATEST_VALUE).getStats();

			if (admobStats.size() > 0) {
				// found previous sync, no bulk import
				Date startDate = admobStats.get(0).getDate();

				startDateCal = Calendar.getInstance();
				startDateCal.setTime(startDate);
				startDateCal.add(Calendar.DAY_OF_YEAR, -4);
			} else {
				startDateCal = Calendar.getInstance();
				startDateCal.setTime(endDateCal.getTime());
				startDateCal.add(Calendar.MONTH, -6);
			}
		}

		return new Calendar[] { startDateCal, endDateCal };
	}

	private static String getClientId(AdSense adsense) throws IOException {
		AdClients adClients = adsense.adclients().list().setMaxResults(MAX_LIST_PAGE_SIZE)
				.setPageToken(null).execute();
		if (adClients.getItems() == null || adClients.getItems().isEmpty()) {
			return null;
		}

		// we assume there is only one(?)
		return adClients.getItems().get(0).getId();
	}

	private static void updateStats(Context context, boolean bulkInsert, List<AdmobStats> result) {
		ContentAdapter contentAdapter = ContentAdapter.getInstance((Application) context
				.getApplicationContext());
		if (bulkInsert) {
			if (result.size() > 6) {
				// insert first results single to avoid manual triggered doubles
				List<AdmobStats> subList1 = result.subList(0, 5);
				for (AdmobStats admob : subList1) {
					contentAdapter.insertOrUpdateAdmobStats(admob);
				}

				List<AdmobStats> subList2 = result.subList(5, result.size());
				contentAdapter.bulkInsertAdmobStats(subList2);
			} else {
				contentAdapter.bulkInsertAdmobStats(result);
			}
		} else {
			for (AdmobStats admob : result) {
				contentAdapter.insertOrUpdateAdmobStats(admob);
			}
		}
	}

	private static AdSense createBackgroundSyncClient(Context context, String admobAccount,
			Bundle extras, String authority, Bundle syncBundle) {
		BackgroundGoogleAccountCredential credential = BackgroundGoogleAccountCredential
				.usingOAuth2(context, Collections.singleton(AdSenseScopes.ADSENSE_READONLY),
						extras, authority, syncBundle);
		credential.setSelectedAccountName(admobAccount);
		AdSense adsense = new AdSense.Builder(AndroidHttp.newCompatibleTransport(), JSON_FACTORY,
				credential).setApplicationName(APPLICATION_NAME).build();
		return adsense;
	}

	private static List<AdmobStats> generateReport(AdSense adsense, String adClientId,
			Date startDate, Date endDate) throws IOException, ParseException {
		String startDateStr = DATE_FORMATTER.format(startDate);
		String endDateStr = DATE_FORMATTER.format(endDate);
		Generate request = adsense.reports().generate(startDateStr, endDateStr);

		// Specify the desired ad client using a filter.
		request.setFilter(Arrays.asList("AD_CLIENT_ID==" + escapeFilterParameter(adClientId)));

		request.setDimension(Arrays.asList("DATE", "AD_UNIT_ID", "AD_UNIT_CODE", "AD_UNIT_NAME"));
		request.setMetric(Arrays.asList("PAGE_VIEWS", "AD_REQUESTS", "AD_REQUESTS_COVERAGE",
				"CLICKS", "AD_REQUESTS_CTR", "COST_PER_CLICK", "AD_REQUESTS_RPM", "EARNINGS"));

		// Sort by ascending date.
		request.setSort(Arrays.asList("+DATE"));
		request.setUseTimezoneReporting(true);

		AdsenseReportsGenerateResponse response = request.execute();
		List<AdmobStats> result = new ArrayList<AdmobStats>();
		if (response.getRows() == null || response.getRows().isEmpty()) {
			Log.d(TAG, "AdSense API returned no rows.");
		}

		if (DEBUG) {
			StringBuilder buff = new StringBuilder();
			for (AdsenseReportsGenerateResponse.Headers header : response.getHeaders()) {
				buff.append(String.format("%25s", header.getName()));
				if (header.getCurrency() != null) {
					buff.append(" " + header.getCurrency());
				}
			}
			Log.d(TAG, "");
		}
		String currencyCode = null;
		AdsenseReportsGenerateResponse.Headers revenueHeader = response.getHeaders().get(11);
		if (revenueHeader != null && revenueHeader.getCurrency() != null) {
			currencyCode = revenueHeader.getCurrency();
		}

		for (List<String> row : response.getRows()) {
			if (DEBUG) {
				StringBuilder buff = new StringBuilder();
				for (String column : row) {
					buff.append(String.format("%25s", column));
				}
				Log.d(TAG, buff.toString());
			}

			AdmobStats admob = new AdmobStats();
			admob.setSiteId(row.get(1));
			admob.setClicks(Integer.parseInt(row.get(7)));
			//				admob.setCpcRevenue(Float.parseFloat(adObject.getString(KEY_CPC_REVENUE)));
			//				admob.setCpmRevenue(Float.parseFloat(adObject.getString(KEY_CPM_REVENUE)));
			admob.setCtr(Float.parseFloat(row.get(8)));
			admob.setDate(DATE_FORMATTER.parse(row.get(0)));
			//				admob.setEcpm(Float.parseFloat(adObject.getString(KEY_ECPM)));
			//				admob.setExchangeDownloads(adObject.getInt(KEY_EXCHANGE_DOWNLOADS));
			admob.setFillRate(Float.parseFloat(row.get(6)));
			//				admob.setHouseAdClicks(adObject.getInt(KEY_HOUSEAD_CLICKS));
			//				admob.setHouseadFillRate(Float.parseFloat(adObject.getString(KEY_HOUSEAD_FILL_RATE)));
			//				admob.setHouseadRequests(adObject.getInt(KEY_HOUSEAD_REQUESTS));
			//				admob.setImpressions(adObject.getInt(KEY_IMPRESSIONS));
			//				admob.setInterstitialRequests(adObject.getInt(KEY_INTERSTITIAL_REQUESTS));
			//				admob.setOverallFillRate(Float.parseFloat(adObject.getString(KEY_OVERALL_FILL_RATE)));
			admob.setRequests(Integer.parseInt(row.get(5)));
			admob.setRevenue(Float.parseFloat(row.get(11)));
			admob.setCurrencyCode(currencyCode);

			result.add(admob);
		}

		return result;
	}

	public static String escapeFilterParameter(String parameter) {
		return parameter.replace("\\", "\\\\").replace(",", "\\,");
	}
}
