package br.com.cambioclaro.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Device-local archive of actual AwesomeAPI observations; no interpolation or synthetic points. */
public final class MarketArchive {
    private static final int MAX_POINTS=5000;
    private static final long MAX_AGE_DAYS=365;
    private static SharedPreferences prefs(Context c,FxCurrency fx){return c.getSharedPreferences("market_archive_v1_"+fx.name(),Context.MODE_PRIVATE);}
    public static List<MarketHistory.Point> read(Context c,FxCurrency fx){
        String raw=prefs(c,fx).getString("points",null);if(raw==null)return Collections.emptyList();
        try{JSONArray arr=new JSONArray(raw);if(arr.length()>MAX_POINTS)return Collections.emptyList();TreeMap<Instant,MarketHistory.Point> sorted=new TreeMap<>();Instant last=Instant.EPOCH;for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);long stamp=o.getLong("t");Instant at=stamp>100000000000L?Instant.ofEpochMilli(stamp):Instant.ofEpochSecond(stamp);java.math.BigDecimal ask=new java.math.BigDecimal(o.getString("a"));if(!at.isAfter(last)||ask.signum()<=0)throw new IOException("Arquivo de mercado inválido");sorted.put(at,new MarketHistory.Point(at,ask));last=at;}return new ArrayList<>(sorted.values());}catch(Exception invalid){return Collections.emptyList();}
    }
    public static synchronized void merge(Context c,FxCurrency fx,List<MarketHistory.Point> incoming){
        if(incoming==null||incoming.isEmpty())return;TreeMap<Instant,MarketHistory.Point> all=new TreeMap<>();for(MarketHistory.Point p:read(c,fx))all.put(p.at,p);Instant cutoff=Instant.now().minus(Duration.ofDays(MAX_AGE_DAYS));
        for(MarketHistory.Point p:incoming)if(p!=null&&!p.at.isAfter(Instant.now().plusSeconds(300))&&!p.at.isBefore(cutoff)&&p.ask!=null&&p.ask.signum()>0)all.put(p.at,p);
        while(all.size()>MAX_POINTS)all.pollFirstEntry();JSONArray arr=new JSONArray();for(MarketHistory.Point p:all.values()){JSONObject o=new JSONObject();try{o.put("t",p.at.toEpochMilli());o.put("a",p.ask.toPlainString());arr.put(o);}catch(Exception ignored){}}
        prefs(c,fx).edit().putString("points",arr.toString()).putLong("updated",System.currentTimeMillis()).apply();
    }
    public static void mergeResponse(Context c,FxCurrency fx,String raw)throws Exception{merge(c,fx,MarketHistory.parseAll(raw,fx,Instant.now()));}
    public static List<MarketHistory.Point> period(Context c,FxCurrency fx,int days){if(days<1||days>365)throw new IllegalArgumentException("Período inválido");Instant cutoff=Instant.now().minus(Duration.ofDays(days));ArrayList<MarketHistory.Point> result=new ArrayList<>();for(MarketHistory.Point p:read(c,fx))if(!p.at.isBefore(cutoff))result.add(p);return result;}
}
