package io.jchat.android.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;



import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cn.jpush.im.android.api.model.Conversation;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.model.Message;
import cn.jpush.im.android.api.callback.DownloadCompletionCallback;
import cn.jpush.im.android.api.callback.ProgressUpdateCallback;
import cn.jpush.im.android.api.content.ImageContent;
import cn.jpush.im.android.api.enums.ContentType;
import cn.jpush.im.android.api.enums.MessageDirect;
import io.jchat.android.application.JPushDemoApplication;
import io.jchat.android.tools.BitmapLoader;
import io.jchat.android.tools.HandleResponseCode;
import io.jchat.android.view.ImgBrowserViewPager;
import io.jchat.android.view.photoview.PhotoView;

//鐢ㄤ簬娴忚鍥剧墖
public class BrowserViewPagerActivity extends BaseActivity {

    private static String TAG = BrowserViewPagerActivity.class.getSimpleName();
    private PhotoView photoView;
    private ImgBrowserViewPager mViewPager;
    private ProgressDialog mProgressDialog;
    //瀛樻斁鎵�湁鍥剧墖鐨勮矾寰�
    private List<String> mPathList = new ArrayList<String>();
    //瀛樻斁鍥剧墖娑堟伅鐨処D
    private List<Integer> mMsgIDList = new ArrayList<Integer>();
    private TextView mNumberTv;
    private Button mSendBtn;
    private CheckBox mOriginPictureCb;
    private TextView mTotalSizeTv;
    private CheckBox mPictureSelectedCb;
    private Button mLoadBtn;
    private int mPosition;
    private Conversation mConv;
    private Message mMsg;
    private String mTargetID;
    private boolean mFromChatActivity = true;
    private int mWidth;
    private int mHeight;
    private Context mContext;
    private boolean mDownloading = false;
    private boolean mIsGroup;
    private Long mGroupID;
    private int[] mMsgIDs;
    private final MyHandler myHandler = new MyHandler(this);
    /**
     * 鐢ㄦ潵瀛樺偍鍥剧墖鐨勯�涓儏鍐�
     */
    private SparseBooleanArray mSelectMap = new SparseBooleanArray();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageButton returnBtn;
        RelativeLayout titleBarRl, checkBoxRl;

        mContext = this;
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        mWidth = dm.widthPixels;
        mHeight = dm.heightPixels;
        Log.i(TAG, "width height :" + mWidth + mHeight);
        setContentView(R.layout.activity_image_browser);
        mViewPager = (ImgBrowserViewPager) findViewById(R.id.img_browser_viewpager);
        returnBtn = (ImageButton) findViewById(R.id.return_btn);
        mNumberTv = (TextView) findViewById(R.id.number_tv);
        mSendBtn = (Button) findViewById(R.id.pick_picture_send_btn);
        titleBarRl = (RelativeLayout) findViewById(R.id.title_bar_rl);
        checkBoxRl = (RelativeLayout) findViewById(R.id.check_box_rl);
        mOriginPictureCb = (CheckBox) findViewById(R.id.origin_picture_cb);
        mTotalSizeTv = (TextView) findViewById(R.id.total_size_tv);
        mPictureSelectedCb = (CheckBox) findViewById(R.id.picture_selected_cb);
        mLoadBtn = (Button) findViewById(R.id.load_image_btn);

        Intent intent = this.getIntent();
        mIsGroup = intent.getBooleanExtra("isGroup", false);
        if (mIsGroup) {
            mGroupID = intent.getLongExtra("groupID", 0);
            mConv = JMessageClient.getGroupConversation(mGroupID);
        } else {
            mTargetID = intent.getStringExtra("targetID");
            mConv = JMessageClient.getSingleConversation(mTargetID);
        }
        mPosition = intent.getIntExtra("position", 0);
        mFromChatActivity = intent.getBooleanExtra("fromChatActivity", true);
        boolean browserAvatar = intent.getBooleanExtra("browserAvatar", false);

