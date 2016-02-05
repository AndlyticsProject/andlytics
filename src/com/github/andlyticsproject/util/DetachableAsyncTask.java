package com.github.andlyticsproject.util;

import android.os.AsyncTask;


public abstract class DetachableAsyncTask<Params, Progress, Result, Parent> extends
		AsyncTask<Params, Progress, Result> {

	protected Parent activity;

	public DetachableAsyncTask(Parent activity) {
		this.activity = activity;
	}

	public void attach(Parent activity) {
		this.activity = activity;
	}

	public DetachableAsyncTask<Params, Progress, Result, Parent> detach() {
		activity = null;

		return this;
	}

	Parent getParent() {
		return activity;
	}
}
