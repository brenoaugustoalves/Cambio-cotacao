import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.market.application.MarketAlertSink;
import br.com.cambioclaro.app.market.application.MarketHistoryWriter;
import br.com.cambioclaro.app.market.application.MarketQuoteRepository;
import br.com.cambioclaro.app.market.application.MarketQuoteSource;
import br.com.cambioclaro.app.market.application.RefreshMarketQuotes;
import br.com.cambioclaro.app.market.domain.MarketObservation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RefreshMarketQuotesTest {
    private static int checks;
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private static final class FakeGateway implements MarketQuoteSource, MarketQuoteRepository,
            MarketHistoryWriter, MarketAlertSink {
        final Map<FxCurrency, MarketObservation> observations = new EnumMap<>(FxCurrency.class);
        final List<String> events = new ArrayList<>();
        String savedResponse;
        int fetches, snapshots;
        boolean failFetch, failSave;

        @Override public String fetch() throws Exception {
            fetches++;
            if (failFetch) throw new java.io.IOException("provider unavailable");
            events.add("fetch");
            return "provider-batch";
        }
        @Override public void saveBatch(String rawResponse) throws Exception {
            if (failSave) throw new java.io.IOException("invalid provider response");
            savedResponse = rawResponse;
            events.add("save");
        }
        @Override public MarketObservation snapshot(FxCurrency currency) {
            snapshots++;
            return observations.get(currency);
        }
        @Override public void archive(FxCurrency currency, MarketObservation observation) {
            events.add("archive:" + currency.name());
        }
        @Override public void evaluate(FxCurrency currency, MarketObservation observation) {
            events.add("alert:" + currency.name());
        }
    }

    public static void main(String[] args) throws Exception {
        Instant time = Instant.parse("2026-10-30T13:00:00Z");
        FakeGateway gateway = new FakeGateway();
        gateway.observations.put(FxCurrency.USD, new MarketObservation(new BigDecimal("5.4000"), time));
        gateway.observations.put(FxCurrency.EUR, new MarketObservation(new BigDecimal("6.2000"), time));
        new RefreshMarketQuotes(new br.com.cambioclaro.app.market.application.StoreMarketBatch(gateway, gateway), gateway, gateway, gateway).execute();

        check(gateway.fetches == 1 && "provider-batch".equals(gateway.savedResponse),
                "One provider batch is fetched and persisted unchanged");
        check(gateway.snapshots == FxCurrency.values().length,
                "Effective stored observations are read for each supported currency");
        check(gateway.events.equals(java.util.Arrays.asList("fetch", "save", "archive:USD", "alert:USD",
                "archive:EUR", "alert:EUR")), "Archive and alert are invoked in deterministic per-currency order");

        FakeGateway empty = new FakeGateway();
        new RefreshMarketQuotes(new br.com.cambioclaro.app.market.application.StoreMarketBatch(empty, empty), empty, empty, empty).execute();
        check(empty.events.equals(java.util.Arrays.asList("fetch", "save")),
                "Missing observations are not archived and do not trigger alerts");

        FakeGateway failed = new FakeGateway();
        failed.failFetch = true;
        boolean failedAsExpected = false;
        try { new RefreshMarketQuotes(new br.com.cambioclaro.app.market.application.StoreMarketBatch(failed, failed), failed, failed, failed).execute(); }
        catch (java.io.IOException expected) { failedAsExpected = true; }
        check(failedAsExpected && failed.savedResponse == null && failed.snapshots == 0,
                "Source failure propagates before persistence or side effects");

        FakeGateway rejectedBatch = new FakeGateway();
        rejectedBatch.failSave = true;
        boolean rejectedAsExpected = false;
        try { new RefreshMarketQuotes(new br.com.cambioclaro.app.market.application.StoreMarketBatch(rejectedBatch, rejectedBatch), rejectedBatch, rejectedBatch, rejectedBatch).execute(); }
        catch (java.io.IOException expected) { rejectedAsExpected = true; }
        check(rejectedAsExpected && rejectedBatch.snapshots == 0 && rejectedBatch.events.equals(java.util.Collections.singletonList("fetch")),
                "Rejected provider batch does not trigger archive or alert side effects");

        boolean rejectedDependency = false;
        try { new RefreshMarketQuotes(null, gateway, gateway, gateway); }
        catch (IllegalArgumentException expected) { rejectedDependency = true; }
        check(rejectedDependency, "Missing required dependency is rejected at construction");

        boolean rejectedObservation = false;
        try { new MarketObservation(BigDecimal.ZERO, time); }
        catch (IllegalArgumentException expected) { rejectedObservation = true; }
        check(rejectedObservation, "Market observation rejects non-positive provider prices");

        System.out.println(checks + " market refresh use case checks passed");
    }
}
