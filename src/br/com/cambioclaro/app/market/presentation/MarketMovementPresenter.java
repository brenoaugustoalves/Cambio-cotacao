package br.com.cambioclaro.app.market.presentation;

import br.com.cambioclaro.app.market.domain.MarketObservation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Presents changes between actual sequential market observations only. */
public final class MarketMovementPresenter {
    private static final Locale BR=new Locale("pt","BR");
    private static final DateTimeFormatter CLOCK=DateTimeFormatter.ofPattern("HH:mm",BR);
    private static final DateTimeFormatter DATE_CLOCK=DateTimeFormatter.ofPattern("dd/MM HH:mm",BR);
    private static final DateTimeFormatter FULL_DATE_CLOCK=DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm",BR);
    public BigDecimal absoluteDelta(MarketObservation previous,MarketObservation current){
        if(previous==null||current==null)throw new IllegalArgumentException("Duas publicações são necessárias");
        return current.ask.subtract(previous.ask);
    }
    public BigDecimal percentDelta(MarketObservation previous,MarketObservation current){
        if(previous==null||current==null||previous.ask.signum()<=0)throw new IllegalArgumentException("Base inválida");
        return absoluteDelta(previous,current).multiply(new BigDecimal("100")).divide(previous.ask,6,RoundingMode.HALF_UP);
    }
    public int direction(List<MarketObservation> points,int index){
        if(points==null||index<=0||index>=points.size())return 0;
        return absoluteDelta(points.get(index-1),points.get(index)).signum();
    }
    public String summary(List<MarketObservation> points,int index,boolean includeDate,ZoneId zone){
        return summary(points,index,includeDate,false,zone);
    }
    public String summary(List<MarketObservation> points,int index,boolean includeDate,boolean includeYear,ZoneId zone){
        if(points==null||points.isEmpty()||index<0||index>=points.size())return "Sem publicação para comparar.";
        if(zone==null)throw new IllegalArgumentException("Fuso horário obrigatório");
        MarketObservation current=points.get(index);DateTimeFormatter format=includeYear?FULL_DATE_CLOCK:includeDate?DATE_CLOCK:CLOCK;
        String time=format.format(current.publishedAt.atZone(zone));
        if(index==0)return "Base do período · "+time+" · R$ "+value(current.ask)+" · variação indisponível sem publicação anterior.";
        MarketObservation previous=points.get(index-1);BigDecimal delta=absoluteDelta(previous,current),percent=percentDelta(previous,current);
        String direction=delta.signum()>0?"Subiu":delta.signum()<0?"Caiu":"Ficou estável";
        String amount=(delta.signum()>0?"+":delta.signum()<0?"−":"")+"R$ "+value(delta.abs());
        String percentText=(percent.signum()>0?"+":percent.signum()<0?"−":"")+value(percent.abs());
        return time+" · "+direction+" "+amount+" ("+percentText+"%) desde "+format.format(previous.publishedAt.atZone(zone))+" · cotação R$ "+value(current.ask);
    }
    public String selectedMovement(List<MarketObservation> points,int index,boolean includeDate,ZoneId zone){
        return selectedMovement(points,index,includeDate,false,zone);
    }
    public String selectedMovement(List<MarketObservation> points,int index,boolean includeDate,boolean includeYear,ZoneId zone){
        if(points==null||points.isEmpty()||index<0||index>=points.size())return "Sem publicação para comparar.";
        DateTimeFormatter format=includeYear?FULL_DATE_CLOCK:includeDate?DATE_CLOCK:CLOCK;MarketObservation current=points.get(index);
        String time=format.format(current.publishedAt.atZone(zone));
        if(index==0)return "BASE · "+time+" · R$ "+value(current.ask)+" · sem publicação anterior";
        MarketObservation previous=points.get(index-1);BigDecimal delta=absoluteDelta(previous,current),change=percentDelta(previous,current);
        String direction=delta.signum()>0?"▲ Alta ":delta.signum()<0?"▼ Baixa ":"● Estável ";
        String sign=delta.signum()>0?"+":delta.signum()<0?"−":"";String percentSign=change.signum()>0?"+":change.signum()<0?"−":"";
        return time+" · "+direction+sign+"R$ "+value(delta.abs())+" ("+percentSign+value(change.abs())+"%) desde "+format.format(previous.publishedAt.atZone(zone))+" · atual R$ "+value(current.ask);
    }
    public String value(BigDecimal amount){
        if(amount==null)throw new IllegalArgumentException("Valor obrigatório");
        NumberFormat format=NumberFormat.getNumberInstance(BR);int places=Math.max(4,Math.min(8,amount.scale()));
        format.setMinimumFractionDigits(places);format.setMaximumFractionDigits(places);return format.format(amount);
    }
}
