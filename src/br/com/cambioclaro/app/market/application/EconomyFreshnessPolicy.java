package br.com.cambioclaro.app.market.application;

/** Pure time-window policy for cached macro data and publications. */
public final class EconomyFreshnessPolicy {
    public static final long INDICATORS_TTL_MS = 6L * 60 * 60 * 1000;
    public static final long OFFICIAL_NEWS_TTL_MS = 3L * 60 * 60 * 1000;
    public static final long INTERNATIONAL_NEWS_TTL_MS = 3L * 60 * 60 * 1000;
    private EconomyFreshnessPolicy() { }

    public static boolean isFresh(long checkedAt, long now, long ttl) {
        return checkedAt > 0 && now >= checkedAt && ttl > 0 && now - checkedAt < ttl;
    }

    public static boolean shouldRefresh(boolean force, long checkedAt, long now, long ttl) {
        return force || !isFresh(checkedAt, now, ttl);
    }
}
