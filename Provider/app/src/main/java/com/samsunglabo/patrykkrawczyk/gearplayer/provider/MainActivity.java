package com.samsunglabo.patrykkrawczyk.gearplayer.provider;

import android.support.v7.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;

import java.util.ArrayList;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.view.MenuItem;

import com.samsunglabo.patrykkrawczyk.gearplayer.provider.MusicService.MusicBinder;
import android.widget.MediaController.MediaPlayerControl;
import android.media.AudioManager;

import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl, AudioManager.OnAudioFocusChangeListener
{

    private ArrayList<Song> songList;
    private ListView songView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;
    private MusicController controller;
    private boolean paused = false, playbackPaused = false;
    private AudioManager audioManager;
    private ActionBar actionbar;


    private ProviderService providerService;
    private boolean providerServiceBound = false;
    private Intent providerServiceIntent;
    private EventBus bus = EventBus.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bus.register(this);

        songView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<Song>();

        getSongList();

        Collections.sort(songList, new Comparator<Song>()
        {
            @Override
            public int compare(Song lhs, Song rhs)
            {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);



        setController();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        {
            // No audio focus
        }

        actionbar = getSupportActionBar();
        actionbar.setTitle("MP3 Player");
    }

    public void onEvent(STAEvent event) {
        String action = event.message;

        if (action.contains("volume")) {
            action = action.replace("volume", "");
            setVolume(Integer.parseInt(action));
        } else {
            if      (action.equals("nextbutton")) playNext();
            else if (action.equals("prevbutton")) playPrev();
            else if (action.equals("pause"))      pause();
            else if (action.equals("play"))       start();
            else if (action.equals("exit")) {
                stopService(playIntent);
                musicSrv = null;
                System.exit(0);
            }
        }
    }

    private void setVolume(int v)
    {
        musicSrv.setVolume(v);

        controller.show(0);
    }

    @Override
    protected void onStop()
    {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (providerServiceIntent == null)
        {
            providerServiceIntent = new Intent(this, ProviderService.class);
            bindService(providerServiceIntent, providerServiceConnection, Context.BIND_AUTO_CREATE);
            startService(providerServiceIntent);
        }
        if (playIntent == null)
        {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (paused)
        {
            setController();
            paused = false;
        }
    }

    @Override
    protected void onDestroy()
    {
        stopService(playIntent);
        musicSrv = null;
        stopService(providerServiceIntent);
        providerService = null;
        bus.unregister(this);
        super.onDestroy();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        paused = true;
    }

    private ServiceConnection providerServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            ProviderService.ProviderServiceBinder binder = (ProviderService.ProviderServiceBinder)service;

            providerService = binder.getService();

          //  EventBus eb = EventBus.getDefault();
          //  eb.register(providerService);

            providerServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            providerServiceBound = false;
        }
    };




    @Override
    public void onAudioFocusChange(int focusChange)
    {
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
        {
            // App has audio focus, we can play
            start();
        }
        else
        {
            // No audio focus
            pause();
        }
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MusicBinder binder = (MusicBinder)service;

            musicSrv = binder.getService();

            musicSrv.setList(songList);
            musicSrv.setABReference(actionbar);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            musicBound = false;
        }
    };



    private void setController()
    {
        controller = new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(View v)
                                            {
                                                playNext();
                                            }
                                        },
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        playPrev();
                    }
                }
        );

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }
    private void playNext()
    {
        musicSrv.playNext();

        if (playbackPaused)
        {
            setController();
            playbackPaused = false;
        }

        controller.show(0);
    }

    private void playPrev()
    {
        musicSrv.playPrev();

        if (playbackPaused)
        {
            setController();
            playbackPaused = false;
        }

        controller.show(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_shuffle :
                boolean shuffleState = musicSrv.toggleShuffle();
                if (shuffleState)
                    item.setIcon(R.drawable.rand);
                else
                    item.setIcon(R.drawable.randoff);
                break;
            case R.id.action_end :
                stopService(playIntent);
                musicSrv = null;
                System.exit(0);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void songPicked(View view)
    {
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        Song current = musicSrv.playSong();
        if (playbackPaused)
        {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    public void getSongList()
    {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (null != musicCursor && musicCursor.moveToFirst())
        {
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

            do
            {
                long thisID = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisID, thisTitle, thisArtist));
            }
            while(musicCursor.moveToNext());
        }

    }

    @Override
    public void pause()
    {
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    @Override
    public void seekTo(int pos)
    {
        musicSrv.seek(pos);
    }

    @Override
    public void start()
    {
        musicSrv.startPlayer();
    }

    @Override
    public int getDuration()
    {
        if (musicSrv != null && musicBound && musicSrv.isPlaying())
            return musicSrv.getDuration();
        else
            return 0;
    }

    @Override
    public int getCurrentPosition()
    {
        if (musicSrv != null && musicBound && musicSrv.isPlaying())
            return musicSrv.getPosition();
        else
            return 0;
    }

    @Override
    public boolean isPlaying()
    {
        if (musicSrv != null && musicBound)
            return musicSrv.isPlaying();
        return false;
    }

    @Override
    public int getBufferPercentage()
    {
        return 0;
    }

    @Override
    public boolean canPause()
    {
        return true;
    }

    @Override
    public boolean canSeekBackward()
    {
        return true;
    }

    @Override
    public boolean canSeekForward()
    {
        return true;
    }

    @Override
    public int getAudioSessionId()
    {
        return 0;
    }




}
