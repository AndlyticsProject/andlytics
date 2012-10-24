package com.github.andlyticsproject.console.v2;

import java.util.HashMap;
import java.util.Map;

public class DevConsoleRegistry {

	private Map<String, DeveloperConsoleV2> registry = new HashMap<String, DeveloperConsoleV2>();

	private static DevConsoleRegistry instance = new DevConsoleRegistry();

	private DevConsoleRegistry() {
	}

	public static DevConsoleRegistry getInstance() {
		return instance;
	}

	public synchronized void put(String accountName, DeveloperConsoleV2 devConsole) {
		registry.put(accountName, devConsole);
	}

	public synchronized DeveloperConsoleV2 get(String accountName) {
		return registry.get(accountName);
	}
}
