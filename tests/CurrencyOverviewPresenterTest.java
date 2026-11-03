import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.Rate;
import br.com.cambioclaro.app.market.domain.MarketObservation;
import br.com.cambioclaro.app.market.presentation.CurrencyOverviewDisplay;
import br.com.cambioclaro.app.market.presentation.CurrencyOverviewPresenter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

public final class CurrencyOverviewPresenterTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    private static Rate rate(int day,String value){BigDecimal sell=new BigDecimal(value);return new Rate(sell.subtract(new BigDecimal("0.01")),sell,LocalDateTime.of(2026,10,day,13,0));}
    public static void main(String[] args){
        CurrencyOverviewPresenter presenter=new CurrencyOverviewPresenter();LocalDate today=LocalDate.of(2026,10,30);ZoneId zone=ZoneId.of("America/Sao_Paulo");
        Instant first=Instant.parse("2026-10-30T12:00:00Z"),second=first.plusSeconds(60);
        MarketObservation one=new MarketObservation(new BigDecimal("5.4000"),first),two=new MarketObservation(new BigDecimal("5.4100"),second);
        CurrencyOverviewDisplay market=presenter.present(FxCurrency.USD,two,Arrays.asList(one,two),Collections.emptyList(),false,today,zone);
        check(market.price.equals("5,4100 por USD 1")&&market.change.contains("Mercado ·")&&market.direction>0,"Valid current market prices take priority and show an actual movement");
        CurrencyOverviewDisplay staleMarket=presenter.present(FxCurrency.USD,new MarketObservation(BigDecimal.ONE,first.minusSeconds(86400)),Collections.emptyList(),Collections.singletonList(rate(29,"5.00")),false,today,zone);
        check(staleMarket.direction==2&&staleMarket.change.contains("sem observação de hoje"),"Previous-day market quote is distinguished from today's PTAX fallback");
        CurrencyOverviewDisplay onePoint=presenter.present(FxCurrency.EUR,new MarketObservation(new BigDecimal("6.0000"),second),Collections.singletonList(two),Collections.emptyList(),false,today,zone);
        check(onePoint.change.contains("aguardando outra publicação"),"Single market point has no fabricated movement");
        CurrencyOverviewDisplay ptaxSingle=presenter.present(FxCurrency.USD,null,Collections.emptyList(),Collections.singletonList(rate(29,"5.10")),true,today,zone);
        check(ptaxSingle.price.equals("5,1000 por USD 1")&&ptaxSingle.change.contains("sem publicação anterior"),"Single PTAX value is explicitly presented without a change");
        CurrencyOverviewDisplay ptaxMove=presenter.present(FxCurrency.USD,null,Collections.emptyList(),Arrays.asList(rate(29,"5.00"),rate(30,"5.50")),true,today,zone);
        check(ptaxMove.direction>0&&ptaxMove.change.contains("▲ Alta 10,00%")&&ptaxMove.change.contains("Atualização falhou • dado salvo"),"Saved PTAX movement remains labeled after refresh failure");
        CurrencyOverviewDisplay unavailable=presenter.present(FxCurrency.EUR,null,Collections.emptyList(),Collections.emptyList(),true,today,zone);
        check(unavailable.price.contains("Fonte indisponível")&&unavailable.change.contains("Sem cotação válida"),"No quote/cache is explicit and not estimated");
        CurrencyOverviewDisplay pending=presenter.present(FxCurrency.EUR,null,Collections.emptyList(),Collections.emptyList(),false,today,zone);
        check(pending.price.contains("Aguardando dados"),"Unrequested quote has a waiting state rather than an error");
        boolean nullCurrency=false;try{presenter.present(null,null,Collections.emptyList(),Collections.emptyList(),false,today,zone);}catch(IllegalArgumentException expected){nullCurrency=true;}
        check(nullCurrency,"Missing currency is rejected");
        System.out.println(checks+" currency overview presentation checks passed");
    }
}
