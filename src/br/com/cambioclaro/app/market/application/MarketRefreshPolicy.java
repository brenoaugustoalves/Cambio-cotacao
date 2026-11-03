package br.com.cambioclaro.app.market.application;

/** Pure scheduling rules for foreground market refreshes. */
public final class MarketRefreshPolicy {
    public static final long FULL_REFRESH_INTERVAL_MS = 12L * 60L * 60L * 1000L;
    private static final long UNMETERED_FOREGROUND_MS = 60L * 1000L;
    private static final long METERED_FOREGROUND_MS = 5L * 60L * 1000L;
    private static final long POWER_SAVE_MINIMUM_MS = 15L * 60L * 1000L;
    private static final long UNMETERED_HISTORY_MS = 5L * 60L * 1000L;
    private static final long METERED_HISTORY_MS = 15L * 60L * 1000L;

    private MarketRefreshPolicy() { }

    public static long foregroundInterval(boolean meteredNetwork, boolean powerSave) {
        long normalInterval = meteredNetwork ? METERED_FOREGROUND_MS : UNMETERED_FOREGROUND_MS;
        return powerSave ? Math.max(normalInterval, POWER_SAVE_MINIMUM_MS) : normalInterval;
    }

    public static long historyInterval(boolean meteredNetwork) {
        return meteredNetwork ? METERED_HISTORY_MS : UNMETERED_HISTORY_MS;
    }

    public static boolean isFullRefreshDue(long nowMs, long lastRefreshMs) {
        return nowMs >= lastRefreshMs && nowMs - lastRefreshMs >= FULL_REFRESH_INTERVAL_MS;
    }
}
