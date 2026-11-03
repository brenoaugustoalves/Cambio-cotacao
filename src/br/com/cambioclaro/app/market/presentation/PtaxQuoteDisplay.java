package br.com.cambioclaro.app.market.presentation;

/** Immutable PTAX value/date/change texts for the currency screen. */
public final class PtaxQuoteDisplay {
    public final String value,publishedAt,change;
    public final int direction;
    PtaxQuoteDisplay(String value,String publishedAt,String change,int direction){this.value=value;this.publishedAt=publishedAt;this.change=change;this.direction=direction;}
}
