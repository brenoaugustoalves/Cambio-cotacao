package br.com.cambioclaro.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.text.*;
import android.view.*;
import android.widget.*;
import br.com.cambioclaro.app.conversion.application.ConvertCurrency;
import br.com.cambioclaro.app.conversion.data.AndroidConversionQuoteSource;
import br.com.cambioclaro.app.conversion.domain.ConversionRequest;
import br.com.cambioclaro.app.conversion.domain.ConversionResult;
import br.com.cambioclaro.app.conversion.presentation.ConversionDisplay;
import br.com.cambioclaro.app.conversion.presentation.ConversionPresenter;
import br.com.cambioclaro.app.market.application.MarketRefreshPolicy;
import br.com.cambioclaro.app.market.application.RefreshMarketQuotes;
import br.com.cambioclaro.app.market.application.LatestRequestCoordinator;
import br.com.cambioclaro.app.market.application.RefreshStockQuotes;
import br.com.cambioclaro.app.market.application.StockRefreshResult;
import br.com.cambioclaro.app.market.application.RefreshMarketHistory;
import br.com.cambioclaro.app.market.application.RefreshPtaxRates;
import br.com.cambioclaro.app.market.application.StorePtaxRates;
import br.com.cambioclaro.app.market.application.StoredPtaxRates;
import br.com.cambioclaro.app.market.application.RefreshTourismQuotes;
import br.com.cambioclaro.app.market.application.TourismRefreshResult;
import br.com.cambioclaro.app.market.application.PtaxRefreshPolicy;
import br.com.cambioclaro.app.market.domain.Comparison;
import br.com.cambioclaro.app.market.domain.MarketObservation;
import br.com.cambioclaro.app.market.presentation.MarketQuotePresenter;
import br.com.cambioclaro.app.market.presentation.MarketQuoteDisplay;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private int BG=0xFF0A141C,CARD=0xFF12232E,TEXT=0xFFF2F6F8,MUTED=0xFF9FB3C1,MINT=0xFF66E0BF,AMBER=0xFFF3C976;
    private final Locale br=new Locale("pt","BR");
    private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private final LatestRequestCoordinator ptaxRequests=new LatestRequestCoordinator();
    private FxCurrency currency=FxCurrency.USD;
    private boolean tourismMode=false,tourismLoading=false;
    private FxCurrency converterCurrency=FxCurrency.USD;
    private boolean converterTourismMode=false;
    private ConvertCurrency conversionUseCase;
    private RefreshMarketQuotes marketQuoteRefresh;
    private RefreshStockQuotes stockQuoteRefresh;
    private RefreshMarketHistory marketHistoryRefresh;
    private StorePtaxRates ptaxStore;
    private RefreshPtaxRates ptaxRefresh;
    private RefreshTourismQuotes tourismRefresh;
    private final MarketQuotePresenter marketQuotePresenter=new MarketQuotePresenter();
    private boolean ptaxOpen=false;
    public static final long AUTO_REFRESH_MS=MarketRefreshPolicy.foregroundInterval(false,false);
    private static final long FULL_REFRESH_MS=MarketRefreshPolicy.FULL_REFRESH_INTERVAL_MS;
    private LinearLayout usdModes;private Button commercialButton,tourismButton;
    private long lastHistoryRefresh=0,lastStocksRefresh=0;
    private long lastFullRefresh=0;
    private final Handler autoHandler=new Handler(Looper.getMainLooper());
    private volatile boolean foreground=false;
    private boolean overviewLoading=false;
    private boolean marketLoading=false;
    private boolean stocksLoading=false;
    private TextView stocksStatus;private final Map<String,TextView> stockPrices=new LinkedHashMap<>(),stockChanges=new LinkedHashMap<>();
    private final Map<String,Spinner> futuresSelectors=new LinkedHashMap<>();
    private final Map<String,TextView> futuresDetails=new LinkedHashMap<>();
    private final Set<FxCurrency> historyLoading=EnumSet.noneOf(FxCurrency.class);
    private final Set<String> dailyHistoryLoading=new HashSet<>();
    private Map<FxCurrency,MarketClient.Quote> market=Collections.emptyMap();
    private TextView marketPair,marketRate,marketDate,marketStatus,marketMove;
    private TextView marketSourceNote,tourismOverviewPrice,tourismOverviewDate;
    private TextView intradayStatus,intradaySourceNote,intradayMovement;private IntradayView intradayChart;private Button intradayChangesToggle;private LinearLayout intradayChanges;private boolean intradayChangesExpanded=false;private int marketPeriodDays=1;private final Map<Integer,Button> marketPeriods=new LinkedHashMap<>();
    private List<MarketHistory.Point> currentIntradayPoints=Collections.emptyList();
    private final Map<FxCurrency,TextView> overviewPrices=new LinkedHashMap<>(),overviewChanges=new LinkedHashMap<>();
    private final Map<FxCurrency,String> overviewErrors=new HashMap<>();
    private Spinner selector;
    private Button favoriteButton;
    private String themeMode;
    private boolean light;
    private LinearLayout overviewContainer;
    private LinearLayout currencySection,marketsSection,sourcesSection;
    private final Map<String,Button> sectionButtons=new LinkedHashMap<>();
    private LinearLayout currencyQuoteSection,marketChartSection,ptaxSection,converterSection,compareSection;
    private final Map<String,Button> currencySectionButtons=new LinkedHashMap<>();
    private String selectedSection="Moedas";
    private String selectedCurrencySection="Cotação";
    private LinearLayout overviewList;
    private Button overviewMore;
    private boolean overviewExpanded=false;
    private final Map<FxCurrency,View> overviewRows=new LinkedHashMap<>();
    private final Map<FxCurrency,TextView> overviewTitles=new LinkedHashMap<>();
    private ScrollView mainScroll;
    private final Runnable autoRefresh=new Runnable(){public void run(){if(foreground){long now=System.currentTimeMillis(),interval=foregroundRefreshInterval();if(MarketRefreshPolicy.isFullRefreshDue(now,lastFullRefresh))load();else{loadMarket();if(now-lastHistoryRefresh>=historyRefreshInterval()){if(tourismMode)loadTourism();else loadHistory(currency);lastHistoryRefresh=now;}if(now-lastStocksRefresh>=interval*5){loadStocks();lastStocksRefresh=now;}}autoHandler.postDelayed(this,interval);}}};
    private List<Rate> rates=new ArrayList<>(),visible=new ArrayList<>();
    private long fetchedAt=0;
    private int days=30;
    private boolean loading=false,confirmed=false;
    private TextView rateText,quoteDate,deltaText,statusText,selectedText,rangeText,minimum,maximum,resultText,convertNote,countText;
    private TextView pairText,unitText,chartUnit,converterTitle,amountLabel;
    private EditText amount;
    private Spinner converterSelector;
    private LinearLayout converterUsdModes;
    private Button converterCommercialButton,converterTourismButton;
    private Button refresh;
    private ChartView chart;
    private final Map<Integer,Button> filters=new LinkedHashMap<>();
    private android.content.SharedPreferences prefs;
    private DateTimeFormatter dayFormat=DateTimeFormatter.ofPattern("dd/MM/uuuu",br);
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private boolean meteredNetwork(){try{ConnectivityManager cm=getSystemService(ConnectivityManager.class);return cm!=null&&cm.isActiveNetworkMetered();}catch(Exception ignored){return true;}}
    private boolean powerSaveMode(){try{PowerManager pm=getSystemService(PowerManager.class);return pm!=null&&pm.isPowerSaveMode();}catch(Exception ignored){return false;}}
    private long foregroundRefreshInterval(){return MarketRefreshPolicy.foregroundInterval(meteredNetwork(),powerSaveMode());}
    private long historyRefreshInterval(){return MarketRefreshPolicy.historyInterval(meteredNetwork());}
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private TextView label(String value,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setFontFeatureSettings("tnum");v.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));v.setLineSpacing(dp(2),1);return v;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void showMainSection(String destination){
        if(currencySection==null)return;selectedSection=destination;
        currencySection.setVisibility("Moedas".equals(destination)?View.VISIBLE:View.GONE);
        marketsSection.setVisibility("Bolsa".equals(destination)?View.VISIBLE:View.GONE);
        sourcesSection.setVisibility("Fontes".equals(destination)?View.VISIBLE:View.GONE);
        for(Map.Entry<String,Button> entry:sectionButtons.entrySet()){Button tab=entry.getValue();boolean active=entry.getKey().equals(destination);tab.setSelected(active);tab.setBackground(shape(active?MINT:(light?0xFFE1ECEF:0xFF203641),14));tab.setTextColor(active?(light?0xFFFFFFFF:BG):MUTED);tab.setContentDescription(entry.getKey()+(active?", seção selecionada":", abrir seção"));}
        if(mainScroll!=null)mainScroll.smoothScrollTo(0,0);
    }
    private void showCurrencySection(String destination){
        if(currencyQuoteSection==null)return;selectedCurrencySection=destination;
        currencyQuoteSection.setVisibility("Cotação".equals(destination)?View.VISIBLE:View.GONE);
        marketChartSection.setVisibility("Gráfico".equals(destination)?View.VISIBLE:View.GONE);
        ptaxSection.setVisibility("PTAX".equals(destination)?View.VISIBLE:View.GONE);
        converterSection.setVisibility("Converter".equals(destination)?View.VISIBLE:View.GONE);
        compareSection.setVisibility("Outras".equals(destination)?View.VISIBLE:View.GONE);
        for(Map.Entry<String,Button> entry:currencySectionButtons.entrySet()){Button tab=entry.getValue();boolean active=entry.getKey().equals(destination);tab.setSelected(active);tab.setBackground(shape(active?MINT:(light?0xFFE1ECEF:0xFF203641),12));tab.setTextColor(active?(light?0xFFFFFFFF:BG):MUTED);tab.setContentDescription(entry.getKey()+(active?", assunto selecionado":", abrir assunto"));}if(mainScroll!=null)mainScroll.smoothScrollTo(0,0);
    }
    private void add(LinearLayout parent,View child,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);parent.addView(child,p);}
    private LinearLayout panel(LinearLayout parent,int color){LinearLayout l=column();l.setPadding(dp(16),dp(17),dp(16),dp(17));l.setBackground(shape(color,20));add(parent,l,12);return l;}
    private Button button(String value){Button b=new Button(this);b.setText(value);b.setTextSize(13);b.setAllCaps(false);b.setTextColor(MINT);b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(10),0,dp(10),0);b.setBackground(shape(light?0xFFE1ECEF:0xFF203641,14));return b;}
    private String money(BigDecimal n,int digits){NumberFormat f=NumberFormat.getNumberInstance(br);f.setMinimumFractionDigits(digits);f.setMaximumFractionDigits(digits);return "R$ "+f.format(n);}
    private String percent(BigDecimal n){BigDecimal shown=n.setScale(2,java.math.RoundingMode.HALF_UP);NumberFormat f=NumberFormat.getNumberInstance(br);f.setMinimumFractionDigits(2);f.setMaximumFractionDigits(2);return (shown.signum()>0?"+":"")+f.format(shown)+"%";}
    private void setMarketFreshness(String text,boolean stale){if(marketStatus==null)return;marketStatus.setText(text);marketStatus.setTextColor(stale?AMBER:(light?0xFF086A53:MINT));marketStatus.setBackground(shape(stale?(light?0xFFFFF1CC:0xFF3A3020):(light?0xFFE5F3EE:0xFF173931),10));marketStatus.setContentDescription("Estado da cotação: "+text);}
    @Override public void onCreate(Bundle saved){
        Preferences.theme(this);super.onCreate(saved);light=Preferences.light(this);themeMode=Preferences.prefs(this).getString("theme","system");
        Uri launchData=getIntent().getData();if(launchData!=null&&"cambioclaro".equals(launchData.getScheme())&&"widget".equals(launchData.getHost()))Diagnostics.widgetEvent(this,"cotacao tocada; tela aberta");
        if(light){BG=0xFFF1F5F7;CARD=0xFFFFFFFF;TEXT=0xFF132A35;MUTED=0xFF476271;MINT=0xFF086A53;AMBER=0xFF805300;}
        String incoming=getIntent().getStringExtra("currency");if(incoming!=null)try{RateStore.select(this,FxCurrency.valueOf(incoming));}catch(IllegalArgumentException ignored){}
        boolean hasTourism=getIntent().hasExtra("tourism"),incomingTourism=getIntent().getBooleanExtra("tourism",false);getIntent().removeExtra("currency");getIntent().removeExtra("tourism");currency=RateStore.active(this);tourismMode=currency==FxCurrency.USD&&(hasTourism?incomingTourism:Preferences.prefs(this).getBoolean("usd_tourism",false));if(hasTourism)Preferences.prefs(this).edit().putBoolean("usd_tourism",incomingTourism).apply();prefs=RateStore.prefs(this,currency);conversionUseCase=new ConvertCurrency(new AndroidConversionQuoteSource(getApplicationContext()));marketQuoteRefresh=AndroidMarketRefreshGateway.create(getApplicationContext());stockQuoteRefresh=new RefreshStockQuotes(new AndroidStockQuoteGateway(getApplicationContext()));marketHistoryRefresh=AndroidMarketHistoryGateway.create(getApplicationContext());ptaxStore=AndroidPtaxGateway.createStore(getApplicationContext());ptaxRefresh=AndroidPtaxGateway.create(getApplicationContext());tourismRefresh=AndroidTourismGateway.create(getApplicationContext());
        try{converterCurrency=FxCurrency.valueOf(Preferences.prefs(this).getString("converter_currency",currency.name()));}catch(IllegalArgumentException ignored){converterCurrency=currency;}converterTourismMode=converterCurrency==FxCurrency.USD&&Preferences.prefs(this).getBoolean("converter_usd_tourism",tourismMode);
        if(saved!=null){days=saved.getInt("days",30);ptaxOpen=saved.getBoolean("ptaxOpen",false);marketPeriodDays=saved.getInt("marketPeriodDays",1);selectedSection=saved.getString("section","Moedas");selectedCurrencySection=saved.getString("currencySection","Cotação");try{converterCurrency=FxCurrency.valueOf(saved.getString("converterCurrency",converterCurrency.name()));}catch(IllegalArgumentException ignored){}converterTourismMode=converterCurrency==FxCurrency.USD&&saved.getBoolean("converterTourism",converterTourismMode);}
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        UiBars.apply(this,light);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);mainScroll=scroll;scroll.setFillViewport(true);scroll.setBackgroundColor(BG);scroll.setClipToPadding(true);
        LinearLayout wrapper=column();wrapper.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);scroll.addView(wrapper,new ScrollView.LayoutParams(-1,-1));
        LinearLayout content=column();content.setPadding(dp(20),dp(22),dp(20),dp(26));
        int usable=getResources().getConfiguration().screenWidthDp;
        wrapper.addView(content,new LinearLayout.LayoutParams(usable>720?dp(700):-1,-2));
        boolean largeFont=getResources().getConfiguration().fontScale>1.3f;LinearLayout header=largeFont?column():row();LinearLayout names=column();
        names.addView(label("CÂMBIO CLARO",13,MINT,true));add(names,label("Moedas em reais",25,TEXT,true),4);
        header.addView(names,new LinearLayout.LayoutParams(largeFont?-1:0,-2,largeFont?0:1));
        refresh=button("Atualizar ↻");refresh.setContentDescription("Atualizar cotações");refresh.setOnClickListener(v->load());header.addView(refresh,new LinearLayout.LayoutParams(largeFont?-1:dp(105),-2));
        add(content,header,0);
        LinearLayout actions=row();Button settings=button("Preferências");settings.setContentDescription("Preferências, alertas e privacidade");settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));Button diagnostic=button("Diagnóstico");diagnostic.setContentDescription("Diagnóstico do widget e falhas");diagnostic.setOnClickListener(v->startActivity(new Intent(this,DiagnosticsActivity.class)));actions.addView(settings,new LinearLayout.LayoutParams(0,dp(48),1));LinearLayout.LayoutParams diagnosticLp=new LinearLayout.LayoutParams(0,dp(48),1);diagnosticLp.leftMargin=dp(8);actions.addView(diagnostic,diagnosticLp);add(content,actions,8);
        LinearLayout navigation=row();String[] destinations={"Moedas","Bolsa","Economia","Fontes"};for(int i=0;i<destinations.length;i++){String destination=destinations[i];Button tab=button(destination);tab.setTextSize(11);tab.setPadding(dp(3),0,dp(3),0);LinearLayout.LayoutParams tabLp=new LinearLayout.LayoutParams(0,dp(48),1);if(i>0)tabLp.leftMargin=dp(8);navigation.addView(tab,tabLp);sectionButtons.put(destination,tab);tab.setOnClickListener(v->{if(destination.equals("Economia")){startActivity(new Intent(this,EconomyActivity.class));return;}showMainSection(destination);});}
        currencySection=column();marketsSection=column();sourcesSection=column();add(content,currencySection,10);add(content,marketsSection,10);add(content,sourcesSection,10);marketsSection.setVisibility(View.GONE);sourcesSection.setVisibility(View.GONE);
        LinearLayout currencyMenu=row();String[] currencyTopics={"Cotação","Gráfico","PTAX","Converter","Outras"};for(int i=0;i<currencyTopics.length;i++){String topic=currencyTopics[i];Button tab=button(topic);tab.setTextSize(10);tab.setPadding(dp(2),0,dp(2),0);LinearLayout.LayoutParams topicLp=new LinearLayout.LayoutParams(0,dp(48),1);if(i>0)topicLp.leftMargin=dp(8);currencyMenu.addView(tab,topicLp);currencySectionButtons.put(topic,tab);tab.setOnClickListener(v->showCurrencySection(topic));}add(currencySection,currencyMenu,0);
        currencyQuoteSection=column();marketChartSection=column();ptaxSection=column();converterSection=column();compareSection=column();add(currencySection,currencyQuoteSection,0);add(currencySection,marketChartSection,0);add(currencySection,ptaxSection,0);add(currencySection,converterSection,0);add(currencySection,compareSection,0);showCurrencySection(selectedCurrencySection);showMainSection(selectedSection);
        pairText=label("",13,MUTED,false);add(currencyQuoteSection,pairText,2);
        selector=new Spinner(this);selector.setContentDescription("Selecionar moeda para cotação em reais");selector.setBackground(shape(CARD,14));selector.setPopupBackgroundDrawable(shape(CARD,14));
        ArrayAdapter<FxCurrency> adapter=new ArrayAdapter<FxCurrency>(this,android.R.layout.simple_spinner_item,FxCurrency.values()){
            @Override public View getView(int pos,View old,ViewGroup parent){TextView t=label(getItem(pos).toString()+"   ▾",16,TEXT,true);t.setPadding(dp(14),dp(15),dp(14),dp(15));return t;}
            @Override public View getDropDownView(int pos,View old,ViewGroup parent){TextView t=label(getItem(pos).toString(),15,TEXT,false);t.setPadding(dp(16),dp(14),dp(16),dp(14));return t;}
        };selector.setAdapter(adapter);selector.setSelection(currency.ordinal());add(currencyQuoteSection,selector,10);
        selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int position,long id){FxCurrency next=FxCurrency.values()[position];if(next==currency)return;ptaxRequests.invalidate();loading=false;confirmed=false;currency=next;tourismMode=next==FxCurrency.USD&&Preferences.prefs(MainActivity.this).getBoolean("usd_tourism",false);RateStore.select(MainActivity.this,currency);prefs=RateStore.prefs(MainActivity.this,currency);readCache();updateDisplay();CambioWidget.renderAll(MainActivity.this,null);load();if(marketPeriodDays>1)loadDailyPeriod(currency,marketPeriodDays);}});
        usdModes=row();commercialButton=button("Dólar comercial");tourismButton=button("Dólar turismo");usdModes.addView(commercialButton,new LinearLayout.LayoutParams(0,dp(48),1));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(48),1);tp.leftMargin=dp(8);usdModes.addView(tourismButton,tp);add(currencyQuoteSection,usdModes,8);
        commercialButton.setOnClickListener(v->selectUsdMode(false));tourismButton.setOnClickListener(v->selectUsdMode(true));
        favoriteButton=button("");favoriteButton.setOnClickListener(v->{Preferences.favorite(this,currency,!Preferences.favorite(this,currency));updateFavorite();reorderOverview();});add(currencyQuoteSection,favoriteButton,8);
        add(currencyQuoteSection,label("Atualização automática pode ser adiada pelo Android. Consulte o horário da fonte.",11,MUTED,false),5);
        LinearLayout marketPanel=panel(currencyQuoteSection,light?0xFFDCF1E9:0xFF16302F);
        marketPair=label("COTAÇÃO DE MERCADO",12,MINT,true);add(marketPanel,marketPair,0);
        marketRate=label("—",40,TEXT,true);marketRate.setAutoSizeTextTypeUniformWithConfiguration(26,40,1,android.util.TypedValue.COMPLEX_UNIT_SP);marketRate.setMaxLines(1);add(marketPanel,marketRate,5);
        marketSourceNote=label("Venda (ask) · AwesomeAPI · referência, não preço final",12,MUTED,false);add(marketPanel,marketSourceNote,3);
        marketMove=label("Aguardando duas publicações de mercado",12,MUTED,true);add(marketPanel,marketMove,10);
        marketDate=label("Aguardando cotação de mercado",12,TEXT,false);add(marketPanel,marketDate,12);
        marketStatus=label("Consultando a fonte de mercado…",11,AMBER,true);marketStatus.setMaxLines(2);marketStatus.setPadding(dp(9),dp(6),dp(9),dp(6));marketStatus.setBackground(shape(light?0xFFFFF1CC:0xFF3A3020,10));add(marketPanel,marketStatus,8);
        LinearLayout intradayPanel=panel(marketChartSection,CARD);
        add(intradayPanel,label("Gráfico de mercado · subida e queda",19,TEXT,true),0);
        intradaySourceNote=label("Moeda selecionada acima · venda (ask) · AwesomeAPI",12,MUTED,false);add(intradayPanel,intradaySourceNote,4);
        LinearLayout marketPeriodRow=row();int[] marketDays={1,5,30,365};String[] marketLabels={"1D","5D","1M","1A"};for(int i=0;i<marketDays.length;i++){final int n=marketDays[i];Button b=button(marketLabels[i]);marketPeriods.put(n,b);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(48),1);if(i>0)bp.leftMargin=dp(8);marketPeriodRow.addView(b,bp);b.setOnClickListener(v->{marketPeriodDays=n;if(n==1){updateIntraday();loadHistory(currency);}else loadDailyPeriod(currency,n);});}add(intradayPanel,marketPeriodRow,10);
        intradayStatus=label("Aguardando pontos publicados hoje",12,MUTED,false);add(intradayPanel,intradayStatus,8);
        intradayMovement=label("Aguardando duas publicações para calcular a variação",13,TEXT,true);intradayMovement.setMaxLines(3);intradayMovement.setPadding(dp(12),dp(11),dp(12),dp(11));intradayMovement.setBackground(shape(light?0xFFF0F5F6:0xFF172A34,12));intradayMovement.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);add(intradayPanel,intradayMovement,7);
        intradayChart=new IntradayView(this);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(225));ip.topMargin=dp(6);intradayPanel.addView(intradayChart,ip);
        add(intradayPanel,label("Verde = subiu · vermelho = caiu. Toque ou arraste para consultar preço, horário e variação. A linha reta liga pontos reais; não representa cotações entre eles.",11,MUTED,false),4);
        intradayChangesToggle=button("Ver histórico completo");intradayChangesToggle.setVisibility(View.GONE);add(intradayPanel,intradayChangesToggle,7);intradayChanges=column();add(intradayPanel,intradayChanges,4);
        intradayChangesToggle.setOnClickListener(v->{intradayChangesExpanded=!intradayChangesExpanded;intradayChanges.setVisibility(intradayChangesExpanded?View.VISIBLE:View.GONE);intradayChangesToggle.setText(intradayChangesExpanded?"Ocultar alterações ↑":"Ver alterações do período");updateIntradayMovements(currentIntradayPoints,-1);});
        intradayChart.setOnSelectionChangedListener(index->updateIntradayMovements(currentIntradayPoints,index));
        add(intradayPanel,label("1D mostra observações intradiárias reais. 5D, 1M e 1A consultam fechamentos diários da AwesomeAPI. Períodos usam séries separadas. Cada ponto e horário vêm da fonte; nenhum preço é criado no intervalo.",11,MUTED,false),7);
        Button ptaxToggle=button("");LinearLayout ptaxContent=column();add(ptaxSection,ptaxToggle,8);add(ptaxSection,ptaxContent,0);
        Runnable updatePtax=()->{ptaxContent.setVisibility(ptaxOpen?View.VISIBLE:View.GONE);ptaxToggle.setText(ptaxOpen?"Ocultar PTAX · referência do Banco Central ↑":"Ver PTAX · referência do Banco Central ↓");};
        ptaxToggle.setOnClickListener(v->{ptaxOpen=!ptaxOpen;updatePtax.run();});updatePtax.run();
        statusText=label("Consultando a fonte…",12,AMBER,false);statusText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);add(ptaxContent,statusText,12);
        LinearLayout hero=panel(ptaxContent,light?0xFFDCF1E9:0xFF16302F);
        unitText=label("",12,light?0xFF34594F:0xFFA7CBC3,true);add(hero,unitText,0);
        rateText=label("—",44,TEXT,true);rateText.setAutoSizeTextTypeUniformWithConfiguration(28,44,1,android.util.TypedValue.COMPLEX_UNIT_SP);rateText.setMaxLines(1);add(hero,rateText,7);
        add(hero,label("PTAX de venda · última publicação disponível",12,light?0xFF34594F:0xFFA7CBC3,false),2);
        deltaText=label("Variação indisponível",13,MINT,true);add(hero,deltaText,13);
        quoteDate=label("Aguardando uma cotação válida",12,light?0xFF34594F:0xFFB8CEC8,false);add(hero,quoteDate,9);
        LinearLayout plot=panel(ptaxContent,CARD);
        LinearLayout chartHeader=largeFont?column():row();TextView title=label("Histórico real",19,TEXT,true);chartHeader.addView(title,new LinearLayout.LayoutParams(largeFont?-1:0,-2,largeFont?0:1));
        Button table=button("Ver tabela");table.setOnClickListener(v->showTable());chartHeader.addView(table,new LinearLayout.LayoutParams(largeFont?-1:dp(96),-2));add(plot,chartHeader,0);
        chartUnit=label("",12,MUTED,false);add(plot,chartUnit,3);
        LinearLayout periods=row();int[] values={7,30,90,365};String[] captions={"7 dias","30 dias","90 dias","1 ano"};
        for(int i=0;i<values.length;i++){final int n=values[i];Button b=button(captions[i]);filters.put(n,b);b.setOnClickListener(v->{days=n;updateChart();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(i>0)p.leftMargin=dp(6);periods.addView(b,p);}add(plot,periods,16);
        selectedText=label("Nenhuma publicação carregada",13,TEXT,true);add(plot,selectedText,18);
        chart=new ChartView(this);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(205));cp.topMargin=dp(5);plot.addView(chart,cp);
        rangeText=label("—",12,MUTED,false);add(plot,rangeText,4);
        LinearLayout stats=row();LinearLayout a=column(),b=column();a.addView(label("MÍNIMA DO PERÍODO",10,MUTED,true));b.addView(label("MÁXIMA DO PERÍODO",10,MUTED,true));minimum=label("—",18,TEXT,true);maximum=label("—",18,TEXT,true);add(a,minimum,4);add(b,maximum,4);stats.addView(a,new LinearLayout.LayoutParams(0,-2,1));stats.addView(b,new LinearLayout.LayoutParams(0,-2,1));add(plot,stats,18);
        countText=label("Somente publicações da fonte. Escala vertical ajustada.",11,MUTED,false);add(plot,countText,14);
        LinearLayout converter=panel(converterSection,CARD);converterTitle=label("",19,TEXT,true);add(converter,converterTitle,0);add(converter,label("Moeda de origem",12,MUTED,false),10);
        converterSelector=new Spinner(this);converterSelector.setContentDescription("Selecionar moeda de origem do conversor");converterSelector.setBackground(shape(light?0xFFE1ECEF:0xFF1C333F,12));converterSelector.setPopupBackgroundDrawable(shape(CARD,14));ArrayAdapter<FxCurrency> converterAdapter=new ArrayAdapter<FxCurrency>(this,android.R.layout.simple_spinner_item,FxCurrency.values()){@Override public View getView(int position,View old,ViewGroup parent){TextView t=label(getItem(position).toString()+"   ▾",15,TEXT,true);t.setPadding(dp(12),dp(9),dp(12),dp(9));return t;}@Override public View getDropDownView(int position,View old,ViewGroup parent){TextView t=label(getItem(position).toString(),15,TEXT,false);t.setPadding(dp(16),dp(12),dp(16),dp(12));return t;}};converterSelector.setAdapter(converterAdapter);converterSelector.setSelection(converterCurrency.ordinal());add(converter,converterSelector,5);
        converterUsdModes=row();converterCommercialButton=button("Dólar comercial");converterTourismButton=button("Dólar turismo");converterUsdModes.addView(converterCommercialButton,new LinearLayout.LayoutParams(0,dp(48),1));LinearLayout.LayoutParams converterTourismLp=new LinearLayout.LayoutParams(0,dp(48),1);converterTourismLp.leftMargin=dp(8);converterUsdModes.addView(converterTourismButton,converterTourismLp);add(converter,converterUsdModes,5);converterSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){FxCurrency selected=FxCurrency.values()[position];if(selected==converterCurrency)return;converterCurrency=selected;if(converterCurrency!=FxCurrency.USD)converterTourismMode=false;Preferences.prefs(MainActivity.this).edit().putString("converter_currency",converterCurrency.name()).putBoolean("converter_usd_tourism",converterTourismMode).apply();updateConverterModeButtons();updateConversion();}});converterCommercialButton.setOnClickListener(v->setConverterUsdMode(false));converterTourismButton.setOnClickListener(v->setConverterUsdMode(true));updateConverterModeButtons();
        TextView destinationLabel=label("Destino · BRL · Real brasileiro",12,TEXT,true);destinationLabel.setContentDescription("Moeda de destino: BRL, real brasileiro");add(converter,destinationLabel,9);
        amountLabel=label("",12,MUTED,false);add(converter,amountLabel,6);
        amount=new EditText(this);amount.setTextColor(TEXT);amount.setHintTextColor(MUTED);amount.setTextSize(25);amount.setSingleLine(true);amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);amount.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});amount.setPadding(dp(14),dp(10),dp(14),dp(10));amount.setBackground(shape(light?0xFFE1ECEF:0xFF1C333F,12));amount.setContentDescription("Valor na moeda de origem. Use vírgula ou ponto decimal, sem separador de milhar.");amount.setSelectAllOnFocus(true);amount.setText(saved==null?Preferences.prefs(this).getString("amount","1,00"):saved.getString("amount","1,00"));add(converter,amount,6);
        resultText=label("—",31,MINT,true);resultText.setAutoSizeTextTypeUniformWithConfiguration(18,31,1,android.util.TypedValue.COMPLEX_UNIT_SP);resultText.setMaxLines(1);add(converter,resultText,13);
        convertNote=label("A conversão depende de uma cotação válida.",12,MUTED,false);add(converter,convertNote,4);
        amount.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int c){updateConversion();}public void afterTextChanged(Editable e){}});
        LinearLayout overview=panel(compareSection,CARD);overviewContainer=overview;add(overview,label("Moedas × real",17,TEXT,true),0);
        add(overview,label("Mercado · valor e horário da fonte",11,MUTED,false),3);
        Button compare=button("Comparar moedas");compare.setTextSize(12);compare.setMinHeight(dp(48));compare.setMinimumHeight(dp(48));compare.setPadding(dp(10),0,dp(10),0);compare.setOnClickListener(v->chooseComparison());add(overview,compare,7);
        overviewList=column();add(overview,overviewList,4);
        for(FxCurrency item:Preferences.ordered(this)){
            LinearLayout currencyItem=column();currencyItem.setPadding(dp(2),dp(7),dp(2),dp(7));LinearLayout currencyRow=row();
            LinearLayout description=column();TextView currencyTitle=label((Preferences.favorite(this,item)?"★ ":"")+item.toString(),12,TEXT,true);currencyTitle.setMaxLines(1);currencyTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);add(description,currencyTitle,0);overviewTitles.put(item,currencyTitle);overviewRows.put(item,currencyItem);TextView price=label("Aguardando a fonte",14,TEXT,true),change=label("Sem variação disponível",10,MUTED,false);price.setMaxLines(1);price.setAutoSizeTextTypeUniformWithConfiguration(11,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);
            overviewPrices.put(item,price);overviewChanges.put(item,change);add(description,change,2);currencyRow.addView(description,new LinearLayout.LayoutParams(0,-2,1));LinearLayout value=column();value.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);value.addView(price);currencyRow.addView(value,new LinearLayout.LayoutParams(-2,-2));currencyItem.addView(currencyRow,new LinearLayout.LayoutParams(-1,-2));View divider=new View(this);divider.setBackgroundColor(light?0xFFDDE6E9:0xFF263B45);LinearLayout.LayoutParams dividerLp=new LinearLayout.LayoutParams(-1,dp(1));dividerLp.topMargin=dp(7);currencyItem.addView(divider,dividerLp);add(overviewList,currencyItem,0);
            currencyItem.setFocusable(true);currencyItem.setClickable(true);currencyItem.setOnClickListener(v->{selector.setSelection(item.ordinal());showCurrencySection("Cotação");mainScroll.smoothScrollTo(0,0);});
        }
        overviewMore=button("Ver todas as moedas");overviewMore.setTextSize(12);overviewMore.setMinHeight(dp(48));overviewMore.setMinimumHeight(dp(48));overviewMore.setOnClickListener(v->{overviewExpanded=!overviewExpanded;updateOverviewVisibility();});add(overviewList,overviewMore,2);updateOverviewVisibility();
        LinearLayout tourismRow=column();tourismRow.setPadding(dp(12),dp(14),dp(12),dp(14));tourismRow.setBackground(shape(light?0xFFF0F5F7:0xFF192F3B,14));add(tourismRow,label("USD Turismo • dólar para viagem",14,TEXT,true),0);tourismOverviewPrice=label("Aguardando fonte de mercado",14,TEXT,false);tourismOverviewDate=label("Cotação separada da PTAX",11,MUTED,false);add(tourismRow,tourismOverviewPrice,5);add(tourismRow,tourismOverviewDate,4);add(overview,tourismRow,10);tourismRow.setOnClickListener(v->{selector.setSelection(FxCurrency.USD.ordinal());showCurrencySection("Cotação");selectUsdMode(true);mainScroll.smoothScrollTo(0,0);});
        LinearLayout stocksPanel=panel(marketsSection,CARD);add(stocksPanel,label("Bolsa de valores",19,TEXT,true),0);
        add(stocksPanel,label("Ibovespa em pontos; ações em R$. Fonte: brapi; planos gratuitos podem atrasar e a exibição depende dos termos do provedor. Dados antigos recebem aviso. Fora do pregão, pode permanecer a última publicação.",12,MUTED,false),7);
        stocksStatus=label("Aguardando consulta da bolsa",12,MUTED,false);add(stocksPanel,stocksStatus,10);
        String[] symbols={"^BVSP","PETR4","VALE3","ITUB4","MGLU3"};String[] stockNames={"Ibovespa (IBOV)","Petrobras PN","Vale ON","Itaú Unibanco PN","Magazine Luiza ON"};
        for(int i=0;i<symbols.length;i++){String symbol=symbols[i];LinearLayout item=column();item.setPadding(dp(12),dp(13),dp(12),dp(13));item.setBackground(shape(light?0xFFF0F5F7:0xFF192F3B,14));add(item,label(stockNames[i]+" · "+symbol,14,TEXT,true),0);TextView price=label("Aguardando cotação",16,TEXT,true),change=label("Fonte indisponível",11,MUTED,false);stockPrices.put(symbol,price);stockChanges.put(symbol,change);add(item,price,4);add(item,change,3);add(stocksPanel,item,8);}
        Button stocksConfig=button("Configurar servidor do Ibovespa");stocksConfig.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));add(stocksPanel,stocksConfig,11);
        LinearLayout futuresPanel=panel(marketsSection,CARD);add(futuresPanel,label("B3  /  DERIVATIVOS",11,MINT,true),0);
        add(futuresPanel,label("Mercado futuro",22,TEXT,true),5);
        add(futuresPanel,label("Contratos e vencimentos conforme calendário da B3.",12,MUTED,false),5);
        for(String base:new String[]{"WIN","DOL","WDO"}){
            LinearLayout item=column();item.setPadding(dp(12),dp(13),dp(12),dp(13));item.setBackground(shape(light?0xFFF0F5F7:0xFF192F3B,14));
            String name=base.equals("WIN")?"Mini Ibovespa":base.equals("DOL")?"Dólar futuro · contrato cheio":"Mini dólar futuro";
            add(item,label(name+" · "+base,15,TEXT,true),0);
            Spinner selected=new Spinner(this);selected.setContentDescription("Vencimento de "+base);selected.setBackground(shape(light?0xFFE1ECEF:0xFF203641,12));futuresSelectors.put(base,selected);add(item,selected,9);
            add(item,label("—",32,TEXT,true),10);
            add(item,label("COTAÇÃO INDISPONÍVEL",11,AMBER,true),1);
            TextView detail=label("Carregando calendário da B3",12,MUTED,false);futuresDetails.put(base,detail);add(item,detail,8);add(futuresPanel,item,10);
        }
        Button calendar=button("Ver calendário oficial da B3");calendar.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(FuturesContracts.CALENDAR_URL)));}catch(ActivityNotFoundException ex){new AlertDialog.Builder(this).setMessage(FuturesContracts.CALENDAR_URL).setPositiveButton("OK",null).show();}});add(futuresPanel,calendar,11);
        add(futuresPanel,label("Preços aguardam fonte autorizada. Não exibimos valores estimados.",11,MUTED,false),7);
        refreshFutures();
        LinearLayout source=panel(sourcesSection,light?0xFFE5EDEF:0xFF101E28);add(source,label("Fontes e atualização",18,TEXT,true),0);
        add(source,label("MOEDAS  ·  AwesomeAPI",11,MINT,true),15);
        add(source,label("Venda (ask) com horário da fonte. Dólar turismo tem cotação própria; preços podem atrasar.",12,MUTED,false),4);
        add(source,label("BOLSA  ·  brapi",11,MINT,true),14);
        add(source,label("Ações e Ibovespa com horário da fonte. Fora do pregão, aparece a última publicação.",12,MUTED,false),4);
        add(source,label("PTAX  ·  Banco Central",11,MINT,true),14);
        add(source,label("Referência diária. Gráficos usam publicações reais; fins de semana e feriados podem ficar sem novos pontos.",12,MUTED,false),4);
        add(source,label("A conversão não inclui spread, tarifas nem IOF.",11,MUTED,false),12);
        Button widgetButton=button("Adicionar widget à tela inicial");widgetButton.setOnClickListener(v->{
            android.appwidget.AppWidgetManager manager=getSystemService(android.appwidget.AppWidgetManager.class);
            if(manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(new ComponentName(this,CambioWidget.class),null,null);
            else new AlertDialog.Builder(this).setTitle("Adicionar widget").setMessage("Na tela inicial, toque e segure em um espaço vazio. Abra Widgets e escolha Câmbio Claro.").setPositiveButton("OK",null).show();
        });add(source,widgetButton,14);
        add(source,label("Cada widget guarda sua moeda. Atualização automática é solicitada a cada 30 min; o Android pode adiar. ↻ pede uma nova consulta, sujeita à conexão e ao agendador do Android.",12,MUTED,false),9);
        Button sourceButton=button("Abrir dados PTAX na fonte ↗");sourceButton.setOnClickListener(v->openSource());add(source,sourceButton,14);
        Button marketSource=button("Abrir fonte de mercado ↗");marketSource.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(MarketClient.INFO)));}catch(ActivityNotFoundException ex){new AlertDialog.Builder(this).setMessage(MarketClient.INFO).setPositiveButton("OK",null).show();}});add(source,marketSource,8);
        add(content,label("Sem anúncios · Sem rastreadores\nCâmbio Claro 1.8.0 · Aplicativo independente",11,MUTED,false),16);
        LinearLayout appRoot=column();appRoot.setBackgroundColor(BG);appRoot.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));navigation.setBackgroundColor(CARD);navigation.setPadding(dp(8),dp(6),dp(8),dp(6));appRoot.addView(navigation,new LinearLayout.LayoutParams(-1,dp(60)));
        if(Build.VERSION.SDK_INT>=30)appRoot.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());int bottom=Math.max(bars.bottom,ime.bottom);v.setPadding(bars.left,bars.top,bars.right,bottom);return insets;});
        setContentView(appRoot);readCache();market=MarketStore.read(this);updateDisplay();load();if(marketPeriodDays>1)loadDailyPeriod(currency,marketPeriodDays);
    }
    private void readCache(){
        rates=RateStore.read(this,currency);fetchedAt=rates.isEmpty()?0:prefs.getLong("fetched",0);
    }
    private void loadMarket(){
        if(marketLoading)return;
        marketLoading=true;final FxCurrency requested=currency;
        worker.execute(()->{
            String selectedError=null;
            try{marketQuoteRefresh.execute();}
            catch(Exception ex){Diagnostics.record(getApplicationContext(),"consulta de mercado",ex);selectedError="Consulta de mercado falhou · exibindo último dado salvo, se houver";}
            final String failure=selectedError;
            runOnUiThread(()->{marketLoading=false;if(!isDestroyed()){market=MarketStore.read(this);updateDisplay();CambioWidget.renderAll(this,null);if(failure!=null&&currency==requested)setMarketFreshness(failure,true);}});
        });
    }
    private void selectUsdMode(boolean tourism){
        Preferences.prefs(this).edit().putBoolean("usd_tourism",tourism).apply();
        if(currency!=FxCurrency.USD){selector.setSelection(FxCurrency.USD.ordinal());return;}
        tourismMode=tourism;updateDisplay();if(tourism)loadTourism();
    }
    private void setConverterUsdMode(boolean tourism){if(converterCurrency!=FxCurrency.USD){converterSelector.setSelection(FxCurrency.USD.ordinal());}converterTourismMode=tourism;Preferences.prefs(this).edit().putBoolean("converter_usd_tourism",tourism).apply();updateConverterModeButtons();updateConversion();if(tourism)loadTourism();}
    private void updateConverterModeButtons(){if(converterUsdModes==null)return;boolean usd=converterCurrency==FxCurrency.USD;converterUsdModes.setVisibility(usd?View.VISIBLE:View.GONE);converterCommercialButton.setBackground(shape(usd&&!converterTourismMode?MINT:(light?0xFFE1ECEF:0xFF203641),14));converterCommercialButton.setTextColor(usd&&!converterTourismMode?(light?0xFFFFFFFF:BG):MINT);converterTourismButton.setBackground(shape(usd&&converterTourismMode?MINT:(light?0xFFE1ECEF:0xFF203641),14));converterTourismButton.setTextColor(usd&&converterTourismMode?(light?0xFFFFFFFF:BG):MINT);}
    private void updateUsdModeButtons(){
        usdModes.setVisibility(currency==FxCurrency.USD?View.VISIBLE:View.GONE);
        commercialButton.setBackground(shape(currency==FxCurrency.USD&&!tourismMode?MINT:(light?0xFFE1ECEF:0xFF203641),14));commercialButton.setTextColor(currency==FxCurrency.USD&&!tourismMode?(light?0xFFFFFFFF:BG):MINT);
        tourismButton.setBackground(shape(currency==FxCurrency.USD&&tourismMode?MINT:(light?0xFFE1ECEF:0xFF203641),14));tourismButton.setTextColor(currency==FxCurrency.USD&&tourismMode?(light?0xFFFFFFFF:BG):MINT);
    }
    private void loadTourism(){
        if(tourismLoading)return;tourismLoading=true;
        worker.execute(()->{
            TourismRefreshResult result=tourismRefresh.execute(System.currentTimeMillis(),historyRefreshInterval(),false);String error=null;
            if(result.latestError!=null){Diagnostics.record(getApplicationContext(),"consulta USD turismo",result.latestError);error="Consulta turismo falhou";}
            if(result.historyError!=null){Diagnostics.record(getApplicationContext(),"histórico USD turismo",result.historyError);error=error==null?"Histórico turismo indisponível":error+"; histórico indisponível";}
            final String failure=error;runOnUiThread(()->{tourismLoading=false;if(!isDestroyed()){updateDisplay();CambioWidget.renderAll(this,null);if(failure!=null&&tourismMode)setMarketFreshness(failure+" · último dado salvo, se houver",true);}});
        });
    }
    private void loadStocks(){
        if(stocksLoading)return;stocksLoading=true;
        worker.execute(()->{
            StockRefreshResult result=stockQuoteRefresh.execute();String error=null;
            if(result.stocksError!=null){Diagnostics.record(getApplicationContext(),"consulta de ações",result.stocksError);error="Ações: fonte indisponível";}
            if(result.indexError!=null){Diagnostics.record(getApplicationContext(),"consulta do Ibovespa",result.indexError);error="Ibovespa: "+result.indexError.getMessage();}
            final String failure=error;runOnUiThread(()->{stocksLoading=false;if(!isDestroyed()){updateStocks();if(failure!=null)stocksStatus.setText(failure+" · confira as datas salvas");}});
        });
    }
    private void updateStocks(){
        Map<String,Stocks.Quote> values=new LinkedHashMap<>(Stocks.read(this,false));boolean indexConfigured=!Stocks.backend(this).isEmpty();if(indexConfigured)values.putAll(Stocks.read(this,true));
        stocksStatus.setText(indexConfigured?"Fonte brapi consultada; compare o horário da publicação de cada ativo.":"Ibovespa aguarda servidor HTTPS. As quatro ações abaixo dispensam chave.");
        for(String symbol:stockPrices.keySet()){TextView price=stockPrices.get(symbol),change=stockChanges.get(symbol);Stocks.Quote quote=values.get(symbol);if(quote==null){price.setText(symbol.equals("^BVSP")&&!indexConfigured?"Configure o servidor HTTPS":"Sem cotação válida");change.setText("Nenhum valor foi estimado");change.setTextColor(MUTED);continue;}price.setText(symbol.equals("^BVSP")?NumberFormat.getNumberInstance(br).format(quote.price)+" pontos":money(quote.price,2));boolean stale=Stocks.stale(quote.time,Instant.now());change.setText(percent(quote.change)+" · "+Stocks.displayTime(quote.time)+(stale?" · DADO ANTIGO":""));change.setTextColor(stale?AMBER:quote.change.signum()>0?MINT:quote.change.signum()<0?(light?0xFFA33122:0xFFF2B5A9):MUTED);}
    }
    private void loadHistory(FxCurrency requested){
        if(historyLoading.contains(requested))return;historyLoading.add(requested);
        worker.execute(()->{
            try{long checked=getSharedPreferences("market_history_v1_"+requested.name(),MODE_PRIVATE).getLong("checked",0);if(System.currentTimeMillis()-checked<historyRefreshInterval()){runOnUiThread(()->historyLoading.remove(requested));return;}marketHistoryRefresh.refreshIntraday(requested);runOnUiThread(()->{historyLoading.remove(requested);if(!isDestroyed()&&currency==requested){updateDisplay();CambioWidget.renderAll(this,null);}});}
            catch(Exception ex){Diagnostics.record(getApplicationContext(),"histórico de mercado "+requested.name(),ex);runOnUiThread(()->{historyLoading.remove(requested);if(!isDestroyed()&&currency==requested){updateIntraday();intradayStatus.append(" · consulta falhou; mostrando dados salvos");}});}
        });
    }
    private void loadDailyPeriod(FxCurrency requested,int observations){
        if(tourismMode){updateIntraday();intradayStatus.setText("Histórico diário de USD turismo indisponível nesta fonte. Nenhum valor comercial será usado como substituto.");return;}
        updateIntraday();String key=requested.name()+":"+observations;List<MarketHistory.Point> cached=MarketArchive.dailyPeriod(this,requested,observations);
        if(!cached.isEmpty()){long checked=getSharedPreferences("market_daily_v1",MODE_PRIVATE).getLong(key,0);if(System.currentTimeMillis()-checked<historyRefreshInterval())return;}
        if(!dailyHistoryLoading.add(key))return;intradayStatus.setText("Consultando fechamentos diários reais na AwesomeAPI…");
        worker.execute(()->{
            String error=null;
            try{marketHistoryRefresh.refreshDaily(requested,observations);}
            catch(Exception failure){Diagnostics.record(getApplicationContext(),"histórico diário "+requested.name()+" "+observations,failure);error="Consulta falhou";}
            final String failure=error;runOnUiThread(()->{dailyHistoryLoading.remove(key);if(!isDestroyed()&&currency==requested&&marketPeriodDays==observations){updateIntraday();if(failure!=null){intradayStatus.append(cached.isEmpty()?" · sem dados salvos para mostrar":" · mostrando somente fechamentos reais salvos anteriormente");}}});
        });
    }
    private void updateIntraday(){
        for(Map.Entry<Integer,Button> entry:marketPeriods.entrySet()){boolean active=entry.getKey()==marketPeriodDays;entry.getValue().setBackground(shape(active?MINT:(light?0xFFE1ECEF:0xFF203641),12));entry.getValue().setTextColor(active?(light?0xFFFFFFFF:BG):MINT);}
        TreeMap<Instant,MarketHistory.Point> published=new TreeMap<>();List<MarketHistory.Point> source=marketPeriodDays==1?(tourismMode?Tourism.readHistory(this):MarketHistory.read(this,currency)):(tourismMode?Collections.emptyList():MarketArchive.dailyPeriod(this,currency,marketPeriodDays==365?360:marketPeriodDays));for(MarketHistory.Point p:source)published.put(p.at,p);
        MarketClient.Quote snapshot=tourismMode?Tourism.readLatest(this):MarketStore.snapshot(this,currency);if(snapshot!=null&&marketPeriodDays==1&&(tourismMode||snapshot.at.atZone(BcbClient.ZONE).toLocalDate().equals(BcbClient.today())))published.put(snapshot.at,new MarketHistory.Point(snapshot.at,snapshot.ask));
        List<MarketHistory.Point> points=new ArrayList<>(published.values());currentIntradayPoints=points;intradayChart.setPoints(points,marketPeriodDays==1,marketPeriodDays);updateIntradayMovements(points,points.isEmpty()?-1:points.size()-1);
        String period=marketPeriodDays==1?"1 dia":marketPeriodDays==5?"5 dias":marketPeriodDays==30?"1 mês":"1 ano";
        if(points.isEmpty()){intradayStatus.setText(tourismMode&&marketPeriodDays>1?"Histórico diário de USD turismo indisponível na fonte consultada.":"Sem observações reais no período de "+period+". Consulte a fonte ou tente novamente.");return;}
        DateTimeFormatter clock=DateTimeFormatter.ofPattern("dd/MM HH:mm",br);String first=points.get(0).at.atZone(BcbClient.ZONE).format(clock),last=points.get(points.size()-1).at.atZone(BcbClient.ZONE).format(clock);
        String heldUntil=marketPeriodDays==1&&Instant.now().isAfter(points.get(points.size()-1).at)?" · sem publicação nova desde "+points.get(points.size()-1).at.atZone(BcbClient.ZONE).format(DateTimeFormatter.ofPattern("HH:mm",br)):"";
        intradayStatus.setText((tourismMode?"USD/BRLT":currency.name()+"/BRL")+" · "+period+" · "+points.size()+" "+(marketPeriodDays==1?"observações intradiárias":"fechamentos diários reais")+" · "+first+"–"+last+" (Brasília)"+heldUntil+(points.size()<2?" · aguardando outro ponto real":""));
    }
    private void updateIntradayMovements(List<MarketHistory.Point> points,int selected){
        if(intradayMovement==null||intradayChangesToggle==null||intradayChanges==null)return;
        if(points==null||points.isEmpty()){intradayMovement.setText("Sem publicação real para comparar.");intradayChangesToggle.setVisibility(View.GONE);intradayChanges.removeAllViews();return;}
        int index=selected>=0&&selected<points.size()?selected:points.size()-1;
        intradayMovement.setText(IntradayMovement.selectedMovement(points,index,marketPeriodDays>1));int selectedDirection=IntradayMovement.direction(points,index);int movementColor=selectedDirection>0?MINT:selectedDirection<0?(light?0xFFA33122:0xFFF2B5A9):MUTED;int movementSurface=selectedDirection>0?(light?0xFFE5F3EE:0xFF173931):selectedDirection<0?(light?0xFFFCEAE8:0xFF3C2528):(light?0xFFF0F5F6:0xFF172A34);intradayMovement.setTextColor(movementColor);intradayMovement.setBackground(shape(movementSurface,12));
        int changes=Math.max(0,points.size()-1);boolean hasOlder=changes>6;intradayChangesToggle.setVisibility(hasOlder?View.VISIBLE:View.GONE);intradayChangesToggle.setText(intradayChangesExpanded?"Mostrar só as 6 mais recentes ↑":"Ver todas as "+changes+" alterações");
        intradayChanges.removeAllViews();
        add(intradayChanges,label("PUBLICAÇÕES E DIFERENÇAS",11,MUTED,true),2);
        int firstChange=intradayChangesExpanded?1:Math.max(1,points.size()-6);
        MarketHistory.Point base=points.get(0);String baseTime=base.at.atZone(BcbClient.ZONE).format(DateTimeFormatter.ofPattern(marketPeriodDays==1?"HH:mm":"dd/MM HH:mm",br));TextView baseRow=label("BASE · "+baseTime+" · R$ "+IntradayMovement.value(base.ask)+" · sem comparação anterior",12,MUTED,true);baseRow.setPadding(dp(11),dp(9),dp(11),dp(9));baseRow.setBackground(shape(light?0xFFF0F5F6:0xFF172A34,10));add(intradayChanges,baseRow,5);
        for(int i=firstChange;i<points.size();i++){
            int direction=IntradayMovement.direction(points,i);int color=direction>0?MINT:direction<0?(light?0xFFB42318:0xFFF28B82):MUTED;int surface=direction>0?(light?0xFFE5F3EE:0xFF173931):direction<0?(light?0xFFFCEAE8:0xFF3C2528):(light?0xFFF0F5F6:0xFF172A34);
            TextView item=label(IntradayMovement.summary(points,i,marketPeriodDays>1),12,color,true);item.setPadding(dp(11),dp(9),dp(11),dp(9));item.setBackground(shape(surface,10));add(intradayChanges,item,5);
        }
        if(!intradayChangesExpanded&&hasOlder)add(intradayChanges,label("Exibindo as seis variações mais recentes. Abra o histórico completo para consultar as anteriores.",10,MUTED,false),4);
    }
    private void load(){
        if(loading)return;loading=true;lastFullRefresh=System.currentTimeMillis();refresh.setEnabled(false);refresh.setText("Buscando…");
        loadMarket();
        if(currency==FxCurrency.USD)loadTourism();
        loadStocks();
        loadHistory(currency);
        refreshOverview();
        final long token=ptaxRequests.begin();final FxCurrency requestedCurrency=currency;
        statusText.setText(rates.isEmpty()?"Consultando o Banco Central…":"Consultando a fonte • exibindo dados salvos");statusText.setTextColor(AMBER);
        Future<?> task=worker.submit(()->{
            try {
                StoredPtaxRates result=ptaxRefresh.execute(requestedCurrency,BcbClient.today());
                runOnUiThread(()->{
                    if(isDestroyed()||!ptaxRequests.isCurrent(token)||requestedCurrency!=currency)return;
                    rates=result.rates;fetchedAt=result.fetchedAt;confirmed=true;
                    CambioWidget.renderAll(this,null);
                    overviewErrors.remove(requestedCurrency);
                    loading=false;refresh.setEnabled(true);refresh.setText("Atualizar ↻");updateDisplay();
                    statusText.setText("PTAX consultada em "+Instant.ofEpochMilli(fetchedAt).atZone(BcbClient.ZONE).format(DateTimeFormatter.ofPattern("dd/MM/uuuu 'às' HH:mm",br))+" (Brasília)");statusText.setTextColor(MINT);
                });
            }catch(Exception ex){if(Thread.currentThread().isInterrupted())return;Diagnostics.record(getApplicationContext(),"consulta PTAX "+requestedCurrency.name(),ex);runOnUiThread(()->{if(!isDestroyed()&&ptaxRequests.isCurrent(token)&&requestedCurrency==currency)completeError(friendly(ex));});}
        });
        ptaxRequests.attach(token,task);
    }
    private String friendly(Exception ex){
        if(ex instanceof java.net.UnknownHostException || ex instanceof java.net.ConnectException)return "Sem conexão com a fonte. Verifique sua internet e tente atualizar.";
        if(ex instanceof java.net.SocketTimeoutException)return "A fonte demorou a responder. Tente atualizar novamente.";
        if(ex instanceof javax.net.ssl.SSLException)return "Não foi possível validar a conexão segura com o Banco Central.";
        if(ex instanceof java.io.IOException)return ex.getMessage();
        return "A fonte enviou uma resposta inválida. Nenhum valor foi estimado. Tente mais tarde.";
    }
    private void completeError(String message){
        loading=false;confirmed=false;refresh.setEnabled(true);refresh.setText("Tentar ↻");updateDisplay();
        String saved="";if(!rates.isEmpty())saved="\nDados salvos da consulta de "+Instant.ofEpochMilli(fetchedAt).atZone(BcbClient.ZONE).format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm",br))+" (Brasília). Podem estar desatualizados.";
        statusText.setText(message+saved);statusText.setTextColor(AMBER);
    }
    private void updateDisplay(){
        updateFavorite();
        updateUsdModeButtons();marketPair.setText("COTAÇÃO DE MERCADO · "+(tourismMode?"USD/BRLT · turismo":currency.name()+"/BRL"));
        marketSourceNote.setText(tourismMode?"Dólar turismo · venda (ask) · AwesomeAPI · sem IOF/tarifas":"Venda (ask) · AwesomeAPI · referência, não preço final");
        intradaySourceNote.setText(tourismMode?"Dólar turismo · venda (ask) · AwesomeAPI":"Moeda selecionada acima · venda (ask) · AwesomeAPI");
        MarketClient.Quote current=tourismMode?Tourism.latest(this):MarketStore.latest(this,currency);
        List<MarketObservation> observations=new ArrayList<>();for(MarketHistory.Point point:tourismMode?Tourism.readHistory(this):MarketHistory.read(this,currency))observations.add(new MarketObservation(point.ask,point.at));
        MarketQuoteDisplay quoteDisplay=marketQuotePresenter.present(currency,tourismMode,current==null?null:current.ask,current==null?null:current.at,observations,BcbClient.today(),Instant.now(),BcbClient.ZONE);
        marketRate.setText(quoteDisplay.rate);marketMove.setText(quoteDisplay.movement);marketDate.setText(quoteDisplay.publishedAt);
        marketMove.setTextColor(quoteDisplay.direction>0?MINT:quoteDisplay.direction<0?(light?0xFFA33122:0xFFF2B5A9):MUTED);setMarketFreshness(quoteDisplay.freshness,quoteDisplay.stale);
        if(quoteDisplay.exactRate!=null)marketSourceNote.setText(quoteDisplay.exactRate);
        pairText.setText(tourismMode?"USD turismo / BRL":currency.name()+" / BRL");unitText.setText("1 "+currency.name()+" · "+currency.label.toUpperCase(br));chartUnit.setText("PTAX de venda · R$ por "+currency.name()+" 1");
        if(rates.isEmpty()){rateText.setText("—");quoteDate.setText("Sem cotação recebida da fonte");deltaText.setText("Variação indisponível");}
        else {
            Rate latest=rates.get(rates.size()-1);rateText.setText(money(latest.sell,4));
            quoteDate.setText("Publicado em "+latest.at.format(DateTimeFormatter.ofPattern("dd/MM/uuuu 'às' HH:mm",br))+" (Brasília)"+(!latest.at.toLocalDate().equals(BcbClient.today())?"\nSem publicação de hoje nesta consulta.":""));
            if(rates.size()>1){BigDecimal change=Rate.variation(rates.get(rates.size()-2).sell,latest.sell);deltaText.setText(percent(change)+" vs. publicação PTAX anterior (Banco Central)");deltaText.setTextColor(change.signum()<0?(light?0xFFA33122:0xFFF2B5A9):MINT);}else deltaText.setText("Sem publicação PTAX anterior para comparar");
        }
        updateIntraday();updateChart();updateConversion();updateOverview();updateStocks();
    }
    private void updateOverview(){
        MarketClient.Quote tourism=Tourism.latest(this);if(tourism==null){tourismOverviewPrice.setText("Sem cotação turismo recebida");tourismOverviewDate.setText("Fonte: AwesomeAPI USD/BRLT");}else{tourismOverviewPrice.setText(money(tourism.ask,4)+" por USD 1");tourismOverviewDate.setText("Turismo · "+tourism.at.atZone(BcbClient.ZONE).format(DateTimeFormatter.ofPattern("dd/MM HH:mm",br))+" · "+(tourism.at.atZone(BcbClient.ZONE).toLocalDate().equals(BcbClient.today())?"hoje":"última disponível"));}
        for(FxCurrency item:overviewPrices.keySet()){
            MarketClient.Quote quote=MarketStore.latest(this,item);
            List<Rate> data=RateStore.read(this,item);TextView price=overviewPrices.get(item),change=overviewChanges.get(item);
            if(quote!=null){price.setText(money(quote.ask,4)+" por "+item.name()+" 1");List<MarketHistory.Point> observations=marketTodayPoints(item,quote);change.setText(marketVariation(quote,observations));change.setTextColor(marketVariationColor(observations,quote));continue;}
            if(data.isEmpty()){price.setText(overviewErrors.containsKey(item)?"Fonte indisponível • tente atualizar":"Aguardando dados do Banco Central");change.setText("Sem cotação válida para comparar");change.setTextColor(MUTED);continue;}
            Rate latest=data.get(data.size()-1);price.setText(money(latest.sell,4)+" por "+item.name()+" 1");
            String detail="PTAX · publicado em "+latest.at.format(dayFormat);
            if(data.size()>1){BigDecimal value=Rate.variation(data.get(data.size()-2).sell,latest.sell),shown=value.setScale(2,java.math.RoundingMode.HALF_UP);String direction=shown.signum()>0?"▲ Alta ":shown.signum()<0?"▼ Baixa ":"● Estável ";change.setText(direction+percent(value)+" · "+detail+(overviewErrors.containsKey(item)?"\nAtualização falhou • dado salvo":""));change.setTextColor(shown.signum()>0?MINT:shown.signum()<0?(light?0xFFA33122:0xFFF2B5A9):MUTED);}
            else{change.setText(detail+" · sem publicação anterior");change.setTextColor(MUTED);}
        }
    }
    private List<MarketHistory.Point> marketTodayPoints(FxCurrency item,MarketClient.Quote quote){
        TreeMap<Instant,MarketHistory.Point> observations=new TreeMap<>();for(MarketHistory.Point point:MarketArchive.period(this,item,1))if(point.at.atZone(BcbClient.ZONE).toLocalDate().equals(BcbClient.today()))observations.put(point.at,point);
        if(quote.at.atZone(BcbClient.ZONE).toLocalDate().equals(BcbClient.today()))observations.put(quote.at,new MarketHistory.Point(quote.at,quote.ask));return new ArrayList<>(observations.values());
    }
    private String marketVariation(MarketClient.Quote quote,List<MarketHistory.Point> observations){
        ZonedDateTime currentAt=quote.at.atZone(BcbClient.ZONE);String stamp=currentAt.format(DateTimeFormatter.ofPattern("dd/MM HH:mm",br));
        if(!currentAt.toLocalDate().equals(BcbClient.today()))return "Mercado · última publicação "+stamp+" · sem observação de hoje";
        if(observations.size()<2)return "Mercado · "+stamp+" · variação indisponível: aguardando outra publicação";
        return "Mercado · "+IntradayMovement.summary(observations,observations.size()-1)+" · publicação "+stamp;
    }
    private int marketVariationColor(List<MarketHistory.Point> observations,MarketClient.Quote quote){
        if(!quote.at.atZone(BcbClient.ZONE).toLocalDate().equals(BcbClient.today()))return AMBER;
        return observations.size()<2?MUTED:IntradayMovement.direction(observations,observations.size()-1)>0?MINT:IntradayMovement.direction(observations,observations.size()-1)<0?(light?0xFFA33122:0xFFF2B5A9):MUTED;
    }
    private void refreshFutures(){
        LocalDate today=BcbClient.today();
        for(String base:futuresSelectors.keySet()){
            Spinner spinner=futuresSelectors.get(base);TextView details=futuresDetails.get(base);
            List<FuturesContracts> choices=FuturesContracts.available(base,today);
            if(choices.isEmpty()){spinner.setEnabled(false);details.setText("Calendário verificado somente até 2027. Contrato, cotação, variação e horário indisponíveis; atualize o app.");continue;}
            spinner.setEnabled(true);
            ArrayAdapter<FuturesContracts> adapter=new ArrayAdapter<FuturesContracts>(this,android.R.layout.simple_spinner_item,choices){
                @Override public View getView(int position,View old,ViewGroup parent){TextView t=label(getItem(position).toString()+"   ▾",15,TEXT,true);t.setPadding(dp(12),dp(10),dp(12),dp(10));return t;}
                @Override public View getDropDownView(int position,View old,ViewGroup parent){TextView t=label(getItem(position).toString(),15,TEXT,false);t.setPadding(dp(12),dp(10),dp(12),dp(10));return t;}
            };
            spinner.setAdapter(adapter);
            String saved=Preferences.prefs(this).getString("futures_contract_"+base,"");int index=0;
            for(int i=0;i<choices.size();i++)if(choices.get(i).symbol.equals(saved)){index=i;break;}
            spinner.setSelection(index);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                @Override public void onNothingSelected(AdapterView<?> p){}
                @Override public void onItemSelected(AdapterView<?> p,View v,int position,long id){
                    FuturesContracts c=choices.get(position);
                    Preferences.prefs(MainActivity.this).edit().putString("futures_contract_"+base,c.symbol).apply();
                    details.setText("Variação —   ·   Atualização —   ·   Atraso —\nVencimento "+c.expiry.getMonthValue()+"/"+c.expiry.getYear()+"   ·   último pregão "+c.lastTrade.format(dayFormat));
                }
            });
        }
    }
    private void refreshOverview(){
        if(!foreground||overviewLoading||worker.isShutdown())return;overviewLoading=true;final FxCurrency selectedAtStart=currency;
        worker.execute(()->{
            try{
                for(FxCurrency item:FxCurrency.values()){
                    if(!foreground||Thread.currentThread().isInterrupted())break;
                    if(item==selectedAtStart)continue;
                    long cached=RateStore.prefs(this,item).getLong("fetched",0);if(!PtaxRefreshPolicy.shouldRefresh(System.currentTimeMillis(),cached,24*60*60*1000L))continue;
                    try{ptaxStore.execute(item,BcbClient.today());if(Thread.currentThread().isInterrupted())break;Alerts.evaluate(this,item,RateStore.read(this,item));runOnUiThread(()->{if(!isDestroyed()){overviewErrors.remove(item);updateOverview();CambioWidget.renderAll(this,null);}});}
                    catch(Exception ex){Diagnostics.record(getApplicationContext(),"resumo de PTAX "+item.name(),ex);runOnUiThread(()->{if(!isDestroyed()){overviewErrors.put(item,"Falha");updateOverview();}});}
                }
            }finally{runOnUiThread(()->overviewLoading=false);}
        });
    }
    private void selected(Rate r){selectedText.setText(r.at.format(dayFormat)+"   ·   "+money(r.sell,4));}
    private void updateChart(){
        for(Map.Entry<Integer,Button> e:filters.entrySet()){boolean active=e.getKey()==days;e.getValue().setBackground(shape(active?MINT:(light?0xFFE1ECEF:0xFF203641),12));e.getValue().setTextColor(active?(light?0xFFFFFFFF:BG):MUTED);e.getValue().setSelected(active);}
        visible=Rate.period(rates,BcbClient.today(),days);chart.setRates(visible,this::selected);
        if(visible.isEmpty()){selectedText.setText("Sem publicações neste período");rangeText.setText("Nenhuma cotação foi estimada.");minimum.setText("—");maximum.setText("—");countText.setText("Selecione outro período ou atualize os dados.");return;}
        Rate first=visible.get(0),last=visible.get(visible.size()-1);selected(last);
        rangeText.setText(first.at.format(dayFormat)+" → "+last.at.format(dayFormat)+(visible.size()>1?"\n"+percent(Rate.variation(first.sell,last.sell))+" entre a primeira e a última publicação":""));
        BigDecimal lo=first.sell,hi=first.sell;for(Rate r:visible){lo=lo.min(r.sell);hi=hi.max(r.sell);}minimum.setText(money(lo,4));maximum.setText(money(hi,4));
        countText.setText(visible.size()+" publicações reais • toque no gráfico para consultar.\nEscala vertical ajustada. Datas sem publicação não recebem valores estimados.");
    }
    private void updateConversion(){
        if(resultText==null)return;
        converterTitle.setText("Converter "+(converterTourismMode?"USD turismo":converterCurrency.name())+" → BRL");amountLabel.setText("Valor em "+converterCurrency.name()+" ("+converterCurrency.label+")");amount.setContentDescription("Valor em "+converterCurrency.name()+". Use vírgula ou ponto decimal, sem separador de milhar.");
        ConversionResult result=conversionUseCase.execute(new ConversionRequest(converterCurrency.name(),amount.getText().toString(),converterTourismMode));
        ConversionDisplay display=ConversionPresenter.present(result,converterCurrency.name(),converterTourismMode,BcbClient.ZONE);
        resultText.setText(display.isAvailable()?money(display.amountInReais,2):"—");
        convertNote.setText(display.note);
    }
    private void showTable(){
        if(visible.isEmpty()){new AlertDialog.Builder(this).setTitle("Sem dados").setMessage("Não há publicações válidas para o período selecionado.").setPositiveButton("OK",null).show();return;}
        StringBuilder s=new StringBuilder("Cotações em reais por "+currency.name()+" 1.\nCompra e venda são as taxas de referência do BCB.\n\n");
        for(int i=visible.size()-1;i>=0;i--){Rate r=visible.get(i);s.append(r.at.format(dayFormat)).append("\nVenda: ").append(money(r.sell,4)).append("  •  Compra: ").append(money(r.buy,4)).append("\n\n");}
        TextView text=new TextView(this);text.setText(s.toString());text.setTextColor(TEXT);text.setTextSize(14);text.setTextIsSelectable(true);text.setPadding(dp(22),dp(12),dp(22),dp(12));
        ScrollView sc=new ScrollView(this);sc.addView(text);new AlertDialog.Builder(this).setTitle("Publicações do Banco Central").setView(sc).setPositiveButton("Fechar",null).show();
    }
    private void openSource(){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(BcbClient.url(BcbClient.today(),currency))));}catch(ActivityNotFoundException ex){new AlertDialog.Builder(this).setTitle("Fonte oficial").setMessage(BcbClient.INFO).setPositiveButton("OK",null).show();}}
    @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("days",days);b.putInt("marketPeriodDays",marketPeriodDays);b.putString("section",selectedSection);b.putString("currencySection",selectedCurrencySection);b.putBoolean("ptaxOpen",ptaxOpen);b.putString("converterCurrency",converterCurrency.name());b.putBoolean("converterTourism",converterTourismMode);b.putString("amount",amount.getText().toString());Preferences.prefs(this).edit().putString("amount",amount.getText().toString()).apply();}
    @Override public void onDestroy(){ptaxRequests.invalidate();autoHandler.removeCallbacks(autoRefresh);worker.shutdownNow();super.onDestroy();}
    @Override public void onPause(){foreground=false;autoHandler.removeCallbacks(autoRefresh);super.onPause();}
    @Override public void onResume(){super.onResume();if(themeMode!=null&&(!themeMode.equals(Preferences.prefs(this).getString("theme","system"))||light!=Preferences.light(this))){recreate();return;}CambioWidget.syncAlertSchedule(this);refreshFutures();foreground=true;autoHandler.removeCallbacks(autoRefresh);autoHandler.postDelayed(autoRefresh,foregroundRefreshInterval());if(rateText!=null&&!loading){readCache();market=MarketStore.read(this);confirmed=false;updateDisplay();if(fetchedAt==0||System.currentTimeMillis()-fetchedAt>FULL_REFRESH_MS)load();else{lastFullRefresh=System.currentTimeMillis();loadMarket();if(System.currentTimeMillis()-lastHistoryRefresh>=historyRefreshInterval()){if(tourismMode)loadTourism();else loadHistory(currency);lastHistoryRefresh=System.currentTimeMillis();}if(System.currentTimeMillis()-lastStocksRefresh>=foregroundRefreshInterval()*5){loadStocks();lastStocksRefresh=System.currentTimeMillis();}statusText.setText("Consulta PTAX salva em "+Instant.ofEpochMilli(fetchedAt).atZone(BcbClient.ZONE).format(DateTimeFormatter.ofPattern("dd/MM/uuuu 'às' HH:mm",br))+" (Brasília)");statusText.setTextColor(MUTED);}}refreshOverview();if(marketPeriodDays>1)loadDailyPeriod(currency,marketPeriodDays);}

    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);String code=intent.getStringExtra("currency");boolean hasTourism=intent.hasExtra("tourism"),tourism=intent.getBooleanExtra("tourism",false);intent.removeExtra("currency");intent.removeExtra("tourism");if(code!=null)try{selector.setSelection(FxCurrency.valueOf(code).ordinal());if(hasTourism)selectUsdMode(tourism);mainScroll.smoothScrollTo(0,0);}catch(IllegalArgumentException ignored){}}
    private void updateFavorite(){if(favoriteButton!=null)favoriteButton.setText(Preferences.favorite(this,currency)?"★ Remover "+currency.name()+" das favoritas":"☆ Favoritar "+currency.name());}
    private void updateOverviewVisibility(){if(overviewList==null)return;List<FxCurrency> ordered=Preferences.ordered(this);int visible=overviewExpanded?ordered.size():Math.min(4,ordered.size());for(int i=0;i<ordered.size();i++)overviewRows.get(ordered.get(i)).setVisibility(i<visible?View.VISIBLE:View.GONE);overviewMore.setVisibility(ordered.size()>4?View.VISIBLE:View.GONE);overviewMore.setText(overviewExpanded?"Mostrar menos":"Ver todas as moedas  ·  "+ordered.size());}
    private void reorderOverview(){if(overviewList==null)return;overviewList.removeAllViews();for(FxCurrency fx:Preferences.ordered(this)){overviewTitles.get(fx).setText((Preferences.favorite(this,fx)?"★ ":"")+fx.toString());add(overviewList,overviewRows.get(fx),0);}add(overviewList,overviewMore,2);updateOverviewVisibility();}
    private void chooseComparison(){
        String[] labels=new String[FxCurrency.values().length];boolean[] selected=new boolean[labels.length];for(int i=0;i<labels.length;i++)labels[i]=FxCurrency.values()[i].toString();
        new AlertDialog.Builder(this).setTitle("Comparar de 2 a 3 moedas").setMultiChoiceItems(labels,selected,(d,w,checked)->selected[w]=checked).setPositiveButton("Comparar",(d,w)->{List<FxCurrency> chosen=new ArrayList<>();for(int i=0;i<selected.length;i++)if(selected[i])chosen.add(FxCurrency.values()[i]);if(chosen.size()<2||chosen.size()>3){new AlertDialog.Builder(this).setMessage("Selecione duas ou três moedas para manter o gráfico legível.").setPositiveButton("OK",null).show();return;}showComparison(chosen);}).setNegativeButton("Cancelar",null).show();
    }
    private void showComparison(List<FxCurrency> chosen){
        Map<FxCurrency,List<Rate>> input=new LinkedHashMap<>();for(FxCurrency fx:chosen)input.put(fx,Rate.period(RateStore.read(this,fx),BcbClient.today(),days));Comparison data=new Comparison(input);
        if(data.dates.size()<2){new AlertDialog.Builder(this).setTitle("Dados insuficientes").setMessage("São necessárias duas datas de publicação em comum entre todas as moedas. Atualize as cotações ou escolha outro período.").setPositiveButton("OK",null).show();return;}
        ScrollView scroll=new ScrollView(this);LinearLayout body=column();body.setPadding(dp(16),dp(16),dp(16),dp(16));scroll.addView(body);
        add(body,label("Variação em % frente ao real • período selecionado: "+days+" dias. Base 0% em "+data.dates.get(0).format(dayFormat)+" para todas as moedas. Somente datas publicadas em comum, sem preencher lacunas. Dados salvos; confira a data final.",14,TEXT,false),0);
        int index=0;StringBuilder table=new StringBuilder();for(FxCurrency fx:data.series.keySet()){List<BigDecimal> values=data.series.get(fx);add(body,label(fx.name()+": "+percent(values.get(values.size()-1)),17,ComparisonView.color(this,index++),true),8);}
        body.addView(new ComparisonView(this,data),new LinearLayout.LayoutParams(-1,dp(250)));
        for(int i=0;i<data.dates.size();i++){table.append(data.dates.get(i).format(dayFormat));for(FxCurrency fx:data.series.keySet())table.append(" • ").append(fx.name()).append(" ").append(percent(data.series.get(fx).get(i)));table.append("\n");}
        TextView values=label(table.toString(),13,TEXT,false);values.setTextIsSelectable(true);add(body,values,12);
        new AlertDialog.Builder(this).setTitle("Comparação de moedas").setView(scroll).setPositiveButton("Fechar",null).show();
    }
}


