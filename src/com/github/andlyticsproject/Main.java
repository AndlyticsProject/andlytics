package com.github.andlyticsproject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.github.andlyticsproject.Preferences.StatsMode;
import com.github.andlyticsproject.Preferences.Timeframe;
import com.github.andlyticsproject.admob.AdmobRequest;
import com.github.andlyticsproject.dialog.AutosyncDialog;
import com.github.andlyticsproject.dialog.ExportDialog;
import com.github.andlyticsproject.dialog.GhostDialog;
import com.github.andlyticsproject.dialog.GhostDialog.GhostSelectonChangeListener;
import com.github.andlyticsproject.dialog.ImportDialog;
import com.github.andlyticsproject.dialog.NotificationsDialog;
import com.github.andlyticsproject.exception.AuthenticationException;
import com.github.andlyticsproject.exception.InvalidJSONResponseException;
import com.github.andlyticsproject.exception.NetworkException;
import com.github.andlyticsproject.io.ServiceExceptoin;
import com.github.andlyticsproject.io.StatsCsvReaderWriter;
import com.github.andlyticsproject.model.Admob;
import com.github.andlyticsproject.model.AppInfo;
import com.github.andlyticsproject.sync.AutosyncHandler;
import com.github.andlyticsproject.sync.AutosyncHandlerFactory;

public class Main extends BaseActivity implements GhostSelectonChangeListener, AuthenticationCallback {

    public static final String TAG = Main.class.getSimpleName();
    private View buttonRefresh;
    private boolean cancelRequested;
    private ListView mainListView;
    private ContentAdapter db;
    private TextView statusText;
    private ViewSwitcher mainViewSwitcher;
    private MainListAdapter adapter;
    public boolean dotracking;
    private View footer;
    private View feedbackButton;
    private View ghostButton;
    public GhostDialog ghostDialog;

    private boolean isAuthenticationRetry;
    private ViewSwitcher progressSwitcher;
    public Animation aniPrevIn;
    private View statsModeToggle;
    private StatsMode currentStatsMode;
    private TextView statsModeText;
    private ImageView statsModeIcon;
    private View notificationButton;
    private View autosyncButton;
    public NotificationsDialog notificationDialog;
    private View exportButton;
    private View importButton;
    public ExportDialog exportDialog;
    public ImportDialog importDialog;

    private static final int FEEDBACK_DIALOG = 0;



    /** Called when the activity is first created. */
    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        db = getDbAdapter();
        LayoutInflater layoutInflater = getLayoutInflater();

        // style headline
        TextView headline = (TextView) findViewById(R.id.main_headline);
        Style.getInstance(getAssets()).styleHeadline(headline);
        TextView headlineBeta = (TextView) findViewById(R.id.main_headline_beta);
        Style.getInstance(getAssets()).styleHeadline(headlineBeta);

        // setup main list
        mainListView = (ListView) findViewById(R.id.main_app_list);
        mainListView.addHeaderView(layoutInflater.inflate(R.layout.main_list_header, null), null, false);
        footer = layoutInflater.inflate(R.layout.main_list_footer, null);
        footer.setVisibility(View.INVISIBLE);
        TextView accountNameTextView = (TextView) footer.findViewById(R.id.main_app_account_name);
        accountNameTextView.setText(accountname);
        mainListView.addFooterView(footer, null, false);
        adapter = new MainListAdapter(this, accountname, db, currentStatsMode);
        mainListView.setAdapter(adapter);
        mainViewSwitcher = (ViewSwitcher) findViewById(R.id.main_viewswitcher);

        // status & progess bar
        statusText = (TextView) findViewById(R.id.main_app_status_line);
        feedbackButton = (View) findViewById(R.id.main_feedback_button);
        ghostButton = (View) findViewById(R.id.main_ghost_button);
        notificationButton = (View) findViewById(R.id.main_notification_button);
        autosyncButton = (View) findViewById(R.id.main_autosync_button);
        exportButton = (View) findViewById(R.id.main_export_button);
        importButton = (View) findViewById(R.id.main_import_button);
        progressSwitcher = (ViewSwitcher) findViewById(R.id.main_toobar_switcher);

