package com.github.andlyticsproject;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.console.v2.DevConsoleRegistry;
import com.github.andlyticsproject.console.v2.DevConsoleV2;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.Comment;
import com.github.andlyticsproject.model.CommentGroup;
import com.github.andlyticsproject.util.DetachableAsyncTask;
import com.github.andlyticsproject.util.Utils;

public class CommentsActivity extends BaseDetailsActivity {

	public static final String TAG = Main.class.getSimpleName();

	private CommentsListAdapter commentsListAdapter;

	private ExpandableListView list;

	private View footer;

	private int maxAvalibleComments;

	private ArrayList<CommentGroup> commentGroups;

	private List<Comment> comments;

	public int nextCommentIndex;

	private View nocomments;

	public boolean hasMoreComments;

	private ContentAdapter db;

	private static final int MAX_LOAD_COMMENTS = 20;

	private static class State {
		LoadCommentsCache loadCommentsCache;
		LoadCommentsData loadCommentsData;
		List<Comment> comments;

		void detachAll() {
			if (loadCommentsCache != null) {
				loadCommentsCache.detach();
			}

			if (loadCommentsData != null) {
				loadCommentsData.detach();
			}
		}

		void attachAll(CommentsActivity activity) {
			if (loadCommentsCache != null) {
				loadCommentsCache.attach(activity);
			}

			if (loadCommentsData != null) {
				loadCommentsData.attach(activity);
			}
		}

		void setLoadCommentsCache(LoadCommentsCache task) {
			if (loadCommentsCache != null) {
				loadCommentsCache.detach();
			}
			loadCommentsCache = task;
		}

		void setLoadCommentsData(LoadCommentsData task) {
			if (loadCommentsData != null) {
				loadCommentsData.detach();
			}
			loadCommentsData = task;
		}
	}

