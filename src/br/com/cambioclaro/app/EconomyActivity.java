package br.com.cambioclaro.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;
import br.com.cambioclaro.app.market.application.RefreshSources;
import br.com.cambioclaro.app.economy.presentation.EconomyPresenter;
import br.com.cambioclaro.app.economy.presentation.EconomyDisplay;
import br.com.cambioclaro.app.economy.presentation.NewsDisplay;
import br.com.cambioclaro.app.economy.presentation.NewsPresenter;
import br.com.cambioclaro.app.economy.presentation.HistoryDisplay;
import br.com.cambioclaro.app.economy.presentation.EconomyHistoryPresenter;
import br.com.cambioclaro.app.economy.presentation.EconomicScenarioPresenter;
import br.com.cambioclaro.app.economy.presentation.EconomicScenarioDisplay;
import br.com.cambioclaro.app.economy.application.CalculateEconomicScenario;
import br.com.cambioclaro.app.economy.application.EconomicScenario;
import br.com.cambioclaro.app.economy.domain.EconomicSimulator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

/** Macro overview. Observed data and Focus market expectations are deliberately separate. */
public final class EconomyActivity extends Activity {
    private final ExecutorService worker=Executors.newFixedThreadPool(3);private final Handler main=new Handler(Looper.getMainLooper());
    private AndroidEconomySources economySources;private final RefreshSources refreshSources=new RefreshSources();private final EconomyPresenter economyPresenter=new EconomyPresenter();private final NewsPresenter newsPresenter=new NewsPresenter();private final EconomyHistoryPresenter historyPresenter=new EconomyHistoryPresenter();private final EconomicScenarioPresenter scenarioPresenter=new EconomicScenarioPresenter();private CalculateEconomicScenario scenarioUseCase;
    private UiStyle style;private LinearLayout body,newsItems,globalNewsItems,historyItems;private TextView status,selic,ipca,focusIpca,focusSelic,focusFx,focusPib,scenarioStatus,scenarioResult,newsStatus,globalNewsStatus,historyStatus;private Button refresh,calculateScenario;private Spinner scenarioCurrency,scenarioSource;private EditText scenarioAmount,scenarioSpread,scenarioExtra,scenarioFixed;private boolean loading,scenarioLoading,newsLoading,globalNewsLoading,historyLoading;
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTypeface(android.graphics.Typeface.create("sans-serif",bold?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL));style.text(t);return t;}
    private EditText input(String hint,String description){EditText e=new EditText(this);style.input(e);e.setHint(hint);e.setContentDescription(description);e.setSingleLine(true);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});return e;}
    private void add(LinearLayout parent,android.view.View child,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);parent.addView(child,p);}
    private LinearLayout card(String title){LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(16),dp(15),dp(16),dp(15));panel.setBackground(style.shape(style.surface));add(body,panel,10);add(panel,text(title,15,true),0);return panel;}
    @Override public void onCreate(Bundle state){Preferences.theme(this);super.onCreate(state);economySources=new AndroidEconomySources(getApplicationContext());scenarioUseCase=new CalculateEconomicScenario(new AndroidEconomicQuoteProvider(getApplicationContext()));style=new UiStyle(this);ScrollView scroll=new ScrollView(this);style.root(scroll);body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(20),dp(22),dp(20),dp(28));style.root(body);scroll.addView(body);setContentView(scroll);UiBars.apply(this,style.light);if(android.os.Build.VERSION.SDK_INT>=30)scroll.setOnApplyWindowInsetsListener((v,in)->{android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return in;});
        Button back=new Button(this);back.setText("← Voltar");style.button(back);body.addView(back);back.setOnClickListener(v->finish());add(body,text("Economia brasileira",25,true),12);add(body,text("Indicadores oficiais e expectativas de mercado. Cada número informa sua própria data de referência.",14,false),5);
        refresh=new Button(this);refresh.setText("Atualizar indicadores e notícias");style.button(refresh);add(body,refresh,8);refresh.setOnClickListener(v->{load(true);loadNews(true);loadGlobalNews(true);loadHistory(true);});status=text("Carregando dados salvos…",12,false);add(body,status,10);
        LinearLayout actual=card("DADOS OBSERVADOS · BANCO CENTRAL");selic=text("Meta Selic\nSem dado válido",17,true);add(actual,selic,10);ipca=text("IPCA mensal\nSem dado válido",17,true);add(actual,ipca,12);Button actualSource=new Button(this);actualSource.setText("Abrir séries oficiais SGS");style.button(actualSource);add(actual,actualSource,8);actualSource.setOnClickListener(v->open("https://dadosabertos.bcb.gov.br/dataset?res_format=API"));
        LinearLayout expectations=card("EXPECTATIVAS DO MERCADO · FOCUS");add(expectations,text("Medianas das instituições participantes; não são previsões do Banco Central.",13,false),6);focusIpca=text("IPCA · indisponível",16,true);add(expectations,focusIpca,10);focusSelic=text("Selic · indisponível",16,true);add(expectations,focusSelic,8);focusFx=text("Câmbio · indisponível",16,true);add(expectations,focusFx,8);focusPib=text("PIB · indisponível",16,true);add(expectations,focusPib,8);Button focusSource=new Button(this);focusSource.setText("Abrir Relatório Focus oficial");style.button(focusSource);add(expectations,focusSource,8);focusSource.setOnClickListener(v->open(EconomyClient.FOCUS_PAGE));
        LinearLayout history=card("HISTÓRICO · ÚLTIMAS 12 COMPETÊNCIAS");add(history,text("IPCA é a variação mensal. Meta Selic é anualizada e mostra a última observação do mês. As medidas têm unidades diferentes; valores ausentes não são interpolados.",13,false),6);historyStatus=text("Consultando séries SGS…",12,false);add(history,historyStatus,8);historyItems=new LinearLayout(this);historyItems.setOrientation(1);add(history,historyItems,4);Button historySource=new Button(this);historySource.setText("Abrir histórico oficial SGS");style.button(historySource);add(history,historySource,8);historySource.setOnClickListener(v->open("https://dadosabertos.bcb.gov.br/dataset?res_format=API"));
        LinearLayout simulator=card("SIMULADOR DE CUSTO DE CÂMBIO");add(simulator,text("Calcule um cenário usando uma cotação publicada e os encargos que você informar. Não é uma oferta nem o preço final de uma instituição.",13,false),6);
        scenarioCurrency=new Spinner(this);scenarioCurrency.setContentDescription("Moeda do cenário");style.spinner(scenarioCurrency,FxCurrency.values());scenarioCurrency.setSelection(FxCurrency.USD.ordinal());simulator.addView(scenarioCurrency);
        scenarioSource=new Spinner(this);scenarioSource.setContentDescription("Fonte da taxa de referência");style.spinner(scenarioSource,new String[]{"Mercado · venda (ask) · AwesomeAPI","PTAX · venda · Banco Central"});simulator.addView(scenarioSource);
        add(simulator,text("Quantidade da moeda estrangeira",13,false),8);scenarioAmount=input("Ex.: 100,00","Quantidade de moeda estrangeira");simulator.addView(scenarioAmount);
        add(simulator,text("Spread informado pelo seu banco (%)",13,false),8);scenarioSpread=input("Ex.: 2,00","Spread percentual informado");scenarioSpread.setText("0");simulator.addView(scenarioSpread);
        add(simulator,text("Outros encargos percentuais informados (%)",13,false),8);scenarioExtra=input("Ex.: 1,10","Outros encargos percentuais informados");scenarioExtra.setText("0");simulator.addView(scenarioExtra);
        add(simulator,text("Tarifas fixas informadas (R$)",13,false),8);scenarioFixed=input("Ex.: 12,00","Tarifa fixa em reais informada");scenarioFixed.setText("0");simulator.addView(scenarioFixed);
        calculateScenario=new Button(this);calculateScenario.setText("Buscar taxa e calcular cenário");style.button(calculateScenario);add(simulator,calculateScenario,8);calculateScenario.setOnClickListener(v->calculateScenario());scenarioStatus=text("Os encargos começam em zero apenas como entrada do usuário; isso não significa que sejam zero no banco ou cartão.",12,false);add(simulator,scenarioStatus,8);scenarioResult=text("Informe a quantidade e calcule para consultar uma fonte.",16,true);add(simulator,scenarioResult,8);
        LinearLayout news=card("BRASIL · BANCO CENTRAL");add(news,text("Notas oficiais do BC, com data e link para a publicação original.",13,false),6);newsStatus=text("Carregando comunicados…",12,false);add(news,newsStatus,8);newsItems=new LinearLayout(this);newsItems.setOrientation(1);add(news,newsItems,4);Button newsSource=new Button(this);newsSource.setText("Ver feeds oficiais do BC");style.button(newsSource);add(news,newsSource,8);newsSource.setOnClickListener(v->open("https://www.bcb.gov.br/acessoinformacao/rss"));Button b3News=new Button(this);b3News.setText("Notícias da B3");style.button(b3News);add(news,b3News,5);b3News.setOnClickListener(v->open(MarketNewsLinks.B3));
        LinearLayout world=card("EXTERIOR · FONTES OFICIAIS");add(world,text("Comunicados do Fed e do BCE para acompanhar política monetária. São publicações originais; o app não presume nem prevê a reação dos mercados.",13,false),6);globalNewsStatus=text("Carregando comunicados internacionais…",12,false);add(world,globalNewsStatus,8);globalNewsItems=new LinearLayout(this);globalNewsItems.setOrientation(1);add(world,globalNewsItems,4);Button blsCpi=new Button(this);blsCpi.setText("Inflação dos EUA · BLS");style.button(blsCpi);add(world,blsCpi,7);blsCpi.setOnClickListener(v->open(MarketNewsLinks.BLS_CPI));Button blsJobs=new Button(this);blsJobs.setText("Emprego dos EUA · BLS");style.button(blsJobs);add(world,blsJobs,5);blsJobs.setOnClickListener(v->open(MarketNewsLinks.BLS_EMPLOYMENT));
        LinearLayout agenda=card("AGENDA ECONÔMICA OFICIAL");add(agenda,text("Consulte as datas mantidas pelo Banco Central. A agenda abre no site oficial para refletir alterações sem usar datas copiadas ou estimadas.",13,false),6);Button copomCalendar=new Button(this);copomCalendar.setText("Reuniões do Copom");style.button(copomCalendar);add(agenda,copomCalendar,8);copomCalendar.setOnClickListener(v->open(EconomicAgenda.COPOM));Button releasesCalendar=new Button(this);releasesCalendar.setText("Divulgações macroeconômicas");style.button(releasesCalendar);add(agenda,releasesCalendar,6);releasesCalendar.setOnClickListener(v->open(EconomicAgenda.RELEASES));
        add(body,text("Atualização ao abrir ou por solicitação. As séries têm frequências próprias: a Selic muda por decisão do Copom e o IPCA é mensal. Dados salvos podem estar defasados quando a fonte falha.",12,false),12);
        render(EconomyClient.cached(this,BcbClient.today()),null);renderNews(BcbNews.cached(this,java.time.Instant.now()),null,BcbNews.cachedAt(this));renderGlobalNews(cachedGlobalNews(),null);renderHistory(EconomyHistory.cached(this,BcbClient.today()),null,EconomyHistory.cachedAt(this));load(false);loadNews(false);loadGlobalNews(false);loadHistory(false);
    }
    private void loadGlobalNews(boolean force){if(globalNewsLoading)return;long now=System.currentTimeMillis();if(!force&&GlobalNews.fresh(this,now))return;globalNewsLoading=true;globalNewsStatus.setText("Consultando feeds oficiais do exterior…");worker.execute(()->{RefreshSources.Result result=refreshSources.execute(economySources.globalNews(force,now));String error=sourceErrors(result," indisponível");for(RefreshSources.Failure failure:result.failures)Diagnostics.record(getApplicationContext(),failure.sourceId,failure.cause);List<GlobalNews.Item> items=cachedGlobalNews();long at=Math.max(GlobalNews.cachedAt(getApplicationContext(),"fed"),GlobalNews.cachedAt(getApplicationContext(),"ecb"));main.post(()->{globalNewsLoading=false;if(isFinishing()||isDestroyed())return;renderGlobalNews(items,error,at);});});}
    private List<GlobalNews.Item> cachedGlobalNews(){List<GlobalNews.Item> items=new java.util.ArrayList<>();for(GlobalNews.Source source:GlobalNews.SOURCES)items.addAll(GlobalNews.cached(this,source.id,java.time.Instant.now()));items.sort((a,b)->b.published.compareTo(a.published));return items;}
    private void renderGlobalNews(List<GlobalNews.Item> items,String failure){renderGlobalNews(items,failure,Math.max(GlobalNews.cachedAt(this,"fed"),GlobalNews.cachedAt(this,"ecb")));}
    private void renderGlobalNews(List<GlobalNews.Item> items,String failure,long fetchedAt){
        if(globalNewsItems==null||globalNewsStatus==null)return;
        List<NewsPresenter.Publication> publications=new java.util.ArrayList<>();
        if(items!=null)for(GlobalNews.Item item:items)publications.add(new NewsPresenter.Publication(item.source,item.title,item.url,item.published));
        NewsDisplay display=newsPresenter.present(publications,8,fetchedAt,failure,"Sem publicações internacionais disponíveis.","publicações internacionais · títulos no idioma da fonte",BcbClient.ZONE,System.currentTimeMillis());
        globalNewsStatus.setText(display.status);bindNews(globalNewsItems,display);
    }
    private void loadHistory(boolean force){if(historyLoading)return;if(!force&&EconomyHistory.fresh(this,System.currentTimeMillis()))return;historyLoading=true;historyStatus.setText("Consultando histórico SGS oficial…");worker.execute(()->{LocalDate today=BcbClient.today();RefreshSources.Result refreshed=refreshSources.execute(economySources.history(today));long at=System.currentTimeMillis();EconomyHistory.markAttempt(getApplicationContext(),at);List<EconomyHistory.Month> result=EconomyHistory.cached(getApplicationContext(),today);String error=sourceErrors(refreshed," indisponível");for(RefreshSources.Failure failure:refreshed.failures)Diagnostics.record(getApplicationContext(),"histórico econômico / "+failure.sourceId,failure.cause);final String errors=error;final long fetched=EconomyHistory.cachedAt(getApplicationContext());main.post(()->{historyLoading=false;if(isFinishing()||isDestroyed())return;renderHistory(result,errors,fetched);});});}
    private void renderHistory(List<EconomyHistory.Month> months,String failure,long fetchedAt){
        if(historyItems==null||historyStatus==null)return;
        List<EconomyHistoryPresenter.Month> values=new java.util.ArrayList<>();
        if(months!=null)for(EconomyHistory.Month month:months){
            EconomyHistoryPresenter.Observation ipca=month.ipca==null?null:new EconomyHistoryPresenter.Observation(month.ipca.date,month.ipca.value);
            EconomyHistoryPresenter.Observation selic=month.selic==null?null:new EconomyHistoryPresenter.Observation(month.selic.date,month.selic.value);
            values.add(new EconomyHistoryPresenter.Month(month.month,ipca,selic));
        }
        HistoryDisplay display=historyPresenter.present(values,failure,fetchedAt,System.currentTimeMillis(),BcbClient.ZONE);
        historyStatus.setText(display.status);historyItems.removeAllViews();
        for(HistoryDisplay.Month month:display.months){TextView title=text(month.title,14,true);add(historyItems,title,8);add(historyItems,text(month.observations,13,false),3);}
    }
    private void loadNews(boolean force){if(newsLoading)return;if(!force&&BcbNews.fresh(this,System.currentTimeMillis()))return;newsLoading=true;newsStatus.setText("Consultando feed oficial de comunicados…");worker.execute(()->{RefreshSources.Result refreshed=refreshSources.execute(economySources.bcbNews(BcbClient.today().getYear(),java.time.Instant.now()));for(RefreshSources.Failure failure:refreshed.failures)Diagnostics.record(getApplicationContext(),"feed de comunicados BCB",failure.cause);List<BcbNews.Item> entries=BcbNews.cached(getApplicationContext(),java.time.Instant.now());String failure=refreshed.hasFailures()?"Comunicados indisponíveis agora; tentando usar a última cópia válida.":null;final String error=failure;final long at=BcbNews.cachedAt(getApplicationContext());main.post(()->{newsLoading=false;if(isFinishing()||isDestroyed())return;renderNews(entries,error,at);});});}
    private void renderNews(List<BcbNews.Item> entries,String failure,long fetchedAt){
        if(newsItems==null||newsStatus==null)return;
        List<NewsPresenter.Publication> publications=new java.util.ArrayList<>();
        if(entries!=null)for(BcbNews.Item item:entries)publications.add(new NewsPresenter.Publication("Banco Central",item.title,item.url,item.published));
        NewsDisplay display=newsPresenter.present(publications,5,fetchedAt,failure,"Nenhum comunicado disponível na fonte.","publicações do BC",BcbClient.ZONE,System.currentTimeMillis());
        newsStatus.setText(display.status);bindNews(newsItems,display);
    }
    private void bindNews(LinearLayout parent,NewsDisplay display){
        parent.removeAllViews();
        for(NewsDisplay.Item item:display.items){Button headline=new Button(this);headline.setText(item.text);headline.setAllCaps(false);headline.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);style.button(headline);add(parent,headline,5);headline.setOnClickListener(v->open(item.url));}
    }
    private void load(boolean force){if(loading)return;if(!force&&EconomyClient.cacheFresh(this,System.currentTimeMillis())){status.setText("Consulta recente · os indicadores seguem a frequência de cada fonte.");return;}loading=true;refresh.setEnabled(false);refresh.setText("Consultando fontes oficiais…");status.setText("Consultando SGS e Focus por HTTPS…");worker.execute(()->{LocalDate today=BcbClient.today();RefreshSources.Result refreshed=refreshSources.execute(economySources.overview(today));long attempted=System.currentTimeMillis();EconomyClient.markAttempt(getApplicationContext(),attempted);EconomyClient.Snapshot result=EconomyClient.cached(getApplicationContext(),today);String failure=sourceErrors(refreshed," indisponível");for(RefreshSources.Failure item:refreshed.failures)Diagnostics.record(getApplicationContext(),"painel econômico / "+item.sourceId,item.cause);final EconomyClient.Snapshot display=result;final String errors=failure;main.post(()->{loading=false;if(isFinishing()||isDestroyed())return;refresh.setEnabled(true);refresh.setText("Atualizar indicadores");render(display,errors);});});}
    private String sourceErrors(RefreshSources.Result result,String suffix){if(!result.hasFailures())return null;StringBuilder message=new StringBuilder();for(RefreshSources.Failure failure:result.failures){if(message.length()>0)message.append("; ");message.append(failure.sourceId).append(suffix);}return message.toString();}
    private void render(EconomyClient.Snapshot data,String failure){EconomyPresenter.Metric selicMetric=data==null||data.selic==null?null:new EconomyPresenter.Metric(data.selic.value,data.selic.date);EconomyPresenter.Metric ipcaMetric=data==null||data.ipca==null?null:new EconomyPresenter.Metric(data.ipca.value,data.ipca.date);java.util.Map<String,EconomyPresenter.Expectation> expected=new java.util.LinkedHashMap<>();if(data!=null)for(java.util.Map.Entry<String,EconomyClient.Point> entry:data.expectations.entrySet()){EconomyClient.Point p=entry.getValue();expected.put(entry.getKey(),new EconomyPresenter.Expectation(p.value,p.unit,p.period,p.date,p.respondents));}EconomyDisplay display=economyPresenter.present(selicMetric,ipcaMetric,expected,data==null?0:data.fetchedAt,System.currentTimeMillis(),failure,BcbClient.ZONE);selic.setText(display.selic);ipca.setText(display.ipca);focusIpca.setText(display.focusIpca);focusSelic.setText(display.focusSelic);focusFx.setText(display.focusFx);focusPib.setText(display.focusPib);status.setText(display.status);}
    private void calculateScenario(){
        if(scenarioLoading)return;
        final FxCurrency currency=(FxCurrency)scenarioCurrency.getSelectedItem();
        final boolean marketSource=scenarioSource.getSelectedItemPosition()==0;
        final BigDecimal amount,spread,extra,fixed;
        try{
            amount=EconomicSimulator.parse(scenarioAmount.getText().toString(),6,false);
            spread=EconomicSimulator.parse(scenarioSpread.getText().toString(),2,true);
            extra=EconomicSimulator.parse(scenarioExtra.getText().toString(),2,true);
            fixed=EconomicSimulator.parse(scenarioFixed.getText().toString(),2,true);
        }catch(IllegalArgumentException invalid){scenarioResult.setText(invalid.getMessage());return;}
        scenarioLoading=true;calculateScenario.setEnabled(false);calculateScenario.setText("Consultando referência…");scenarioStatus.setText("Buscando a taxa selecionada por HTTPS.");
        worker.execute(()->{
            try{
                EconomicScenario scenario=scenarioUseCase.execute(currency,marketSource,amount,spread,extra,fixed,java.time.Instant.now());
                main.post(()->showScenario(scenario));
            }catch(Exception unavailable){
                Diagnostics.record(getApplicationContext(),"simulador econômico",unavailable);
                main.post(()->showScenarioUnavailable());
            }
        });
    }
    private void showScenario(EconomicScenario scenario){
        scenarioLoading=false;if(isFinishing()||isDestroyed())return;
        calculateScenario.setEnabled(true);calculateScenario.setText("Buscar taxa e calcular cenário");
        EconomicScenarioDisplay display=scenarioPresenter.present(scenario,BcbClient.ZONE);scenarioStatus.setText(display.status);scenarioResult.setText(display.details);
    }
    private void showScenarioUnavailable(){
        scenarioLoading=false;if(isFinishing()||isDestroyed())return;
        calculateScenario.setEnabled(true);calculateScenario.setText("Buscar taxa e calcular cenário");
        EconomicScenarioDisplay display=scenarioPresenter.unavailable();scenarioStatus.setText(display.status);scenarioResult.setText(display.details);
    }
    private void open(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ex){status.setText("Não foi possível abrir a página da fonte neste aparelho.");}}
    @Override protected void onDestroy(){worker.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
}
