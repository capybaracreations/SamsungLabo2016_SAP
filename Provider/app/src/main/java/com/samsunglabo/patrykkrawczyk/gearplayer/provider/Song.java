package com.samsunglabo.patrykkrawczyk.gearplayer.provider;

/**
 * Created by Capybara on 09/12/2015.
 */
public class Song
{
    private long id;
    private String title;
    private String artist;

    Song(long i, String t, String a)
    {
        id = i;
        title = t;
        artist = a;
    }

    public long getID() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }

}
