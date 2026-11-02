package br.com.cambioclaro.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Formats only observed source values; deltas compare consecutive published observations. */
public final class IntradayMovement {
    private static final Locale BR=new Locale("pt","BR");
    private static final DateTimeFormatter CLOCK=DateTimeFormatter.ofPattern("HH:mm",BR);
    private IntradayMovement(){}

    public static BigDecimal absoluteDelta(MarketHistory.Point previous,MarketHistory.Point current){
        if(previous==null||current==null)throw new IllegalArgumentException("Duas publicações são necessárias");
        return current.ask.subtract(previous.ask);
    }

    public static BigDecimal percentDelta(MarketHistory.Point previous,MarketHistory.Point current){
        if(previous==null||current==null||previous.ask.signum()<=0)throw new IllegalArgumentException("Base inválida");
        return absoluteDelta(previous,current).multiply(new BigDecimal("100")).divide(previous.ask,6,RoundingMode.HALF_UP);
    }

    public static String summary(List<MarketHistory.Point> points,int index){
        if(points==null||points.isEmpty()||index<0||index>=points.size())return "Sem publicação para comparar.";
        MarketHistory.Point current=points.get(index);
        String time=current.at.atZone(BcbClient.ZONE).format(CLOCK);
        if(index==0)return "Base do dia · "+time+" · R$ "+value(current.ask)+" · variação indisponível sem publicação anterior.";
        MarketHistory.Point previous=points.get(index-1);
        BigDecimal delta=absoluteDelta(previous,current),percent=percentDelta(previous,current);
        String direction=delta.signum()>0?"Subiu":delta.signum()<0?"Caiu":"Ficou estável";
        String sign=delta.signum()>0?"+":delta.signum()<0?"−":"";
        String pctSign=percent.signum()>0?"+":percent.signum()<0?"−":"";
        String amount=sign.isEmpty()?"R$ "+value(delta.abs()):sign+"R$ "+value(delta.abs());
        return time+" · "+direction+" "+amount+" ("+pctSign+value(percent.abs())+"%) desde "+previous.at.atZone(BcbClient.ZONE).format(CLOCK)+" · cotação R$ "+value(current.ask);
    }

    public static int direction(List<MarketHistory.Point> points,int index){
        if(points==null||index<=0||index>=points.size())return 0;
        return absoluteDelta(points.get(index-1),points.get(index)).signum();
    }

    public static String value(BigDecimal amount){
        NumberFormat f=NumberFormat.getNumberInstance(BR);
        int places=Math.max(4,Math.min(8,amount.scale()));
        f.setMinimumFractionDigits(places);f.setMaximumFractionDigits(places);
        return f.format(amount);
    }
}
