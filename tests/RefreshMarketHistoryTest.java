import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.market.application.MarketHistoryRepository;
import br.com.cambioclaro.app.market.application.MarketHistorySource;
import br.com.cambioclaro.app.market.application.RefreshMarketHistory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class RefreshMarketHistoryTest {
    private static int checks;
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private static final class FakeGateway implements MarketHistorySource, MarketHistoryRepository {
        final List<String> events = new ArrayList<>();
        LocalDate dailyStart,dailyEnd;
        boolean failSource;
        @Override public String fetchIntraday(FxCurrency currency) throws Exception {
            events.add("fetch-intraday:" + currency.name());
            if (failSource) throw new java.io.IOException("source failed");
            return "intraday-response";
        }
        @Override public String fetchDaily(FxCurrency currency, LocalDate startDate, LocalDate endDate) throws Exception {
            dailyStart = startDate;dailyEnd=endDate;
            events.add("fetch-daily:" + currency.name());
            if (failSource) throw new java.io.IOException("source failed");
            return "daily-response";
        }
        @Override public void saveIntraday(FxCurrency currency, String raw) {
            events.add("save-intraday:" + currency.name() + ":" + raw);
        }
        @Override public void saveDaily(FxCurrency currency, LocalDate startDate, LocalDate endDate, String raw) {
            events.add("save-daily:" + currency.name() + ":" + startDate + ":" + endDate + ":" + raw);
        }
    }

    public static void main(String[] args) throws Exception {
        FakeGateway gateway = new FakeGateway();
        RefreshMarketHistory refresh = new RefreshMarketHistory(gateway, gateway);
        refresh.refreshIntraday(FxCurrency.USD);
        check(gateway.events.equals(java.util.Arrays.asList("fetch-intraday:USD", "save-intraday:USD:intraday-response")),
                "Intraday request persists the same provider response after fetching");

        gateway.events.clear();
        LocalDate fiveDaysStart=LocalDate.of(2026,10,29),rangeEnd=LocalDate.of(2026,11,3);
        refresh.refreshDaily(FxCurrency.EUR, fiveDaysStart, rangeEnd);
        check(gateway.dailyStart.equals(fiveDaysStart)&&gateway.dailyEnd.equals(rangeEnd)&&gateway.events.equals(java.util.Arrays.asList(
                "fetch-daily:EUR", "save-daily:EUR:2026-10-29:2026-11-03:daily-response")),
                "Five-day selection fetches and saves the exact calendar boundaries");
        LocalDate monthStart=LocalDate.of(2026,10,3);
        refresh.refreshDaily(FxCurrency.EUR, monthStart, rangeEnd);
        check(gateway.dailyStart.equals(monthStart)&&gateway.dailyEnd.equals(rangeEnd), "Monthly selection preserves month boundaries");
        LocalDate priorYear=LocalDate.of(2025,11,3);
        refresh.refreshDaily(FxCurrency.GBP, priorYear.minusYears(1), priorYear);
        check(gateway.dailyStart.equals(LocalDate.of(2024,11,3))&&gateway.dailyEnd.equals(priorYear), "Year comparison fetches the same prior-year date range");

        int eventsBeforeInvalid = gateway.events.size();
        boolean rejected = false;
        try { refresh.refreshDaily(FxCurrency.USD, LocalDate.of(2024,10,31), LocalDate.of(2026,11,3)); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected && gateway.events.size() == eventsBeforeInvalid, "Invalid period is rejected before network access");

        gateway.failSource = true;
        int beforeFailure = gateway.events.size();
        boolean propagated = false;
        try { refresh.refreshIntraday(FxCurrency.CAD); }
        catch (java.io.IOException expected) { propagated = true; }
        check(propagated && gateway.events.size() == beforeFailure + 1,
                "Source failure propagates without attempting to save history");

        boolean rejectedDependency = false;
        try { new RefreshMarketHistory(null, gateway); }
        catch (IllegalArgumentException expected) { rejectedDependency = true; }
        check(rejectedDependency, "Missing source dependency is rejected");

        System.out.println(checks + " market history refresh checks passed");
    }
}
