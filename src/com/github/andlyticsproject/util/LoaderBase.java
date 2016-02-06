package com.github.andlyticsproject.util;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

public abstract class LoaderBase<T> extends AsyncTaskLoader<LoaderResult<T>> {

    private static final String TAG = LoaderBase.class.getSimpleName();

    protected LoaderResult<T> lastResult;

    protected LoaderBase(Context context) {
        super(context);
    }

    @Override
    public void deliverResult(LoaderResult<T> result) {
        if (isReset()) {
            if (result != null) {
                releaseResult(result);
            }
            return;
        }

        LoaderResult<T> oldResult = lastResult;
        lastResult = result;

        if (isStarted()) {
            super.deliverResult(result);
        }

        if (oldResult != null && oldResult != result && isActive(result)) {
            releaseResult(oldResult);
        }
    }

    @Override
    protected void onStartLoading() {
        if (lastResult != null) {
            deliverResult(lastResult);
        }

        if (takeContentChanged() || lastResult == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(LoaderResult<T> result) {
        super.onCanceled(result);

        if (result != null && isActive(result)) {
            releaseResult(result);
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        onStopLoading();

        if (lastResult != null && isActive(lastResult)) {
            releaseResult(lastResult);
        }
        lastResult = null;
    }

    @Override
    public LoaderResult<T> loadInBackground() {
        try {
            return LoaderResult.create(load());
        } catch (Exception e) {
            Log.e(TAG, "Error loading data: " + e.getMessage(), e);

            return LoaderResult.createFailed(e);
        }
    }

    protected abstract T load() throws Exception;

    protected abstract void releaseResult(LoaderResult<T> result);

    protected abstract boolean isActive(LoaderResult<T> result);

}
