package br.com.cambioclaro.app.market.application;

/** Separate source/cache boundary for USD/BRLT latest quote and its intraday history. */
public interface TourismQuoteGateway {
    long latestCheckedAt();
    long historyCheckedAt();
    void refreshLatest() throws Exception;
    void refreshHistory() throws Exception;
}
