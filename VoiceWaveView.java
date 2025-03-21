package com.cars.guazi.mp.ai.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.cars.guazi.mp.ai.utils.DisplayUtil;

import java.util.Random;

public class VoiceWaveView extends View {
    private static final int WAVE_COUNT = 17;
    private static final int DEFAULT_MAX_VOLUME = 100;
    private static final int DEFAULT_MIN_VOLUME = 0;
    private int maxVolume = DEFAULT_MAX_VOLUME;
    private int minVolume = DEFAULT_MIN_VOLUME;
    private int currentVolume = 0;
    private Paint paint;

    int waveWidth = 12;
    int waveSpace = 10;

    private int width;
    private int height;

    int waveColor = Color.parseColor("#ff24BF7E");

    //静音音量
    public static final int waveSilentVolume = 20;
    //静音动效更新频次
    private static final int TIME_CLICK_SPACE_DEFAULT = 30;
    //当前静音动效次数

    //    int[] heightRatios1 = {2, 2, 4, 6, 4, 2, 2, 4, 8, 4, 2, 2, 4, 6, 4, 2, 2};
    int[] heightRatios1 = {4, 4, 6, 8, 6, 4, 4, 6, 8, 6, 4, 4, 6, 8, 6, 4, 4};  //音量条高度比例
    int[] heightRatios2 = {8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8};  //静音音量条高度比例

    
    int currentSilentIndex = 0;//静音时增加高度的位置

    //频率
    int silentUpdateFrequency = 0; //静音动效更新频次
    int randomUpdateFrequency = 0; //随机动效更新频次
    

    public VoiceWaveView(Context context) {
        super(context);
        init(context);
    }

    public VoiceWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VoiceWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        paint.setColor(waveColor);
        paint.setStyle(Paint.Style.FILL);

        width = DisplayUtil.dp2px(context, 180);//适配宽度（根据手机屏幕宽度-50）
        height = DisplayUtil.dp2px(context, 40);

        waveWidth = DisplayUtil.dp2px(context, 6);
        //计算waveSpace的值，让在宽度内均匀分布
        waveSpace = (width - waveWidth * WAVE_COUNT) / (WAVE_COUNT - 1);
    }

    public void setMaxVolume(int maxVolume) {
        this.maxVolume = maxVolume;
    }

    public void setMinVolume(int minVolume) {
        this.minVolume = minVolume;
    }

    private boolean isUpdating = true;

    public void setCurrentVolume(int currentVolume) {
        this.currentVolume = currentVolume;
        invalidate();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //设置宽高
        setMeasuredDimension(width, height);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int height = getHeight();
        int baseWaveHeight = (int) ((currentVolume - minVolume) / (float) (maxVolume - minVolume) * height);
        //打印baseWaveHeight的值，方便调试
//        System.out.println("baseWaveHeight:" + baseWaveHeight);
        // 定义高度变化的比例，让波形有高低变化
        int[] heightRatios = this.heightRatios1;
        if (baseWaveHeight <= waveSilentVolume) {
            heightRatios = this.heightRatios2;
            baseWaveHeight = waveSilentVolume;
            defaultAnim2(canvas, baseWaveHeight, heightRatios, height);

        } else {
            //每30次随机高度变化
            if (randomUpdateFrequency % TIME_CLICK_SPACE_DEFAULT == 0) {
                shuffleArray(heightRatios);
            }
            if (randomUpdateFrequency >= Integer.MAX_VALUE - 1) {
                randomUpdateFrequency = 0;
            }
            randomUpdateFrequency++;
            currentSilentIndex = 0;
            for (int i = 0; i < WAVE_COUNT; i++) {
                int left = i * (this.waveWidth + waveSpace);
                int top = (height - baseWaveHeight * heightRatios[i] / 8) / 2;
                int right = left + this.waveWidth;
                int bottom = top + baseWaveHeight * heightRatios[i] / 8;
                //绘制圆角矩形，radius为60
                canvas.drawRoundRect(left, top, right, bottom, 60, 60, paint);
            }
        }
    }

    void shuffleArray(int[] array) {
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            // 交换 array[i] 和 array[index]
            int temp = array[i];
            array[i] = array[index];
            array[index] = temp;
        }
    }

    private void defaultAnim2(Canvas canvas, int baseWaveHeight, int[] heightRatios, int height) {

        //先每个绘制高度为12的矩形
        for (int i = 0; i < WAVE_COUNT; i++) {
            int left = i * (this.waveWidth + waveSpace);
            int top = (height - baseWaveHeight * heightRatios[i] / 8) / 2;
            int right = left + this.waveWidth;
            int bottom = top + baseWaveHeight * heightRatios[i] / 8;
//            canvas.drawRoundRect(left, top, right, bottom, paint);
            //绘制圆角矩形，radius为60
            canvas.drawRoundRect(left, top, right, bottom, 60, 60, paint);
        }

        //对currentIndex进行自增操作，并取余，实现循环
        int index = (this.currentSilentIndex) % heightRatios.length;

        //绘制他的左边一个
        if (index > 0) {
            int left = (index - 1) * (this.waveWidth + waveSpace);
            int top = (height - baseWaveHeight * 12 / 8) / 2;
            int right = left + this.waveWidth;
            int bottom = top + baseWaveHeight * 12 / 8;
            //绘制圆角矩形，radius为60
            canvas.drawRoundRect(left, top, right, bottom, 60, 60, paint);
        }

        //绘制他的右边一个
        if (index < heightRatios.length - 1) {
            int left = (index + 1) * (this.waveWidth + waveSpace);
            int top = (height - baseWaveHeight * 12 / 8) / 2;
            int right = left + this.waveWidth;
            int bottom = top + baseWaveHeight * 12 / 8;
            canvas.drawRoundRect(left, top, right, bottom, 60, 60, paint);
        }


        //让第index矩形变高
        int left = index * (this.waveWidth + waveSpace);
        int top = (height - baseWaveHeight * 16 / 8) / 2;
        int right = left + this.waveWidth;
        int bottom = top + baseWaveHeight * 16 / 8;
//            canvas.drawRoundRect(left, top, right, bottom, paint);
        //绘制圆角矩形，radius为60
        canvas.drawRoundRect(left, top, right, bottom, 60, 60, paint);

        //每3次执行这个方法，让currentIndex自增1
        if (silentUpdateFrequency % 3 == 0) {
            if (currentSilentIndex >= heightRatios.length - 1) {
                currentSilentIndex = 0;
            }
            currentSilentIndex++;
        }

        if(silentUpdateFrequency >= Integer.MAX_VALUE - 1)
            silentUpdateFrequency = 0;
        else if(silentUpdateFrequency < 0)
            silentUpdateFrequency = 0;
        else
            //否则自增1（防止溢出
        silentUpdateFrequency++;
    }

}
