package com.github.andlyticsproject.adsense;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.github.andlyticsproject.ContentAdapter;
import com.github.andlyticsproject.DeveloperAccountManager;
import com.github.andlyticsproject.R;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.model.AdmobStats;
import com.github.andlyticsproject.model.AppInfo;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.adsense.AdSense;
import com.google.api.services.adsense.AdSense.Reports.Generate;
import com.google.api.services.adsense.AdSenseScopes;
import com.google.api.services.adsense.model.Accounts;
import com.google.api.services.adsense.model.AdClients;
import com.google.api.services.adsense.model.AdUnit;
import com.google.api.services.adsense.model.AdUnits;
import com.google.api.services.adsense.model.AdsenseReportsGenerateResponse;

public class AdSenseTest extends SherlockFragmentActivity implements OnClickListener {

	private static final String TAG = AdSenseTest.class.getSimpleName();

	private static final String PREF_ACCOUNT_NAME = "accountName";

	private static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
	private static final int REQUEST_AUTHORIZATION = 1;
	private static final int REQUEST_ACCOUNT_PICKER = 2;


	private static final String APPLICATION_NAME = "Andlytics/2.6.0";

	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

	// Request parameters.
	private static final int MAX_LIST_PAGE_SIZE = 50;
	private static final int MAX_REPORT_PAGE_SIZE = 50;

	private HttpTransport httpTransport;

	private GoogleAccountCredential credential;
	private AdSense adsense;

	private Button syncButton;

	private AdSense initializeAdsense() throws Exception {
		credential = GoogleAccountCredential.usingOAuth2(this,
				Collections.singleton(AdSenseScopes.ADSENSE_READONLY));
		AdSense adsense = new AdSense.Builder(httpTransport, JSON_FACTORY, credential)
				.setApplicationName(APPLICATION_NAME).build();

		return adsense;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		// enable logging
		//		Logger.getLogger("com.google.api.client").setLevel(LOGGING_LEVEL);

		setContentView(R.layout.adsense);

		syncButton = (Button) findViewById(R.id.syncButton);
		syncButton.setOnClickListener(this);

		try {
			httpTransport = AndroidHttp.newCompatibleTransport();
			adsense = initializeAdsense();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void testAdsense() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				setProgressBarIndeterminateVisibility(true);
			}

			@Override
			protected Void doInBackground(Void... arg0) {
				try {
					Accounts accounts = adsense.accounts().list().setMaxResults(MAX_LIST_PAGE_SIZE)
							.setPageToken(null).execute();
					if ((accounts.getItems() != null) && !accounts.getItems().isEmpty()) {
						// Get an example account ID, so we can run the following sample.
						String exampleAccountId = accounts.getItems().get(0).getId();
						AdClients adClients = adsense.accounts().adclients().list(exampleAccountId)
								.setMaxResults(MAX_LIST_PAGE_SIZE).setPageToken(null).execute();
					}

					AdClients adClients = adsense.adclients().list()
							.setMaxResults(MAX_LIST_PAGE_SIZE).setPageToken(null).execute();
					if ((adClients.getItems() != null) && !adClients.getItems().isEmpty()) {
						// Get an ad client ID, so we can run the rest of the samples.
						String exampleAdClientId = adClients.getItems().get(0).getId();
						syncSiteStats(exampleAdClientId, AdSenseTest.this, null);

						Context ctx = AdSenseTest.this;
						AndlyticsDb db = AndlyticsDb.getInstance(ctx);
						ContentAdapter adapter = ContentAdapter.getInstance(getApplication());
						List<AppInfo> apps = adapter.getAllAppsLatestStats(DeveloperAccountManager
								.getInstance(ctx).getSelectedDeveloperAccount().getName());
						AdUnits units = adsense.adunits().list(exampleAdClientId)
								.setMaxResults(MAX_LIST_PAGE_SIZE).setPageToken(null).execute();
						List<AdUnit> items = units.getItems();
						for (AdUnit unit : items) {
							for (AppInfo app : apps) {
								if (app.getAdmobAdUnitId() == null
										&& app.getName().equals(unit.getName())) {
									db.saveAdmobAdUnitId(app.getPackageName(), unit.getId());
								}
							}
						}
					} else {
						Log.w(TAG, "No ad clients found, unable to run remaining methods.");
					}
				} catch (UserRecoverableAuthIOException userRecoverableException) {
					startActivityForResult(userRecoverableException.getIntent(),
							REQUEST_AUTHORIZATION);
				} catch (IOException e) {
					System.err.println(e.getMessage());
				} catch (Throwable t) {
					t.printStackTrace();
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				setProgressBarIndeterminateVisibility(false);
			}

		}.execute();
	}

