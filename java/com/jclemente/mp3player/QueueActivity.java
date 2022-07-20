package com.jclemente.mp3player;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class QueueActivity extends AppCompatActivity {

    private static WeakReference<MusicService> serviceReference;
    private static WeakReference<ListActivity> listActivityReference;
    private MusicService service;
    private ListActivity listActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);
        service = serviceReference.get();
        listActivity = listActivityReference.get();
        ArrayList<Audio> queue = new ArrayList<>(service.getQueue());
        ArrayList<Bitmap> imagesList = new ArrayList<>();
        for (Audio audio : queue) {
            imagesList.add(listActivity.getImagesList().get(listActivity.getMapValues().indexOf(audio)));
        }
        QueueSongAdapter adapter = new QueueSongAdapter(this, queue, imagesList);
        ListView listView = findViewById(R.id.queue_list);
        listView.setAdapter(adapter);
    }

    public static void updateActivity(MusicService service) {
        serviceReference = new WeakReference<>(service);
    }

    public static void updateActivity(ListActivity listActivity) {
        listActivityReference = new WeakReference<>(listActivity);
    }
}