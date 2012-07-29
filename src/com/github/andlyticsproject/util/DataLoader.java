
package com.github.andlyticsproject.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;

/**
 * Utility implementation for loading data which is pre-packages in the app.
 * 
 * @author appricoo, Andy Scherzinger
 * @version $Id: DataLoader.java 577 2011-08-17 00:01:37Z ascherzinger $
 */
public final class DataLoader {
	/** Private constructor. */
	private DataLoader() {
	};

	/**
	 * reads the changelog file and returns its content as a String.
	 * 
	 * @param context
	 *            the context.
	 * @param ressourceName
	 *            Name of the ressource to be loaded
	 * @return content of the ressource as a String.
	 * @throws IOException
	 *             if errors occur while reading the changelog file
	 */
	public static String loadData(final Context context, final String ressourceName)
			throws IOException {
		int resourceIdentifier = context
				.getApplicationContext()
				.getResources()
				.getIdentifier(ressourceName, "raw",
						context.getApplicationContext().getPackageName());
		if (resourceIdentifier != 0) {
			InputStream inputStream = context.getApplicationContext().getResources()
					.openRawResource(resourceIdentifier);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			String line;
			StringBuffer data = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				data.append(line);
			}
			reader.close();
			return data.toString();
		}
		return null;
	}
}
