package com.guo.duoduo.fastscrollbarapp.utils;


import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import com.guo.duoduo.fastscrollbarapp.entity.GridItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class ImageScanner
{
    private static final String tag = ImageScanner.class.getSimpleName();
    private Context mContext;
    private List<GridItem> mImgList = new ArrayList<>();
    private List<GridItem> mVideoList = new ArrayList<>();

    public ImageScanner(Context context)
    {
        this.mContext = context;
    }

    public void scanImages(final ScanCompleteCallBack callback)
    {
        final Handler mHandler = new Handler()
        {

            @Override
            public void handleMessage(Message msg)
            {
                super.handleMessage(msg);
                callback.scanComplete((List<GridItem>) msg.obj);
            }
        };

        new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                Log.d(tag, "get Image start");
                Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver mContentResolver = mContext.getContentResolver();

                Cursor cursor = mContentResolver.query(mImageUri, null, null, null, null);

                if (cursor == null)
                    return;
                while (cursor.moveToNext())
                {
                    // 获取图片的路径
                    String path = cursor.getString(
                        cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    long times = cursor.getLong(
                        cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN));

                    GridItem mGridItem = new GridItem(path, parserTimeToYM(times));
                    mGridItem.setVideo(false);
                    mImgList.add(mGridItem);
                }
                cursor.close();
                Collections.sort(mImgList, new YMComparator());
                Log.d(tag, "get image end");
                Message msg = mHandler.obtainMessage();
                msg.obj = mImgList;
                mHandler.sendMessage(msg);
            }
        }).start();

        new Thread()
        {
            public void run()
            {
                Log.d(tag, "get video start");
                Cursor cursor = mContext.getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

                if (cursor == null)
                    return;
                while (cursor.moveToNext())
                {
                    String path = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                    long times = cursor.getLong(
                        cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN));

                    GridItem mGridItem = new GridItem(path, parserTimeToYM(times));
                    mGridItem.setVideo(true);
                    mVideoList.add(mGridItem);
                }
                cursor.close();
                Collections.sort(mVideoList, new YMComparator());

                Log.d(tag, "get video end");
                Message msg = mHandler.obtainMessage();
                msg.obj = mVideoList;
                mHandler.sendMessage(msg);
            }
        }.start();
    }

    private String parserTimeToYM(long time)
    {
        System.setProperty("user.timezone", "Asia/Shanghai");
        TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        TimeZone.setDefault(tz);
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日",
            Locale.getDefault());
        return format.format(new Date(time));
    }

    public static interface ScanCompleteCallBack
    {
        public void scanComplete(List<GridItem> list);
    }

}