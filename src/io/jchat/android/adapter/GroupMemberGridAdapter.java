package io.jchat.android.adapter;

import io.jchat.android.activity.R;
import io.jchat.android.tools.BitmapLoader;
import io.jchat.android.tools.NativeImageLoader;
import io.jchat.android.view.CircleImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.callback.DownloadAvatarCallback;
import cn.jpush.im.android.api.model.UserInfo;

public class GroupMemberGridAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    //缇ゆ垚鍛樺垪琛�
    private List<UserInfo> mMemberList = new ArrayList<UserInfo>();
    //绌虹櫧椤瑰垪琛�
    private ArrayList<String> mBlankList = new ArrayList<String>();
    private boolean mIsCreator = false;
    private boolean mIsShowDelete;
    //缇ゆ垚鍛樹釜鏁�
    private int mCurrentNum;
    //璁板綍绌虹櫧椤圭殑鏁扮粍
    private int[] mRestArray = new int[]{2, 1, 0, 3};
    //鐢ㄧ兢鎴愬憳椤规暟浣�寰楀埌锛屼綔涓轰笅鏍囨煡鎵緈RestArray锛屽緱鍒扮┖鐧介」
    private int mRestNum;
    private int mDefaultSize;

    public GroupMemberGridAdapter(Context context, List<UserInfo> memberList,
                                  boolean isCreator, boolean isGroup) {
        this.mMemberList = memberList;
        this.mIsCreator = isCreator;
        mInflater = LayoutInflater.from(context);
        mIsShowDelete = false;
        initBlankItem();
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        mDefaultSize = (int )(50 * dm.density);
    }

    public void initBlankItem() {
        mCurrentNum = mMemberList.size();
        mRestNum = mRestArray[mCurrentNum % 4];
        //琛ュ叏绌虹櫧椤�
        for (int i = 0; i < mRestNum; i++) {
            mBlankList.add("");
        }
    }

    public void refreshGroupMember(List<UserInfo> memberList) {
        mMemberList.clear();
        mMemberList = memberList;
        initBlankItem();
        notifyDataSetChanged();
    }

    public void addMemberToList(UserInfo userInfo) {
        if (!mMemberList.contains(userInfo)) {
            mMemberList.add(userInfo);
        }
        initBlankItem();
        notifyDataSetChanged();
    }

    public void remove(int position) {
        if (position >= mMemberList.size()) {
            return;
        }
        mMemberList.remove(position);
        --mCurrentNum;
        mRestNum = mRestArray[mCurrentNum % 4];
        notifyDataSetChanged();
    }

    public void setIsShowDelete(boolean isShowDelete) {
        this.mIsShowDelete = isShowDelete;
        notifyDataSetChanged();
    }

    public void setIsShowDelete(boolean isShowDelete, int restNum) {
        this.mIsShowDelete = isShowDelete;
        mRestNum = restNum;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        //濡傛灉鏄櫘閫氭垚鍛橈紝骞朵笖缇ょ粍鎴愬憳浣�绛変簬3锛岀壒娈婂鐞嗭紝闅愯棌涓嬮潰涓�爮绌虹櫧
        if(mCurrentNum % 4 == 3 && !mIsCreator)
            return mCurrentNum + 1;
        else return mCurrentNum + mRestNum + 2;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemViewTag viewTag;
        Bitmap bitmap;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.group_grid_view_item, null);
            viewTag = new ItemViewTag((CircleImageView) convertView.findViewById(R.id.grid_avatar),
                    (TextView) convertView.findViewById(R.id.grid_name),
                    (ImageView) convertView.findViewById(R.id.grid_delete_icon));
            convertView.setTag(viewTag);
        } else {
            viewTag = (ItemViewTag) convertView.getTag();
        }

        if(position < mCurrentNum){
            UserInfo userInfo = mMemberList.get(position);
            viewTag = (ItemViewTag) convertView.getTag();
            viewTag.icon.setVisibility(View.VISIBLE);
            viewTag.name.setVisibility(View.VISIBLE);
            bitmap = NativeImageLoader.getInstance().getBitmapFromMemCache(userInfo.getUserName());
            if (bitmap != null)
                viewTag.icon.setImageBitmap(bitmap);
            else{
                //濡傛灉mediaID涓虹┖锛岃〃鏄庣敤鎴锋病鏈夎缃繃澶村儚锛岀敤榛樿澶村儚
                if(TextUtils.isEmpty(userInfo.getAvatar())){
                    viewTag.icon.setImageResource(R.drawable.head_icon);
                }else {
                    File file = userInfo.getAvatarFile();
                    //濡傛灉鏈湴瀛樺湪澶村儚
                    if(file != null && file.isFile()){
                        bitmap = BitmapLoader.getBitmapFromFile(file.getAbsolutePath(), mDefaultSize, mDefaultSize);
                        NativeImageLoader.getInstance().updateBitmapFromCache(userInfo.getUserName(), bitmap);
                        viewTag.icon.setImageBitmap(bitmap);
                        //浠庣綉涓婃嬁澶村儚
                    }else {
                        viewTag.icon.setImageResource(R.drawable.head_icon);
                        final String userName = userInfo.getUserName();
                        userInfo.getAvatarFileAsync(new DownloadAvatarCallback() {
                            @Override
                            public void gotResult(int status, String desc, File file) {
                                if(status == 0){
                                    Bitmap bitmap = BitmapLoader.getBitmapFromFile(file.getAbsolutePath(),  mDefaultSize,  mDefaultSize);
                                    NativeImageLoader.getInstance().updateBitmapFromCache(userName, bitmap);
                                    notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }
            }

            if(TextUtils.isEmpty(userInfo.getNickname())){
                viewTag.name.setText(userInfo.getUserName());
            }else {
                viewTag.name.setText(userInfo.getNickname());
            }
        }
        //鏄疍elete鐘舵�
        if (mIsShowDelete) {
            if (position < mCurrentNum) {
                UserInfo userInfo = mMemberList.get(position);
                //缇や富涓嶈兘鍒犻櫎鑷繁
                if (userInfo.getUserName().equals(JMessageClient.getMyInfo().getUserName()))
                    viewTag.deleteIcon.setVisibility(View.GONE);
                else viewTag.deleteIcon.setVisibility(View.VISIBLE);

            } else {
                viewTag.deleteIcon.setVisibility(View.INVISIBLE);
                viewTag.icon.setVisibility(View.INVISIBLE);
                viewTag.name.setVisibility(View.INVISIBLE);
            }
            //闈濪elete鐘舵�
        } else {
            viewTag.deleteIcon.setVisibility(View.INVISIBLE);
            if(position < mCurrentNum){
                viewTag.icon.setVisibility(View.VISIBLE);
                viewTag.name.setVisibility(View.VISIBLE);
            }else if (position == mCurrentNum) {
                viewTag = (ItemViewTag) convertView.getTag();
                viewTag.icon.setImageResource(R.drawable.chat_detail_add);
                viewTag.icon.setVisibility(View.VISIBLE);
                viewTag.name.setVisibility(View.INVISIBLE);

                //璁剧疆鍒犻櫎缇ゆ垚鍛樻寜閽�
            } else if (position == mCurrentNum + 1) {
                if (mIsCreator && mCurrentNum > 1) {
                    viewTag = (ItemViewTag) convertView.getTag();
                    viewTag.icon.setImageResource(R.drawable.chat_detail_del);
                    viewTag.icon.setVisibility(View.VISIBLE);
                    viewTag.name.setVisibility(View.INVISIBLE);
                } else {
                    viewTag = (ItemViewTag) convertView.getTag();
                    viewTag.icon.setVisibility(View.GONE);
                    viewTag.name.setVisibility(View.GONE);
                }
                //绌虹櫧椤�
            } else {
                viewTag = (ItemViewTag) convertView.getTag();
                viewTag.icon.setVisibility(View.INVISIBLE);
                viewTag.name.setVisibility(View.INVISIBLE);
            }
        }


        return convertView;
    }

    public void setCreator(boolean isCreator) {
        mIsCreator = isCreator;
        notifyDataSetChanged();
    }


    class ItemViewTag {

        protected CircleImageView icon;
        protected ImageView deleteIcon;
        protected TextView name;

        public ItemViewTag(CircleImageView icon, TextView name, ImageView deleteIcon) {
            this.icon = icon;
            this.deleteIcon = deleteIcon;
            this.name = name;
        }
    }
}
