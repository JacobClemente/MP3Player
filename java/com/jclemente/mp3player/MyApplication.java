package com.jclemente.mp3player;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class MyApplication extends Application {
    private static Context context;
    public static final String CHANNEL_ID = "jacobsmp3player";

    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
        createNotificationChannel();
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
