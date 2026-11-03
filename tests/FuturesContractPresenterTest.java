import br.com.cambioclaro.app.market.domain.FuturesContracts;
import br.com.cambioclaro.app.market.presentation.FuturesContractDisplay;
import br.com.cambioclaro.app.market.presentation.FuturesContractPresenter;
import java.time.LocalDate;

public final class FuturesContractPresenterTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        FuturesContractPresenter presenter=new FuturesContractPresenter();
        FuturesContractDisplay display=presenter.present(FuturesContracts.current("WIN",LocalDate.of(2026,4,16)));
        check(display.contractCode.equals("WINM26"),"Selected contract code is exposed to the UI");
        check(display.details.contains("Vencimento: 06/2026")&&display.details.contains("17/06/2026"),"Official maturity month and negotiation date are formatted");
        check(display.details.contains("Última cotação, variação, horário e atraso indisponíveis")&&display.details.contains("provedor de futuros não integrado"),"Missing market data is explicit without placeholder values");
        boolean missing=false;try{presenter.present(null);}catch(IllegalArgumentException expected){missing=true;}
        check(missing,"Missing contract is rejected");
        System.out.println(checks+" futures contract presentation checks passed");
    }
}
