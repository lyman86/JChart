package io.jchat.android.controller;

import io.jchat.android.activity.ChatDetailActivity;
import io.jchat.android.activity.FriendInfoActivity;
import io.jchat.android.activity.MeInfoActivity;
import io.jchat.android.activity.R;
import io.jchat.android.adapter.GroupMemberGridAdapter;
import io.jchat.android.tools.BitmapLoader;
import io.jchat.android.tools.HandleResponseCode;
import io.jchat.android.tools.NativeImageLoader;
import io.jchat.android.view.ChatDetailView;
import io.jchat.android.view.DialogCreator;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.callback.CreateGroupCallback;
import cn.jpush.im.android.api.callback.GetGroupInfoCallback;
import cn.jpush.im.android.api.callback.GetGroupMembersCallback;
import cn.jpush.im.android.api.callback.GetUserInfoCallback;
import cn.jpush.im.android.api.enums.ConversationType;
import cn.jpush.im.android.api.model.Conversation;
import cn.jpush.im.android.api.model.GroupInfo;
import cn.jpush.im.android.api.model.UserInfo;
import cn.jpush.im.api.BasicCallback;

public class ChatDetailController implements OnClickListener,
        OnItemClickListener, OnItemLongClickListener {

    private static final String TAG = "ChatDetailController";

    private ChatDetailView mChatDetailView;
    private ChatDetailActivity mContext;
    private GroupMemberGridAdapter mGridAdapter;
    private List<UserInfo> mMemberIDList = new ArrayList<UserInfo>();
    // 褰撳墠GridView缇ゆ垚鍛橀」鏁�
    private int mCurrentNum;
    // 绌虹櫧椤圭殑椤规暟
    private int[] mRestArray;
    private boolean mIsGroup = false;
    private boolean mIsCreator = false;
    private long mGroupID;
    private String mTargetID;
    private DialogCreator mLD;
    private Dialog mLoadingDialog = null;
    private boolean mIsShowDelete = false;
    private static final int GET_GROUP_MEMBER = 2047;
    private static final int ADD_TO_GRIDVIEW = 2048;
    private static final int DELETE_FROM_GRIDVIEW = 2049;
    private double mDensity;
    private String mGroupName;
    private final MyHandler myHandler = new MyHandler(this);

    public ChatDetailController(ChatDetailView chatDetailView,
                                ChatDetailActivity context) {
        this.mChatDetailView = chatDetailView;
        this.mContext = context;
        initData();
        DisplayMetrics dm = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(dm);
        mDensity = dm.density;
    }

    /*
     * 鑾峰緱缇ょ粍淇℃伅锛屽垵濮嬪寲缇ょ粍鐣岄潰锛屽厛浠庢湰鍦拌鍙栵紝濡傛灉娌℃湁鍐嶄粠鏈嶅姟鍣ㄨ鍙�
     */
    private void initData() {
        Intent intent = mContext.getIntent();
        mIsGroup = intent.getBooleanExtra("isGroup", false);
        mGroupID = intent.getLongExtra("groupID", 0);
        Log.i(TAG, "mGroupID" + mGroupID);
        mTargetID = intent.getStringExtra("targetID");
        Log.i(TAG, "mTargetID: " + mTargetID);
        // 鏄兢缁�
        if (mIsGroup) {
            //鑾峰緱缇ょ粍鍩烘湰淇℃伅锛氱兢涓籌D銆佺兢缁勫悕銆佺兢缁勪汉鏁�
            JMessageClient.getGroupInfo(mGroupID,
                    new GetGroupInfoCallback(false) {
                        @Override
                        public void gotResult(final int status, final String desc, GroupInfo group) {
                            if (status == 0) {
                                android.os.Message msg = myHandler.obtainMessage();
                                msg.what = 0;
                                msg.obj = group;
                                Log.i(TAG, "Group owner is " + group.getGroupOwner());
                                msg.sendToTarget();
                            } else {
                                mContext.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        HandleResponseCode.onHandle(mContext, status, false);
                                    }
                                });
                            }

                        }
                    }
            );

            String myNickName = JMessageClient.getMyInfo().getNickname();
            // TODO 缇ゅ悕锛屾樀绉帮紝缇や汉鏁扮瓑鍒濆鍖�
            mChatDetailView.setMyName(myNickName);
            //鑾峰緱缇ょ粍鎴愬憳ID
            JMessageClient
                    .getGroupMembers(mGroupID, new GetGroupMembersCallback() {
                        @Override
                        public void gotResult(int status, String desc, List<UserInfo> members) {
                            if (status == 0) {
                                android.os.Message msg = myHandler.obtainMessage();
                                mMemberIDList = members;
                                msg.what = GET_GROUP_MEMBER;
                                msg.sendToTarget();
                            }
                        }
                    });
