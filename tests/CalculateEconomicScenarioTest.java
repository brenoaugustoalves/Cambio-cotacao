import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.economy.domain.EconomicSimulator;
import br.com.cambioclaro.app.economy.application.CalculateEconomicScenario;
import br.com.cambioclaro.app.economy.application.EconomicQuoteProvider;
import br.com.cambioclaro.app.economy.application.EconomicScenario;
import br.com.cambioclaro.app.economy.domain.EconomicQuoteReference;
import java.math.BigDecimal;
import java.time.Instant;

public final class CalculateEconomicScenarioTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    private static final class FakeProvider implements EconomicQuoteProvider {
        EconomicQuoteReference reference;boolean fail;boolean lastMarket;FxCurrency lastCurrency;
        public EconomicQuoteReference get(FxCurrency currency,boolean market,Instant now)throws Exception{lastCurrency=currency;lastMarket=market;if(fail)throw new java.io.IOException("provider unavailable");return reference;}
    }
    public static void main(String[] args)throws Exception{
        FakeProvider provider=new FakeProvider();Instant published=Instant.parse("2026-10-30T13:00:00Z"),now=published.plusSeconds(60);
        provider.reference=new EconomicQuoteReference(new BigDecimal("5.40"),"AwesomeAPI · venda (ask)",published,"Referência antiga");
        CalculateEconomicScenario useCase=new CalculateEconomicScenario(provider);
        EconomicScenario market=useCase.execute(FxCurrency.USD,true,new BigDecimal("100"),new BigDecimal("2"),new BigDecimal("1"),new BigDecimal("10"),now);
        check(provider.lastCurrency==FxCurrency.USD&&provider.lastMarket,"Selected currency and market source reach the provider");
        check(market.totals.base.compareTo(new BigDecimal("540.00"))==0&&market.totals.total.compareTo(new BigDecimal("566.20"))==0,"Scenario totals use the returned published quote");
        check(market.reference.publishedAt.equals(published)&&market.reference.warning.equals("Referência antiga"),"Result retains quote publication time and staleness warning");
        provider.reference=new EconomicQuoteReference(new BigDecimal("5.35"),"Banco Central · PTAX venda",published,null);
        EconomicScenario ptax=useCase.execute(FxCurrency.USD,false,new BigDecimal("10"),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,now);
        check(!provider.lastMarket&&ptax.totals.base.compareTo(new BigDecimal("53.50"))==0,"PTAX path is selected independently from market quote");
        boolean invalidAmount=false;try{useCase.execute(FxCurrency.USD,false,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,now);}catch(IllegalArgumentException expected){invalidAmount=true;}
        check(invalidAmount,"Invalid scenario input is rejected by the domain calculation");
        provider.fail=true;boolean sourceFailure=false;try{useCase.execute(FxCurrency.USD,true,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,now);}catch(java.io.IOException expected){sourceFailure=true;}
        check(sourceFailure,"Provider outage is returned to caller without inventing a rate");
        provider.fail=false;provider.reference=null;boolean noRate=false;try{useCase.execute(FxCurrency.EUR,true,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,now);}catch(IllegalStateException expected){noRate=true;}
        check(noRate,"Missing quote is explicitly unavailable");
        boolean nullCurrency=false;try{useCase.execute(null,true,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,now);}catch(IllegalArgumentException expected){nullCurrency=true;}
        check(nullCurrency,"Missing currency is rejected");
        boolean nullNow=false;try{useCase.execute(FxCurrency.USD,true,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,null);}catch(IllegalArgumentException expected){nullNow=true;}
        check(nullNow,"Missing evaluation instant is rejected");
        boolean nullProvider=false;try{new CalculateEconomicScenario(null);}catch(IllegalArgumentException expected){nullProvider=true;}
        check(nullProvider,"Missing quote provider is rejected");
        System.out.println(checks+" economic scenario use case checks passed");
    }
}
