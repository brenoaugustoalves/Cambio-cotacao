package br.com.cambioclaro.app.market.presentation;

import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.Rate;
import br.com.cambioclaro.app.market.domain.MarketObservation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/** Presents a published ask quote, today's observed movement, and source freshness. */
public final class MarketQuotePresenter {
    private static final Locale BR=new Locale("pt","BR");
    private static final DateTimeFormatter DATE_TIME=DateTimeFormatter.ofPattern("dd/MM/uuuu 'às' HH:mm",BR);
    private static final DateTimeFormatter CLOCK=DateTimeFormatter.ofPattern("HH:mm",BR);

    public MarketQuoteDisplay present(FxCurrency currency,boolean tourism,BigDecimal ask,Instant publishedAt,
            List<MarketObservation> todayObservations,LocalDate today,Instant now,ZoneId zone){
        if(currency==null||todayObservations==null||today==null||now==null||zone==null||tourism&&currency!=FxCurrency.USD)
            throw new IllegalArgumentException("Dados de cotação inválidos");
        for(MarketObservation observation:todayObservations)if(observation==null)throw new IllegalArgumentException("Observação de mercado ausente");
        String pair=tourism?"USD/BRLT":currency.name()+"/BRL";
        if(ask==null||publishedAt==null)return new MarketQuoteDisplay(pair,"—","Variação de mercado indisponível","Sem cotação de mercado recebida","Sem cotação recebida · toque em Atualizar",null,0,true);
        if(ask.signum()<=0||publishedAt.isAfter(now.plusSeconds(300)))throw new IllegalArgumentException("Cotação de mercado inválida");
        int digits=currency==FxCurrency.USD?2:4;
        String rate=number(ask,digits);
        String exact=currency==FxCurrency.USD?"Taxa exata: "+number(ask,4)+" · venda (ask) · AwesomeAPI"+(tourism?" · sem IOF/tarifas":""):null;
        TreeMap<Instant,BigDecimal> sequence=new TreeMap<>();
        for(MarketObservation point:todayObservations)if(point.publishedAt.atZone(zone).toLocalDate().equals(today))sequence.put(point.publishedAt,point.ask);
        if(publishedAt.atZone(zone).toLocalDate().equals(today))sequence.put(publishedAt,ask);
        String movement;int direction=0;
        if(sequence.size()<2)movement="Variação indisponível: aguardando outra publicação de hoje";
        else{
            java.util.Map.Entry<Instant,BigDecimal> latest=sequence.lastEntry(),previous=sequence.lowerEntry(latest.getKey());
            BigDecimal sincePrevious=Rate.variation(previous.getValue(),latest.getValue());
            BigDecimal sinceFirst=Rate.variation(sequence.firstEntry().getValue(),latest.getValue());
            direction=sincePrevious.setScale(2,RoundingMode.HALF_UP).signum();
            String arrow=direction>0?"▲ Alta ":direction<0?"▼ Baixa ":"● Estável ";
            movement=arrow+percent(sincePrevious)+" desde a publicação anterior ("+CLOCK.format(previous.getKey().atZone(zone))+ ")\n"+percent(sinceFirst)+" desde a 1ª observação de hoje ("+CLOCK.format(sequence.firstKey().atZone(zone))+")";
        }
        java.time.ZonedDateTime at=publishedAt.atZone(zone);
        String timestamp="Publicada em "+DATE_TIME.format(at)+" (Brasília)";
        boolean isToday=at.toLocalDate().equals(today);long age=Math.max(0,Duration.between(publishedAt,now).toMinutes());
        String freshness=isToday?(age>=10?"Atrasada · publicada há "+age+" min":"Atualizada há "+age+" min · pode haver atraso na fonte"):
                "Sem dado de hoje · última publicação em "+DateTimeFormatter.ofPattern("dd/MM HH:mm",BR).format(at);
        return new MarketQuoteDisplay(pair,rate,movement,timestamp,freshness,exact,direction,!isToday||age>=10);
    }
    private static String number(BigDecimal value,int digits){NumberFormat f=NumberFormat.getNumberInstance(BR);f.setMinimumFractionDigits(digits);f.setMaximumFractionDigits(digits);return f.format(value);}
    private static String percent(BigDecimal value){return number(value,2)+"%";}
}
