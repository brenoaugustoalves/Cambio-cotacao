package br.com.cambioclaro.app;
import android.app.*;
import android.appwidget.*;
import android.app.job.*;
import android.content.*;
import android.os.*;
import android.widget.RemoteViews;
import android.graphics.*;
import android.view.View;
import java.util.*;
import java.time.*;
import java.time.format.*;
import java.text.NumberFormat;
public final class CambioWidget extends AppWidgetProvider {
    public static final String REFRESH="br.com.cambioclaro.app.REFRESH_WIDGET";
    public static final int JOB_ID=3701, ALERT_JOB_ID=3702;
    @Override public void onDeleted(Context c,int[] ids){for(int id:ids)Preferences.prefs(c).edit().remove("widget_"+id).remove("widget_tourism_"+id).apply();}
    public static void syncAlertSchedule(Context c){JobScheduler s=c.getSystemService(JobScheduler.class);if(Alerts.currencies(c).isEmpty()&&MarketAlerts.currencies(c).isEmpty()){s.cancel(ALERT_JOB_ID);return;}if(s.getPendingJob(ALERT_JOB_ID)==null)s.schedule(new JobInfo.Builder(ALERT_JOB_ID,new ComponentName(c,WidgetUpdateService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(1800000).setPersisted(true).build());}
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){renderAll(c,null);schedule(c);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){renderAll(c,null);}
    @Override public void onDisabled(Context c){if(Alerts.currencies(c).isEmpty())c.getSystemService(JobScheduler.class).cancel(JOB_ID);}
    @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);if(REFRESH.equals(i.getAction())){Diagnostics.widgetEvent(c,"atualizar solicitado");schedule(c,true);}}
    public static void schedule(Context c){
        schedule(c,false);
    }
    public static void schedule(Context c,boolean userRequested){
        JobScheduler scheduler=c.getSystemService(JobScheduler.class);
        if(!userRequested&&scheduler.getPendingJob(JOB_ID)!=null){renderAll(c,"Atualização em segundo plano solicitada • pode ser adiada pelo Android");return;}
        if(userRequested){long now=System.currentTimeMillis(),last=Preferences.prefs(c).getLong("widget_manual_refresh",0);if(now-last<30000){renderAll(c,"Pedido recente • aguarde alguns segundos ou abra o app");return;}Preferences.prefs(c).edit().putLong("widget_manual_refresh",now).apply();scheduler.cancel(JOB_ID);}
        JobInfo.Builder builder=new JobInfo.Builder(JOB_ID,new ComponentName(c,WidgetUpdateService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        if(Build.VERSION.SDK_INT>=28)builder.setEstimatedNetworkBytes(100000,2000);
        JobInfo info=builder.build();
        int result=scheduler.schedule(info);renderAll(c,result==JobScheduler.RESULT_SUCCESS?(userRequested?"Consulta solicitada • depende de conexão e do Android":"Atualização automática solicitada • o Android pode adiar"):"Não foi possível agendar. Abra o app.");
    }
    public static RemoteViews views(Context c,String message){return views(c,message,-1);}
    public static RemoteViews views(Context c,String message,int id){
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.cambio_widget);
        UiStyle style=new UiStyle(c);v.setInt(R.id.widget_root,"setBackgroundResource",style.light?R.drawable.widget_bg_light:R.drawable.widget_bg);
        v.setInt(R.id.widget_refresh,"setBackgroundResource",style.light?R.drawable.widget_button_light:R.drawable.widget_button);
        for(int field:new int[]{R.id.widget_pair,R.id.widget_refresh,R.id.widget_settings,R.id.widget_change,R.id.widget_day_move})v.setTextColor(field,style.accent);
        for(int field:new int[]{R.id.widget_rate,R.id.widget_date})v.setTextColor(field,style.text);
        for(int field:new int[]{R.id.widget_unit,R.id.widget_status,R.id.widget_chart_label})v.setTextColor(field,style.muted);
        v.setViewVisibility(R.id.widget_chart,View.GONE);
        v.setTextViewText(R.id.widget_chart_label,"Histórico indisponível");
        v.setTextViewText(R.id.widget_change,"Variação indisponível");
        v.setTextViewText(R.id.widget_day_move,"Aguardando observações reais de hoje");
        v.setViewVisibility(R.id.widget_exact,View.GONE);
        FxCurrency currency=id<0?RateStore.active(c):Preferences.widget(c,id);boolean tourism=id>=0&&Preferences.widgetTourism(c,id);v.setTextViewText(R.id.widget_pair,tourism?"USD turismo / BRL":currency.name()+" / BRL");v.setTextViewText(R.id.widget_unit,tourism?"USD 1 · turismo (ask) · AwesomeAPI":currency.name()+" 1 · mercado (ask) · AwesomeAPI");
        List<Rate> rates=RateStore.read(c,currency);long fetched=RateStore.prefs(c,currency).getLong("fetched",0);
        MarketClient.Quote market=tourism?Tourism.latest(c):MarketStore.latest(c,currency);
        if(market!=null){
            NumberFormat f=NumberFormat.getNumberInstance(new Locale("pt","BR"));f.setMinimumFractionDigits(currency==FxCurrency.USD?2:4);f.setMaximumFractionDigits(currency==FxCurrency.USD?2:Math.max(4,market.ask.scale()));
            v.setTextViewText(R.id.widget_rate,"R$ "+f.format(market.ask));
            ZonedDateTime at=market.at.atZone(BcbClient.ZONE);
            v.setTextViewText(R.id.widget_date,"Mercado: "+at.format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm"))+" • Brasília");
            long age=Math.max(0,Duration.between(market.at,Instant.now()).toMinutes());
            v.setTextViewText(R.id.widget_status,message==null?(at.toLocalDate().equals(BcbClient.today())?(age>=10?"Dado desatualizado • última publicação há "+age+" min":"Última publicação há "+age+" min • pode haver atraso") :"Última cotação • sem publicação hoje"):message+" • último dado salvo");
            if(currency==FxCurrency.USD&&!tourism){NumberFormat exact=NumberFormat.getNumberInstance(new Locale("pt","BR"));exact.setMinimumFractionDigits(4);exact.setMaximumFractionDigits(Math.max(4,market.ask.scale()));v.setTextViewText(R.id.widget_exact,"Taxa exata da fonte: R$ "+exact.format(market.ask));v.setViewVisibility(R.id.widget_exact,View.VISIBLE);}
            List<MarketHistory.Point> points=tourism?Tourism.readHistory(c):MarketHistory.read(c,currency);java.util.TreeMap<Instant,java.math.BigDecimal> day=new java.util.TreeMap<>();for(MarketHistory.Point point:points)day.put(point.at,point.ask);if(at.toLocalDate().equals(BcbClient.today()))day.put(market.at,market.ask);
            if(day.size()>1){java.util.Map.Entry<Instant,java.math.BigDecimal> first=day.firstEntry(),last=day.lastEntry(),prior=day.lowerEntry(last.getKey());java.math.BigDecimal sincePrevious=Rate.variation(prior.getValue(),last.getValue()),sinceFirst=Rate.variation(first.getValue(),last.getValue()),shownPrevious=sincePrevious.setScale(2,java.math.RoundingMode.HALF_UP);String d=shownPrevious.signum()>0?"▲ Alta ":shownPrevious.signum()<0?"▼ Baixa ":"● Estável ";v.setTextViewText(R.id.widget_change,d+String.format(new Locale("pt","BR"),"%+.2f%%",shownPrevious)+" vs publicação anterior");v.setTextViewText(R.id.widget_day_move,String.format(new Locale("pt","BR"),"%+.2f%% desde 1ª observação de hoje",sinceFirst));v.setTextColor(R.id.widget_change,shownPrevious.signum()<0?(style.light?0xFFA33122:0xFFF2B5A9):shownPrevious.signum()==0?(style.light?0xFF476271:0xFF9FB3C1):style.accent);}else{v.setTextViewText(R.id.widget_change,"Variação vs publicação anterior indisponível");v.setTextViewText(R.id.widget_day_move,"Variação do dia aguarda mais pontos reais");}
        }
        if(!tourism&&!rates.isEmpty()){
            Rate r=rates.get(rates.size()-1);NumberFormat f=NumberFormat.getNumberInstance(new Locale("pt","BR"));f.setMinimumFractionDigits(4);f.setMaximumFractionDigits(Math.max(4,r.sell.scale()));
            if(market==null){v.setTextViewText(R.id.widget_unit,currency.name()+" 1 · PTAX venda · Banco Central");v.setTextViewText(R.id.widget_rate,"R$ "+f.format(r.sell));
            if(rates.size()>1){java.math.BigDecimal change=Rate.variation(rates.get(rates.size()-2).sell,r.sell).setScale(2,java.math.RoundingMode.HALF_UP);String direction=change.signum()>0?"▲ Alta ":change.signum()<0?"▼ Baixa ":"● Estável ";v.setTextViewText(R.id.widget_change,direction+String.format(new Locale("pt","BR"),"%+.2f%%",change)+" vs publicação PTAX anterior");v.setTextColor(R.id.widget_change,change.signum()<0?(style.light?0xFFA33122:0xFFF2B5A9):change.signum()==0?(style.light?0xFF476271:0xFF9FB3C1):style.accent);}
            v.setTextViewText(R.id.widget_date,"PTAX: "+r.at.format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm"))+" • Brasília");
            String sync=fetched>0?Instant.ofEpochMilli(fetched).atZone(BcbClient.ZONE).format(DateTimeFormatter.ofPattern("dd/MM HH:mm")):"indisponível";
            v.setTextViewText(R.id.widget_status,message==null?"PTAX consultada: "+sync+(!r.at.toLocalDate().equals(BcbClient.today())?" • sem publicação hoje":""):message+" • exibindo dados salvos");}
            List<Rate> history=Rate.period(rates,BcbClient.today(),30);
            if(history.size()>1){v.setImageViewBitmap(R.id.widget_chart,sparkline(history,style.light));v.setViewVisibility(R.id.widget_chart,View.VISIBLE);v.setTextViewText(R.id.widget_chart_label,"Histórico PTAX · 30 dias · "+history.size()+" publicações");}
        }else if(market==null){v.setTextViewText(R.id.widget_rate,"—");v.setTextViewText(R.id.widget_date,"Sem cotação recebida");v.setTextViewText(R.id.widget_status,message==null?"Toque em ↻ ou abra o app":message);}
        if(tourism)v.setTextViewText(R.id.widget_chart_label,"Gráfico de hoje no app · USD turismo");
        Intent open=new Intent(c,MainActivity.class).putExtra("currency",currency.name()).putExtra("tourism",tourism).setData(android.net.Uri.parse("cambioclaro://widget/"+id)).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent refresh=new Intent(c,CambioWidget.class).setAction(REFRESH);
        v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        v.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(c,1,refresh,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        Intent config=new Intent(c,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
        v.setOnClickPendingIntent(R.id.widget_settings,PendingIntent.getActivity(c,10000+id,config,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        return v;
    }
    public static Bitmap sparkline(List<Rate> history){return sparkline(history,false);}
    public static Bitmap sparkline(List<Rate> history,boolean light){
        Bitmap bitmap=Bitmap.createBitmap(480,120,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);Paint paint=new Paint(3);
        if(history.size()<2)return bitmap;
        double min=Double.MAX_VALUE,max=-Double.MAX_VALUE;for(Rate r:history){min=Math.min(min,r.sell.doubleValue());max=Math.max(max,r.sell.doubleValue());}
        double margin=Math.max((max-min)*.12,.001);min-=margin;max+=margin;
        long first=history.get(0).at.toLocalDate().toEpochDay(),last=history.get(history.size()-1).at.toLocalDate().toEpochDay();
        Path path=new Path();float endX=0,endY=0;
        for(int i=0;i<history.size();i++){Rate r=history.get(i);float x=5+(r.at.toLocalDate().toEpochDay()-first)*470f/Math.max(1,last-first);float y=(float)(112-(r.sell.doubleValue()-min)/(max-min)*104);if(i==0)path.moveTo(x,y);else path.lineTo(x,y);endX=x;endY=y;}
        Path fill=new Path(path);fill.lineTo(endX,119);fill.lineTo(5,119);fill.close();paint.setShader(new LinearGradient(0,0,0,120,0x5566E0BF,0x0066E0BF,Shader.TileMode.CLAMP));canvas.drawPath(fill,paint);paint.setShader(null);paint.setColor(light?0xFF086A53:0xFF66E0BF);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.5f);paint.setStrokeJoin(Paint.Join.ROUND);canvas.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(endX,endY,3.5f,paint);return bitmap;
    }
    public static void renderAll(Context c,String message){
        AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new ComponentName(c,CambioWidget.class));
        for(int id:ids){if(!Preferences.prefs(c).contains("widget_"+id))Preferences.widget(c,id,RateStore.active(c));m.updateAppWidget(id,views(c,message,id));}
    }
}
