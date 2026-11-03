package br.com.cambioclaro.app.market.application;

/** Refreshes configured stock sources independently and preserves partial success. */
public final class RefreshStockQuotes {
    private final StockQuoteGateway gateway;

    public RefreshStockQuotes(StockQuoteGateway gateway) {
        if (gateway == null) throw new IllegalArgumentException("Gateway de cotações obrigatório");
        this.gateway = gateway;
    }

    public StockRefreshResult execute() {
        boolean indexEnabled = gateway.indexEnabled();
        Exception stocksError = refresh(false);
        Exception indexError = indexEnabled ? refresh(true) : null;
        return new StockRefreshResult(indexEnabled, stocksError, indexError);
    }

    private Exception refresh(boolean index) {
        try {
            String response = index ? gateway.fetchIndex() : gateway.fetchStocks();
            gateway.save(response, index);
            return null;
        } catch (Exception failure) {
            return failure;
        }
    }
}
