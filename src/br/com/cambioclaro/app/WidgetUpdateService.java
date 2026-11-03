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
            if(refreshTourism){br.com.cambioclaro.app.market.application.TourismRefreshResult tourism=AndroidTourismGateway.create(getApplicationContext()).execute(System.currentTimeMillis(),1,true);if(tourism.latestError!=null){Diagnostics.record(getApplicationContext(),"atualização USD turismo do widget",tourism.latestError);error="Cotação turismo indisponível";}if(tourism.historyError!=null){Diagnostics.record(getApplicationContext(),"histórico USD turismo do widget",tourism.historyError);error="Histórico turismo indisponível";}}
            br.com.cambioclaro.app.market.application.StorePtaxRates ptaxStore=AndroidPtaxGateway.createStore(getApplicationContext());
            for(FxCurrency currency:targets){
                if(Thread.currentThread().isInterrupted())return;
                try{long fetched=RateStore.prefs(this,currency).getLong("fetched",0);if(fetched==0||System.currentTimeMillis()-fetched>=6*60*60*1000L){ptaxStore.execute(currency,BcbClient.today());if(Thread.currentThread().isInterrupted())return;}Alerts.evaluate(this,currency,RateStore.read(this,currency));}
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
