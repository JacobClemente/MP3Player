package com.jclemente.mp3player;

import org.apache.commons.io.FilenameUtils;

import java.util.Comparator;

public class AudioComparator implements Comparator<Audio> {
    @Override
    public int compare(Audio o1, Audio o2) {
        if (o1.getTitle() != null && o2.getTitle() != null)
            return o1.getTitle().compareToIgnoreCase(o2.getTitle());
        else
            return FilenameUtils.removeExtension(o1.getFileName()).compareToIgnoreCase(FilenameUtils.removeExtension(o2.getFileName()));
    }
}
