package com.github.andlyticsproject;

import java.util.ArrayList;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.andlyticsproject.model.Link;

public class LinksListAdapter extends BaseAdapter {

	private LayoutInflater layoutInflater;

	private ArrayList<Link> links;

	public LinksListAdapter(LinksActivity activity) {
		this.layoutInflater = activity.getLayoutInflater();
		this.links = new ArrayList<Link>();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		final Link link = getItem(position);
		ViewHolderChild holder;

		if (convertView == null) {
			convertView = layoutInflater.inflate(R.layout.links_list_item, null);

			holder = new ViewHolderChild();
			holder.name = (TextView) convertView.findViewById(R.id.links_list_item_name);
			holder.url = (TextView) convertView.findViewById(R.id.links_list_item_url);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolderChild) convertView.getTag();
		}
		
		holder.name.setText(link.getName());
		holder.url.setText(link.getURL());

		return convertView;
	}

	static class ViewHolderChild {
		TextView name;
		TextView url;
	}
	
	public void setLinks(ArrayList<Link> links) {
		this.links = links;
	}

	@Override
	public int getCount() {
		return links.size();
	}

	@Override
	public Link getItem(int position) {
		return links.get(position);
	}

	@Override
	public long getItemId(int position) {
		return links.get(position).getId().longValue();
	}
}
