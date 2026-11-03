import br.com.cambioclaro.app.market.domain.MarketObservation;
import br.com.cambioclaro.app.market.domain.YearOverYearComparison;
import br.com.cambioclaro.app.market.presentation.YearOverYearComparisonPresenter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;

public final class YearOverYearComparisonTest {
    private static int checks;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    public static void main(String[] args){
        ZoneId zone=ZoneId.of("America/Sao_Paulo");
        MarketObservation priorStart=new MarketObservation(new BigDecimal("5.0000"),Instant.parse("2025-10-01T12:00:00Z"));
        MarketObservation priorEnd=new MarketObservation(new BigDecimal("5.5000"),Instant.parse("2025-11-03T12:00:00Z"));
        MarketObservation currentStart=new MarketObservation(new BigDecimal("5.2000"),Instant.parse("2026-10-01T12:00:00Z"));
        MarketObservation currentEnd=new MarketObservation(new BigDecimal("5.4600"),Instant.parse("2026-11-03T12:00:00Z"));
        YearOverYearComparison comparison=YearOverYearComparison.compare(currentStart,currentEnd,priorStart,priorEnd);
        check(comparison.currentChange.compareTo(new BigDecimal("0.2600"))==0,"Current movement uses the selected interval endpoints");
        check(comparison.currentPercentChange.compareTo(new BigDecimal("5.000000"))==0,"Current percentage uses its own starting quote");
        check(comparison.previousYearChange.compareTo(new BigDecimal("0.5000"))==0,"Prior-year movement uses the equivalent interval endpoints");
        check(comparison.previousYearPercentChange.compareTo(new BigDecimal("10.000000"))==0,"Prior-year percentage uses its own starting quote");
        check(comparison.percentagePointDifference.compareTo(new BigDecimal("-5.000000"))==0,"Comparison measures the difference between interval movements");
        String shown=new YearOverYearComparisonPresenter().present(comparison,zone);
        check(shown.contains("2025 (01/10–03/11)")&&shown.contains("2026 (01/10–03/11)"),"Presentation identifies both equivalent calendar intervals");
        check(shown.contains("+R$ 0,5000 (+10,00%)")&&shown.contains("+R$ 0,2600 (+5,00%)"),"Presentation gives both actual interval changes");
        check(shown.contains("Diferença das variações: −5,00 p.p."),"Presentation labels difference in percentage points");
        MarketObservation declineStart=new MarketObservation(new BigDecimal("5.5000"),Instant.parse("2026-10-01T12:00:00Z"));
        MarketObservation declineEnd=new MarketObservation(new BigDecimal("5.0000"),Instant.parse("2026-11-03T12:00:00Z"));
        YearOverYearComparison decline=YearOverYearComparison.compare(declineStart,declineEnd,priorStart,priorEnd);
        check(decline.currentChange.signum()<0&&decline.currentPercentChange.signum()<0,"A declining interval preserves negative movement signs");
        boolean invalid=false;try{YearOverYearComparison.compare(currentStart,null,priorStart,priorEnd);}catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"Comparison requires four real interval endpoint observations");
        boolean reversed=false;try{YearOverYearComparison.compare(currentEnd,currentStart,priorStart,priorEnd);}catch(IllegalArgumentException expected){reversed=true;}
        check(reversed,"Comparison rejects reversed observation dates");
        MarketObservation twoYearsBack=new MarketObservation(new BigDecimal("4.5000"),Instant.parse("2024-11-03T12:00:00Z"));
        YearOverYearComparison annual=YearOverYearComparison.compare(twoYearsBack,priorEnd,priorStart,currentEnd);
        check(new YearOverYearComparisonPresenter().present(annual,zone).contains("2025 (03/11/2024–03/11/2025)"),"Annual period prints both years when its range crosses a calendar year");
        System.out.println(checks+" year-over-year comparison checks passed");
    }
}
