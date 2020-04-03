package fr.themsou.calendarwatchface;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class MyWatchFace extends CanvasWatchFaceService {

    // ADD connection :
    // cd C:\\Users\Clement\AppData\Local\Android\Sdk\platform-tools
    // adb tcpip 5555
    // adb devices
    // adb connect 192.168.192.5:5555

    //Updates rate in milliseconds for interactive mode
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }
    private class Engine extends CanvasWatchFaceService.Engine {

        /* Handler to update the time once a second in interactive mode. */
        @SuppressLint("HandlerLeak")
        private final Handler mUpdateTimeHandler = new Handler() {
            @Override public void handleMessage(Message message) {
                if(R.id.message_update == message.what){
                    invalidate();
                    if(shouldTimerBeRunning()){
                        long timeMs = System.currentTimeMillis();
                        long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                        mUpdateTimeHandler.sendEmptyMessageDelayed(R.id.message_update, delayMs);
                    }
                }
            }
        };

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        private boolean mRegisteredTimeZoneReceiver = false;

        private static final float STROKE_WIDTH = 3f;

        private Calendar mCalendar;

        private Paint mBackgroundPaint;
        private Paint mHandPaint;

        private boolean mAmbient;

        private float mHourHandLength;
        private float mMinuteHandLength;
        private float mSecondHandLength;

        private int mWidth;
        private int mHeight;
        private float mCenterX;
        private float mCenterY;
        private float mScale = 1;

        //////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////// CREATE - DESTROY //////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this).build());

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);

            mHandPaint = new Paint();
            mHandPaint.setColor(Color.WHITE);
            mHandPaint.setStrokeWidth(STROKE_WIDTH);
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mCalendar = Calendar.getInstance();

            Log.d("MyWatchFace", "----------------------------------------");
            Log.d("MyWatchFace", "      CALENDAR WATCH FACE CREATED");
            Log.d("MyWatchFace", "----------------------------------------");

        }
        @Override
        public void onDestroy(){
            mUpdateTimeHandler.removeMessages(R.id.message_update);
            super.onDestroy();
            Log.d("MyWatchFace", "----------------------------------------");
            Log.d("MyWatchFace", "     CALENDAR WATCH FACE DESTROYED");
            Log.d("MyWatchFace", "----------------------------------------");
        }

        //////////////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////// BASE ////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if(mAmbient != inAmbientMode){
                mAmbient = inAmbientMode;
                invalidate();

                if(inAmbientMode){
                    Log.d("MyWatchFace", "---------- AMBIENT MODE ----------");
                }else{
                    Log.d("MyWatchFace", "----------- FULL MODE ------------");
                }
            }

            /*
             * Whether the timer should be running depends on whether we're visible (as well as
             * whether we're in ambient mode), so we may need to start or stop the timer.
             */
            updateTimer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mWidth = width;
            mHeight = height;
            /*
             * Find the coordinates of the center point on the screen.
             * Ignore the window insets so that, on round watches
             * with a "chin", the watch face is centered on the entire screen,
             * not just the usable portion.
             */
            mCenterX = mWidth / 2f;
            mCenterY = mHeight / 2f;
            /*
             * Calculate the lengths of the watch hands and store them in member variables.
             */
            mHourHandLength = mCenterX - 80;
            mMinuteHandLength = mCenterX - 40;
            mSecondHandLength = mCenterX - 20;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            Log.d("MyWatchFace", "-> Draw...");

            // Draw the background.
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float secondsRotation = (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f) * 6f;
            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + mCalendar.get(Calendar.MINUTE) / 2f;

            // save the canvas state before we begin to rotate it
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(mCenterX, mCenterY, mCenterX, mCenterY - mHourHandLength, mHandPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(mCenterX, mCenterY, mCenterX, mCenterY - mMinuteHandLength, mHandPaint);

            if(!mAmbient){
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(mCenterX, mCenterY, mCenterX, mCenterY - mSecondHandLength, mHandPaint);
            }
            // restore the canvas' original orientation.
            canvas.restore();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if(visible){
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }else{
                unregisterReceiver();
            }

            /*
             * Whether the timer should be running depends on whether we're visible
             * (as well as whether we're in ambient mode),
             * so we may need to start or stop the timer.
             */
            updateTimer();
        }

        //////////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////// METHODS //////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(R.id.message_update);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(R.id.message_update);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}