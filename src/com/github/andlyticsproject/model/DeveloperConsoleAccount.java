package com.github.andlyticsproject.model;

public class DeveloperConsoleAccount {

	private String developerId;
	private String name;

	public DeveloperConsoleAccount(String developerId, String name) {
		this.developerId = developerId;
		this.name = name;
	}

	public String getDeveloperId() {
		return developerId;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DeveloperConsoleAccount)) {
			return false;
		}
		DeveloperConsoleAccount rhs = (DeveloperConsoleAccount) o;

		return developerId.equals(rhs.developerId);
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + developerId.hashCode();

		return result;
	}

	@Override
	public String toString() {
		return String
				.format("DeveloperConsoleAccount [developerId=%s, name=%s]", developerId, name);
	}

}
