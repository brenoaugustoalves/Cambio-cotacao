package br.com.cambioclaro.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;
import br.com.cambioclaro.app.market.application.EconomyFreshnessPolicy;

/** Small, bounded reader for the BCB's official Atom press-release feed. */
public final class BcbNews {
    public static final String FEED_BASE="https://www.bcb.gov.br/api/feed/sitebcb/sitefeeds/notasImprensa?ano=";
    private static final int MAX_BYTES=512*1024;
    private static final int MAX_ITEMS=100;
    private BcbNews(){}

    public static final class Item {
        public final String title,url;
        public final Instant published;
        Item(String title,String url,Instant published){this.title=title;this.url=url;this.published=published;}
    }

    public static String feedUrl(int year){if(year<2020||year>LocalDate.now().getYear()+1)throw new IllegalArgumentException("Ano de feed inválido");return FEED_BASE+year;}

    public static List<Item> parse(String xml,Instant now)throws Exception{
        if(xml==null||xml.length()>MAX_BYTES)throw new IOException("Feed do BC excedeu o limite");
        XmlPullParserFactory factory=XmlPullParserFactory.newInstance();factory.setNamespaceAware(true);
        XmlPullParser parser=factory.newPullParser();parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);parser.setInput(new StringReader(xml));
        List<Item> result=new ArrayList<>();int event;
        while((event=parser.next())!=XmlPullParser.END_DOCUMENT){
            if(event!=XmlPullParser.START_TAG||!"entry".equals(parser.getName()))continue;
            if(result.size()>=MAX_ITEMS)throw new IOException("Feed do BC excedeu o limite de itens");
            String title=null,href=null,updated=null;int depth=parser.getDepth();
            while((event=parser.next())!=XmlPullParser.END_DOCUMENT){
                if(event==XmlPullParser.END_TAG&&parser.getDepth()==depth&&"entry".equals(parser.getName()))break;
                if(event!=XmlPullParser.START_TAG)continue;
                String name=parser.getName();
                if("title".equals(name)&&title==null)title=parser.nextText();
                else if("updated".equals(name)&&updated==null)updated=parser.nextText();
                else if("link".equals(name)&&href==null&&"alternate".equals(parser.getAttributeValue(null,"rel")))href=parser.getAttributeValue(null,"href");
            }
            if(title==null||updated==null||href==null)continue;
            title=title.replaceAll("\\s+"," ").trim();if(title.isEmpty()||title.length()>240)continue;
            Instant date;try{date=OffsetDateTime.parse(updated.trim()).toInstant();}catch(Exception ex){continue;}
            if(date.isAfter(now.plusSeconds(300))||date.isBefore(Instant.parse("2000-01-01T00:00:00Z")))continue;
            if(!isBcbArticle(href))continue;
            result.add(new Item(title,href,date));
        }
        Collections.sort(result,Comparator.comparing((Item item)->item.published).reversed());
        return Collections.unmodifiableList(result);
    }
    private static boolean isBcbArticle(String raw){try{URL url=new URL(raw);String host=url.getHost().toLowerCase(Locale.ROOT);return "https".equalsIgnoreCase(url.getProtocol())&&("www.bcb.gov.br".equals(host)||"bcb.gov.br".equals(host))&&url.getUserInfo()==null;}catch(Exception ex){return false;}}
    public static String download(String address)throws IOException{URL url=new URL(address);if(!"https".equalsIgnoreCase(url.getProtocol())||!"www.bcb.gov.br".equalsIgnoreCase(url.getHost())||url.getUserInfo()!=null||!address.startsWith(FEED_BASE)||!address.matches("https://www\\.bcb\\.gov\\.br/api/feed/sitebcb/sitefeeds/notasImprensa\\?ano=[0-9]{4}"))throw new IOException("Feed fora da lista oficial");HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);connection.setUseCaches(false);connection.setRequestProperty("Accept","application/atom+xml");connection.setRequestProperty("User-Agent","CambioClaro-Android/1.8.0");try{if(connection.getResponseCode()!=200)throw new IOException("Feed oficial respondeu HTTP "+connection.getResponseCode());ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException("Consulta cancelada");if(out.size()+count>MAX_BYTES)throw new IOException("Feed oficial excedeu o limite");out.write(buffer,0,count);}}return new String(out.toByteArray(),StandardCharsets.UTF_8);}finally{connection.disconnect();}}
    public static void save(Context context,String xml,long at){SharedPreferences p=context.getSharedPreferences("economy_news_v1",Context.MODE_PRIVATE);p.edit().putString("atom",xml).putLong("fetched",at).apply();}
    public static List<Item> cached(Context context,Instant now){String xml=context.getSharedPreferences("economy_news_v1",Context.MODE_PRIVATE).getString("atom",null);if(xml==null)return Collections.emptyList();try{return parse(xml,now);}catch(Exception ex){return Collections.emptyList();}}
    public static long cachedAt(Context context){return context.getSharedPreferences("economy_news_v1",Context.MODE_PRIVATE).getLong("fetched",0);}
    public static boolean fresh(Context context,long now){SharedPreferences p=context.getSharedPreferences("economy_news_v1",Context.MODE_PRIVATE);long at=p.getLong("fetched",0);return EconomyFreshnessPolicy.isFresh(at,now,EconomyFreshnessPolicy.OFFICIAL_NEWS_TTL_MS);}
}


