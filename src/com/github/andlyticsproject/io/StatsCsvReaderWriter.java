package com.github.andlyticsproject.io;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.Revenue;
import com.github.andlyticsproject.util.FileUtils;
import com.github.andlyticsproject.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@SuppressLint("SimpleDateFormat")
public class StatsCsvReaderWriter {

	private static final String TAG = StatsCsvReaderWriter.class.getSimpleName();

	public static final String[] HEADER_LIST = new String[] { "PACKAGE_NAME", "DATE",
			"TOTAL_DOWNLOADS", "ACTIVE_INSTALLS", "NUMBER_OF_COMMENTS", "1_STAR_RATINGS",
			"2_STAR_RATINGS", "3_STAR_RATINGS", "4_STAR_RATINGS", "5_STAR_RATINGS", "VERSION_CODE",
			"NUM_ERRORS", "TOTAL_REVENUE", "CURRENCY" };

	private static final String EXPORT_DIR = "andlytics/";

	private static final String DEFAULT_EXPORT_ZIP_FILE = "andlytics.zip";
	private static final String EXPORT_ZIP_FILE_TEMPLATE = "andlytics-%s.zip";

	private static final String CSV_SUFFIX = ".csv";

	// create this every time because SDF is not threadsafe
	private static SimpleDateFormat createTimestampFormat() {
		SimpleDateFormat result = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		result.setTimeZone(TimeZone.getTimeZone("UTC"));

		return result;
	}

	public static String getExportDirPath() {
		return getExportDir().getAbsolutePath();
	}

	public static File getExportDir() {
		return new File(Environment.getExternalStorageDirectory(), EXPORT_DIR);
	}

	public static File getDefaultExportFile() {
		return new File(getExportDir(), DEFAULT_EXPORT_ZIP_FILE);
	}

	public static File getExportFileForAccount(String accountName) {
		return new File(getExportDir(), String.format(EXPORT_ZIP_FILE_TEMPLATE, accountName));
	}

	public static String getAccountNameForExport(String filename) {
		int firstDashIdx = filename.indexOf('-');
		int suffixIdx = filename.indexOf(".zip");
		if (firstDashIdx == -1 || suffixIdx == -1) {
			return null;
		}

		return filename.substring(firstDashIdx + 1, suffixIdx);
	}

	public StatsCsvReaderWriter(Context context) {
	}

	@SuppressWarnings("resource")
	public void writeStats(String packageName, List<AppStats> stats, ZipOutputStream zip)
			throws IOException {
		zip.putNextEntry(new ZipEntry(packageName + CSV_SUFFIX));

		// we don't own the stream, it's closed by the caller
		CSVWriter writer = new CSVWriter(new OutputStreamWriter(zip));
		writer.writeNext(HEADER_LIST);

		String[] line = new String[HEADER_LIST.length];

		for (AppStats stat : stats) {

			line[0] = packageName;
			line[1] = createTimestampFormat().format(stat.getDate());
			line[2] = Integer.toString(stat.getTotalDownloads());
			line[3] = Integer.toString(stat.getActiveInstalls());
			line[4] = Integer.toString(stat.getNumberOfComments());

			line[5] = Utils.safeToString(stat.getRating1());
			line[6] = Utils.safeToString(stat.getRating2());
			line[7] = Utils.safeToString(stat.getRating3());
			line[8] = Utils.safeToString(stat.getRating4());
			line[9] = Utils.safeToString(stat.getRating5());

			line[10] = Utils.safeToString(stat.getVersionCode());

			line[11] = Utils.safeToString(stat.getNumberOfErrors());

			line[12] = stat.getTotalRevenue() == null ? "" : String.format(Locale.US, "%.2f", stat
					.getTotalRevenue().getAmount());
			line[13] = stat.getTotalRevenue() == null ? "" : stat.getTotalRevenue()
					.getCurrencyCode();

			writer.writeNext(line);
		}
		writer.flush();
	}

