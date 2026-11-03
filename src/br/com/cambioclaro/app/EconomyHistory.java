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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.net.ssl.HttpsURLConnection;
import br.com.cambioclaro.app.market.application.EconomyFreshnessPolicy;

/** Actual BCB observations, aligned by month without interpolating missing data. */
public final class EconomyHistory {
    public static final int SELIC_SERIES=432, IPCA_SERIES=433;
    private static final int MAX_BYTES=262144;
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/uuuu",Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private EconomyHistory(){}

    public static final class Observation {
        public final LocalDate date; public final BigDecimal value;
        Observation(LocalDate date,BigDecimal value){this.date=date;this.value=value;}
    }
    public static final class Month {
        public final YearMonth month; public final Observation ipca,selic;
        Month(YearMonth month,Observation ipca,Observation selic){this.month=month;this.ipca=ipca;this.selic=selic;}
    }

    public static String url(int series,LocalDate today){if((series!=SELIC_SERIES&&series!=IPCA_SERIES)||today==null)throw new IllegalArgumentException("Série econômica não autorizada");LocalDate start=today.minusMonths(12).withDayOfMonth(1);try{return "https://api.bcb.gov.br/dados/serie/bcdata.sgs."+series+"/dados?formato=json&dataInicial="+URLEncoder.encode(start.format(DATE),"UTF-8")+"&dataFinal="+URLEncoder.encode(today.format(DATE),"UTF-8");}catch(Exception ex){throw new IllegalStateException(ex);}}
    public static List<Observation> parse(String raw,int series,LocalDate today)throws Exception{
        if(raw==null||raw.length()>MAX_BYTES||(series!=SELIC_SERIES&&series!=IPCA_SERIES))throw new IOException("Histórico BCB fora do perfil permitido");
        JSONArray rows=new JSONArray(raw);if(rows.length()<1||rows.length()>400)throw new IOException("Quantidade de observações BCB inesperada");
        List<Observation> parsed=new ArrayList<>();java.util.HashSet<LocalDate> dates=new java.util.HashSet<>();
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);LocalDate date;try{date=LocalDate.parse(row.getString("data"),DATE);}catch(Exception ex){throw new IOException("Data inválida no histórico BCB",ex);}String text=row.getString("valor");if(!text.matches("-?[0-9]{1,3}(\\.[0-9]{1,8})?"))throw new IOException("Valor inválido no histórico BCB");BigDecimal value=new BigDecimal(text);if(value.abs().compareTo(new BigDecimal("100"))>0||(series==SELIC_SERIES&&value.signum()<=0))throw new IOException("Valor fora da faixa da série SGS");if(date.isAfter(today)||date.isBefore(LocalDate.of(1980,1,1))||!dates.add(date))throw new IOException("Data futura, histórica ou duplicada no SGS");parsed.add(new Observation(date,value));}
        Collections.sort(parsed,Comparator.comparing(point->point.date));return Collections.unmodifiableList(parsed);
    }
    public static List<Month> align(List<Observation> ipca,List<Observation> selic){Map<YearMonth,Observation> prices=new TreeMap<>(),rates=new TreeMap<>();for(Observation p:ipca){YearMonth m=YearMonth.from(p.date);if(!prices.containsKey(m)||p.date.isAfter(prices.get(m).date))prices.put(m,p);}for(Observation p:selic){YearMonth m=YearMonth.from(p.date);if(!rates.containsKey(m)||p.date.isAfter(rates.get(m).date))rates.put(m,p);}java.util.TreeSet<YearMonth> months=new java.util.TreeSet<>();months.addAll(prices.keySet());months.addAll(rates.keySet());List<Month> result=new ArrayList<>();for(YearMonth month:months)result.add(new Month(month,prices.get(month),rates.get(month)));int from=Math.max(0,result.size()-12);return Collections.unmodifiableList(new ArrayList<>(result.subList(from,result.size())));}
    public static String download(String address)throws IOException{URL url=new URL(address);String host=url.getHost();String path=url.getPath();String query=url.getQuery();if(!"https".equalsIgnoreCase(url.getProtocol())||!"api.bcb.gov.br".equalsIgnoreCase(host)||url.getUserInfo()!=null||path==null||!path.matches("/dados/serie/bcdata\\.sgs\\.(432|433)/dados")||query==null||!query.matches("formato=json&dataInicial=[0-9]{2}%2F[0-9]{2}%2F[0-9]{4}&dataFinal=[0-9]{2}%2F[0-9]{2}%2F[0-9]{4}"))throw new IOException("URL de histórico não autorizada");try{String[] parts=query.split("&");LocalDate start=LocalDate.parse(URLDecoder.decode(parts[1].substring("dataInicial=".length()),"UTF-8"),DATE),end=LocalDate.parse(URLDecoder.decode(parts[2].substring("dataFinal=".length()),"UTF-8"),DATE);if(start.isAfter(end)||start.isBefore(end.minusDays(400))||end.isAfter(BcbClient.today()))throw new IOException("Intervalo de histórico fora do limite");}catch(IOException ex){throw ex;}catch(Exception ex){throw new IOException("Intervalo de histórico inválido",ex);}HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);connection.setUseCaches(false);connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("User-Agent","CambioClaro-Android/1.7.3");try{if(connection.getResponseCode()!=200)throw new IOException("Histórico SGS respondeu HTTP "+connection.getResponseCode());ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException("Consulta cancelada");if(out.size()+count>MAX_BYTES)throw new IOException("Histórico SGS excedeu o limite");out.write(buffer,0,count);}}return new String(out.toByteArray(),StandardCharsets.UTF_8);}finally{connection.disconnect();}}
    public static void save(Context c,int series,String raw,long at){SharedPreferences p=c.getSharedPreferences("economy_history_v1",Context.MODE_PRIVATE);p.edit().putString(series==SELIC_SERIES?"selic":"ipca",raw).putLong(series==SELIC_SERIES?"selic_at":"ipca_at",at).apply();}
    public static List<Month> cached(Context c,LocalDate today){SharedPreferences p=c.getSharedPreferences("economy_history_v1",Context.MODE_PRIVATE);List<Observation> selic=read(p.getString("selic",null),SELIC_SERIES,today),ipca=read(p.getString("ipca",null),IPCA_SERIES,today);return align(ipca,selic);}
    private static List<Observation> read(String raw,int series,LocalDate today){try{return parse(raw,series,today);}catch(Exception ex){return Collections.emptyList();}}
    public static long cachedAt(Context c){SharedPreferences p=c.getSharedPreferences("economy_history_v1",Context.MODE_PRIVATE);return Math.max(p.getLong("selic_at",0),p.getLong("ipca_at",0));}
    public static boolean fresh(Context c,long now){long at=c.getSharedPreferences("economy_history_v1",Context.MODE_PRIVATE).getLong("last_attempt",0);return EconomyFreshnessPolicy.isFresh(at,now,EconomyFreshnessPolicy.INDICATORS_TTL_MS);}
    public static void markAttempt(Context c,long at){c.getSharedPreferences("economy_history_v1",Context.MODE_PRIVATE).edit().putLong("last_attempt",at).apply();}
}
