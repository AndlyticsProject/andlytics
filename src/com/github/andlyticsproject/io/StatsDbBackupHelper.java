package com.github.andlyticsproject.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.github.andlyticsproject.AndlyticsApp;
import com.github.andlyticsproject.ContentAdapter;
import com.github.andlyticsproject.Preferences;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.AppStatsList;

public class StatsDbBackupHelper implements BackupHelper {

	private static final String TAG = StatsDbBackupHelper.class.getSimpleName();

	private Context ctx;

	public StatsDbBackupHelper(Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
			ParcelFileDescriptor newState) {
		try {
			Log.d(TAG, "******onBackup");
			// backup preferences
			//		super.onBackup(oldState, data, newState);
			Log.d(TAG, "Backed up SharedPreferences");

			long lastModified = getDbLastModified();

			if (oldState != null && oldState.getStatSize() > 0) {
				Log.d(TAG, String.format("oldState statSize=%d", oldState.getStatSize()));
				FileInputStream instream = new FileInputStream(oldState.getFileDescriptor());
				DataInputStream in = new DataInputStream(instream);

				try {
					long stateModified = in.readLong();
					// nothing to do
					if (stateModified == lastModified) {
						return;
					}
				} catch (EOFException e) {
					// ignore? 
				}
			}

			// Write structured data
			// XXX get active accounts from preferences? Race? 
			// also needs to lock the DB?
			String accountName = Preferences.getAccountName(ctx);
			ContentAdapter db = AndlyticsApp.getInstance().getDbAdapter();
			List<AppInfo> appInfos = db.getAllAppsLatestStats(accountName);
			List<String> packageNames = new ArrayList<String>();
			for (AppInfo ai : appInfos) {
				packageNames.add(ai.getPackageName());
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipOutputStream zip = new ZipOutputStream(baos);
			StatsCsvReaderWriter statsWriter = new StatsCsvReaderWriter(ctx);
			for (String packageName : packageNames) {
				Log.d(TAG, "Backing up data for : " + packageName);
				AppStatsList statsForApp = db.getStatsForApp(packageName, Timeframe.UNLIMITED,
						false);
				statsWriter.writeStats(packageName, statsForApp.getAppStats(), zip);
			}
			Log.d(TAG, String.format("Backed up for %d apps", packageNames.size()));

			// Send the data to the Backup Manager via the BackupDataOutput
			byte[] buff = baos.toByteArray();
			data.writeEntityHeader(accountName, buff.length);
			data.writeEntityData(buff, buff.length);

			writeStateFile(newState, lastModified);
		} catch (IOException e) {
			// ignore?
			Log.w(TAG, "Error writing newState: " + e.getMessage(), e);
		}
	}

	@Override
	public void restoreEntity(BackupDataInputStream data) {
		try {
			String key = data.getKey();
			int dataSize = data.size();

			// TODO compare to what here? Preferences might not be restored yet?
			//		String accountName = Preferences.getAccountName(this);
			String accountName = "nikolay.elenkov@gmail.com";
			if (accountName.equals(key)) {
				byte[] buff = new byte[dataSize];
				data.read(buff, 0, dataSize);
				ByteArrayInputStream bais = new ByteArrayInputStream(buff);
				ZipInputStream inzip = new ZipInputStream(bais);

				ZipEntry entry = null;
				StatsCsvReaderWriter statsWriter = new StatsCsvReaderWriter(ctx);
				ContentAdapter db = AndlyticsApp.getInstance().getDbAdapter();
				while ((entry = inzip.getNextEntry()) != null) {
					Log.d(TAG, "Reading data from  " + entry.getName());
					try {
						List<AppStats> stats = statsWriter.readStats(inzip);
						if (!stats.isEmpty()) {
							String packageName = stats.get(0).getPackageName();
							Log.d(TAG, "Restoring data for  " + packageName);
							for (AppStats appStats : stats)
								db.insertOrUpdateAppStats(appStats, packageName);
						}
					} catch (ServiceException e) {
						Log.w(TAG, "Error reading app data: " + e.getMessage(), e);
						//try the next one?
						continue;
					}
				}
			}
		} catch (IOException e) {
			// ignore?
			Log.w(TAG, "Error writing newState: " + e.getMessage(), e);
		}
	}

	@Override
	public void writeNewStateDescription(ParcelFileDescriptor newState) {
		try {
			writeStateFile(newState, getDbLastModified());
		} catch (IOException e) {
			// ignore?
			Log.w(TAG, "Error writing newState: " + e.getMessage(), e);
		}

	}

	private void writeStateFile(ParcelFileDescriptor state, long lastModified) throws IOException {
		Log.d(TAG, String.format("Writing lastModified = %d to state file", lastModified));
		FileOutputStream outstream = new FileOutputStream(state.getFileDescriptor());
		DataOutputStream out = new DataOutputStream(outstream);
		out.writeLong(lastModified);
	}

	private long getDbLastModified() {
		File andlyticsDb = ctx.getDatabasePath("andlytics");
		long lastModified = andlyticsDb.lastModified();
		return lastModified;
	}

}
