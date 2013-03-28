package com.github.andlyticsproject.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AppDetails {

	private Long id;

	private String description;
	private String changelog;
	private Date lastStoreUpdate;

	private List<Link> links = new ArrayList<Link>();

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

	public List<Link> getLinks() {
		return Collections.unmodifiableList(links);
	}

	public void setLinks(List<Link> links) {
		this.links = links;
	}

	public void addLink(Link link) {
		links.add(link);
	}

}
