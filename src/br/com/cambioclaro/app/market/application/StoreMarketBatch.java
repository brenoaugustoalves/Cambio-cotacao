package br.com.cambioclaro.app.market.application;

/** Fetches one provider batch and asks the repository to validate and persist it. */
public final class StoreMarketBatch {
    private final MarketQuoteSource source;
    private final MarketQuoteRepository quotes;

    public StoreMarketBatch(MarketQuoteSource source, MarketQuoteRepository quotes) {
        if (source == null || quotes == null) {
            throw new IllegalArgumentException("Fonte e repositório de mercado são obrigatórios");
        }
        this.source = source;
        this.quotes = quotes;
    }

    public void execute() throws Exception {
        quotes.saveBatch(source.fetch());
    }
}
