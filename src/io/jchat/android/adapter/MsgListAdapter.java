package io.jchat.android.adapter;

import io.jchat.android.activity.BrowserViewPagerActivity;
import io.jchat.android.activity.FriendInfoActivity;
import io.jchat.android.activity.MeInfoActivity;
import io.jchat.android.activity.R;
import io.jchat.android.tools.BitmapLoader;
import io.jchat.android.tools.HandleResponseCode;
import io.jchat.android.tools.NativeImageLoader;
import io.jchat.android.tools.TimeFormat;
import io.jchat.android.view.CircleImageView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.callback.DownloadAvatarCallback;
import cn.jpush.im.android.api.callback.DownloadCompletionCallback;
import cn.jpush.im.android.api.callback.GetUserInfoCallback;
import cn.jpush.im.android.api.callback.ProgressUpdateCallback;
import cn.jpush.im.android.api.content.EventNotificationContent;
import cn.jpush.im.android.api.content.ImageContent;
import cn.jpush.im.android.api.content.TextContent;
import cn.jpush.im.android.api.content.VoiceContent;
import cn.jpush.im.android.api.enums.ContentType;
import cn.jpush.im.android.api.enums.MessageDirect;
import cn.jpush.im.android.api.model.Conversation;
import cn.jpush.im.android.api.model.GroupInfo;
import cn.jpush.im.android.api.model.Message;
import cn.jpush.im.android.api.model.UserInfo;
import cn.jpush.im.api.BasicCallback;

import com.squareup.picasso.Picasso;

@SuppressLint("NewApi")
public class MsgListAdapter extends BaseAdapter {

    private static final String TAG = "MsgListAdapter";

    private Context mContext;
    private String mTargetID;
    private Conversation mConv;
    private List<Message> mMsgList = new ArrayList<Message>();//鎵�湁娑堟伅鍒楄〃
    private List<Integer> mIndexList = new ArrayList<Integer>();//璇煶绱㈠紩
    private LayoutInflater mInflater;
    private boolean mSetData = false;
    private boolean mIsGroup = false;
    private long mGroupID;
    private int mPosition = -1;// 鍜宮SetData涓�捣缁勬垚鍒ゆ柇鎾斁鍝潯褰曢煶鐨勪緷鎹�
    private static final int UPDATE_IMAGEVIEW = 1999;
    private final int UPDATE_PROGRESS = 1998;
    // 9绉岻tem鐨勭被鍨�
    // 鏂囨湰
    private final int TYPE_RECEIVE_TXT = 0;
    private final int TYPE_SEND_TXT = 1;
    // 鍥剧墖
    private final int TYPE_SEND_IMAGE = 2;
    private final int TYPE_RECEIVER_IMAGE = 3;
    // 浣嶇疆
    private final int TYPE_SEND_LOCATION = 4;
    private final int TYPE_RECEIVER_LOCATION = 5;
    // 璇煶
    private final int TYPE_SEND_VOICE = 6;
    private final int TYPE_RECEIVER_VOICE = 7;
    //缇ゆ垚鍛樺彉鍔�
    private final int TYPE_GROUP_CHANGE = 8;
    //鑷畾涔夋秷鎭�
    private final int TYPE_CUSTOM_TXT = 9;
    private final MediaPlayer mp = new MediaPlayer();
    private AnimationDrawable mVoiceAnimation;
    private FileInputStream mFIS;
    private FileDescriptor mFD;
    private Activity mActivity;
    private final MyHandler myHandler = new MyHandler(this);
    private boolean autoPlay = false;
    private int mWidth;
    private int nextPlayPosition = 0;
    private double mDensity;
    private boolean mIsEarPhoneOn;
    private GroupInfo mGroupInfo;

