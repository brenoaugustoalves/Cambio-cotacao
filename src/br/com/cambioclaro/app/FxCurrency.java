package br.com.cambioclaro.app;
/** Supported codes verified against BCB PTAX /Moedas. Never contains prices. */
public enum FxCurrency {
    USD("Dólar americano"), EUR("Euro"), GBP("Libra esterlina"), JPY("Iene japonês"),
    CAD("Dólar canadense"), AUD("Dólar australiano"), CHF("Franco suíço"),
    DKK("Coroa dinamarquesa"), NOK("Coroa norueguesa"), SEK("Coroa sueca");
    public final String label;
    FxCurrency(String label){this.label=label;}
    @Override public String toString(){return name()+" • "+label;}
}
