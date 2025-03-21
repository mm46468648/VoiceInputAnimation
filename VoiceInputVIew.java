package com.cars.guazi.mp.ai.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.cars.awesome.utils.android.ScreenUtil;
import com.cars.guazi.mp.ai.R;
import com.cars.guazi.mp.ai.databinding.ViewVoiceInputBinding;
import com.cars.guazi.mp.utils.ToastUtil;


/**
 * 语音输入控件
 */
public class VoiceInputVIew extends FrameLayout implements View.OnClickListener {

    private static final String TAG = "VoiceInputVIew";
    public static final int DEFAULT_VOICE_INPUT_ING_STATE = 0; //默认语音输入中状态
    public static final int DEFAULT_VOICE_INPUT_OVER_STATE = 1; //语音输入完毕状态
    public static final int EDIT_USER_KEYBOARD_INPUT_STATE = 2; //编辑状态下用输入键盘
    public static final int EDIT_USE_VOICE_INPUT_STATE = 3; //编辑状态下用语音输入
    public static final int EDIT_VOICE_INPUT_ING_STATE = 4; //编辑状态下用语音输入中

    int currentState = 1;
    private Vibrator vibrator;

    // 长按的延迟时间，单位为毫秒
    private static final long LONG_PRESS_DELAY = 500;
    // 允许的触摸移动范围
    private static final int TOUCH_SLOP = 20;
    // 记录触摸开始的 X 坐标
    private float mDownX;
    // 记录触摸开始的 Y 坐标
    private float mDownY;
    // 记录触摸开始的时间
    private long mDownTime;
    // 用于处理长按延迟任务的 Handler
    private Handler mHandler;
    // 标识是否为长按状态
    private boolean isLongPress;
    //创建手势识别帮助类
    private ViewVoiceInputBinding mBinding;
    private int mScreenHeight;
    private int lastKeyBoardHeight = 0;
    Context mContext;

    public VoiceInputVIew(@NonNull Context context) {
        super(context);
        init(context);
    }

    public VoiceInputVIew(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VoiceInputVIew(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.view_voice_input, this, true);
        mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if (msg.what == 0) {
                    // 触发长按事件
                    isLongPress = true;
                    onLongPress();
                }
                return true;
            }
        });
        initListener();

    }

    int keyboardHeight = 0;

    private void initListener() {

        //监听EditText的获取焦点事件
        mBinding.etVoiceInput.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                //打印日志
                Log.d(TAG, "EditText获取焦点：" + hasFocus);
                if (hasFocus) {
                    // 当EditText获取焦点时，切换到编辑状态
                    setVoiceInputState(EDIT_USER_KEYBOARD_INPUT_STATE);
                }
            }
        });
        mBinding.setOnClickListener(this);

        mScreenHeight = getResources().getDisplayMetrics().heightPixels;

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int[] location = new int[2];
                getLocationOnScreen(location);
                int keyboardHeight = mScreenHeight - (location[1] + getHeight());

                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                Log.d(TAG, "onGlobalLayout: keyboardHeight = " + keyboardHeight);

                if(keyboardHeight == mScreenHeight){
                    return;
                }
                if (lastKeyBoardHeight == keyboardHeight) {
                    return;
                }
                lastKeyBoardHeight = keyboardHeight;
                if (keyboardHeight > 100) {
                    VoiceInputVIew.this.keyboardHeight = keyboardHeight;
                    Log.d(TAG, "onGlobalLayout: 键盘弹出");
                    onKeyboardVisible();
                } else {
                    Log.d(TAG, "onGlobalLayout: 键盘隐藏");
                    onKeyboardInVisible();
                }
            }
        });

        //监听EditText变化
        mBinding.etVoiceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                VoiceInputVIew.this.currentMessage = s.toString();
