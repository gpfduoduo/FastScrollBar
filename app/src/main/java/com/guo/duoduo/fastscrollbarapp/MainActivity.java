package com.guo.duoduo.fastscrollbarapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.GridView;

import android.widget.RelativeLayout;
import com.guo.duoduo.fastscrollbarapp.adapter.ImageAdapter;
import com.guo.duoduo.fastscrollbarapp.entity.GridItem;
import com.guo.duoduo.fastscrollbarapp.utils.ImageScanner;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String tag = MainActivity.class.getSimpleName();
    private ProgressDialog mProgressDialog;
    private ImageScanner mScanner;
    private GridView mGridView;
    private RelativeLayout mFastScrollLayout;
    private List<GridItem> mGirdList = new ArrayList<>();
    private ImageAdapter imageAdapter;
    private float lastY;
    private float currentY;
    private int mStatusHeight;
    private int screenHeight, slidviewHeight;
    private boolean isSlidMoveing = false;
    private boolean isGridViewMoveing = false;
    private int mStart, mEnd;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mGridView = (GridView) findViewById(R.id.asset_grid);
        imageAdapter = new ImageAdapter(mGridView, MainActivity.this, R.layout.grid_item,
                mGirdList);
        mGridView.setAdapter(imageAdapter);
        mScanner = new ImageScanner(this);
        mScanner.scanImages(new ImageScanner.ScanCompleteCallBack() {
            {
                mProgressDialog = ProgressDialog.show(MainActivity.this, null, "正在加载...");
            }


            @Override public void scanComplete(List<GridItem> list) {
                // 关闭进度条
                mProgressDialog.dismiss();

                mGirdList.addAll(list);
                imageAdapter.notifyDataSetChanged();
            }
        });

        imageAdapter.addOnCustomScrollListener(new ImageAdapter.OnCustomScrollListener() {
            @Override
            public void onCustomScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                float percent = (float) firstVisibleItem / (totalItemCount - visibleItemCount);

                mStart = firstVisibleItem;
                mEnd = firstVisibleItem + visibleItemCount;

                if (!isSlidMoveing) {
                    mFastScrollLayout.setTranslationY((screenHeight - slidviewHeight) * percent);
                }
            }


            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isGridViewMoveing = true;
                    isSlidMoveing = false;
                }
            }
        });

        mFastScrollLayout = (RelativeLayout) findViewById(R.id.fast_layout);
        mFastScrollLayout.setOnTouchListener(onTouchListener);

        mStatusHeight = getStatusBarHeight(getApplicationContext()) +
                (int) getResources().getDimension(R.dimen.titlebar_height);
        screenHeight = getScreenHeight() - mStatusHeight;
        slidviewHeight = (int) getResources().getDimension(R.dimen.y60);
    }


    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    currentY = event.getRawY();
                    lastY = currentY;
                    return true;
                case MotionEvent.ACTION_UP:
                    if (mFastScrollLayout.getY() <= 0) mFastScrollLayout.setY(0f);
                    if (mFastScrollLayout.getY() > screenHeight - slidviewHeight) {
                        mFastScrollLayout.setY(screenHeight - slidviewHeight);
                    }
                    imageAdapter.loadImage(mStart, mEnd);
                case MotionEvent.ACTION_MOVE:
                    return processMove(event);
            }
            return false;
        }
    };


    private boolean processMove(MotionEvent event) {
        if (event.getPointerCount() != 1) return false;

        isSlidMoveing = true;
        isGridViewMoveing = false;
        currentY = event.getRawY();
        float deltaY = currentY - lastY;
        lastY = currentY;

        if (mFastScrollLayout.getY() >= 0 &&
                mFastScrollLayout.getY() <= (screenHeight - slidviewHeight)) {
            mFastScrollLayout.setY(deltaY + mFastScrollLayout.getY());
            float percent = (deltaY + mFastScrollLayout.getY()) / (screenHeight - slidviewHeight);

            if (isGridViewMoveing) return false;

            mGridView.setSelection((int) ((mGridView.getCount() -
                    mGridView.getHeight() / mGridView.getChildAt(0).getHeight()) * percent));
        }
        return false;
    }


    private int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources()
                                .getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) result = context.getResources().getDimensionPixelSize(resourceId);

        return result;
    }


    private int getScreenHeight() {
        Display display = ((WindowManager) getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point screen = new Point();
        display.getSize(screen);
        return Math.max(screen.x, screen.y);
    }
}
