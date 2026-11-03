import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.market.domain.MarketObservation;
import br.com.cambioclaro.app.market.presentation.MarketQuoteDisplay;
import br.com.cambioclaro.app.market.presentation.MarketQuotePresenter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

public final class MarketQuotePresenterTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        MarketQuotePresenter presenter=new MarketQuotePresenter();ZoneId zone=ZoneId.of("America/Sao_Paulo");Instant first=Instant.parse("2026-10-30T12:00:00Z"),second=first.plusSeconds(300),now=second.plusSeconds(60);LocalDate today=LocalDate.of(2026,10,30);
        MarketObservation a=new MarketObservation(new BigDecimal("5.4000"),first),b=new MarketObservation(new BigDecimal("5.4100"),second);
        MarketQuoteDisplay usd=presenter.present(FxCurrency.USD,false,b.ask,b.publishedAt,Arrays.asList(a,b),today,now,zone);
        check(usd.pair.equals("USD/BRL")&&usd.rate.equals("5,41"),"USD card presents the correct pair and two-decimal main price");
        check(usd.exactRate.contains("5,4100")&&usd.exactRate.contains("venda (ask)"),"Exact USD quote retains source and ask semantics");
        check(usd.movement.contains("▲ Alta")&&usd.movement.contains("0,19% desde a publicação anterior")&&usd.movement.contains("desde a 1ª observação"),"Card explains today's actual percent movements");
        check(usd.publishedAt.contains("30/10/2026 às 09:05 (Brasília)")&&!usd.stale&&usd.freshness.contains("Atualizada há 1 min"),"Publication timestamp and freshness are explicit in local time");
        MarketQuoteDisplay eur=presenter.present(FxCurrency.EUR,false,new BigDecimal("6.123456"),b.publishedAt,Collections.singletonList(b),today,now,zone);
        check(eur.rate.equals("6,1235")&&eur.exactRate==null,"Other currencies retain four price decimals and no USD-only exact-rate copy");
        MarketQuoteDisplay noQuote=presenter.present(FxCurrency.USD,false,null,null,Collections.emptyList(),today,now,zone);
        check(noQuote.rate.equals("—")&&noQuote.movement.contains("indisponível")&&noQuote.stale,"Missing quote is explicit and marked stale");
        MarketQuoteDisplay onlyOne=presenter.present(FxCurrency.USD,false,b.ask,b.publishedAt,Collections.singletonList(b),today,now,zone);
        check(onlyOne.movement.contains("aguardando outra publicação"),"One real point has no fabricated movement");
        MarketQuoteDisplay old=presenter.present(FxCurrency.USD,false,b.ask,b.publishedAt,Collections.singletonList(b),today.minusDays(1),now,zone);
        check(old.stale&&old.freshness.contains("Sem dado de hoje"),"Previous-day quote is marked stale rather than current");
        MarketQuoteDisplay tourism=presenter.present(FxCurrency.USD,true,b.ask,b.publishedAt,Collections.singletonList(b),today,now,zone);
        check(tourism.pair.equals("USD/BRLT")&&tourism.exactRate.contains("sem IOF/tarifas"),"Tourism pair remains separate from commercial USD/BRL");
        boolean badTourism=false;try{presenter.present(FxCurrency.EUR,true,b.ask,b.publishedAt,Collections.emptyList(),today,now,zone);}catch(IllegalArgumentException expected){badTourism=true;}
        check(badTourism,"Tourism presentation cannot be applied to another currency");
        boolean badQuote=false;try{presenter.present(FxCurrency.USD,false,BigDecimal.ZERO,b.publishedAt,Collections.emptyList(),today,now,zone);}catch(IllegalArgumentException expected){badQuote=true;}
        check(badQuote,"Nonpositive quote is rejected");
        System.out.println(checks+" market quote presentation checks passed");
    }
}
