package com.github.andlyticsproject;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ViewSwitcher.ViewFactory;

import com.github.andlyticsproject.chart.Chart;
import com.github.andlyticsproject.chart.ChartTextSwitcher;
import com.github.andlyticsproject.view.ChartGallery;
import com.github.andlyticsproject.view.ChartGalleryAdapter;
import com.github.andlyticsproject.view.ViewSwitcher3D.ViewSwitcherListener;

public abstract class BaseChartActivity extends BaseActivity implements ViewSwitcherListener {
	private static String LOG_TAG=BaseChartActivity.class.toString();
  private ChartTextSwitcher titleTextSwitcher;
  private Animation inNegative;
  private Animation outNegative;
  private Animation inPositive;
  private Animation outPositive;
  
  private ChartGalleryAdapter chartGalleryAdapter;
  private ChartGallery chartGallery;
  private int currentChartPosition;
  private ListView dataList;
  private TextView timeframeText;
  protected String timetext;

  BaseChartListAdapter myAdapter;  
  @Override
  final protected void onCreate(Bundle savedInstanceState)
  {
    // TODO Auto-generated method stub
    super.onCreate(savedInstanceState);
    setContentView(getLayoutId());
    currentChartPosition = -1;
    
    titleTextSwitcher = getTitleTextSwitcher();
    titleTextSwitcher.setFactory(new ViewFactory() {
      
      public View makeView()
      {
        
        return getLayoutInflater().inflate(R.layout.base_chart_headline, null); 
      }
    });
    inNegative = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
    outNegative = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
    inPositive = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
    outPositive = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);    
    
    
    chartGallery = getChartGallery();
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

        if (view.getTag() != null) {
        	int pageColumn[]=(int[]) view.getTag();
        	myAdapter.setCurrentChart(pageColumn[0], pageColumn[1]);
         updateChartHeadline();

         myAdapter.notifyDataSetChanged();

       }
        
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    
    dataList=getDataListView();
    timeframeText=getTimeframeText();
    onCreateChild(savedInstanceState);
    
  }
  protected final void setAdapter(BaseChartListAdapter adapter)
  {
    myAdapter=adapter;
    dataList.setAdapter(adapter);
  }
  private final void updateTitleTextSwitcher(String string,Drawable image) {

    /*
     This commented code does not work for ranking
      Object tag = titleTextSwitcher.getTag();

    if(tag != null && tag.equals(string)) {
        return;
    } else*/ 
    
    {
        titleTextSwitcher.setTag(string);
        titleTextSwitcher.setText(string, image);
    }

}
  @Override
  protected void onResume()
  {
    super.onResume();
    chartGallery.setIgnoreLayoutCalls(false);
  }
  @Override
  public void onViewChanged(boolean frontsideVisible) {
    chartGallery.setIgnoreLayoutCalls(true);

  }


  @Override
  public void onRender() {
    chartGallery.invalidate();

  }
  protected final void setChartIgnoreCallLayouts(boolean ignoreLayoutCalls)
  {
    chartGallery.setIgnoreLayoutCalls(ignoreLayoutCalls);
  }
  
  
  public void updateCharts(List<?> statsForApp)
  {
    Chart chart = new Chart();
		int page=myAdapter.getCurrentPage();
		int column=myAdapter.getCurrentColumn();

		int position=-1;
    List<View> charts = new ArrayList<View>();

    int pos=0;
    for(int i=0;i<myAdapter.getNumPages();i++)
      for(int j=1;j<myAdapter.getNumCharts(i);j++){
      	Log.i(LOG_TAG,"Updating chart p="+i+" c="+j+"for class="+this.getClass().toString());
      	int pageColumn[]=new int[2];
        View chartView = myAdapter.buildChart(this, chart,statsForApp, i, j);
        /*if(chartView==null)
        {
        	Log.i(LOG_TAG,"Ignoring chart p="+i+" c="+j+"for class="+this.getClass().toString());
        	continue;
        }*/
        Gallery.LayoutParams params = new Gallery.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        chartView.setLayoutParams(params);
        pageColumn[0]=i;
        pageColumn[1]=j;
        if(i==page&&j==column)
        	position=pos;
        pos++;
        chartView.setTag(pageColumn);
        charts.add(chartView);
    }
    chartGallery.setIgnoreLayoutCalls(false);
    chartGalleryAdapter.setViews(charts);
    if(position>=0)chartGallery.setSelection(position);
    Log.i(LOG_TAG,"Psition in updae="+position);
    chartGalleryAdapter.notifyDataSetChanged();
    chartGallery.invalidate();
  }

  protected final void updateChartHeadline() {

    String subHeadlineText = "";
    String title=myAdapter.getCurrentChartTitle();
    Drawable image=myAdapter.getCurrentChartTitleDrawable();
    String ret=myAdapter.getCurrentSubHeadLine();
    if(ret!=null)subHeadlineText=ret;

    updateTitleTextSwitcher(title,image);
    
    if(Preferences.getShowChartHint(this)) {
          timeframeText.setText(Html.fromHtml(getChartHint()));
    } else {
        if(timetext != null) {
            timeframeText.setText(Html.fromHtml(timetext + ": <b>" + subHeadlineText + "</b>"));
        }
    }


  }
protected abstract String getChartHint();

  
  protected abstract ListView getDataListView();
  protected abstract ChartGallery getChartGallery();
  
  protected abstract  ChartTextSwitcher getTitleTextSwitcher();
  protected abstract  TextView getTimeframeText();

  /**
   * TODO Implement in best way
   * onCreate in inherited classes
   */
  protected abstract void onCreateChild(Bundle savedInstanceState);
  /**
   * TODO Implement in best way
   * @return Layout id that will be used
   */
  protected abstract int getLayoutId();
  
}
