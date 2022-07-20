package com.jclemente.mp3player;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.widget.ArrayAdapter;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ReadImagesThread extends Thread {
    private ArrayList<Audio> audioList;
    private ArrayList<Bitmap> images;
    private ListActivity context;
    private ArrayAdapter adapter;

    public ReadImagesThread(ListActivity context, ArrayList<Audio> audioList, ArrayList<Bitmap> images, ArrayAdapter adapter) {
        this.audioList = audioList;
        this.images = images;
        this.context = context;
        this.adapter = adapter;
    }

    @Override
    public void run() {
        for (int i = 0; i < audioList.size(); i++) {
            File file = new File(audioList.get(i).getImagePath());
            if (file.isFile()) {
                Bitmap bitmap = BitmapFactory.decodeFile(audioList.get(i).getImagePath());
                if (bitmap != null) {
                    images.set(i, bitmap);
                    updateAdapter(context, adapter);
                }
            }
        }
        context.updateNowPlaying();
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        for (int i = 0; i < audioList.size(); i++) {
            File file = new File(audioList.get(i).getImagePath());
            if (!file.isFile()) {
                mmr.setDataSource(context.getApplicationContext(), audioList.get(i).getUri());
                byte[] data = mmr.getEmbeddedPicture();
                if (data != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    File imageThumbnail = new File(context.getApplicationContext().getFilesDir(), FilenameUtils.removeExtension(audioList.get(i).getFileName()) + ".thumb");
                    try (FileOutputStream out = new FileOutputStream(imageThumbnail)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out);
                        images.set(i, bitmap);
                        updateAdapter(context, adapter);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        context.updateNowPlaying();
    }

    private static void updateAdapter(Activity activity, ArrayAdapter adapter) {
        activity.runOnUiThread(adapter::notifyDataSetChanged);
    }
}
