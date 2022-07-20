package com.jclemente.mp3player;

import android.app.Activity;
import android.widget.ArrayAdapter;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class AudioDataAttacher extends Thread {

    private Activity context;
    private final HashMap<String, Audio> hashMap;
    private ArrayAdapter adapter;

    public AudioDataAttacher(Activity context, HashMap<String, Audio> hashMap, ArrayAdapter adapter) {
        this.hashMap = hashMap;
        this.context = context;
        this.adapter = adapter;
    }

    @Override
    public void run() {
        ArrayList<Audio> audioList = new ArrayList<>(hashMap.values());
        for (int i = 0; i < audioList.size(); i++) {
            if (audioList.get(i).getTitle() == null || audioList.get(i).getArtist() == null) {
                Mp3File mp3;
                ID3v2 id3v2Tag = null;
                try {
                    mp3 = new Mp3File(audioList.get(i).getFile().getAbsolutePath(), false);
                    id3v2Tag = mp3.getId3v2Tag();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (UnsupportedTagException e) {
                    e.printStackTrace();
                } catch (InvalidDataException e) {
                    e.printStackTrace();
                }
                if (id3v2Tag != null) {
                    audioList.get(i).setTitle(id3v2Tag.getTitle());
                    audioList.get(i).setArtist(id3v2Tag.getArtist());
                    updateAdapter(context, adapter);
                    File audioMapFile = new File(context.getApplicationContext().getFilesDir(), "audio_map.data");
                    try {
                        FileOutputStream fileOutput = new FileOutputStream(audioMapFile);
                        ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput);
                        objectOutput.writeObject(hashMap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private static void updateAdapter(Activity activity, ArrayAdapter adapter) {
        activity.runOnUiThread(adapter::notifyDataSetChanged);
    }
}