//                mBinding.etVoiceHint.setHint(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * 软键盘弹出
     */
    void onKeyboardVisible() {
        if(currentState == DEFAULT_VOICE_INPUT_ING_STATE){
            return;
        }
        //设置mBinding.flInputState2的marginbottom为键盘高度 - 191dp
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mBinding.flInputState3.getLayoutParams();
        layoutParams.bottomMargin = 0;
        mBinding.flInputState3.setLayoutParams(layoutParams);
        //输入框的高度为131+软键盘的高度
        FrameLayout.LayoutParams editLayoutParams = (FrameLayout.LayoutParams) mBinding.etVoiceInput.getLayoutParams();
        editLayoutParams.height = (int) (131 * getResources().getDisplayMetrics().density) + keyboardHeight;
        mBinding.etVoiceInput.setLayoutParams(editLayoutParams);



        postDelayed(new Runnable() {
            @Override
            public void run() {
                setVoiceInputState(EDIT_USER_KEYBOARD_INPUT_STATE);

            }
        },200);
    }

    /**
     * 软键盘隐藏
     */
    void onKeyboardInVisible() {
        if(currentState == DEFAULT_VOICE_INPUT_ING_STATE || currentState == DEFAULT_VOICE_INPUT_OVER_STATE){
            return;
        }

        //设置mBinding.flInputState2的marginbottom为键盘高度 - 191dp
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mBinding.flInputState3.getLayoutParams();
        int bottomMargin = (int) (keyboardHeight - 191 * getResources().getDisplayMetrics().density);
        layoutParams.bottomMargin = bottomMargin;
        mBinding.flInputState3.setLayoutParams(layoutParams);

        postDelayed(new Runnable() {
            @Override
            public void run() {
                setVoiceInputState(EDIT_USE_VOICE_INPUT_STATE);

            }
        },200);


    }

    void onCollapse(){
        mBinding.rlEditStateTop.setVisibility(GONE);
        mBinding.flInputState3.setVisibility(GONE);
        mBinding.flInputState2.setVisibility(GONE);
        //Edit失去焦点
        mBinding.etVoiceInput.clearFocus();
        //关闭软键盘
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mBinding.etVoiceInput.getWindowToken(), 0);

        //设置mBinding.flInputState2的marginbottom为键盘高度 - 191dp
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mBinding.flInputState3.getLayoutParams();
        layoutParams.bottomMargin = 0;
        mBinding.flInputState3.setLayoutParams(layoutParams);
        //恢复输入框的高度为131
        FrameLayout.LayoutParams editLayoutParams = (FrameLayout.LayoutParams) mBinding.etVoiceInput.getLayoutParams();
        editLayoutParams.height = ScreenUtil.dp2px(131);
        mBinding.etVoiceInput.setLayoutParams(editLayoutParams);
        setVoiceInputState(DEFAULT_VOICE_INPUT_OVER_STATE);
    }

    public void setVoiceInputState(int state) {
        Log.d(TAG, "setVoiceInputState: " + state);
        switch (state) {
            case DEFAULT_VOICE_INPUT_ING_STATE:
                mBinding.llInputState0.setVisibility(VISIBLE);
                mBinding.llInputState1.setVisibility(INVISIBLE);
                break;
            case DEFAULT_VOICE_INPUT_OVER_STATE:
                mBinding.llInputState0.setVisibility(INVISIBLE);
                mBinding.llInputState1.setVisibility(VISIBLE);
                break;
            case EDIT_USER_KEYBOARD_INPUT_STATE:
                mBinding.llInputState0.setVisibility(GONE);
                mBinding.llInputState1.setVisibility(GONE);

                mBinding.flInputState2.setVisibility(VISIBLE);
                mBinding.flInputState3.setVisibility(GONE);

                mBinding.rlEditStateTop.setVisibility(VISIBLE);
                break;
            case EDIT_USE_VOICE_INPUT_STATE:
                mBinding.llInputState0.setVisibility(INVISIBLE);
                mBinding.llInputState1.setVisibility(VISIBLE);

                mBinding.flInputState2.setVisibility(GONE);
                mBinding.flInputState3.setVisibility(VISIBLE);

                mBinding.rlEditStateTop.setVisibility(VISIBLE);
                break;

            case EDIT_VOICE_INPUT_ING_STATE:
                mBinding.llInputState0.setVisibility(VISIBLE);
                mBinding.llInputState1.setVisibility(INVISIBLE);

                mBinding.flInputState2.setVisibility(GONE);
                mBinding.flInputState3.setVisibility(VISIBLE);

                mBinding.rlEditStateTop.setVisibility(VISIBLE);
                break;
        }
        currentState = state;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();

        int[] micLocation = new int[2];
        mBinding.ivMic.getLocationInWindow(micLocation);
        int[] viewLocation = new int[2];
        getLocationInWindow(viewLocation);

        int micLeft = micLocation[0] - viewLocation[0];
        int micTop = micLocation[1] - viewLocation[1];
        int micRight = micLeft + mBinding.ivMic.getWidth();
        int micBottom = micTop + mBinding.ivMic.getHeight();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownTime = System.currentTimeMillis();
                Log.d(TAG, "onTouchEvent: ACTION_DOWN" + mDownTime);
                mDownX = x;
                mDownY = y;
                mDownTime = System.currentTimeMillis();
                isLongPress = false;
                // 判断是否在 iv_mic 区域内
                if (x >= micLeft && x <= micRight && y >= micTop && y <= micBottom) {
                    // 发送延迟消息，用于判断长按
                    mHandler.sendEmptyMessageDelayed(0, LONG_PRESS_DELAY);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // 检查触摸移动是否超出范围
                if (Math.abs(x - mDownX) > TOUCH_SLOP || Math.abs(y - mDownY) > TOUCH_SLOP) {
                    // 超出范围，取消长按判定
                    mHandler.removeMessages(0);
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onTouchEvent: ACTION_UP" + (System.currentTimeMillis() - mDownTime));
                getParent().requestDisallowInterceptTouchEvent(false);
                mHandler.removeMessages(0);

               //写一段逻辑判断是点击事件
                if (System.currentTimeMillis() - mDownTime < LONG_PRESS_DELAY) {
                    // 如果不是长按，则处理点击事件
                    // 判断点击了ll_bottom以上的区域
                    if (event.getY() < mBinding.llBottom.getTop()) {
                        // 点击了ll_bottom以上的区域
                        // 处理点击事件
                        onClickTop();
                    }
                    break;
                }

                // 处理触摸抬起事件
                if (isLongPress) {
                    onTouchUp();
                }

                //判断点击事件

                break;
        }
        return true;
    }

    private void onClickTop() {
//        ToastUtil.showHintToast("点击了顶部区域");
        mBinding.etVoiceInput.setText("");
        setVisibility(GONE);
    }

    // 长按事件处理方法
    private void onLongPress() {
        if (currentState == DEFAULT_VOICE_INPUT_ING_STATE) {
            return;
        }

        middleStartMessage = mBinding.etVoiceInput.getText().toString();
        middleStartCursorPosition = mBinding.etVoiceInput.getSelectionStart();
        //实现系统震动
        triggerVibration();
        // 这里可以添加长按事件的处理逻辑
        System.out.println("Long press detected");

        setVoiceInputState(DEFAULT_VOICE_INPUT_ING_STATE);
        if (mOnLongPressListener != null) {
            mOnLongPressListener.onLongPress();
        }

    }

    // 触摸抬起事件处理方法
    private void onTouchUp() {
        // 这里可以添加触摸抬起事件的处理逻辑
        System.out.println("Touch up detected");
        setVoiceInputState(DEFAULT_VOICE_INPUT_OVER_STATE);

        //松手，显示按钮，隐藏输入中状态
        mBinding.llInputState0.setVisibility(INVISIBLE);
        mBinding.llInputState1.setVisibility(VISIBLE);

        if (mOnLongPressListener != null) {
            mOnLongPressListener.onTouchUp();
        }
    }

    /**
     * 展示录音状态并且开始识别
     */
    public void showInputIngStateAndStartRecord(){
        onLongPress();
    }

    /**
     * 外部按钮的长按事件
     */
    public void outSideTouchUp(){
        onTouchUp();
    }

    private void triggerVibration() {
        if (vibrator == null) {
            // 获取震动服务
            vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0 及以上版本使用 VibrationEffect
                VibrationEffect vibrationEffect = VibrationEffect.createOneShot(100, 50);
                vibrator.vibrate(vibrationEffect);
            } else {
                // Android 8.0 以下版本使用旧的方法
                vibrator.vibrate(100);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tv_collapse) {
            onCollapse();


        } else if (id == R.id.ib_keyboard) {
            setVoiceInputState(EDIT_USER_KEYBOARD_INPUT_STATE);
            //打开软键盘
            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mBinding.etVoiceInput, InputMethodManager.SHOW_IMPLICIT);
        } else if (id == R.id.ib_voice_input) {
            //关闭软键盘
            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mBinding.etVoiceInput.getWindowToken(), 0);
//            setVoiceInputState(EDIT_USE_VOICE_INPUT_STATE);
        } else if (id == R.id.tv_clear) {
            mBinding.etVoiceInput.setText("");
            setVisibility(GONE);
        } else if (id == R.id.tv_send || id == R.id.tv_state2_send) {
            if (mOnLongPressListener != null) {
                mOnLongPressListener.onSend(mBinding.etVoiceInput.getText().toString());
            }
            onCollapse();
            post(() -> {
                mBinding.etVoiceInput.setText("");
                setVisibility(GONE);
            });



        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setKeyboardHeight(int keyboardHeight) {
        this.keyboardHeight = keyboardHeight;
        //，重新设置RootView的paddingBottom
        mBinding.getRoot().setPadding(0, 0, 0, keyboardHeight);
    }

    ValueAnimator animator;
    int lastVolume = 0;
    public void setVolume(float volume, float maxVolume, float minVolume) {
        if(maxVolume <= VoiceWaveView.waveSilentVolume){
            mBinding.voiceWaveView.setMaxVolume((int) maxVolume);
            mBinding.voiceWaveView.setMinVolume((int) minVolume);
            mBinding.voiceWaveView.setCurrentVolume((int) volume);
            return;
        }
        //如果音量设置频率过快该如何限制
        //写一个Value属性动画改变音量的属性动画
        //如果动画执行过程中则不执行
//        if (animator != null && animator.isRunning()) {
//            Log.d(TAG, "animator isRunning: " + System.currentTimeMillis());
//            return;
//        }
        animator = ValueAnimator.ofFloat(lastVolume, volume);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mBinding.voiceWaveView.setMaxVolume((int) maxVolume);
                mBinding.voiceWaveView.setMinVolume((int) minVolume);
                float value = (float) animation.getAnimatedValue();
                mBinding.voiceWaveView.setCurrentVolume((int) value);
            }

        });
        animator.setDuration(500);
        animator.start();
        lastVolume = (int) volume;

    }

    OnLongPressListener mOnLongPressListener;

    public void setOnLongPressListener(OnLongPressListener onLongPressListener) {
        mOnLongPressListener = onLongPressListener;
    }

    private String currentMessage = "";

    String middleStartMessage = ""; //中间态开始文案
    int middleStartCursorPosition = 0;
    /**
     * 设置中间态文案
     * @param message
     */
    public void setMiddleMessage(String message) {
        //去掉message中的/r/n
        message = message.replaceAll("\r\n", "");
        int cursorPosition =middleStartCursorPosition;
        Log.d(TAG, "setMiddleMessage cursorPosition: " + cursorPosition);

        //将message拼接到当前光标位置对应的message之后
        String temp = middleStartMessage.substring(0, cursorPosition) + message + middleStartMessage.substring(cursorPosition);
        Log.d(TAG, "setMiddleMessage temp: " + temp);
        mBinding.etVoiceInput.setText(temp);

        int movePosition = cursorPosition + message.length();
        Log.d(TAG, "setArsResult movePosition: " + movePosition + " currentMessage length: " + currentMessage.length());
        if (movePosition > temp.length()) {
            movePosition = temp.length();
        }
        mBinding.etVoiceInput.setSelection(movePosition);
    }

    /**
     * 设置最终态结果
     * 并设置最终的光标位置
     * @param message
     */
    public void setArsResult(String message) {
    }


    public interface OnLongPressListener {
        void onLongPress();

        void onTouchUp();

        void onSend(String message);
    }
}
