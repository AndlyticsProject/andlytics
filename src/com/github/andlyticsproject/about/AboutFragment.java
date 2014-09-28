package com.github.andlyticsproject.about;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.github.andlyticsproject.R;
import com.github.andlyticsproject.util.DataLoader;

import java.io.IOException;

public class AboutFragment extends Fragment {
	private static final String TAG = "AboutFragment";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup group,
			Bundle saved) {
		return inflater.inflate(R.layout.about_content, group, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		WebView creditsWebView = (WebView) getActivity().findViewById(
				R.id.about_thirdsparty_credits);
		try {
			creditsWebView.loadData(DataLoader.loadData(getActivity()
					.getBaseContext(), "credits_thirdparty"), "text/html",
					"UTF-8");
		} catch (IOException ioe) {
			Log.e(TAG, "Error reading changelog file!", ioe);
		}
	}		
}
