package br.com.cambioclaro.app.market.application;

/** Source and persistence boundary for listed-stock and index quotes. */
public interface StockQuoteGateway {
    boolean indexEnabled();
    String fetchStocks() throws Exception;
    String fetchIndex() throws Exception;
    void save(String response, boolean index) throws Exception;
}
