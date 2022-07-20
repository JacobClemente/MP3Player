package com.jclemente.mp3player;

import android.net.Uri;

import java.io.File;
import java.io.Serializable;

public class Audio implements Serializable, Comparable {
    private final String uri;
    private final File file;
    private final String fileName;
    private final int duration;
    private final int size;
    private String title;
    private String artist;
    private String imagePath;

    public Audio(Uri uri, File file, String fileName, int duration, int size) {
        this.uri = uri.toString();
        this.file = file;
        this.fileName = fileName;
        this.duration = duration;
        this.size = size;
    }

    public File getFile() {
        return this.file;
    }

    public String getFileName() {
        return this.fileName;
    }

    public Uri getUri() {
        return Uri.parse(this.uri);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getImagePath() {
        return imagePath;
    }

    @Override
    public int compareTo(Object o) {
        Audio audio = (Audio) o;
        return this.getFileName().compareTo(audio.getFileName());
    }
}
