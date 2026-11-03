package br.com.cambioclaro.app.market.presentation;

/** Validates persisted destinations so an unknown label cannot hide every screen. */
public final class MainNavigationPresenter {
    private static final String[] MAIN_SECTIONS={"Moedas","Bolsa","Fontes"};
    private static final String[] CURRENCY_SECTIONS={"Cotação","Gráfico","PTAX","Converter","Outras"};

    public MainNavigationDisplay present(String mainSection,String currencySection){
        return new MainNavigationDisplay(contains(MAIN_SECTIONS,mainSection)?mainSection:"Moedas",
                contains(CURRENCY_SECTIONS,currencySection)?currencySection:"Cotação");
    }

    private static boolean contains(String[] allowed,String value){
        if(value==null)return false;
        for(String item:allowed)if(item.equals(value))return true;
        return false;
    }
}
