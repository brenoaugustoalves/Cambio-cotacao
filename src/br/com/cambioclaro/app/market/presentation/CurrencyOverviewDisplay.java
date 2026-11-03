package br.com.cambioclaro.app.market.presentation;

/** Immutable overview row for one BRL currency. */
public final class CurrencyOverviewDisplay {
    public final String price,change;
    public final int direction;
    public CurrencyOverviewDisplay(String price,String change,int direction){this.price=price;this.change=change;this.direction=direction;}
}
