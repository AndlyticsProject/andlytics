package com.github.andlyticsproject.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppDetails {

	private Long id;

	private String description;
	private String changelog;
	private Date lastStoreUpdate;

	private List<String> links = new ArrayList<String>();

	public AppDetails(String description, String changelog, Date lastStoreUpdate) {
		this.description = description;
		this.changelog = changelog;
		this.lastStoreUpdate = lastStoreUpdate == null ? null : (Date) lastStoreUpdate.clone();
	}

	public AppDetails(String description, String changelog, Long lastStoreUpdate) {
		this.description = description;
		this.changelog = changelog;
		this.lastStoreUpdate = lastStoreUpdate == null ? null : new Date(lastStoreUpdate);
	}

	public AppDetails(String description) {
		this(description, null, (Date) null);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getChangelog() {
		return changelog;
	}

	public void setChangelog(String changelog) {
		this.changelog = changelog;
	}

	public Date getLastStoreUpdate() {
		return lastStoreUpdate == null ? null : (Date) lastStoreUpdate.clone();
	}

	public void setLastStoreUpdate(Date lastStoreUpdate) {
		this.lastStoreUpdate = lastStoreUpdate == null ? null : (Date) lastStoreUpdate.clone();
	}

	public List<String> getLinks() {
		return links;
	}

	public void setLinks(List<String> links) {
		this.links = links;
	}

}
