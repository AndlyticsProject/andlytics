package com.github.andlyticsproject;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.github.andlyticsproject.CommentsFragment.Comments;
import com.github.andlyticsproject.console.v2.DevConsoleRegistry;
import com.github.andlyticsproject.console.v2.DevConsoleV2;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.Comment;
import com.github.andlyticsproject.model.CommentGroup;
import com.github.andlyticsproject.model.StatsSummary;
import com.github.andlyticsproject.util.LoaderBase;
import com.github.andlyticsproject.util.LoaderResult;
import com.github.andlyticsproject.util.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class CommentsFragment extends Fragment implements StatsView<Comment>,
		LoaderManager.LoaderCallbacks<LoaderResult<Comments>> {

	private static final String TAG = CommentsFragment.class.getSimpleName();

	private static final int MAX_LOAD_COMMENTS = 20;

	private static final int DB_LOADER_ID = 0;
	private static final int REMOTE_LOADER_ID = 1;

	private CommentsListAdapter commentsListAdapter;
	private ExpandableListView list;
	private View nocomments;
	private View footer;

	private int maxAvailableComments;
	// need to preserve insertion order
	// yyymmdd -> CommentGroup
	private LinkedHashMap<String, CommentGroup> commentGroups;
	private List<Comment> comments;
	public int nextCommentIndex;
	public boolean hasMoreComments;

	private DevConsoleV2 devConsole;

	protected DetailedStatsActivity statsActivity;

	static class Comments {
		List<Comment> loaded = new ArrayList<Comment>();
		int maxAvailableComments = 0;
		boolean cacheUpdated = false;
		boolean canReply = true;
	}

	static class CommentsLoader extends LoaderBase<Comments> {

		protected Activity activity;
		protected ContentAdapter db;
		protected String packageName;
		protected String accountName;
		protected String developerId;
		protected int nextCommentIndex;

		protected List<Comment> comments;
		protected int maxAvailableComments = -1;

		public CommentsLoader(Activity context, String accountName, String developerId,
				String packageName, int nextCommentIndex) {
			super(context);
			this.activity = context;
			db = ContentAdapter.getInstance(AndlyticsApp.getInstance());
			this.accountName = accountName;
			this.developerId = developerId;
			this.packageName = packageName;
			this.nextCommentIndex = nextCommentIndex;
		}

		@Override
		protected Comments load() throws Exception {
			if (packageName == null || accountName == null || developerId == null) {
				return null;
			}

			return loadFromCache();
		}

		protected void updateCommentsCacheIfNecessary(List<Comment> newComments) {
			if (newComments == null || newComments.isEmpty()) {
				return;
			}

			// if refreshing, clear and rebuild cache
			if (nextCommentIndex == 0) {
				updateCommentsCache(newComments);
			}
		}

		protected void updateCommentsCache(List<Comment> commentsToCache) {
			db.updateCommentsCache(commentsToCache, packageName);
			comments = new ArrayList<Comment>();
		}


		protected Comments loadFromCache() {
			Comments result = new Comments();
			result.cacheUpdated = false;
			result.canReply = true;
			result.loaded = db.getCommentsFromCache(packageName);
			AppStats appInfo = db.getLatestForApp(packageName);
			if (appInfo != null) {
				result.maxAvailableComments = appInfo.getNumberOfComments();
			} else {
				result.maxAvailableComments = comments.size();
			}

			return result;
		}

		@Override
		protected void releaseResult(LoaderResult<Comments> result) {
			// just a string, nothing to do
		}

		@Override
		protected boolean isActive(LoaderResult<Comments> result) {
			return false;
		}
	}

	static class RemoteCommentsLoader extends CommentsLoader {

		public RemoteCommentsLoader(Activity context, String accountName, String developerId,
				String packageName, int nextCommentIndex) {
			super(context, accountName, developerId, packageName, nextCommentIndex);
		}

		@Override
		protected Comments load() throws Exception {
			if (packageName == null || accountName == null || developerId == null) {
				return null;
			}

			boolean canReply = true;
			if (maxAvailableComments == -1) {
				AppStats appInfo = db.getLatestForApp(packageName);
				if (appInfo != null) {
					maxAvailableComments = appInfo.getNumberOfComments();
				} else {
					maxAvailableComments = MAX_LOAD_COMMENTS;
				}
			}

			Comments result = new Comments();
			if (maxAvailableComments != 0) {
				DevConsoleV2 console = DevConsoleRegistry.getInstance().get(accountName);

				List<Comment> loaded = console.getComments(activity, packageName, developerId,
						nextCommentIndex, MAX_LOAD_COMMENTS, Utils.getDisplayLocale());
				updateCommentsCacheIfNecessary(loaded);
				// XXX
				// we can only do this after we authenticate at least once,
				// which may not happen before refreshing if we are loading
				// from cache
				canReply = console.canReplyToComments();

				AndlyticsDb.getInstance(getContext()).saveLastCommentsRemoteUpdateTime(packageName,
						System.currentTimeMillis());

				result.loaded = loaded;
				result.maxAvailableComments = maxAvailableComments;
				result.cacheUpdated = true;
				result.canReply = canReply;
			}

			return result;
		}

	}

	public CommentsFragment() {
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		getActivity().getActionBar().setTitle(getTitle());
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// calling initLoader() here results in onLoadFinished() being 
		// called twice. Bad things happen then...
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.comments_fragment, container, false);

		list = (ExpandableListView) view.findViewById(R.id.comments_list);
		// TODO Use ListView.setEmptyView
		nocomments = (View) view.findViewById(R.id.comments_nocomments);

		// footer
		View inflate = inflater.inflate(R.layout.comments_list_footer, null);
		footer = (View) inflate.findViewById(R.id.comments_list_footer);
		list.addFooterView(inflate, null, false);

		View header = inflater.inflate(R.layout.comments_list_header, null);
		list.addHeaderView(header, null, false);

		list.setGroupIndicator(null);

		devConsole = DevConsoleRegistry.getInstance().get(statsActivity.getAccountName());
		commentsListAdapter = new CommentsListAdapter(getActivity());
		if (devConsole.hasSessionCredentials()) {
			commentsListAdapter.setCanReplyToComments(devConsole.canReplyToComments());
		}
		list.setAdapter(commentsListAdapter);

		maxAvailableComments = -1;
		commentGroups = new LinkedHashMap<String, CommentGroup>();
		comments = new ArrayList<Comment>();

		footer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				fetchNextComments();
			}
		});
		hideFooter();

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (statsActivity.shouldRemoteUpdateStats()) {
			loadRemoteData();
		} else {
			loadCurrentData();
		}
	}

	private void loadRemoteData() {
		Bundle args = new Bundle();
		args.putString("accountName", statsActivity.getAccountName());
		args.putString("developerId", statsActivity.getDeveloperId());
		args.putString("packageName", statsActivity.getPackage());
		args.putInt("nextCommentIndex", nextCommentIndex);

		statsActivity.refreshStarted();
		disableFooter();

		getLoaderManager().restartLoader(REMOTE_LOADER_ID, args, this);
	}

	private void loadCurrentData() {
		nextCommentIndex = 0;
		Bundle args = new Bundle();
		args.putString("accountName", statsActivity.getAccountName());
		args.putString("developerId", statsActivity.getDeveloperId());
		args.putString("packageName", statsActivity.getPackage());
		args.putInt("nextCommentIndex", nextCommentIndex);

		statsActivity.refreshStarted();
		disableFooter();

		getLoaderManager().initLoader(DB_LOADER_ID, args, this);
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.comments_menu, menu);

		if (statsActivity.isRefreshing()) {
			menu.findItem(R.id.itemCommentsmenuRefresh).setActionView(
					R.layout.action_bar_indeterminate_progress);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Context ctx = getActivity();
		if (ctx == null) {
			return false;
		}

		switch (item.getItemId()) {
		case R.id.itemCommentsmenuRefresh:
			refreshComments();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void refreshCommentsIfNecessary() {
		if (shouldRemoteUpdateComments()) {
			resetNextCommentIndex();
			fetchNextComments();
		}
	}

	void refreshComments() {
		resetNextCommentIndex();
		fetchNextComments();
	}

	private void incrementNextCommentIndex(int increment) {
		nextCommentIndex += increment;
		if (nextCommentIndex >= maxAvailableComments) {
			hasMoreComments = false;
		} else {
			hasMoreComments = true;
		}
	}

	private void resetNextCommentIndex() {
		maxAvailableComments = -1;
		nextCommentIndex = 0;
		hasMoreComments = true;
	}

	private void fetchNextComments() {
		loadRemoteData();
	}

	protected boolean shouldRemoteUpdateComments() {
		if (comments == null || comments.isEmpty()) {
			return true;
		}

		long now = System.currentTimeMillis();
		long lastUpdate = AndlyticsDb.getInstance(getActivity()).getLastCommentsRemoteUpdateTime(
				statsActivity.getPackage());
		// never updated
		if (lastUpdate == 0) {
			return true;
		}

		return (now - lastUpdate) >= Preferences.COMMENTS_REMOTE_UPDATE_INTERVAL;
	}


	private void enableFooter() {
		footer.setEnabled(true);
	}

	private void disableFooter() {
		footer.setEnabled(false);
	}

	private void hideFooter() {
		footer.setVisibility(View.GONE);
	}

	private void showFooterIfNecessary() {
		footer.setVisibility(hasMoreComments ? View.VISIBLE : View.GONE);
	}

	private void expandCommentGroups() {
		if (comments != null && comments.size() > 0) {
			nocomments.setVisibility(View.GONE);
			commentsListAdapter.setCommentGroups(toList(commentGroups));
			for (int i = 0; i < commentGroups.size(); i++) {
				list.expandGroup(i);
			}
			commentsListAdapter.notifyDataSetChanged();
		} else {
			nocomments.setVisibility(View.VISIBLE);
		}
	}

	private static List<CommentGroup> toList(LinkedHashMap<String, CommentGroup> groups) {
		List<CommentGroup> result = new ArrayList<CommentGroup>();
		for (LinkedHashMap.Entry<String, CommentGroup> e : groups.entrySet()) {
			result.add(e.getValue());
		}

		return result;
	}

	public void rebuildCommentGroups() {
		long start = System.currentTimeMillis();
		commentGroups = new LinkedHashMap<String, CommentGroup>();
		List<Comment> expanded = Comment.expandReplies(comments);
		for (Comment comment : expanded) {
			CommentGroup group = new CommentGroup(comment);
			if (commentGroups.containsKey(group.getFormattedDate())) {
				group = commentGroups.get(group.getFormattedDate());
				group.addComment(comment);
			} else {
				commentGroups.put(group.getFormattedDate(), group);
			}
		}

		if (BuildConfig.DEBUG) {
			long elapsed = System.currentTimeMillis() - start;
			Log.d(TAG, String.format("rebuildCommentGroups took: %d [ms]", elapsed));
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			statsActivity = (DetailedStatsActivity) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		statsActivity = null;
	}


	@Override
	public void updateView(StatsSummary<Comment> statsSummary) {
		// XXX do nothing, revise interface!
	}

	@Override
	public String getTitle() {
		// this can be called before the fragment is attached
		Context ctx = AndlyticsApp.getInstance();
		return ctx.getString(R.string.comments);
	}


	@Override
	public Loader<LoaderResult<Comments>> onCreateLoader(int id, Bundle args) {
		String accountName = null;
		String developerId = null;
		String packageName = null;
		int nextCommentIndex = 0;
		if (args != null) {
			accountName = args.getString("accountName");
			developerId = args.getString("developerId");
			packageName = args.getString("packageName");
			nextCommentIndex = args.getInt("nextCommentIndex");
		}

		if (id == DB_LOADER_ID) {
			return new CommentsLoader(getActivity(), accountName, developerId, packageName,
					nextCommentIndex);
		}

		// id = 1
		return new RemoteCommentsLoader(getActivity(), accountName, developerId, packageName,
				nextCommentIndex);
	}


	@Override
	public void onLoadFinished(Loader<LoaderResult<Comments>> loader, LoaderResult<Comments> result) {
		if (getActivity() == null) {
			return;
		}

		statsActivity.refreshFinished();
		enableFooter();

		if (result.isFailed()) {
			Log.e(TAG, "Error fetching comments: " + result.getError().getMessage(),
					result.getError());
			statsActivity.handleUserVisibleException(result.getError());
			hideFooter();

			return;
		}

		if (result.getData() == null) {
			return;
		}

		Comments newComments = result.getData();
		boolean refreshed = false;
		if (comments == null || comments.isEmpty()) {
			comments = newComments.loaded;
		} else {
			// if refreshing clear current
			if (nextCommentIndex == 0) {
				comments = newComments.loaded;
				refreshed = true;
			} else {
				// 	otherwise add to adapter and display
				comments.addAll(newComments.loaded);
			}
		}
		maxAvailableComments = newComments.maxAvailableComments;

		commentsListAdapter.setCanReplyToComments(newComments.canReply);
		incrementNextCommentIndex(newComments.loaded.size());
		// XXX optimize or move to loader!
		rebuildCommentGroups();
		showFooterIfNecessary();

		expandCommentGroups();
		if (refreshed) {
			list.setSelectedGroup(0);
		}

		// XXX check here?
		if (!result.getData().cacheUpdated) {
			refreshCommentsIfNecessary();
		}
	}


	@Override
	public void onLoaderReset(Loader<LoaderResult<Comments>> loader) {
	}

	@Override
	public void setCurrentChart(int page, int column) {
		// NOOP, we don't have charts
	}

	@Override
	public int getCurrentChart() {
		// we don't have charts
		return -1;
	}

}
