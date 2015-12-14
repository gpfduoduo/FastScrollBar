package com.guo.duoduo.customadapterlibrary;


import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by 郭攀峰 on 2015/11/21.
 */
public class ViewHolder
{
    SparseArray<View> mViews;
    View mConvertView;

    public ViewHolder(LayoutInflater layoutInflater, int resId, ViewGroup parent)
    {
        mViews = new SparseArray<>();
        mConvertView = layoutInflater.inflate(resId, parent, false);
        mConvertView.setTag(this);
    }

    public static ViewHolder getViewHolder(View convertView, ViewGroup parent, int resId,
            LayoutInflater layoutInflater)
    {
        if (convertView == null)
            return new ViewHolder(layoutInflater, resId, parent);
        return (ViewHolder) convertView.getTag();
    }

    public View getConvertView()
    {
        return mConvertView;
    }

    public <T extends View> T getView(int viewId)
    {
        View view = mViews.get(viewId);
        if (view == null)
        {
            view = mConvertView.findViewById(viewId);
            mViews.put(viewId, view);
        }
        return (T) view;
    }

    public TextView getTextView(int viewId)
    {
        return getView(viewId);
    }

    public ImageView getImageView(int viewId)
    {
        return getView(viewId);
    }
}
