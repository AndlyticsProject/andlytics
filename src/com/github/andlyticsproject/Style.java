package com.github.andlyticsproject;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.widget.TextView;

public class Style {

    private static Style instance;
    private static Typeface tf;
    
    private Style(AssetManager mgr){
        tf = Typeface.createFromAsset(mgr, "fonts/DroidSansBold.ttf");
    }
    
    public static Style getInstance(AssetManager mgr) {
        if(instance == null) {
            instance = new Style(mgr);
        }
        return instance;
    }
    
    public void styleHeadline(final TextView headline) {
        //headline.setTypeface(tf);
    }
}
