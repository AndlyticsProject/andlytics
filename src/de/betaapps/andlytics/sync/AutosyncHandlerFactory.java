package de.betaapps.andlytics.sync;

import android.content.Context;

public class AutosyncHandlerFactory {

    public static AutosyncHandler getInstance(Context context) {
        AutosyncHandler autosyncHandler = null;
        if(isFroyoOrAbove()) {
            autosyncHandler = new AutosyncHandlerLevel8();
        } else {
            autosyncHandler = new AutosyncHandlerLevel7(context);
        }
        return autosyncHandler;
    }
    
    private static boolean isFroyoOrAbove() {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        return currentapiVersion >= android.os.Build.VERSION_CODES.FROYO;
    }

    
}
