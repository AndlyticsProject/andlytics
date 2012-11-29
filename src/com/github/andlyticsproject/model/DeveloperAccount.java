package com.github.andlyticsproject.model;

import java.util.Date;

public class DeveloperAccount {

	public enum State {
		HIDDEN, ACTIVE, SELECTED
	};

	private Long id;
	private String name;
	private State state;
	private Date lastStatsUpdate;

	public DeveloperAccount(String name) {
		this(name, State.ACTIVE);
	}

	public DeveloperAccount(String name, State state) {
		if (name == null || "".equals(name)) {
			throw new IllegalArgumentException("Name must not be empty or null");
		}
		this.name = name;
		this.state = state;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public Date getLastStatsUpdate() {
		return lastStatsUpdate == null ? null : (Date) lastStatsUpdate.clone();
	}

	public void setLastStatsUpdate(Date lastStatsUpdate) {
		this.lastStatsUpdate = lastStatsUpdate == null ? null : (Date) lastStatsUpdate.clone();
	}

	@Override
	public String toString() {
		return String.format("DeveloperAccount [name=%s, state=%s]", name, state);
	}

	public boolean isHidden() {
		return state == State.HIDDEN;
	}

	public boolean isSelected() {
		return state == State.SELECTED;
	}

	public void select() {
		state = State.SELECTED;
	}

	public void hide() {
		state = State.HIDDEN;
		lastStatsUpdate = null;
	}

	public void activate() {
		state = State.ACTIVE;
	}

	public void deselect() {
		activate();
	}

}
