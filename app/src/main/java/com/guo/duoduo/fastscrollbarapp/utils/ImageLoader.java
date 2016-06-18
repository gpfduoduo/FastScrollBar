package com.guo.duoduo.fastscrollbarapp.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ProgressBar;

/**
 * 本地图片加载类
 */
public class ImageLoader {
    private static final String tag = ImageLoader.class.getSimpleName();

    private static int threadCount = Runtime.getRuntime().availableProcessors() + 1;

    /**
     * 图片缓存的核心类
     */
    private LruCache<String, Bitmap> mLruCache;
    /**
     * 线程池
     */
    private ExecutorService mThreadPool;
    /**
     * 队列的调度方式
     */
    private Type mType = Type.LIFO;
    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTasks;
    /**
     * 轮询的线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHander;
    private Looper mLooper;

    /**
     * 运行在UI线程的handler，用于给ImageView设置图片
     */
    private Handler mHandler;

    /**
     * 引入一个值为1的信号量，防止mPoolThreadHander未初始化完成
     */
    private volatile Semaphore mSemaphore = new Semaphore(0);

    /**
     * 引入一个值为1的信号量，由于线程池内部也有一个阻塞线程，防止加入任务的速度过快，使LIFO效果不明显
     */
    private volatile Semaphore mPoolSemaphore;

    private static ImageLoader mInstance;

    /**
     * 队列的调度方式
     */
    public enum Type {
        FIFO, LIFO
    }

    private static final int GET_IMAGE = 0x110;
    private static final int EXIT = 0x111;


    public static ImageLoader getInstance() {
        return getInstance(threadCount, Type.FIFO);
    }


