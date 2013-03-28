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
	private String developerId;

	public static DeveloperAccount createActive(String name) {
		return new DeveloperAccount(name, State.ACTIVE);
	}

	public static DeveloperAccount createHidden(String name) {
		return new DeveloperAccount(name, State.HIDDEN);
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

	public synchronized String getDeveloperId() {
		return developerId;
	}

	public synchronized void setDeveloperId(String developerId) {
		this.developerId = developerId;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DeveloperAccount)) {
			return false;
		}
		DeveloperAccount rhs = (DeveloperAccount) o;

		if (!name.equals(rhs.name)) {
			return false;
		}

		if (developerId == null) {
			return developerId == rhs.developerId;
		}

		return developerId.equals(rhs.developerId);
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + name.hashCode();
		result = 31 * result + developerId == null ? 0 : developerId.hashCode();

		return result;
	}

	@Override
	public String toString() {
		return String.format("DeveloperAccount [name=%s, developerId=%s, state=%s]", name,
				developerId, state);
	}

	public boolean isHidden() {
		return state == State.HIDDEN;
	}

	public boolean isActive() {
		return state == State.ACTIVE;
	}

	public boolean isSelected() {
		return state == State.SELECTED;
	}

	public boolean isVisible() {
		return state != State.HIDDEN;
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
