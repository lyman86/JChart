package io.jchat.android.tools;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.model.UserInfo;
import cn.jpush.im.android.api.callback.GetUserInfoCallback;


/**
 * 鏈湴鍥剧墖鍔犺浇鍣�閲囩敤鐨勬槸寮傛瑙ｆ瀽鏈湴鍥剧墖锛屽崟渚嬫ā寮忓埄鐢╣etInstance()鑾峰彇NativeImageLoader瀹炰緥
 * 璋冪敤loadNativeImage()鏂规硶鍔犺浇鏈湴鍥剧墖锛屾绫诲彲浣滀负涓�釜鍔犺浇鏈湴鍥剧墖鐨勫伐鍏风被
 */
public class NativeImageLoader {
    private LruCache<String, Bitmap> mMemoryCache;
    private static NativeImageLoader mInstance = new NativeImageLoader();
    private ExecutorService mImageThreadPool = Executors.newFixedThreadPool(1);


    private NativeImageLoader() {
        //鑾峰彇搴旂敤绋嬪簭鐨勬渶澶у唴瀛�
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        //鐢ㄦ渶澶у唴瀛樼殑1/4鏉ュ瓨鍌ㄥ浘鐗�
        final int cacheSize = maxMemory / 4;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

            //鑾峰彇姣忓紶鍥剧墖鐨勫ぇ灏�
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
            }
        };
    }

    /**
     * 鍒濆鍖栫敤鎴峰ご鍍忕紦瀛�
     *
     * @param userIDList 鐢ㄦ埛ID List
     * @param length     澶村儚瀹介珮
     * @param callBack   缂撳瓨鍥炶皟
     */
    public void setAvatarCache(final List<String> userIDList, final int length, final cacheAvatarCallBack callBack) {
        final Handler handler = new Handler() {

            @Override
            public void handleMessage(android.os.Message msg) {
                super.handleMessage(msg);
                if (msg.getData() != null) {
                    callBack.onCacheAvatarCallBack(msg.getData().getInt("status", -1));
                }
            }
        };
        if(null == userIDList){
            return;
        }

        for (final String userID : userIDList) {
            //鑻ヤ负CurrentUser锛岀洿鎺ヨ幏鍙栨湰鍦扮殑澶村儚锛圕urrentUser鏈湴澶村儚涓烘渶鏂帮級
            if (userID.equals(JMessageClient.getMyInfo().getUserName())) {
                File file = JMessageClient.getMyInfo().getAvatarFile();
                if (file == null || !file.exists()) {
                    continue;
                } else {
                    Bitmap bitmap = BitmapLoader.getBitmapFromFile(file.getAbsolutePath(), length, length);
                    if (null != bitmap) {
                        mMemoryCache.put(userID, bitmap);
                    }
                    continue;
                }
            } else if (mMemoryCache.get(userID) != null) {
                continue;
            } else {
                JMessageClient.getUserInfo(userID, new GetUserInfoCallback(false) {
                    @Override
                    public void gotResult(int i, String s, UserInfo userInfo) {
                        if (i == 0) {
                            File file = userInfo.getAvatarFile();
                            if (file != null) {
                                Bitmap bitmap = BitmapLoader.getBitmapFromFile(file.getAbsolutePath(), length, length);
                                addBitmapToMemoryCache(userID, bitmap);
                            } else {
//                                Bitmap bitmap = BitmapLoader.getBitmapFromFile(getR.drawable.head_icon, length, length);
                            }
                            android.os.Message msg = handler.obtainMessage();
                            Bundle bundle = new Bundle();
                            bundle.putInt("status", 0);
                            msg.setData(bundle);
                            msg.sendToTarget();
                        }
                    }
                });
            }
        }


    }

    /**
     * setAvatarCache鐨勯噸杞藉嚱鏁�
     * @param userName
     * @param length
     * @param callBack
     */
    public void setAvatarCache(final String userName, final int length, final cacheAvatarCallBack callBack) {
        final Handler handler = new Handler() {

            @Override
            public void handleMessage(android.os.Message msg) {
                super.handleMessage(msg);
                if (msg.getData() != null) {
                    callBack.onCacheAvatarCallBack(msg.getData().getInt("status", -1));
                }
            }
        };

        JMessageClient.getUserInfo(userName, new GetUserInfoCallback(false) {
            @Override
            public void gotResult(int status, String desc, UserInfo userInfo) {
                if (status == 0) {
                    File file = userInfo.getAvatarFile();
                    if (file != null) {
                        Bitmap bitmap = BitmapLoader.getBitmapFromFile(file.getAbsolutePath(), length, length);
                        addBitmapToMemoryCache(userName, bitmap);
                    }
                    android.os.Message msg = handler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putInt("status", 0);
                    msg.setData(bundle);
                    msg.sendToTarget();
                }
            }
        });
    }

    /**
     * 姝ゆ柟娉曠洿鎺ュ湪LruCache涓鍔犱竴涓敭鍊煎
     *
     * @param targetID 鐢ㄦ埛鍚�
     * @param path     澶村儚璺緞
     */
    public void putUserAvatar(String targetID, String path, int size) {
        if (path != null) {
            Bitmap bitmap = BitmapLoader.getBitmapFromFile(path, size, size);
            if (bitmap != null) {
                addBitmapToMemoryCache(targetID, bitmap);
            }
        }
    }

    /**
     * 閫氳繃姝ゆ柟娉曟潵鑾峰彇NativeImageLoader鐨勫疄渚�
     *
     * @return
     */
    public static NativeImageLoader getInstance() {
        return mInstance;
    }


    /**
     * 姝ゆ柟娉曟潵鍔犺浇鏈湴鍥剧墖锛屾垜浠細鏍规嵁length鏉ヨ鍓狟itmap
     *
     * @param path 鍥剧墖璺緞
     * @param length 鍥剧墖瀹介珮
     * @param callBack 鍥炶皟
     * @return
     */
    public Bitmap loadNativeImage(final String path, final int length, final NativeImageCallBack callBack) {
        //鍏堣幏鍙栧唴瀛樹腑鐨凚itmap
        Bitmap bitmap = getBitmapFromMemCache(path);

        final Handler handler = new Handler() {

            @Override
            public void handleMessage(android.os.Message msg) {
                super.handleMessage(msg);
                callBack.onImageLoader((Bitmap) msg.obj, path);
            }

        };

        //鑻ヨBitmap涓嶅湪鍐呭瓨缂撳瓨涓紝鍒欏惎鐢ㄧ嚎绋嬪幓鍔犺浇鏈湴鐨勫浘鐗囷紝骞跺皢Bitmap鍔犲叆鍒癿MemoryCache涓�
        if (bitmap == null) {
            mImageThreadPool.execute(new Runnable() {

                @Override
                public void run() {
                    //鍏堣幏鍙栧浘鐗囩殑缂╃暐鍥�
                    Bitmap mBitmap = decodeThumbBitmapForFile(path, length, length);
                    Message msg = handler.obtainMessage();
                    msg.obj = mBitmap;
                    handler.sendMessage(msg);

                    //灏嗗浘鐗囧姞鍏ュ埌鍐呭瓨缂撳瓨
                    addBitmapToMemoryCache(path, mBitmap);
                }
            });
        }
        return bitmap;

    }

    /**
     * 姝ゆ柟娉曟潵鍔犺浇鏈湴鍥剧墖锛岃繖閲岀殑mPoint鏄敤鏉ュ皝瑁匢mageView鐨勫鍜岄珮锛屾垜浠細鏍规嵁ImageView鎺т欢鐨勫ぇ灏忔潵瑁佸壀Bitmap
     *
     * @param path
     * @param point
     * @param mCallBack
     * @return
     */
    public Bitmap loadNativeImage(final String path, final Point point, final NativeImageCallBack mCallBack) {
        //鍏堣幏鍙栧唴瀛樹腑鐨凚itmap
        Bitmap bitmap = getBitmapFromMemCache(path);

        final Handler mHander = new Handler() {

            @Override
            public void handleMessage(android.os.Message msg) {
                super.handleMessage(msg);
                mCallBack.onImageLoader((Bitmap) msg.obj, path);
            }

        };

        //鑻ヨBitmap涓嶅湪鍐呭瓨缂撳瓨涓紝鍒欏惎鐢ㄧ嚎绋嬪幓鍔犺浇鏈湴鐨勫浘鐗囷紝骞跺皢Bitmap鍔犲叆鍒癿MemoryCache涓�
        if (bitmap == null) {
            mImageThreadPool.execute(new Runnable() {

                @Override
                public void run() {
                    //鍏堣幏鍙栧浘鐗囩殑缂╃暐鍥�
                    Bitmap mBitmap = decodeThumbBitmapForFile(path, point.x == 0 ? 0 : point.x, point.y == 0 ? 0 : point.y);
                    Message msg = mHander.obtainMessage();
                    msg.obj = mBitmap;
                    mHander.sendMessage(msg);

                    //灏嗗浘鐗囧姞鍏ュ埌鍐呭瓨缂撳瓨
                    addBitmapToMemoryCache(path, mBitmap);
                }
            });
        }
        return bitmap;

    }


    /**
     * 寰�唴瀛樼紦瀛樹腑娣诲姞Bitmap
     *
     * @param key
     * @param bitmap
     */
    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null && bitmap != null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public void updateBitmapFromCache(String key, Bitmap bitmap) {
        if (null != bitmap) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public void releaseCache() {
        mMemoryCache.evictAll();
    }

    /**
     * 鏍规嵁key鏉ヨ幏鍙栧唴瀛樹腑鐨勫浘鐗�
     *
     * @param key
     * @return
     */
    public Bitmap getBitmapFromMemCache(String key) {
        if (key == null) {
            return null;
        } else {
            return mMemoryCache.get(key);
        }
    }


    /**
     * 鏍规嵁View(涓昏鏄疘mageView)鐨勫鍜岄珮鏉ヨ幏鍙栧浘鐗囩殑缂╃暐鍥�
     *
     * @param path
     * @param viewWidth
     * @param viewHeight
     * @return
     */
    private Bitmap decodeThumbBitmapForFile(String path, int viewWidth, int viewHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //璁剧疆涓簍rue,琛ㄧず瑙ｆ瀽Bitmap瀵硅薄锛岃瀵硅薄涓嶅崰鍐呭瓨
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        //璁剧疆缂╂斁姣斾緥
        options.inSampleSize = calculateInSampleSize(options, viewWidth, viewHeight);

        //璁剧疆涓篺alse,瑙ｆ瀽Bitmap瀵硅薄鍔犲叆鍒板唴瀛樹腑
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(path, options);
    }


    /**
     * 璁＄畻鍘嬬缉姣斾緥鍊�
     *
     * @param options   瑙ｆ瀽鍥剧墖鐨勯厤缃俊鎭�
     * @param reqWidth  鎵�渶鍥剧墖鍘嬬缉灏哄鏈�皬瀹藉害
     * @param reqHeight 鎵�渶鍥剧墖鍘嬬缉灏哄鏈�皬楂樺害
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // 淇濆瓨鍥剧墖鍘熷楂樺�
        final int height = options.outHeight;
        final int width = options.outWidth;

        // 鍒濆鍖栧帇缂╂瘮渚嬩负1
        int inSampleSize = 1;

        // 褰撳浘鐗囧楂樺�浠讳綍涓�釜澶т簬鎵�渶鍘嬬缉鍥剧墖瀹介珮鍊兼椂,杩涘叆寰幆璁＄畻绯荤粺
        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // 鍘嬬缉姣斾緥鍊兼瘡娆″惊鐜袱鍊嶅鍔�
            // 鐩村埌鍘熷浘瀹介珮鍊肩殑涓�崐闄や互鍘嬬缉鍊煎悗閮絶澶т簬鎵�渶瀹介珮鍊间负姝�
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


    /**
     * 鍔犺浇鏈湴鍥剧墖鐨勫洖璋冩帴鍙�
     *
     * @author xiaanming
     */
    public interface NativeImageCallBack {
        /**
         * 褰撳瓙绾跨▼鍔犺浇瀹屼簡鏈湴鐨勫浘鐗囷紝灏咮itmap鍜屽浘鐗囪矾寰勫洖璋冨湪姝ゆ柟娉曚腑
         *
         * @param bitmap
         * @param path
         */
        public void onImageLoader(Bitmap bitmap, String path);
    }

    public interface cacheAvatarCallBack {
        public void onCacheAvatarCallBack(int status);
    }
}
