package com.innerfunction.util;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;

/**
 * Utility class for executing tasks on a background thread.
 * @author juliangoacher
 *
 */
public class BackgroundTaskRunner {
    
    @SuppressLint("NewApi")
    public static void run(final Runnable task) {
        AsyncTask<Void,Void,Void> atask = new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... arg0) {
                task.run();
                return null;
            }
        };
        // Taken from http://stackoverflow.com/a/11977186
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
            atask.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null );
        }
        else {
            atask.execute();
        }
    }
}
