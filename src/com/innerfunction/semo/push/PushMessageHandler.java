package com.innerfunction.semo.push;

import android.content.Intent;

public interface PushMessageHandler {

    public boolean handleMessageIntent(Intent intent, NotificationService service);
    
}
