package dev.clean.tvhome;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
/** Event-driven Home redirect. Does not inspect screen contents or filter remote keys. */
public class HomeRedirectService extends AccessibilityService {
    private long lastLaunch;
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)return;
        String pkg=String.valueOf(event.getPackageName()), cls=String.valueOf(event.getClassName());
        if(!pkg.equals("com.amazon.tv.launcher") || !(cls.equals("com.amazon.tv.launcher.ui.HomeActivity_vNext")||cls.equals("com.amazon.tv.launcher.ui.HomeActivity")))return;
        long now=SystemClock.elapsedRealtime();if(now-lastLaunch<700)return;lastLaunch=now;
        startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }
    @Override public void onInterrupt(){}
}
