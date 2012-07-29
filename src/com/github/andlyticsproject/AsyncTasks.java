
package com.github.andlyticsproject;

import java.util.List;

import android.os.AsyncTask;

import com.github.andlyticsproject.model.AppInfo;

public class AsyncTasks {

	public interface LoadAppListTaskCompleteListener {
		public void onLoadAppListTaskComplete(List<AppInfo> apps);
	}

	public static class LoadAppListTask extends AsyncTask<String, Void, List<AppInfo>> {

		private List<AppInfo> mResult = null;
		private LoadAppListTaskCompleteListener mCallback;

		public LoadAppListTask(LoadAppListTaskCompleteListener callback) {
			mCallback = callback;
		}

		public void attach(LoadAppListTaskCompleteListener callback) {
			mCallback = callback;
		}

		public void detach() {
			mCallback = null;
		}

		public List<AppInfo> getResult() {
			return mResult;
		}

		@Override
		protected List<AppInfo> doInBackground(String... params) {
			return AndlyticsApp.getInstance().getDbAdapter().getAllAppsLatestStats(params[0]);
		}

		@Override
		protected void onPostExecute(List<AppInfo> result) {
			mResult = result;
			if (mCallback != null) {
				mCallback.onLoadAppListTaskComplete(mResult);
			}
		}

	}
}
