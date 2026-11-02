package br.com.cambioclaro.app;
import android.content.*;
import java.util.*;
import java.time.*;
import java.io.IOException;
public final class RateStore {
    private static final Map<FxCurrency,Snapshot> MEMORY=new EnumMap<>(FxCurrency.class);
    private static final class Snapshot {final String raw;final LocalDate day;final List<Rate> rates;Snapshot(String raw,LocalDate day,List<Rate> rates){this.raw=raw;this.day=day;this.rates=Collections.unmodifiableList(new ArrayList<>(rates));}}
    public static SharedPreferences prefs(Context c){return prefs(c,FxCurrency.USD);}
    public static SharedPreferences prefs(Context c,FxCurrency currency){return c.getSharedPreferences("ptax_v2_"+currency.name(),Context.MODE_PRIVATE);}
    public static FxCurrency active(Context c){try{return FxCurrency.valueOf(c.getSharedPreferences("settings",Context.MODE_PRIVATE).getString("currency","USD"));}catch(IllegalArgumentException ex){return FxCurrency.USD;}}
    public static void select(Context c,FxCurrency currency){c.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putString("currency",currency.name()).apply();}
    public static List<Rate> read(Context c){
        return read(c,FxCurrency.USD);
    }
    public static synchronized List<Rate> read(Context c,FxCurrency currency){
        try{
            SharedPreferences p=prefs(c,currency);String raw=p.getString("raw",null);
            if(raw==null||!currency.name().equals(p.getString("currency",null))){MEMORY.remove(currency);return Collections.emptyList();}
            long fetched=p.getLong("fetched",0);if(fetched<=0)return Collections.emptyList();
            LocalDate day=Instant.ofEpochMilli(fetched).atZone(BcbClient.ZONE).toLocalDate();
            if(day.isAfter(BcbClient.today()))return Collections.emptyList();
            Snapshot cached=MEMORY.get(currency);if(cached!=null&&cached.day.equals(day)&&cached.raw.equals(raw))return cached.rates;
            // Validate the original requested range, so a valid offline cache survives midnight.
            Snapshot snapshot=new Snapshot(raw,day,BcbClient.parse(raw,day,currency));MEMORY.put(currency,snapshot);return snapshot.rates;
        }
        catch(Exception ex){return new ArrayList<>();}
    }
    public static synchronized void save(Context c,String raw,long fetched) throws Exception {
        save(c,FxCurrency.USD,raw,fetched);
    }
    public static synchronized void save(Context c,FxCurrency currency,String raw,long fetched) throws Exception {
        List<Rate> fresh=BcbClient.parse(raw,BcbClient.today(),currency);List<Rate> old=read(c,currency);
        if(!old.isEmpty() && fresh.get(fresh.size()-1).at.isBefore(old.get(old.size()-1).at))throw new IOException("A fonte retornou uma publicação mais antiga. Os dados salvos foram preservados.");
        prefs(c,currency).edit().putString("currency",currency.name()).putString("raw",raw).putLong("fetched",fetched).apply();
        MEMORY.put(currency,new Snapshot(raw,BcbClient.today(),fresh));
    }
}
