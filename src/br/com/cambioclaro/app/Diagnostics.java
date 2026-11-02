package br.com.cambioclaro.app;
import android.content.Context;
import android.os.Build;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
/** Bounded private report. No network, telemetry, account data or quote cache is read. */
public final class Diagnostics {
    private static AtomicFile file(Context c){return new AtomicFile(new File(c.getFilesDir(),"last-failure.txt"));}
    public static void widgetEvent(Context c,String event){String cleaned=event==null?"evento indisponível":event.replaceAll("[^\\p{L}0-9 _-]","");String safe=cleaned.substring(0,Math.min(cleaned.length(),80));c.getSharedPreferences("diagnostics_v1",Context.MODE_PRIVATE).edit().putString("widget_event",safe).putLong("widget_event_at",System.currentTimeMillis()).apply();}
    public static void install(Context context){
        final Context c=context.getApplicationContext();final Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            try{record(c,thread.getName(),error);}catch(Throwable ignored){/* Never replace the original crash with a recorder failure. */}
            if(previous!=null)previous.uncaughtException(thread,error);else{android.os.Process.killProcess(android.os.Process.myPid());System.exit(10);}
        });
    }
    public static String device(Context c){
        String version="indisponível";try{version=c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName;}catch(Exception ignored){}
        return "Câmbio Claro "+version+"\nAparelho: "+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid: "+Build.VERSION.RELEASE+" • API "+Build.VERSION.SDK_INT+"\n";
    }
    public static synchronized void record(Context c,String stage,Throwable error){
        StringBuilder out=new StringBuilder(device(c)).append("Data UTC: ").append(Instant.now()).append("\nEtapa/thread: ").append(stage).append('\n');
        for(int depth=0;error!=null&&depth<6;depth++,error=error.getCause()){
            out.append(depth==0?"Erro: ":"Causado por: ").append(error.getClass().getName());String message=error.getMessage();
            if(message!=null)out.append(": ").append(safeMessage(message));out.append('\n');
            StackTraceElement[] frames=error.getStackTrace();for(int i=0;i<Math.min(frames.length,24);i++)out.append("  at ").append(frames[i]).append('\n');
            if(out.length()>14000)break;
        }
        String report=out.substring(0,Math.min(out.length(),16000));AtomicFile f=file(c);FileOutputStream stream=null;
        try{stream=f.startWrite();stream.write(report.getBytes(StandardCharsets.UTF_8));f.finishWrite(stream);}catch(IOException e){if(stream!=null)f.failWrite(stream);}
    }
    private static String safeMessage(String message){String safe=message.replaceAll("(?i)https?://\\S+","[endereço oculto]").replaceAll("(?i)(authorization\\s*[:=]?\\s*(?:bearer\\s+)?|bearer\\s+|api[_-]?key\\s*[:=]?\\s*|token\\s*[:=]?\\s*)[^\\s,;]+","$1[oculto]");return safe.substring(0,Math.min(safe.length(),500));}
    public static synchronized String read(Context c){
        try(InputStream in=file(c).openRead()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[2048];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>64000)return "Diagnóstico excedeu o limite local.";out.write(buffer,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);}
        catch(IOException e){return device(c)+"\nNenhuma falha registrada nesta versão. Reproduza a falha no widget e abra novamente este diagnóstico.";}
    }
    /** Local state only; never sends a probe or includes entered amounts and alert limits. */
    public static String report(Context c){
        StringBuilder b=new StringBuilder(device(c));DateTimeFormatter time=DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");ZoneId zone=BcbClient.ZONE;
        b.append("Diagnóstico criado: ").append(Instant.now().atZone(zone).format(time)).append(" (Brasília)\n");
        try{ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);NetworkCapabilities n=cm.getNetworkCapabilities(cm.getActiveNetwork());b.append("Rede: ").append(n==null?"sem conexão ativa":n.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?"conexão validada pelo Android":"conexão não validada pelo Android").append('\n');}catch(Exception ex){b.append("Rede: estado indisponível\n");}
        try{int[] ids=AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,CambioWidget.class));b.append("Widgets ativos: ").append(ids.length).append('\n');for(int id:ids)b.append("  Widget ").append(id).append(": ").append(Preferences.widgetTourism(c,id)?"USD turismo":Preferences.widget(c,id).name()).append('\n');}catch(Exception ex){b.append("Widgets: estado indisponível\n");}
        try{android.content.SharedPreferences p=c.getSharedPreferences("diagnostics_v1",Context.MODE_PRIVATE);long at=p.getLong("widget_event_at",0);b.append("Último toque no widget: ").append(at==0?"nenhum registrado":p.getString("widget_event","evento")+" em "+Instant.ofEpochMilli(at).atZone(zone).format(time)).append(" (Brasília)\n");}catch(Exception ex){b.append("Toques no widget: estado indisponível\n");}
        try{JobScheduler jobs=c.getSystemService(JobScheduler.class);JobInfo widget=jobs.getPendingJob(CambioWidget.JOB_ID),alert=jobs.getPendingJob(CambioWidget.ALERT_JOB_ID);b.append("Atualização de widget agendada: ").append(widget!=null?"sim":"não").append("\nAlertas PTAX agendados: ").append(alert!=null?"sim":"não").append('\n');}catch(Exception ex){b.append("Agendamentos: estado indisponível\n");}
        b.append("Fonte de mercado: AwesomeAPI (ask); fonte PTAX: Banco Central (venda).\nAtualização automática em segundo plano depende do agendador e das restrições de bateria/rede do Android.\n");
        Map<FxCurrency,MarketClient.Quote> quotes=MarketStore.read(c);long checked=MarketStore.checked(c);
        b.append("Mercado consultado: ").append(checked>0?Instant.ofEpochMilli(checked).atZone(zone).format(time):"nunca neste aparelho").append("\n");
        for(FxCurrency fx:FxCurrency.values()){
            b.append(fx.name()).append(": mercado ");MarketClient.Quote quote=MarketStore.latest(c,fx);b.append(quote==null?"sem dado":quote.at.atZone(zone).format(time));
            try{List<Rate> ptax=RateStore.read(c,fx);b.append(" | PTAX ").append(ptax.isEmpty()?"sem dado":ptax.get(ptax.size()-1).at.format(time));}
            catch(Exception ex){b.append(" | PTAX indisponível");}
            b.append('\n');
        }
        MarketClient.Quote travel=Tourism.latest(c);b.append("USD turismo: ").append(travel==null?"sem dado":travel.at.atZone(zone).format(time)).append("\n");
        b.append("Bolsa brapi: servidor do índice ").append(Stocks.backend(c).isEmpty()?"não configurado":"configurado (endereço oculto)").append('\n');Map<String,Stocks.Quote> stocks=new LinkedHashMap<>(Stocks.read(c,false));if(!Stocks.backend(c).isEmpty())stocks.putAll(Stocks.read(c,true));for(String symbol:new String[]{"^BVSP","PETR4","VALE3","ITUB4","MGLU3"}){Stocks.Quote q=stocks.get(symbol);b.append(symbol).append(": ").append(q==null?"sem dado":Stocks.displayTime(q.time)).append('\n');}
        EconomyClient.Snapshot economy=EconomyClient.cached(c,BcbClient.today());b.append("Painel econômico: ").append(economy==null?"sem cache válido":"consulta "+Instant.ofEpochMilli(economy.fetchedAt).atZone(zone).format(time)+"; SGS Selic "+(economy.selic==null?"sem dado":economy.selic.date)+"; IPCA "+(economy.ipca==null?"sem dado":economy.ipca.date)+"; Focus "+economy.expectations.size()+" indicadores").append('\n');
        b.append("\nÚltima falha local:\n").append(read(c));return b.toString();
    }
    public static synchronized void clear(Context c){file(c).delete();}
}
