package com.guo.duoduo.fastscrollbarapp.view;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.guo.duoduo.fastscrollbarapp.R;


/**
 * Created by 郭攀峰 on 2015/12/13.
 */
public class FastScrollBar extends View
{

    private static final String tag = FastScrollBar.class.getSimpleName();

    private OnChangeFastScrollPlaceListener onChangeFastScrollPlaceListener;

    private float currentY = 0;
    private float savedY = 0;
    private float downY = 0;

    private int barHeight = 0;
    private int barWidth = 0;
    private int viewHeight = 0;

    private Bitmap bitFastScroll;

    public FastScrollBar(Context context)
    {
        super(context);
    }

    public FastScrollBar(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public FastScrollBar(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    private void init()
    {
        bitFastScroll = BitmapFactory
                .decodeResource(getResources(), R.mipmap.ic_launcher);
        barHeight = bitFastScroll.getHeight();
        barWidth = bitFastScroll.getWidth();
        //Log.d(tag, " bar height =" + barHeight);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        if (viewHeight == 0)
        {
            viewHeight = getHeight();
        }
        //Log.d(tag, "viewHeight:" + viewHeight);
        Paint paint = new Paint();
        canvas.drawBitmap(bitFastScroll, 0, currentY, paint);
    }

    public void setCurrentPlace(float percent)
    {
        currentY = (viewHeight - barHeight) * percent;
        savedY = currentY;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        setMeasuredDimension(barWidth, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        float eventY = event.getY();
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN :
                currentY = savedY;
                downY = eventY;
                //Log.d(tag, "down:" + currentY);
                break;
            case MotionEvent.ACTION_MOVE :
                if ((int) (savedY + eventY - downY) >= 0
                    && (int) (savedY + eventY - downY) <= (viewHeight - barHeight))
                {
                    currentY = savedY + eventY - downY;

                    float percent = currentY / (viewHeight - barHeight);

                    //Log.d(tag, "percent:" + percent);

                    if (onChangeFastScrollPlaceListener != null)
                    {
                        onChangeFastScrollPlaceListener.onTouchingLetterChanged(percent);
                        onChangeFastScrollPlaceListener.onState(true);
                    }
                }
                Log.d(tag, "move:" + currentY);
                break;
            case MotionEvent.ACTION_UP :
                savedY = currentY;
                if (onChangeFastScrollPlaceListener != null)
                {
                    onChangeFastScrollPlaceListener.onState(false);
                }
                //Log.d(tag, "up:" + currentY);
                break;
        }

        invalidate();

        return true;
    }

    public void setOnChangeFastScrollPlace(
            OnChangeFastScrollPlaceListener onChangeListener)
    {
        this.onChangeFastScrollPlaceListener = onChangeListener;
    }

    public interface OnChangeFastScrollPlaceListener
    {
        public void onTouchingLetterChanged(float percent);

        public void onState(boolean isMoving);
    }
}
