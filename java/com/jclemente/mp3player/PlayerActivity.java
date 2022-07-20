package com.jclemente.mp3player;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.FilenameUtils;

import java.lang.ref.WeakReference;

public class PlayerActivity extends AppCompatActivity {

    private static WeakReference<MusicService> serviceReference;
    private MusicService service;

    // Views of player are used as local variables for performance reasons.
    private ImageButton playButton;
    private ImageView albumArt;
    private TextView title;
    private TextView artist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        // Inform the music-playing service of our new player activity and retrieving its instance for our use.
        MusicService.updateActivity(this);
        service = serviceReference.get();
        service.updatePlayer();

        // Local variables initialized.
        playButton = findViewById(R.id.button_play);
        albumArt = findViewById(R.id.image_art);
        title = findViewById(R.id.text_title);
        artist = findViewById(R.id.text_artist);

        // Views of the player activity declared for readability.
        SeekBar seekBar = findViewById(R.id.seekBar);
        TextView duration = findViewById(R.id.text_duration);
        ImageButton autoPlayButton = findViewById(R.id.buttonAutoPlay);
        ImageButton modeButton = findViewById(R.id.buttonFlow);
        updatePlayerInfo(service.getCurrentSong());

        if (service.isAutoPlay())
            autoPlayButton.setImageResource(R.drawable.auto);
        switch (service.getFlowEnum()) {
            case STRAIGHT:
                modeButton.setImageResource(R.drawable.ordered);
                break;
            case REPEAT:
                modeButton.setImageResource(R.drawable.loop);
                break;
            case SHUFFLE:
                modeButton.setImageResource(R.drawable.shuffle);
                break;
        }

        MediaPlayer player = service.getPlayer();
        Handler mHandler = new Handler();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (player != null) {
                    int position = player.getCurrentPosition();
                    int maximum = player.getDuration();
                    seekBar.setProgress(position);
                    seekBar.setMax(maximum);
                    int posSecond = (position / 1000) % 60;
                    int posMinute = (position / (1000 * 60)) % 60;
                    int durSecond = (maximum / 1000) % 60;
                    int durMinute = (maximum / (1000 * 60)) % 60;
                    String time = String.format("%02d:%02d / %02d:%02d", posMinute, posSecond, durMinute, durSecond);
                    duration.setText(time);
                }
                mHandler.postDelayed(this, 100);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (player != null && fromUser) {
                    player.seekTo(progress);
                }
            }
        });
    }

    public void updatePlayerInfo(Audio currentSong) {
        if (service.getPlayer().isPlaying())
            playButton.setImageResource(R.drawable.pause);
        else
            playButton.setImageResource(R.drawable.play);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(MyApplication.getAppContext(), currentSong.getUri());
        byte[] data = mmr.getEmbeddedPicture();
        if (data != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            albumArt.setImageBitmap(bitmap);
        } else {
            albumArt.setImageResource(R.drawable.aqua);
        }
        if (currentSong.getTitle() != null)
            title.setText(currentSong.getTitle());
        else
            title.setText(FilenameUtils.removeExtension(currentSong.getFileName()));
        if (currentSong.getArtist() != null)
            artist.setText(currentSong.getArtist());
        else
            artist.setText("Artist unknown");
    }

    public void clickPlay(View view) {
        service.play(view);
    }

    public void clickPrevious(View view) {
        service.previous();
        updatePlayerInfo(service.getCurrentSong());
    }

    public void clickNext(View view) {
        service.next();
        updatePlayerInfo(service.getCurrentSong());
    }

    public void clickChangeFlow(View view) {
        service.changeFlow(view);
    }

    public void clickAutoPlay(View view) {
        service.toggleAutoPlay(view);
    }

    public static void updateActivity(MusicService service) {
        serviceReference = new WeakReference<>(service);
    }

    public ImageButton getPlayButton() {
        return findViewById(R.id.button_play);
    }

}

