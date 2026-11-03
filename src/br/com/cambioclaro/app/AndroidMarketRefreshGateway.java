package br.com.cambioclaro.app;

import android.content.Context;
import br.com.cambioclaro.app.market.application.MarketAlertSink;
import br.com.cambioclaro.app.market.application.MarketHistoryWriter;
import br.com.cambioclaro.app.market.application.MarketQuoteRepository;
import br.com.cambioclaro.app.market.application.MarketQuoteSource;
import br.com.cambioclaro.app.market.domain.MarketObservation;
import java.util.Collections;

/** Android adapter for the market refresh use case and its local integrations. */
public final class AndroidMarketRefreshGateway implements MarketQuoteSource,
        MarketQuoteRepository, MarketHistoryWriter, MarketAlertSink {
    private final Context context;

    public AndroidMarketRefreshGateway(Context context) {
        if (context == null) throw new IllegalArgumentException("Contexto obrigatório");
        this.context = context.getApplicationContext();
    }

    @Override public String fetch() throws Exception {
        return MarketClient.fetch();
    }

    @Override public void saveBatch(String rawResponse) throws Exception {
        MarketStore.save(context, rawResponse);
    }

    @Override public MarketObservation snapshot(FxCurrency currency) {
        MarketClient.Quote quote = MarketStore.snapshot(context, currency);
        return quote == null ? null : new MarketObservation(quote.ask, quote.at);
    }

    @Override public void archive(FxCurrency currency, MarketObservation observation) {
        MarketHistory.Point point = new MarketHistory.Point(observation.publishedAt, observation.ask);
        MarketArchive.merge(context, currency, Collections.singletonList(point));
    }

    @Override public void evaluate(FxCurrency currency, MarketObservation observation) {
        MarketAlerts.evaluate(context, currency, new MarketClient.Quote(observation.ask, observation.publishedAt));
    }
}
