package br.com.cambioclaro.app;
import android.app.*;
import android.content.*;
import android.os.Build;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
/** One-shot, opt-in alerts. Only evaluate after a successful source fetch. */
public final class Alerts {
    public static final String CHANNEL="ptax_alerts";
    public static SharedPreferences prefs(Context c){return c.getSharedPreferences("alerts",Context.MODE_PRIVATE);}
    public static BigDecimal threshold(String input){if(input==null||!input.trim().matches("[0-9]{1,8}([.,][0-9]{1,6})?"))throw new IllegalArgumentException("Use até 8 dígitos e 6 casas decimais, sem separador de milhar.");BigDecimal v=new BigDecimal(input.trim().replace(',','.'));if(v.signum()<=0)throw new IllegalArgumentException("O limite precisa ser maior que zero.");return v;}
    public static boolean reached(BigDecimal value,BigDecimal threshold,boolean above){int cmp=value.compareTo(threshold);return above?cmp>=0:cmp<=0;}
    public static boolean enabled(Context c,FxCurrency fx){return prefs(c).getBoolean(fx.name()+"_enabled",false);}
    public static Set<FxCurrency> currencies(Context c){Set<FxCurrency> out=EnumSet.noneOf(FxCurrency.class);for(FxCurrency fx:FxCurrency.values())if(enabled(c,fx))out.add(fx);return out;}
    public static synchronized void save(Context c,FxCurrency fx,String value,boolean above){BigDecimal number=threshold(value);prefs(c).edit().putString(fx.name()+"_value",number.toPlainString()).putBoolean(fx.name()+"_above",above).putBoolean(fx.name()+"_enabled",true).putBoolean(fx.name()+"_fired",false).apply();CambioWidget.syncAlertSchedule(c);}
    public static synchronized void disable(Context c,FxCurrency fx){prefs(c).edit().putBoolean(fx.name()+"_enabled",false).apply();c.getSystemService(NotificationManager.class).cancel(8000+fx.ordinal());CambioWidget.syncAlertSchedule(c);}
    public static void channel(Context c){c.getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Alertas de cotação PTAX",NotificationManager.IMPORTANCE_DEFAULT));}
    public static boolean permitted(Context c){channel(c);NotificationManager n=c.getSystemService(NotificationManager.class);return (Build.VERSION.SDK_INT<33||c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==android.content.pm.PackageManager.PERMISSION_GRANTED)&&n.areNotificationsEnabled()&&n.getNotificationChannel(CHANNEL).getImportance()!=NotificationManager.IMPORTANCE_NONE;}
    public static synchronized void evaluate(Context c,FxCurrency fx,List<Rate> rates){
        if(!enabled(c,fx)||rates.isEmpty()||prefs(c).getBoolean(fx.name()+"_fired",false)||!permitted(c))return;
        Rate latest=rates.get(rates.size()-1);BigDecimal limit;
        try{limit=threshold(prefs(c).getString(fx.name()+"_value",""));}catch(IllegalArgumentException e){return;}
        boolean above=prefs(c).getBoolean(fx.name()+"_above",true);if(!reached(latest.sell,limit,above))return;
        String text="PTAX venda: R$ "+latest.sell.toPlainString().replace('.',',')+" • publicada em "+latest.at.format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm"))+" (Brasília). Referência diária.";
        Intent open=new Intent(c,MainActivity.class).putExtra("currency",fx.name()).setData(android.net.Uri.parse("cambioclaro://alert/"+fx.name()));
        PendingIntent action=PendingIntent.getActivity(c,8000+fx.ordinal(),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_launcher).setContentTitle(fx.name()+": limite "+(above?"superior":"inferior")+" atingido").setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setContentIntent(action).setAutoCancel(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
        try{c.getSystemService(NotificationManager.class).notify(8000+fx.ordinal(),notification);prefs(c).edit().putBoolean(fx.name()+"_fired",true).apply();}catch(SecurityException ignored){/* Keep pending if permission changed during evaluation. */}
    }
}
