package com.m.push;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.util.Log;
//import android.support.v4.app.NotificationCompat;

import androidx.core.app.NotificationCompat;

import com.m.androidNativeApp.MainActivity;
import com.m.androidNativeApp.R;

import org.jboss.aerogear.android.unifiedpush.MessageHandler;
import org.jboss.aerogear.android.unifiedpush.fcm.UnifiedPushMessage;

public class NotifyingHandler implements MessageHandler {

    public static final int NOTIFICATION_ID = 1;
    private Context context;

    public static final NotifyingHandler instance = new NotifyingHandler();

    public NotifyingHandler() {
    }

    /**
     * Handle received notifications
     * This is called when app is both in the Foreground and Background
     * @param context
     * @param bundle
     */
    @Override
    public void onMessage(Context context, Bundle bundle) {
        this.context = context;

        String message = bundle.getString(UnifiedPushMessage.ALERT_KEY);

        Log.d("APP: Notification", message);

        notify(bundle);
    }

    /**
     * Configure how the notification is displayed when the app is in the background
     * @param bundle
     */
    private void notify(Bundle bundle) {

        NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        String message = bundle.getString(UnifiedPushMessage.ALERT_KEY);

        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(PendingIntent.FLAG_UPDATE_CURRENT)
                .putExtra(UnifiedPushMessage.ALERT_KEY, message);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentText(message);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

}