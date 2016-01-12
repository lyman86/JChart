package io.jchat.android.view;



import io.jchat.android.Listener.OnChangedListener;
import io.jchat.android.activity.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;


public class SlipButton extends View implements OnTouchListener{
	private boolean enabled = true;
	public boolean flag = true;//璁剧疆鍒濆鍖栫姸鎬�
	public boolean NowChoose = true;//璁板綍褰撳墠鎸夐挳鏄惁鎵撳紑,true涓烘墦寮�flase涓哄叧闂�
	private boolean OnSlip = false;//璁板綍鐢ㄦ埛鏄惁鍦ㄦ粦鍔ㄧ殑鍙橀噺
	public float DownX=0f,NowX=0f;//鎸変笅鏃剁殑x,褰撳墠鐨剎,NowX>100鏃朵负ON鑳屾櫙,鍙嶄箣涓篛FF鑳屾櫙
	private Rect Btn_On,Btn_Off;//鎵撳紑鍜屽叧闂姸鎬佷笅,娓告爣鐨凴ect
	
	private boolean isChgLsnOn = false;
	private OnChangedListener ChgLsn;
	private Bitmap bg_on,bg_off, slip_btn;
	private int mId;
	
	public SlipButton(Context context) {
		super(context);
		init();
	}

	public SlipButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public void setChecked(boolean fl){
		if(fl){
			flag = true; NowChoose = true; NowX = 80;
		}else{
			flag = false; NowChoose = false; NowX = 0;
		}
	}
	
	public void setEnabled(boolean b){
		if(b){
			enabled = true;
		}else{
			enabled = false;
		}
	}

	private void init(){//鍒濆鍖�
		//杞藉叆鍥剧墖璧勬簮
		bg_on = BitmapFactory.decodeResource(getResources(), R.drawable.slip_on);
		bg_off = BitmapFactory.decodeResource(getResources(), R.drawable.slip_off);
		slip_btn = BitmapFactory.decodeResource(getResources(), R.drawable.slip);
		//鑾峰緱闇�鐨凴ect鏁版嵁
		Btn_On = new Rect(0,0,slip_btn.getWidth(),slip_btn.getHeight());
		Btn_Off = new Rect(
				bg_off.getWidth()-slip_btn.getWidth(),
				0,
				bg_off.getWidth(),
				slip_btn.getHeight());
		setOnTouchListener(this);//璁剧疆鐩戝惉鍣�涔熷彲浠ョ洿鎺ュ鍐橭nTouchEvent
	}
	
	@Override
	protected void onDraw(Canvas canvas) {//缁樺浘鍑芥暟
		super.onDraw(canvas);
		Matrix matrix = new Matrix();
		Paint paint = new Paint();
		float x;
		{
			if (flag) {NowX = 80;flag = false;
			}//bg_on.getWidth()=71
			if(NowX<(bg_on.getWidth()/2))//婊戝姩鍒板墠鍗婃涓庡悗鍗婃鐨勮儗鏅笉鍚�鍦ㄦ鍋氬垽鏂�
				canvas.drawBitmap(bg_off,matrix, paint);//鐢诲嚭鍏抽棴鏃剁殑鑳屾櫙
			else
				canvas.drawBitmap(bg_on,matrix, paint);//鐢诲嚭鎵撳紑鏃剁殑鑳屾櫙
//			if(NowX >= bg_on.getWidth() - slip_btn.getWidth())
//				canvas.drawBitmap(bg_on, matrix, paint);
//			else if(NowX <= 0)
//				canvas.drawBitmap(bg_off, matrix, paint);
//			else if(0 < NowX && NowX < 80)
//				canvas.drawBitmap(slipping, matrix, paint);
			
			if(OnSlip)//鏄惁鏄湪婊戝姩鐘舵�,
			{
				if(NowX >= bg_on.getWidth())//鏄惁鍒掑嚭鎸囧畾鑼冨洿,涓嶈兘璁╂父鏍囪窇鍒板澶�蹇呴』鍋氳繖涓垽鏂�
					x = bg_on.getWidth()-slip_btn.getWidth()/2;//鍑忓幓娓告爣1/2鐨勯暱搴�..
				else
					x = NowX - slip_btn.getWidth()/2;
			}else{//闈炴粦鍔ㄧ姸鎬�
				if(NowChoose)//鏍规嵁鐜板湪鐨勫紑鍏崇姸鎬佽缃敾娓告爣鐨勪綅缃�
					x = Btn_Off.left;
				else
					x = Btn_On.left;
			}
		if(x<0)//瀵规父鏍囦綅缃繘琛屽紓甯稿垽鏂�..
			x = 0;
		else if(x>bg_on.getWidth()-slip_btn.getWidth())
			x = bg_on.getWidth()-slip_btn.getWidth();
		canvas.drawBitmap(slip_btn,x, 0, paint);//鐢诲嚭娓告爣.
		}
	}


	public boolean onTouch(View v, MotionEvent event) {
		if(!enabled){
			return false;
		}
		switch(event.getAction())//鏍规嵁鍔ㄤ綔鏉ユ墽琛屼唬鐮�
		{
		case MotionEvent.ACTION_MOVE://婊戝姩
			OnSlip = true;
			NowX = event.getX();
			break;
		case MotionEvent.ACTION_DOWN://鎸変笅
		if(event.getX()>bg_on.getWidth()||event.getY()>bg_on.getHeight())
            return false;
			DownX = event.getX();
			NowX = DownX;
			break;
		case MotionEvent.ACTION_UP://鏉惧紑
			OnSlip = false;
			boolean LastChoose = NowChoose;
			if(event.getX() >= (bg_on.getWidth()/2))
				NowChoose = true;
			else
				NowChoose = false;
			if(isChgLsnOn && (LastChoose!=NowChoose))//濡傛灉璁剧疆浜嗙洃鍚櫒,灏辫皟鐢ㄥ叾鏂规硶..
				ChgLsn.OnChanged(mId, NowChoose);
			break;
		default:
		
		}
		invalidate();//閲嶇敾鎺т欢
		return true;
	}
	
	public void setOnChangedListener(int id, OnChangedListener l){//璁剧疆鐩戝惉鍣�褰撶姸鎬佷慨鏀圭殑鏃跺�
        mId = id;
		isChgLsnOn = true;
		ChgLsn = l;
	}
}
