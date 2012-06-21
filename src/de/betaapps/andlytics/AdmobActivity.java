package de.betaapps.andlytics;

import de.betaapps.andlytics.Preferences.Timeframe;
import de.betaapps.andlytics.admob.AdmobRequest;
import de.betaapps.andlytics.admob.AdmobRequest.SyncCallback;
import de.betaapps.andlytics.chart.Chart;
import de.betaapps.andlytics.chart.ChartTextSwitcher;
import de.betaapps.andlytics.chart.Chart.AdmobChartType;
import de.betaapps.andlytics.exception.NetworkException;
import de.betaapps.andlytics.model.Admob;
import de.betaapps.andlytics.model.AdmobList;
import de.betaapps.andlytics.view.ChartGallery;
import de.betaapps.andlytics.view.ChartGalleryAdapter;
import de.betaapps.andlytics.view.ViewSwitcher3D;
import de.betaapps.andlytics.view.ViewSwitcher3D.ViewSwitcherListener;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ViewSwitcher.ViewFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdmobActivity extends BaseActivity implements ViewSwitcherListener {

    private NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);

    public static final String TAG = AdmobActivity.class.getSimpleName();

	protected ContentAdapter db;
	private ChartGallery chartGallery;
	private AdmobChartType currentChart;
	private ListView historyList;
	private AdmobListAdapter admobListAdapter;
	private TextView timeframeText;
	private ChartTextSwitcher titleTextSwitcher;
	private int currentChartPosition;
	private Animation inNegative;
	private Animation outNegative;
	private Animation inPositive;
	private Animation outPositive;
	public String timetext;
	public Integer heighestRatingChange;
	public Integer lowestRatingChange;
	private ViewSwitcher3D mainViewSwitcher;
	private ViewSwitcher3D smallConfigViewSwitcher;
	private ChartGalleryAdapter chartGalleryAdapter;
    private ViewGroup accountList;

    private View addAccountButton;

    private ViewSwitcher configSwitcher;

    protected String admobToken;

    private ViewGroup siteList;

    private ViewSwitcher toolbarViewSwitcher;

    private Timeframe currentTimeFrame;

    public Admob overallStats;

    protected String subHeadlineText;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.admob);
		currentTimeFrame = Preferences.getAdmobTimeframe(this);

		currentChartPosition = -1;

		toolbarViewSwitcher = (ViewSwitcher) findViewById(R.id.admob_toobar_switcher);


        titleTextSwitcher = (ChartTextSwitcher) findViewById(R.id.admob_admob_type);
        titleTextSwitcher.setFactory(new ViewFactory() {

			public View makeView() {

				return getLayoutInflater().inflate(R.layout.admob_headline, null);
			}
		});
		inNegative = AnimationUtils.loadAnimation(AdmobActivity.this, R.anim.slide_in_right);
		outNegative = AnimationUtils.loadAnimation(AdmobActivity.this, R.anim.slide_out_left);
		inPositive = AnimationUtils.loadAnimation(AdmobActivity.this, R.anim.slide_in_left);
		outPositive = AnimationUtils.loadAnimation(AdmobActivity.this, R.anim.slide_out_right);

		db = getDbAdapter();
		//chartFrame = (ViewSwitcher) ;

		View backButton = findViewById(R.id.admob_button_back);
		backButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(Main.class, false, true);
				overridePendingTransition(R.anim.activity_prev_in, R.anim.activity_prev_out);
			}
		});

		View refreshButton = findViewById(R.id.admob_button_refresh);
		if(refreshButton != null) {
		    refreshButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    chartGallery.setIgnoreLayoutCalls(true);
                    new LoadRemoteEntiesTask().execute();

                }
            });
		}


		View configButton = findViewById(R.id.admob_button_config);

		mainViewSwitcher = new ViewSwitcher3D((ViewGroup) findViewById(R.id.admob_main_frame));
		mainViewSwitcher.setListener(this);

		if(configButton != null) {


			smallConfigViewSwitcher = new ViewSwitcher3D((ViewGroup) findViewById(R.id.admob_bottom_frame));
			smallConfigViewSwitcher.setListener(this);

			configButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					chartGallery.setIgnoreLayoutCalls(true);

					String admobSiteId = Preferences.getAdmobSiteId(AdmobActivity.this, packageName);

					if(admobSiteId == null) {

					    View currentView = configSwitcher.getCurrentView();
					    if(currentView.getId() != R.id.admob_config) {
					        configSwitcher.showPrevious();
					    }
					    mainViewSwitcher.swap();
					    showAccountList();
					} else {
					    smallConfigViewSwitcher.swap();
					}

				}
			});

            RadioButton radioLastSeven = (RadioButton) findViewById(R.id.admob_config3_ratio_last_seven_days);
            if(Timeframe.LAST_SEVEN_DAYS.equals(currentTimeFrame)) {
                radioLastSeven.setChecked(true);
            }
            radioLastSeven.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        currentTimeFrame = Timeframe.LAST_SEVEN_DAYS;
                        new LoadDbEntiesTask().execute(false);
                        Preferences.saveAdmobTimeframe(Timeframe.LAST_SEVEN_DAYS, AdmobActivity.this);
                    }
                }
            });


            RadioButton radioLastThrity = (RadioButton) findViewById(R.id.admob_config3_ratio_last_thrity_days);
            if(Timeframe.LAST_THIRTY_DAYS.equals(currentTimeFrame)) {
                radioLastThrity.setChecked(true);
            }
            radioLastThrity.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        currentTimeFrame = Timeframe.LAST_THIRTY_DAYS;
                        new LoadDbEntiesTask().execute(false);
                        Preferences.saveAdmobTimeframe(Timeframe.LAST_THIRTY_DAYS, AdmobActivity.this);
                    }
                }
            });
            RadioButton radioUnlimited = (RadioButton) findViewById(R.id.admob_config3_ratio_last_unlimited);
            if(Timeframe.UNLIMITED.equals(currentTimeFrame)) {
                radioUnlimited.setChecked(true);
            }
            radioUnlimited.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        currentTimeFrame = Timeframe.UNLIMITED;
                        new LoadDbEntiesTask().execute(false);
                        Preferences.saveAdmobTimeframe(Timeframe.UNLIMITED, AdmobActivity.this);
                    }
                }
            });

            String dateFormatLong = Preferences.getDateFormatLong(this);
            RadioButton radioDmy = (RadioButton) findViewById(R.id.admob_config3_ratio_dmy);
            if("dd/MM/yyyy".equals(dateFormatLong)) {
                radioDmy.setChecked(true);
            }
            radioDmy.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        Preferences.saveDateFormatLong(AdmobActivity.this, "dd/MM/yyyy");
                        Preferences.saveDateFormatShort(AdmobActivity.this, "dd/MM");
                        new LoadDbEntiesTask().execute(false);
                    }
                }
            });

            RadioButton radioYmd = (RadioButton) findViewById(R.id.admob_config3_ratio_ymd);
            if("yyyy/MM/dd".equals(dateFormatLong)) {
                radioYmd.setChecked(true);
            }
            radioYmd.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        Preferences.saveDateFormatLong(AdmobActivity.this, "yyyy/MM/dd");
                        Preferences.saveDateFormatShort(AdmobActivity.this, "MM/dd");
                        new LoadDbEntiesTask().execute(false);
                    }
                }
            });
            RadioButton radioMdy = (RadioButton) findViewById(R.id.admob_config3_ratio_mdy);
            if("MM/dd/yyyy".equals(dateFormatLong)) {
                radioMdy.setChecked(true);
            }
            radioMdy.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        Preferences.saveDateFormatLong(AdmobActivity.this, "MM/dd/yyyy");
                        Preferences.saveDateFormatShort(AdmobActivity.this, "MM/dd");
                        new LoadDbEntiesTask().execute(false);
                    }
                }
            });

            View configDoneButton = (View) findViewById(R.id.admob_config3_done_button);
            configDoneButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    smallConfigViewSwitcher.swap();
                }
            });


            View removeButton = (View) findViewById(R.id.admob_config3_remove_button);
            removeButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Preferences.saveAdmobSiteId(AdmobActivity.this, packageName, null);
                    showAccountList();
                    if(configSwitcher.getCurrentView().getId() != R.id.admob_config) {
                        configSwitcher.showPrevious();
                    }
                    mainViewSwitcher.swap();
                }
            });

		}

		configSwitcher = (ViewSwitcher) findViewById(R.id.admob_viewswitcher_config);
		configSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
        configSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_left));

		chartGallery = (ChartGallery) findViewById(R.id.admob_gallery);
		chartGalleryAdapter = new ChartGalleryAdapter(new ArrayList<View>());
		chartGallery.setAdapter(chartGalleryAdapter);
		chartGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {


				chartGallery.setIgnoreLayoutCalls(true);

				if(currentChartPosition < position) {
					titleTextSwitcher.setInAnimation(inNegative);
					titleTextSwitcher.setOutAnimation(outNegative);
				} else {
					titleTextSwitcher.setInAnimation(inPositive);
					titleTextSwitcher.setOutAnimation(outPositive);
				}
				currentChartPosition = position;

				subHeadlineText = "";

				Object tag = view.getTag();
				if (tag != null) {

				    currentChart = (AdmobChartType) tag;
				    updateChartHeadline();


					admobListAdapter.setCurrentChart(currentChart);
					admobListAdapter.notifyDataSetChanged();
				}


			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});


		accountList = (ViewGroup) findViewById(R.id.admob_accountlist);
        siteList = (ViewGroup) findViewById(R.id.admob_sitelist);

		addAccountButton = (View) findViewById(R.id.admob_addaccount_button);
		addAccountButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                addNewAdmobAccount();
            }
        });


		timeframeText = (TextView) findViewById(R.id.admob_timeframe);

		historyList = (ListView) findViewById(R.id.admob_list);

		admobListAdapter = new AdmobListAdapter(this);

        List<AdmobChartType> secondPageChats = new ArrayList<AdmobChartType>();
        secondPageChats.add(AdmobChartType.HOUSEAD_CLICKS);
        secondPageChats.add(AdmobChartType.CTR);
        secondPageChats.add(AdmobChartType.ECPM);
        secondPageChats.add(AdmobChartType.IMPRESSIONS);

        admobListAdapter.setSecondPageCharts(secondPageChats);

		historyList.setAdapter(admobListAdapter);

		if(iconFilePath != null) {

			ImageView appIcon = (ImageView) findViewById(R.id.admob_app_icon);
			Bitmap bm = BitmapFactory.decodeFile(iconFilePath);
			appIcon.setImageBitmap(bm);
		}

		currentChart = AdmobChartType.REVENUE;

		String currentAdmobAccount  = null;
	    String currentSiteId = Preferences.getAdmobSiteId(AdmobActivity.this, packageName);
        if(currentSiteId != null) {
            currentAdmobAccount = Preferences.getAdmobAccount(this, currentSiteId);
        }

		if(currentAdmobAccount == null) {
		    mainViewSwitcher.swap();
            if(configSwitcher.getCurrentView().getId() != R.id.admob_config) {
                configSwitcher.showPrevious();
            }
            showAccountList();
		} else {
		    new LoadDbEntiesTask().execute(false);
		}



	}

    protected void updateChartHeadline() {

        subHeadlineText = "";

        switch (currentChart) {

        case REVENUE:
            updateTitleTextSwitcher(this.getString(R.string.admob__revenue));
            if(overallStats != null)
                subHeadlineText = numberFormat.format(overallStats.getRevenue());
            break;

          case EPC:
            updateTitleTextSwitcher(this.getString(R.string.admob__epc));
            if(overallStats != null)
              subHeadlineText = overallStats.getEpcCents();
            break;
            
        case REQUESTS:
            Preferences.saveShowChartHint(AdmobActivity.this, false);
            updateTitleTextSwitcher(this.getString(R.string.admob__requests));
            if(overallStats != null)
                subHeadlineText = overallStats.getRequests() + "";
            break;

        case CLICKS:
            updateTitleTextSwitcher(this.getString(R.string.admob__clicks));
            if(overallStats != null)
                subHeadlineText = overallStats.getClicks() + "";
            break;

        case FILL_RATE:
            updateTitleTextSwitcher(this.getString(R.string.admob__fill_rate));
            if(overallStats != null)
                subHeadlineText = (new BigDecimal(overallStats.getFillRate() * 100)).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "%";

            break;

        case ECPM:
            updateTitleTextSwitcher(this.getString(R.string.admob__eCPM));
            if(overallStats != null)
                subHeadlineText = numberFormat.format(overallStats.getEcpm());
            break;

        case IMPRESSIONS:
            updateTitleTextSwitcher(this.getString(R.string.admob__impressions));
            if(overallStats != null)
                subHeadlineText = overallStats.getImpressions() + "";
            break;

        case CTR:
            updateTitleTextSwitcher(this.getString(R.string.admob__CTR));
            if(overallStats != null)
                subHeadlineText = (new BigDecimal(overallStats.getCtr() * 100)).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "%";
            break;

        case HOUSEAD_CLICKS:
            updateTitleTextSwitcher(this.getString(R.string.admob__house_ad_clicks));
            if(overallStats != null)
                subHeadlineText = overallStats.getHouseAdClicks() + "";
            break;

        default:
            break;
        }

        if(Preferences.getShowChartHint(AdmobActivity.this)) {
            timeframeText.setText(Html.fromHtml("8 " + this.getString(R.string.admob__charts_available)+ " ->"));
        } else {
            if(timetext != null) {
                timeframeText.setText(Html.fromHtml(timetext + ": <b>" + subHeadlineText + "</b>"));
            }
        }


    }


    private void updateTitleTextSwitcher(String string) {

        Object tag = titleTextSwitcher.getTag();

        if(tag != null && tag.equals(string)) {
            return;
        } else {
            titleTextSwitcher.setTag(string);
            titleTextSwitcher.setText(string, null);
        }

    }

    protected void showAccountList() {

        final AccountManager manager = AccountManager.get(this);
        final Account[] accounts = manager.getAccountsByType(Constants.ACCOUNT_TYPE_ADMOB);
        final int size = accounts.length;
        String[] names = new String[size];
        accountList.removeAllViews();
        for (int i = 0; i < size; i++) {
            names[i] = accounts[i].name;

            View inflate = getLayoutInflater().inflate(R.layout.login_list_item, null);
            TextView accountName = (TextView) inflate.findViewById(R.id.login_list_item_text);
            accountName.setText(accounts[i].name);
            inflate.setTag(accounts[i].name);
            inflate.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View view) {

                    String currentAdmobAccount = (String) view.getTag();


                    configSwitcher.showNext();
                    new LoadRemoteSiteListTask(currentAdmobAccount).execute();

                }
            });
            accountList.addView(inflate);
        }
    }


    private void addNewAdmobAccount() {

        AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bundle = future.getResult();
                    bundle.keySet();
                    Log.d(TAG, "account added: " + bundle);

                    showAccountList();

                } catch (OperationCanceledException e) {
                    Log.d(TAG, "addAccount was canceled");
                } catch (IOException e) {
                    Log.d(TAG, "addAccount failed: " + e);
                } catch (AuthenticatorException e) {
                    Log.d(TAG, "addAccount failed: " + e);
                }
                // gotAccount(false);
            }
        };

        AccountManager.get(AdmobActivity.this).addAccount(Constants.ACCOUNT_TYPE_ADMOB, Constants.AUTHTOKEN_TYPE_ADMOB,
                        null, null /* options */, AdmobActivity.this, callback, null /* handler */);
    }

	@Override
	protected void onResume() {
		super.onResume();
		chartGallery.setIgnoreLayoutCalls(false);

	}

    private class LoadDbEntiesTask extends AsyncTask<Boolean, Void, Exception> {


        private List<Admob> admobStats;
        private Boolean executeRemoteCall = false;

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Exception doInBackground(Boolean... params) {


            String currentSiteId = Preferences.getAdmobSiteId(AdmobActivity.this, packageName);
            AdmobList admobList = db.getAdmobStats(currentSiteId, currentTimeFrame);
            admobStats = admobList.getAdmobs();
            overallStats = admobList.getOverallStats();
            executeRemoteCall = params[0];
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {

            loadChartData(admobStats);
            Collections.reverse(admobStats);

            admobListAdapter.setStats(admobStats);
            admobListAdapter.setCurrentChart(currentChart);
            admobListAdapter.notifyDataSetChanged();

            if(executeRemoteCall) {

                new LoadRemoteEntiesTask().execute();
            }

        }
    };

    private class LoadRemoteEntiesTask extends AsyncTask<Void, Void, Exception> {

        boolean isRunning;

        @Override
        protected void onPreExecute() {

            showLoadingIndecator(toolbarViewSwitcher);
            isRunning = true;

        }

        @Override
        protected Exception doInBackground(Void... lastValueDate) {

            isRunning = true;

            String currentAdmobAccount  = null;
            String currentSiteId = Preferences.getAdmobSiteId(AdmobActivity.this, packageName);
            if(currentSiteId != null) {
                currentAdmobAccount = Preferences.getAdmobAccount(AdmobActivity.this, currentSiteId);
            }


            try {

                List<String> siteList = new ArrayList<String>();
                siteList.add(currentSiteId);

                AdmobRequest.syncSiteStats(currentAdmobAccount, AdmobActivity.this, siteList, new SyncCallback() {

                    @Override
                    public void initialImportStarted() {
                        publishProgress();
                    }
                });

            } catch (Exception e) {

                if(e instanceof IOException) {
                    e = new NetworkException(e);
                }

                return e;
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            Toast.makeText(AdmobActivity.this, "Initial AdMob import, this may take a while...", Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(Exception result) {

            if(result != null) {
                Log.e(TAG, "admob exception", result);
                handleUserVisibleException(result);
            } else {

                new LoadDbEntiesTask().execute(false);

            }

            if(isRunning)
                hideLoadingIndecator(toolbarViewSwitcher);
        }
    };

    private class LoadRemoteSiteListTask extends AsyncTask<Void, Void, Exception> {

        private Map<String, String> data;
        private String currentAdmobAccount;

        public LoadRemoteSiteListTask(String currentAdmobAccount) {
            this.currentAdmobAccount = currentAdmobAccount;
        }

        @Override
        protected void onPreExecute() {
            showLoadingIndecator(toolbarViewSwitcher);
        }

        @Override
        protected Exception doInBackground(Void... params) {

            try {
                data = AdmobRequest.getSiteList(currentAdmobAccount, AdmobActivity.this);
            } catch (Exception e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {

            if(result != null) {
                handleUserVisibleException(result);
            } else {

                if(data.size() > 0) {

                    siteList.removeAllViews();

                    Set<String> keySet = data.keySet();
                    for (String siteId : keySet) {

                        String siteName = data.get(siteId);

                        //pull the id from the data
                        View inflate = getLayoutInflater().inflate(R.layout.admob_account_list_item, null);
                        TextView accountName = (TextView) inflate.findViewById(R.id.admob_account_list_item_text);
                        accountName.setText(siteName);
                        inflate.setTag(siteId);
                        inflate.setOnClickListener(new OnClickListener() {


                            @Override
                            public void onClick(View view) {

                                Preferences.saveAdmobSiteId(AdmobActivity.this, packageName, (String) view.getTag());
                                Preferences.saveAdmobAccount(AdmobActivity.this, (String) view.getTag(), currentAdmobAccount);
                                mainViewSwitcher.swap();
                                new LoadDbEntiesTask().execute(true);
                            }
                        });
                        siteList.addView(inflate);


                    }
                }
            }

            hideLoadingIndecator(toolbarViewSwitcher);
        }
    };


	private void loadChartData(List<Admob> statsForApp) {
           /* if(radioLastThrity != null) {
                radioLastThrity.setEnabled(false);
                radioUnlimited.setEnabled(false);
                checkSmooth.setEnabled(false);
            }*/

            if(statsForApp != null && statsForApp.size() > 0) {
                Chart chart = new Chart();

                List<View> charts = new ArrayList<View>();

                AdmobChartType[] chartTypes = AdmobChartType.values();
                for (int i = 0; i < chartTypes.length; i++) {
                    View chartView = chart.buildAdmobChart(AdmobActivity.this, statsForApp, chartTypes[i]);
                    Gallery.LayoutParams params = new Gallery.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
                    chartView.setLayoutParams(params);
                    chartView.setTag(chartTypes[i]);
                    charts.add(chartView);
                }



                chartGallery.setIgnoreLayoutCalls(false);
                chartGalleryAdapter.setViews(charts);
                chartGalleryAdapter.notifyDataSetChanged();
                chartGallery.invalidate();

                SimpleDateFormat dateFormat = new SimpleDateFormat(Preferences.getDateFormatLong(AdmobActivity.this));
                if(statsForApp.size() > 0) {

                    timetext = dateFormat.format(statsForApp.get(0).getDate()) + " - " + dateFormat.format(statsForApp.get(statsForApp.size() -1).getDate());
                    updateChartHeadline();
                }


                //chartFrame.showNext();

            }
/*            if(radioLastThrity != null) {
                radioLastThrity.setEnabled(true);
                radioUnlimited.setEnabled(true);
                checkSmooth.setEnabled(true);

            }*/



	}


	@Override
	public void onViewChanged(boolean frontsideVisible) {
		chartGallery.setIgnoreLayoutCalls(true);

	}


	@Override
	public void onRender() {
		chartGallery.invalidate();

	}

}
