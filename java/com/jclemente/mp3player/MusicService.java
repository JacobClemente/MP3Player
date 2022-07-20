package com.jclemente.mp3player;

import static com.jclemente.mp3player.MyApplication.CHANNEL_ID;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

public class MusicService extends Service {

    private MediaPlayer player;
    private Audio currentSong;
    private MusicIntentReceiver musicIntentReceiver;
    private MediaButtonIntentReceiver mediaButtonIntentReceiver;

    private HashMap<String, Audio> audioMap;
    private ArrayList<Audio> mapValues;
    private LinkedList<Audio> queue;
    private LinkedList<Audio> previous;
    private LinkedList<Audio> shuffleQueue;
    private LinkedList<Audio> shufflePrevious;
    private LinkedList<Audio> userQueue;
    private LinkedList<Audio> shuffleUserQueue;
    private boolean isAutoPlay;
    private FlowEnum flowEnum;

    private static WeakReference<ListActivity> listActivityReference;
    private static WeakReference<PlayerActivity> playerActivityReference;
    private ListActivity listActivity;
    private PlayerActivity playerActivity;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate() {
        super.onCreate();
        Thread thread = new Thread((() -> {
            ListActivity.updateActivity(this);
            PlayerActivity.updateActivity(this);
            QueueActivity.updateActivity(this);
            listActivity = listActivityReference.get();
            player = new MediaPlayer();
            mapValues = listActivity.getMapValues();
            audioMap = listActivity.getAudioMap();
            queue = new LinkedList<>();
            userQueue = new LinkedList<>();
            shuffleUserQueue = new LinkedList<>();
            previous = new LinkedList<>();
            flowEnum = FlowEnum.STRAIGHT;
            isAutoPlay = false;
            Collections.sort(mapValues, (new AudioComparator()));
            for (Audio audio : mapValues)
                queue.add(audio);
            player.setOnCompletionListener(event -> {
                ImageButton nowPlayingButton = listActivity.getPlayButton();
                if (!isAutoPlay) {
                    switch (flowEnum) {
                        case STRAIGHT:
                        case SHUFFLE:
                            next();
                            player.pause();
                            nowPlayingButton.setImageResource(R.drawable.play);
                            if (playerActivity != null)
                                playerActivity.getPlayButton().setImageResource(R.drawable.play);
                            break;
                        case REPEAT:
                            try {
                                player.reset();
                                player.setDataSource(MyApplication.getAppContext(), currentSong.getUri());
                                player.prepare();
                                nowPlayingButton.setImageResource(R.drawable.play);
                                if (playerActivity != null)
                                    playerActivity.getPlayButton().setImageResource(R.drawable.play);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                } else {
                    switch (flowEnum) {
                        case STRAIGHT:
                        case SHUFFLE:
                            next();
                            player.start();
                            nowPlayingButton.setImageResource(R.drawable.pause);
                            if (playerActivity != null)
                                playerActivity.getPlayButton().setImageResource(R.drawable.pause);
                            break;
                        case REPEAT:
                            try {
                                player.reset();
                                player.setDataSource(MyApplication.getAppContext(), currentSong.getUri());
                                player.prepare();
                                player.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            });
            player.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build());
            try {
                currentSong = queue.poll();
                player.setDataSource(getApplicationContext(), currentSong.getUri());
                player.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            listActivity.performServiceStarted();
            musicIntentReceiver = new MusicIntentReceiver();
            mediaButtonIntentReceiver = new MediaButtonIntentReceiver();

            IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
            IntentFilter buttonFilter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
            buttonFilter.setPriority(1000);
            registerReceiver(musicIntentReceiver, filter);
            registerReceiver(mediaButtonIntentReceiver, buttonFilter);
        }));
        thread.start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, ListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("This is a test.")
                .setContentText("The test's text.")
                .setSmallIcon(R.drawable.shinobu)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        return START_STICKY;
    }

    public void play(View view) {
        ImageButton nowPlayingButton = listActivity.getPlayButton();
        if (!player.isPlaying()) {
            player.start();
            if (view instanceof ImageButton)
                ((ImageButton) view).setImageResource(R.drawable.pause);
            nowPlayingButton.setImageResource(R.drawable.pause);
        } else {
            player.pause();
            if (view instanceof ImageButton)
                ((ImageButton) view).setImageResource(R.drawable.play);
            nowPlayingButton.setImageResource(R.drawable.play);
        }
    }

    public void previous() {
        switch (flowEnum) {
            case STRAIGHT:
            case REPEAT:
                if (!previous.isEmpty()) {
                    try {
                        player.stop();
                        queue.addFirst(currentSong);
                        currentSong = previous.getLast();
                        previous.removeLast();
                        player.reset();
                        player.setDataSource(MyApplication.getAppContext(), currentSong.getUri());
                        player.prepare();
                        player.start();
                        if (playerActivity != null)
                            playerActivity.updatePlayerInfo(currentSong);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    listActivity.updateNowPlaying();
                }
                break;
            case SHUFFLE:
                if (!shufflePrevious.isEmpty()) {
                    try {
                        player.stop();
                        shuffleQueue.addFirst(currentSong);
                        currentSong = shufflePrevious.getLast();
                        shufflePrevious.removeLast();
                        Collections.shuffle(shuffleQueue);
                        player.reset();
                        player.setDataSource(MyApplication.getAppContext(), currentSong.getUri());
                        player.prepare();
                        player.start();
                        if (playerActivity != null)
                            playerActivity.updatePlayerInfo(currentSong);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    listActivity.updateNowPlaying();
                }
                break;
        }
    }

    public void next() {
        switch (flowEnum) {
            case STRAIGHT:
            case REPEAT:
                try {
                    previous.add(currentSong);
                    if (userQueue.isEmpty())
                        if (queue.peek() != null)
                            currentSong = queue.poll();
                        else {
                            queue = previous;
                            previous.clear();
                        }
                    else {
                        currentSong = userQueue.poll();
                        if (queue.contains(currentSong))
                            queue.remove(currentSong);
                        if (previous.contains(currentSong))
                            previous.remove(currentSong);
                    }

                    player.reset();
                    player.setDataSource(MyApplication.getAppContext(), currentSong.getUri());
                    player.prepare();
                    player.start();
                    if (playerActivity != null)
                        playerActivity.updatePlayerInfo(currentSong);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                listActivity.updateNowPlaying();
                break;
            case SHUFFLE:
                try {
                    if (shufflePrevious.contains(currentSong))
                        shufflePrevious.remove(currentSong);
                    shufflePrevious.add(currentSong);
                    if (shuffleUserQueue.peek() == null)
                        if (shuffleQueue.peek() != null)
                            currentSong = shuffleQueue.poll();
                        else {
                            shuffleQueue = previous;
                            previous.clear();
                            Collections.shuffle(shuffleQueue);
                        }
                    else {
                        currentSong = shuffleUserQueue.poll();
                        userQueue.remove(currentSong);
                        if (queue.contains(currentSong))
                            queue.remove(currentSong);
                        if (previous.contains(currentSong))
                            previous.remove(currentSong);
                    }
                    player.reset();
                    player.setDataSource(MyApplication.getAppContext(), currentSong.getUri());
                    player.prepare();
                    player.start();
                    if (playerActivity != null)
                        playerActivity.updatePlayerInfo(currentSong);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                listActivity.updateNowPlaying();
                break;
        }
    }

    public void changeFlow(View view) {
        switch (flowEnum) {
            case STRAIGHT:
                flowEnum = FlowEnum.SHUFFLE;
                ((ImageButton) view).setImageResource(R.drawable.shuffle);
                shuffleQueue = new LinkedList<>();
                for (Audio audio : mapValues)
                    shuffleQueue.add(audio);
                shufflePrevious = previous;
                Collections.shuffle(shuffleQueue);
                if (shuffleUserQueue != null) {
                    Collections.shuffle(shuffleUserQueue);
                }
                break;
            case SHUFFLE:
                flowEnum = FlowEnum.REPEAT;
                ((ImageButton) view).setImageResource(R.drawable.loop);
                queue = new LinkedList<>();
                previous = new LinkedList<>();
                int songIndex = mapValues.indexOf(currentSong);
                for (int i = songIndex + 1; i < mapValues.size(); i++)
                    queue.add(mapValues.get(i));
                for (int i = 0; i < songIndex; i++)
                    previous.add(mapValues.get(i));
                break;
            case REPEAT:
                flowEnum = FlowEnum.STRAIGHT;
                ((ImageButton) view).setImageResource(R.drawable.ordered);
                break;
        }
    }

    public void toggleAutoPlay(View view) {
        isAutoPlay = !isAutoPlay;
        if (isAutoPlay)
            ((ImageButton) view).setImageResource(R.drawable.auto);
        else
            ((ImageButton) view).setImageResource(R.drawable.auto_off);
    }

    public void setQueue(LinkedList<Audio> queue) {
        this.queue = queue;
    }

    public void setPrevious(LinkedList<Audio> previous) {
        this.previous = previous;
    }

    public Audio getCurrentSong() {
        return currentSong;
    }

    public void setCurrentSong(Audio currentSong) {
        this.currentSong = currentSong;
    }

    public void changeSong() {
        try {
            player.stop();
            player.reset();
            player.setDataSource(MyApplication.getAppContext(), currentSong.getUri());
            player.prepare();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addUserSong(Audio userSong, Context context) {
        if (!userQueue.contains(userSong) && currentSong != userSong) {
            userQueue.add(userSong);
            shuffleUserQueue.add(userSong);
            Collections.shuffle(shuffleUserQueue);
            Toast toast = Toast.makeText(context, "Song added to queue.", Toast.LENGTH_LONG);
            toast.show();
        } else if (currentSong != userSong) {
            userQueue.remove(userSong);
            shuffleUserQueue.remove(userSong);
            Toast toast = Toast.makeText(context, "Song removed from queue.", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    public FlowEnum getFlowEnum() {
        return flowEnum;
    }

    public boolean isAutoPlay() {
        return isAutoPlay;
    }

    public ArrayList<Audio> getMapValues() {
        return this.mapValues;
    }

    public HashMap<String, Audio> getAudioMap() {
        return audioMap;
    }

    public LinkedList<Audio> getQueue() {
        return this.userQueue;
    }

    public static void updateActivity(ListActivity activity) {
        listActivityReference = new WeakReference<>(activity);
    }

    public static void updateActivity(PlayerActivity activity) {
        playerActivityReference = new WeakReference<>(activity);
    }

    public void updateListActivity() {
        this.listActivity = listActivityReference.get();
    }

    public void updatePlayer() {
        this.playerActivity = playerActivityReference.get();
    }

    private class MusicIntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        System.out.println("Headset is unplugged.");
                        if (player.isPlaying()) {
                            player.pause();
                            ImageButton nowPlayingButton = listActivity.getPlayButton();
                            nowPlayingButton.setImageResource(R.drawable.play);
                            if (playerActivity != null) {
                                ImageButton playerButton = playerActivity.getPlayButton();
                                playerButton.setImageResource(R.drawable.play);
                            }
                        }
                        break;
                    case 1:
                        System.out.println("Headset is plugged in.");
                        break;
                    default:
                        System.out.println("Unknown headset state.");
                        break;
                }
            }
        }
    }

    private class MediaButtonIntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
                return;
            }
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }
            System.out.println("I have received the broadcast.");
            int action = event.getAction();
            if (action == KeyEvent.ACTION_DOWN) {
                System.out.println("The media key was pressed.");
                play(null);
            }
            abortBroadcast();
        }

    }
}