        statsModeToggle = (View) findViewById(R.id.main_button_statsmode);
        statsModeText = (TextView) findViewById(R.id.main_button_statsmode_text);
        statsModeIcon = (ImageView) findViewById(R.id.main_button_statsmode_icon);

        aniPrevIn = AnimationUtils.loadAnimation(Main.this, R.anim.activity_fade_in);

        statsModeToggle.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (currentStatsMode.equals(StatsMode.PERCENT)) {
                    currentStatsMode = StatsMode.DAY_CHANGES;
                } else {
                    currentStatsMode = StatsMode.PERCENT;
                }

                updateStatsMode();

            }
        });

        View buttonLogout = (View) findViewById(R.id.main_button_logout);
        buttonLogout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Preferences.removeAccountName(Main.this);
                Preferences.saveSkipAutoLogin(Main.this, true);
                Intent intent = new Intent(Main.this, LoginActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
            }
        });

        buttonRefresh = (View) findViewById(R.id.main_button_refresh);
        buttonRefresh.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                authenticateAccountFromPreferences(false, Main.this);
            }
        });

        feedbackButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

               showDialog(FEEDBACK_DIALOG);
            }
        });

        ghostButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                (new LoadGhostDialog()).execute();
            }
        });

        exportButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if(!isProVersion()) {
                    showProDialog();
                } else {
                    (new LoadExportDialog()).execute();
                }

            }
        });

        importButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!isProVersion()) {
                    showProDialog();
                } else {
                    (new LoadImportDialog()).execute();
                }
            }
        });

        notificationButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!isProVersion()) {
                    showProDialog();
                } else {
                    (new LoadNotificationDialog()).execute();
                }
            }
        });

        autosyncButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showAutosyncDialog();
            }
        });

        dotracking = true;
        isAuthenticationRetry = false;

        currentStatsMode = Preferences.getStatsMode(this);
        updateStatsMode();

        final List<AppInfo> lastAppList = (List<AppInfo>) getLastNonConfigurationInstance();
        if (lastAppList != null) {
            getAndlyticsApplication().setSkipMainReload(true);

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean mainSkipDataReload = getAndlyticsApplication().isSkipMainReload();

        if (!mainSkipDataReload) {
            new LoadDbEntries().execute(true);
        } else {
            new LoadDbEntries().execute(false);
        }

        getAndlyticsApplication().setSkipMainReload(false);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return adapter.getAppInfos();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void updateMainList(List<AppInfo> apps) {

        if (apps != null) {

            if (apps.size() > 0) {
                footer.setVisibility(View.VISIBLE);

                String autosyncSet = Preferences.getAutosyncSet(Main.this, accountname);
                if (autosyncSet == null) {

                    // set autosync default value
                    AutosyncHandlerFactory.getInstance(Main.this).setAutosyncPeriod(accountname,
                                    AutosyncHandler.DEFAULT_PERIOD);

                    Preferences.saveAutosyncSet(Main.this, accountname);
                }
            }

            adapter.setAppInfos(apps);
            adapter.notifyDataSetChanged();

            Date lastUpdateDate = null;

            for (int i = 0; i < apps.size(); i++) {
                Date dateObject = apps.get(i).getLastUpdate();
                if (lastUpdateDate == null || lastUpdateDate.before(dateObject)) {
                    lastUpdateDate = dateObject;
                }
            }

            if (lastUpdateDate != null) {
                statusText.setText(this.getString(R.string.last_update) + ": " + ContentAdapter.formatDate(lastUpdateDate));
            }

        }

        if (!(R.id.main_app_list == mainViewSwitcher.getCurrentView().getId())) {
            mainViewSwitcher.showNext();
        }

    }

    private class LoadRemoteEntries extends AsyncTask<String, Integer, Exception> {

        @SuppressWarnings("unchecked")
        @Override
        protected Exception doInBackground(String... params) {

            // authentication failed before, retry with token invalidation

            Exception exception = null;



                String authtoken = ((AndlyticsApp) getApplication()).getAuthToken();


                List<AppInfo> appDownloadInfos = null;
                try {

                    DeveloperConsole console = new DeveloperConsole(Main.this);
                    appDownloadInfos = console.getAppDownloadInfos(authtoken, accountname);

                    if (cancelRequested) {
                        cancelRequested = false;
                        return null;
                    }

                    Map<String, List<String>> admobAccountSiteMap = new HashMap<String, List<String>>();

                    for (AppInfo appDownloadInfo : appDownloadInfos) {
                        // update in database
                        db.insertOrUpdateStats(appDownloadInfo);
                        String admobSiteId = Preferences.getAdmobSiteId(Main.this, appDownloadInfo.getPackageName());
                        if(admobSiteId != null) {
                            String admobAccount = Preferences.getAdmobAccount(Main.this, admobSiteId);
                            if(admobAccount != null) {
                                List<String> siteList = admobAccountSiteMap.get(admobAccount);
                                if(siteList == null) {
                                    siteList = new ArrayList<String>();
                                }
                                siteList.add(admobSiteId);
                                admobAccountSiteMap.put(admobAccount, siteList);
                            }
                        }
                    }

                    // sync admob accounts
                    Set<String> admobAccuntKeySet = admobAccountSiteMap.keySet();
                    for (String admobAccount : admobAccuntKeySet) {

                        AdmobRequest.syncSiteStats(admobAccount, Main.this, admobAccountSiteMap.get(admobAccount), null);
                    }

                    new LoadIconInCache().execute(appDownloadInfos);

                } catch (Exception e) {

                    if(e instanceof IOException) {
                        e = new NetworkException(e);
                    }

                    exception = e;

                    Log.e(TAG, "error while requesting developer console", e);
                }

                if (dotracking == true) {
                    int size = 0;
                    if (appDownloadInfos != null) {
                        size = appDownloadInfos.size();
                    }
                    // TODO endless loop in case of exception!!!
                    if (exception == null) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("num", size + "");
                    } else {
                    }
                    dotracking = false;
                }



            return exception;
        }

        /*
         * (non-Javadoc)
         *
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Exception e) {

            hideLoadingIndecator(progressSwitcher);

            buttonRefresh.setEnabled(true);

            if (e != null) {

                if ((e instanceof InvalidJSONResponseException || e instanceof AuthenticationException)
                                && !isAuthenticationRetry) {
                    Log.w("Andlytics", "authentication faild, retry with new token");
                    isAuthenticationRetry = true;
                    authenticateAccountFromPreferences(true, Main.this);


                } else {
                    handleUserVisibleException(e);
                    new LoadDbEntries().execute(false);
                }

            } else {
                new LoadDbEntries().execute(false);

                if(Preferences.getProVersionHint(Main.this) && !isProVersion()) {
                    showProDialog();
                    Preferences.saveProVersionHint(Main.this, false);
                }

            }

        }

        /*
         * (non-Javadoc)
         *
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {

            buttonRefresh.setEnabled(false);

            showLoadingIndecator(progressSwitcher);

        }

    }

    private class LoadDbEntries extends AsyncTask<Boolean, Void, Boolean> {

        private List<AppInfo> allStats = new ArrayList<AppInfo>();
        private List<AppInfo> filteredStats = new ArrayList<AppInfo>();

        private Boolean triggerRemoteCall;

        @Override
        protected Boolean doInBackground(Boolean... params) {

            allStats = db.getAllAppsLatestStats(accountname);

            for (AppInfo appInfo : allStats) {

                if (!appInfo.isGhost()) {
                    String admobSiteId = Preferences.getAdmobSiteId(Main.this, appInfo.getPackageName());
                    if(admobSiteId != null) {
                        List<Admob> admobStats = db.getAdmobStats(admobSiteId, Timeframe.LAST_TWO_DAYS).getAdmobs();
                        if(admobStats.size() > 0) {
                            Admob admob = admobStats.get(admobStats.size() -1);
                            appInfo.setAdmobStats(admob);
                        }
                    }
                    filteredStats.add(appInfo);
                }

            }


            triggerRemoteCall = params[0];

            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            updateMainList(filteredStats);

            if (triggerRemoteCall) {
                authenticateAccountFromPreferences(false, Main.this);

            } else {

                if (allStats.size() == 0) {
                    Toast.makeText(Main.this, "no published apps found", Toast.LENGTH_LONG).show();
                }
            }

            if (ghostDialog != null && ghostDialog.isShowing()) {
                ghostDialog.setAppInfos(allStats);
            }

        }

    }

    private class LoadIconInCache extends AsyncTask<List<AppInfo>, Void, Boolean> {

        @Override
        protected Boolean doInBackground(List<AppInfo>... params) {

            List<AppInfo> appInfos = params[0];

            Boolean success = false;

            for (AppInfo appInfo : appInfos) {

                String iconUrl = appInfo.getIconUrl();

                if (iconUrl != null) {

                    File iconFile = new File(getCacheDir() + "/" + appInfo.getIconName());
                    if (!iconFile.exists()) {

                        try {
                            iconFile.createNewFile();
                            URL url = new URL(iconUrl);
                            HttpURLConnection c = (HttpURLConnection) url.openConnection();
                            c.setRequestMethod("GET");
                            //c.setDoOutput(true);
                            c.connect();

                            FileOutputStream fos = new FileOutputStream(iconFile);

                            InputStream is = c.getInputStream();

                            byte[] buffer = new byte[1024];
                            int len1 = 0;
                            while ((len1 = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, len1);
                            }
                            fos.close();
                            is.close();

                            success = true;
                        } catch (IOException e) {

                            if (iconFile.exists()) {
                                iconFile.delete();
                            }

                            Log.d("log_tag", "Error: " + e);
                        }

                    }
                }

            }

            return success;

        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                adapter.notifyDataSetChanged();
            }
        }

    }

    private class LoadGhostDialog extends AsyncTask<Boolean, Void, Boolean> {

        private List<AppInfo> allStats;

        @Override
        protected Boolean doInBackground(Boolean... params) {

            allStats = db.getAllAppsLatestStats(accountname);

            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (!isFinishing()) {
                ghostDialog = new GhostDialog(Main.this, allStats, Main.this);
                ghostDialog.show();
            }
        }

    }

    private class LoadExportDialog extends AsyncTask<Boolean, Void, Boolean> {

        private List<AppInfo> allStats;

        @Override
        protected Boolean doInBackground(Boolean... params) {

            allStats = db.getAllAppsLatestStats(accountname);

            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (!isFinishing()) {
                exportDialog = new ExportDialog(Main.this, allStats, accountname);
                exportDialog.show();
            }
        }

    }

    private class LoadImportDialog extends AsyncTask<Boolean, Void, Boolean> {

        private List<String> fileNames;

        @Override
        protected Boolean doInBackground(Boolean... params) {

            if (android.os.Environment.getExternalStorageState().equals(
                            android.os.Environment.MEDIA_MOUNTED)) {

                List<AppInfo> allStats = db.getAllAppsLatestStats(accountname);
                try {
                    fileNames = StatsCsvReaderWriter.getImportFileNames(accountname, allStats);
                } catch (ServiceExceptoin e) {
                    e.printStackTrace();
                    return false;
                }
                return true;

            } else {

                return false;
            }

        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (!isFinishing()) {

                if(result) {
                    importDialog = new ImportDialog(Main.this, fileNames, accountname);
                    importDialog.show();
                } else {
                    Toast.makeText(Main.this, "SD-Card not mounted or invalid file format, can't import!", Toast.LENGTH_LONG).show();
                }
            }
        }

    }


    protected void showAutosyncDialog() {

        if(!isFinishing()) {

            AutosyncDialog autosyncDialog = new AutosyncDialog(Main.this, accountname);
            autosyncDialog.show();
        }
    }

    private class LoadNotificationDialog extends AsyncTask<Boolean, Void, Boolean> {

        private List<AppInfo> allStats;

        @Override
        protected Boolean doInBackground(Boolean... params) {

            allStats = db.getAllAppsLatestStats(accountname);

            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (!isFinishing()) {
                notificationDialog = new NotificationsDialog(Main.this, allStats, accountname, db);
                notificationDialog.show();
            }
        }

    }

    @Override
    public void onBackPressed() {
        Preferences.removeAccountName(Main.this);
        super.onBackPressed();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onGhostSelectionChanged(String packageName, boolean isGhost) {

        db.setGhost(accountname, packageName, isGhost);
        (new LoadDbEntries()).execute(false);

    }

    @Override
    public void onGhostDialogClose() {

        (new LoadDbEntries()).execute(false);

    }

    private void updateStatsMode() {
        switch (currentStatsMode) {
        case PERCENT:
            statsModeText.setText(this.getString(R.string.daily));
            statsModeIcon.setImageDrawable(getResources().getDrawable(R.drawable.icon_plusminus));
            break;

        case DAY_CHANGES:
            statsModeText.setText(this.getString(R.string.percentage));
            statsModeIcon.setImageDrawable(getResources().getDrawable(R.drawable.icon_percent));
            break;

        default:
            break;
        }

        adapter.setStatsMode(currentStatsMode);
        adapter.notifyDataSetChanged();
        Preferences.saveStatsMode(currentStatsMode, Main.this);
    }
    /*
    @SuppressWarnings("unchecked")
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_I) {
            Intent intent = new Intent(this, DemoDataActivity.class);
            intent.putExtra(Constants.AUTH_ACCOUNT_NAME, accountname);
            startActivity(intent);
            return true;
        } else if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Preferences.removeAccountName(Main.this);
            Intent intent = new Intent(Main.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_D) {

            try {
                //List<AppInfo> allAppsLatestStats = db.getAllAppsLatestStats(accountname);
                //for (AppInfo appInfo : allAppsLatestStats) {
                    //db.deleteAllForPackageName(appInfo.getPackageName());
                //}
            } catch (Exception e) {
                showCrashDialog(e);
            }
            return true;
        } else {

            try {
                Integer.parseInt(event.getNumber() + "");

                DeveloperConsole console = new DeveloperConsole(Main.this);
                List<AppInfo> appDownloadInfos;
                try {
                    appDownloadInfos = console.parseAppStatisticsResponse(DemoDataActivity.readTestData(event.getNumber()),
                            accountname);

                    for (AppInfo appDownloadInfo : appDownloadInfos) {
                        // update in database

                        db.insertOrUpdateStats(appDownloadInfo);

                        new LoadIconInCache().execute(appDownloadInfos);

                    }
                } catch (AuthenticationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (DeveloperConsoleException e) {
                e.printStackTrace();
            } catch (InvalidJSONResponseException e) {
                e.printStackTrace();
            }

        }
        return false;
    }*/

    @Override
    protected Dialog onCreateDialog(int id) {

        Dialog dialog = null;

        switch (id) {
        case FEEDBACK_DIALOG:
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AndlyticsProject/andlytics/issues")));
/*
            FeedbackDialog.FeedbackDialogBuilder builder = new FeedbackDialogBuilder(Main.this);
            builder.setTitle(this.getString(R.string.feedback));

            builder.setMessage(this.getString(R.string.help_us));
            builder.setPositiveButton(this.getString(R.string.send), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }

            });
            builder.setNegativeButton(this.getString(R.string.cancel), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }

            });

            dialog = builder.create(accountname + "\n\n", getApplication());
*/
            break;

        default:
            break;
        }

        return dialog;
    }

    @Override
    public void authenticationSuccess() {
        new LoadRemoteEntries().execute();
    }




}
