package io.jchat.android.activity;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;

import java.io.File;

import cn.jpush.im.android.api.event.MessageEvent;
import cn.jpush.im.android.api.model.Conversation;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.enums.ConversationType;
import cn.jpush.im.android.api.event.ConversationRefreshEvent;
import cn.jpush.im.android.api.model.Message;

import io.jchat.android.application.JPushDemoApplication;
import io.jchat.android.controller.ConversationListController;
import io.jchat.android.controller.MenuItemController;
import io.jchat.android.tools.NativeImageLoader;
import io.jchat.android.view.ConversationListView;
import io.jchat.android.view.MenuItemView;

/*
 * 浼氳瘽鍒楄〃鐣岄潰
 */
public class ConversationListFragment extends BaseFragment {

    private static String TAG = ConversationListFragment.class.getSimpleName();
    private View mRootView;
    private ConversationListView mConvListView;
    private ConversationListController mConvListController;
    private PopupWindow mMenuPopWindow;
    private View mMenuView;
    private MenuItemView mMenuItemView;
    private MenuItemController mMenuController;
    //MainActivity瑕佸疄鐜扮殑鎺ュ彛锛岀敤鏉ユ樉绀烘垨鑰呴殣钘廇ctionBar涓柊娑堟伅鎻愮ず

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        JMessageClient.registerEventReceiver(this);
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        mRootView = layoutInflater.inflate(R.layout.fragment_conv_list,
                (ViewGroup) getActivity().findViewById(R.id.main_view),
                false);
        mConvListView = new ConversationListView(mRootView, this.getActivity());
        mConvListView.initModule();
        mMenuView = getActivity().getLayoutInflater().inflate(R.layout.drop_down_menu, null);
        mConvListController = new ConversationListController(mConvListView, this);
        mConvListView.setListener(mConvListController);
        mConvListView.setItemListeners(mConvListController);
        mConvListView.setLongClickListener(mConvListController);
        mMenuPopWindow = new PopupWindow(mMenuView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
        mMenuItemView = new MenuItemView(mMenuView);
        mMenuItemView.initModule();
        mMenuController = new MenuItemController(mMenuItemView, this, mConvListController);
        mMenuItemView.setListeners(mMenuController);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    //鏄剧ず涓嬫媺鑿滃崟
    public void showMenuPopWindow() {
        mMenuPopWindow.setTouchable(true);
        mMenuPopWindow.setOutsideTouchable(true);
        mMenuPopWindow.setBackgroundDrawable(new BitmapDrawable(getResources(),
                (Bitmap) null));
        if (mMenuPopWindow.isShowing()) {
            mMenuPopWindow.dismiss();
        } else mMenuPopWindow.showAsDropDown(mRootView.findViewById(R.id.create_group_btn), -10, -5);
    }

    /**
     * 褰撹Е鍙慓etUserInfo鍚庯紝寰楀埌Conversation鍚庯紝鍒锋柊鐣岄潰
     * 閫氬父瑙﹀彂鐨勬儏鍐垫槸鏂颁細璇濆垱寤烘椂鍒锋柊鐩爣澶村儚
     *
     * @param conversationRefreshEvent
     */
    public void onEvent(ConversationRefreshEvent conversationRefreshEvent) {
        Log.i(TAG, "ConversationRefreshEvent execute");
        Conversation conv = conversationRefreshEvent.getConversation();
        if (conv.getType() == ConversationType.single) {
            File file = conv.getAvatarFile();
            if (file != null) {
                mConvListController.loadAvatarAndRefresh(conv.getTargetId(), file.getAbsolutePath());
            }
        } else {
            mConvListController.getAdapter().notifyDataSetChanged();
        }
    }

    /**
     * 鍦ㄤ細璇濆垪琛ㄤ腑鎺ユ敹娑堟伅
     *
     * @param event
     */
    public void onEventMainThread(MessageEvent event) {
        Log.i(TAG, "onEventMainThread MessageEvent execute");
        Message msg = event.getMessage();
        String targetID = msg.getTargetID();
        ConversationType convType = msg.getTargetType();
        Conversation conv;
        if (convType == ConversationType.group) {
            conv = JMessageClient.getGroupConversation(Integer.parseInt(targetID));
        } else {
            conv = JMessageClient.getSingleConversation(targetID);
        }
        if (conv != null && convType == ConversationType.single) {
            //濡傛灉缂撳瓨浜嗗ご鍍忥紝鐩存帴鍒锋柊浼氳瘽鍒楄〃
            if (NativeImageLoader.getInstance().getBitmapFromMemCache(targetID) != null) {
                Log.i("Test", "conversation ");
                mConvListController.refreshConvList();
                //娌℃湁澶村儚锛屼粠Conversation鎷�
            } else {
                File file = conv.getAvatarFile();
                //鎷垮埌鍚庣紦瀛樺苟鍒锋柊
                if (file != null) {
                    mConvListController.loadAvatarAndRefresh(targetID, file.getAbsolutePath());
                    //conversation涓病鏈夊ご鍍忥紝鐩存帴鍒锋柊锛孲DK浼氬湪鍚庡彴鑾峰緱澶村儚锛屾嬁鍒板悗浼氭墽琛宱nEvent(ConversationRefreshEvent conversationRefreshEvent)
                } else mConvListController.refreshConvList();
            }
        } else {
            mConvListController.refreshConvList();
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        ViewGroup p = (ViewGroup) mRootView.getParent();
        if (p != null) {
            p.removeAllViewsInLayout();
        }
        return mRootView;
    }

    @Override
    public void onResume() {
        //褰撳墠鐢ㄦ埛淇℃伅涓虹┖锛岄渶瑕侀噸鏂扮櫥褰�
        if (null == JMessageClient.getMyInfo() || TextUtils.isEmpty(JMessageClient.getMyInfo().getUserName())) {
//            Intent intent = new Intent();
//            intent.setClass(this.getActivity(), LoginActivity.class);
//            startActivity(intent);
//            getActivity().finish();
        } else {
            dismissPopWindow();
            mConvListController.refreshConvList();
        }
        super.onResume();
    }

    public void dismissPopWindow() {
        if (mMenuPopWindow.isShowing()) {
            mMenuPopWindow.dismiss();
        }
    }


    @Override
    public void onDestroy() {
        JMessageClient.unRegisterEventReceiver(this);
        super.onDestroy();
    }


    public void StartCreateGroupActivity() {
        Intent intent = new Intent();
        intent.setClass(getActivity(), CreateGroupActivity.class);
        startActivity(intent);
    }

}
