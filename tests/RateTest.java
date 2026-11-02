import br.com.cambioclaro.app.Rate;
import br.com.cambioclaro.app.EconomicSimulator;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public class RateTest {
    private static int count=0;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);count++;}
    private static void rejects(Runnable r,String label){try{r.run();}catch(IllegalArgumentException expected){count++;return;}throw new AssertionError(label);}
    public static void main(String[]args){
        // Values below are from the captured BCB response, not shipped app defaults.
        BigDecimal sell=new BigDecimal("5.3420");
        check(Rate.convert("100,00",sell).equals(new BigDecimal("534.20")),"BR comma conversion");
        check(Rate.convert("1.25",sell).equals(new BigDecimal("6.68")),"Decimal dot + rounding");
        check(Rate.convert("0",sell).equals(new BigDecimal("0.00")),"Zero");
        rejects(()->Rate.convert("1.000,00",sell),"Ambiguous thousands rejected");
        rejects(()->Rate.convert("-1",sell),"Negative rejected");
        rejects(()->Rate.convert("NaN",sell),"Non-number rejected");
        rejects(()->Rate.convert("1,234",sell),"Excess precision rejected");
        rejects(()->Rate.variation(BigDecimal.ZERO,sell),"Zero denominator rejected");
        check(Rate.variation(sell,sell).signum()==0,"Flat change");
        check(Rate.variation(new BigDecimal("5.2204"),sell).toPlainString().equals("2.32932342"),"Exact percent");
        LocalDate today=LocalDate.of(2026,10,29);
        Rate old=new Rate(new BigDecimal("5.2198"),new BigDecimal("5.2204"),LocalDateTime.of(2026,9,29,13,6));
        Rate last=new Rate(new BigDecimal("5.3414"),sell,LocalDateTime.of(2026,10,28,13,5));
        List<Rate> all=Rate.normalize(Arrays.asList(last,old,last),today);
        check(all.size()==2 && all.get(0)==old && all.get(1)==last,"Sort and deduplicate");
        check(Rate.period(all,today,7).size()==1,"Calendar range");
        check(Rate.period(all,today,30).size()==1,"30 calendar days exclude day -30");
        check(Rate.period(all,today,365).size()==2,"Year range");
        rejects(()->Rate.normalize(Arrays.asList(last),today.minusDays(2)),"Future dates rejected");
        rejects(()->new Rate(BigDecimal.ZERO,sell,last.at),"Invalid rate rejected");
        rejects(()->new Rate(new BigDecimal("6"),sell,last.at),"Inverted spread rejected");
        rejects(()->new Rate(new BigDecimal("1e-100000000"),sell,last.at),"Extreme scale rejected");
        rejects(()->new Rate(new BigDecimal("1E+3"),new BigDecimal("1E+4"),last.at),"Exponent format rejected");
        rejects(()->Rate.convert("9999999999999",sell),"Amount length bounded");
        check(Rate.convert("999999999999,99",sell).equals(new BigDecimal("5341999999999.95")),"Maximum accepted amount remains exact");
        BigDecimal amount=EconomicSimulator.parse("100,00",6,false),spread=EconomicSimulator.parse("2,00",2,true),fees=EconomicSimulator.parse("1,10",2,true),fixed=EconomicSimulator.parse("12,00",2,true);
        EconomicSimulator.Result scenario=EconomicSimulator.calculate(amount,new BigDecimal("5.4000"),spread,fees,fixed);
        check(scenario.base.equals(new BigDecimal("540.00"))&&scenario.spread.equals(new BigDecimal("10.80"))&&scenario.extra.equals(new BigDecimal("5.94"))&&scenario.total.equals(new BigDecimal("568.74")),"Scenario totals are transparent sum of base and user-entered costs");
        check(EconomicSimulator.parse("0",6,true).signum()==0,"Zero charge accepted");
        rejects(()->EconomicSimulator.parse("1.000,00",2,false),"Scenario rejects thousands separators");
        rejects(()->EconomicSimulator.parse("NaN",2,true),"Scenario rejects non-number");
        rejects(()->EconomicSimulator.parse("-1",2,true),"Scenario rejects negative costs");
        rejects(()->EconomicSimulator.calculate(BigDecimal.ZERO,new BigDecimal("5"),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO),"Scenario rejects zero foreign amount");
        rejects(()->EconomicSimulator.calculate(BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO),"Scenario rejects zero reference rate");
        rejects(()->EconomicSimulator.calculate(BigDecimal.ONE,BigDecimal.ONE,new BigDecimal("1001"),BigDecimal.ZERO,BigDecimal.ZERO),"Scenario bounds pathological percentages");
        System.out.println(count+" domain checks passed");
    }
}