	private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

	public void syncSiteStats(String adClientId, Context context, List<String> siteList)
			throws Exception {
		Date today = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(today);
		calendar.add(Calendar.DATE, -1);
		Date oneWeekAgo = calendar.getTime();

		String startDate = DATE_FORMATTER.format(oneWeekAgo);
		String endDate = DATE_FORMATTER.format(today);
		Generate request = adsense.reports().generate(startDate, endDate);

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
		if (response.getRows() != null && !response.getRows().isEmpty()) {
			StringBuilder buff = new StringBuilder();
			for (AdsenseReportsGenerateResponse.Headers header : response.getHeaders()) {
				buff.append(String.format("%25s", header.getName()));
				if (header.getCurrency() != null) {
					buff.append(" " + header.getCurrency());
				}
			}
			Log.d(TAG, "");

			for (List<String> row : response.getRows()) {
				buff = new StringBuilder();
				for (String column : row) {
					buff.append(String.format("%25s", column));
				}
				Log.d(TAG, buff.toString());
				Log.d(TAG, "");

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

			Log.d(TAG, "");
		} else {
			Log.d(TAG, "No rows returned.");
		}

		Log.d(TAG, "");

		ContentAdapter contentAdapter = ContentAdapter.getInstance(getApplication());
		for (AdmobStats admob : result) {
			contentAdapter.insertOrUpdateAdmobStats(admob);
		}
	}

	public static String escapeFilterParameter(String parameter) {
		return parameter.replace("\\", "\\\\").replace(",", "\\,");
	}

	void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
		runOnUiThread(new Runnable() {
			public void run() {
				Dialog dialog = GooglePlayServicesUtil.getErrorDialog(connectionStatusCode,
						AdSenseTest.this, REQUEST_GOOGLE_PLAY_SERVICES);
				dialog.show();
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_GOOGLE_PLAY_SERVICES:
			if (resultCode == Activity.RESULT_OK) {
				haveGooglePlayServices();
			} else {
				checkGooglePlayServicesAvailable();
			}
			break;
		case REQUEST_AUTHORIZATION:
			if (resultCode == Activity.RESULT_OK) {
				testAdsense();
			} else {
				chooseAccount();
			}
			break;
		case REQUEST_ACCOUNT_PICKER:
			if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
				String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					credential.setSelectedAccountName(accountName);
					SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = settings.edit();
					editor.putString(PREF_ACCOUNT_NAME, accountName);
					editor.commit();

					testAdsense();
				}
			}
			break;
		}
	}

	/** Check that Google Play services APK is installed and up to date. */
	private boolean checkGooglePlayServicesAvailable() {
		int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
			showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
			return false;
		}
		return true;
	}

	private void haveGooglePlayServices() {
		// check if there is already an account selected
		if (credential.getSelectedAccountName() == null) {
			Context ctx = AdSenseTest.this;
			AndlyticsDb db = AndlyticsDb.getInstance(ctx);
			ContentAdapter adapter = ContentAdapter.getInstance(getApplication());
			List<AppInfo> apps = adapter.getAllAppsLatestStats(DeveloperAccountManager
					.getInstance(ctx).getSelectedDeveloperAccount().getName());
			String account = null;
			for (AppInfo app : apps) {
				if (app.getAdmobAccount() != null) {
					account = app.getAdmobAccount();
					break;
				}
			}

			if (account == null) {
				chooseAccount();
			} else {
				credential.setSelectedAccountName(account);
				getSupportActionBar().setSubtitle(account);
				testAdsense();
			}
		} else {
			testAdsense();
		}
	}

	private void chooseAccount() {
		startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.syncButton) {
			if (checkGooglePlayServicesAvailable()) {
				haveGooglePlayServices();
			}
		}
	}


}
