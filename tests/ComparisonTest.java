import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.Rate;
import br.com.cambioclaro.app.market.domain.Comparison;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ComparisonTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    private static Rate rate(int day,String value){return new Rate(new BigDecimal(value).subtract(new BigDecimal("0.01")),new BigDecimal(value),LocalDateTime.of(2026,1,day,13,0));}
    public static void main(String[] args){
        Map<FxCurrency,List<Rate>> input=new LinkedHashMap<>();
        input.put(FxCurrency.USD,Arrays.asList(rate(1,"5.00"),rate(2,"5.10"),rate(4,"5.40")));
        input.put(FxCurrency.EUR,Arrays.asList(rate(2,"6.20"),rate(3,"6.10"),rate(4,"6.00")));
        Comparison comparison=new Comparison(input);
        check(comparison.dates.equals(Arrays.asList(LocalDate.of(2026,1,2),LocalDate.of(2026,1,4))),"Only common actual publication dates are aligned");
        check(comparison.series.get(FxCurrency.USD).size()==2&&comparison.series.get(FxCurrency.EUR).size()==2,"Each currency has values only for common dates");
        check(comparison.series.get(FxCurrency.USD).get(0).signum()==0,"The common starting observation is the zero baseline");
        check(comparison.series.get(FxCurrency.USD).get(1).compareTo(new BigDecimal("5.88235294"))==0,"Variation is measured against each currency's common-date base");
        check(comparison.series.get(FxCurrency.EUR).get(1).compareTo(new BigDecimal("-3.22580645"))==0,"Negative currency variation is kept exact to domain precision");
        boolean immutable=false;try{comparison.series.put(FxCurrency.GBP,Collections.emptyList());}catch(UnsupportedOperationException expected){immutable=true;}
        check(immutable,"Comparison series map is immutable");
        boolean valuesImmutable=false;try{comparison.series.get(FxCurrency.USD).add(BigDecimal.ONE);}catch(UnsupportedOperationException expected){valuesImmutable=true;}
        check(valuesImmutable,"Currency series values are immutable");
        Map<FxCurrency,List<Rate>> noOverlap=new LinkedHashMap<>();noOverlap.put(FxCurrency.USD,Collections.singletonList(rate(1,"5.00")));noOverlap.put(FxCurrency.EUR,Collections.singletonList(rate(2,"6.00")));
        check(new Comparison(noOverlap).dates.isEmpty()&&new Comparison(noOverlap).series.isEmpty(),"Non-overlapping dates return no comparison instead of filling gaps");
        check(new Comparison(Collections.emptyMap()).dates.isEmpty(),"Empty input returns an empty comparison");
        boolean nullInput=false;try{new Comparison(null);}catch(IllegalArgumentException expected){nullInput=true;}
        check(nullInput,"Missing input map is rejected");
        System.out.println(checks+" currency comparison domain checks passed");
    }
}
