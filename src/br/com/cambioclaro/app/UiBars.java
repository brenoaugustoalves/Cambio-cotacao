package br.com.cambioclaro.app;
import android.app.Activity;
import android.os.Build;
import android.view.*;
/** Apply appearance only after the decor is attached; some windows have no controller yet. */
public final class UiBars {
    public static void apply(Activity activity,boolean light){
        if(Build.VERSION.SDK_INT<30)return;
        View decor=activity.getWindow().getDecorView();
        decor.post(()->{if(activity.isDestroyed()||activity.isFinishing())return;applyController(decor.getWindowInsetsController(),light);});
    }
    public static void applyController(WindowInsetsController controller,boolean light){
        if(controller==null)return;
        int mask=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        controller.setSystemBarsAppearance(light?mask:0,mask);
    }
}