    public MsgListAdapter(Context context, String targetID){
        initData(context);
        this.mTargetID = targetID;
        this.mConv = JMessageClient.getSingleConversation(mTargetID);
        this.mMsgList = mConv.getAllMessage();
        List<String> userIDList = new ArrayList<String>();
        userIDList.add(targetID);
        userIDList.add(JMessageClient.getMyInfo().getUserName());
        NativeImageLoader.getInstance().setAvatarCache(userIDList, (int) (50 * mDensity), new NativeImageLoader.cacheAvatarCallBack() {
            @Override
            public void onCacheAvatarCallBack(int status) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "init avatar succeed");
                        notifyDataSetChanged();
                    }
                });
            }
        });
    }

    public MsgListAdapter(Context context, long groupID, GroupInfo groupInfo){
        initData(context);
        this.mGroupID = groupID;
        this.mIsGroup = true;
        this.mConv = JMessageClient.getGroupConversation(groupID);
        this.mMsgList = mConv.getAllMessage();
        this.mGroupInfo = groupInfo;
    }

    private void initData(Context context){
        this.mContext = context;
        mActivity = (Activity) context;
        DisplayMetrics dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        mDensity = dm.density;
        mWidth = dm.widthPixels;
        mInflater = LayoutInflater.from(mContext);
        AudioManager audioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        if(audioManager.isSpeakerphoneOn()){
            audioManager.setSpeakerphoneOn(true);
        }else audioManager.setSpeakerphoneOn(false);
        mp.setAudioStreamType(AudioManager.STREAM_RING);
        mp.setOnErrorListener(new OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
    }

    public void initMediaPlayer(){
        mp.reset();
    }

    public void setSendImg(int[] msgIDs) {
        for (int i = 0; i < msgIDs.length; i++) {
            JMessageClient.sendMessage(mConv.getMessage(msgIDs[i]));
        }
        notifyDataSetChanged();
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public void refresh() {
        mMsgList.clear();
        if (mIsGroup) {
            mConv = JMessageClient.getGroupConversation(mGroupID);
        } else {
            mConv = JMessageClient.getSingleConversation(mTargetID);
        }
        if (null != mConv) {
            mMsgList = mConv.getAllMessage();
            notifyDataSetChanged();
        }
    }

    public void releaseMediaPlayer() {
        if (mp != null)
            mp.release();
    }

    public void addMsgToList(Message msg) {
        mMsgList.add(msg);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getCount() {
        return mMsgList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = mMsgList.get(position);
        //鏄枃瀛楃被鍨嬫垨鑰呰嚜瀹氫箟绫诲瀷锛堢敤鏉ユ樉绀虹兢鎴愬憳鍙樺寲娑堟伅锛�
        if (msg.getContentType().equals(ContentType.text)) {
            return msg.getDirect().equals(MessageDirect.send) ? TYPE_SEND_TXT
                    : TYPE_RECEIVE_TXT;
        } else if (msg.getContentType().equals(ContentType.image)) {
            return msg.getDirect().equals(MessageDirect.send) ? TYPE_SEND_IMAGE
                    : TYPE_RECEIVER_IMAGE;
        } else if (msg.getContentType().equals(ContentType.voice)) {
            return msg.getDirect().equals(MessageDirect.send) ? TYPE_SEND_VOICE
                    : TYPE_RECEIVER_VOICE;
        } else if (msg.getContentType().equals(ContentType.eventNotification)) {
            return TYPE_GROUP_CHANGE;
        }else if(msg.getContentType().equals(ContentType.location)) {
            return msg.getDirect().equals(MessageDirect.send) ? TYPE_SEND_LOCATION
                    : TYPE_RECEIVER_LOCATION;
        }else {
            return TYPE_CUSTOM_TXT;
        }
    }

    public int getViewTypeCount() {
        return 11;
    }

    private View createViewByType(Message msg, int position) {
        // 浼氳瘽绫诲瀷
        switch (msg.getContentType()) {
            case image:
                return getItemViewType(position) == TYPE_SEND_IMAGE ? mInflater
                        .inflate(R.layout.chat_item_send_image, null) : mInflater
                        .inflate(R.layout.chat_item_receive_image, null);
            case voice:
                return getItemViewType(position) == TYPE_SEND_VOICE ? mInflater
                        .inflate(R.layout.chat_item_send_voice, null) : mInflater
                        .inflate(R.layout.chat_item_receive_voice, null);
            case location:
                return getItemViewType(position) == TYPE_SEND_LOCATION ? mInflater
                        .inflate(R.layout.chat_item_send_location, null)
                        : mInflater.inflate(R.layout.chat_item_receive_location,
                        null);
            case eventNotification:
                if (getItemViewType(position) == TYPE_GROUP_CHANGE)
                    return mInflater.inflate(R.layout.chat_item_group_change, null);
            case text:
                return getItemViewType(position) == TYPE_SEND_TXT ? mInflater
                        .inflate(R.layout.chat_item_send_text, null) : mInflater
                        .inflate(R.layout.chat_item_receive_text, null);
            default:
                return mInflater.inflate(R.layout.chat_item_group_change, null);
        }
    }

    @Override
    public Message getItem(int position) {
        return mMsgList.get(position);
    }

    public void setAudioPlayByEarPhone(int state) {
        AudioManager audioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        int currVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        if(state == 0){
            mIsEarPhoneOn = false;
            audioManager.setSpeakerphoneOn(true);
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                    AudioManager.STREAM_VOICE_CALL);
            Log.i(TAG, "set SpeakerphoneOn true!");
        }else {
            mIsEarPhoneOn = true;
            audioManager.setSpeakerphoneOn(false);
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, currVolume,
                    AudioManager.STREAM_VOICE_CALL);
            Log.i(TAG, "set SpeakerphoneOn false!");
        }
    }

    public void refreshGroupInfo(GroupInfo groupInfo) {
        mGroupInfo = groupInfo;
        notifyDataSetChanged();
    }

    private static class MyHandler extends Handler{
        private final WeakReference<MsgListAdapter> mAdapter;

        public MyHandler(MsgListAdapter adapter){
            mAdapter = new WeakReference<MsgListAdapter>(adapter);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            MsgListAdapter adapter = mAdapter.get();
            if(adapter != null){
                switch (msg.what) {
                    case UPDATE_IMAGEVIEW:
                        Bundle bundle = msg.getData();
                        ViewHolder holder = (ViewHolder) msg.obj;
                        String path = bundle.getString("path");
                        Picasso.with(adapter.mContext).load(new File(path)).into(holder.picture);
                        adapter.refresh();
                        Log.i(TAG, "Refresh Received picture");
                        break;
                }
            }
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Message msg = mMsgList.get(position);
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            ContentType contentType = msg.getContentType();
            convertView = createViewByType(msg, position);
            if (contentType.equals(ContentType.text)) {
                try {
                    holder.headIcon = (CircleImageView) convertView
                            .findViewById(R.id.avatar_iv);
                    holder.displayName = (TextView) convertView
                            .findViewById(R.id.display_name_tv);
                    holder.txtContent = (TextView) convertView
                            .findViewById(R.id.msg_content);
                    holder.sendingIv = (ImageView) convertView
                            .findViewById(R.id.sending_iv);
                    holder.resend = (ImageButton) convertView
                            .findViewById(R.id.fail_resend_ib);
                    holder.groupChange = (TextView) convertView
                            .findViewById(R.id.group_content);
                } catch (Exception e) {
                }
            } else if (contentType.equals(ContentType.image)) {
                try {
                    holder.headIcon = (CircleImageView) convertView
                            .findViewById(R.id.avatar_iv);
                    holder.displayName = (TextView) convertView
                            .findViewById(R.id.display_name_tv);
                    holder.picture = (ImageView) convertView
                            .findViewById(R.id.picture_iv);
                    holder.sendingIv = (ImageView) convertView
                            .findViewById(R.id.sending_iv);
                    holder.progressTv = (TextView) convertView
                            .findViewById((R.id.progress_tv));
                    holder.resend = (ImageButton) convertView
                            .findViewById(R.id.fail_resend_ib);
                } catch (Exception e) {
                }
            } else if (contentType.equals(ContentType.voice)) {
                try {
                    holder.headIcon = (CircleImageView) convertView
                            .findViewById(R.id.avatar_iv);
                    holder.displayName = (TextView) convertView
                            .findViewById(R.id.display_name_tv);
                    holder.txtContent = (TextView) convertView
                            .findViewById(R.id.msg_content);
                    holder.voice = ((ImageView) convertView
                            .findViewById(R.id.voice_iv));
                    holder.sendingIv = (ImageView) convertView
                            .findViewById(R.id.sending_iv);
                    holder.voiceLength = (TextView) convertView
                            .findViewById(R.id.voice_length_tv);
                    holder.readStatus = (ImageView) convertView
                            .findViewById(R.id.read_status_iv);
                    holder.resend = (ImageButton) convertView
                            .findViewById(R.id.fail_resend_ib);
                } catch (Exception e) {
                }
            } else if (contentType.equals(ContentType.eventNotification)) {
                try {
                    holder.groupChange = (TextView) convertView
                            .findViewById(R.id.group_content);
                } catch (Exception e) {
                }
            } else if(contentType.equals(ContentType.location)) {
                try {
                    holder.headIcon = (CircleImageView) convertView
                            .findViewById(R.id.avatar_iv);
                    holder.displayName = (TextView) convertView
                            .findViewById(R.id.display_name_tv);
                    holder.txtContent = (TextView) convertView
                            .findViewById(R.id.msg_content);
                    holder.sendingIv = (ImageView) convertView
                            .findViewById(R.id.sending_iv);
                    holder.resend = (ImageButton) convertView
                            .findViewById(R.id.fail_resend_ib);
                } catch (Exception e) {
                }
            }else {
                try {
                    holder.groupChange = (TextView) convertView
                            .findViewById(R.id.group_content);
                } catch (Exception e) {
                }
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        switch (msg.getContentType()) {
            case text:
                handleTextMsg(msg, holder, position);
                break;
            case image:
                handleImgMsg(msg, holder, position);
                break;
            case voice:
                handleVoiceMsg(msg, holder, position);
                break;
            case location:
                handleLocationMsg(msg, holder, position);
                break;
            case eventNotification:
                handleGroupChangeMsg(msg, holder);
                break;
            default:
                handleCustomMsg(holder);
        }
        //鏄剧ず鏃堕棿
        TextView msgTime = (TextView) convertView
                .findViewById(R.id.send_time_txt);
        long nowDate = msg.getCreateTime();
        if (position != 0) {
            long lastDate = mMsgList.get(position - 1).getCreateTime();
            // 濡傛灉涓ゆ潯娑堟伅涔嬮棿鐨勯棿闅旇秴杩囧崄鍒嗛挓鍒欐樉绀烘椂闂�
            if (nowDate - lastDate > 600000) {
                TimeFormat timeFormat = new TimeFormat(mContext, nowDate);
                msgTime.setText(timeFormat.getDetailTime());
                msgTime.setVisibility(View.VISIBLE);
            } else {
                msgTime.setVisibility(View.GONE);
            }
        } else {
            TimeFormat timeFormat = new TimeFormat(mContext, nowDate);
            msgTime.setText(timeFormat.getDetailTime());
        }
        //鏄剧ず澶村儚
        if (holder.headIcon != null) {
            Bitmap bitmap;
            //缇よ亰
            if(mIsGroup){
                bitmap = NativeImageLoader.getInstance().getBitmapFromMemCache(msg.getFromID());
                if (bitmap != null)
                    holder.headIcon.setImageBitmap(bitmap);
                else if(mGroupInfo != null){
                    final UserInfo userInfo = mGroupInfo.getGroupMemberInfo(msg.getFromID());
                    //濡傛灉鏈湴瀛樺湪鐢ㄦ埛淇℃伅
                    if(userInfo != null){
                        //濡傛灉mediaID涓虹┖锛岃〃鏄庣敤鎴锋病鏈夎缃繃澶村儚锛岀敤榛樿澶村儚
                        if(TextUtils.isEmpty(userInfo.getAvatar())){
                            holder.headIcon.setImageResource(R.drawable.head_icon);
                        }else {
                            File file = userInfo.getAvatarFile();
                            if(file != null && file.isFile()){
                                bitmap = BitmapLoader.getBitmapFromFile(file.getAbsolutePath(), (int)(50 * mDensity), (int)(50 * mDensity));
                                NativeImageLoader.getInstance().updateBitmapFromCache(msg.getFromID(), bitmap);
                                holder.headIcon.setImageBitmap(bitmap);
                                //鏈湴涓嶅瓨鍦ㄥご鍍忥紝浠庢湇鍔″櫒鎷�
                            }else {
                                userInfo.getAvatarFileAsync(new DownloadAvatarCallback() {
                                    @Override
                                    public void gotResult(int status, String desc, File file) {
                                        if (status == 0) {
                                            Bitmap bitmap = BitmapLoader.getBitmapFromFile(file.getAbsolutePath(), (int)(50 * mDensity), (int)(50 * mDensity));
                                            NativeImageLoader.getInstance().updateBitmapFromCache(msg.getFromID(), bitmap);
                                            holder.headIcon.setImageBitmap(bitmap);
                                        }else {
                                            holder.headIcon.setImageResource(R.drawable.head_icon);
                                        }
                                    }
                                });
                            }
                        }
                        //鏈湴涓嶅瓨鍦ㄧ敤鎴蜂俊鎭紝浠庢湇鍔″櫒鎷�
                    }else {
                        Log.i(TAG, "Get UserInfo from server, UserName: " + msg.getFromName());
                        JMessageClient.getUserInfo(msg.getFromID(), new GetUserInfoCallback() {
                            @Override
                            public void gotResult(int status, String desc, UserInfo userInfo) {
                                if(status == 0){
                                    File file = userInfo.getAvatarFile();
                                    if(file != null && file.isFile()){
                                        Bitmap bitmap1 = BitmapLoader.getBitmapFromFile(file.getAbsolutePath(), (int)(50 * mDensity), (int)(50 * mDensity));
                                        NativeImageLoader.getInstance().updateBitmapFromCache(msg.getFromID(), bitmap1);
                                        holder.headIcon.setImageBitmap(bitmap1);
                                    }
                                }else {
                                    holder.headIcon.setImageResource(R.drawable.head_icon);
                                }
                            }
                        });
                    }
                }
                //鍗曡亰
            }else {
                bitmap = NativeImageLoader.getInstance().getBitmapFromMemCache(msg.getFromID());
                if (bitmap != null)
                    holder.headIcon.setImageBitmap(bitmap);
                else holder.headIcon.setImageResource(R.drawable.head_icon);
            }



            // 鐐瑰嚮澶村儚璺宠浆鍒颁釜浜轰俊鎭晫闈�
            holder.headIcon.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent();
                    if (msg.getDirect().equals(MessageDirect.send)) {
                        intent.putExtra("targetID", msg.getFromName());
                        Log.i(TAG, "msg.getFromName() " + msg.getFromName());
                        intent.setClass(mContext, MeInfoActivity.class);
                        mContext.startActivity(intent);
                    } else {
                        String targetID = msg.getFromID();
                        intent.putExtra("targetID", targetID);
                        intent.setClass(mContext, FriendInfoActivity.class);
                        mContext.startActivity(intent);
                    }
                }
            });
        }

        OnLongClickListener longClickListener = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                // 闀挎寜鏂囨湰寮瑰嚭鑿滃崟
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                View view = LayoutInflater.from(mContext).inflate(
                        R.layout.dialog_msg_alert, null);
                builder.setView(view);
                Button copyBtn = (Button) view
                        .findViewById(R.id.copy_msg_btn);
                Button forwardBtn = (Button) view
                        .findViewById(R.id.forward_msg_btn);
                View line1 = view.findViewById(R.id.forward_split_line);
                View line2 = view.findViewById(R.id.delete_split_line);
                Button deleteBtn = (Button) view.findViewById(R.id.delete_msg_btn);
                final TextView title = (TextView) view
                        .findViewById(R.id.dialog_title);
                if (msg.getContentType().equals(ContentType.voice)) {
                    copyBtn.setVisibility(View.GONE);
                    forwardBtn.setVisibility(View.GONE);
                    line1.setVisibility(View.GONE);
                    line2.setVisibility(View.GONE);
                }
                String name;
                name = msg.getFromName();
                title.setText(name);
                final Dialog dialog = builder.create();
                dialog.show();
                dialog.getWindow().setLayout((int) (0.8 * mWidth), WindowManager.LayoutParams.WRAP_CONTENT);
                OnClickListener listener = new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        switch (msg.getContentType()) {
                            case image:
                                break;
                            case voice:
                                break;
                            case video:
                                break;
                            case location:
                                break;
                            default:
                        }
                        switch (v.getId()) {
                            case R.id.copy_msg_btn:
                                if (msg.getContentType().equals(ContentType.text)) {
                                    final String content = ((TextContent) msg.getContent()).getText();
                                    if (Build.VERSION.SDK_INT > 11) {
                                        ClipboardManager clipboard = (ClipboardManager) mContext
                                                .getSystemService(mContext.CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText(
                                                "Simple text", content);
                                        clipboard.setPrimaryClip(clip);
                                    } else {
                                        ClipboardManager clipboard = (ClipboardManager) mContext
                                                .getSystemService(mContext.CLIPBOARD_SERVICE);
                                        clipboard.setText(content);// 璁剧疆Clipboard 鐨勫唴瀹�
                                        if (clipboard.hasText()) {
                                            clipboard.getText();
                                        }
                                    }

                                    Toast.makeText(mContext, mContext.getString(R.string.copy_toast), Toast.LENGTH_SHORT)
                                            .show();
                                    dialog.dismiss();
                                }
                                break;
                            case R.id.forward_msg_btn:
                                dialog.dismiss();
                                break;
                            case R.id.delete_msg_btn:
                                mConv.deleteMessage(msg.getId());
                                mMsgList.remove(position);
                                notifyDataSetChanged();
                                dialog.dismiss();
                                break;
                        }
                    }
                };
                copyBtn.setOnClickListener(listener);
                forwardBtn.setOnClickListener(listener);
                deleteBtn.setOnClickListener(listener);
                return true;
            }
        };
        try {
            holder.txtContent.setOnLongClickListener(longClickListener);
        } catch (Exception e) {
        }

        return convertView;
    }

    private void handleGroupChangeMsg(Message msg, ViewHolder holder) {
        String content = ((EventNotificationContent)msg.getContent()).getEventText();
        holder.groupChange.setText(content);
        holder.groupChange.setVisibility(View.VISIBLE);
    }

    private void handleCustomMsg(ViewHolder holder){
        holder.groupChange.setVisibility(View.GONE);
    }

    private void handleTextMsg(final Message msg, final ViewHolder holder,
                               final int position) {
        final String content = ((TextContent) msg.getContent()).getText();
        holder.txtContent.setText(content);

        // 妫�煡鍙戦�鐘舵�锛屽彂閫佹柟鏈夐噸鍙戞満鍒�
        if (msg.getDirect().equals(MessageDirect.send)) {
            final Animation sendingAnim = AnimationUtils.loadAnimation(mContext, R.anim.rotate);
            LinearInterpolator lin = new LinearInterpolator();
            sendingAnim.setInterpolator(lin);
            switch (msg.getStatus()) {
                case send_success:
                    if (sendingAnim != null) {
                        holder.sendingIv.clearAnimation();
                        holder.sendingIv.setVisibility(View.GONE);
                    }
                    holder.resend.setVisibility(View.GONE);
                    break;
                case send_fail:
                    if (sendingAnim != null) {
                        holder.sendingIv.clearAnimation();
                        holder.sendingIv.setVisibility(View.GONE);
                    }
                    holder.resend.setVisibility(View.VISIBLE);
                    break;
                case send_going:
                    sendingTextOrVoice(holder, sendingAnim, msg);
                    break;
                default:
            }
            // 鐐瑰嚮閲嶅彂鎸夐挳锛岄噸鍙戞秷鎭�
            holder.resend.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    showResendDialog(holder, sendingAnim, msg);
                }
            });

        } else {
            if (mIsGroup) {
                holder.displayName.setVisibility(View.VISIBLE);
                holder.displayName.setText(msg.getFromName());
            }
        }
    }

    //姝ｅ湪鍙戦�鏂囧瓧鎴栬闊�
    private void sendingTextOrVoice(ViewHolder holder, Animation sendingAnim, Message msg) {
        holder.sendingIv.setVisibility(View.VISIBLE);
        holder.sendingIv.startAnimation(sendingAnim);
        holder.resend.setVisibility(View.GONE);
        //娑堟伅姝ｅ湪鍙戦�锛岄噸鏂版敞鍐屼竴涓洃鍚秷鎭彂閫佸畬鎴愮殑Callback
        if (!msg.isSendCompleteCallbackExists()) {
            msg.setOnSendCompleteCallback(new BasicCallback() {
                @Override
                public void gotResult(final int status, final String desc) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status != 0)
                                HandleResponseCode.onHandle(mContext, status, false);
                            refresh();
                        }

                    });
                }
            });
        }
    }

    //閲嶅彂瀵硅瘽妗�
    private void showResendDialog(final ViewHolder holder, final Animation sendingAnim, final Message msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        View view = LayoutInflater.from(mContext).inflate(
                R.layout.dialog_resend_msg, null);
        builder.setView(view);
        Button cancelBtn = (Button) view.findViewById(R.id.cancel_btn);
        Button resendBtn = (Button) view.findViewById(R.id.resend_btn);
        final Dialog dialog = builder.create();
        dialog.show();
        OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.cancel_btn:
                        dialog.dismiss();
                        break;
                    case R.id.resend_btn:
                        dialog.dismiss();
                        if (msg.getContentType().equals(ContentType.image)) {
                            sendImage(holder, sendingAnim, msg);
                        } else {
                            resendTextOrVoice(holder, sendingAnim, msg);
                        }
                        break;
                }
            }
        };
        cancelBtn.setOnClickListener(listener);
        resendBtn.setOnClickListener(listener);

    }

    // 澶勭悊鍥剧墖
    private void handleImgMsg(final Message msg, final ViewHolder holder, final int position) {
        final ImageContent imgContent = (ImageContent) msg.getContent();
        // 鍏堟嬁鏈湴缂╃暐鍥�
        final String path = imgContent.getLocalThumbnailPath();
        // 鎺ユ敹鍥剧墖
        if (msg.getDirect().equals(MessageDirect.receive)) {
            if (path == null) {
                //浠庢湇鍔″櫒涓婃嬁缂╃暐鍥�
                imgContent.downloadThumbnailImage(msg,
                        new DownloadCompletionCallback() {
                            @Override
                            public void onComplete(int status, String desc, File file) {
                                if (status == 0) {
                                    android.os.Message handleMsg = myHandler.obtainMessage();
                                    handleMsg.what = UPDATE_IMAGEVIEW;
                                    handleMsg.obj = holder;
                                    Bundle bundle = new Bundle();
                                    bundle.putString("path", file.getAbsolutePath());
                                    handleMsg.setData(bundle);
                                    handleMsg.sendToTarget();
                                }
                            }
                        });
            } else {
                setPictureScale(path, holder.picture);
                Picasso.with(mContext).load(new File(path))
                        .into(holder.picture);
            }
            //缇よ亰涓樉绀烘樀绉�
            if (mIsGroup) {
                holder.displayName.setVisibility(View.VISIBLE);
                holder.displayName.setText(msg.getFromName());
            }

            switch (msg.getStatus()) {
                case receive_fail:
                    holder.picture.setBackgroundResource(R.drawable.fetch_failed);
                    break;
                default:
            }
            // 鍙戦�鍥剧墖鏂癸紝鐩存帴鍔犺浇缂╃暐鍥�
        } else {
            try {
                Log.i(TAG, "msg.getID() + thumbnailPath : " + msg.getId() + " " + path);
                setPictureScale(path, holder.picture);
                Picasso.with(mContext).load(new File(path))
                        .into(holder.picture);
            } catch (NullPointerException e) {
                Picasso.with(mContext).load(R.drawable.friends_sends_pictures_no)
                        .into(holder.picture);
            }

            final Animation sendingAnim = AnimationUtils.loadAnimation(mContext, R.anim.rotate);
            LinearInterpolator lin = new LinearInterpolator();
            sendingAnim.setInterpolator(lin);
            //妫�煡鐘舵�
            switch (msg.getStatus()) {
                case send_success:
                    if (sendingAnim != null) {
                        holder.sendingIv.clearAnimation();
                        holder.sendingIv.setVisibility(View.GONE);
                    }
                    holder.picture.setAlpha(1.0f);
                    holder.progressTv.setVisibility(View.GONE);
                    holder.resend.setVisibility(View.GONE);
                    break;
                case send_fail:
                    if (sendingAnim != null) {
                        holder.sendingIv.clearAnimation();
                        holder.sendingIv.setVisibility(View.GONE);
                    }
                    holder.picture.setAlpha(1.0f);
                    holder.progressTv.setVisibility(View.GONE);
                    holder.resend.setVisibility(View.VISIBLE);
                    break;
                case send_going:
                    sendingImage(holder, sendingAnim, msg, path);
                    break;
            }
            // 鐐瑰嚮閲嶅彂鎸夐挳锛岄噸鍙戝浘鐗�
            holder.resend.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
//                    sendImage(holder, sendingAnim, msg);
                    showResendDialog(holder, sendingAnim, msg);
                }
            });
        }
        if (holder.picture != null) {
            // 鐐瑰嚮棰勮鍥剧墖
            holder.picture.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent();
                    intent.putExtra("targetID", mTargetID);
                    intent.putExtra("position", position);
                    intent.putExtra("msgID", msg.getId());
                    intent.putExtra("groupID", mGroupID);
                    intent.putExtra("isGroup", mIsGroup);
                    intent.putExtra("fromChatActivity", true);
                    intent.setClass(mContext, BrowserViewPagerActivity.class);
                    mContext.startActivity(intent);
                }
            });

            holder.picture.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    View view = LayoutInflater.from(mContext).inflate(
                            R.layout.dialog_msg_alert, null);
                    builder.setView(view);
                    Button copyBtn = (Button) view
                            .findViewById(R.id.copy_msg_btn);
                    Button forwardBtn = (Button) view
                            .findViewById(R.id.forward_msg_btn);
                    View line1 = view.findViewById(R.id.forward_split_line);
                    View line2 = view.findViewById(R.id.delete_split_line);
                    Button deleteBtn = (Button) view.findViewById(R.id.delete_msg_btn);
                    final TextView title = (TextView) view
                            .findViewById(R.id.dialog_title);
                    copyBtn.setVisibility(View.GONE);
                    forwardBtn.setVisibility(View.GONE);
                    line1.setVisibility(View.GONE);
                    line2.setVisibility(View.GONE);
                    String name;
                    name = msg.getFromName();
                    title.setText(name);
                    final Dialog dialog = builder.create();
                    dialog.show();
                    dialog.getWindow().setLayout((int) (0.8 * mWidth), WindowManager.LayoutParams.WRAP_CONTENT);
                    OnClickListener listener = new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            switch (v.getId()) {
                                case R.id.copy_msg_btn:
                                    break;
                                case R.id.forward_msg_btn:
                                    dialog.dismiss();
                                    break;
                                case R.id.delete_msg_btn:
                                    mConv.deleteMessage(msg.getId());
                                    mMsgList.remove(position);
                                    notifyDataSetChanged();
                                    dialog.dismiss();
                                    break;
                            }
                        }
                    };
                    copyBtn.setOnClickListener(listener);
                    forwardBtn.setOnClickListener(listener);
                    deleteBtn.setOnClickListener(listener);
                    return true;
                }
            });

        }
    }

    private void sendingImage(final ViewHolder holder, Animation sendingAnim, Message msg, final String path) {
        holder.picture.setAlpha(0.75f);
        holder.sendingIv.setVisibility(View.VISIBLE);
        holder.sendingIv.startAnimation(sendingAnim);
        holder.progressTv.setVisibility(View.VISIBLE);
        holder.resend.setVisibility(View.GONE);
        //濡傛灉鍥剧墖姝ｅ湪鍙戦�锛岄噸鏂版敞鍐屼笂浼犺繘搴allback
        if (!msg.isContentUploadProgressCallbackExists()) {
            msg.setOnContentUploadProgressCallback(new ProgressUpdateCallback() {
                @Override
                public void onProgressUpdate(double v) {
                    Log.i(TAG, "progress v :" + v);
                    holder.progressTv.setText((int) (v * 100) + "%");
                }
            });
        }
        if (!msg.isSendCompleteCallbackExists()) {
            msg.setOnSendCompleteCallback(new BasicCallback() {
                @Override
                public void gotResult(final int status, String desc) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Picasso.with(mContext).load(new File(path)).into(holder.picture);
                            Log.i("Send picture", "update: ");
                            refresh();
                        }
                    });
                }
            });
        }
    }

    /**
     * 璁剧疆鍥剧墖鏈�皬瀹介珮
     *
     * @param path 鍥剧墖璺緞
     * @param imageView 鏄剧ず鍥剧墖鐨刅iew
     */
    private void setPictureScale(String path, ImageView imageView) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);
