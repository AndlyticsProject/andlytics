package com.github.andlyticsproject;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.dialog.AddEditLinkDialog;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.model.Link;
import com.github.andlyticsproject.util.DetachableAsyncTask;
import com.github.andlyticsproject.util.Utils;

public class LinksActivity extends SherlockFragmentActivity implements
		AddEditLinkDialog.OnFinishAddEditLinkDialogListener {

	public static final String TAG = Main.class.getSimpleName();

	private LinksListAdapter linksListAdapter;

	private LoadLinksDb loadLinksDb;

	private AppInfo appInfo;
	private List<Link> links;

	private ListView list;

	private View nolinks;

	private AndlyticsDb db;

	private static final int EDIT = 1;
	private static final int DELETE = 2;

	private String packageName;
	private String iconFilePath;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.links);

		Bundle b = getIntent().getExtras();
		if (b != null) {
			packageName = b.getString(Constants.PACKAGE_NAME_PARCEL);
			iconFilePath = b.getString(Constants.ICON_FILE_PARCEL);
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		String appName = getDbAdapter().getAppName(packageName);
		if (appName != null) {
			getSupportActionBar().setSubtitle(appName);
		}

		if (iconFilePath != null) {
			Bitmap bm = BitmapFactory.decodeFile(iconFilePath);
			BitmapDrawable icon = new BitmapDrawable(getResources(), bm);
			getSupportActionBar().setIcon(icon);
		}

		list = (ListView) findViewById(R.id.links_list);

		nolinks = findViewById(R.id.links_nolinks);
		list.setEmptyView(nolinks);

		links = new ArrayList<Link>();

		linksListAdapter = new LinksListAdapter(this);
		list.setAdapter(linksListAdapter);

		linksListAdapter.setLinks(links);
		linksListAdapter.notifyDataSetChanged();

		db = AndlyticsDb.getInstance(this);

		loadLinksDb = new LoadLinksDb(this);
		Utils.execute(loadLinksDb);

		registerForContextMenu(list);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, EDIT, 0, R.string.edit);
		menu.add(0, DELETE, 0, R.string.delete);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		switch (item.getItemId()) {
		case EDIT:
			android.widget.AdapterView.AdapterContextMenuInfo menuInfo = (android.widget.AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();

			int position = menuInfo.position;
			Link link = links.get(position);

			showAddEditLinkDialog(link);
			return true;
		case DELETE:
			menuInfo = (android.widget.AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

			position = menuInfo.position;
			link = links.get(position);

			db.deleteLink(link.getId().longValue());

			loadLinksDb = new LoadLinksDb(this);
			Utils.execute(loadLinksDb);

			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		getSupportMenuInflater().inflate(R.menu.links_menu, menu);

		return true;
	}

	/**
	 * Called if item in option menu is selected.
	 * 
	 * @param item
	 * The chosen menu item
	 * @return boolean true/false
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			overridePendingTransition(R.anim.activity_prev_in, R.anim.activity_prev_out);
			return true;
		case R.id.itemLinksmenuAdd:
			showAddEditLinkDialog(null);
			return true;
		default:
			return (super.onOptionsItemSelected(item));
		}
	}

	@Override
	public void onBackPressed() {
		finish();
		overridePendingTransition(R.anim.activity_prev_in, R.anim.activity_prev_out);
	}

	public ContentAdapter getDbAdapter() {
		return getAndlyticsApplication().getDbAdapter();
	}

	public AndlyticsApp getAndlyticsApplication() {
		return (AndlyticsApp) getApplication();
	}

	private static class LoadLinksDb extends DetachableAsyncTask<Void, Void, Void, LinksActivity> {

		LoadLinksDb(LinksActivity activity) {
			super(activity);
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (activity == null) {
				return null;
			}

			activity.getLinksFromDb();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (activity == null) {
				return;
			}

			activity.refreshLinks();
		}

	}

	private void getLinksFromDb() {
		appInfo = db.findAppByPackageName(packageName);
		links = appInfo.getDetails().getLinks();
	}

	private void refreshLinks() {
		linksListAdapter.setLinks(links);
		linksListAdapter.notifyDataSetChanged();

		// TODO display these somehow
		System.out.println(appInfo.getDetails().getDescription());
		System.out.println(appInfo.getDetails().getChangelog());
		System.out.println(appInfo.getDetails().getLastStoreUpdate());
	}

	@Override
	public void onFinishAddEditLink(String url, String name, Long id) {
		if (id == null) {
			db.addLink(appInfo.getDetails(), url, name);
		} else {
			db.editLink(id, url, name);
		}

		loadLinksDb = new LoadLinksDb(this);
		Utils.execute(loadLinksDb);
	}

	private void showAddEditLinkDialog(Link link) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag("fragment_addedit_link");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		AddEditLinkDialog addEditLinkDialog = new AddEditLinkDialog();

		Bundle arguments = new Bundle();
		if (link != null) {
			arguments.putLong("id", link.getId().longValue());
			arguments.putString("name", link.getName());
			arguments.putString("url", link.getURL());
		}

		arguments.putString("packageName", packageName);

		addEditLinkDialog.setArguments(arguments);

		addEditLinkDialog.setOnFinishAddEditLinkDialogListener(this);

		addEditLinkDialog.show(ft, "fragment_addedit_link");
	}
}
