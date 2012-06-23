package com.github.andlyticsproject.view;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

public class ChartGalleryAdapter extends BaseAdapter {

	private List<View> views;

	public ChartGalleryAdapter(List<View> views) {
		this.setViews(views);
	}

	
	public View getView(int position, View convertView, ViewGroup parent) {
		return getItem(position);
	}


	@Override
	public int getCount() {
		return getViews().size();
	}


	@Override
	public View getItem(int position) {
		return getViews().get(position);
	}


	@Override
	public long getItemId(int position) {
		return position;
	}


	public void setViews(List<View> views) {
		this.views = views;
	}


	public List<View> getViews() {
		return views;
	}

	
}