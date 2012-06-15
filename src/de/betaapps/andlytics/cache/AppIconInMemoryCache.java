package de.betaapps.andlytics.cache;


public class AppIconInMemoryCache extends LRUBitmapCache {

	private static final long serialVersionUID = 1L;

	private static final int MAX_ENTRIES = 100;

	private static AppIconInMemoryCache instance;

	public AppIconInMemoryCache() {
		super(MAX_ENTRIES);
	}

	public static AppIconInMemoryCache getInstance() {
		if (instance == null) {
			instance = new AppIconInMemoryCache();
		}
		return instance;
	}
	
}
