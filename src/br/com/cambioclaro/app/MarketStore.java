package br.com.cambioclaro.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.Instant;
import java.util.*;

public final class MarketStore {
    private static Map<FxCurrency,MarketClient.Quote> memory;
    private static String lastRaw;
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("market_v1",Context.MODE_PRIVATE);}
    public static synchronized Map<FxCurrency,MarketClient.Quote> read(Context c){
        String raw=prefs(c).getString("raw",null);
        if(raw==null)return Collections.emptyMap();
        if(raw.equals(lastRaw)&&memory!=null)return memory;
        try{memory=MarketClient.parse(raw,Instant.now());lastRaw=raw;return memory;}
        catch(Exception ex){return Collections.emptyMap();}
    }
    public static synchronized void save(Context c,String raw) throws Exception {
        Map<FxCurrency,MarketClient.Quote> next=MarketClient.parse(raw,Instant.now());
        Map<FxCurrency,MarketClient.Quote> old=read(c);
        org.json.JSONObject updated=new org.json.JSONObject(raw),previous=lastRaw==null?null:new org.json.JSONObject(lastRaw);
        for(FxCurrency currency:FxCurrency.values())if(old.containsKey(currency)&&next.get(currency).at.isBefore(old.get(currency).at)&&previous!=null)updated.put(currency.name()+"BRL",previous.getJSONObject(currency.name()+"BRL"));
        String merged=updated.toString();memory=MarketClient.parse(merged,Instant.now());lastRaw=merged;
        prefs(c).edit().putString("raw",merged).putLong("checked",System.currentTimeMillis()).apply();
    }
    public static synchronized void saveOne(Context c,FxCurrency currency,String raw)throws Exception{
        MarketClient.Quote fresh=MarketClient.parseOne(raw,currency,Instant.now());MarketClient.Quote previous=readOne(c,currency);
        if(previous!=null&&fresh.at.isBefore(previous.at))return;
        prefs(c).edit().putString("single_"+currency.name(),raw).putLong("single_checked_"+currency.name(),System.currentTimeMillis()).apply();
    }
    public static MarketClient.Quote readOne(Context c,FxCurrency currency){String raw=prefs(c).getString("single_"+currency.name(),null);if(raw==null)return null;try{return MarketClient.parseOne(raw,currency,Instant.now());}catch(Exception ex){return null;}}
    public static long checked(Context c){return prefs(c).getLong("checked",0);}
    public static MarketClient.Quote snapshot(Context c,FxCurrency currency){MarketClient.Quote batch=read(c).get(currency),single=readOne(c,currency);return single!=null&&(batch==null||single.at.isAfter(batch.at))?single:batch;}
    public static MarketClient.Quote latest(Context c,FxCurrency currency){
        MarketClient.Quote snapshot=snapshot(c,currency);List<MarketHistory.Point> points=MarketHistory.read(c,currency);
        if(points.isEmpty())return snapshot;MarketHistory.Point point=points.get(points.size()-1);
        return snapshot==null||point.at.isAfter(snapshot.at)?new MarketClient.Quote(point.ask,point.at):snapshot;
    }
}
