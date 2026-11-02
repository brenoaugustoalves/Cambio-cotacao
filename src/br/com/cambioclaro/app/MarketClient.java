package br.com.cambioclaro.app;

import org.json.JSONObject;
import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

/** Public market observations. Never synthesize a missing pair or timestamp. */
public final class MarketClient {
    public static final String INFO="https://docs.awesomeapi.com.br/api-de-moedas";
    private static final String ORIGIN="https://economia.awesomeapi.com.br/json/last/";
    public static final class Quote {
        public final BigDecimal ask;
        public final Instant at;
        public Quote(BigDecimal ask,Instant at){this.ask=ask;this.at=at;}
    }
    public static String url(){
        StringBuilder b=new StringBuilder(ORIGIN);
        for(FxCurrency currency:FxCurrency.values()){if(b.length()>ORIGIN.length())b.append(',');b.append(currency.name()).append("-BRL");}
        return b.toString();
    }
    public static String singleUrl(FxCurrency currency){if(currency==null)throw new IllegalArgumentException("Moeda obrigatória");return ORIGIN+currency.name()+"-BRL";}
    public static String fetch() throws IOException {return fetchUrl(url());}
    public static String fetchOne(FxCurrency currency) throws IOException {return fetchUrl(singleUrl(currency));}
    private static String fetchUrl(String source) throws IOException {
        HttpsURLConnection c=(HttpsURLConnection)new URL(source).openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);
        c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","CambioClaro/1.2 (Android; market quotes)");
        try{
            if(c.getResponseCode()!=200)throw new IOException("Fonte de mercado respondeu HTTP "+c.getResponseCode());
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()){
                byte[] buf=new byte[4096];int n;
                while((n=in.read(buf))!=-1){
                    if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Consulta cancelada");
                    if(out.size()+n>65536)throw new IOException("Resposta de mercado maior que o esperado");
                    out.write(buf,0,n);
                }
            }
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }finally{c.disconnect();}
    }
    public static Map<FxCurrency,Quote> parse(String json,Instant now) throws Exception {
        if(json==null||json.length()>65536)throw new IOException("Resposta de mercado fora do limite");
        checkDepth(json);
        JSONObject root=new JSONObject(json);
        EnumMap<FxCurrency,Quote> result=new EnumMap<>(FxCurrency.class);
        for(FxCurrency currency:FxCurrency.values()){
            JSONObject item=root.getJSONObject(currency.name()+"BRL");
            result.put(currency,parseItem(item,currency,now));
        }
        return Collections.unmodifiableMap(result);
    }
    public static Quote parseOne(String json,FxCurrency currency,Instant now)throws Exception{
        if(currency==null||json==null||json.length()>65536)throw new IOException("Resposta individual fora do limite");
        checkDepth(json);JSONObject root=new JSONObject(json);if(root.length()!=1)throw new IOException("Resposta individual inesperada");
        return parseItem(root.getJSONObject(currency.name()+"BRL"),currency,now);
    }
    private static Quote parseItem(JSONObject item,FxCurrency currency,Instant now)throws Exception{
            if(!currency.name().equals(item.getString("code"))||!"BRL".equals(item.getString("codein")))throw new IOException("Par de moedas inesperado");
            String bidText=item.getString("bid"),askText=item.getString("ask"),timeText=item.getString("timestamp");
            if(!bidText.matches("[0-9]{1,8}(\\.[0-9]{1,8})?")||!askText.matches("[0-9]{1,8}(\\.[0-9]{1,8})?")||!timeText.matches("[0-9]{10}"))throw new IOException("Formato de mercado inesperado");
            BigDecimal bid=new BigDecimal(bidText),ask=new BigDecimal(askText);
            Instant at=Instant.ofEpochSecond(Long.parseLong(timeText));
            if(bid.signum()<=0||ask.signum()<=0||bid.compareTo(ask)>0||ask.compareTo(new BigDecimal("1000000"))>0||at.isAfter(now.plusSeconds(300))||at.isBefore(Instant.parse("2020-01-01T00:00:00Z")))throw new IOException("Cotação de mercado inválida");
            return new Quote(ask,at);
    }
    static void checkDepth(String raw)throws IOException{boolean quoted=false,escaped=false;int depth=0;for(int i=0;i<raw.length();i++){char ch=raw.charAt(i);if(quoted){if(escaped)escaped=false;else if(ch=='\\')escaped=true;else if(ch=='"')quoted=false;}else if(ch=='"')quoted=true;else if(ch=='{'||ch=='['){if(++depth>12)throw new IOException("JSON profundo demais");}else if(ch=='}'||ch==']'){if(--depth<0)throw new IOException("JSON inválido");}}if(quoted||depth!=0)throw new IOException("JSON incompleto");}
}
