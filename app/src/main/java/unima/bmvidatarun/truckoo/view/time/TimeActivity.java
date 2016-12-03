package unima.bmvidatarun.truckoo.view.time;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gigamole.infinitecycleviewpager.HorizontalInfiniteCycleViewPager;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import unima.bmvidatarun.R;
import unima.bmvidatarun.truckoo.adapter.OverviewPagerAdapter;
import unima.bmvidatarun.truckoo.model.DailyLog;
import unima.bmvidatarun.truckoo.model.WeeklyLog;
import unima.bmvidatarun.truckoo.persistence.LogStorage;
import unima.bmvidatarun.truckoo.view.warning.WarningActivity;

/**
 * Created by Marko on 02.12.16.
 */

public class TimeActivity extends AppCompatActivity {

    private Timer     mTimer1;
    private TimerTask mTt1;
    private Handler mTimerHandler = new Handler();

    private WeeklyLog weeklyLog;
    private DailyLog  currentDailyLog;
    private int       position;
    private Activity  activity;

    @BindView(R.id.toolbar) Toolbar  toolbar;
    @BindView(R.id.title)   TextView title;


    @BindView(R.id.hours)     TextView hoursCurrent;
    @BindView(R.id.hoursDay)  TextView hoursDay;
    @BindView(R.id.hoursWeek) TextView hoursWeek;

    @BindView(R.id.minutes)     TextView minutesCurrent;
    @BindView(R.id.minutesDay)  TextView minutesDay;
    @BindView(R.id.minutesWeek) TextView minutesWeek;

    @BindView(R.id.circle_progress_bar)      ProgressBar progressBarCurrent;
    @BindView(R.id.circle_progress_bar_day)  ProgressBar progressBarDay;
    @BindView(R.id.circle_progress_bar_week) ProgressBar progressBarWeek;

    @BindView(R.id.start_stop) Button  button;
    private                    boolean isRunning;

    @BindView(R.id.warningTriangle) ImageView warningTriangle;
    @BindView(R.id.divider)         TextView  divider;
    @BindView(R.id.totalTime)       TextView  totalTime;
    @BindView(R.id.record)          ImageView record;


    @Override

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        title.setText(R.string.rest_time_assistent);
        title.setTextColor(Color.WHITE);
        isRunning = false;
        weeklyLog = new WeeklyLog();
        position = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        currentDailyLog = weeklyLog.getDailyLogs().get(position);
        weeklyLog.getDailyLogs().remove(position);
        weeklyLog.fillOtherWeekDays();
        currentDailyLog.mockDrivenTime();
        this.activity = this;
        update();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.overview_menu, menu);//Menu Resource, Menu
        return super.onCreateOptionsMenu(menu);
    }

    @OnClick(R.id.start_stop)
    protected void startStopPressed(View view) {
        if (isRunning) {
            isRunning = false;
            stopTimer();
            button.setText(R.string.start_driving);
            button.setBackgroundColor(getColor(R.color.greenColor));
        } else {
            isRunning = true;

            startTimer();
            button.setText(R.string.stop_driving);
            button.setBackgroundColor(getColor(R.color.colorAccent));

        }

    }

    private void stopTimer() {
        record.clearAnimation();
        record.setVisibility(View.GONE);
        if (mTimer1 != null) {
            mTimer1.cancel();
            mTimer1.purge();
        }
    }

    private void startTimer() {
        Animation myFadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.blinking);
        record.setVisibility(View.VISIBLE);
        record.startAnimation(myFadeInAnimation);
        mTimer1 = new Timer();
        mTt1 = new TimerTask() {
            public void run() {
                mTimerHandler.post(new Runnable() {
                    public void run() {
                        currentDailyLog.addMinutes(15);
                        update();
                    }
                });
            }
        };

        mTimer1.schedule(mTt1, 1, 1000);
    }

    private void update() {


        if (currentDailyLog.getDrivenSinceLastPause() <= 270) {
            long currentHours = currentDailyLog.getDrivenSinceLastPause() / 60;
            long currentMinutes = currentDailyLog.getDrivenSinceLastPause() % 60;
            int percentage = (int) (((double) currentDailyLog.getDrivenSinceLastPause() / 270) * 100);

            if (percentage > 70) {
                progressBarCurrent.setProgressDrawable(getDrawable(R.drawable.progress_foreground_warning));
                warningTriangle.setVisibility(View.VISIBLE);
                divider.setTextColor(getColor(R.color.colorWarning));
                totalTime.setTextColor(getColor(R.color.colorWarning));

            }
            hoursCurrent.setText(getDoubleDigitValue(currentHours));
            minutesCurrent.setText(getDoubleDigitValue(currentMinutes));
            progressBarCurrent.setProgress(percentage);
        }
        if (currentDailyLog.getDrivenToday() <= 540) {
            long dayHours = currentDailyLog.getDrivenToday() / 60;
            long dayMinutes = currentDailyLog.getDrivenToday() % 60;
            int dayPercentage = (int) (((double) currentDailyLog.getDrivenToday() / 540d) * 100);

            hoursDay.setText(getDoubleDigitValue(dayHours));
            minutesDay.setText(getDoubleDigitValue(dayMinutes));
            progressBarDay.setProgress(dayPercentage);
        }
        long weekSum = currentDailyLog.getDrivenToday();
        for (DailyLog dailyLog : weeklyLog.getDailyLogs()) {
            weekSum += dailyLog.getDrivenToday();
        }
        if (weekSum <= 3240) {
            long weekHours = weekSum / 60;
            long weekMinutes = weekSum % 60;
            int weekPercentage = (int) (((double) weekSum / 3240d) * 100);

            hoursWeek.setText(getDoubleDigitValue(weekHours));
            minutesWeek.setText(getDoubleDigitValue(weekMinutes));
            progressBarWeek.setProgress(weekPercentage);
        }
    }

    private String getDoubleDigitValue(long total) {
        DecimalFormat twoPlaces = new DecimalFormat("00");
        return twoPlaces.format(total);
    }


}
