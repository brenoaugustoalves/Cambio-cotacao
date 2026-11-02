package br.com.cambioclaro.app;
import android.content.*;
import android.content.res.Configuration;
import java.util.*;
public final class Preferences {
    public static SharedPreferences prefs(Context c){return c.getSharedPreferences("preferences",Context.MODE_PRIVATE);}
    public static boolean favorite(Context c,FxCurrency fx){return prefs(c).getBoolean("favorite_"+fx.name(),false);}
    public static void favorite(Context c,FxCurrency fx,boolean value){prefs(c).edit().putBoolean("favorite_"+fx.name(),value).apply();}
    public static List<FxCurrency> ordered(Context c){List<FxCurrency> list=new ArrayList<>(Arrays.asList(FxCurrency.values()));Collections.sort(list,(a,b)->Boolean.compare(favorite(c,b),favorite(c,a)));return list;}
    public static void initializeAppearance(Context c){if(!prefs(c).getBoolean("appearance_v112",false))prefs(c).edit().putString("theme","dark").putBoolean("appearance_v112",true).apply();}
    public static boolean light(Context c){String mode=prefs(c).getString("theme","dark");return mode.equals("light")||mode.equals("system")&&(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)!=Configuration.UI_MODE_NIGHT_YES;}
    public static void theme(android.app.Activity a){a.setTheme(light(a)?R.style.AppTheme:R.style.DarkTheme);}
    public static FxCurrency widget(Context c,int id){try{return FxCurrency.valueOf(prefs(c).getString("widget_"+id,RateStore.active(c).name()));}catch(IllegalArgumentException e){return FxCurrency.USD;}}
    public static void widget(Context c,int id,FxCurrency fx){prefs(c).edit().putString("widget_"+id,fx.name()).apply();}
    public static boolean widgetTourism(Context c,int id){return prefs(c).getBoolean("widget_tourism_"+id,false);}
    public static void widgetTourism(Context c,int id,boolean tourism){prefs(c).edit().putBoolean("widget_tourism_"+id,tourism).apply();}
}
