import br.com.cambioclaro.app.market.application.MarketRefreshPolicy;

public final class MarketRefreshPolicyTest {
    private static int checks;

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    public static void main(String[] args) {
        check(MarketRefreshPolicy.foregroundInterval(false, false) == 60_000L,
                "Unmetered foreground refresh uses one minute");
        check(MarketRefreshPolicy.foregroundInterval(true, false) == 300_000L,
                "Metered foreground refresh uses five minutes");
        check(MarketRefreshPolicy.foregroundInterval(false, true) == 900_000L,
                "Battery saver reduces unmetered refresh frequency");
        check(MarketRefreshPolicy.foregroundInterval(true, true) == 900_000L,
                "Battery saver is never more frequent than the metered policy");
        check(MarketRefreshPolicy.historyInterval(false) == 300_000L,
                "Unmetered history refresh uses five minutes");
        check(MarketRefreshPolicy.historyInterval(true) == 900_000L,
                "Metered history refresh uses fifteen minutes");

        long dueAt = 50_000L + MarketRefreshPolicy.FULL_REFRESH_INTERVAL_MS;
        check(MarketRefreshPolicy.isFullRefreshDue(dueAt, 50_000L),
                "Full refresh becomes due at the interval boundary");
        check(!MarketRefreshPolicy.isFullRefreshDue(dueAt - 1L, 50_000L),
                "Full refresh is not due just before the interval boundary");
        check(!MarketRefreshPolicy.isFullRefreshDue(40_000L, 50_000L),
                "Clock rollback does not trigger a full refresh");
        check(MarketRefreshPolicy.isFullRefreshDue(MarketRefreshPolicy.FULL_REFRESH_INTERVAL_MS, 0L),
                "An uninitialized timestamp becomes due after one full interval");
        check(MarketRefreshPolicy.isWidgetMarketRefreshDue(10_000L, 0L),
                "Widget market refresh is due when no prior check exists");
        check(!MarketRefreshPolicy.isWidgetMarketRefreshDue(310_000L, 10_000L),
                "Widget keeps the existing strict five-minute minimum boundary");
        check(MarketRefreshPolicy.isWidgetMarketRefreshDue(310_001L, 10_000L),
                "Widget refresh becomes due after the five-minute minimum");

        System.out.println(checks + " market refresh policy checks passed");
    }
}
