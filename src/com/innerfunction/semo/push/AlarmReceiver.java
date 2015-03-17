package com.innerfunction.semo.push;

import com.innerfunction.semo.AppContainer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    boolean configured;
    private NotificationService notificationService;

    public void setNotificationService(NotificationService service) {
        this.notificationService = service;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if( !configured ) {
            AppContainer.getAppContainer( context ).configureAndroid( this );
            configured = true;
        }
        if( notificationService != null ) {
            notificationService.handleMessageIntent( intent );
        }
    }

}