	private State state = new State();

	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.comments);

		list = (ExpandableListView) findViewById(R.id.comments_list);
		nocomments = (View) findViewById(R.id.comments_nocomments);

		// footer
		View inflate = getLayoutInflater().inflate(R.layout.comments_list_footer, null);
		footer = (View) inflate.findViewById(R.id.comments_list_footer);
		list.addFooterView(inflate, null, false);

		View header = getLayoutInflater().inflate(R.layout.comments_list_header, null);
		list.addHeaderView(header, null, false);

		list.setGroupIndicator(null);

		commentsListAdapter = new CommentsListAdapter(this);
		list.setAdapter(commentsListAdapter);

		maxAvalibleComments = -1;
		commentGroups = new ArrayList<CommentGroup>();
		comments = new ArrayList<Comment>();

		footer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				forceLoadCommentsData();
			}
		});
		footer.setVisibility(View.GONE);

		db = getDbAdapter();

		State lastState = (State) getLastNonConfigurationInstance();
		if (lastState != null) {
			state = lastState;
			state.attachAll(this);
			if (state.comments != null) {
				comments = state.comments;
				rebuildCommentGroups();
				expandCommentGroups();
				loadCommentsData();
			}
		} else {
			state.setLoadCommentsCache(new LoadCommentsCache(this));
			Utils.execute(state.loadCommentsCache);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		state.comments = comments;
		state.detachAll();

		return state;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		getSupportMenuInflater().inflate(R.menu.comments_menu, menu);
		if (isRefreshing())
			menu.findItem(R.id.itemCommentsmenuRefresh).setActionView(
					R.layout.action_bar_indeterminate_progress);
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
		case R.id.itemCommentsmenuRefresh:
			maxAvalibleComments = -1;
			nextCommentIndex = 0;
			forceLoadCommentsData();
			return true;
		default:
			return (super.onOptionsItemSelected(item));
		}
	}

	private static class LoadCommentsCache extends
			DetachableAsyncTask<Void, Void, Void, CommentsActivity> {

		LoadCommentsCache(CommentsActivity activity) {
			super(activity);
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (activity == null) {
				return null;
			}

			activity.comments = activity.db.getCommentsFromCache(activity.packageName);
			activity.rebuildCommentGroups();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (activity == null) {
				return;
			}

			activity.expandCommentGroups();
			activity.loadCommentsData();
		}

	}

	protected boolean shouldRemoteUpdateComments() {
		long now = System.currentTimeMillis();
		long lastUpdate = AndlyticsDb.getInstance(this)
				.getLastCommentsRemoteUpdateTime(packageName);
		// never updated
		if (lastUpdate == 0) {
			return true;
		}

		return (now - lastUpdate) >= Preferences.COMMENTS_REMOTE_UPDATE_INTERVAL;
	}

	private void expandCommentGroups() {
		if (comments.size() > 0) {
			commentsListAdapter.setCommentGroups(commentGroups);
			for (int i = 0; i < commentGroups.size(); i++) {
				list.expandGroup(i);
			}
			commentsListAdapter.notifyDataSetChanged();
		}
	}

	private static class LoadCommentsData extends
			DetachableAsyncTask<Void, Void, Exception, CommentsActivity> {

		LoadCommentsData(CommentsActivity activity) {
			super(activity);
		}

		@Override
		protected void onPreExecute() {
			if (activity == null) {
				return;
			}

			activity.refreshStarted();
			activity.footer.setEnabled(false);
		}

		@Override
		protected Exception doInBackground(Void... params) {
			if (activity == null) {
				return null;
			}

			Exception exception = null;

			if (activity.maxAvalibleComments == -1) {

				ContentAdapter db = activity.getDbAdapter();
				AppStats appInfo = db.getLatestForApp(activity.packageName);
				if (appInfo != null) {
					activity.maxAvalibleComments = appInfo.getNumberOfComments();
				} else {
					activity.maxAvalibleComments = MAX_LOAD_COMMENTS;
				}
			}

			if (activity.maxAvalibleComments != 0) {
				DevConsoleV2 console = DevConsoleRegistry.getInstance().get(activity.accountName);
				try {

					List<Comment> result = console.getComments(activity, activity.packageName,
							activity.nextCommentIndex, MAX_LOAD_COMMENTS);

					// put in cache if index == 0
					if (activity.nextCommentIndex == 0) {
						activity.db.updateCommentsCache(result, activity.packageName);
						activity.comments.clear();
					}
					activity.comments.addAll(result);

					activity.rebuildCommentGroups();

				} catch (Exception e) {
					exception = e;
				}

				activity.nextCommentIndex += MAX_LOAD_COMMENTS;
				if (activity.nextCommentIndex >= activity.maxAvalibleComments) {
					activity.hasMoreComments = false;
				} else {
					activity.hasMoreComments = true;
				}
			}

			return exception;
		}

		@Override
		protected void onPostExecute(Exception exception) {
			if (activity == null) {
				return;
			}

			activity.refreshFinished();
			activity.footer.setEnabled(true);

			if (exception != null) {
				Log.e(TAG, "Error fetching comments: " + exception.getMessage(), exception);
				activity.handleUserVisibleException(exception);
				activity.footer.setVisibility(View.GONE);

				return;
			}

			activity.footer.setVisibility(View.VISIBLE);

			if (activity.comments != null && activity.comments.size() > 0) {
				activity.commentsListAdapter.setCommentGroups(activity.commentGroups);
				for (int i = 0; i < activity.commentGroups.size(); i++) {
					activity.list.expandGroup(i);
				}
				activity.commentsListAdapter.notifyDataSetChanged();
			} else {
				activity.nocomments.setVisibility(View.VISIBLE);
			}

			if (!activity.hasMoreComments) {
				activity.footer.setVisibility(View.GONE);
			}

			AndlyticsDb.getInstance(activity).saveLastCommentsRemoteUpdateTime(
					activity.packageName, System.currentTimeMillis());
		}

	}

	public void rebuildCommentGroups() {
		commentGroups = new ArrayList<CommentGroup>();
		Comment prevComment = null;
		for (Comment comment : Comment.expandReplies(comments)) {
			if (prevComment != null) {

				CommentGroup group = new CommentGroup();
				group.setDate(comment.getDate());

				if (commentGroups.contains(group)) {

					int index = commentGroups.indexOf(group);
					group = commentGroups.get(index);
					group.addComment(comment);

				} else {
					addNewCommentGroup(comment);
				}

			} else {
				addNewCommentGroup(comment);
			}
			prevComment = comment;
		}

	}

	private void addNewCommentGroup(Comment comment) {
		CommentGroup group = new CommentGroup();
		group.setDate(comment.getDate());
		List<Comment> groupComments = new ArrayList<Comment>();
		groupComments.add(comment);
		group.setComments(groupComments);
		commentGroups.add(group);
	}

	private void loadCommentsData() {
		loadCommentsData(false);
	}

	private void forceLoadCommentsData() {
		loadCommentsData(true);
	}

	private void loadCommentsData(boolean forceLoad) {
		if (forceLoad || shouldRemoteUpdateComments()) {
			state.setLoadCommentsData(new LoadCommentsData(this));
			Utils.execute(state.loadCommentsData);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_AUTHENTICATE) {
			if (resultCode == RESULT_OK) {
				// user entered credentials, etc, try to get data again
				loadCommentsData();
			} else {
				Toast.makeText(this, getString(R.string.auth_error, accountName), Toast.LENGTH_LONG)
						.show();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

}
