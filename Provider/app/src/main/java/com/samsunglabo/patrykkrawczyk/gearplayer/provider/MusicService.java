package com.samsunglabo.patrykkrawczyk.gearplayer.provider;


import java.util.ArrayList;

import android.support.v7.app.ActionBar;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;
import android.widget.RemoteViews;

/**
 * Created by Capybara on 09/12/2015.
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener
{

    private MediaPlayer player;
    private ArrayList<Song> songs;
    private int songPosition;
    private final IBinder musicBind = new MusicBinder();
    private String songTitle = "";
    private static final int NOTIFY_ID = 1;
    private boolean shuffle = false;
    private Random rand;
    private ActionBar actionbar;
    public static final String PLAY_ACTION = "com.patrykkrawczyk.samsunglabo.mediaplayerapp.PLAY_ACTION";
    public static final String NEXT_ACTION = "com.patrykkrawczyk.samsunglabo.mediaplayerapp.NEXT_ACTION";
    public static final String PREV_ACTION = "com.patrykkrawczyk.samsunglabo.mediaplayerapp.PREV_ACTION";
    private BroadcastReceiver receiver;

    public void onCreate()
    {
        super.onCreate();
        songPosition = 0;
        player = new MediaPlayer();

        rand = new Random();

        initMusicPlayer();

        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent.getAction().equals(NEXT_ACTION))
                {
                    playNext();
                }
                else if (intent.getAction().equals(PREV_ACTION))
                {
                    playPrev();
                }
                else if (intent.getAction().equals(PLAY_ACTION))
                {
                    if (isPlaying())
                        pausePlayer();
                    else
                        playSong();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(NEXT_ACTION);
        filter.addAction(PLAY_ACTION);
        filter.addAction(PREV_ACTION);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy()
    {
        stopForeground(true);
    }

    public void initMusicPlayer()
    {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public boolean toggleShuffle()
    {
        shuffle = !shuffle;
        return shuffle;
    }

    public void setList(ArrayList<Song> s)
    {
        songs = s;
    }
    public void setABReference(ActionBar ab)
    {
        actionbar = ab;
    }

    public void setSong(int songIndex)
    {
        songPosition = songIndex;
    }

    //binder
    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    //activity will bind to service
    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    //release resources when unbind
    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }


    public Song playSong(){
        player.reset();

        Song playSong = songs.get(songPosition);
        songTitle = playSong.getTitle();
        long currSong = playSong.getID();

        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);
        //set the data source
        try
        {
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e)
        {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player.prepareAsync();

        startPlayer();

        actionbar.setTitle(playSong.getTitle());
        actionbar.setSubtitle(playSong.getArtist());

        return playSong;
    }



    public void playPrev()
    {
        songPosition--;
        if(songPosition < 0) songPosition = songs.size() - 1;
        playSong();
    }

    public void setVolume(int v)
    {
        float volume = v;
        volume /= 100;
        player.setVolume(volume, volume);
    }

    public void playNext()
    {
        if(shuffle)
        {
            int newSong = songPosition;
            while (newSong == songPosition)
            {
                newSong = rand.nextInt(songs.size());
            }
            songPosition = newSong;
        }
        else
        {
            songPosition++;
            if (songPosition > songs.size()) songPosition = 0;
        }

        playSong();
    }

    public int getPosition()
    {
        return player.getCurrentPosition();
    }

    public int getDuration()
    {
        return player.getDuration();
    }

    public boolean isPlaying()
    {
        return player.isPlaying();
    }

    public void pausePlayer()
    {
        player.pause();

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.pause)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Paused")
                .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);

    }

    public void seek(int posn)
    {
        player.seekTo(posn);
    }

    public void startPlayer()
    {
        player.start();

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);

        Intent prevReceive = new Intent();
        Intent playReceive = new Intent();
        Intent nextReceive = new Intent();

        prevReceive.setAction(PREV_ACTION);
        playReceive.setAction(PLAY_ACTION);
        nextReceive.setAction(NEXT_ACTION);

        PendingIntent pendingIntentPrev = PendingIntent.getBroadcast(this, 123,   prevReceive, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntentPlay = PendingIntent.getBroadcast(this, 1234,  playReceive, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntentNext = PendingIntent.getBroadcast(this, 12345, nextReceive, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.addAction(R.drawable.notprev, "Prev", pendingIntentPrev);
        builder.addAction(R.drawable.notplay, "Play", pendingIntentPlay);
        builder.addAction(R.drawable.notnext, "Next", pendingIntentNext);






        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }



    @Override
    public void onCompletion(MediaPlayer mp)
    {
        if (player.getCurrentPosition() > 0)
        {
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp)
    {
        mp.start();
        AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        if(manager.isMusicActive())
        {
            // Something is being played.
        }
    }
}
