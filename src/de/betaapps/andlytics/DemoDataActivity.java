package de.betaapps.andlytics;

import de.betaapps.andlytics.model.AppInfo;
import de.betaapps.andlytics.model.AppStats;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DemoDataActivity extends BaseActivity {

	private ContentAdapter db;
	private String accountname;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		db = getDbAdapter();

		new LoadDemoData().execute();

		Bundle b = getIntent().getExtras();
		accountname = b.getString(Constants.AUTH_ACCOUNT_NAME);
	}

	public static String readTestData(char c) {

		StringBuffer result = new StringBuffer();
		try {
			File f = new File(Environment.getExternalStorageDirectory() + "/json" + c + ".txt");
			FileInputStream fileIS = new FileInputStream(f);
			BufferedReader buf = new BufferedReader(new InputStreamReader(fileIS));
			String readString = new String();
			// just reading each line and pass it on the debugger
			while ((readString = buf.readLine()) != null) {
				result.append(readString);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result.toString();
	}

	private class LoadDemoData extends AsyncTask<Void, Integer, Void> {

		private static final int VALUE_LENGTH_1 = 90;

		private ProgressDialog progressDialog;

		@Override
		protected Void doInBackground(Void... params) {

			//db.deleteAllForPackageName("de.betaapps.demo1");
			int count = 0;
			for (int i = 0; i < VALUE_LENGTH_1; i++) {

				AppInfo info = new AppInfo();
				AppStats downloadInfo = new AppStats();

				info.setAccount(accountname);
				count += new Random().nextInt(200);
				downloadInfo.setActiveInstalls((int) (count * 0.6));
				Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.YEAR, 2011);
				calendar.set(Calendar.DAY_OF_YEAR, i);
				downloadInfo.setRequestDate(calendar.getTime());
				info.setLastUpdate(calendar.getTime());
				info.setName("Demo App 1");
				info.setPackageName("de.betaapps.demo1");

				Map<Integer, Integer> ratings = new HashMap<Integer, Integer>();
				ratings.put(1, (int) ((i + 1) * 10 * ((float) new Random().nextInt(100)) / 100.0));
				ratings.put(2, (int) ((i + 1) * 10 * ((float) new Random().nextInt(100)) / 100.0));
				ratings.put(3, (int) ((i + 1) * 10 * ((float) new Random().nextInt(100)) / 100.0));
				ratings.put(4, (int) ((i + 1) * 10 * ((float) new Random().nextInt(100)) / 100.0));
				ratings.put(5, (int) ((i + 1) * 10 * ((float) new Random().nextInt(100)) / 100.0));

				downloadInfo.setTotalDownloads(count);

				downloadInfo.setRating(ratings);
				
				info.setLatestStats(downloadInfo);
				
				if (i % 7 != 0) {
					db.insertOrUpdateStats(info);
				}

				publishProgress(i);
			}

			
			return null;
		}

		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(DemoDataActivity.this);
			progressDialog.setMax(VALUE_LENGTH_1);
			progressDialog.setProgress(0);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setTitle("import");
			progressDialog.show();
		}

		@Override
		protected void onPostExecute(Void result) {
			progressDialog.dismiss();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			progressDialog.setProgress(values[0]);
		}

	}

}
