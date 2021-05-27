package com.zh.android.timerclosepanel;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getSimpleName();
  private Disposable mDisposable;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    TimerClosePanel timerClosePanel = findViewById(R.id.timer_close_panel);
    //设置一个松手后的回调，主要是为了，延迟一段时间后，就开始设置值
    mDisposable = Observable.create(new ObservableOnSubscribe<Integer>() {
      @Override public void subscribe(@NonNull ObservableEmitter<Integer> emitter)
          throws Throwable {
        timerClosePanel.addCallback(new TimerClosePanel.Callback() {
          @Override void onStopTrackingTouch(int progress, float progressRatio) {
            super.onStopTrackingTouch(progress, progressRatio);
            emitter.onNext(progress);
          }
        });
      }
    })//防抖，一定时间内取最后一次
        .throttleLast(1, TimeUnit.SECONDS)
        .subscribeOn(AndroidSchedulers.mainThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<Integer>() {
          @Override public void accept(Integer progress) throws Throwable {
            //开启倒计时
            // TODO: 2021/5/27 倒计时
            //震动一下
            //时间转换
            String timeString = getTimeString(progress * 60 * 1000);
            //提示用户
            Toast.makeText(getApplicationContext(), timeString + "后停止播放", Toast.LENGTH_SHORT)
                .show();
          }
        });
    //设置第二个监听，主要时实时更新提示文本（不延时）
    timerClosePanel.addCallback(new TimerClosePanel.Callback() {
      @Override void onToggleChange(boolean isOpen) {
        super.onToggleChange(isOpen);
        String msg;
        //如果开启，则拿取当前值进行提示，关闭则提示关闭
        if (isOpen) {
          int progress = timerClosePanel.getCurrent();
          //时间转换
          String timeString = getTimeString(progress * 60 * 1000);
          msg = timeString + "后停止播放";
        } else {
          msg = "已停止定时关闭";
        }
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT)
            .show();
      }

      @Override public void onProgressChange(int progress, float progressRatio) {
        Log.d(TAG, "progress = " + progress);
        //时间转换
        String timeString = getTimeString(progress * 60 * 1000);
        //设置指示器的文本
        timerClosePanel.setIndicatorText(
            timeString
        );
      }
    });
    timerClosePanel.setTitle("定时停止播放");
    //设置最小、最大时间文本
    timerClosePanel.setStartTimeText("1分钟");
    timerClosePanel.setEndTimeText("8小时");
    //默认30分钟
    timerClosePanel.setCurrent(30);
    //最小1分钟
    timerClosePanel.setMin(1);
    //最大8小时
    timerClosePanel.setMax(8 * 60);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    if (mDisposable != null && mDisposable.isDisposed()) {
      mDisposable.dispose();
    }
  }

  /**
   * 时间转换
   *
   * @param millis 毫秒值
   */
  public static String getTimeString(long millis) {
    StringBuilder builder = new StringBuilder();
    int hours = (int) (millis / (1000 * 60 * 60));
    int minutes = (int) ((millis % (1000 * 60 * 60)) / (1000 * 60));
    if (hours > 0) {
      builder.append(String.format("%2d", hours))
          .append("小时");
      //有分钟时，才显示分钟
      if (minutes > 0) {
        builder.append(String.format("%2d", minutes))
            .append("分钟");
      }
    } else {
      builder.append(String.format("%2d", minutes))
          .append("分钟");
    }
    return builder.toString();
  }
}