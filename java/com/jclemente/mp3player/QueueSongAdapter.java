package com.jclemente.mp3player;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;

public class QueueSongAdapter extends ArrayAdapter {
    private QueueActivity queueActivity;
    private final ArrayList<Audio> audioList;
    private final ArrayList<Bitmap> imagesList;

    public QueueSongAdapter(@NonNull QueueActivity queueActivity, ArrayList<Audio> audioList, ArrayList<Bitmap> imagesList) {
        super(queueActivity, R.layout.queue_song_row, audioList);
        this.queueActivity = queueActivity;
        this.audioList = audioList;
        this.imagesList = imagesList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) queueActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row;
        if (convertView == null)
            row = inflater.inflate(R.layout.queue_song_row, null, true);
        else
            row = convertView;
        TextView textViewName = row.findViewById(R.id.textViewNameQueue);
        TextView textViewArtist = row.findViewById(R.id.textViewArtistQueue);
        ImageView imageViewArt = row.findViewById(R.id.imageViewArtistQueue);
        if (audioList.get(position).getTitle() == null || audioList.get(position).getArtist() == null) {
            textViewName.setText(FilenameUtils.removeExtension(audioList.get(position).getFileName()));
            textViewArtist.setText("Unknown artist");
        } else {
            textViewName.setText(audioList.get(position).getTitle());
            textViewArtist.setText(audioList.get(position).getArtist());
        }
        imageViewArt.setImageBitmap(imagesList.get(position));

        return row;
    }
}
