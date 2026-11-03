package br.com.cambioclaro.app.market.presentation;

/** Normalized navigation selection restored from saved Android state. */
public final class MainNavigationDisplay {
    public final String mainSection;
    public final String currencySection;

    public MainNavigationDisplay(String mainSection,String currencySection){
        this.mainSection=mainSection;
        this.currencySection=currencySection;
    }
}