//                }
            // 鏄崟鑱�
        } else {
            JMessageClient.getUserInfo(mTargetID, new GetUserInfoCallback() {
                @Override
                public void gotResult(int status, String desc, UserInfo userInfo) {
                    if (status == 0) {
                        mMemberIDList.add(userInfo);
                        initAdapter();
                    }
                }
            });
            // 璁剧疆鍗曡亰鐣岄潰
            mChatDetailView.setSingleView();
        }
    }


    private void initAdapter() {
        mCurrentNum = mMemberIDList.size();
        // 闄や簡缇ゆ垚鍛業tem鍜屾坊鍔犮�鍒犻櫎鎸夐挳锛屽墿涓嬬殑閮界湅鎴愭槸绌虹櫧椤癸紝
        // 瀵瑰簲鐨刴RestNum[mCurrent%4]鐨勫�鍗充负绌虹櫧椤圭殑鏁扮洰
        mRestArray = new int[]{2, 1, 0, 3};
        // 鍒濆鍖栧ご鍍�
        mGridAdapter = new GroupMemberGridAdapter(mContext, mMemberIDList, mIsCreator, mIsGroup);
        mChatDetailView.setAdapter(mGridAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.return_btn:
                mContext.finish();
                break;

            // 璁剧疆缇ょ粍鍚嶇О
            case R.id.group_name_rl:
                mContext.showGroupNameSettingDialog(1, mGroupID, mGroupName);
                break;

            // 璁剧疆鎴戝湪缇ょ粍鐨勬樀绉�
            case R.id.group_my_name_ll:
                mContext.showGroupNameSettingDialog(2, mGroupID, mGroupName);
                break;

            // 缇ょ粍浜烘暟
            case R.id.group_num_rl:
                break;

            // 鏌ヨ鑱婂ぉ璁板綍
            case R.id.group_chat_record_ll:
                break;

            // 鍒犻櫎鑱婂ぉ璁板綍
            case R.id.group_chat_del_rl:
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_reset_password, null);
                builder.setView(view);
                TextView title = (TextView) view.findViewById(R.id.title_tv);
                title.setText(mContext.getString(R.string.clear_history_confirm_title));
                final EditText pwdEt = (EditText) view.findViewById(R.id.password_et);
                pwdEt.setVisibility(View.GONE);
                final Button cancel = (Button) view.findViewById(R.id.cancel_btn);
                final Button commit = (Button) view.findViewById(R.id.commit_btn);
                final Dialog dialog = builder.create();
                dialog.show();
                View.OnClickListener listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        switch (view.getId()) {
                            case R.id.cancel_btn:
                                dialog.cancel();
                                break;
                            case R.id.commit_btn:
                                Conversation conv;
                                if (mIsGroup)
                                    conv = JMessageClient.getGroupConversation(mGroupID);
                                else
                                    conv = JMessageClient.getSingleConversation(mTargetID);
                                if (conv != null) {
                                    conv.deleteAllMessage();
                                }
                                dialog.cancel();
                                break;
                        }
                    }
                };
                cancel.setOnClickListener(listener);
                commit.setOnClickListener(listener);
                break;
            case R.id.chat_detail_del_group:
                deleteAndExit();
                break;
        }
    }

    /**
     * 鍒犻櫎骞堕�鍑�
     */
    private void deleteAndExit() {
        mLD = new DialogCreator();
        mLoadingDialog = mLD.createLoadingDialog(mContext, mContext.getString(R.string.exiting_group_toast));
        mLoadingDialog.show();
        JMessageClient.exitGroup(mGroupID, new BasicCallback(false) {
            @Override
            public void gotResult(final int status, final String desc) {
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mLoadingDialog != null)
                            mLoadingDialog.dismiss();
                        if (status == 0) {
                            boolean deleted = JMessageClient.deleteGroupConversation(mGroupID);
                            Log.i(TAG, "deleted: " + deleted);
                            mContext.StartMainActivity();
                        } else HandleResponseCode.onHandle(mContext, status, false);
                    }
                });
            }
        });
    }

    // GridView鐐瑰嚮浜嬩欢
    @Override
    public void onItemClick(AdapterView<?> viewAdapter, View view,
                            final int position, long id) {
        // 娌℃湁瑙﹀彂delete鏃�
        if (!mIsShowDelete) {
            // 鐐瑰嚮缇ゆ垚鍛橀」鏃�
            if (position < mCurrentNum) {
                Intent intent = new Intent();
                if (mMemberIDList.get(position).getUserName().equals(JMessageClient.getMyInfo().getUserName())) {
                    intent.setClass(mContext, MeInfoActivity.class);
                } else {
                    intent.putExtra("targetID", mMemberIDList.get(position).getUserName());
                    intent.setClass(mContext, FriendInfoActivity.class);
                }
                mContext.startActivity(intent);
                // 鐐瑰嚮娣诲姞鎴愬憳鎸夐挳
            } else if (position == mCurrentNum) {
                addMemberToGroup();
                // mContext.showContacts();

                // 鏄兢涓� 鎴愬憳涓暟澶т簬1骞剁偣鍑诲垹闄ゆ寜閽�
            } else if (position == mCurrentNum + 1 && mIsCreator && mCurrentNum > 1) {
                // delete friend from group
                mIsShowDelete = true;
                mGridAdapter.setIsShowDelete(true,
                        mRestArray[mCurrentNum % 4]);
            }
            // delete鐘舵�
        } else {
            // 鐐瑰嚮缇ゆ垚鍛業tem鏃�
            if (position < mCurrentNum) {
                //濡傛灉缇や富鍒犻櫎鑷繁
                if (mMemberIDList.get(position).getUserName().equals(JMessageClient.getMyInfo().getUserName())) {
                    return;
                } else {
                    // 鍒犻櫎鏌愪釜缇ゆ垚鍛�
                    mLD = new DialogCreator();
                    mLoadingDialog = mLD.createLoadingDialog(mContext, mContext.getString(R.string.deleting_hint));
                    List<String> delList = new ArrayList<String>();
                    //涔嬫墍浠ヨ浼犱竴涓狶ist锛岃�铏戝埌涔嬪悗鍙兘鏀寔鍚屾椂鍒犻櫎澶氫汉鍔熻兘锛岀幇鍦↙ist涓彧鏈変竴涓厓绱�
                    delList.add(mMemberIDList.get(position).getUserName());
                    delMember(delList, position);
                    // 褰撳墠鎴愬憳鏁颁负0锛岄�鍑哄垹闄ょ姸鎬�
                    if (mMemberIDList.size() == 0) {
                        mIsShowDelete = false;
                        mGridAdapter.setIsShowDelete(false);
                    }
                }
                // 鐐瑰嚮绌虹櫧椤规椂, 鎭㈠GridView鐣岄潰
            } else {
                if (mIsShowDelete) {
                    mIsShowDelete = false;
                    mGridAdapter.setIsShowDelete(false,
                            mRestArray[mCurrentNum % 4]);
                }
            }
        }
    }

    //鐐瑰嚮娣诲姞鎸夐挳瑙﹀彂浜嬩欢
    private void addMemberToGroup() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final View view = LayoutInflater.from(mContext).inflate(
                R.layout.dialog_add_friend_to_conv_list, null);
        builder.setView(view);
        final Dialog dialog = builder.create();
        dialog.show();
        TextView title = (TextView) view.findViewById(R.id.dialog_name);
        title.setText(mContext.getString(R.string.add_friend_to_group_title));
        final EditText userNameEt = (EditText) view.findViewById(R.id.user_name_et);
        final Button cancel = (Button) view.findViewById(R.id.cancel_btn);
        final Button commit = (Button) view.findViewById(R.id.commit_btn);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.cancel_btn:
                        dialog.cancel();
                        break;
                    case R.id.commit_btn:
                        final String targetID = userNameEt.getText().toString().trim();
                        Log.i(TAG, "targetID " + targetID);
                        if (TextUtils.isEmpty(targetID)) {
                            Toast.makeText(mContext, mContext.getString(R.string.username_not_null_toast), Toast.LENGTH_SHORT).show();
                            break;
                            //妫�煡缇ょ粍涓槸鍚﹀寘鍚鐢ㄦ埛
                        } else if (checkIfNotContainUser(targetID)) {
                            mLD = new DialogCreator();
                            mLoadingDialog = mLD.createLoadingDialog(mContext, mContext.getString(R.string.searching_user));
                            mLoadingDialog.show();
                            JMessageClient.getUserInfo(targetID, new GetUserInfoCallback(false) {
                                @Override
                                public void gotResult(final int status, String desc, UserInfo userInfo) {
                                    if (status == 0) {
                                        //缂撳瓨澶村儚
                                        File file = userInfo.getAvatarFile();
                                        if (file != null) {
                                            Bitmap bitmap = BitmapLoader.getBitmapFromFile(file.getAbsolutePath(),
                                                    (int) (50 * mDensity), (int) (50 * mDensity));
                                            if (bitmap != null)
                                                NativeImageLoader.getInstance().updateBitmapFromCache(targetID, bitmap);
                                        }
                                        dialog.cancel();
                                        // add friend to group
                                        // 瑕佸鍔犲埌缇ょ殑鎴愬憳ID闆嗗悎
                                        ArrayList<String> userIDs = new ArrayList<String>();
                                        userIDs.add(targetID);
                                        android.os.Message msg = myHandler.obtainMessage();
                                        msg.what = 1;
                                        msg.obj = userInfo;
                                        msg.sendToTarget();
                                    } else {
                                        mContext.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (mLoadingDialog != null)
                                                    mLoadingDialog.dismiss();
                                                HandleResponseCode.onHandle(mContext, status, true);
                                            }
                                        });
                                    }
                                }
                            });

                        } else {
                            dialog.cancel();
                            mContext.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext, mContext.getString(R.string.user_already_exist_toast), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        break;
                }
            }
        };
        cancel.setOnClickListener(listener);
        commit.setOnClickListener(listener);
    }

    /**
     * 娣诲姞鎴愬憳鏃舵鏌ユ槸鍚﹀瓨鍦ㄨ缇ゆ垚鍛�
     *
     * @param targetID 瑕佹坊鍔犵殑鐢ㄦ埛
     * @return 杩斿洖鏄惁瀛樺湪璇ョ敤鎴�
     */
    private boolean checkIfNotContainUser(String targetID) {
        if (mMemberIDList != null) {
            for (UserInfo userInfo : mMemberIDList) {
                if (userInfo.getUserName().equals(targetID))
                    return false;
            }
            return true;
        }
        return true;
    }

    /**
     * @param userInfo 瑕佸鍔犵殑鎴愬憳鐨勭敤鎴峰悕锛岀洰鍓嶄竴娆″彧鑳藉鍔犱竴涓�
     */
    private void addAMember(final UserInfo userInfo) {
        try {
            mLD = new DialogCreator();
            mLoadingDialog = mLD.createLoadingDialog(mContext, mContext.getString(R.string.adding_hint));
            mLoadingDialog.show();
            ArrayList<String> list = new ArrayList<String>();
            list.add(userInfo.getUserName());
            JMessageClient.addGroupMembers(mGroupID, list,
                    new BasicCallback() {

                        @Override
                        public void gotResult(final int status, final String desc) {
                            if (status == 0) {
                                // 娣诲姞缇ゆ垚鍛�
                                ++mCurrentNum;
                                mGridAdapter.addMemberToList(userInfo);
                                //Log.i("ADD_TO_GRIDVIEW", "宸叉坊鍔�);
                                mLoadingDialog.dismiss();
                            } else {
                                mLoadingDialog.dismiss();
                                HandleResponseCode.onHandle(mContext, status, true);
                            }
                        }
                    });
        } catch (Exception e) {
            mLoadingDialog.dismiss();
            e.printStackTrace();
            Toast.makeText(mContext, mContext.getString(R.string.unknown_error_toast), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 鍒犻櫎鎴愬憳
     *
     * @param list     琚垹闄ょ殑鐢ㄦ埛鍚峀ist
     * @param position 鍒犻櫎鐨勪綅缃�
     */
    private void delMember(List<String> list, final int position) {
        mLoadingDialog.show();
        try {
            JMessageClient.removeGroupMembers(mGroupID,
                    list,
                    new BasicCallback() {

                        @Override
                        public void gotResult(final int status, final String desc) {
                            mLoadingDialog.dismiss();
                            if (status == 0) {
                                android.os.Message msg = myHandler.obtainMessage();
                                msg.what = DELETE_FROM_GRIDVIEW;
                                Bundle bundle = new Bundle();
                                bundle.putInt("position", position);
                                msg.setData(bundle);
                                msg.sendToTarget();
                            } else {
                                mContext.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        HandleResponseCode.onHandle(mContext, status, false);
                                    }
                                });
                            }
                        }
                    });
        } catch (Exception e) {
            mLoadingDialog.dismiss();
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, mContext.getString(R.string.unknown_error_toast), Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }
    }

    public void refreshGroupName(String newName) {
        mGroupName = newName;
    }

    private static class MyHandler extends Handler{
        private final WeakReference<ChatDetailController> mController;

        public MyHandler(ChatDetailController controller){
            mController = new WeakReference<ChatDetailController>(controller);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ChatDetailController controller = mController.get();
            if(controller != null){
                switch (msg.what) {
                    // 鍒濆鍖栫兢缁�
                    case 0:
                        GroupInfo groupInfo = (GroupInfo) msg.obj;
                        String groupOwnerID = groupInfo.getGroupOwner();
                        controller.mGroupName = groupInfo.getGroupName();
                        if(TextUtils.isEmpty(controller.mGroupName)){
                            controller.mChatDetailView.setGroupName(controller.mContext.getString(R.string.unnamed));
                        }else {
                            controller.mChatDetailView.setGroupName(controller.mGroupName);
                        }
                        // 鍒ゆ柇鏄惁涓虹兢涓�
                        if (groupOwnerID != null && groupOwnerID.equals(JMessageClient.getMyInfo().getUserName()))
                            controller.mIsCreator = true;
                        Log.d(TAG, "groupOwnerID = " + groupOwnerID + "isCreator = " + true);
                        controller.mChatDetailView.setMyName(JMessageClient.getMyInfo().getUserName());
                        if (controller.mGridAdapter != null) {
                            controller.mGridAdapter.setCreator(controller.mIsCreator);
                        }
                        break;
                    //鐐瑰嚮鍔犱汉鎸夐挳骞朵笖鐢ㄦ埛淇℃伅杩斿洖姝ｇ‘
                    case 1:
                        Log.i(TAG, "Adding Group Member, got UserInfo");
                        if (controller.mLoadingDialog != null)
                            controller.mLoadingDialog.dismiss();
                        final UserInfo userInfo = (UserInfo) msg.obj;
                        if (controller.mIsGroup)
                            controller.addAMember(userInfo);
                            //鍦ㄥ崟鑱婁腑鐐瑰嚮鍔犱汉鎸夐挳骞朵笖鐢ㄦ埛淇℃伅杩斿洖姝ｇ‘,濡傛灉涓虹涓夋柟鍒欏垱寤虹兢鑱�
                        else {
                            if (userInfo.getUserName().equals(JMessageClient.getMyInfo().getUserName()) || userInfo.getUserName().equals(controller.mTargetID))
                                return;
                            else controller.addMemberAndCreateGroup(userInfo.getUserName());
                        }
                        break;
                    // 鑾峰彇鎴愬憳鍒楄〃锛岀紦瀛樺ご鍍忥紝鏇存柊GridView
                    case GET_GROUP_MEMBER:
                        Log.i(TAG, "GroupMember: " + controller.mMemberIDList.toString());
                        controller.mChatDetailView.setTitle(controller.mMemberIDList.size());
                        controller.initAdapter();
                        break;
                    // 娣诲姞鎴愬憳
                    case ADD_TO_GRIDVIEW:
//                    ++mCurrentNum;
//                    mGridAdapter.addMemberToList(msg.getData().getStringArrayList("memberList"));
//                    Log.i("ADD_TO_GRIDVIEW", "宸叉坊鍔�);
                        break;
                    // 鍒犻櫎鎴愬憳
                    case DELETE_FROM_GRIDVIEW:
                        // 鏇存柊GridView
                        --controller.mCurrentNum;
                        int position = msg.getData().getInt("position");
                        controller.mGridAdapter.remove(position);
                        //Log.i("DELETE_FROM_GRIDVIEW", "宸插垹闄�);
                        break;
                }
            }
        }
    }

    /**
     * 鍦ㄥ崟鑱婁腑鐐瑰嚮澧炲姞鎸夐挳瑙﹀彂浜嬩欢锛屽垱寤虹兢鑱�
     *
     * @param newMember 瑕佸鍔犵殑鎴愬憳
     */
    private void addMemberAndCreateGroup(final String newMember) {
        mLD = new DialogCreator();
        mLoadingDialog = mLD.createLoadingDialog(mContext, mContext.getString(R.string.creating_hint));
        mLoadingDialog.show();
        JMessageClient.createGroup("", "", new CreateGroupCallback(false) {
            @Override
            public void gotResult(int status, final String desc, final long groupID) {
                if (status == 0) {
                    ArrayList<String> list = new ArrayList<String>();
                    list.add(mTargetID);
                    list.add(newMember);
                    JMessageClient.addGroupMembers(groupID, list, new BasicCallback(false) {
                        @Override
                        public void gotResult(int status, String desc) {
                            if (mLoadingDialog != null)
                                mLoadingDialog.dismiss();
                            Conversation conv = Conversation.createConversation(ConversationType.group, groupID);
                            if (status == 0) {
                                mContext.StartChatActivity(groupID, conv.getTitle());
                            } else {
                                mContext.StartChatActivity(groupID, conv.getTitle());
                                Toast.makeText(mContext, desc, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mLoadingDialog != null)
                            mLoadingDialog.dismiss();
                        Toast.makeText(mContext, desc, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // 缇ゆ垚鍛樺彉鍖�

    @Override
    public boolean onItemLongClick(AdapterView<?> viewAdapter, View view,
                                   int position, long id) {
        // 鏄兢涓诲苟鍦ㄩ潪鍒犻櫎鐘舵�涓嬮暱鎸塈tem瑙﹀彂浜嬩欢
        if (!mIsShowDelete && mIsCreator) {
            if (position < mCurrentNum && mCurrentNum > 1) {
                mIsShowDelete = true;
                mGridAdapter.setIsShowDelete(true,
                        mRestArray[mCurrentNum % 4]);
            }
        }
        return true;
    }

    public long getGroupID(){
        return mGroupID;
    }

    /**
     * 褰撴敹鍒扮兢鎴愬憳鍙樺寲鐨凟vent鍚庯紝鍒锋柊鎴愬憳鍒楄〃
     *
     * @param groupID 缇ょ粍ID
     */
    public void refresh(long groupID) {
        //褰撳墠缇よ亰
        if (mGroupID == groupID) {
            JMessageClient.getGroupMembers(groupID, new GetGroupMembersCallback() {
                @Override
                public void gotResult(int status, String s, List<UserInfo> memberList) {
                    if (status == 0) {
                        mMemberIDList = memberList;
                        mCurrentNum = mMemberIDList.size();
                        mChatDetailView.setTitle(mCurrentNum);
                        if (mGridAdapter != null)
                            mGridAdapter.refreshGroupMember(mMemberIDList);
                    }
                }
            });
            Log.i(TAG, "Group Member Changing");
        }
    }

    //鍒锋柊
    public void NotifyGroupChange() {
        if (mGridAdapter != null)
            mGridAdapter.notifyDataSetChanged();
    }

}
