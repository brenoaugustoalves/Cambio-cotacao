package br.com.cambioclaro.app;

import android.content.Context;
import br.com.cambioclaro.app.market.application.PtaxAlertSink;
import br.com.cambioclaro.app.market.application.PtaxQuoteSource;
import br.com.cambioclaro.app.market.application.PtaxRateRepository;
import br.com.cambioclaro.app.market.application.RefreshPtaxRates;
import br.com.cambioclaro.app.market.application.StorePtaxRates;
import br.com.cambioclaro.app.market.domain.PtaxBatch;
import java.time.LocalDate;
import java.util.List;

/** Android adapter for the BCB PTAX service, local store, and user alert settings. */
public final class AndroidPtaxGateway implements PtaxQuoteSource, PtaxRateRepository, PtaxAlertSink {
    private final Context context;

    public AndroidPtaxGateway(Context context) {
        if (context == null) throw new IllegalArgumentException("Contexto obrigatório");
        this.context = context.getApplicationContext();
    }

    public static StorePtaxRates createStore(Context context) {
        AndroidPtaxGateway gateway = new AndroidPtaxGateway(context);
        return new StorePtaxRates(gateway, gateway);
    }

    public static RefreshPtaxRates create(Context context) {
        AndroidPtaxGateway gateway = new AndroidPtaxGateway(context);
        StorePtaxRates store = new StorePtaxRates(gateway, gateway);
        return new RefreshPtaxRates(store, gateway);
    }

    @Override public PtaxBatch fetch(FxCurrency currency, LocalDate date) throws Exception {
        String raw = BcbClient.fetch(date, currency);
        List<Rate> parsed = BcbClient.parse(raw, date, currency);
        return new PtaxBatch(raw, parsed);
    }

    @Override public Rate latest(FxCurrency currency) {
        List<Rate> cached = RateStore.read(context, currency);
        return cached.isEmpty() ? null : cached.get(cached.size() - 1);
    }

    @Override public void save(FxCurrency currency, PtaxBatch batch, long fetchedAt) throws Exception {
        RateStore.save(context, currency, batch.rawResponse, fetchedAt);
    }

    @Override public void evaluate(FxCurrency currency, PtaxBatch batch) {
        Alerts.evaluate(context, currency, batch.rates);
    }
}
