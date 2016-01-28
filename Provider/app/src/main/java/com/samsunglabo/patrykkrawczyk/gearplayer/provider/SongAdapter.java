package com.samsunglabo.patrykkrawczyk.gearplayer.provider;

import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Capybara on 09/12/2015.
 */
public class SongAdapter extends android.widget.BaseAdapter
{
    private ArrayList<Song> songs;
    private LayoutInflater songInf;

    public SongAdapter(Context c, ArrayList<Song> list)
    {
        songs = list;
        songInf = LayoutInflater.from(c);
    }

    @Override
    public int getCount()
    {
        return songs.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LinearLayout songLayout = (LinearLayout)songInf.inflate(R.layout.song, parent, false);
        TextView titleView = (TextView)songLayout.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLayout.findViewById(R.id.song_artist);

        Song current = songs.get(position);

        titleView.setText(current.getTitle());
        artistView.setText(current.getArtist());

        songLayout.setTag(position);

        return songLayout;
    }

    @Override
    public Object getItem(int position)
    {
        return null;
    }

    @Override
    public long getItemId(int position)
    {
        return 0;
    }
}
