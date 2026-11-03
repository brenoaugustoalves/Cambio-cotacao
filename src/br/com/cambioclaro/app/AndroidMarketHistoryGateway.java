package br.com.cambioclaro.app;

import android.content.Context;
import br.com.cambioclaro.app.market.application.MarketHistoryRepository;
import br.com.cambioclaro.app.market.application.MarketHistorySource;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Android adapter for AwesomeAPI history and its separate local archives. */
public final class AndroidMarketHistoryGateway implements MarketHistorySource, MarketHistoryRepository {
    private final Context context;

    public AndroidMarketHistoryGateway(Context context) {
        if (context == null) throw new IllegalArgumentException("Contexto obrigatório");
        this.context = context.getApplicationContext();
    }

    public static br.com.cambioclaro.app.market.application.RefreshMarketHistory create(Context context) {
        AndroidMarketHistoryGateway gateway = new AndroidMarketHistoryGateway(context);
        return new br.com.cambioclaro.app.market.application.RefreshMarketHistory(gateway, gateway);
    }

    @Override public String fetchIntraday(FxCurrency currency) throws Exception {
        return MarketHistory.fetch(currency);
    }

    @Override public String fetchDaily(FxCurrency currency, LocalDate startDate, LocalDate endDate) throws Exception {
        return MarketHistory.fetchDaily(currency, startDate, endDate);
    }

    @Override public void saveIntraday(FxCurrency currency, String rawResponse) throws Exception {
        MarketArchive.mergeResponse(context, currency, rawResponse);
        MarketHistory.save(context, currency, rawResponse);
    }

    @Override public void saveDaily(FxCurrency currency, LocalDate startDate, LocalDate endDate, String rawResponse) throws Exception {
        List<MarketHistory.Point> points = MarketHistory.parseDaily(rawResponse, currency, Instant.now());
        if (points.isEmpty()) throw new IOException("A fonte não publicou fechamentos para o período.");
        MarketArchive.mergeDaily(context, currency, points);
        String key = currency.name() + ":" + startDate + ":" + endDate;
        context.getSharedPreferences("market_daily_v1", Context.MODE_PRIVATE).edit()
                .putLong(key, System.currentTimeMillis()).apply();
    }
}
