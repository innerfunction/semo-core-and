package com.innerfunction.push;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance( context );
        String messageType = gcm.getMessageType( intent );
        if( messageType == null || GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals( messageType ) ) {

            Bundle extras = intent.getExtras();
            
            // Create a new intent to open the app's main activity class. This will dispatch any
            // action defined on the incoming intent.
            Intent nextIntent = new Intent( context, ABFragmentActivity.class );
            nextIntent.setAction( Intent.ACTION_VIEW );
            nextIntent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            // Pass notification extras though to the ABFragmentActivity for handling.
            String key = ABFragmentActivity.Actions.DispatchAction.name();
            nextIntent.putExtra( key, extras.getString( key ) );
            
            // Setup back stack.
            PendingIntent sender = PendingIntent.getActivity( context, 0, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT );
            
            // Build the notification message.
            String title = extras.getString("title");
            String message = extras.getString("message");
            int smallIcon = extras.getInt("notificationSmallIcon", 0 );
            Bitmap largeIcon = BitmapFactory.decodeResource( context.getResources(), extras.getInt("notificationLargeIcon", 0 ));
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            
            Notification notification = new NotificationCompat.Builder( context )
            .setContentTitle( title )
            .setContentText( message )
            .setSmallIcon( smallIcon )
            .setLargeIcon( largeIcon )
            .setContentIntent( sender )
            .setAutoCancel( true )
            .setSound( alarmSound )
            .build();
            
            // Display the notification message.
            NotificationManager notificationManager = (NotificationManager)context.getSystemService( Context.NOTIFICATION_SERVICE );
            int notificationID = extras.getInt("notificationID", 0 );
            notificationManager.notify( notificationID, notification );
        }
    }

}
