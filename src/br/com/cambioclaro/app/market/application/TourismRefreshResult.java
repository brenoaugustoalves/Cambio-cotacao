package br.com.cambioclaro.app.market.application;

/** Independent outcome of the tourism latest quote and history updates. */
public final class TourismRefreshResult {
    public final boolean latestAttempted;
    public final boolean historyAttempted;
    public final Exception latestError;
    public final Exception historyError;

    TourismRefreshResult(boolean latestAttempted, boolean historyAttempted,
            Exception latestError, Exception historyError) {
        this.latestAttempted = latestAttempted;
        this.historyAttempted = historyAttempted;
        this.latestError = latestError;
        this.historyError = historyError;
    }

    public boolean hasError() { return latestError != null || historyError != null; }
}
