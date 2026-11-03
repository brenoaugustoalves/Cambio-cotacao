import br.com.cambioclaro.app.market.domain.MarketObservation;
import br.com.cambioclaro.app.market.presentation.MarketMovementPresenter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

public final class MarketMovementPresenterTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        MarketMovementPresenter presenter=new MarketMovementPresenter();ZoneId zone=ZoneId.of("America/Sao_Paulo");
        MarketObservation first=new MarketObservation(new BigDecimal("5.4000"),Instant.parse("2026-10-30T12:00:00Z"));
        MarketObservation risen=new MarketObservation(new BigDecimal("5.4002"),Instant.parse("2026-10-30T12:05:00Z"));
        MarketObservation fallen=new MarketObservation(new BigDecimal("5.3902"),Instant.parse("2026-10-30T12:10:00Z"));
        MarketObservation stable=new MarketObservation(new BigDecimal("5.3902"),Instant.parse("2026-10-30T12:15:00Z"));
        check(presenter.direction(Arrays.asList(first,risen),1)>0,"Positive observation is an increase");
        check(presenter.summary(Arrays.asList(first,risen),1,false,zone).contains("Subiu +R$ 0,0002"),"Increase displays exact absolute movement");
        check(presenter.summary(Arrays.asList(risen,fallen),1,false,zone).contains("Caiu −R$ 0,0100 (−0,185178%)"),"Decrease displays exact percent change from previous point");
        check(presenter.direction(Arrays.asList(fallen,stable),1)==0&&presenter.summary(Arrays.asList(fallen,stable),1,false,zone).contains("Ficou estável"),"Equal published prices are explicitly stable");
        check(presenter.summary(Collections.singletonList(first),0,false,zone).contains("Base do período")&&presenter.summary(Collections.singletonList(first),0,false,zone).contains("variação indisponível"),"First observation is a baseline without invented previous change");
        check(presenter.summary(Collections.singletonList(first),0,true,zone).contains("30/10 09:00"),"Long-period observation includes date and local time");
        check(presenter.summary(Collections.singletonList(first),0,true,true,zone).contains("30/10/2026 09:00"),"Year-scale selection includes the year in point timestamps");
        check(presenter.selectedMovement(Arrays.asList(first,risen),1,false,zone).contains("▲ Alta +R$ 0,0002"),"Selected chart point uses consistent exact movement");
        check(presenter.summary(Collections.emptyList(),0,false,zone).contains("Sem publicação"),"Empty point selection is safe");
        check(presenter.value(new BigDecimal("0.0002")).equals("0,0002"),"Quote precision is retained in Brazilian formatting");
        boolean missing=false;try{presenter.absoluteDelta(first,null);}catch(IllegalArgumentException expected){missing=true;}
        check(missing,"Movement requires two actual source observations");
        System.out.println(checks+" market movement presentation checks passed");
    }
}
