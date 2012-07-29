
package com.github.andlyticsproject.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.util.FloatMath;
import android.util.Log;

public class LRUBitmapCache {

	private static final float hashTableLoadFactor = 0.75f;

	private LinkedHashMap<String, Bitmap> map;

	private int cacheSize;

	private static final String TAG = LRUBitmapCache.class.getSimpleName();

	/**
	 * Creates a new LRU cache.
	 *
	 * @param cacheSize
	 *            the maximum number of entries that will be kept in this cache.
	 */
	public LRUBitmapCache(int cacheSize) {
		this.cacheSize = cacheSize;
		int hashTableCapacity = (int) FloatMath
				.ceil(cacheSize / hashTableLoadFactor) + 1;
		map = new LinkedHashMap<String, Bitmap>(hashTableCapacity, hashTableLoadFactor,
				true) {
			// (an anonymous inner class)
			private static final long serialVersionUID = 1;

			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) {
				boolean result = size() > LRUBitmapCache.this.cacheSize;
				if (result) {
					if (eldest.getValue() != null) {
						Log.d(TAG, "Recycling bitmap: " + eldest.getKey() + " current cache size: " + size());
					}
				}
				return result;
			}
		};
	}

	/**
	 * Retrieves an entry from the cache.<br>
	 * The retrieved entry becomes the MRU (most recently used) entry.
	 *
	 * @param key
	 *            the key whose associated value is to be returned.
	 * @return the value associated to this key, or null if no value with this
	 *         key exists in the cache.
	 */
	public synchronized Bitmap get(String key) {
		return map.get(key);
	}

	/**
	 * Adds an entry to this cache. The new entry becomes the MRU (most recently
	 * used) entry. If an entry with the specified key already exists in the
	 * cache, it is replaced by the new entry. If the cache is full, the LRU
	 * (least recently used) entry is removed from the cache.
	 *
	 * @param key
	 *            the key with which the specified value is to be associated.
	 * @param value
	 *            a value to be associated with the specified key.
	 */
	public synchronized void put(String key, Bitmap value) {
		map.put(key, value);
	}

	/**
	 * Clears the cache.
	 */
	public synchronized void clear() {
		map.clear();
	}

	/**
	 * Returns the number of used entries in the cache.
	 *
	 * @return the number of entries currently in the cache.
	 */
	public synchronized int usedEntries() {
		return map.size();
	}

	public synchronized boolean contains(String item) {
		return map.containsKey(item);
	}

	/**
	 * Returns a <code>Collection</code> that contains a copy of all cache
	 * entries.
	 *
	 * @return a <code>Collection</code> with a copy of the cache content.
	 */
	public synchronized Collection<Map.Entry<String, Bitmap>> getAll() {
		return new ArrayList<Map.Entry<String, Bitmap>>(map.entrySet());
	}

}
