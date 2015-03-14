package com.innerfunction.semo.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationService.getInstance( context ).handleMessageIntent( intent );
    }

}
