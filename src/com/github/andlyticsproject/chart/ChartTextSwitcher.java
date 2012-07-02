package com.github.andlyticsproject.chart;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class ChartTextSwitcher extends ViewSwitcher {

	    /**
	     * Creates a new empty TextSwitcher.
	     *
	     * @param context the application's environment
	     */
	    public ChartTextSwitcher(Context context) {
	        super(context);
	    }

	    /**
	     * Creates a new empty TextSwitcher for the given context and with the
	     * specified set attributes.
	     *
	     * @param context the application environment
	     * @param attrs a collection of attributes
	     */
	    public ChartTextSwitcher(Context context, AttributeSet attrs) {
	        super(context, attrs);
	    }

	    /**
	     * {@inheritDoc}
	     *
	     * @throws IllegalArgumentException if child is not an instance of
	     *         {@link android.widget.TextView}
	     */
	    @Override
	    public void addView(View child, int index, ViewGroup.LayoutParams params) {
	        if (!(child instanceof RelativeLayout)) {
	            throw new IllegalArgumentException(
	                    "ChartRatingSwitcher children must be instances of RelativeLayout");
	        }

	        super.addView(child, index, params);
	    }

	    /**
	     * Sets the text of the next view and switches to the next view. This can
	     * be used to animate the old text out and animate the next text in.
	     *
	     * @param text the new text to display
	     */
	    public void setText(CharSequence text, Drawable image) {
	        RelativeLayout r = (RelativeLayout) getNextView();
	        r = (RelativeLayout) r.getChildAt(0);
	        ImageView i = (ImageView) r.getChildAt(0);
	        if(image == null) {
	        	i.setVisibility(View.GONE);
	        } else {
	        	i.setVisibility(View.VISIBLE);
	        	i.setImageDrawable(image);
	        }
	        TextView t = (TextView) r.getChildAt(1);
	        t.setText(text);
	        showNext();
	    }

	    /**
	     * Sets the text of the text view that is currently showing.  This does
	     * not perform the animations.
	     *
	     * @param text the new text to display
	     */
	    public void setCurrentText(CharSequence text, Drawable image) {
	        RelativeLayout r = (RelativeLayout) getCurrentView();
	        r = (RelativeLayout) r.getChildAt(0);
	        ImageView i = (ImageView) r.getChildAt(0);
	        if(image == null) {
	        	i.setVisibility(View.GONE);
	        } else {
	        	i.setVisibility(View.VISIBLE);
	        	i.setImageDrawable(image);
	        }

	        TextView t = (TextView) r.getChildAt(1);
	        t.setText(text);	    }
	}
