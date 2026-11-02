package br.com.cambioclaro.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

/** Actual one-minute market observations for the selected pair; no interpolation. */
public final class MarketHistory {
    public static final class Point {
        public final Instant at;public final BigDecimal ask;
        Point(Instant at,BigDecimal ask){this.at=at;this.ask=ask;}
    }
    public static String url(FxCurrency currency){if(currency==null)throw new IllegalArgumentException("Moeda obrigatória");return "https://economia.awesomeapi.com.br/"+currency.name()+"-BRL/100";}
    public static String dailyUrl(FxCurrency currency,int days){if(currency==null||days<1||days>360)throw new IllegalArgumentException("Moeda ou período inválido");return "https://economia.awesomeapi.com.br/json/daily/"+currency.name()+"-BRL/"+days;}
    public static String fetch(FxCurrency currency)throws IOException{
        HttpsURLConnection c=(HttpsURLConnection)new URL(url(currency)).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");
        try{if(c.getResponseCode()!=200)throw new IOException("Histórico de mercado indisponível: HTTP "+c.getResponseCode());ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Consulta cancelada");if(out.size()+n>131072)throw new IOException("Histórico de mercado maior que o esperado");out.write(b,0,n);}}return new String(out.toByteArray(),StandardCharsets.UTF_8);}finally{c.disconnect();}
    }
    public static String fetchDaily(FxCurrency currency,int days)throws IOException{
        HttpsURLConnection c=(HttpsURLConnection)new URL(dailyUrl(currency,days)).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");
        try{if(c.getResponseCode()!=200)throw new IOException("Fechamentos diários indisponíveis: HTTP "+c.getResponseCode());ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Consulta cancelada");if(out.size()+n>512000)throw new IOException("Histórico diário excedeu o limite");out.write(b,0,n);}}return new String(out.toByteArray(),StandardCharsets.UTF_8);}finally{c.disconnect();}
    }
    public static List<Point> parse(String raw,FxCurrency currency,LocalDate day,Instant now)throws Exception{
        return parseRange(raw,currency,day,now,100);
    }
    /** All timestamped observations returned by the provider, without filling gaps. */
    public static List<Point> parseAll(String raw,FxCurrency currency,Instant now)throws Exception{return parseRange(raw,currency,null,now,100);}
    /** Daily provider closes, kept separate from one-minute observations. */
    public static List<Point> parseDaily(String raw,FxCurrency currency,Instant now)throws Exception{return parseRange(raw,currency,null,now,360);}
    private static List<Point> parseRange(String raw,FxCurrency currency,LocalDate day,Instant now,int maxRows)throws Exception{
        int maxChars=maxRows>100?512000:131072;if(raw==null||raw.length()>maxChars)throw new IOException("Histórico fora do limite");MarketClient.checkDepth(raw);JSONArray rows=new JSONArray(raw);if(rows.length()>maxRows)throw new IOException("Histórico acima do limite da fonte");TreeMap<Instant,Point> result=new TreeMap<>();
        for(int i=0;i<rows.length();i++){JSONObject item=rows.getJSONObject(i);if((i==0&&(!item.has("code")||!item.has("codein")))||(item.has("code")&&!currency.name().equals(item.getString("code")))||(item.has("codein")&&!"BRL".equals(item.getString("codein"))))throw new IOException("Par inesperado no histórico");String price=item.getString("ask"),time=item.getString("timestamp");if(!price.matches("[0-9]{1,8}(\\.[0-9]{1,8})?")||!time.matches("[0-9]{10}([0-9]{3})?"))throw new IOException("Ponto de mercado inválido");BigDecimal ask=new BigDecimal(price);if(ask.signum()<=0||ask.compareTo(new BigDecimal("1000000"))>0)throw new IOException("Preço inválido");Instant at=time.length()==13?Instant.ofEpochMilli(Long.parseLong(time)):Instant.ofEpochSecond(Long.parseLong(time));if(at.isAfter(now.plusSeconds(300)))throw new IOException("Ponto futuro");if(day==null||at.atZone(BcbClient.ZONE).toLocalDate().equals(day))result.put(at,new Point(at,ask));}
        return Collections.unmodifiableList(new ArrayList<>(result.values()));
    }
    private static SharedPreferences prefs(Context c,FxCurrency currency){return c.getSharedPreferences("market_history_v1_"+currency.name(),Context.MODE_PRIVATE);}
    public static List<Point> read(Context c,FxCurrency currency){
        SharedPreferences p=prefs(c,currency);if(!BcbClient.today().toString().equals(p.getString("day","")))return Collections.emptyList();String raw=p.getString("points",null);if(raw==null)return Collections.emptyList();
        try{JSONArray arr=new JSONArray(raw);if(arr.length()>1000)return Collections.emptyList();ArrayList<Point> points=new ArrayList<>();Instant last=Instant.EPOCH;for(int i=0;i<arr.length();i++){JSONObject obj=arr.getJSONObject(i);Instant at=Instant.ofEpochSecond(obj.getLong("t"));BigDecimal ask=new BigDecimal(obj.getString("a"));if(!at.isAfter(last)||!at.atZone(BcbClient.ZONE).toLocalDate().equals(BcbClient.today())||ask.signum()<=0)throw new IOException("Cache de gráfico inválido");points.add(new Point(at,ask));last=at;}return points;}catch(Exception ex){return Collections.emptyList();}
    }
    public static void save(Context c,FxCurrency currency,String raw)throws Exception{
        LocalDate today=BcbClient.today();List<Point> fresh=parse(raw,currency,today,Instant.now());TreeMap<Instant,Point> merged=new TreeMap<>();for(Point p:read(c,currency))merged.put(p.at,p);for(Point p:fresh)merged.put(p.at,p);while(merged.size()>1000)merged.pollFirstEntry();JSONArray arr=new JSONArray();for(Point p:merged.values()){JSONObject o=new JSONObject();o.put("t",p.at.getEpochSecond());o.put("a",p.ask.toPlainString());arr.put(o);}prefs(c,currency).edit().putString("day",today.toString()).putString("points",arr.toString()).putLong("checked",System.currentTimeMillis()).apply();
    }
}
