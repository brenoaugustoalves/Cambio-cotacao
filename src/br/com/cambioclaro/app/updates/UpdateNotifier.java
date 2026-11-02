package br.com.cambioclaro.app.updates;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import br.com.cambioclaro.app.R;
import br.com.cambioclaro.app.SettingsActivity;

/** Creates a local notification when a stable update is available. */
public final class UpdateNotifier {
    private static final String CHANNEL = "github_updates";
    private UpdateNotifier() { }

    public static void available(Context context, GithubRelease release) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                    "Atualizações do aplicativo", NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(context, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent action = PendingIntent.getActivity(context, 18010, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Câmbio Claro " + release.version + " disponível")
                .setContentText("Abra Preferências para baixar e instalar.")
                .setContentIntent(action)
                .setAutoCancel(true)
                .build();
        manager.notify(18008, notification);
    }
}
