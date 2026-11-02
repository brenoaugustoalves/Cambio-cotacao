package br.com.cambioclaro.app;

import android.app.*;
import android.content.*;
import android.os.Build;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Opt-in, one-shot alerts evaluated against fresh AwesomeAPI ask quotes only. */
public final class MarketAlerts {
    public static final String CHANNEL="market_alerts";
    private static final long FRESH_SECONDS=10*60;
    public static SharedPreferences prefs(Context c){return c.getSharedPreferences("market_alerts",Context.MODE_PRIVATE);}
    public static boolean enabled(Context c,FxCurrency fx){return prefs(c).getBoolean(fx.name()+"_enabled",false);}
    public static Set<FxCurrency> currencies(Context c){Set<FxCurrency> out=EnumSet.noneOf(FxCurrency.class);for(FxCurrency fx:FxCurrency.values())if(enabled(c,fx))out.add(fx);return out;}
    public static synchronized void save(Context c,FxCurrency fx,String value,boolean above){BigDecimal n=Alerts.threshold(value);prefs(c).edit().putString(fx.name()+"_value",n.toPlainString()).putBoolean(fx.name()+"_above",above).putBoolean(fx.name()+"_enabled",true).putBoolean(fx.name()+"_fired",false).apply();CambioWidget.syncAlertSchedule(c);}
    public static synchronized void disable(Context c,FxCurrency fx){prefs(c).edit().putBoolean(fx.name()+"_enabled",false).apply();c.getSystemService(NotificationManager.class).cancel(9000+fx.ordinal());CambioWidget.syncAlertSchedule(c);}
    public static boolean fresh(MarketClient.Quote q,Instant now){return q!=null&&!q.at.isAfter(now.plusSeconds(300))&&!q.at.isBefore(now.minusSeconds(FRESH_SECONDS));}
    public static boolean shouldFire(BigDecimal value,BigDecimal threshold,boolean above,boolean alreadyFired,boolean fresh){return fresh&&!alreadyFired&&Alerts.reached(value,threshold,above);}
    private static void channel(Context c){c.getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Alertas de mercado",NotificationManager.IMPORTANCE_DEFAULT));}
    private static boolean permitted(Context c){channel(c);NotificationManager n=c.getSystemService(NotificationManager.class);return (Build.VERSION.SDK_INT<33||c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==android.content.pm.PackageManager.PERMISSION_GRANTED)&&n.areNotificationsEnabled()&&n.getNotificationChannel(CHANNEL).getImportance()!=NotificationManager.IMPORTANCE_NONE;}
    public static synchronized void evaluate(Context c,FxCurrency fx,MarketClient.Quote q){
        if(!enabled(c,fx)||!fresh(q,Instant.now())||!permitted(c))return;SharedPreferences p=prefs(c);if(p.getBoolean(fx.name()+"_fired",false))return;BigDecimal threshold;
        try{threshold=Alerts.threshold(p.getString(fx.name()+"_value",""));}catch(IllegalArgumentException bad){return;}
        boolean above=p.getBoolean(fx.name()+"_above",true);if(!shouldFire(q.ask,threshold,above,false,true))return;
        String when=q.at.atZone(BcbClient.ZONE).format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm"));String body="Mercado · AwesomeAPI ask: R$ "+q.ask.toPlainString().replace('.',',')+" · publicado em "+when+" (Brasília). Pode haver atraso na fonte.";
        Intent open=new Intent(c,MainActivity.class).putExtra("currency",fx.name()).setData(android.net.Uri.parse("cambioclaro://market-alert/"+fx.name()));PendingIntent action=PendingIntent.getActivity(c,9000+fx.ordinal(),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_launcher).setContentTitle(fx.name()+": limite de mercado "+(above?"superior":"inferior")+" atingido").setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(action).setAutoCancel(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
        try{c.getSystemService(NotificationManager.class).notify(9000+fx.ordinal(),n);p.edit().putBoolean(fx.name()+"_fired",true).apply();}catch(SecurityException ignored){/* Keep alert armed until permission is available. */}
    }
}
