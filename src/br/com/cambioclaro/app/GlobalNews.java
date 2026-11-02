package br.com.cambioclaro.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import javax.net.ssl.HttpsURLConnection;

/** Official international macroeconomic releases; titles are shown without generated analysis. */
public final class GlobalNews {
    private static final int MAX_BYTES=131072,MAX_ITEMS=80;
    public static final String FED="https://www.federalreserve.gov/feeds/press_monetary.xml";
    public static final String ECB="https://www.ecb.europa.eu/rss/press.html";
    private GlobalNews(){}
    public static final class Source {public final String id,label,url,host;Source(String id,String label,String url,String host){this.id=id;this.label=label;this.url=url;this.host=host;}}
    public static final Source[] SOURCES={new Source("fed","EUA · Federal Reserve",FED,"www.federalreserve.gov"),new Source("ecb","Zona do Euro · BCE",ECB,"www.ecb.europa.eu")};
    public static final class Item {public final String source,title,url;public final java.time.Instant published;Item(String source,String title,String url,java.time.Instant published){this.source=source;this.title=title;this.url=url;this.published=published;}}
    public static Source source(String id){for(Source source:SOURCES)if(source.id.equals(id))return source;throw new IllegalArgumentException("Fonte internacional não autorizada");}
    public static List<Item> parse(String id,String xml,java.time.Instant now)throws Exception{Source source=source(id);if(xml==null||xml.length()>MAX_BYTES)throw new IOException("Feed internacional excedeu o limite");XmlPullParserFactory factory=XmlPullParserFactory.newInstance();factory.setNamespaceAware(true);XmlPullParser parser=factory.newPullParser();parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);parser.setInput(new StringReader(xml));List<Item> items=new ArrayList<>();int event;while((event=parser.next())!=XmlPullParser.END_DOCUMENT){if(event!=XmlPullParser.START_TAG||!"item".equals(parser.getName()))continue;if(items.size()>=MAX_ITEMS)throw new IOException("Feed internacional tem itens demais");int depth=parser.getDepth();String title=null,link=null,pubDate=null;while((event=parser.next())!=XmlPullParser.END_DOCUMENT){if(event==XmlPullParser.END_TAG&&parser.getDepth()==depth&&"item".equals(parser.getName()))break;if(event!=XmlPullParser.START_TAG)continue;String name=parser.getName();if("title".equals(name)&&title==null)title=parser.nextText();else if("link".equals(name)&&link==null)link=parser.nextText();else if("pubDate".equals(name)&&pubDate==null)pubDate=parser.nextText();}if(title==null||link==null||pubDate==null)continue;title=title.replaceAll("\\s+"," ").trim();if(title.isEmpty()||title.length()>240||!articleLink(source,link))continue;java.time.Instant at=parseDate(pubDate);if(at==null||at.isAfter(now.plusSeconds(300))||at.isBefore(java.time.Instant.parse("2000-01-01T00:00:00Z")))continue;items.add(new Item(source.label,title,link,at));}Collections.sort(items,Comparator.comparing((Item item)->item.published).reversed());return Collections.unmodifiableList(items);}
    private static java.time.Instant parseDate(String value){for(String pattern:new String[]{"EEE, dd MMM yyyy HH:mm:ss z","EEE, dd MMM yyyy HH:mm z"}){try{SimpleDateFormat format=new SimpleDateFormat(pattern,Locale.US);format.setLenient(false);format.setTimeZone(TimeZone.getTimeZone("UTC"));ParsePosition position=new ParsePosition(0);Date date=format.parse(value.trim(),position);if(date!=null&&position.getIndex()==value.trim().length())return date.toInstant();}catch(Exception ignored){}}return null;}
    private static boolean articleLink(Source source,String raw){try{URL url=new URL(raw);return "https".equalsIgnoreCase(url.getProtocol())&&source.host.equalsIgnoreCase(url.getHost())&&url.getUserInfo()==null;}catch(Exception ex){return false;}}
    public static String download(String id)throws IOException{Source source;try{source=source(id);}catch(Exception ex){throw new IOException("Feed não autorizado",ex);}URL url=new URL(source.url);if(!"https".equalsIgnoreCase(url.getProtocol())||!source.host.equalsIgnoreCase(url.getHost()))throw new IOException("Host internacional não autorizado");HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);connection.setUseCaches(false);connection.setRequestProperty("Accept","application/rss+xml, application/xml, text/xml");connection.setRequestProperty("User-Agent","CambioClaro-Android/1.7.9");try{if(connection.getResponseCode()!=200)throw new IOException("Feed internacional respondeu HTTP "+connection.getResponseCode());ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException("Consulta cancelada");if(out.size()+count>MAX_BYTES)throw new IOException("Feed internacional excedeu o limite");out.write(buffer,0,count);}}return new String(out.toByteArray(),StandardCharsets.UTF_8).replaceFirst("^\\uFEFF","");}finally{connection.disconnect();}}
    public static void save(Context context,String id,String xml,long at){source(id);context.getSharedPreferences("global_market_news_v1",Context.MODE_PRIVATE).edit().putString(id,xml).putLong(id+"_fetched",at).apply();}
    public static List<Item> cached(Context context,String id,java.time.Instant now){try{return parse(id,context.getSharedPreferences("global_market_news_v1",Context.MODE_PRIVATE).getString(id,null),now);}catch(Exception ex){return Collections.emptyList();}}
    public static long cachedAt(Context context,String id){source(id);return context.getSharedPreferences("global_market_news_v1",Context.MODE_PRIVATE).getLong(id+"_fetched",0);}
    public static boolean freshSource(Context context,String id,long now){long at=cachedAt(context,id);return at>0&&at<=now&&now-at<3L*60*60*1000;}
    public static boolean fresh(Context context,long now){SharedPreferences p=context.getSharedPreferences("global_market_news_v1",Context.MODE_PRIVATE);long fed=p.getLong("fed_fetched",0),ecb=p.getLong("ecb_fetched",0);return fed>0&&ecb>0&&fed<=now&&ecb<=now&&now-fed<3L*60*60*1000&&now-ecb<3L*60*60*1000;}
}


