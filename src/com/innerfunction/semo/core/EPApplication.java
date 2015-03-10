package com.innerfunction.semo.core;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

public class EPApplication extends Application {

    static final String Tag = EPApplication.class.getSimpleName();
    
    public String configurationPath = "app:/common/configuration.json";
    
    public EPApplication(String configurationPath) {
        this.configurationPath = configurationPath;
    }
    
    public void onCreate() {
        super.onCreate();
        try {
            Core.setupWithConfiguration(configurationPath, this.getApplicationContext()).startService();
            
            // Taken from https://developer.chrome.com/devtools/docs/remote-debugging#debugging-webviews
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ) {
                if( 0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE) ) {
                    WebView.setWebContentsDebuggingEnabled( true );
                }
            }
        }
        catch(Exception e) {
            Log.e(Tag,"Error starting application", e );
        }
    }

}
