package com.guo.duoduo.fastscrollbarapp.adapter;


import android.content.Context;
import android.graphics.Point;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.guo.duoduo.customadapterlibrary.CustomAdapter;
import com.guo.duoduo.customadapterlibrary.ScrollDirectionListener;
import com.guo.duoduo.customadapterlibrary.ViewHolder;
import com.guo.duoduo.fastscrollbarapp.R;
import com.guo.duoduo.fastscrollbarapp.entity.GridItem;
import com.guo.duoduo.fastscrollbarapp.utils.ImageLoader;
import com.guo.duoduo.fastscrollbarapp.view.MyImageView;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersSimpleAdapter;

import java.util.List;


/**
 * Created by 郭攀峰 on 2015/12/7.
 */
public class ImageAdapter extends CustomAdapter<GridItem>
    implements
        AbsListView.OnScrollListener,
        StickyGridHeadersSimpleAdapter
{
    private static final String tag = ImageAdapter.class.getSimpleName();

    private boolean mIsInit = false;
    private StickyGridHeadersGridView mStickyGridHeadersGridView;
    private Point mPoint = new Point(0, 0);
    private int mStartIndex, mEndIndex;
    private OnCustomScrollListener mOnCustomScrollListener;
    private int mLastScrollY;
    private int mPreviousFirstVisibleItem;
    private int mScrollThreshold = 150;

    private ScrollDirectionListener mScrollDirectionListener;

    public ImageAdapter(AbsListView view, Context context, int resLayoutId,
            List<GridItem> list)
    {
        super(context, resLayoutId, list);
        this.mStickyGridHeadersGridView = (StickyGridHeadersGridView) view;
        this.mStickyGridHeadersGridView.setOnScrollListener(this);
    }

    @Override
    public void convert(ViewHolder viewHolder, GridItem gridItem)
    {
        MyImageView mImageView = (MyImageView) viewHolder.getImageView(R.id.grid_item);
        mImageView.setImageResource(R.mipmap.friends_sends_pictures_no);
        ImageView imgVideo = (ImageView) viewHolder.getImageView(R.id.img_video);
        String path = gridItem.getPath();
        mImageView.setTag(path);

        if (gridItem.isVideo())
            imgVideo.setVisibility(View.VISIBLE);
        else
            imgVideo.setVisibility(View.GONE);
        if (!mIsInit)
        {
            ImageLoader.getInstance().loadImage(path, mImageView, false);
        }
    }

    public void addScrollDirectionListener(ScrollDirectionListener listener)
    {
        this.mScrollDirectionListener = listener;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState)
    {
        mIsInit = true;
        switch (scrollState)
        {
            case SCROLL_STATE_IDLE :
                loadImage();
                break;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount)
    {
        if (totalItemCount == 0)
            return;
        if (isSameRow(firstVisibleItem))
        {
            int newScrollY = getTopItemScrollY();
            boolean isSignificantDelta = Math
                    .abs(mLastScrollY - newScrollY) > mScrollThreshold;
            if (isSignificantDelta)
            {
                if (mLastScrollY > newScrollY)
                    onScrollUp();
                else
                    onScrollDown();
            }
        }
        else
        {
            if (firstVisibleItem > mPreviousFirstVisibleItem)
                onScrollUp();
            else
                onScrollDown();
        }
        mLastScrollY = getTopItemScrollY();
        mPreviousFirstVisibleItem = firstVisibleItem;

        mStartIndex = firstVisibleItem;
        mEndIndex = firstVisibleItem + visibleItemCount;
        if (mOnCustomScrollListener != null)
            mOnCustomScrollListener.onCustomScroll(view, firstVisibleItem,
                visibleItemCount, totalItemCount);
    }

    public void onScrollUp()
    {
        if (mScrollDirectionListener != null)
            mScrollDirectionListener.onScrollUp();
    }

    public void onScrollDown()
    {
        if (mScrollDirectionListener != null)
            mScrollDirectionListener.onScrollDown();
    }

    public void loadImage()
    {
        for (; mStartIndex < mEndIndex; mStartIndex++)
        {
            int realIndex = mStickyGridHeadersGridView.translatePosition(mStartIndex);
            if (realIndex < 0)
                continue;

            String path = mList.get(realIndex).getPath();
            ImageView img = (ImageView) mStickyGridHeadersGridView.findViewWithTag(path);
            ImageLoader.getInstance().loadImage(path, img, false);
        }
    }

    public void addOnCustomScrollListener(OnCustomScrollListener listener)
    {
        mOnCustomScrollListener = listener;
    }

    private boolean isSameRow(int firstVisibleItem)
    {
        return firstVisibleItem == mPreviousFirstVisibleItem;
    }

    private int getTopItemScrollY()
    {
        if (mStickyGridHeadersGridView == null
            || mStickyGridHeadersGridView.getChildAt(0) == null)
            return 0;
        View topChild = mStickyGridHeadersGridView.getChildAt(0);
        return topChild.getTop();
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent)
    {
        HeaderViewHolder mHeaderHolder;
        if (convertView == null)
        {
            mHeaderHolder = new HeaderViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.header, parent, false);
            mHeaderHolder.mTextView = (TextView) convertView.findViewById(R.id.header);
            convertView.setTag(mHeaderHolder);
        }
        else
        {
            mHeaderHolder = (HeaderViewHolder) convertView.getTag();
        }

        mHeaderHolder.mTextView.setText(mList.get(position).getTime());

        return convertView;
    }

    public interface OnCustomScrollListener
    {
        public void onCustomScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount);
    }

    public static class HeaderViewHolder
    {
        public TextView mTextView;
    }

    @Override
    public long getHeaderId(int position)
    {
        return mList.get(position).getSection();
    }
}
