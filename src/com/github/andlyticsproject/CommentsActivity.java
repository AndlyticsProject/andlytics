package com.github.andlyticsproject;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.List;

import com.github.andlyticsproject.model.AppStats;
import com.github.andlyticsproject.model.Comment;
import com.github.andlyticsproject.model.CommentGroup;

public class CommentsActivity extends BaseActivity implements AuthenticationCallback {

    public static final String TAG = Main.class.getSimpleName();
    
    private CommentsListAdapter commentsListAdapter;

    private ExpandableListView list;

    private View footer;

    private ViewSwitcher toolbarViewSwitcher;

    private int maxAvalibleComments;

    private ArrayList<CommentGroup> commentGroups;

    private List<Comment> comments;

    public int nextCommentIndex;

    private View nocomments;

    public boolean hasMoreComments;

    private ContentAdapter db;

    private View refreshButton;

    private View backButton;

    private static final int MAX_LOAD_COMMENTS = 20;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.comments);
        toolbarViewSwitcher = (ViewSwitcher) findViewById(R.id.comments_toobar_switcher);
        refreshButton = (View)findViewById(R.id.comments_button_refresh);
        backButton = (View)findViewById(R.id.comments_button_back);
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

        if (iconFilePath != null) {

            ImageView appIcon = (ImageView) findViewById(R.id.comments_app_icon);
            Bitmap bm = BitmapFactory.decodeFile(iconFilePath);
            appIcon.setImageBitmap(bm);
        }

        maxAvalibleComments = -1;
        commentGroups = new ArrayList<CommentGroup>();
        comments = new ArrayList<Comment>();
        
        refreshButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                maxAvalibleComments = -1;
                nextCommentIndex = 0;
                authenticateAccountFromPreferences(false, CommentsActivity.this);
            }
        });
        
        backButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                startActivity(Main.class, false, true);
                overridePendingTransition(R.anim.activity_prev_in, R.anim.activity_prev_out);
            }
        });

        footer.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                authenticateAccountFromPreferences(false, CommentsActivity.this);
            }
        });
        footer.setVisibility(View.GONE);
        
        db = getDbAdapter();

    }

    @Override
    protected void onResume() {
        super.onResume();
        new LoadCommentsCache().execute();

    }
    
    private class LoadCommentsCache extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            comments = db.getCommentsFromCache(packageName);
            rebuildCommentGroups();
            
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if(comments.size() > 0) {
                commentsListAdapter.setCommentGroups(commentGroups);
                for (int i = 0; i < commentGroups.size(); i++) {
                    list.expandGroup(i);
                }
                commentsListAdapter.notifyDataSetChanged();
            }
            
            authenticateAccountFromPreferences(false, CommentsActivity.this);
            
        }
        
    }

    
    private class LoadCommentsData extends AsyncTask<Void, Void, Exception> {

        @Override
        protected Exception doInBackground(Void... params) {

            Exception exception = null;

            if (maxAvalibleComments == -1) {

                ContentAdapter db = getDbAdapter();
                AppStats appInfo = db.getLatestForApp(packageName);
                if (appInfo != null) {
                    maxAvalibleComments = appInfo.getNumberOfComments();
                } else {
                    maxAvalibleComments = MAX_LOAD_COMMENTS;
                }
            }

            if (maxAvalibleComments != 0) {
                DeveloperConsole console = new DeveloperConsole(CommentsActivity.this);
                try {

                    
                    String authtoken = getAndlyticsApplication().getAuthToken();
                    List<Comment> result = console.getAppComments(authtoken, accountname, packageName,
                                    nextCommentIndex, MAX_LOAD_COMMENTS);
                    
                    // put in cache if index == 0
                    if(nextCommentIndex == 0) {
                        db.updateCommentsCache(result, packageName);
                        comments.clear();
                    }
                    comments.addAll(result);

                    rebuildCommentGroups();

                } catch (Exception e) {
                    exception = e;
                }

                nextCommentIndex += MAX_LOAD_COMMENTS;
                if (nextCommentIndex >= maxAvalibleComments) {
                    hasMoreComments = false;
                } else {
                    hasMoreComments = true;
                }

            }

            return exception;
        }

        @Override
        protected void onPostExecute(Exception result) {

            footer.setEnabled(true);

            if (result != null) {
                handleUserVisibleException(result);
                result.printStackTrace();
                footer.setVisibility(View.GONE);
            } else {

                footer.setVisibility(View.VISIBLE);

                if (comments != null && comments.size() > 0) {

                    commentsListAdapter.setCommentGroups(commentGroups);
                    for (int i = 0; i < commentGroups.size(); i++) {
                        list.expandGroup(i);
                    }
                    commentsListAdapter.notifyDataSetChanged();
                } else {
                    nocomments.setVisibility(View.VISIBLE);
                }

                if (!hasMoreComments) {
                    footer.setVisibility(View.GONE);
                }

            }

            hideLoadingIndecator(toolbarViewSwitcher);
        }

        @Override
        protected void onPreExecute() {
            showLoadingIndecator(toolbarViewSwitcher);
            footer.setEnabled(false);
        }

    }

    public void rebuildCommentGroups() {

        commentGroups = new ArrayList<CommentGroup>();
        Comment prevComment = null;
        for (Comment comment : comments) {
            if (prevComment != null) {

                CommentGroup group = new CommentGroup();
                group.setDateString(comment.getDate());

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
        group.setDateString(comment.getDate());
        List<Comment> groupComments = new ArrayList<Comment>();
        groupComments.add(comment);
        group.setComments(groupComments);
        commentGroups.add(group);
    }

    @Override
    public void authenticationSuccess() {
        
        new LoadCommentsData().execute();
        
    }

}
