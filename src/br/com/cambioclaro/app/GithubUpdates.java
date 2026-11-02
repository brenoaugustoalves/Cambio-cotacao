package br.com.cambioclaro.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import br.com.cambioclaro.app.updates.ApkInstaller;
import br.com.cambioclaro.app.updates.GithubRelease;
import br.com.cambioclaro.app.updates.GithubReleaseClient;
import br.com.cambioclaro.app.updates.UpdateNotifier;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Coordinates background checks and user-initiated installation of public GitHub releases. */
public final class GithubUpdates {
    private static final int JOB_ID = 18008;
    private static final long CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final GithubReleaseClient releases = new GithubReleaseClient();
    private static final java.util.concurrent.atomic.AtomicBoolean scheduledCheckRunning = new java.util.concurrent.atomic.AtomicBoolean();
    private GithubUpdates() { }

    public interface Callback { void done(String message, boolean updateAvailable); }

    public static void schedule(Context context) {
        android.app.job.JobScheduler scheduler = context.getSystemService(android.app.job.JobScheduler.class);
        if (scheduler.getPendingJob(JOB_ID) != null) return;
        android.app.job.JobInfo job = new android.app.job.JobInfo.Builder(JOB_ID,
                new android.content.ComponentName(context, UpdateCheckJob.class))
                .setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(CHECK_INTERVAL_MS)
                .build();
        scheduler.schedule(job);
    }

    public static void checkIfDueAsync(Context context) {
        Context app = context.getApplicationContext();
        IO.execute(() -> checkIfDue(app));
    }

    static void checkIfDue(Context context) {
        if (!scheduledCheckRunning.compareAndSet(false, true)) return;
        Context app = context.getApplicationContext();
        try {
            long last = app.getSharedPreferences("github_updates_v1", Context.MODE_PRIVATE)
                    .getLong("checked_at", 0);
            if (System.currentTimeMillis() - last < CHECK_INTERVAL_MS) return;
            GithubRelease release = releases.fetchLatest();
            recordCheck(app);
            if (isNewer(app, release)) UpdateNotifier.available(app, release);
        } catch (Exception failure) {
            Diagnostics.record(app, "github_update_check", failure);
        } finally { scheduledCheckRunning.set(false); }
    }

    public static void checkAsync(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            try {
                GithubRelease release = releases.fetchLatest();
                recordCheck(app);
                boolean available = isNewer(app, release);
                if (available) UpdateNotifier.available(app, release);
                callback.done(available ? "Atualização " + release.version + " disponível no GitHub."
                        : "O app já está na versão mais recente.", available);
            } catch (Exception failure) {
                Diagnostics.record(app, "github_update_check", failure);
                callback.done(failure.getMessage() == null ? "Falha ao consultar o GitHub." : failure.getMessage(), false);
            }
        });
    }

    public static void downloadAndInstall(Activity activity, Callback callback) {
        Context app = activity.getApplicationContext();
        IO.execute(() -> {
            try {
                GithubRelease release = releases.fetchLatest();
                if (!isNewer(app, release)) {
                    callback.done("O app já está na versão mais recente.", false);
                    return;
                }
                File apk = releases.download(app.getCacheDir(), release);
                ApkInstaller.verify(app, apk, release);
                ApkInstaller.install(app, apk);
                callback.done("Arquivo validado. O Android exibirá a confirmação de instalação.", true);
            } catch (Exception failure) {
                Diagnostics.record(app, "github_update_install", failure);
                callback.done(failure.getMessage() == null ? "Falha ao preparar a instalação." : failure.getMessage(), false);
            }
        });
    }

    private static boolean isNewer(Context context, GithubRelease release) {
        String installed = "";
        try { installed = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName; }
        catch (PackageManager.NameNotFoundException ignored) { }
        return release.isNewerThan(installed);
    }

    private static void recordCheck(Context context) {
        context.getSharedPreferences("github_updates_v1", Context.MODE_PRIVATE)
                .edit().putLong("checked_at", System.currentTimeMillis()).apply();
    }
}
