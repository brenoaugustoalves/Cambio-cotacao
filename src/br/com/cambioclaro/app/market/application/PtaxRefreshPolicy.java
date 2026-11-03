package br.com.cambioclaro.app.market.application;

/** Determines whether an official PTAX lookup is due without depending on Android clocks or storage. */
public final class PtaxRefreshPolicy {
    private PtaxRefreshPolicy() { }

    public static boolean shouldRefresh(long now, long checkedAt, long interval) {
        if (interval <= 0) throw new IllegalArgumentException("Intervalo PTAX inválido");
        return checkedAt <= 0 || (now >= checkedAt && now - checkedAt >= interval);
    }
}
