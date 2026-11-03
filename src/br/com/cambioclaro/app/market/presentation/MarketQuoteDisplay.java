package br.com.cambioclaro.app.market.presentation;

/** Immutable display values for the selected market quote card. */
public final class MarketQuoteDisplay {
    public final String pair,rate,movement,publishedAt,freshness,exactRate;
    public final int direction;
    public final boolean stale;
    MarketQuoteDisplay(String pair,String rate,String movement,String publishedAt,String freshness,
            String exactRate,int direction,boolean stale){this.pair=pair;this.rate=rate;this.movement=movement;this.publishedAt=publishedAt;this.freshness=freshness;this.exactRate=exactRate;this.direction=direction;this.stale=stale;}
}
