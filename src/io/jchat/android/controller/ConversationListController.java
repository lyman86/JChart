package io.jchat.android.controller;

import io.jchat.android.activity.ChatActivity;
import io.jchat.android.activity.ConversationListFragment;
import io.jchat.android.activity.R;
import io.jchat.android.adapter.ConversationListAdapter;
import io.jchat.android.tools.NativeImageLoader;
import io.jchat.android.tools.SortConvList;
import io.jchat.android.view.ConversationListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.TextView;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.enums.ConversationType;
import cn.jpush.im.android.api.model.Conversation;

public class ConversationListController implements OnClickListener,
        OnItemClickListener, OnItemLongClickListener {

    private ConversationListView mConvListView;
    private ConversationListFragment mContext;
    private List<Conversation> mDatas = new ArrayList<Conversation>();
    private ConversationListAdapter mListAdapter;
    private double mDensity;
    private int mWidth;

    public ConversationListController(ConversationListView listView,
                                      ConversationListFragment context) {
        this.mConvListView = listView;
        this.mContext = context;
        DisplayMetrics dm = new DisplayMetrics();
        mContext.getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        mDensity = dm.density;
        mWidth = dm.widthPixels;
        initConvListAdapter();
    }

    // 寰楀埌浼氳瘽鍒楄〃
    private void initConvListAdapter() {
        mDatas = JMessageClient.getConversationList();
        Log.i("ConversationListController", "Conversation size : " + mDatas.size());
        //瀵逛細璇濆垪琛ㄨ繘琛屾椂闂存帓搴�
        if (mDatas.size() > 1) {
            SortConvList sortList = new SortConvList();
            Collections.sort(mDatas, sortList);
        }

        // mDatas = JMessageClient.getConversationList();
        mListAdapter = new ConversationListAdapter(mContext, mDatas);
        mConvListView.setConvListAdapter(mListAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_group_btn:
                mContext.showMenuPopWindow();
                break;
        }
    }

    // 鐐瑰嚮浼氳瘽鍒楄〃
    @Override
    public void onItemClick(AdapterView<?> viewAdapter, View view,
                            int position, long id) {
        // TODO Auto-generated method stub
        final Intent intent = new Intent();
        String targetID = mDatas.get(position).getTargetId();
        intent.putExtra("targetID", targetID);
        mDatas.get(position).resetUnreadCount();
        // 褰撳墠鐐瑰嚮鐨勪細璇濇槸鍚︿负缇ょ粍
        if (mDatas.get(position).getType().equals(ConversationType.group)) {
            intent.putExtra("isGroup", true);
            intent.putExtra("groupID", Long.parseLong(targetID));
            intent.setClass(mContext.getActivity(), ChatActivity.class);
            mContext.startActivity(intent);
            return;
        } else
            intent.putExtra("isGroup", false);
        intent.setClass(mContext.getActivity(), ChatActivity.class);
        mContext.startActivity(intent);

    }

    /*
     * 鍒锋柊浼氳瘽鍒楄〃
     */
    public void refreshConvList() {
        mDatas = JMessageClient.getConversationList();
        SortConvList sortList = new SortConvList();
        Collections.sort(mDatas, sortList);
        mContext.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListAdapter.refresh(mDatas);
            }
        });
    }

    /**
     * 鍔犺浇澶村儚骞跺埛鏂�
     * @param targetID 鐢ㄦ埛鍚�
     * @param path 澶村儚璺緞
     */
    public void loadAvatarAndRefresh(String targetID, String path) {
        int size = (int) (50 * mDensity);
        NativeImageLoader.getInstance().putUserAvatar(targetID, path, size);
        refreshConvList();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> viewAdapter, View view,
                                   final int position, long id) {
        final Conversation conv = mDatas.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(
                mContext.getActivity());
        final View v = LayoutInflater.from(mContext.getActivity()).inflate(
                R.layout.dialog_delete_conv, null);
        builder.setView(v);
        final TextView title = (TextView) v.findViewById(R.id.dialog_title);
        final Button deleteBtn = (Button) v.findViewById(R.id.delete_conv_btn);
        title.setText(conv.getTitle());
        final Dialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setLayout((int)(0.8 * mWidth), WindowManager.LayoutParams.WRAP_CONTENT);
        deleteBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (conv.getType().equals(ConversationType.group))
                    JMessageClient.deleteGroupConversation(Integer.parseInt(conv.getTargetId()));
                else
                    JMessageClient.deleteSingleConversation(conv.getTargetId());
                mDatas.remove(position);
                mListAdapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        });
        return true;
    }

    public ConversationListAdapter getAdapter() {
        return mListAdapter;
    }
}
