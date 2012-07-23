package com.github.andlyticsproject.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.AppStats;

public class StatsCsvReaderWriter {

	private static final String TAG = StatsCsvReaderWriter.class.getSimpleName();

	public static final String[] HEADER_LIST = new String[] { "PACKAGE_NAME", "DATE",
			"TOTAL_DOWNLOADS", "ACTIVE_INSTALLS", "NUMBER_OF_COMMENTS", "1_STAR_RATINGS",
			"2_STAR_RATINGS", "3_STAR_RATINGS", "4_STAR_RATINGS", "5_STAR_RATINGS", "VERSION_CODE" };

	private static final String EXPORT_DIR = "andlytics/";

	private static final String EXPORT_ZIP_FILE = "andlytics.zip";

	static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");
	{
		TIMESTAMP_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public static String getDefaultDirectory() {
		return Environment.getExternalStorageDirectory() + System.getProperty("file.separator")
				+ EXPORT_DIR;
	}

	public static File getDefaultExportFile() {
		return new File(Environment.getExternalStorageDirectory()
				+ System.getProperty("file.separator") + EXPORT_DIR, EXPORT_ZIP_FILE);
	}


	public StatsCsvReaderWriter(Context context) {
	}

	public void writeStats(String packageName, List<AppStats> stats) throws IOException {

		String path = getDefaultDirectory();

		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		File file = new File(dir + "/" + packageName + ".csv");

		if (file.exists()) {
			file.delete();
		}

		FileWriter fileWriter = new FileWriter(file);

		CSVWriter writer = new CSVWriter(fileWriter);
		writer.writeNext(HEADER_LIST);

		String[] line = new String[HEADER_LIST.length];

		for (AppStats stat : stats) {

			line[0] = packageName;
			line[1] = TIMESTAMP_FORMAT.format(stat.getRequestDate());
			line[2] = stat.getTotalDownloads() + "";
			line[3] = stat.getActiveInstalls() + "";
			line[4] = stat.getNumberOfComments() + "";

			line[5] = stat.getRating1() + "";
			line[6] = stat.getRating2() + "";
			line[7] = stat.getRating3() + "";
			line[8] = stat.getRating4() + "";
			line[9] = stat.getRating5() + "";

			line[10] = stat.getVersionCode() + "";

			writer.writeNext(line);

		}

		writer.close();
		fileWriter.close();

	}

	public static List<String> getImportFileNames(String accountName, List<AppInfo> appInfos)
			throws ServiceExceptoin {

		List<String> result = new ArrayList<String>();

		String defaultDirectory = StatsCsvReaderWriter.getDefaultDirectory();

		File file = new File(defaultDirectory);

		if (file.exists()) {
			File[] listFiles = file.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String filename) {
					//                    return filename.endsWith(".csv") || filename.endsWith(".CSV");
					return filename.equalsIgnoreCase(EXPORT_ZIP_FILE);
				}

			});

			for (File dirFile : listFiles) {
				result.add(dirFile.getName());
			}
		}

		//        Iterator<String> iterator = result.iterator();
		//        while (iterator.hasNext()) {
		//            String fileName = (String) iterator.next();
		//
		//            if(!isValidFile(accountName, fileName, appInfos)) {
		//                 iterator.remove();
		//            }
		//
		//        }

		return result;

	}

	public static List<String> getImportFileNamesFromZip(String accountName,
			List<AppInfo> appInfos, String zipFilename) throws ServiceExceptoin {

		List<String> result = new ArrayList<String>();

		try {
			if (!new File(zipFilename).exists()) {
				return result;
			}

			ZipFile zipFile = new ZipFile(zipFilename);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String filename = entry.getName();
				if (isValidFile(accountName, filename, appInfos)) {
					result.add(entry.getName());
				}
			}
			return result;
		} catch (IOException e) {
			Log.e(TAG, "Error reading zip file: " + e.getMessage());

			return new ArrayList<String>();
		}

	}

	private static boolean isValidFile(String accountName, String fileName, List<AppInfo> apps)
			throws ServiceExceptoin {

		if (apps.isEmpty()) {
			return true;
		}

		String dir = getDefaultDirectory();

		File file = new File(dir + "/" + fileName);

		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(file));

			String[] firstLine = reader.readNext();

			if (firstLine != null) {

				if (HEADER_LIST.length >= firstLine.length) {

					for (int i = 0; i < HEADER_LIST.length - 1; i++) {

						if (!HEADER_LIST[i].equals(firstLine[i])) {
							return false;
						}
					}

					// validate package name
					String[] secondLine = reader.readNext();
					if (secondLine != null) {

						for (AppInfo appInfo : apps) {
							if (appInfo.getPackageName().equals(secondLine[0])) {
								return true;
							}
						}

					}
				}
			}

		} catch (FileNotFoundException e) {
			throw new ServiceExceptoin(e);
		} catch (IOException e) {
			throw new ServiceExceptoin(e);
		}

		return false;
	}


	public List<AppStats> readStats(String fileName) throws ServiceExceptoin {
		try {
			return readStats(new FileInputStream(new File(getDefaultDirectory(), fileName)));
		} catch (IOException e) {
			throw new ServiceExceptoin(e);
		}
	}

	public List<AppStats> readStats(InputStream in) throws ServiceExceptoin {

		List<AppStats> appStats = new ArrayList<AppStats>();

		CSVReader reader;
		try {
			reader = new CSVReader(new InputStreamReader(in));

			String[] firstLine = reader.readNext();

			if (firstLine != null) {

				String[] nextLine = null;

				while ((nextLine = reader.readNext()) != null) {

					AppStats stats = new AppStats();
					stats.setPackageName(nextLine[0]);
					stats.setRequestDate(TIMESTAMP_FORMAT.parse(nextLine[1]));
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

					appStats.add(stats);

				}
			}

		} catch (FileNotFoundException e) {
			throw new ServiceExceptoin(e);
		} catch (IOException e) {
			throw new ServiceExceptoin(e);
		} catch (ParseException e) {
			throw new ServiceExceptoin(e);
		}

		return appStats;
	}


	public String readPackageName(String fileName) throws ServiceExceptoin {
		try {
			return readPackageName(new FileInputStream(new File(getDefaultDirectory(), fileName)));
		} catch (IOException e) {
			throw new ServiceExceptoin(e);
		}
	}

	public String readPackageName(InputStream in) throws ServiceExceptoin {
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

		} catch (FileNotFoundException e) {
			throw new ServiceExceptoin(e);
		} catch (IOException e) {
			throw new ServiceExceptoin(e);
		}

		return packageName;

	}

}
