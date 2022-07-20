package com.jclemente.mp3player;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

public class ListActivity extends AppCompatActivity {

    private ArrayList<Audio> mapValues;
    private ArrayList<Bitmap> imagesList;
    private HashMap<String, Audio> audioMap;

    private static WeakReference<MusicService> serviceReference;
    private MusicService service;

    @RequiresApi(api = 26)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        try {
//            this.getSupportActionBar().hide();
//        } catch (NullPointerException e) {
//        }
        setContentView(R.layout.activity_list);
        MusicService.updateActivity(this);
        QueueActivity.updateActivity(this);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ListActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }


        if (serviceReference == null) {
            // If the service is not already started, we check whether the device has an audio hash map saved already. If so, initialize the map from the read data. Otherwise, begin scanning the device for audio files and fill the hashmap with its results.
            audioMap = new HashMap<>();
            File audioListFile = new File(getApplicationContext().getFilesDir(), "audio_map.data");
            if (audioListFile.isFile()) {
                try {
                    FileInputStream inputStream = new FileInputStream(audioListFile);
                    ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                    audioMap = (HashMap<String, Audio>) objectInputStream.readObject();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                // File containing audio hashmap was not found; will now scan for songs.
                loadSongs(audioMap);
            }
            mapValues = new ArrayList<>(audioMap.values());
            Intent notificationIntent = new Intent(this, MusicService.class);
            startService(notificationIntent);
        } else {
            // The service was detected; initialize the audio hashmap and audio list from the service's stored instances. Inform the service of the new list activity instance.
            service = serviceReference.get();
            mapValues = service.getMapValues();
            audioMap = service.getAudioMap();
            service.updateListActivity();
            performServiceStarted();
        }
    }

    /**
     * This magic method is responsible for retrieving the path of a specific audio file from the user's device in the cursor while-loop.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor
                        .getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * This magic method fills the hashmap with all detected audio files on the user's device.
     * TODO: Learn how the fuck this actually works...
     */
    private void loadSongs(HashMap<String, Audio> audioMap) {
        HashMap<String, Integer> directoryList;
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE
        };
        String sortOrder = MediaStore.Audio.Media.DISPLAY_NAME + " ASC";
        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                collection,
                projection,
                null,
                null,
                sortOrder
        )) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            directoryList = new HashMap<>();
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColumn);
                int size = cursor.getInt(sizeColumn);
                Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                File path = new File(getDataColumn(getApplicationContext(), contentUri, null, null));
                if (directoryList.containsKey(path.getParent()))
                    directoryList.put(path.getParent(), directoryList.get(path.getParent()) + 1);
                else
                    directoryList.put(path.getParent(), 1);
                // Registering the audio object to a key from its file path.
                Audio audio = new Audio(contentUri, path, FilenameUtils.removeExtension(path.getName()), duration, size);
                audio.setImagePath(new File(this.getFilesDir(), FilenameUtils.removeExtension(audio.getFileName()) + ".thumb").getAbsolutePath());
                audioMap.put(path.toString(), audio);
            }
            // Once finished with creating the hashmap, try saving it to the user's device for future usage.
            File audioMapFile = new File(getApplicationContext().getFilesDir(), "audio_map.data");
            try {
                FileOutputStream fileOutput = new FileOutputStream(audioMapFile);
                ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput);
                objectOutput.writeObject(audioMap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    directoryList.entrySet().forEach(e -> System.out.println(e.getKey() + " - Occurrences: " + e.getValue()));
                }
        }
    }

    // When the service has been created, obtain its reference and begin initializing its variables for playing audio files. Additionally,
    public void performServiceStarted() {
        runOnUiThread(() -> {
            this.service = serviceReference.get();
            // The image array list is filled here because we do not know whether we load the hashmap from the loadSongs method or from the existing service's hashmap.
            imagesList = new ArrayList<>();
            Bitmap aquaBmp = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.aqua);
            for (int i = 0; i < mapValues.size(); i++) {
                imagesList.add(aquaBmp);
            }
            // Set the "now playing" tab to have the correct information and image on it.
            View nowPlaying = findViewById(R.id.nowPlaying);
            nowPlaying.setOnClickListener(this::displayPlayer);
            updateNowPlaying();
            // Create song adapter from read files lists.
            SongAdapter adapter = new SongAdapter(this, mapValues, imagesList);
            ListView listView = findViewById(R.id.listSongs);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                System.out.println("You just clicked a song!");
                LinkedList<Audio> queue = new LinkedList<>();
                LinkedList<Audio> previous = new LinkedList<>();
                for (int i = 0; i < position; i++)
                    previous.add(mapValues.get(i));
                for (int i = position; i < mapValues.size(); i++)
                    queue.add(mapValues.get(i));
                Audio currentSong = queue.poll();
                if (service.getFlowEnum() == FlowEnum.SHUFFLE)
                    Collections.shuffle(queue);
                service.setQueue(queue);
                service.setPrevious(previous);
                service.setCurrentSong(currentSong);
                service.changeSong();
                updateNowPlaying();
            });

            // Attach ID3V2 data to audio objects and read image thumbnails from their paths.
            AudioDataAttacher attacher = new AudioDataAttacher(this, audioMap, adapter);
            ReadImagesThread reader = new ReadImagesThread(this, mapValues, imagesList, adapter);
            reader.start();
            attacher.start();
        });
    }

    // To be called whenever the list activity's "Now Playing" view needs to be refreshed.
    public void updateNowPlaying() {
        runOnUiThread(() -> {
            ImageView nowPlayingArt = findViewById(R.id.nowPlaying_art);
            TextView nowPlayingTitle = findViewById(R.id.nowPlaying_title);
            TextView nowPlayingArtist = findViewById(R.id.nowPlaying_artist);
            ImageButton nowPlayingButton = findViewById(R.id.nowPlaying_play);

            if (service.getCurrentSong() != null) {
                System.out.printf("Service Current Song: %s, Map Value's Size: %d, Map Values Index: %d\n", service.getCurrentSong().getTitle(), mapValues.size(), mapValues.indexOf(service.getCurrentSong()));
                nowPlayingArt.setImageBitmap(imagesList.get(mapValues.indexOf(service.getCurrentSong())));
                if (service.getCurrentSong().getTitle() != null)
                    nowPlayingTitle.setText(service.getCurrentSong().getTitle());
                else
                    nowPlayingTitle.setText(FilenameUtils.removeExtension(service.getCurrentSong().getFileName()));
                if (service.getCurrentSong().getArtist() != null)
                    nowPlayingArtist.setText(service.getCurrentSong().getArtist());
                else
                    nowPlayingArtist.setText("Artist unknown");
                if (service.getPlayer().isPlaying())
                    nowPlayingButton.setImageResource(R.drawable.pause);
                else
                    nowPlayingButton.setImageResource(R.drawable.play);
            }
        });
    }

    public void addUserSong(Audio audio) {
        if (service != null)
            service.addUserSong(audio, this);
    }

    public void displayPlayer(View view) {
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }

    public void clickPlay(View view) {
        this.service.play(view);
    }

    public void clickDisplayQueue(View view) {
        Intent intent = new Intent(this, QueueActivity.class);
        startActivity(intent);
    }

    public static void updateActivity(MusicService service) {
        serviceReference = new WeakReference<>(service);
    }

    public ImageButton getPlayButton() {
        return findViewById(R.id.nowPlaying_play);
    }

    public ArrayList<Audio> getMapValues() {
        return mapValues;
    }

    public HashMap<String, Audio> getAudioMap() {
        return audioMap;
    }

    public ArrayList<Bitmap> getImagesList() {
        return this.imagesList;
    }
}
