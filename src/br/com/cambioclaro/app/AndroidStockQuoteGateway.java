package br.com.cambioclaro.app;

import android.content.Context;
import br.com.cambioclaro.app.market.application.StockQuoteGateway;

/** Android adapter for the existing public stock source and optional HTTPS index proxy. */
public final class AndroidStockQuoteGateway implements StockQuoteGateway {
    private final Context context;

    public AndroidStockQuoteGateway(Context context) {
        if (context == null) throw new IllegalArgumentException("Contexto obrigatório");
        this.context = context.getApplicationContext();
    }

    @Override public boolean indexEnabled() {
        return !Stocks.backend(context).isEmpty();
    }

    @Override public String fetchStocks() throws Exception {
        return Stocks.fetchDemo();
    }

    @Override public String fetchIndex() throws Exception {
        return Stocks.fetchIndex(context);
    }

    @Override public void save(String response, boolean index) throws Exception {
        Stocks.save(context, response, index);
    }
}
