package br.com.cambioclaro.app.conversion.data;

import android.content.Context;
import br.com.cambioclaro.app.BcbClient;
import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.MarketClient;
import br.com.cambioclaro.app.MarketStore;
import br.com.cambioclaro.app.Rate;
import br.com.cambioclaro.app.RateStore;
import br.com.cambioclaro.app.Tourism;
import br.com.cambioclaro.app.conversion.application.ConversionQuoteSource;
import br.com.cambioclaro.app.conversion.domain.ConversionQuote;
import java.util.List;

/** Android persistence adapter; it never fetches data or invents a fallback value. */
public final class AndroidConversionQuoteSource implements ConversionQuoteSource {
    private final Context context;

    public AndroidConversionQuoteSource(Context context) {
        if (context == null) throw new IllegalArgumentException("Context obrigatório");
        this.context = context.getApplicationContext();
    }

    @Override public ConversionQuote market(String currencyCode) {
        FxCurrency currency = currency(currencyCode);
        if (currency == null) return null;
        MarketClient.Quote quote = MarketStore.latest(context, currency);
        return quote == null ? null : new ConversionQuote(quote.ask, quote.at, ConversionQuote.Source.MARKET);
    }

    @Override public ConversionQuote tourism() {
        MarketClient.Quote quote = Tourism.latest(context);
        return quote == null ? null : new ConversionQuote(quote.ask, quote.at, ConversionQuote.Source.TOURISM);
    }

    @Override public ConversionQuote ptax(String currencyCode) {
        FxCurrency currency = currency(currencyCode);
        if (currency == null) return null;
        List<Rate> rates = RateStore.read(context, currency);
        if (rates.isEmpty()) return null;
        Rate latest = rates.get(rates.size() - 1);
        return new ConversionQuote(latest.sell, latest.at.atZone(BcbClient.ZONE).toInstant(), ConversionQuote.Source.PTAX);
    }

    private FxCurrency currency(String currencyCode) {
        try { return FxCurrency.valueOf(currencyCode); }
        catch (IllegalArgumentException | NullPointerException invalid) { return null; }
    }
}
