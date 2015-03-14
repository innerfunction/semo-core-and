package com.innerfunction.semo.push;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.innerfunction.semo.Component;
import com.innerfunction.semo.ComponentFactory;
import com.innerfunction.semo.Configuration;
import com.innerfunction.semo.ConfiguredLocals;
import com.innerfunction.util.BackgroundTaskRunner;

@SuppressLint("NewApi")
public class NotificationService {

    static final String Tag = NotificationService.class.getSimpleName();
    
    static final Map<Context,NotificationService> Instances = new HashMap<Context,NotificationService>();
    
    static final int FavouriteNotificationID = 1;
    
    //private Configuration configuration;
    /** The Android context. */
    private Context context;
    /** Map of GCM message handlers, keyed by handler name. */
    private Map<String,PushMessageHandler> messageHandlers = new HashMap<String,PushMessageHandler>();
    /** Locally stored settings. */
    private ConfiguredLocals localSettings;

    private NotificationService(Context context) {
        this.context = context;
    }
    
    public void configure(Configuration configuration, ComponentFactory factory) {
        /*
        this.configuration = configuration;
        pushNotificationsEnabled = configuration.getValueAsBoolean("pushNotificationsEnabled", false );
        pushSenderID = configuration.getValueAsString("and:pushSenderID");
        */
        localSettings = new ConfiguredLocals("semo.push", configuration );
        
        Map<String,Configuration> handlerConfigs = configuration.getValueAsConfigurationMap("handlers");
        for( String name : handlerConfigs.keySet() ) {
            String id = String.format("NotificationService.handler.%s", name );
            Component handler = factory.makeComponent( handlerConfigs.get( name ), id );
            if( handler instanceof PushMessageHandler ) {
                messageHandlers.put( name, (PushMessageHandler)handler );
            }
            else {
                Log.w(Tag,String.format("Component %s is not an instance of GCMessageHandler", id ) );
            }
        }
        registerForPushNotifications();
    }
    
    public boolean handleMessageIntent(Intent intent) {
        boolean handled = false;
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance( context );
        String messageType = gcm.getMessageType( intent );
        if( messageType == null || GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals( messageType ) ) {
            Bundle extras = intent.getExtras();
            String handlerName = extras.getString("handler");
            if( handlerName == null ) {
                Log.w(Tag,"No handler specified on incoming GCM intent");
            }
            else {
                PushMessageHandler handler = messageHandlers.get( handlerName );
                if( handler == null ) {
                    Log.w(Tag,String.format("No handler named '%s' found for incoming CGM intent", handlerName ) );
                }
                else {
                    handled = handler.handleMessageIntent( intent, this );
                }
            }
        }
        return handled;
    }
    
    private void registerForPushNotifications() {
        boolean pushNotificationsEnabled = localSettings.getBoolean("notificationsEnabled");
        if( pushNotificationsEnabled ) {
            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable( context );
            if( resultCode == ConnectionResult.SUCCESS ) {
                try {
                    // Check for stored registration ID
                    String regID = localSettings.getString("registrationID");
                    int regVersion = localSettings.getInt("registrationVersion");
                    // Get app's current version number.
                    PackageInfo pinfo = context.getPackageManager().getPackageInfo( context.getPackageName(), 0 );
                    final int currVersion = pinfo.versionCode;
                    // Registration needed if no reg ID found, or app version has changed.
                    boolean registrationNeeded = regID == null || regVersion != currVersion;
                    if( registrationNeeded ) {
                        // Register app in background.
                        BackgroundTaskRunner.run(new BackgroundTaskRunner.Task() {
                            @Override
                            public void run() {
                                String regID;
                                try {
                                    String pushSenderID = localSettings.getString("senderID");
                                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance( context );
                                    regID = gcm.register( pushSenderID );
                                    // TODO: Send registration ID to backend.

                                    // Store the registration ID.
                                    localSettings.setString("registrationID", regID );
                                    localSettings.setInt("registrationVersion", currVersion ); 
                                }
                                catch(IOException e) {
                                    Log.e(Tag,"Registering for push notifications", e );
                                }
                            }
                        });
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
    
    /**
     * Return a singleton instance of this class for the specified context.
     */
    public static synchronized NotificationService getInstance(Context context) {
        NotificationService service = Instances.get( context );
        if( service == null ) {
            service = new NotificationService( context );
            Instances.put( context, service );
        }
        return service;
    }
    
}
