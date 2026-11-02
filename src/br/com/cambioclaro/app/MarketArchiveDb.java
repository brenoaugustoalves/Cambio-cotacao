package br.com.cambioclaro.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Transactional local store for timestamped market observations. */
public final class MarketArchiveDb extends SQLiteOpenHelper {
    public static final int MAX_POINTS=5000;
    public static final long MAX_AGE_DAYS=365;
    private static final String TABLE="market_observations";
    private static final String DAILY_TABLE="market_daily_observations";
    private static final java.util.regex.Pattern PRICE=java.util.regex.Pattern.compile("[0-9]{1,8}(\\.[0-9]{1,8})?");
    public MarketArchiveDb(Context context){this(context,"market_archive.db");}
    /** Named databases let instrumentation exercise the real schema without touching user data. */
    public MarketArchiveDb(Context context,String databaseName){super(context.getApplicationContext(),databaseName,null,2);if(databaseName==null||databaseName.trim().isEmpty())throw new IllegalArgumentException("Nome de banco obrigatório");}
    @Override public void onCreate(SQLiteDatabase db){
        createTable(db,TABLE,"market_observations_range");createTable(db,DAILY_TABLE,"market_daily_observations_range");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){if(oldVersion<2)createTable(db,DAILY_TABLE,"market_daily_observations_range");}
    private static void createTable(SQLiteDatabase db,String table,String index){db.execSQL("CREATE TABLE "+table+" (currency TEXT NOT NULL, observed_at_ms INTEGER NOT NULL, ask TEXT NOT NULL, PRIMARY KEY(currency, observed_at_ms))");db.execSQL("CREATE INDEX "+index+" ON "+table+" (currency, observed_at_ms)");}
    public synchronized void merge(FxCurrency currency,List<MarketHistory.Point> points,Instant now){
        mergeInto(TABLE,currency,points,now);
    }
    public synchronized void mergeDaily(FxCurrency currency,List<MarketHistory.Point> points,Instant now){mergeInto(DAILY_TABLE,currency,points,now);}
    private void mergeInto(String table,FxCurrency currency,List<MarketHistory.Point> points,Instant now){
        if(currency==null||points==null||points.isEmpty())return;long current=now.toEpochMilli(),cutoff=now.minusSeconds(MAX_AGE_DAYS*86400L).toEpochMilli();SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            db.delete(table,"currency=? AND observed_at_ms<?",new String[]{currency.name(),Long.toString(cutoff)});
            for(MarketHistory.Point p:points){if(p==null||p.at==null||!valid(p.ask))continue;long at=p.at.toEpochMilli();if(at<cutoff||at>current+300000L)continue;ContentValues row=new ContentValues();row.put("currency",currency.name());row.put("observed_at_ms",at);row.put("ask",p.ask.toPlainString());db.insertWithOnConflict(table,null,row,SQLiteDatabase.CONFLICT_REPLACE);}
            db.execSQL("DELETE FROM "+table+" WHERE currency=? AND observed_at_ms NOT IN (SELECT observed_at_ms FROM "+table+" WHERE currency=? ORDER BY observed_at_ms DESC LIMIT ?)",new Object[]{currency.name(),currency.name(),MAX_POINTS});db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    public synchronized List<MarketHistory.Point> read(FxCurrency currency,long fromMillis,long toMillis){
        return readFrom(TABLE,currency,fromMillis,toMillis);
    }
    public synchronized List<MarketHistory.Point> readDaily(FxCurrency currency,long fromMillis,long toMillis){return readFrom(DAILY_TABLE,currency,fromMillis,toMillis);}
    private List<MarketHistory.Point> readFrom(String table,FxCurrency currency,long fromMillis,long toMillis){
        if(currency==null)return Collections.emptyList();ArrayList<MarketHistory.Point> result=new ArrayList<>();SQLiteDatabase db=getReadableDatabase();
        try(Cursor cursor=db.query(table,new String[]{"observed_at_ms","ask"},"currency=? AND observed_at_ms>=? AND observed_at_ms<=?",new String[]{currency.name(),Long.toString(fromMillis),Long.toString(toMillis)},null,null,"observed_at_ms ASC",Integer.toString(MAX_POINTS))){
            while(cursor.moveToNext()){try{long at=cursor.getLong(0);BigDecimal ask=new BigDecimal(cursor.getString(1));if(valid(ask))result.add(new MarketHistory.Point(Instant.ofEpochMilli(at),ask));}catch(RuntimeException malformed){/* Ignore only the damaged row; keep valid observations. */}}
        }return result;
    }
    private static boolean valid(BigDecimal ask){if(ask==null||ask.signum()<=0||ask.compareTo(new BigDecimal("1000000"))>0||ask.precision()>16||ask.scale()<0||ask.scale()>8)return false;String text=ask.toPlainString();return text.length()<=17&&PRICE.matcher(text).matches();}
    public synchronized int count(FxCurrency currency){return count(TABLE,currency);}
    public synchronized int countDaily(FxCurrency currency){return count(DAILY_TABLE,currency);}
    private int count(String table,FxCurrency currency){if(currency==null)return 0;try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table+" WHERE currency=?",new String[]{currency.name()})){return c.moveToFirst()?c.getInt(0):0;}}
}
