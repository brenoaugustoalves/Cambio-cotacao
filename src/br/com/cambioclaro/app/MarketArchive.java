package br.com.cambioclaro.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Repository façade for real market observations stored in SQLite. */
public final class MarketArchive {
    public static final int MAX_POINTS=MarketArchiveDb.MAX_POINTS;
    public static final long MAX_AGE_DAYS=MarketArchiveDb.MAX_AGE_DAYS;
    private static final String MIGRATION_PREFS="market_archive_migrations_v1",MIGRATION_KEY="json_to_sqlite_done";
    private static final Object MIGRATION_LOCK=new Object();
    private static volatile MarketArchiveDb instance;
    private static volatile boolean migrationRunning=false;
    private MarketArchive(){}
    private static MarketArchiveDb db(Context c){MarketArchiveDb value=instance;if(value==null)synchronized(MarketArchive.class){value=instance;if(value==null)instance=value=new MarketArchiveDb(c);}return value;}

    /** Imports the 1.7.x JSON archive and today's existing points into the supplied store. */
    public static int migrateLegacy(Context c,MarketArchiveDb target){
        if(c==null||target==null)throw new IllegalArgumentException("Contexto e banco obrigatórios");int imported=0;Instant now=Instant.now();
        for(FxCurrency fx:FxCurrency.values()){
            ArrayList<MarketHistory.Point> points=new ArrayList<>();SharedPreferences old=c.getSharedPreferences("market_archive_v1_"+fx.name(),Context.MODE_PRIVATE);String raw=old.getString("points",null);
            if(raw!=null)try{if(raw.length()>1500000)throw new IOException("Arquivo legado excedeu o limite");JSONArray rows=new JSONArray(raw);if(rows.length()>20000)throw new IOException("Muitos pontos no arquivo legado");for(int i=0;i<rows.length();i++){JSONObject item=rows.getJSONObject(i);long stamp=item.getLong("t");Instant at=stamp>100000000000L?Instant.ofEpochMilli(stamp):Instant.ofEpochSecond(stamp);BigDecimal ask=new BigDecimal(item.getString("a"));if(ask.signum()>0)points.add(new MarketHistory.Point(at,ask));}}catch(Exception malformed){Diagnostics.record(c,"migração do histórico local "+fx.name(),malformed);}
            points.addAll(MarketHistory.read(c,fx));int before=target.count(fx);target.merge(fx,points,now);imported+=Math.max(0,target.count(fx)-before);
        }return imported;
    }
    public static void prepare(Context context){if(context!=null)scheduleMigration(context.getApplicationContext());}
    private static void scheduleMigration(Context c){if(c.getSharedPreferences(MIGRATION_PREFS,Context.MODE_PRIVATE).getBoolean(MIGRATION_KEY,false))return;synchronized(MIGRATION_LOCK){if(migrationRunning)return;migrationRunning=true;new Thread(()->{try{ensureMigrated(c);}catch(RuntimeException ignored){}finally{migrationRunning=false;}},"market-archive-migration").start();}}
    private static void ensureMigrated(Context c){SharedPreferences marker=c.getSharedPreferences(MIGRATION_PREFS,Context.MODE_PRIVATE);if(marker.getBoolean(MIGRATION_KEY,false))return;synchronized(MIGRATION_LOCK){if(marker.getBoolean(MIGRATION_KEY,false))return;try{migrateLegacy(c,db(c));if(!marker.edit().putBoolean(MIGRATION_KEY,true).commit())throw new IllegalStateException("Não foi possível salvar a versão da migração");}catch(RuntimeException error){Diagnostics.record(c,"migração do arquivo local de mercado",error);throw error;}}}

    public static List<MarketHistory.Point> read(Context c,FxCurrency fx){
        if(c==null||fx==null)return Collections.emptyList();if(!c.getSharedPreferences(MIGRATION_PREFS,Context.MODE_PRIVATE).getBoolean(MIGRATION_KEY,false)){scheduleMigration(c);return Collections.emptyList();}try{Instant now=Instant.now();return db(c).read(fx,now.minus(Duration.ofDays(MAX_AGE_DAYS)).toEpochMilli(),now.toEpochMilli()+300000L);}catch(RuntimeException failure){Diagnostics.record(c,"leitura do histórico local "+fx.name(),failure);return Collections.emptyList();}
    }
    public static void merge(Context c,FxCurrency fx,List<MarketHistory.Point> incoming){
        if(c==null||fx==null||incoming==null||incoming.isEmpty())return;try{ensureMigrated(c);db(c).merge(fx,incoming,Instant.now());}catch(RuntimeException failure){Diagnostics.record(c,"gravação do histórico local "+fx.name(),failure);}
    }
    public static void mergeDaily(Context c,FxCurrency fx,List<MarketHistory.Point> incoming){
        if(c==null||fx==null||incoming==null||incoming.isEmpty())return;try{db(c).mergeDaily(fx,incoming,Instant.now());}catch(RuntimeException failure){Diagnostics.record(c,"gravação do histórico diário "+fx.name(),failure);}
    }
    public static List<MarketHistory.Point> dailyPeriod(Context c,FxCurrency fx,int observations){
        if(observations<1||observations>365)throw new IllegalArgumentException("Período inválido");if(c==null||fx==null)return Collections.emptyList();try{Instant now=Instant.now();long calendarWindow=Math.min(MAX_AGE_DAYS,observations*2L+7L);List<MarketHistory.Point> values=db(c).readDaily(fx,now.minus(Duration.ofDays(calendarWindow)).toEpochMilli(),now.toEpochMilli()+300000L);if(values.size()<=observations)return values;return new ArrayList<>(values.subList(values.size()-observations,values.size()));}catch(RuntimeException failure){Diagnostics.record(c,"consulta do histórico diário "+fx.name(),failure);return Collections.emptyList();}
    }
    public static void mergeResponse(Context c,FxCurrency fx,String raw)throws Exception{merge(c,fx,MarketHistory.parseAll(raw,fx,Instant.now()));}
    public static List<MarketHistory.Point> period(Context c,FxCurrency fx,int days){
        if(days<1||days>MAX_AGE_DAYS)throw new IllegalArgumentException("Período inválido");if(c==null||fx==null)return Collections.emptyList();if(!c.getSharedPreferences(MIGRATION_PREFS,Context.MODE_PRIVATE).getBoolean(MIGRATION_KEY,false)){scheduleMigration(c);return Collections.emptyList();}try{Instant now=Instant.now();return db(c).read(fx,now.minus(Duration.ofDays(days)).toEpochMilli(),now.toEpochMilli()+300000L);}catch(RuntimeException failure){Diagnostics.record(c,"consulta do histórico local "+fx.name(),failure);return Collections.emptyList();}
    }
}
