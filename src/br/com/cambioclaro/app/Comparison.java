package br.com.cambioclaro.app;
import java.util.*;
import java.time.LocalDate;
import java.math.BigDecimal;
/** Intersection of publication dates: never align different base dates or fill missing quotes. */
public final class Comparison {
    public final List<LocalDate> dates;
    public final Map<FxCurrency,List<BigDecimal>> series=new LinkedHashMap<>();
    public Comparison(Map<FxCurrency,List<Rate>> input){
        TreeSet<LocalDate> common=null;Map<FxCurrency,Map<LocalDate,Rate>> indexed=new LinkedHashMap<>();
        for(Map.Entry<FxCurrency,List<Rate>> e:input.entrySet()){Map<LocalDate,Rate> byDate=new TreeMap<>();for(Rate r:e.getValue())byDate.put(r.at.toLocalDate(),r);indexed.put(e.getKey(),byDate);if(common==null)common=new TreeSet<>(byDate.keySet());else common.retainAll(byDate.keySet());}
        dates=Collections.unmodifiableList(common==null?new ArrayList<>():new ArrayList<>(common));
        if(dates.isEmpty())return;
        for(FxCurrency fx:indexed.keySet()){List<BigDecimal> values=new ArrayList<>();BigDecimal base=indexed.get(fx).get(dates.get(0)).sell;for(LocalDate day:dates)values.add(Rate.variation(base,indexed.get(fx).get(day).sell));series.put(fx,Collections.unmodifiableList(values));}
    }
}
