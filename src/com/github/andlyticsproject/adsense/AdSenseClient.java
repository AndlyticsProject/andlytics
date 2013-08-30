package com.github.andlyticsproject.adsense;

import java.text.DateFormat;
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

	private AdSenseClient() {
	}

	public static void syncSiteStats(Context context, String admobAccount, List<String> adUnits,
			Bundle extras, String authority, Bundle syncBundle) throws Exception {
		BackgroundGoogleAccountCredential credential = BackgroundGoogleAccountCredential
				.usingOAuth2(context, Collections.singleton(AdSenseScopes.ADSENSE_READONLY),
						extras, authority, syncBundle);
		credential.setSelectedAccountName(admobAccount);
		AdSense adsense = new AdSense.Builder(AndroidHttp.newCompatibleTransport(), JSON_FACTORY,
				credential).setApplicationName(APPLICATION_NAME).build();

		boolean bulkInsert = false;
		Date startDate = null;
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		Date endDate = calendar.getTime();

		for (String adUnit : adUnits) {
			// read db for required sync period
			ContentAdapter contentAdapter = ContentAdapter.getInstance(AndlyticsApp.getInstance());
			List<AdmobStats> admobStats = contentAdapter.getAdmobStats(adUnit,
					Timeframe.LATEST_VALUE).getStats();

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
		}

		AdClients adClients = adsense.adclients().list().setMaxResults(MAX_LIST_PAGE_SIZE)
				.setPageToken(null).execute();
		if (adClients.getItems() == null || adClients.getItems().isEmpty()) {
			// XXX throw?
			return;
		}

		// we assume there is only one(?)
		String adClientId = adClients.getItems().get(0).getId();

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

			return;
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

			result.add(admob);
		}

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

	public static String escapeFilterParameter(String parameter) {
		return parameter.replace("\\", "\\\\").replace(",", "\\,");
	}
}
