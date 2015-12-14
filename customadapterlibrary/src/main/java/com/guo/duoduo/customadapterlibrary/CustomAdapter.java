package com.guo.duoduo.customadapterlibrary;


import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


/**
 * Created by 郭攀峰 on 2015/11/21.
 */
public abstract class CustomAdapter<T> extends BaseAdapter
{
    protected Context mContext;
    protected List<T> mList;
    protected int mResLayoutId;
    protected LayoutInflater mLayoutInflater;

    public CustomAdapter(Context context, int resLayoutId, List<T> list)
    {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
        this.mList = list;
        this.mResLayoutId = resLayoutId;
    }

    @Override
    public int getCount()
    {
        return mList.size();
    }

    @Override
    public Object getItem(int i)
    {
        return mList.get(i);
    }

    @Override
    public long getItemId(int i)
    {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {
        ViewHolder viewHolder = ViewHolder.getViewHolder(view, viewGroup, mResLayoutId,
            mLayoutInflater);
        convert(viewHolder, mList.get(i));

        return viewHolder.getConvertView();
    }

    public abstract void convert(ViewHolder viewHolder, T t);
}
