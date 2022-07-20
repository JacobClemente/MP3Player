package com.jclemente.mp3player;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;

public class SongAdapter extends ArrayAdapter {

    private ListActivity listActivity;
    private final ArrayList<Audio> audioList;
    private final ArrayList<Bitmap> imagesList;

    public SongAdapter(@NonNull ListActivity listActivity, ArrayList<Audio> audioList, ArrayList<Bitmap> imagesList) {
        super(listActivity, R.layout.song_row, audioList);
        this.listActivity = listActivity;
        this.audioList = audioList;
        this.imagesList = imagesList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) listActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row;
        if (convertView == null)
            row = inflater.inflate(R.layout.song_row, null, true);
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
        ImageButton addToQueueButton = row.findViewById(R.id.addToQueue);
        addToQueueButton.setFocusable(false);
        addToQueueButton.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(listActivity, view);
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.add_song_to_queue:
                        listActivity.addUserSong(audioList.get(position));
                    default:
                        return false;
                }
            });
            MenuInflater menuInflater = popupMenu.getMenuInflater();
            menuInflater.inflate(R.menu.song_misc, popupMenu.getMenu());
            popupMenu.show();
        });
        return row;
    }
}
