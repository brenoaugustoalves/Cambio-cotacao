package br.com.cambioclaro.app.updates;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import br.com.cambioclaro.app.Diagnostics;
import br.com.cambioclaro.app.UpdateInstallReceiver;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Set;
import java.util.TreeSet;

/** Verifies package identity and signing lineage, then delegates consent to Android's installer UI. */
public final class ApkInstaller {
    private ApkInstaller() { }

    public static void verify(Context context, File apk, GithubRelease release) throws Exception {
        PackageManager packages = context.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo candidate = packages.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
        if (candidate == null || !context.getPackageName().equals(candidate.packageName)) {
            throw new IOException("O arquivo não é um APK Câmbio Claro válido.");
        }
        long versionCode = versionCode(candidate);
        PackageInfo current = packages.getPackageInfo(context.getPackageName(), 0);
        if (versionCode <= versionCode(current) || !release.version.equals(candidate.versionName)) {
            throw new IOException("A versão do APK não é posterior à instalação atual ou não corresponde à Release.");
        }
        PackageInfo signedCurrent = packages.getPackageInfo(context.getPackageName(), flags);
        if (!signers(signedCurrent).equals(signers(candidate))) {
            throw new IOException("Assinatura diferente da instalação atual; atualização recusada.");
        }
    }

    public static void install(Context context, File apk) throws Exception {
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams parameters = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        parameters.setAppPackageName(context.getPackageName());
        if (Build.VERSION.SDK_INT >= 31) parameters.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED);
        int sessionId = installer.createSession(parameters);
        try (PackageInstaller.Session session = installer.openSession(sessionId);
             InputStream input = new FileInputStream(apk);
             OutputStream output = session.openWrite("base.apk", 0, apk.length())) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            session.fsync(output);
            Intent result = new Intent(context, UpdateInstallReceiver.class)
                    .setAction(context.getPackageName() + ".INSTALL_UPDATE");
            PendingIntent callback = PendingIntent.getBroadcast(context, 18009, result,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            session.commit(callback.getIntentSender());
        } catch (Exception failure) {
            try { installer.abandonSession(sessionId); } catch (Exception ignored) { }
            throw failure;
        }
    }

    public static void handleStatus(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirmation != null) {
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(confirmation);
            }
        } else if (status != PackageInstaller.STATUS_SUCCESS) {
            Diagnostics.record(context, "github_update_installer",
                    new IOException(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)));
        }
    }

    private static long versionCode(PackageInfo info) {
        return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
    }

    private static Set<String> signers(PackageInfo info) {
        Set<String> result = new TreeSet<>();
        Signature[] signatures = Build.VERSION.SDK_INT >= 28
                ? info.signingInfo.getApkContentsSigners() : info.signatures;
        if (signatures == null) return result;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Signature signature : signatures) result.add(hex(digest.digest(signature.toByteArray())));
            return result;
        } catch (Exception failure) {
            throw new IllegalStateException("Não foi possível validar o certificado do APK.", failure);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder();
        for (byte item : bytes) value.append(String.format(java.util.Locale.ROOT, "%02x", item & 255));
        return value.toString();
    }
}
