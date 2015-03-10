package com.innerfunction.push;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.eventpac.core.Core;
import com.eventpac.core.EPEvent;
import com.eventpac.core.EPEventHandler;
import com.eventpac.core.Service;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.innerfunction.semo.core.Configuration;
import com.innerfunction.util.ISO8601;
import com.innerfunction.util.ImageUtil;
import com.innerfunction.util.StringTemplate;

@SuppressLint("NewApi")
public class NotificationService implements Service {

    static final String Tag = NotificationService.class.getSimpleName();
    
    static final int FavouriteNotificationID = 1;
    
    private Map<String,PendingIntent> favouriteNotificationsByPostID = new HashMap<String,PendingIntent>();
    private Configuration configuration;
    private DateFormat dateFormatter;
    private boolean pushNotificationsEnabled;
    private String pushSenderID;
    private int notificationSmallIcon;
    private int notificationLargeIcon;
    /** The Android context. */
    private Context context;
    /** The notification alarm manager. */
    private AlarmManager alarmManager;
    /** Local storage. */
    private SharedPreferences preferences;

    public NotificationService() {}
    
    @Override
    public void configure(Configuration configuration) {
        this.configuration = configuration;
        pushNotificationsEnabled = configuration.getValueAsBoolean("pushNotificationsEnabled", false );
        pushSenderID = configuration.getValueAsString("and.pushSenderID");
    }

    @Override
    public void startService() {
        Core core = Core.getCore();
        preferences = core.getLocalStorage();
        context = core.getAndroidContext();
        alarmManager = (AlarmManager)context.getSystemService( Context.ALARM_SERVICE );
        Controller mvc = (Controller)core.getService("mvc");
        
        String imageName = configuration.getValueAsString("and:notificationSmallIcon","ic_launcher");
        if( imageName != null ) {
            notificationSmallIcon = ImageUtil.imageNameToResourceID( imageName, context );
            if( notificationSmallIcon == 0 ) {
                Log.w(Tag,"Notification small icon not found - notifications will not appear!");
            }
        }
        imageName = configuration.getValueAsString("and:notificationLargeIcon");
        if( imageName != null ) {
            notificationLargeIcon = ImageUtil.imageNameToResourceID( imageName, context );
        }
        registerForPushNotifications();
    }

    @Override
    public void stopService() {
    }
    
    private void registerForPushNotifications() {
        if( pushNotificationsEnabled ) {
            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable( context );
            if( resultCode == ConnectionResult.SUCCESS ) {
                try {
                    // Check for stored registration ID
                    String regID = preferences.getString("push.registrationID", null );
                    int regVersion = preferences.getInt("push.registrationVersion", -1 );
                    // Get app's current version number.
                    PackageInfo pinfo = context.getPackageManager().getPackageInfo( context.getPackageName(), 0 );
                    final int currVersion = pinfo.versionCode;
                    // Registration needed if no reg ID found, or app version has changed.
                    boolean registrationNeeded = regID == null || regVersion != currVersion;
                    if( registrationNeeded ) {
                        // Register app in background.
                        AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {
                            @Override
                            protected Void doInBackground(Void... arg0) {
                                String regID;
                                try {
                                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance( context );
                                    regID = gcm.register( pushSenderID );
                                    // TODO: Send registration ID to backend.

                                    // Store the registration ID.
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString("push.registrationID", regID );
                                    editor.putInt("push.registrationVersion", currVersion );
                                    editor.commit();
                                }
                                catch(IOException e) {
                                    Log.e(Tag,"Registering for push notifications", e );
                                }
                                return null;
                            }
                        };
                        // Taken from http://stackoverflow.com/a/11977186
                        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
                            task.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null );
                        }
                        else {
                            task.execute();
                        }
                    }
                    else {
                        Log.d(Tag, String.format("Registration ID %s found for app version %d", regID, regVersion ));
                    }
                }
                catch(NameNotFoundException e) {
                    Log.w(Tag,"Package name not found when checking push notification registration");
                }
            }
            else {
                Log.e( Tag, String.format("Google play services not available: resultCode=%d", resultCode ));
            }
        }
    }
}
