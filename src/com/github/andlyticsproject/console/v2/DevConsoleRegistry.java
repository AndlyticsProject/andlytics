package com.github.andlyticsproject.console.v2;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.client.DefaultHttpClient;

public class DevConsoleRegistry {

	private Map<String, DevConsoleV2> registry = new HashMap<String, DevConsoleV2>();

	private static DevConsoleRegistry instance = new DevConsoleRegistry();

	private DevConsoleRegistry() {
	}

	public static DevConsoleRegistry getInstance() {
		return instance;
	}

	public synchronized void put(String accountName, DevConsoleV2 devConsole) {
		registry.put(accountName, devConsole);
	}

	public synchronized DevConsoleV2 get(String accountName) {
		DevConsoleV2 result = registry.get(accountName);
		if (result == null) {
			DefaultHttpClient httpClient = HttpClientFactory
					.createDevConsoleHttpClient(DevConsoleV2.TIMEOUT);
			result = DevConsoleV2.createForAccount(accountName, httpClient);
			registry.put(accountName, result);
		}

		return result;
	}
}