    public static ImageLoader getInstance(int threadCount, Type type) {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }


    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }


    private void init(int threadCount, Type type) {
        // loop thread
        mPoolThread = new Thread("JT_ImageLoader") {
            @Override public void run() {
                Looper.prepare();
                mLooper = Looper.myLooper();
                //XLog.d(tag, "loop thread run");
                mPoolThreadHander = new MyPoolThreadHandler(ImageLoader.this);
                // 释放一个信号量
                mSemaphore.release();
                Looper.loop();
            }
        };
        mPoolThread.start();

        //获取应用程序最大可用内存
        if (mLruCache != null) {
            mLruCache.evictAll();
            mLruCache = null;
        }

        mLruCache = new LruCache<String, Bitmap>((int) Runtime.getRuntime().maxMemory() / 4) {
            @Override protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);


            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r,
                        "JT_ImageLoader_ThreadPool #" + mCount.getAndIncrement());
                return thread;
            }
        };

        mThreadPool = Executors.newFixedThreadPool(threadCount, threadFactory);
        mPoolSemaphore = new Semaphore(threadCount);
        mTasks = new LinkedList<Runnable>();
        mType = type == null ? Type.LIFO : type;
    }


    /**
     * 重新设置缓存的大小
     *
     * @param size 单位字节
     */
    public void resetCacheSize(int size) {
        mLruCache.trimToSize(size);
    }


    public void clearCache() {
        if (mLruCache != null) mLruCache.evictAll();
    }


    /**
     * 加载图片
     */
    public void loadImage(final String path, final ImageView imageView) {
        if (TextUtils.isEmpty(path)) return;
        // set tag
        imageView.setTag(path);
        // UI线程
        if (mHandler == null) mHandler = new MyHandler();

        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null) {
            imageView.setImageBitmap(bm);
        }
        else {
            addTask(new Runnable() {
                @Override public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    try {
                        ImageSize imageSize = getImageViewWidth(imageView);

                        int reqWidth = imageSize.width;
                        int reqHeight = imageSize.height;

                        Bitmap bm = decodeSampledBitmapFromResource(path, reqWidth, reqHeight);

                        //XLog.d(tag, "run task path =" + path);
                        addBitmapToLruCache(path, bm);
                        ImgBeanHolder holder = new ImgBeanHolder();
                        holder.bitmap = bm; //getBitmapFromLruCache(path);
                        holder.imageView = new WeakReference<ImageView>(imageView);
                        holder.path = path;
                        holder.isBigImg = false;
                        Message message = Message.obtain();
                        message.obj = holder;
                        mHandler.sendMessage(message);
                        mPoolSemaphore.release();
                    } catch (OutOfMemoryError e) {
                        mPoolSemaphore.release();
                    } catch (IOException e) {
                        mPoolSemaphore.release();
                    }
                }
            });
        }
    }


    /**
     * 带有ProgressBar用来加载大图的时候调用
     */
    public void loadImage(final String path, final ProgressBar progressBar, final ImageView imageView) {
        // set tag
        imageView.setTag(path);
        progressBar.setTag(path);
        // UI线程
        if (mHandler == null) mHandler = new MyHandler();

        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null) {
            progressBar.setVisibility(View.GONE);
            imageView.setImageBitmap(bm);
        }
        else {
            addTask(new Runnable() {
                @Override public void run() {
                    try {
                        ImageSize imageSize = getImageViewWidth(imageView);

                        int reqWidth = imageSize.width;
                        int reqHeight = imageSize.height;

                        Bitmap bm = decodeSampledBitmapFromResource(path, reqWidth, reqHeight);

                        addBitmapToLruCache(path, bm);
                        ImgBeanHolder holder = new ImgBeanHolder();
                        holder.bitmap = bm;//getBitmapFromLruCache(path);
                        holder.imageView = new WeakReference<ImageView>(imageView);
                        holder.path = path;
                        holder.progressBar = new WeakReference<ProgressBar>(progressBar);
                        holder.isBigImg = true;
                        Message message = Message.obtain();
                        message.obj = holder;
                        mHandler.sendMessage(message);
                        mPoolSemaphore.release();
                    } catch (OutOfMemoryError e) {
                        mPoolSemaphore.release();
                    } catch (IOException e) {
                        mPoolSemaphore.release();
                    }
                }
            });
        }
    }


    /**
     * 添加一个任务
     */
    private synchronized void addTask(Runnable runnable) {
        try {
            // 请求信号量，防止mPoolThreadHander为null
            if (mPoolThreadHander == null) mSemaphore.acquire();
        } catch (InterruptedException e) {
        }
        mTasks.add(runnable);

        mPoolThreadHander.sendEmptyMessage(GET_IMAGE);
    }


    /**
     * 退出
     */
    public void quit() {
        if (mPoolThreadHander != null) mPoolThreadHander.sendEmptyMessage(EXIT);
    }


    /**
     * 取出一个任务
     */
    private synchronized Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTasks.removeFirst();
        }
        else if (mType == Type.LIFO) {
            return mTasks.removeLast();
        }
        return null;
    }


    /**
     * 从LruCache中获取一张图片，如果不存在就返回null。
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }


    /**
     * 往LruCache中添加一张图片
     */
    private void addBitmapToLruCache(String key, Bitmap bitmap) {
        if (getBitmapFromLruCache(key) == null) {
            if (bitmap != null) mLruCache.put(key, bitmap);
        }
    }


    private Bitmap getVideoThumbNail(String filepath) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        Bitmap bitmap = null;
        //XLog.d(tag, "get video thumbnail = " + filepath);
        try {
            mmr.setDataSource(filepath);
            bitmap = mmr.getFrameAtTime();
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, 80, 80,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        mmr.release();

        return bitmap;
    }


    /**
     * 计算inSampleSize，用于压缩图片
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;
        if (width > reqWidth || height > reqHeight) {
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);
            inSampleSize = Math.max(widthRadio, heightRadio);
        }
        return inSampleSize;
    }


    /**
     * 根据ImageView获得适当的压缩的宽和高
     */
    private ImageSize getImageViewWidth(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();

        LayoutParams lp = imageView.getLayoutParams();

        int width = imageView.getWidth();// 获取imageview的实际宽度
        if (width <= 0) {
            width = lp.width;// 获取imageview在layout中声明的宽度
        }
        if (width <= 0) {
            //width = imageView.getMaxWidth();// 检查最大值
            width = getImageViewFieldValue(imageView, "mMaxWidth");
        }
        if (width <= 0) {
            width = displayMetrics.widthPixels;
        }

        int height = imageView.getHeight();// 获取imageview的实际高度
        if (height <= 0) {
            height = lp.height;// 获取imageview在layout中声明的宽度
        }
        if (height <= 0) {
            height = getImageViewFieldValue(imageView, "mMaxHeight");// 检查最大值
        }
        if (height <= 0) {
            height = displayMetrics.heightPixels;
        }
        imageSize.width = width;
        imageSize.height = height;

        return imageSize;
    }


    /**
     * 反射获得ImageView设置的最大宽度和高度
     */
    private static int getImageViewFieldValue(Object object, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = (Integer) field.get(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {
        }
        return value;
    }


    /**
     * 根据计算的inSampleSize，得到压缩后图片
     */
    private Bitmap decodeSampledBitmapFromResource(String pathName, int reqWidth, int reqHeight)
            throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        BitmapFactory.decodeFile(pathName, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        //Bitmap bitmap = BitmapFactory.decodeFile(pathName, options);
        Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(pathName), null, options);

        return bitmap;
    }


    private class ImgBeanHolder {
        Bitmap bitmap;
        WeakReference<ProgressBar> progressBar;
        WeakReference<ImageView> imageView;
        String path;
        boolean isBigImg = false;
    }

    private class ImageSize {
        int width;
        int height;
    }

    /**
     * 处理得到的图像
     */
    private static class MyHandler extends Handler {
        @Override public void handleMessage(Message msg) {
            ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
            if (holder.imageView == null) {
                return;
            }

            ImageView imageView = holder.imageView.get();
            if (imageView == null) {
                return;
            }

            Bitmap bm = holder.bitmap;
            String path = holder.path;
            if (imageView.getTag().toString().equals(path)) {
                if (bm == null) {
                }
                else {
                    imageView.setImageBitmap(bm);
                    imageView.setBackgroundResource(android.R.color.transparent);
                }
            }
            else {
            }

            if (holder.isBigImg) {
                if (holder.progressBar == null) return;
                ProgressBar progressBar = holder.progressBar.get();
                if (progressBar == null) return;

                if (progressBar.getTag().toString().equals(path)) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        }
    }

    private static class MyPoolThreadHandler extends Handler {

        WeakReference<ImageLoader> weakReference;


        public MyPoolThreadHandler(ImageLoader imageLoader) {
            weakReference = new WeakReference<ImageLoader>(imageLoader);
        }


        @Override public void handleMessage(Message msg) {
            if (msg == null) return;
            if (weakReference.get() == null) return;

            switch (msg.what) {
                case GET_IMAGE:
                    weakReference.get().mThreadPool.execute(weakReference.get().getTask());
                    try {
                        weakReference.get().mPoolSemaphore.acquire();
                    } catch (InterruptedException e) {
                    }
                    break;
                case EXIT:
                    weakReference.get().mLooper.quit();
                    weakReference.get().clearCache();
                    if (weakReference.get() != null) {
                        weakReference.get().mPoolThread.interrupt();
                        weakReference.get().mPoolThread = null;
                        if (weakReference.get().mPoolThreadHander != null) {
                            weakReference.get().mPoolThreadHander.removeCallbacksAndMessages(null);
                        }
                        if (weakReference.get().mThreadPool != null) {
                            weakReference.get().mThreadPool.shutdown();
                        }
                        if (weakReference.get().mHandler != null) {
                            weakReference.get().mHandler.removeCallbacksAndMessages(null);
                        }
                        if (weakReference.get().mTasks != null) weakReference.get().mTasks.clear();
                    }
                    mInstance = null;
                    break;
            }
        }


        ;
    }
}
