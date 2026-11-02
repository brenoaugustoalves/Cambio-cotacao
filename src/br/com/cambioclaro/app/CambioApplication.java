package br.com.cambioclaro.app;
import android.app.Application;
public final class CambioApplication extends Application {
    @Override public void onCreate(){super.onCreate();Stocks.clearLegacyToken(this);Diagnostics.install(this);Preferences.initializeAppearance(this);MarketArchive.prepare(this);GithubUpdates.schedule(this);GithubUpdates.checkIfDueAsync(this);}
}
