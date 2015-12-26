package com.guo.duoduo.fastscrollbarapp.entity;


public class GridItem
{
    private String path;
    private String time;
    private int section;
    private boolean isVideo = false;

    public GridItem(String path, String time)
    {
        super();
        this.path = path;
        this.time = time;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getTime()
    {
        return time;
    }

    public void setTime(String time)
    {
        this.time = time;
    }

    public int getSection()
    {
        return section;
    }

    public void setSection(int section)
    {
        this.section = section;
    }

    public boolean isVideo()
    {
        return isVideo;
    }

    public void setVideo(boolean video)
    {
        isVideo = video;
    }
}
