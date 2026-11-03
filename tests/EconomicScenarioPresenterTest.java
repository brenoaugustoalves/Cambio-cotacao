import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.economy.application.CalculateEconomicScenario;
import br.com.cambioclaro.app.economy.application.EconomicQuoteProvider;
import br.com.cambioclaro.app.economy.application.EconomicScenario;
import br.com.cambioclaro.app.economy.domain.EconomicQuoteReference;
import br.com.cambioclaro.app.economy.presentation.EconomicScenarioDisplay;
import br.com.cambioclaro.app.economy.presentation.EconomicScenarioPresenter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;

public final class EconomicScenarioPresenterTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static void main(String[] args)throws Exception{
        Instant published=Instant.parse("2026-10-30T13:00:00Z");
        EconomicQuoteProvider source=(currency,market,now)->new EconomicQuoteReference(new BigDecimal("5.40"),"AwesomeAPI · venda (ask)",published,"Referência antiga");
        EconomicScenario scenario=new CalculateEconomicScenario(source).execute(FxCurrency.USD,true,new BigDecimal("100"),new BigDecimal("2"),new BigDecimal("1"),new BigDecimal("10"),published.plusSeconds(1));
        EconomicScenarioPresenter presenter=new EconomicScenarioPresenter();EconomicScenarioDisplay display=presenter.present(scenario,ZoneId.of("America/Sao_Paulo"));
        check(display.status.contains("AwesomeAPI · venda (ask)"),"Reference source is visible");
        check(display.status.contains("30/10/2026 10:00 (Brasília)")&&display.status.contains("Referência antiga"),"Published time and stale-reference warning are preserved");
        check(display.details.contains("Base: R$ 540,00")&&display.details.contains("Spread informado: R$ 10,80"),"Base and percentage costs are formatted as pt-BR currency");
        check(display.details.contains("Outros encargos: R$ 5,40")&&display.details.contains("Tarifa fixa: R$ 10,00")&&display.details.contains("Total estimado: R$ 566,20"),"All cost components and total are visible separately");
        check(presenter.unavailable().details.contains("Cálculo indisponível"),"Unavailable reference never shows a calculated total");
        boolean nullScenario=false;try{presenter.present(null,ZoneId.of("UTC"));}catch(IllegalArgumentException expected){nullScenario=true;}
        check(nullScenario,"Missing scenario is rejected");
        boolean nullZone=false;try{presenter.present(scenario,null);}catch(IllegalArgumentException expected){nullZone=true;}
        check(nullZone,"Missing time zone is rejected");
        System.out.println(checks+" economic scenario presentation checks passed");
    }
}
