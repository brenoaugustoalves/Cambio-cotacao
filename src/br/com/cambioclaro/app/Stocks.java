package br.com.cambioclaro.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

/** B3 informational quotes via brapi. Index access goes through an HTTPS backend. */
public final class Stocks {
    public static final String INFO="https://brapi.dev/docs/acoes";
    public static final String[] DEMO={"PETR4","VALE3","ITUB4","MGLU3"};
    private static final String DEMO_URL="https://brapi.dev/api/v2/stocks/quote?symbols=PETR4,VALE3,ITUB4,MGLU3";
    private static volatile long demoCooldown,indexCooldown;
    public static final class Quote {public final String symbol,time;public final BigDecimal price,change;Quote(String symbol,BigDecimal price,BigDecimal change,String time){this.symbol=symbol;this.price=price;this.change=change;this.time=time;}}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("stocks_v1",Context.MODE_PRIVATE);}
    public static String demoUrl(){return DEMO_URL;}
    public static void clearLegacyToken(Context c){c.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().remove("brapi_token").apply();}
    public static String backend(Context c){String value=c.getSharedPreferences("settings",Context.MODE_PRIVATE).getString("brapi_backend_url","");return value.startsWith("https://")?value:"";}
    public static void backend(Context c,String value){String url=value==null?"":value.trim();if(!url.isEmpty()){
        try{URI u=URI.create(url);String path=u.getRawPath();if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||u.getPort()>65535||!(path==null||path.isEmpty()||"/".equals(path)))throw new IllegalArgumentException();url="https://"+u.getRawAuthority().toLowerCase(Locale.ROOT);}
        catch(Exception ex){throw new IllegalArgumentException("Informe somente o endereço HTTPS do seu servidor, sem caminho ou chave.");}
    }c.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putString("brapi_backend_url",url).apply();}
    public static String indexUrl(Context c){String base=backend(c);return base.isEmpty()?"":base+"/v1/stocks/quote?symbols=%5EBVSP";}
    private static String fetch(String url,boolean index)throws IOException{if(url.isEmpty())throw new IOException("Servidor HTTPS do Ibovespa não configurado.");long now=System.currentTimeMillis(),until=index?indexCooldown:demoCooldown;if(now<until)throw new IOException("Limite da fonte. Aguarde antes de consultar novamente.");HttpsURLConnection c=(HttpsURLConnection)new URL(url).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");
        try{int status=c.getResponseCode();if(status!=200){if(status==429){long wait=retryAfter(c.getHeaderField("Retry-After"));if(index)indexCooldown=now+wait;else demoCooldown=now+wait;throw new IOException("Limite da fonte. Aguarde "+Math.max(1,wait/1000)+" segundos.");}if(status==401)throw new IOException("Acesso negado. Confira a configuração do servidor.");if(status==404)throw new IOException("Cotação não encontrada na fonte.");if(status==403)throw new IOException("Plano da fonte sem acesso a este ativo.");throw new IOException("Fonte da bolsa respondeu HTTP "+status);}ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Consulta cancelada");if(out.size()+n>262144)throw new IOException("Resposta da bolsa maior que o esperado");out.write(b,0,n);}}return new String(out.toByteArray(),StandardCharsets.UTF_8);}finally{c.disconnect();}}
    private static long retryAfter(String header){try{long seconds=Long.parseLong(header);return Math.max(1000,Math.min(86400000,Math.min(86400,seconds)*1000));}catch(Exception ignored){}try{long difference=java.time.ZonedDateTime.parse(header,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-System.currentTimeMillis();return Math.max(1000,Math.min(86400000,difference));}catch(Exception ignored){return 60000;}}
    public static String fetchDemo()throws IOException{return fetch(DEMO_URL,false);}
    public static String fetchIndex(Context c)throws IOException{return fetch(indexUrl(c),true);}
    public static Map<String,Quote> parse(String raw,boolean index)throws Exception{if(raw==null||raw.length()>262144)throw new IOException("Resposta da bolsa fora do limite");MarketClient.checkDepth(raw);JSONArray rows=new JSONObject(raw).getJSONArray("results");if(rows.length()>(index?1:4))throw new IOException("Ativos inesperados na resposta");Map<String,Quote> result=new LinkedHashMap<>();Set<String> allowed=new HashSet<>(Arrays.asList(index?new String[]{"^BVSP"}:DEMO));
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);String symbol=row.getString("symbol");if(!allowed.contains(symbol)||!symbol.equals(row.getString("requestedSymbol"))||result.containsKey(symbol))throw new IOException("Símbolo inesperado");JSONObject data=row.optJSONObject("data");if(data==null)continue;if(!index&&!"BRL".equals(data.optString("currency")))throw new IOException("Moeda de ação inesperada");Object p=data.opt("regularMarketPrice"),v=data.opt("regularMarketChangePercent");if(!(p instanceof Number)||!(v instanceof Number)||data.isNull("regularMarketTime"))continue;BigDecimal price=new BigDecimal(p.toString()),change=new BigDecimal(v.toString());if(price.signum()<=0||price.compareTo(new BigDecimal("1000000000"))>0||change.abs().compareTo(new BigDecimal("1000"))>0)throw new IOException("Valor de bolsa inválido");String stamp=data.optString("regularMarketTime","");if(stamp.length()>64||stamp.isEmpty())continue;result.put(symbol,new Quote(symbol,price,change,stamp));}
        return result;}
    public static String displayTime(String source){try{return Instant.parse(source).atZone(BcbClient.ZONE).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))+" (Brasília)";}catch(Exception ignored){}try{LocalDateTime value=LocalDateTime.parse(source,DateTimeFormatter.ofPattern("MM/dd/uuuu HH:mm:ss",Locale.US));return value.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))+" (fuso da fonte)";}catch(Exception ignored){}return "horário não interpretado";}
    public static boolean stale(String source,Instant now){try{Instant at=Instant.parse(source);return at.isBefore(now.minusSeconds(45*60L))||at.isAfter(now.plusSeconds(300));}catch(Exception ignored){}try{LocalDateTime local=LocalDateTime.parse(source,DateTimeFormatter.ofPattern("MM/dd/uuuu HH:mm:ss",Locale.US));Instant at=local.atZone(BcbClient.ZONE).toInstant();return at.isBefore(now.minusSeconds(45*60L))||at.isAfter(now.plusSeconds(300));}catch(Exception ignored){return false;}}
    public static void save(Context c,String raw,boolean index)throws Exception{if(parse(raw,index).isEmpty())throw new IOException("Nenhuma cotação válida na resposta");prefs(c).edit().putString(index?"index":"demo",raw).putLong(index?"index_checked":"demo_checked",System.currentTimeMillis()).apply();}
    public static Map<String,Quote> read(Context c,boolean index){String raw=prefs(c).getString(index?"index":"demo",null);if(raw==null)return Collections.emptyMap();try{return parse(raw,index);}catch(Exception ex){return Collections.emptyMap();}}
    public static long checked(Context c,boolean index){return prefs(c).getLong(index?"index_checked":"demo_checked",0);}
}
