package io.jchat.android.activity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import io.jchat.android.adapter.AlbumListAdapter;
import io.jchat.android.entity.ImageBean;
import io.jchat.android.tools.SortPictureList;

/*
 * 鏈湴鍥剧墖闆嗙晫闈�
 */
public class PickPictureTotalActivity extends BaseActivity {
    private HashMap<String, List<String>> mGruopMap = new HashMap<String, List<String>>();
	private List<ImageBean> list = new ArrayList<ImageBean>();
	private final static int SCAN_OK = 1;
    private final static int SCAN_ERROR = 2;
	private ProgressDialog mProgressDialog;
//	private PickPictureTotalAdapter adapter;
//	private GridView mGroupGridView;
    private AlbumListAdapter adapter;
    private ListView mListView;
	private ImageButton mReturnBtn;
	private TextView mTitle;
	private ImageButton mMenuBtn;
	private Intent mIntent;
	private final MyHandler myHandler = new MyHandler(this);

	private static class MyHandler extends Handler{
		private final WeakReference<PickPictureTotalActivity> mActivity;

		public MyHandler(PickPictureTotalActivity activity){
			mActivity = new WeakReference<PickPictureTotalActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			PickPictureTotalActivity activity = mActivity.get();
			if(activity != null){
				switch (msg.what) {
					case SCAN_OK:
						//鍏抽棴杩涘害鏉�
						activity.mProgressDialog.dismiss();
						activity.adapter = new AlbumListAdapter(activity, activity.list = activity.subGroupOfImage(activity.mGruopMap), activity.mListView);
						activity.mListView.setAdapter(activity.adapter);
						break;
					case SCAN_ERROR:
						activity.mProgressDialog.dismiss();
						Toast.makeText(activity, activity.getString(R.string.sdcard_not_prepare_toast), Toast.LENGTH_SHORT).show();
						break;
				}
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pick_picture_total);
		mReturnBtn = (ImageButton) findViewById(R.id.return_btn);
		mTitle = (TextView) findViewById(R.id.title);
		mMenuBtn = (ImageButton) findViewById(R.id.right_btn);
//		mGroupGridView = (GridView) findViewById(R.id.pick_picture_total_grid_view);
		mListView = (ListView) findViewById(R.id.pick_picture_total_list_view);
		mTitle.setText(this.getString(R.string.choose_album_title));
		mMenuBtn.setVisibility(View.GONE);
		getImages();
		mIntent = this.getIntent();
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				List<String> childList = mGruopMap.get(list.get(position).getFolderName());
				mIntent.setClass(PickPictureTotalActivity.this, PickPictureActivity.class);
				mIntent.putStringArrayListExtra("data", (ArrayList<String>)childList);
				startActivity(mIntent);
				
			}
		});
		
		mReturnBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				finish();
			}
			
		});
		
	}

    @Override
    protected void onPause() {
        mProgressDialog.dismiss();
        super.onPause();
    }

    /**
	 * 鍒╃敤ContentProvider鎵弿鎵嬫満涓殑鍥剧墖锛屾鏂规硶鍦ㄨ繍琛屽湪瀛愮嚎绋嬩腑
	 */
	private void getImages() {
		//鏄剧ず杩涘害鏉�
		mProgressDialog = ProgressDialog.show(this, null, PickPictureTotalActivity.this.getString(R.string.loading));
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				ContentResolver mContentResolver = PickPictureTotalActivity.this.getContentResolver();

				//鍙煡璇peg鍜宲ng鐨勫浘鐗�
				Cursor mCursor = mContentResolver.query(mImageUri, null,
						MediaStore.Images.Media.MIME_TYPE + "=? or "
								+ MediaStore.Images.Media.MIME_TYPE + "=?",
						new String[] { "image/jpeg", "image/png" }, MediaStore.Images.Media.DATE_MODIFIED);
				if(mCursor == null || mCursor.getCount() == 0){
					myHandler.sendEmptyMessage(SCAN_ERROR);
                }else{
                    while (mCursor.moveToNext()) {
                        //鑾峰彇鍥剧墖鐨勮矾寰�
                        String path = mCursor.getString(mCursor
                                .getColumnIndex(MediaStore.Images.Media.DATA));

                        try{
                            //鑾峰彇璇ュ浘鐗囩殑鐖惰矾寰勫悕
                            String parentName = new File(path).getParentFile().getName();
                            //鏍规嵁鐖惰矾寰勫悕灏嗗浘鐗囨斁鍏ュ埌mGruopMap涓�
                            if (!mGruopMap.containsKey(parentName)) {
                                List<String> chileList = new ArrayList<String>();
                                chileList.add(path);
                                mGruopMap.put(parentName, chileList);
                            } else {
                                mGruopMap.get(parentName).add(path);
                            }
                        }catch (Exception e){
                        }
                    }
                    mCursor.close();
                    //閫氱煡Handler鎵弿鍥剧墖瀹屾垚
					myHandler.sendEmptyMessage(SCAN_OK);
                }
			}
		}).start();
		
	}
	
	
	/**
	 * 缁勮鍒嗙粍鐣岄潰GridView鐨勬暟鎹簮锛屽洜涓烘垜浠壂鎻忔墜鏈虹殑鏃跺�灏嗗浘鐗囦俊鎭斁鍦℉ashMap涓�
	 * 鎵�互闇�閬嶅巻HashMap灏嗘暟鎹粍瑁呮垚List
	 * 
	 * @param mGruopMap
	 * @return
	 */
	private List<ImageBean> subGroupOfImage(HashMap<String, List<String>> mGruopMap){
		if(mGruopMap.size() == 0){
			return null;
		}
		List<ImageBean> list = new ArrayList<ImageBean>();
		
		Iterator<Map.Entry<String, List<String>>> it = mGruopMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, List<String>> entry = it.next();
			ImageBean mImageBean = new ImageBean();
			String key = entry.getKey();
			List<String> value = entry.getValue();
			SortPictureList sortList = new SortPictureList();
			Collections.sort(value, sortList);
			mImageBean.setFolderName(key);
			mImageBean.setImageCounts(value.size());
			mImageBean.setTopImagePath(value.get(0));//鑾峰彇璇ョ粍鐨勭涓�紶鍥剧墖
			
			list.add(mImageBean);
		}
		
		return list;
		
	}
}
