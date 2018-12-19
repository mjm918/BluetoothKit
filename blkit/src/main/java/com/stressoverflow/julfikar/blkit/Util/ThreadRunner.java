package com.stressoverflow.julfikar.blkit.Util;

import android.app.Activity;

public class ThreadRunner {

    public static void handle(boolean isOnUi, Activity activity, Runnable runnable){
        if(isOnUi) {
            activity.runOnUiThread(runnable);
        }
        else{
            runnable.run();
        }
    }

}
