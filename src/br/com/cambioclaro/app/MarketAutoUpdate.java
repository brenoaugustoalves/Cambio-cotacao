package br.com.cambioclaro.app;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import br.com.cambioclaro.app.market.application.MarketRefreshPolicy;

/** Schedules one efficient all-currency refresh independently of app foreground use. */
public final class MarketAutoUpdate {
    public static final int JOB_ID = 3703;

    private MarketAutoUpdate() { }

    public static void schedule(Context context) {
        if (context == null) return;
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null || scheduler.getPendingJob(JOB_ID) != null) return;
        JobInfo.Builder job = new JobInfo.Builder(JOB_ID,
                new ComponentName(context, WidgetUpdateService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(MarketRefreshPolicy.BACKGROUND_MARKET_INTERVAL_MS)
                .setPersisted(true);
        if (Build.VERSION.SDK_INT >= 28) job.setEstimatedNetworkBytes(65_536, 1_024);
        scheduler.schedule(job.build());
    }
}
