package br.com.cambioclaro.app;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;
import java.time.format.DateTimeFormatter;

public final class ChartView extends View {
    public interface Listener { void selected(Rate rate); }
    private final Paint p=new Paint(3);
    private List<Rate> rates=Collections.emptyList();
    private int selected=-1;
    private Listener listener;
    private final float density;
    private float left,right,top,bottom,min,max;
    private long firstDay,lastDay;
    private final int mint,muted;
    public ChartView(Context c) {super(c);mint=Preferences.light(c)?0xFF087F67:0xFF66E0BF;muted=Preferences.light(c)?0xFF476271:0xFF9FB3C1; density=getResources().getDisplayMetrics().density;setLayerType(View.LAYER_TYPE_SOFTWARE,null);setClickable(true);setFocusable(true);setContentDescription("Histórico PTAX. Use Ver tabela para consultar cada publicação.");}
    private float dp(float n){return n*density;}
    public void setRates(List<Rate> value,Listener l){rates=value;listener=l;selected=rates.isEmpty()?-1:rates.size()-1;invalidate();}
    private float x(int i) {return rates.size()==1?(left+right)/2:left+(rates.get(i).at.toLocalDate().toEpochDay()-firstDay)*(right-left)/Math.max(1,lastDay-firstDay);}
    private float y(int i) {return bottom-(rates.get(i).sell.floatValue()-min)/(max-min)*(bottom-top);}
    private void label(Canvas c,String s,float x,float y,int color,float sp,Paint.Align a){p.setColor(color);p.setShader(null);p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(getResources().getDisplayMetrics().scaledDensity*sp);p.setTextAlign(a);c.drawText(s,x,y,p);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);left=dp(55)*Math.min(1.5f,getResources().getConfiguration().fontScale);right=getWidth()-dp(12);top=dp(20);bottom=getHeight()-dp(35)*getResources().getConfiguration().fontScale;
        if(rates.isEmpty()){label(c,"Sem dados no período",getWidth()/2f,getHeight()/2f,muted,14,Paint.Align.CENTER);return;}
        min=Float.MAX_VALUE;max=-Float.MAX_VALUE;
        for(Rate r:rates){min=Math.min(min,r.sell.floatValue());max=Math.max(max,r.sell.floatValue());}
        float margin=Math.max((max-min)*.17f,.004f);min-=margin;max+=margin;
        firstDay=rates.get(0).at.toLocalDate().toEpochDay();lastDay=rates.get(rates.size()-1).at.toLocalDate().toEpochDay();
        for(int i=0;i<4;i++){
            float yy=top+(bottom-top)*i/3;float value=max-(max-min)*i/3;
            p.setColor(Color.rgb(38,58,70));p.setStrokeWidth(dp(1));c.drawLine(left,yy,right,yy,p);
            int decimals=Math.min(7,Math.max(3,(int)Math.ceil(-Math.log10((max-min)/3))+1));
            label(c,String.format(new Locale("pt","BR"),"%."+decimals+"f",value),left-dp(8),yy+dp(4),muted,10,Paint.Align.RIGHT);
        }
        Path path=new Path();path.moveTo(x(0),y(0));for(int i=1;i<rates.size();i++)path.lineTo(x(i),y(i));
        if(rates.size()>1){
            Path fill=new Path(path);fill.lineTo(x(rates.size()-1),bottom);fill.lineTo(x(0),bottom);fill.close();
            p.setShader(new LinearGradient(0,top,0,bottom,new int[]{0x4566E0BF,0x0066E0BF},null,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawPath(fill,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setColor(mint);p.setStrokeWidth(dp(2.3f));p.setStrokeJoin(Paint.Join.ROUND);c.drawPath(path,p);
        }
        p.setStyle(Paint.Style.FILL);p.setColor(mint);
        if(rates.size()<35)for(int i=0;i<rates.size();i++)c.drawCircle(x(i),y(i),dp(2.7f),p);
        if(selected>=0){
            p.setColor(0x8866E0BF);p.setStrokeWidth(dp(1));c.drawLine(x(selected),top,x(selected),bottom,p);
            p.setColor(0xFF12232E);c.drawCircle(x(selected),y(selected),dp(7),p);p.setColor(mint);c.drawCircle(x(selected),y(selected),dp(4),p);
        }
        DateTimeFormatter f=DateTimeFormatter.ofPattern("dd/MM");
        label(c,rates.get(0).at.format(f),left,bottom+dp(24),muted,11,Paint.Align.LEFT);
        if(rates.size()>2)label(c,rates.get(rates.size()/2).at.format(f),x(rates.size()/2),bottom+dp(24),muted,11,Paint.Align.CENTER);
        if(rates.size()>1)label(c,rates.get(rates.size()-1).at.format(f),right,bottom+dp(24),muted,11,Paint.Align.RIGHT);
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
        if(rates.isEmpty())return super.onTouchEvent(e);
        if(e.getAction()==MotionEvent.ACTION_DOWN || e.getAction()==MotionEvent.ACTION_MOVE || e.getAction()==MotionEvent.ACTION_UP){
            int best=0;float distance=Float.MAX_VALUE;
            for(int i=0;i<rates.size();i++){float d=Math.abs(e.getX()-x(i));if(d<distance){distance=d;best=i;}}
            selected=best;if(listener!=null)listener.selected(rates.get(best));invalidate();
            if(e.getAction()==MotionEvent.ACTION_UP)performClick();return true;
        }return super.onTouchEvent(e);
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
