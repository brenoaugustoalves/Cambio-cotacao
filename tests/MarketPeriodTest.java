import br.com.cambioclaro.app.market.domain.MarketPeriod;
import java.time.LocalDate;

public final class MarketPeriodTest {
    private static int checks;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    public static void main(String[] args){
        LocalDate end=LocalDate.of(2026,11,3);
        check(MarketPeriod.startDate(MarketPeriod.FIVE_DAYS,end).equals(LocalDate.of(2026,10,29)),"Five-day range uses five calendar days");
        check(MarketPeriod.startDate(MarketPeriod.MONTH,end).equals(LocalDate.of(2026,10,3)),"Month range subtracts one calendar month");
        check(MarketPeriod.startDate(MarketPeriod.TWO_MONTHS,end).equals(LocalDate.of(2026,9,3)),"Bimonthly range subtracts two calendar months");
        check(MarketPeriod.startDate(MarketPeriod.THREE_MONTHS,end).equals(LocalDate.of(2026,8,3)),"Quarterly range subtracts three calendar months");
        check(MarketPeriod.startDate(MarketPeriod.SIX_MONTHS,end).equals(LocalDate.of(2026,5,3)),"Semiannual range subtracts six calendar months");
        check(MarketPeriod.startDate(MarketPeriod.YEAR,end).equals(LocalDate.of(2025,11,3)),"Annual range subtracts one calendar year");
        check(MarketPeriod.previousYear(LocalDate.of(2026,10,29)).equals(LocalDate.of(2025,10,29)),"Comparison dates shift to the same prior-year calendar date");
        check(MarketPeriod.startDate(MarketPeriod.MONTH,LocalDate.of(2024,3,31)).equals(LocalDate.of(2024,2,29)),"Month subtraction clamps correctly in leap February");
        boolean invalid=false;try{MarketPeriod.startDate(120, end);}catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"Unknown chart periods are rejected");
        System.out.println(checks+" market calendar period checks passed");
    }
}
