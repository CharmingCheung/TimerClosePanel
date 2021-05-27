package com.zh.android.timerclosepanel;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 定时关闭面板View
 */
public class TimerClosePanel extends FrameLayout {
  private TextView vTitle;
  private Switch vTimerSwitch;
  private TextView vIndicator;
  private SeekBar vSeekBar;
  private TextView vStartTime;
  private TextView vEndTime;

  /**
   * 指示器的宽度
   */
  int mIndicatorWidth;
  /**
   * 滑块的宽度
   */
  int mThumbWidth;
  /**
   * 是否开启
   */
  private boolean isOpen;
  /**
   * 开始时间文本
   */
  private String mStartTimeText;
  /**
   * 结束时间文本
   */
  private String mEndTimeText;
  /**
   * 当前值，单位为分钟
   */
  private int mCurrent;
  /**
   * 最大值，单位为分钟，默认8个小时
   */
  private int mMax = 480;
  /**
   * 回调集合
   */
  private List<Callback> mCallbacks = new ArrayList<>();
  private String mIndicatorText;
  private String mTitle;
  private int mMin;

  public TimerClosePanel(@NonNull Context context) {
    this(context, null);
  }

  public TimerClosePanel(@NonNull Context context,
      @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TimerClosePanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs, defStyleAttr);
  }

  private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    LayoutInflater.from(context).inflate(R.layout.view_timer_close_panel, this);
    findView(this);
    bindView();
  }

  private void findView(View view) {
    vTitle = view.findViewById(R.id.title);
    vTimerSwitch = view.findViewById(R.id.timer_switch);
    vIndicator = view.findViewById(R.id.indicator);
    vSeekBar = view.findViewById(R.id.seek_bar);
    vStartTime = view.findViewById(R.id.start_time);
    vEndTime = view.findViewById(R.id.end_time);
  }

  private void bindView() {
    //开关
    vTimerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        toggle(isChecked);
      }
    });
    //进度条
    vSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //小于最小值，则直接设置为最小值
        if (progress < mMin) {
          seekBar.setProgress(mMin);
          return;
        }
        mCurrent = progress;
        float progressRatio = getProgressRatio();
        //移动进度条上的指示器
        moveIndicator(progressRatio, progress);
        for (Callback callback : mCallbacks) {
          callback.onProgressChange(progress, progressRatio);
        }
      }

      @Override public void onStartTrackingTouch(SeekBar seekBar) {
        //拖动进度条，如果没有开启，则开启
        if (!isOpen) {
          vTimerSwitch.setChecked(true);
        }
        float progressRatio = getProgressRatio();
        for (Callback callback : mCallbacks) {
          callback.onStartTrackingTouch(vSeekBar.getProgress(), progressRatio);
        }
      }

      @Override public void onStopTrackingTouch(SeekBar seekBar) {
        float progressRatio = getProgressRatio();
        for (Callback callback : mCallbacks) {
          callback.onStopTrackingTouch(vSeekBar.getProgress(), progressRatio);
        }
      }
    });
  }

  /**
   * 获取当前进度的百分比值
   */
  private float getProgressRatio() {
    return (float) vSeekBar.getProgress() / vSeekBar.getMax();
  }

  /**
   * 渲染
   */
  private void render() {
    vTitle.setText(mTitle);
    vSeekBar.setProgress(mCurrent);
    vSeekBar.setMax(mMax);
    vIndicator.setText(mIndicatorText);
    vStartTime.setText(mStartTimeText);
    vEndTime.setText(mEndTimeText);
  }

  /**
   * 移动当前时间的提示
   */
  private void moveIndicator(float progressRatio, int progress) {
    mIndicatorWidth = vIndicator.getWidth();
    mThumbWidth = vSeekBar.getThumb().getIntrinsicWidth();
    //计算公式：总宽度 * 进度百分比 -（指示器宽度 - 滑块宽度）/ 2 - 滑块宽度 * 进度百分比
    float indicatorOffset = vSeekBar.getWidth() * progressRatio
        - (mIndicatorWidth - mThumbWidth) / 2f
        - mThumbWidth * progressRatio;
    MarginLayoutParams layoutParams = (MarginLayoutParams) vIndicator.getLayoutParams();
    layoutParams.leftMargin = (int) indicatorOffset;
    vIndicator.setLayoutParams(layoutParams);
  }

  /**
   * 切换开启和关闭
   *
   * @param isOpen 是否开启
   */
  private void toggle(boolean isOpen) {
    if (isOpen) {
      toOpen();
    } else {
      toClose();
    }
    this.isOpen = isOpen;
    for (Callback callback : mCallbacks) {
      callback.onToggleChange(isOpen);
    }
  }

  /**
   * 切换到开启
   */
  private void toOpen() {
    Resources resources = getResources();
    //切换进度条的样式-蓝色
    vSeekBar.setProgressDrawable(resources.getDrawable(R.drawable.timer_close_seek_bar_bg_open));
    vSeekBar.setThumb(resources.getDrawable(R.drawable.timer_close_seek_bar_thumb_open));
    //该属性必须设置，因为上面2行设置，会导致ThumbOffset属性原本的设置失效，所以需要重新设置，否则Thumb会跑出去一半的大小
    vSeekBar.setThumbOffset(0);
  }

  /**
   * 切换到关闭
   */
  private void toClose() {
    Resources resources = getResources();
    //切换进度条的样式-灰色
    vSeekBar.setProgressDrawable(resources.getDrawable(R.drawable.timer_close_seek_bar_bg_close));
    vSeekBar.setThumb(resources.getDrawable(R.drawable.timer_close_seek_bar_thumb_close));
    vSeekBar.setThumbOffset(0);
  }

  public abstract static class Callback {
    /**
     * 是否打开
     *
     * @param isOpen 是否打开
     */
    void onToggleChange(boolean isOpen) {
    }

    /**
     * 触摸到进度条时，回调
     */
    void onStartTrackingTouch(int progress, float progressRatio) {
    }

    /**
     * 进度改变时回调
     *
     * @param progress 当前进度
     * @param progressRatio 进度百分比
     */
    void onProgressChange(int progress, float progressRatio) {
    }

    /**
     * 松手后回调
     */
    void onStopTrackingTouch(int progress, float progressRatio) {
    }
  }

  public void addCallback(Callback callback) {
    if (!mCallbacks.contains(callback)) {
      mCallbacks.add(callback);
    }
  }

  /**
   * 设置标题
   */
  public void setTitle(String title) {
    this.mTitle = title;
    render();
  }

  /**
   * 设置指示器的文本
   */
  public void setIndicatorText(String indicatorText) {
    this.mIndicatorText = indicatorText;
    render();
  }

  /**
   * 设置开始时间
   */
  public void setStartTimeText(String startTimeText) {
    this.mStartTimeText = startTimeText;
    render();
  }

  /**
   * 设置结束时间
   */
  public void setEndTimeText(String endTimeText) {
    this.mEndTimeText = endTimeText;
    render();
  }

  /**
   * 设置当前值
   */
  public void setCurrent(int current) {
    this.mCurrent = current;
    render();
  }

  public int getCurrent() {
    return vSeekBar.getProgress();
  }

  /**
   * 设置最小值
   */
  public void setMin(int min) {
    this.mMin = min;
    render();
  }

  /**
   * 设置最大值
   */
  public void setMax(int max) {
    this.mMax = max;
    render();
  }
}