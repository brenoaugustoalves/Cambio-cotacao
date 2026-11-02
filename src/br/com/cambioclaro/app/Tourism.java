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

/** USD tourism is a separate market product (USD/BRLT), never a PTAX currency. */
public final class Tourism {
    private static final String LATEST="https://economia.awesomeapi.com.br/json/last/USD-BRLT";
    private static final String HISTORY="https://economia.awesomeapi.com.br/USD-BRLT/100";
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("tourism_v1",Context.MODE_PRIVATE);}
    public static String latestUrl(){return LATEST;}
    public static String historyUrl(){return HISTORY;}
    private static String fetch(String url,int limit)throws IOException{HttpsURLConnection c=(HttpsURLConnection)new URL(url).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");
        try{if(c.getResponseCode()!=200)throw new IOException("Cotação turismo indisponível: HTTP "+c.getResponseCode());ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Consulta cancelada");if(out.size()+n>limit)throw new IOException("Resposta turismo maior que o esperado");out.write(b,0,n);}}return new String(out.toByteArray(),StandardCharsets.UTF_8);}finally{c.disconnect();}}
    public static String fetchLatest()throws IOException{return fetch(LATEST,16384);}
    public static String fetchHistory()throws IOException{return fetch(HISTORY,131072);}
    public static MarketClient.Quote parseLatest(String raw,Instant now)throws Exception{if(raw==null||raw.length()>16384)throw new IOException("Resposta turismo fora do limite");MarketClient.checkDepth(raw);JSONObject item=new JSONObject(raw).getJSONObject("USDBRLT");if(!"USD".equals(item.getString("code"))||!"BRLT".equals(item.getString("codein")))throw new IOException("Par turismo inválido");String bid=item.getString("bid"),ask=item.getString("ask"),stamp=item.getString("timestamp");if(!bid.matches("[0-9]{1,8}(\\.[0-9]{1,8})?")||!ask.matches("[0-9]{1,8}(\\.[0-9]{1,8})?")||!stamp.matches("[0-9]{10}"))throw new IOException("Formato turismo inválido");BigDecimal b=new BigDecimal(bid),a=new BigDecimal(ask);Instant at=Instant.ofEpochSecond(Long.parseLong(stamp));if(b.signum()<=0||a.compareTo(b)<0||a.compareTo(new BigDecimal("1000000"))>0||at.isAfter(now.plusSeconds(300))||at.isBefore(Instant.parse("2020-01-01T00:00:00Z")))throw new IOException("Cotação turismo inválida");return new MarketClient.Quote(a,at);}
    public static List<MarketHistory.Point> parseHistory(String raw,LocalDate day,Instant now)throws Exception{if(raw==null||raw.length()>131072)throw new IOException("Histórico turismo fora do limite");MarketClient.checkDepth(raw);JSONArray rows=new JSONArray(raw);if(rows.length()>100)throw new IOException("Histórico turismo excedeu limite");TreeMap<Instant,MarketHistory.Point> result=new TreeMap<>();
        for(int i=0;i<rows.length();i++){JSONObject item=rows.getJSONObject(i);if((i==0&&(!item.has("code")||!item.has("codein")))||(item.has("code")&&!"USD".equals(item.getString("code")))||(item.has("codein")&&!"BRLT".equals(item.getString("codein"))))throw new IOException("Par turismo inesperado");String price=item.getString("ask"),stamp=item.getString("timestamp");if(!price.matches("[0-9]{1,8}(\\.[0-9]{1,8})?")||!stamp.matches("[0-9]{10}([0-9]{3})?"))throw new IOException("Ponto turismo inválido");BigDecimal ask=new BigDecimal(price);Instant at=stamp.length()==13?Instant.ofEpochMilli(Long.parseLong(stamp)):Instant.ofEpochSecond(Long.parseLong(stamp));if(ask.signum()<=0||ask.compareTo(new BigDecimal("1000000"))>0||at.isAfter(now.plusSeconds(300)))throw new IOException("Ponto turismo inválido");if(at.atZone(BcbClient.ZONE).toLocalDate().equals(day))result.put(at,new MarketHistory.Point(at,ask));}
        return new ArrayList<>(result.values());}
    public static void saveLatest(Context c,String raw)throws Exception{MarketClient.Quote fresh=parseLatest(raw,Instant.now());MarketClient.Quote old=readLatest(c);if(old!=null&&fresh.at.isBefore(old.at))return;prefs(c).edit().putString("latest",raw).putLong("checked",System.currentTimeMillis()).apply();}
    public static MarketClient.Quote readLatest(Context c){String raw=prefs(c).getString("latest",null);if(raw==null)return null;try{return parseLatest(raw,Instant.now());}catch(Exception ex){return null;}}
    public static List<MarketHistory.Point> readHistory(Context c){SharedPreferences p=prefs(c);if(!BcbClient.today().toString().equals(p.getString("day","")))return Collections.emptyList();try{JSONArray arr=new JSONArray(p.getString("points","[]"));if(arr.length()>1000)return Collections.emptyList();ArrayList<MarketHistory.Point> result=new ArrayList<>();Instant prev=Instant.EPOCH;for(int i=0;i<arr.length();i++){JSONObject item=arr.getJSONObject(i);Instant at=Instant.ofEpochSecond(item.getLong("t"));BigDecimal ask=new BigDecimal(item.getString("a"));if(!at.isAfter(prev)||!at.atZone(BcbClient.ZONE).toLocalDate().equals(BcbClient.today())||ask.signum()<=0)throw new IOException("Cache turismo inválido");result.add(new MarketHistory.Point(at,ask));prev=at;}return result;}catch(Exception ex){return Collections.emptyList();}}
    public static void saveHistory(Context c,String raw)throws Exception{LocalDate day=BcbClient.today();TreeMap<Instant,MarketHistory.Point> merged=new TreeMap<>();for(MarketHistory.Point p:readHistory(c))merged.put(p.at,p);for(MarketHistory.Point p:parseHistory(raw,day,Instant.now()))merged.put(p.at,p);while(merged.size()>1000)merged.pollFirstEntry();JSONArray arr=new JSONArray();for(MarketHistory.Point p:merged.values()){JSONObject o=new JSONObject();o.put("t",p.at.getEpochSecond());o.put("a",p.ask.toPlainString());arr.put(o);}prefs(c).edit().putString("day",day.toString()).putString("points",arr.toString()).putLong("history_checked",System.currentTimeMillis()).apply();}
    public static MarketClient.Quote latest(Context c){MarketClient.Quote quote=readLatest(c);List<MarketHistory.Point> points=readHistory(c);if(points.isEmpty())return quote;MarketHistory.Point p=points.get(points.size()-1);return quote==null||p.at.isAfter(quote.at)?new MarketClient.Quote(p.ask,p.at):quote;}
}
