package br.com.cambioclaro.app;

import br.com.cambioclaro.app.market.domain.MarketObservation;
import br.com.cambioclaro.app.market.presentation.MarketMovementPresenter;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Android model adapter retained for chart callers; wording and math live in the pure presenter. */
public final class IntradayMovement {
    private static final MarketMovementPresenter PRESENTER=new MarketMovementPresenter();
    private IntradayMovement() { }

    public static BigDecimal absoluteDelta(MarketHistory.Point previous,MarketHistory.Point current){
        return PRESENTER.absoluteDelta(adapt(previous),adapt(current));
    }
    public static BigDecimal percentDelta(MarketHistory.Point previous,MarketHistory.Point current){
        return PRESENTER.percentDelta(adapt(previous),adapt(current));
    }
    public static String summary(List<MarketHistory.Point> points,int index){return summary(points,index,false);}
    public static String summary(List<MarketHistory.Point> points,int index,boolean includeDate){
        return PRESENTER.summary(adapt(points),index,includeDate,BcbClient.ZONE);
    }
    public static String summary(List<MarketHistory.Point> points,int index,boolean includeDate,boolean includeYear){
        return PRESENTER.summary(adapt(points),index,includeDate,includeYear,BcbClient.ZONE);
    }
    public static int direction(List<MarketHistory.Point> points,int index){return PRESENTER.direction(adapt(points),index);}
    public static String selectedMovement(List<MarketHistory.Point> points,int index,boolean includeDate){
        return PRESENTER.selectedMovement(adapt(points),index,includeDate,BcbClient.ZONE);
    }
    public static String selectedMovement(List<MarketHistory.Point> points,int index,boolean includeDate,boolean includeYear){
        return PRESENTER.selectedMovement(adapt(points),index,includeDate,includeYear,BcbClient.ZONE);
    }
    public static String value(BigDecimal amount){return PRESENTER.value(amount);}

    private static MarketObservation adapt(MarketHistory.Point point){return point==null?null:new MarketObservation(point.ask,point.at);}
    private static List<MarketObservation> adapt(List<MarketHistory.Point> points){
        if(points==null)return null;List<MarketObservation> result=new ArrayList<>(points.size());
        for(MarketHistory.Point point:points)result.add(adapt(point));return result;
    }
}
