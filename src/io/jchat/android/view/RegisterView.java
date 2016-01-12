package io.jchat.android.view;

import io.jchat.android.activity.R;
import io.jchat.android.activity.RegisterActivity;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;


public class RegisterView extends LinearLayout {

    private EditText mUserId;
    private EditText mPassword;
    private Button mRegistBtn;
    private ImageButton mReturnBtn;
    private Listener mListener;
    private Context mContext;


    public RegisterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    public void initModule() {
        // 鑾峰彇娉ㄥ唽鎵�敤鐨勭敤鎴峰悕銆佸瘑鐮併�鏄电О
        mUserId = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mRegistBtn = (Button) findViewById(R.id.regist_btn);
        mReturnBtn = (ImageButton) findViewById(R.id.return_btn);
        mUserId.requestFocus();
    }

    public String getUserId() {
        return mUserId.getText().toString().trim();
    }

    public String getPassword() {
        return mPassword.getText().toString().trim();
    }

    public void setListeners(OnClickListener onclickListener) {
        mRegistBtn.setOnClickListener(onclickListener);
        mReturnBtn.setOnClickListener(onclickListener);
    }

    public void userNameError(Context context) {
        Toast.makeText(context, context.getString(R.string.username_not_null_toast), Toast.LENGTH_SHORT).show();
    }

    public void passwordError(Context context) {
        Toast.makeText(context, context.getString(R.string.password_not_null_toast), Toast.LENGTH_SHORT).show();
    }

    public void passwordLengthError(RegisterActivity context) {
        Toast.makeText(context, context.getString(R.string.password_length_illegal), Toast.LENGTH_SHORT).show();
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public interface Listener {
        void onSoftKeyboardShown(int softKeyboardHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Rect rect = new Rect();
        Activity activity = (Activity) getContext();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
//        int screenHeight = activity.getWindowManager().getDefaultDisplay().getHeight();
        int screenHeight = dm.heightPixels;
        int diff = (screenHeight - statusBarHeight) - height;
        if (mListener != null) {
            mListener.onSoftKeyboardShown(diff);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
