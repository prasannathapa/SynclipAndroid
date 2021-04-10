package prasanna.snowfox.synclip;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_PHONE;

public class FloatingService  extends Service {

    public static final String FLOATING_WINDOW_X = "floating_window_x";
    public static final String FLOATING_WINDOW_Y = "floating_window_y";

    private final static int DURATION_TIME = 400;

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private Handler handler;

    private boolean openedSetting = false;
    private boolean longClickAble = true;
    private int onStartCommandReturn;

    private boolean checkPermission() {
        return true;
        /*return (
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean(ActivitySetting.PREF_FLOATING_BUTTON, false) &&
                        PreferenceManager.getDefaultSharedPreferences(this).getBoolean(ActivitySetting.PREF_START_SERVICE, true)
        );*/
    }

    private void FWHideAnimate() {

        Log.i("MY FLOATING SERVICE", "FWHideAnimate");
        floatingView.animate().scaleX(0).scaleY(0).setDuration(DURATION_TIME);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                windowManager.removeView(floatingView);
            }
        }, DURATION_TIME);
    }
    public static int dip2px(Context context, float dipValue){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }
    private void FWShowAnimate() {
        Log.i("MY FLOATING SERVICE", "FWShowAnimate");
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ? TYPE_APPLICATION_OVERLAY : TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.x = 300;
        params.y = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            params.width = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material) + dip2px(this, 4);
            params.height = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material) + dip2px(this, 8);
        } else {
            params.width = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);
            params.height = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);
            params.alpha = (float) 0.9;
        }
        windowManager.addView(floatingView, params);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                floatingView.animate().scaleX(1).scaleY(1).setDuration(DURATION_TIME);
            }
        }, DURATION_TIME);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return onStartCommandReturn;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        if (checkPermission()) {
            onStartCommandReturn = START_STICKY;
        } else {
            onStartCommandReturn = START_NOT_STICKY;
            Log.i("MY FLOATING SERVICE", "STOPPED");
            stopSelf();
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler = new Handler();
        LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        floatingView =
                layoutInflater.inflate(R.layout.layoutfloating_widget, null);
        floatingView.setScaleX(0);
        floatingView.setScaleY(0);

        FWShowAnimate();

        try {
            floatingView.setOnLongClickListener(v -> {
                if (!longClickAble) return false;
                floatingView.playSoundEffect(0);
                //MyUtil.vibrator(FloatingWindowService.this);
                openedSetting = true;
                return true;
            });
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                private int diffX;
                private int diffY;
                private int smallLength;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:

                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            longClickAble = true;
                            break;
                        case MotionEvent.ACTION_UP:
                            diffX = Math.abs(params.x - initialX);
                            diffY = Math.abs(params.y - initialY);
                            if (diffX < smallLength && diffY < smallLength) {
                                if (!openedSetting) {
                                    floatingView.playSoundEffect(0);
                                    //MyUtil.vibrator(FloatingWindowService.this);
                                }
                                openedSetting = false;
                            } else {
                            }
                            break;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingView, params);
                            diffX = Math.abs(params.x - initialX);
                            diffY = Math.abs(params.y - initialY);
                            if (diffX > smallLength || diffY > smallLength) {
                                longClickAble = false;
                            }
                            break;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e("MY FLOATING SERVICE", e.toString());
        }

    }

    @Override
    public void onDestroy() {
        Log.i("MY FLOATING SERVICE", "onDestroy");
        FWHideAnimate();
        super.onDestroy();
    }

}
