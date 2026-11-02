package br.com.cambioclaro.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import br.com.cambioclaro.app.updates.ApkInstaller;

/** Receives the package installer result and forwards Android's explicit confirmation screen. */
public final class UpdateInstallReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){ApkInstaller.handleStatus(context,intent);}
}
