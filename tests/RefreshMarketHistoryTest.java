import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.market.application.MarketHistoryRepository;
import br.com.cambioclaro.app.market.application.MarketHistorySource;
import br.com.cambioclaro.app.market.application.RefreshMarketHistory;
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
        int dailyDays;
        boolean failSource;
        @Override public String fetchIntraday(FxCurrency currency) throws Exception {
            events.add("fetch-intraday:" + currency.name());
            if (failSource) throw new java.io.IOException("source failed");
            return "intraday-response";
        }
        @Override public String fetchDaily(FxCurrency currency, int days) throws Exception {
            dailyDays = days;
            events.add("fetch-daily:" + currency.name());
            if (failSource) throw new java.io.IOException("source failed");
            return "daily-response";
        }
        @Override public void saveIntraday(FxCurrency currency, String raw) {
            events.add("save-intraday:" + currency.name() + ":" + raw);
        }
        @Override public void saveDaily(FxCurrency currency, int observations, String raw) {
            events.add("save-daily:" + currency.name() + ":" + observations + ":" + raw);
        }
    }

    public static void main(String[] args) throws Exception {
        FakeGateway gateway = new FakeGateway();
        RefreshMarketHistory refresh = new RefreshMarketHistory(gateway, gateway);
        refresh.refreshIntraday(FxCurrency.USD);
        check(gateway.events.equals(java.util.Arrays.asList("fetch-intraday:USD", "save-intraday:USD:intraday-response")),
                "Intraday request persists the same provider response after fetching");

        gateway.events.clear();
        refresh.refreshDaily(FxCurrency.EUR, 5);
        check(gateway.dailyDays == 5 && gateway.events.equals(java.util.Arrays.asList(
                "fetch-daily:EUR", "save-daily:EUR:5:daily-response")),
                "Five calendar days request the matching date range and persist separately");
        refresh.refreshDaily(FxCurrency.EUR, 30);
        check(gateway.dailyDays == 30, "One-month selection requests thirty calendar days");
        refresh.refreshDaily(FxCurrency.GBP, 365);
        check(gateway.dailyDays == 365, "One-year selection requests its exact 365-day period");

        int eventsBeforeInvalid = gateway.events.size();
        boolean rejected = false;
        try { refresh.refreshDaily(FxCurrency.USD, 366); }
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
