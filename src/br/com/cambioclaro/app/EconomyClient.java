package br.com.cambioclaro.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

/** Official BCB macro series and Focus market expectations, with bounded HTTPS and validation. */
public final class EconomyClient {
    public static final String SELIC_SOURCE="https://api.bcb.gov.br/dados/serie/bcdata.sgs.432/dados/ultimos/1?formato=json";
    public static final String IPCA_SOURCE="https://api.bcb.gov.br/dados/serie/bcdata.sgs.433/dados/ultimos/1?formato=json";
    public static final String FOCUS_PAGE="https://www.bcb.gov.br/publicacoes/focus";
    private static final String FOCUS_BASE="https://olinda.bcb.gov.br/olinda/servico/Expectativas/versao/v1/odata/ExpectativasMercadoAnuais";
    private static final int MAX_BYTES=262144;
    private static final long CACHE_TTL=6L*60*60*1000;
    private static final DateTimeFormatter SGS_DATE=DateTimeFormatter.ofPattern("dd/MM/uuuu",Locale.ROOT);
    private EconomyClient(){}

    public static final class Point {
        public final String key,label,unit,period;
        public final BigDecimal value;
        public final LocalDate date;
        public final int respondents;
        Point(String key,String label,BigDecimal value,String unit,String period,LocalDate date,int respondents){this.key=key;this.label=label;this.value=value;this.unit=unit;this.period=period;this.date=date;this.respondents=respondents;}
    }
    public static final class Snapshot {
        public final Point selic,ipca;
        public final Map<String,Point> expectations;
        public final long fetchedAt;
        Snapshot(Point selic,Point ipca,Map<String,Point> expectations,long fetchedAt){this.selic=selic;this.ipca=ipca;this.expectations=Collections.unmodifiableMap(new LinkedHashMap<>(expectations));this.fetchedAt=fetchedAt;}
    }

