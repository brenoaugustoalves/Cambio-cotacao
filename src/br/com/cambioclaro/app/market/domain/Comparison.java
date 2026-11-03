package br.com.cambioclaro.app.market.domain;
import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.Rate;
import java.util.*;
import java.time.LocalDate;
import java.math.BigDecimal;
/** Intersection of publication dates: never align different base dates or fill missing quotes. */
public final class Comparison {
    public final List<LocalDate> dates;
    public final Map<FxCurrency,List<BigDecimal>> series;
    public Comparison(Map<FxCurrency,List<Rate>> input){
        if(input==null)throw new IllegalArgumentException("Séries para comparar são obrigatórias");
        TreeSet<LocalDate> common=null;Map<FxCurrency,Map<LocalDate,Rate>> indexed=new LinkedHashMap<>();
        for(Map.Entry<FxCurrency,List<Rate>> e:input.entrySet()){
            if(e.getKey()==null||e.getValue()==null)throw new IllegalArgumentException("Série de moeda inválida");
            Map<LocalDate,Rate> byDate=new TreeMap<>();
            for(Rate r:e.getValue()){if(r==null)throw new IllegalArgumentException("Observação de comparação ausente");byDate.put(r.at.toLocalDate(),r);}
            indexed.put(e.getKey(),byDate);if(common==null)common=new TreeSet<>(byDate.keySet());else common.retainAll(byDate.keySet());
        }
        dates=Collections.unmodifiableList(common==null?new ArrayList<>():new ArrayList<>(common));
        Map<FxCurrency,List<BigDecimal>> result=new LinkedHashMap<>();
        if(!dates.isEmpty())for(FxCurrency fx:indexed.keySet()){List<BigDecimal> values=new ArrayList<>();BigDecimal base=indexed.get(fx).get(dates.get(0)).sell;for(LocalDate day:dates)values.add(Rate.variation(base,indexed.get(fx).get(day).sell));result.put(fx,Collections.unmodifiableList(values));}
        series=Collections.unmodifiableMap(result);
    }
}
