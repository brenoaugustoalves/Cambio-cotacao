import br.com.cambioclaro.app.market.presentation.MainNavigationDisplay;
import br.com.cambioclaro.app.market.presentation.MainNavigationPresenter;

public final class MainNavigationPresenterTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        MainNavigationPresenter presenter=new MainNavigationPresenter();
        MainNavigationDisplay restored=presenter.present("Bolsa","Gráfico");
        check(restored.mainSection.equals("Bolsa")&&restored.currencySection.equals("Gráfico"),"Known saved sections are restored");
        MainNavigationDisplay invalid=presenter.present("Economia","Noticias");
        check(invalid.mainSection.equals("Moedas")&&invalid.currencySection.equals("Cotação"),"Renamed or unsupported persisted sections recover to visible defaults");
        MainNavigationDisplay missing=presenter.present(null,null);
        check(missing.mainSection.equals("Moedas")&&missing.currencySection.equals("Cotação"),"Missing saved navigation state recovers to defaults");
        check(presenter.present("Fontes","Outras").currencySection.equals("Outras"),"Every supported currency destination can be restored");
        System.out.println(checks+" main navigation presentation checks passed");
    }
}
