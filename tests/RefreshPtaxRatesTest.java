import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.Rate;
import br.com.cambioclaro.app.market.application.PtaxAlertSink;
import br.com.cambioclaro.app.market.application.PtaxQuoteSource;
import br.com.cambioclaro.app.market.application.PtaxRateRepository;
import br.com.cambioclaro.app.market.application.RefreshPtaxRates;
import br.com.cambioclaro.app.market.application.StorePtaxRates;
import br.com.cambioclaro.app.market.application.StoredPtaxRates;
import br.com.cambioclaro.app.market.domain.PtaxBatch;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public final class RefreshPtaxRatesTest {
    private static int checks;
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private static Rate rate(int day, String sell) {
        return new Rate(new BigDecimal(sell).subtract(new BigDecimal("0.01")), new BigDecimal(sell),
                LocalDateTime.of(2026, 10, day, 13, 0));
    }

    private static final class FakeGateway implements PtaxQuoteSource, PtaxRateRepository, PtaxAlertSink {
        PtaxBatch response;
        Rate previous;
        boolean failSource, failSave;
        String events = "";
        long savedAt;
        @Override public PtaxBatch fetch(FxCurrency currency, LocalDate date) throws Exception {
            events += "fetch:" + currency.name() + ";";
            if (failSource) throw new java.io.IOException("source failed");
            return response;
        }
        @Override public Rate latest(FxCurrency currency) { return previous; }
        @Override public void save(FxCurrency currency, PtaxBatch batch, long fetchedAt) throws Exception {
            events += "save:" + currency.name() + ";";
            if (failSave) throw new java.io.IOException("save failed");
            savedAt = fetchedAt;
        }
        @Override public void evaluate(FxCurrency currency, PtaxBatch batch) { events += "alert:" + currency.name() + ";"; }
    }

    private static PtaxBatch batch(Rate quote) {
        return new PtaxBatch("official-json", Collections.singletonList(quote));
    }

    public static void main(String[] args) throws Exception {
        FakeGateway gateway = new FakeGateway();
        gateway.response = batch(rate(30, "5.40"));
        StorePtaxRates store = new StorePtaxRates(gateway, gateway, () -> 123456L);
        StoredPtaxRates saved = store.execute(FxCurrency.USD, LocalDate.of(2026, 10, 30));
        check(saved.rates.size() == 1 && saved.fetchedAt == 123456L && gateway.savedAt == 123456L,
                "Successful official batch returns its observations and records fetch time");
        check(gateway.events.equals("fetch:USD;save:USD;"), "Official data is fetched before persistence");

        gateway.events = "";
        gateway.previous = rate(30, "5.39");
        gateway.response = batch(rate(29, "5.38"));
        boolean rejectedRegression = false;
        try { store.execute(FxCurrency.USD, LocalDate.of(2026, 10, 30)); }
        catch (java.io.IOException expected) { rejectedRegression = true; }
        check(rejectedRegression && gateway.events.equals("fetch:USD;"),
                "Older source response is rejected before save");

        gateway.events = "";
        gateway.previous = null;
        gateway.response = batch(rate(30, "5.40"));
        RefreshPtaxRates refresh = new RefreshPtaxRates(store, gateway);
        refresh.execute(FxCurrency.EUR, LocalDate.of(2026, 10, 30));
        check(gateway.events.equals("fetch:EUR;save:EUR;alert:EUR;"),
                "Alert evaluation runs only after official rates are saved");

        gateway.events = "";
        gateway.failSource = true;
        boolean sourceFailed = false;
        try { refresh.execute(FxCurrency.GBP, LocalDate.of(2026, 10, 30)); }
        catch (java.io.IOException expected) { sourceFailed = true; }
        check(sourceFailed && gateway.events.equals("fetch:GBP;"),
                "Source failure does not save or evaluate alerts");

        gateway.failSource = false;
        gateway.failSave = true;
        gateway.events = "";
        boolean saveFailed = false;
        try { refresh.execute(FxCurrency.GBP, LocalDate.of(2026, 10, 30)); }
        catch (java.io.IOException expected) { saveFailed = true; }
        check(saveFailed && gateway.events.equals("fetch:GBP;save:GBP;"),
                "Failed save prevents alert evaluation");

        boolean emptyRejected = false;
        try { new PtaxBatch("{}", Collections.emptyList()); }
        catch (IllegalArgumentException expected) { emptyRejected = true; }
        check(emptyRejected, "Empty PTAX batch is rejected");

        boolean nullCurrencyRejected = false;
        try { store.execute(null, LocalDate.of(2026, 10, 30)); }
        catch (IllegalArgumentException expected) { nullCurrencyRejected = true; }
        check(nullCurrencyRejected, "Missing currency is rejected before source access");

        System.out.println(checks + " PTAX refresh use case checks passed");
    }
}
