package br.com.cambioclaro.app;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
public final class WidgetConfigActivity extends Activity {
    @Override public void onCreate(Bundle state){Preferences.theme(this);super.onCreate(state);setResult(RESULT_CANCELED);
        int id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        AppWidgetManager manager=AppWidgetManager.getInstance(this);AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);
        if(info==null||!info.provider.equals(new ComponentName(this,CambioWidget.class))){finish();return;}
        Diagnostics.widgetEvent(this,"engrenagem aberta");
        String[] items=new String[FxCurrency.values().length+1];for(int i=0;i<FxCurrency.values().length;i++)items[i]=FxCurrency.values()[i].toString();items[items.length-1]="USD/BRLT • Dólar turismo";
        new AlertDialog.Builder(this).setTitle("Moeda deste widget").setSingleChoiceItems(items,Preferences.widgetTourism(this,id)?items.length-1:Preferences.widget(this,id).ordinal(),(dialog,which)->{
            boolean tourism=which==FxCurrency.values().length;Preferences.widget(this,id,tourism?FxCurrency.USD:FxCurrency.values()[which]);Preferences.widgetTourism(this,id,tourism);CambioWidget.renderAll(this,null);CambioWidget.schedule(this);
            setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id));finish();
        }).setNegativeButton("Cancelar",(d,w)->finish()).setOnCancelListener(d->finish()).show();
    }
}