//                璁＄畻鍥剧墖缂╂斁姣斾緥
        double imageWidth = opts.outWidth;
        double imageHeight = opts.outHeight;
        if (imageWidth < 100 * mDensity) {
            imageHeight = imageHeight * (100 * mDensity / imageWidth);
            imageWidth = 100 * mDensity;
        }
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        params.width = (int) imageWidth;
        params.height = (int) imageHeight;
        imageView.setLayoutParams(params);
    }

    private void resendTextOrVoice(final ViewHolder holder, Animation sendingAnim, Message msg) {
        holder.resend.setVisibility(View.GONE);
        holder.sendingIv.setVisibility(View.VISIBLE);
        holder.sendingIv.startAnimation(sendingAnim);

        if (!msg.isSendCompleteCallbackExists()) {
            msg.setOnSendCompleteCallback(new BasicCallback() {
                @Override
                public void gotResult(final int status, String desc) {
                    if (status != 0) {
                        HandleResponseCode.onHandle(mContext, status, false);
                        holder.sendingIv.clearAnimation();
                        holder.sendingIv.setVisibility(View.GONE);
                        holder.resend.setVisibility(View.VISIBLE);
                        Log.i(TAG, "Resend message failed!");
                    }
                    refresh();
                }
            });
        }

        JMessageClient.sendMessage(msg);
    }

    private void sendImage(final ViewHolder viewHolder, Animation sendingAnim, Message msg) {
        ImageContent imgContent = (ImageContent) msg.getContent();
        final String path = imgContent.getLocalThumbnailPath();
        viewHolder.sendingIv.setVisibility(View.VISIBLE);
        viewHolder.sendingIv.startAnimation(sendingAnim);
        viewHolder.picture.setAlpha(0.75f);
        viewHolder.resend.setVisibility(View.GONE);
        viewHolder.progressTv.setVisibility(View.VISIBLE);
        try {

            // 鏄剧ず涓婁紶杩涘害
            msg.setOnContentUploadProgressCallback(new ProgressUpdateCallback() {
                @Override
                public void onProgressUpdate(final double progress) {
                    Log.i("Uploding picture", "progress: "
                            + progress);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewHolder.progressTv.setText((int) (progress * 100) + "%");
                        }
                    });
                }
            });
            if (!msg.isSendCompleteCallbackExists()) {
                msg.setOnSendCompleteCallback(new BasicCallback() {
                    @Override
                    public void gotResult(final int status, String desc) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status != 0)
                                    HandleResponseCode.onHandle(mContext, status, false);
                                Picasso.with(mContext).load(new File(path)).into(viewHolder.picture);
                                Log.i("Send picture", "update: ");
                                refresh();
                            }
                        });
                    }
                });
            }
            JMessageClient.sendMessage(msg);
        } catch (Exception e) {
        }
    }

    private void handleVoiceMsg(final Message msg, final ViewHolder holder,
                                final int position) {
        final VoiceContent content = (VoiceContent) msg.getContent();
        final MessageDirect msgDirect = msg.getDirect();
        int length = content.getDuration();
        holder.voiceLength.setText(length + "\"");
        //鎺у埗璇煶闀垮害鏄剧ず锛岄暱搴﹀骞呴殢璇煶闀垮害閫愭笎缂╁皬
        int width = (int) (-0.04 * length * length + 4.526 * length + 75.214);
        holder.txtContent.setWidth((int) (width * mDensity));
        if (msgDirect.equals(MessageDirect.send)) {
            holder.voice.setImageResource(R.drawable.send_3);
            final Animation sendingAnim = AnimationUtils.loadAnimation(mContext, R.anim.rotate);
            LinearInterpolator lin = new LinearInterpolator();
            sendingAnim.setInterpolator(lin);
            switch (msg.getStatus()) {
                case send_success:
                    if (sendingAnim != null) {
                        holder.sendingIv.clearAnimation();
                        holder.sendingIv.setVisibility(View.GONE);
                    }
                    holder.resend.setVisibility(View.GONE);
                    break;
                case send_fail:
                    if (sendingAnim != null) {
                        holder.sendingIv.clearAnimation();
                        holder.sendingIv.setVisibility(View.GONE);
                    }
                    holder.resend.setVisibility(View.VISIBLE);
                    break;
                case send_going:
                    sendingTextOrVoice(holder, sendingAnim, msg);
                    break;
                default:
            }

            holder.resend.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (msg.getContent() != null)
                        showResendDialog(holder, sendingAnim, msg);
                    else
                        Toast.makeText(mContext, mContext.getString(R.string.sdcard_not_exist_toast), Toast.LENGTH_SHORT).show();
                }
            });
        } else switch (msg.getStatus()) {
            case receive_success:
                if (mIsGroup) {
                    holder.displayName.setVisibility(View.VISIBLE);
                    holder.displayName.setText(msg.getFromName());
                }
                holder.voice.setImageResource(R.drawable.receive_3);
                // 鏀跺埌璇煶锛岃缃湭璇�
                if (msg.getContent().getBooleanExtra("isReaded") == null
                        || !msg.getContent().getBooleanExtra("isReaded")) {
                    mConv.updateMessageExtra(msg, "isReaded", false);
                    holder.readStatus.setVisibility(View.VISIBLE);
                    if (mIndexList.size() > 0) {
                        if (!mIndexList.contains(position)) {
                            Log.i("mIndexList", "position: " + position);
                            addTolistAndSort(position);
                            Log.i("mIndexList", "mIndexList.size()" + mIndexList.size());
                        }
                    } else {
                        Log.i("mIndexList", "position: " + position);
                        addTolistAndSort(position);
                    }
                    Log.d("", "current position  = " + position);
                    if (nextPlayPosition == position && autoPlay) {
                        Log.d("", "nextPlayPosition = " + nextPlayPosition);
                        playVoiceThenRefresh(position, holder);
                    }
                } else if (msg.getContent().getBooleanExtra("isReaded").equals(true)) {
                    holder.readStatus.setVisibility(View.GONE);
                }
                break;
            case receive_fail:
                holder.voice.setImageResource(R.drawable.receive_3);
                // 鎺ユ敹澶辫触锛屼粠鏈嶅姟鍣ㄤ笂涓嬭浇
                mConv.deleteMessage(msg.getId());
                content.downloadVoiceFile(msg,
                        new DownloadCompletionCallback() {
                            @Override
                            public void onComplete(int status, String desc, File file) {
                                if (status != 0) {
                                    Toast.makeText(mContext, mContext.getString(R.string.voice_fetch_failed_toast),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.i("VoiceMessage", "reload success");
                                }
                            }
                        });
                break;
            case receive_going:
                break;
            default:
        }


        holder.txtContent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
                if (!sdCardExist && msg.getDirect().equals(MessageDirect.send)) {
                    Toast.makeText(mContext, mContext.getString(R.string.sdcard_not_exist_toast), Toast.LENGTH_SHORT).show();
                    return;
                }
                // 濡傛灉涔嬪墠瀛樺湪鎾斁鍔ㄧ敾锛屾棤璁鸿繖娆＄偣鍑昏Е鍙戠殑鏄殏鍋滆繕鏄挱鏀撅紝鍋滄涓婃鎾斁鐨勫姩鐢�
                if (mVoiceAnimation != null) {
                    mVoiceAnimation.stop();
                }
                // 鎾斁涓偣鍑讳簡姝ｅ湪鎾斁鐨処tem 鍒欐殏鍋滄挱鏀�
                if (mp.isPlaying() && mPosition == position) {
                    if (msgDirect.equals(MessageDirect.send)) {
                        holder.voice.setImageResource(R.anim.voice_send);
                    } else
                        holder.voice.setImageResource(R.anim.voice_receive);
                    mVoiceAnimation = (AnimationDrawable) holder.voice
                            .getDrawable();
                    pauseVoice();
                    mVoiceAnimation.stop();
                    // 寮�鎾斁褰曢煶
                } else if (msgDirect.equals(MessageDirect.send)) {
                    try {
                        holder.voice.setImageResource(R.anim.voice_send);
                        mVoiceAnimation = (AnimationDrawable) holder.voice
                                .getDrawable();

                        // 缁х画鎾斁涔嬪墠鏆傚仠鐨勫綍闊�
                        if (mSetData && mPosition == position) {
                            playVoice();
                            // 鍚﹀垯閲嶆柊鎾斁璇ュ綍闊虫垨鑰呭叾浠栧綍闊�
                        } else {
                            mp.reset();
                            // 璁板綍鎾斁褰曢煶鐨勪綅缃�
                            mPosition = position;
                            Log.i(TAG, "content.getLocalPath:"
                                    + content.getLocalPath());
                            mFIS = new FileInputStream(content
                                    .getLocalPath());
                            mFD = mFIS.getFD();
                            mp.setDataSource(mFD);
                            if(mIsEarPhoneOn){
                                mp.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
                            }else {
                                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            }

                            mp.prepare();
                            playVoice();
                        }
                    } catch (NullPointerException e) {
                        Toast.makeText(mActivity, mContext.getString(R.string.file_not_found_toast),
                                Toast.LENGTH_SHORT).show();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        Toast.makeText(mActivity, mContext.getString(R.string.file_not_found_toast),
                                Toast.LENGTH_SHORT).show();
                    }finally {
                        try {
                            if(mFIS != null){
                                mFIS.close();
                            }
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    // 璇煶鎺ユ敹鏂圭壒娈婂鐞嗭紝鑷姩杩炵画鎾斁鏈璇煶
                } else {
                    try {
                        // 缁х画鎾斁涔嬪墠鏆傚仠鐨勫綍闊�
                        if (mSetData && mPosition == position) {
                            mVoiceAnimation.start();
                            mp.start();
                            // 鍚﹀垯寮�鎾斁鍙︿竴鏉″綍闊�
                        } else {
                            // 閫変腑鐨勫綍闊虫槸鍚﹀凡缁忔挱鏀捐繃锛屽鏋滄湭鎾斁锛岃嚜鍔ㄨ繛缁挱鏀捐繖鏉¤闊充箣鍚庢湭鎾斁鐨勮闊�
                            if (msg.getContent().getBooleanExtra("isReaded") == null || msg.getContent().getBooleanExtra("isReaded") == false) {
                                autoPlay = true;
                                playVoiceThenRefresh(position, holder);
                                // 鍚﹀垯鐩存帴鎾斁閫変腑鐨勮闊�
                            } else {
                                holder.voice.setImageResource(R.anim.voice_receive);
                                mVoiceAnimation = (AnimationDrawable) holder.voice.getDrawable();
                                mp.reset();
                                // 璁板綍鎾斁褰曢煶鐨勪綅缃�
                                mPosition = position;
                                if(content.getLocalPath() != null){
                                    try {
                                        mFIS = new FileInputStream(content
                                                .getLocalPath());
                                        mFD = mFIS.getFD();
                                        mp.setDataSource(mFD);
                                        mp.prepare();
                                        playVoice();
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }finally {
                                        try {
                                            if(mFIS != null){
                                                mFIS.close();
                                            }
                                        }catch (IOException e){
                                           e.printStackTrace();
                                        }
                                    }
                                }else {
                                    Toast.makeText(mContext, mContext.getString(R.string.voice_fetch_failed_toast), Toast.LENGTH_SHORT).show();
                                }

                            }
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }

            private void playVoice() {
                mVoiceAnimation.start();
                mp.start();
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer arg0) {
                        mVoiceAnimation.stop();
                        mp.reset();
                        mSetData = false;
                        // 鎾斁瀹屾瘯锛屾仮澶嶅垵濮嬬姸鎬�
                        if (msgDirect.equals(MessageDirect.send))
                            holder.voice
                                    .setImageResource(R.drawable.send_3);
                        else {
                            holder.voice
                                    .setImageResource(R.drawable.receive_3);
                            holder.readStatus.setVisibility(View.GONE);
                        }
                    }
                });
            }

            private void pauseVoice() {
                mp.pause();
                mSetData = true;
            }
        });
    }

    private void playVoiceThenRefresh(final int position, final ViewHolder holder) {
        Message message = mMsgList.get(position);
        //璁句负宸茶
        mConv.updateMessageExtra(message, "isReaded", true);
        mPosition = position;
        holder.readStatus.setVisibility(View.GONE);
        if (mVoiceAnimation != null) {
            mVoiceAnimation.stop();
            mVoiceAnimation = null;
        }
        holder.voice.setImageResource(R.anim.voice_receive);
        mVoiceAnimation = (AnimationDrawable) holder.voice.getDrawable();
        try {
            VoiceContent vc = (VoiceContent) message.getContent();
            mp.reset();
            mFIS = new FileInputStream(vc.getLocalPath());
            mFD = mFIS.getFD();
            mp.setDataSource(mFD);
            mp.prepare();
            mp.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mVoiceAnimation.start();
                    mp.start();
                }
            });
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mVoiceAnimation.stop();
                    mp.reset();
                    mSetData = false;
                    holder.voice
                            .setImageResource(R.drawable.receive_3);
                    int curCount = mIndexList.indexOf(position);
                    Log.d("", "curCount = " + curCount);
                    if (curCount + 1 >= mIndexList.size()) {
                        nextPlayPosition = -1;
                        autoPlay = false;
                    } else {
                        nextPlayPosition = mIndexList.get(curCount + 1);
                        notifyDataSetChanged();
                    }
                    mIndexList.remove(curCount);
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
        }finally {
            try {
                if(mFIS != null){
                    mFIS.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void addTolistAndSort(int position) {
        mIndexList.add(position);
        Collections.sort(mIndexList);
    }

    private void handleLocationMsg(Message msg, ViewHolder holder, int position) {

    }

    public void stopMediaPlayer() {
        if(mp.isPlaying())
            mp.stop();
    }

    public static class ViewHolder {
        CircleImageView headIcon;
        TextView displayName;
        TextView txtContent;
        ImageView picture;
        TextView progressTv;
        ImageButton resend;
        TextView voiceLength;
        ImageView voice;
        // 褰曢煶鏄惁鎾斁杩囩殑鏍囧織
        ImageView readStatus;
        TextView location;
        TextView groupChange;
        ImageView sendingIv;
    }
}
