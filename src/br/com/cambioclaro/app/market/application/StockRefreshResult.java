package br.com.cambioclaro.app.market.application;

/** Independent outcomes so a failed source does not hide a successful source. */
public final class StockRefreshResult {
    public final boolean indexEnabled;
    public final Exception stocksError;
    public final Exception indexError;

    StockRefreshResult(boolean indexEnabled, Exception stocksError, Exception indexError) {
        this.indexEnabled = indexEnabled;
        this.stocksError = stocksError;
        this.indexError = indexError;
    }

    public boolean hasError() {
        return stocksError != null || indexError != null;
    }
}
