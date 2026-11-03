package br.com.cambioclaro.app;
import android.app.job.*;
import android.os.*;
import br.com.cambioclaro.app.market.application.MarketRefreshPolicy;
import java.util.concurrent.*;
public final class WidgetUpdateService extends JobService {
    private ExecutorService executor;
    private int generation=0;
    @Override public boolean onStartJob(JobParameters params){
        if(executor!=null&&!executor.isShutdown())return false;
        final int token=++generation;
        final java.util.Set<FxCurrency> targets=java.util.EnumSet.noneOf(FxCurrency.class);
        boolean hasTourism=false;for(int id:android.appwidget.AppWidgetManager.getInstance(this).getAppWidgetIds(new android.content.ComponentName(this,CambioWidget.class))){FxCurrency fx=Preferences.widget(this,id);targets.add(fx);if(Preferences.widgetTourism(this,id))hasTourism=true;}
        final boolean refreshTourism=hasTourism;
        targets.addAll(Alerts.currencies(this));
        CambioWidget.renderAll(this,"Consultando fontes…");
        executor=Executors.newSingleThreadExecutor();
        executor.execute(()->{
            String error=null;
            boolean refreshedMarket=false;
            if(MarketRefreshPolicy.isWidgetMarketRefreshDue(System.currentTimeMillis(),MarketStore.checked(this))){try{AndroidMarketRefreshGateway.create(getApplicationContext()).execute();refreshedMarket=true;}catch(Exception ex){Diagnostics.record(getApplicationContext(),"atualização de mercado do widget",ex);error="Consulta de mercado falhou";}}
            if(!refreshedMarket)for(FxCurrency fx:MarketAlerts.currencies(this))MarketAlerts.evaluate(this,fx,MarketStore.latest(this,fx));
            if(refreshTourism){try{Tourism.saveLatest(this,Tourism.fetchLatest());Tourism.saveHistory(this,Tourism.fetchHistory());}catch(Exception ex){Diagnostics.record(getApplicationContext(),"atualização de USD turismo do widget",ex);error="Consulta turismo falhou";}}
            for(FxCurrency currency:targets){
                if(Thread.currentThread().isInterrupted())return;
                try{long fetched=RateStore.prefs(this,currency).getLong("fetched",0);if(fetched==0||System.currentTimeMillis()-fetched>=6*60*60*1000L){String raw=BcbClient.fetch(BcbClient.today(),currency);if(Thread.currentThread().isInterrupted())return;RateStore.save(this,currency,raw,System.currentTimeMillis());}Alerts.evaluate(this,currency,RateStore.read(this,currency));}
                catch(Exception ex){Diagnostics.record(getApplicationContext(),"atualização de widget / PTAX "+currency.name(),ex);error="Uma consulta falhou • confira as datas; toque em ↻";}
            }
            final String message=error;
            new Handler(Looper.getMainLooper()).post(()->{
                if(generation!=token)return;
                CambioWidget.renderAll(this,message);jobFinished(params,message!=null);if(executor!=null)executor.shutdown();
            });
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters p){generation++;if(executor!=null)executor.shutdownNow();CambioWidget.renderAll(this,"Atualização interrompida • aguardando nova tentativa");return true;}
    @Override public void onDestroy(){generation++;if(executor!=null)executor.shutdownNow();super.onDestroy();}
}
