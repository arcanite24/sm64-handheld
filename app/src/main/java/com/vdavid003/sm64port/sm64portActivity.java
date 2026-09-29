package com.vdavid003.sm64port;

import org.libsdl.app.SDLActivity;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

public class sm64portActivity extends SDLActivity
{
    @Override
    protected void onCreate(Bundle state) {
        // Request 60 Hz before SDL creates its EGL surface and probes vsync.
        if (Build.VERSION.SDK_INT >= 23) {
            Display.Mode current = getWindowManager().getDefaultDisplay().getMode();
            for (Display.Mode mode : getWindowManager().getDefaultDisplay().getSupportedModes()) {
                if (mode.getPhysicalWidth() == current.getPhysicalWidth() &&
                    mode.getPhysicalHeight() == current.getPhysicalHeight() &&
                    Math.abs(mode.getRefreshRate() - 60f) < 1f) {
                    WindowManager.LayoutParams params = getWindow().getAttributes();
                    params.preferredDisplayModeId = mode.getModeId();
                    getWindow().setAttributes(params);
                    break;
                }
            }
        }
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    protected String getMainFunction() {
        return "main";
    }
}