    public static String focusUrl(int year){
        String filter="DataReferencia eq '"+year+"' and baseCalculo eq 0 and (Indicador eq 'IPCA' or Indicador eq 'Selic' or Indicador eq 'Câmbio' or Indicador eq 'PIB Total')";
        try{return FOCUS_BASE+"?$filter="+URLEncoder.encode(filter,"UTF-8")+"&$select=Indicador,Data,DataReferencia,Mediana,numeroRespondentes,baseCalculo&$orderby=Data%20desc&$top=100&$format=json";}catch(Exception impossible){throw new IllegalStateException(impossible);}
    }
    public static Point parseSgs(String raw,int series,LocalDate today)throws Exception{
        if(raw==null||raw.length()>16384||(series!=432&&series!=433))throw new IOException("Resposta SGS fora do perfil esperado");
        JSONArray rows=new JSONArray(raw);if(rows.length()!=1)throw new IOException("A série SGS não retornou exatamente uma observação");
        JSONObject row=rows.getJSONObject(0);LocalDate date=LocalDate.parse(row.getString("data"),SGS_DATE);String text=row.getString("valor");
        if(!text.matches("-?[0-9]{1,3}(\\.[0-9]{1,8})?"))throw new IOException("Formato numérico SGS inválido");
        BigDecimal value=new BigDecimal(text);if(value.abs().compareTo(new BigDecimal("100"))>0)throw new IOException("Valor SGS fora da faixa plausível");
        if(date.isAfter(today)||date.isBefore(LocalDate.of(1980,1,1)))throw new IOException("Data SGS inválida ou futura");
        if(series==432&&value.signum()<=0)throw new IOException("Meta Selic inválida");
        return series==432?new Point("selic","Meta Selic",value,"% ao ano","diária",date,0):new Point("ipca","IPCA",value,"% no mês","mensal",date,0);
    }
    public static Map<String,Point> parseFocus(String raw,LocalDate today,int referenceYear)throws Exception{
        if(raw==null||raw.length()>MAX_BYTES)throw new IOException("Resposta Focus excedeu o limite");
        MarketClient.checkDepth(raw);JSONObject root=new JSONObject(raw);if(root.has("@odata.nextLink")||root.has("odata.nextLink"))throw new IOException("Resposta Focus incompleta");JSONArray rows=root.getJSONArray("value");if(rows.length()>500)throw new IOException("Resposta Focus excedeu o limite de registros");
        Map<String,Point> result=new LinkedHashMap<>();LocalDate latest=LocalDate.MIN;
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);String indicator=row.optString("Indicador","");String key=focusKey(indicator);if(key==null||!Integer.toString(referenceYear).equals(row.optString("DataReferencia",""))||row.optInt("baseCalculo",-1)!=0)continue;
            LocalDate date;try{date=LocalDate.parse(row.getString("Data"));}catch(Exception badDate){continue;}if(date.isAfter(today)||date.isBefore(LocalDate.of(2000,1,1)))continue;
            Object medianRaw=row.opt("Mediana");if(!(medianRaw instanceof Number)||row.isNull("numeroRespondentes"))continue;
            BigDecimal median=new BigDecimal(medianRaw.toString());int respondents=row.getInt("numeroRespondentes");if(respondents<1||median.abs().compareTo(new BigDecimal("1000000"))>0)continue;
            String label,unit;if("ipca".equals(key)){label="IPCA";unit="% a.a.";if(median.signum()<0||median.compareTo(new BigDecimal("100"))>0)continue;}else if("selic".equals(key)){label="Selic";unit="% a.a.";if(median.signum()<0||median.compareTo(new BigDecimal("100"))>0)continue;}else if("cambio".equals(key)){label="Câmbio";unit="R$ por US$";if(median.signum()<=0||median.compareTo(new BigDecimal("1000000"))>0)continue;}else{label="PIB";unit="% a.a.";if(median.compareTo(new BigDecimal("-100"))<0||median.compareTo(new BigDecimal("100"))>0)continue;}
            Point old=result.get(key);if(old==null||date.isAfter(old.date))result.put(key,new Point(key,label,median,unit,"ano "+referenceYear,date,respondents));if(date.isAfter(latest))latest=date;
        }
        if(result.isEmpty())throw new IOException("Focus sem expectativas válidas para o ano de referência");
        for(Point point:result.values())if(!point.date.equals(latest))throw new IOException("A resposta Focus está incompleta para a data mais recente");
        return result;
    }
    private static String focusKey(String indicator){if("IPCA".equals(indicator))return "ipca";if("Selic".equals(indicator))return "selic";if("Câmbio".equals(indicator))return "cambio";if("PIB Total".equals(indicator))return "pib";return null;}

    public static String download(String address)throws IOException{
        URL parsed=new URL(address);String host=parsed.getHost().toLowerCase(Locale.ROOT);if(!"https".equalsIgnoreCase(parsed.getProtocol())||!("api.bcb.gov.br".equals(host)||"olinda.bcb.gov.br".equals(host)))throw new IOException("Fonte econômica não autorizada");
        HttpsURLConnection connection=(HttpsURLConnection)parsed.openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);connection.setUseCaches(false);connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("User-Agent","CambioClaro-Android/1.8.0");
        try{int status=connection.getResponseCode();if(status!=200)throw new IOException("Fonte econômica respondeu HTTP "+status);ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException("Consulta cancelada");if(out.size()+count>MAX_BYTES)throw new IOException("Resposta econômica maior que o limite");out.write(buffer,0,count);}}return new String(out.toByteArray(),StandardCharsets.UTF_8);}finally{connection.disconnect();}
    }
    public static void saveSgs(Context context,int series,String raw){SharedPreferences p=context.getSharedPreferences("economy_v1",Context.MODE_PRIVATE);p.edit().putString(series==432?"selic":"ipca",raw).putLong(series==432?"selic_fetched":"ipca_fetched",System.currentTimeMillis()).apply();}
    public static void saveFocus(Context context,String raw){SharedPreferences p=context.getSharedPreferences("economy_v1",Context.MODE_PRIVATE);p.edit().putString("focus",raw).putLong("focus_fetched",System.currentTimeMillis()).apply();}
    public static void markAttempt(Context context,long at){context.getSharedPreferences("economy_v1",Context.MODE_PRIVATE).edit().putLong("last_attempt",at).apply();}
    public static Snapshot cached(Context context,LocalDate today){SharedPreferences p=context.getSharedPreferences("economy_v1",Context.MODE_PRIVATE);Point selic=null,ipca=null;Map<String,Point> focus=Collections.emptyMap();try{String raw=p.getString("selic",null);if(raw!=null)selic=parseSgs(raw,432,today);}catch(Exception ignored){}try{String raw=p.getString("ipca",null);if(raw!=null)ipca=parseSgs(raw,433,today);}catch(Exception ignored){}try{String raw=p.getString("focus",null);if(raw!=null)focus=parseFocus(raw,today,today.getYear());}catch(Exception ignored){}long fetched=Math.max(p.getLong("selic_fetched",0),Math.max(p.getLong("ipca_fetched",0),p.getLong("focus_fetched",0)));if(selic==null&&ipca==null&&focus.isEmpty())return null;if(fetched<=0||fetched>System.currentTimeMillis())return null;return new Snapshot(selic,ipca,focus,fetched);}
    public static boolean cacheFresh(Context context,long now){long attempted=context.getSharedPreferences("economy_v1",Context.MODE_PRIVATE).getLong("last_attempt",0);return attempted>0&&attempted<=now&&now-attempted<CACHE_TTL;}
}