	public static List<String> getImportFileNamesFromZip(String accountName,
			List<String> packageNames, String zipFilename) throws ServiceException {

		List<String> result = new ArrayList<String>();

		try {
			if (!new File(zipFilename).exists()) {
				return result;
			}

			ZipFile zipFile = new ZipFile(zipFilename);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				InputStream in = zipFile.getInputStream(entry);
				if (isValidFile(accountName, in, packageNames)) {
					result.add(entry.getName());
				}
			}

			zipFile.close();
			return result;
		} catch (IOException e) {
			Log.e(TAG, "Error reading zip file: " + e.getMessage());

			return new ArrayList<String>();
		}

	}

	private static boolean isValidFile(String accountName, InputStream in, List<String> packageNames)
			throws ServiceException {

		if (packageNames.isEmpty()) {
			return true;
		}

		CSVReader reader = null;
		try {
			reader = new CSVReader(new InputStreamReader(in));

			String[] firstLine = reader.readNext();
			if (firstLine != null) {
				if (HEADER_LIST.length >= firstLine.length) {
					for (int i = 0; i < firstLine.length - 1; i++) {
						if (!HEADER_LIST[i].equals(firstLine[i])) {
							return false;
						}
					}

					// validate package name
					String[] secondLine = reader.readNext();
					String packageName = secondLine[0];
					if (secondLine != null) {
						return packageNames.contains(packageName);
					}
				}
			}
		} catch (FileNotFoundException e) {
			throw new ServiceException(e);
		} catch (IOException e) {
			throw new ServiceException(e);
		} finally {
			FileUtils.closeSilently(reader);
		}

		return false;
	}

	public static String getPackageName(String filename) {
		int suffixIdx = filename.indexOf(CSV_SUFFIX);
		if (suffixIdx == -1) {
			return null;
		}

		return filename.substring(0, suffixIdx);
	}

	@SuppressWarnings("resource")
	public List<AppStats> readStats(InputStream in) throws ServiceException {

		List<AppStats> appStats = new ArrayList<AppStats>();

		CSVReader reader;
		try {
			// we don't own the stream, it's closed by the caller
			reader = new CSVReader(new InputStreamReader(in));

			String[] firstLine = reader.readNext();

			if (firstLine != null) {

				String[] nextLine = null;

				while ((nextLine = reader.readNext()) != null) {

					AppStats stats = new AppStats();
					stats.setPackageName(nextLine[0]);
					stats.setDate(createTimestampFormat().parse(nextLine[1]));
					stats.setTotalDownloads(Integer.parseInt(nextLine[2]));
					stats.setActiveInstalls(Integer.parseInt(nextLine[3]));
					stats.setNumberOfComments(Integer.parseInt(nextLine[4]));
					stats.setRating1(Integer.parseInt(nextLine[5]));
					stats.setRating2(Integer.parseInt(nextLine[6]));
					stats.setRating3(Integer.parseInt(nextLine[7]));
					stats.setRating4(Integer.parseInt(nextLine[8]));
					stats.setRating5(Integer.parseInt(nextLine[9]));

					if (nextLine.length > 10) {
						stats.setVersionCode(Integer.parseInt(nextLine[10]));
					}

					if (nextLine.length > 11) {
						String numErrorsStr = nextLine[11];
						stats.setNumberOfErrors(parseInt(numErrorsStr));
					}

					if (nextLine.length > 12) {
						String totalRevenueStr = nextLine[12];
						if (!TextUtils.isEmpty(totalRevenueStr)) {
							String currency = nextLine[13];
							stats.setTotalRevenue(new Revenue(Revenue.Type.TOTAL,
									parseDouble(totalRevenueStr.trim()), currency));
						}
					}

					appStats.add(stats);

				}

			}
		} catch (FileNotFoundException e) {
			throw new ServiceException(e);
		} catch (IOException e) {
			throw new ServiceException(e);
		} catch (ParseException e) {
			throw new ServiceException(e);
		}

		return appStats;
	}

	private Double parseDouble(String totalRevenueStr) {
		return TextUtils.isEmpty(totalRevenueStr) ? null : Double.parseDouble(totalRevenueStr);
	}

	private Integer parseInt(String intStr) {
		return TextUtils.isEmpty(intStr) ? null : Integer.parseInt(intStr);
	}

	public String readPackageName(String fileName) throws ServiceException {
		try {
			return readPackageName(new FileInputStream(new File(getExportDirPath(), fileName)));
		} catch (IOException e) {
			throw new ServiceException(e);
		}
	}

	public String readPackageName(InputStream in) throws ServiceException {
		String packageName = null;

		CSVReader reader;
		try {
			reader = new CSVReader(new InputStreamReader(in));

			String[] firstLine = reader.readNext();

			if (firstLine != null) {

				String[] nextLine = null;

				while ((nextLine = reader.readNext()) != null) {

					packageName = nextLine[0];
				}
			}
			reader.close();

		} catch (FileNotFoundException e) {
			throw new ServiceException(e);
		} catch (IOException e) {
			throw new ServiceException(e);
		}

		return packageName;

	}

}