        PagerAdapter pagerAdapter = new PagerAdapter() {

            @Override
            public int getCount() {
                return mPathList.size();
            }

            /**
             * 鐐瑰嚮鏌愬紶鍥剧墖棰勮鏃讹紝绯荤粺鑷姩璋冪敤姝ゆ柟娉曞姞杞借繖寮犲浘鐗囧乏鍙宠鍥撅紙濡傛灉鏈夌殑璇濓級
             */
            @Override
            public View instantiateItem(ViewGroup container, int position) {
                photoView = new PhotoView(mFromChatActivity, container.getContext());
                photoView.setTag(position);
                String path = mPathList.get(position);
                Bitmap bitmap = BitmapLoader.getBitmapFromFile(path, mWidth, mHeight);
                if (bitmap != null)
                    photoView.setImageBitmap(bitmap);
                else photoView.setImageResource(R.drawable.friends_sends_pictures_no);
                container.addView(photoView, LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT);
                return photoView;
            }

            @Override
            public int getItemPosition(Object object) {
                View view = (View) object;
                int currentPage = mViewPager.getCurrentItem();
                if (currentPage == (Integer) view.getTag()) {
                    return POSITION_NONE;
                } else {
                    return POSITION_UNCHANGED;
                }
            }

            @Override
            public void destroyItem(ViewGroup container, int position,
                                    Object object) {
                container.removeView((View) object);
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

        };
        mViewPager.setAdapter(pagerAdapter);
        mViewPager.setOnPageChangeListener(l);
        returnBtn.setOnClickListener(listener);
        mSendBtn.setOnClickListener(listener);
        mLoadBtn.setOnClickListener(listener);

        // 鍦ㄨ亰澶╃晫闈腑鐐瑰嚮鍥剧墖
        if (mFromChatActivity) {
            titleBarRl.setVisibility(View.GONE);
            checkBoxRl.setVisibility(View.GONE);
            if(mViewPager != null && mViewPager.getAdapter() != null){
                mViewPager.getAdapter().notifyDataSetChanged();
            }
            //棰勮澶村儚
            if (browserAvatar) {
                mPathList.add(intent.getStringExtra("avatarPath"));
                photoView = new PhotoView(mFromChatActivity, this);
                mLoadBtn.setVisibility(View.GONE);
                try {
                    photoView.setImageBitmap(BitmapLoader.getBitmapFromFile(mPathList.get(0), mWidth, mHeight));
                } catch (Exception e) {
                    photoView.setImageResource(R.drawable.friends_sends_pictures_no);
                }
            //棰勮鑱婂ぉ鐣岄潰涓殑鍥剧墖
            } else {
                initImgPathList();
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Toast.makeText(this, this.getString(R.string.local_picture_not_found_toast), Toast.LENGTH_SHORT).show();
                }
                mMsg = mConv.getMessage(intent.getIntExtra("msgID", 0));
                photoView = new PhotoView(mFromChatActivity, this);
                try {
                    ImageContent ic = (ImageContent) mMsg.getContent();
                    //濡傛灉鐐瑰嚮鐨勬槸绗竴寮犲浘鐗囧苟涓斿浘鐗囨湭涓嬭浇杩囷紝鍒欐樉绀哄ぇ鍥�
                    if (ic.getLocalPath() == null && mMsgIDList.indexOf(mMsg.getId()) == 0) {
                        downloadImage();
                    }
                    //濡傛灉鍙戦�鏂逛笂浼犱簡鍘熷浘
                    if(ic.getBooleanExtra("originalPicture")){
                        mLoadBtn.setVisibility(View.GONE);
                        setLoadBtnText(ic);
                    }
                    photoView.setImageBitmap(BitmapLoader.getBitmapFromFile(mPathList.get(mMsgIDList.indexOf(mMsg.getId())), mWidth, mHeight));
                    mViewPager.setCurrentItem(mMsgIDList.indexOf(mMsg.getId()));
                } catch (NullPointerException e) {
                    photoView.setImageResource(R.drawable.friends_sends_pictures_no);
                    mViewPager.setCurrentItem(mMsgIDList.indexOf(mMsg.getId()));
                }
            }
            // 鍦ㄩ�鎷╁浘鐗囨椂鐐瑰嚮棰勮鍥剧墖
        } else {
            mPathList = intent.getStringArrayListExtra("pathList");
            int[] pathArray = intent.getIntArrayExtra("pathArray");
            //鍒濆鍖栭�涓簡澶氬皯寮犲浘鐗�
            for (int i = 0; i < pathArray.length; i++) {
                if (pathArray[i] == 1) {
                    mSelectMap.put(i, true);
                }
            }
            showSelectedNum();
            mLoadBtn.setVisibility(View.GONE);
            mViewPager.setCurrentItem(mPosition);
            mNumberTv.setText(mPosition + 1 + "/" + mPathList.size());
            int currentItem = mViewPager.getCurrentItem();
            checkPictureSelected(currentItem);
            checkOriginPictureSelected();
            //绗竴寮犵壒娈婂鐞�
            mPictureSelectedCb.setChecked(mSelectMap.get(currentItem));
            showTotalSize();
        }
    }

    private void setLoadBtnText(ImageContent ic) {
        NumberFormat ddf1 = NumberFormat.getNumberInstance();
        //淇濈暀灏忔暟鐐瑰悗涓や綅
        ddf1.setMaximumFractionDigits(2);
        double size = ic.getFileSize() / 1048576.0;
        String fileSize = "(" + ddf1.format(size) + "M" + ")";
        mLoadBtn.setText(mContext.getString(R.string.load_origin_image) + fileSize);
    }

    /**
     * 鍦ㄥ浘鐗囬瑙堜腑鍙戦�鍥剧墖锛岀偣鍑婚�鎷〤heckBox鏃讹紝瑙﹀彂浜嬩欢
     *
     * @param currentItem 褰撳墠鍥剧墖绱㈠紩
     */
    private void checkPictureSelected(final int currentItem) {
        mPictureSelectedCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (mSelectMap.size() + 1 <= 9) {
                    if (isChecked)
                        mSelectMap.put(currentItem, true);
                    else mSelectMap.delete(currentItem);
                } else if (isChecked) {
                    Toast.makeText(mContext, mContext.getString(R.string.picture_num_limit_toast), Toast.LENGTH_SHORT).show();
                    mPictureSelectedCb.setChecked(mSelectMap.get(currentItem));
                } else {
                    mSelectMap.delete(currentItem);
                }

                showSelectedNum();
                showTotalSize();
            }
        });

    }

    /**
     * 鐐瑰嚮鍙戦�鍘熷浘CheckBox锛岃Е鍙戜簨浠�
     *
     */
    private void checkOriginPictureSelected() {
        mOriginPictureCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    if (mSelectMap.size() < 1)
                        mPictureSelectedCb.setChecked(true);
                }
            }
        });
    }

    //鏄剧ず閫変腑鐨勫浘鐗囨�鐨勫ぇ灏�
    private void showTotalSize() {
        if (mSelectMap.size() > 0) {
            List<String> pathList = new ArrayList<String>();
            for (int i=0; i < mSelectMap.size(); i++) {
                pathList.add(mPathList.get(mSelectMap.keyAt(i)));
            }
            String totalSize = BitmapLoader.getPictureSize(pathList);
            mTotalSizeTv.setText(mContext.getString(R.string.origin_picture) + "(" + totalSize + ")");
        } else mTotalSizeTv.setText(mContext.getString(R.string.origin_picture));
    }

    //鏄剧ず閫変腑浜嗗灏戝紶鍥剧墖
    private void showSelectedNum() {
        if (mSelectMap.size() > 0) {
            mSendBtn.setText(mContext.getString(R.string.send) + "(" + mSelectMap.size() + "/" + "9)");
        } else mSendBtn.setText(mContext.getString(R.string.send));
    }

    private ViewPager.OnPageChangeListener l = new ViewPager.OnPageChangeListener() {
        //鍦ㄦ粦鍔ㄧ殑鏃跺�鏇存柊CheckBox鐨勭姸鎬�
        @Override
        public void onPageScrolled(final int i, float v, int i2) {
            checkPictureSelected(i);
            checkOriginPictureSelected();

            mPictureSelectedCb.setChecked(mSelectMap.get(i));
        }

        @Override
        public void onPageSelected(final int i) {
            Log.i(TAG, "onPageSelected !");
            if (mFromChatActivity) {
                mMsg = mConv.getMessage(mMsgIDList.get(i));
                ImageContent ic = (ImageContent) mMsg.getContent();
                //姣忔閫夋嫨鎴栨粦鍔ㄥ浘鐗囷紝濡傛灉涓嶅瓨鍦ㄦ湰鍦板浘鐗囧垯涓嬭浇锛屾樉绀哄ぇ鍥�
                if (ic.getLocalPath() == null) {
//                    mLoadBtn.setVisibility(View.VISIBLE);
                    downloadImage();
                } else if(ic.getBooleanExtra("hasDownloaded") != null && !ic.getBooleanExtra("hasDownloaded")){
                    setLoadBtnText(ic);
                    mLoadBtn.setVisibility(View.GONE);
                }else {
                    mLoadBtn.setVisibility(View.GONE);
                }
            } else {
                mNumberTv.setText(i + 1 + "/" + mPathList.size());
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }
    };

    /**
     * 鍒濆鍖栦細璇濅腑鐨勬墍鏈夊浘鐗囪矾寰�
     */
    private void initImgPathList() {
        List<Message> msgList = mConv.getAllMessage();
        for (int i = 0; i < msgList.size(); i++) {
            Message msg = msgList.get(i);
            if (msg.getContentType().equals(ContentType.image)) {
                ImageContent ic = (ImageContent) msg.getContent();
                if (msg.getDirect().equals(MessageDirect.send))
                    mPathList.add(ic.getLocalPath());
                else if (ic.getLocalPath() != null) {
                    mPathList.add(ic.getLocalPath());
                } else mPathList.add(ic.getLocalThumbnailPath());
                mMsgIDList.add(msg.getId());
            }
        }
    }

    private OnClickListener listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.return_btn:
                    int pathArray[] = new int[mPathList.size()];
                    for (int i = 0; i < pathArray.length; i++)
                        pathArray[i] = 0;
                    for (int j = 0; j < mSelectMap.size(); j++) {
                        pathArray[mSelectMap.keyAt(j)] = 1;
                    }
                    Intent intent = new Intent();
                    intent.putExtra("pathArray", pathArray);
                    setResult(JPushDemoApplication.RESULTCODE_SELECT_PICTURE, intent);
                    finish();
                    break;
                case R.id.pick_picture_send_btn:
                    mProgressDialog = new ProgressDialog(mContext);
                    mProgressDialog.setMessage(mContext.getString(R.string.sending_hint));
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.show();
                    mPosition = mViewPager.getCurrentItem();
//                    pathList.add(mPathList.get(mPosition));

                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final List<String> pathList = new ArrayList<String>();
                            if (mOriginPictureCb.isChecked()) {
                                Log.i(TAG, "鍙戦�鍘熷浘");
                                mPictureSelectedCb.setChecked(true);
                                getOriginPictures(pathList, mPosition);
                            } else {
                                Log.i(TAG, "...");
                                getThumbnailPictures(pathList, mPosition);
                            }
                            myHandler.sendEmptyMessage(5);
                        }
                    });
                    thread.start();
                    break;
                //鐐瑰嚮鏄剧ず鍘熷浘鎸夐挳锛屼笅杞藉師鍥�
                case R.id.load_image_btn:
                    downloadOriginalPicture();
                    break;
            }
        }
    };

    private void downloadOriginalPicture() {
        final ImageContent imgContent = (ImageContent) mMsg.getContent();
        //濡傛灉涓嶅瓨鍦ㄤ笅杞借繘搴�
        if (!mMsg.isContentDownloadProgressCallbackExists()) {
            mMsg.setOnContentDownloadProgressCallback(new ProgressUpdateCallback() {
                @Override
                public void onProgressUpdate(double progress) {
                    android.os.Message msg = myHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    if (progress < 1.0) {
                        msg.what = 6;
                        bundle.putInt("progress", (int) (progress * 100));
                        msg.setData(bundle);
                        msg.sendToTarget();
                    } else {
                        msg.what = 7;
                        msg.sendToTarget();
                    }
                }
            });
            imgContent.downloadOriginImage(mMsg, new DownloadCompletionCallback() {
                @Override
                public void onComplete(int status, String desc, File file) {
                    if(status == 0){
                        imgContent.setBooleanExtra("hasDownloaded", true);
                    }else{
                        imgContent.setBooleanExtra("hasDownloaded", false);
                        android.os.Message msg = myHandler.obtainMessage();
                        msg.what = 4;
                        Bundle bundle = new Bundle();
                        bundle.putInt("status", status);
                        msg.setData(bundle);
                        msg.sendToTarget();
                    }
                }
            });
        }
    }

    private void createSendMsg(List<String> pathList) {
        mMsgIDs = new int[pathList.size()];
        for (int i = 0; i < pathList.size(); i++) {
            try {
                File file = new File(pathList.get(i));
                ImageContent content = new ImageContent(file);
                Message msg = mConv.createSendMessage(content);
                mMsgIDs[i] = msg.getId();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 鑾峰緱閫変腑鍥剧墖鐨勫師鍥捐矾寰�
     *
     * @param pathList 閫変腑鍥剧墖鐨勫師鍥捐矾寰�
     * @param position 閫変腑鐨勫浘鐗囦綅缃�
     */
    private void getOriginPictures(List<String> pathList, int position) {
        for (int i = 0; i < mSelectMap.size(); i++) {
            pathList.add(mPathList.get(mSelectMap.keyAt(i)));
        }
        if (pathList.size() < 1)
            pathList.add(mPathList.get(position));

//        createSendMsg(pathList);
        mMsgIDs = new int[pathList.size()];
        for (int i = 0; i < pathList.size(); i++){
            try {
                File file = new File(pathList.get(i));
                ImageContent content = new ImageContent(file);
                content.setBooleanExtra("originalPicture", true);
                Message msg = mConv.createSendMessage(content);
                mMsgIDs[i] = msg.getId();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 鑾峰緱閫変腑鍥剧墖鐨勭缉鐣ュ浘璺緞
     *
     * @param pathList 閫変腑鍥剧墖鐨勭缉鐣ュ浘璺緞
     * @param position 閫変腑鐨勫浘鐗囦綅缃�
     */
    private void getThumbnailPictures(List<String> pathList, int position) {
        String tempPath;
        Bitmap bitmap;
        if (mSelectMap.size() < 1)
            mSelectMap.put(position, true);
        for (int i = 0; i < mSelectMap.size(); i++) {
            //楠岃瘉鍥剧墖澶у皬锛岃嫢灏忎簬720 * 1280鍒欑洿鎺ュ彂閫佸師鍥撅紝鍚﹀垯鍘嬬缉
            if (BitmapLoader.verifyPictureSize(mPathList.get(mSelectMap.keyAt(i))))
                pathList.add(mPathList.get(mSelectMap.keyAt(i)));
            else {
                bitmap = BitmapLoader.getBitmapFromFile(mPathList.get(mSelectMap.keyAt(i)), 720, 1280);
                tempPath = BitmapLoader.saveBitmapToLocal(bitmap);
                pathList.add(tempPath);
            }
        }
        createSendMsg(pathList);
    }

    @Override
    protected void onDestroy() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mDownloading) {
            mProgressDialog.dismiss();
            //TODO cancel download image
        }
        int pathArray[] = new int[mPathList.size()];
        for (int i = 0; i < pathArray.length; i++)
            pathArray[i] = 0;
        for (int i = 0; i < mSelectMap.size(); i++) {
            pathArray[mSelectMap.keyAt(i)] = 1;
        }
        Intent intent = new Intent();
        intent.putExtra("pathArray", pathArray);
        setResult(JPushDemoApplication.RESULTCODE_SELECT_PICTURE, intent);
        super.onBackPressed();
    }

    //姣忔鍦ㄨ亰澶╃晫闈㈢偣鍑诲浘鐗囨垨鑰呮粦鍔ㄥ浘鐗囪嚜鍔ㄤ笅杞藉ぇ鍥�
    private void downloadImage() {
        ImageContent imgContent = (ImageContent) mMsg.getContent();
        if(imgContent.getLocalPath() == null){
            //濡傛灉涓嶅瓨鍦ㄨ繘搴︽潯Callback锛岄噸鏂版敞鍐�
            if (!mMsg.isContentDownloadProgressCallbackExists()) {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setMessage(mContext.getString(R.string.downloading_hint));
                mDownloading = true;
                mProgressDialog.show();
                // 鏄剧ず涓嬭浇杩涘害鏉�
                mMsg.setOnContentDownloadProgressCallback(new ProgressUpdateCallback() {

                    @Override
                    public void onProgressUpdate(double progress) {
                        android.os.Message msg = myHandler.obtainMessage();
                        Bundle bundle = new Bundle();
                        if (progress < 1.0) {
                            msg.what = 2;
                            bundle.putInt("progress", (int) (progress * 100));
                            msg.setData(bundle);
                            msg.sendToTarget();
                        } else {
                            msg.what = 3;
                            msg.sendToTarget();
                        }
                    }
                });
                // msg.setContent(imgContent);
                imgContent.downloadOriginImage(mMsg,
                        new DownloadCompletionCallback() {
                            @Override
                            public void onComplete(int status, String desc, File file) {
                                mDownloading = false;
                                if (status == 0) {
                                    android.os.Message msg = myHandler.obtainMessage();
                                    msg.what = 1;
                                    Bundle bundle = new Bundle();
                                    bundle.putString("path", file.getAbsolutePath());
                                    bundle.putInt("position",
                                            mViewPager.getCurrentItem());
                                    msg.setData(bundle);
                                    msg.sendToTarget();
                                } else {
                                    android.os.Message msg = myHandler.obtainMessage();
                                    msg.what = 4;
                                    Bundle bundle = new Bundle();
                                    bundle.putInt("status", status);
                                    msg.setData(bundle);
                                    msg.sendToTarget();
                                }
                            }
                        });
            }
        }
    }

    private static class MyHandler extends Handler{
        private final WeakReference<BrowserViewPagerActivity> mActivity;

        public MyHandler(BrowserViewPagerActivity activity){
            mActivity = new WeakReference<BrowserViewPagerActivity>(activity);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            BrowserViewPagerActivity activity = mActivity.get();
            if(activity != null){
                switch (msg.what) {
                    case 1:
                        //鏇存柊鍥剧墖骞舵樉绀�
                        Bundle bundle = msg.getData();
                        activity.mPathList.set(bundle.getInt("position"), bundle.getString("path"));
                        activity.mViewPager.getAdapter().notifyDataSetChanged();
                        activity.mLoadBtn.setVisibility(View.GONE);
                        break;
                    case 2:
                        activity.mProgressDialog.setProgress(msg.getData().getInt("progress"));
                        break;
                    case 3:
                        activity.mProgressDialog.dismiss();
                        break;
                    case 4:
                        if(activity.mProgressDialog != null){
                            activity.mProgressDialog.dismiss();
                        }
                        HandleResponseCode.onHandle(activity, msg.getData().getInt("status"), false);
                        break;
                    case 5:
                        Intent intent = new Intent();
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("sendPicture", true);
                        intent.putExtra("targetID", activity.mTargetID);
                        intent.putExtra("isGroup", activity.mIsGroup);
                        intent.putExtra("groupID", activity.mGroupID);
                        intent.putExtra("msgIDs", activity.mMsgIDs);
                        intent.setClass(activity, ChatActivity.class);
                        activity.startActivity(intent);
                        activity.finish();
                        break;
                    //鏄剧ず涓嬭浇鍘熷浘杩涘害
                    case 6:
                        activity.mLoadBtn.setText(msg.getData().getInt("progress") + "%");
                        break;
                    case 7:
                        activity.mLoadBtn.setText(activity.getString(R.string.download_completed_toast));
                        activity.mLoadBtn.setVisibility(View.GONE);
                        break;
                }
            }
        }
    }

}
