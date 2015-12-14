package com.guo.duoduo.fastscrollbarapp.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.guo.duoduo.fastscrollbarapp.R;


/**
 * Created by 郭攀峰 on 2015/12/13.
 */
public class FastScrollLayout extends RelativeLayout
{
    private static final String tag = FastScrollLayout.class.getSimpleName();
    private static final float MAX_CLICK_DISTANCE = 5;
    private static final int MAX_CLICK_TIME = 300;
    private static final int MOVE_THREAD_HOLD = 20;
    private OnScrollBarScrolledListener onChangeFastScrollPlaceListener;
    private OnDragViewClick onDragViewClick;
    private TextView mDragView;
    private float currentY = 0;
    private float savedY = 0;
    private float downY = 0;
    private float downX = 0;
    private int barHeight = 0;
    private int viewHeight = 0;
    private long mPressStartTime;

    public FastScrollLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public void setCurrentPlace(float place)
    {
        //Log.d(tag, "set current place = " + place);
        currentY = (viewHeight - barHeight) * place;
        savedY = currentY;
        mDragView.setTranslationY(currentY);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        super.onLayout(changed, l, t, r, b);
        viewHeight = getHeight();
        barHeight = mDragView.getHeight();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (!isViewHit(mDragView, (int) event.getX(), (int) event.getY()))
        {
            switch (event.getAction())
            {
                case MotionEvent.ACTION_UP :
                    savedY = currentY;
                    if (onChangeFastScrollPlaceListener != null)
                    {
                        onChangeFastScrollPlaceListener.onState(false);
                    }
                    break;
            }
            return super.onTouchEvent(event);
        }

        float eventY = event.getY();
        float eventX = event.getX();
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN :
                currentY = savedY;
                downY = eventY;
                downX = eventX;
                break;

            case MotionEvent.ACTION_MOVE :
                mPressStartTime = System.currentTimeMillis();
                if (Math.abs(eventY - downY) > MOVE_THREAD_HOLD)
                {
                    if ((int) (savedY + eventY - downY) >= 0
                        && (int) (savedY + eventY - downY) <= (viewHeight - barHeight))
                    {
                        currentY = savedY + eventY - downY;
                        float percent = currentY / (viewHeight - barHeight);
                        if (onChangeFastScrollPlaceListener != null)
                        {
                            onChangeFastScrollPlaceListener.onScrollBarChanged(percent);
                            onChangeFastScrollPlaceListener.onState(true);
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_UP :
                savedY = currentY;
                if (onChangeFastScrollPlaceListener != null)
                {
                    onChangeFastScrollPlaceListener.onState(false);
                }
                //相当于点击效果
                long pressDuration = System.currentTimeMillis() - mPressStartTime;
                if (distance(downX, downY, event.getX(), event.getY()) < MAX_CLICK_DISTANCE
                    && pressDuration < MAX_CLICK_TIME)
                {
                    if (onDragViewClick != null)
                        onDragViewClick.dragViewClick();
                }
                break;
        }
        mDragView.setTranslationY(currentY);
        return true;
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();
        mDragView = (TextView) findViewById(R.id.drag_view);
    }

    private boolean isViewHit(View view, int x, int y)
    {
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);

        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;

        return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.getWidth()
            && screenY >= viewLocation[1] && screenY < viewLocation[1] + view.getHeight();
    }

    private double distance(float x1, float y1, float x2, float y2)
    {
        float deltaX = x2 - x1;
        float deltaY = y2 - y1;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    public void setOnChangeFastScrollPlace(OnScrollBarScrolledListener onChangeListener)
    {
        this.onChangeFastScrollPlaceListener = onChangeListener;
    }

    public interface OnScrollBarScrolledListener
    {
        public void onScrollBarChanged(float percent);

        public void onState(boolean isMoving);
    }

    public void setDragViewClickListener(OnDragViewClick listener)
    {
        this.onDragViewClick = listener;
    }

    public interface OnDragViewClick
    {
        public void dragViewClick();
    }
}
