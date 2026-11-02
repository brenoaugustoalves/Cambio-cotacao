package br.com.cambioclaro.app;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Every vertex is a source observation, positioned by its actual timestamp. */
public final class IntradayView extends View {
    private List<MarketHistory.Point> points=Collections.emptyList();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density,scaled;
    private final int accent,fall,steady,ink,muted,grid,fillTop,fillBottom;
    private final Locale br=new Locale("pt","BR");
    private int selected=-1;private float left,right,top,bottom;private OnSelectionChangedListener selectionListener;private boolean extendCurrentDay=true;
    public interface OnSelectionChangedListener {void onSelectionChanged(int index);}
    public IntradayView(Context c){super(c);density=getResources().getDisplayMetrics().density;scaled=getResources().getDisplayMetrics().scaledDensity;boolean light=Preferences.light(c);
        accent=light?0xFF087F67:0xFF66E0BF;fall=light?0xFFB42318:0xFFF28B82;steady=light?0xFF476271:0xFF9FB3C1;ink=light?0xFF132A35:0xFFF2F6F8;muted=light?0xFF476271:0xFF9FB3C1;grid=light?0xFFD6E4E7:0xFF2A434C;fillTop=light?0x55087F67:0x5566E0BF;fillBottom=light?0x00087F67:0x0066E0BF;
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);setFocusable(true);setClickable(true);setContentDescription("Gráfico do dia sem valores estimados.");}
    private float dp(float n){return n*density;}
    public void setPoints(List<MarketHistory.Point> values){setPoints(values,true);}
    public void setPoints(List<MarketHistory.Point> values,boolean extendToday){points=new ArrayList<>(values);extendCurrentDay=extendToday;selected=points.isEmpty()?-1:points.size()-1;describe();invalidate();notifySelection();}
    public boolean extendsCurrentDayTail(){return extendCurrentDay;}
    public void setOnSelectionChangedListener(OnSelectionChangedListener listener){selectionListener=listener;notifySelection();}
    private void notifySelection(){if(selectionListener!=null&&selected>=0)selectionListener.onSelectionChanged(selected);}
    private String price(double n){NumberFormat f=NumberFormat.getNumberInstance(br);int places=n<.1?6:n<1?5:4;f.setMinimumFractionDigits(places);f.setMaximumFractionDigits(places);return f.format(n);}
    private String clock(MarketHistory.Point p){return clock(p.at.toEpochMilli());}
    private String clock(long epochMillis){return Instant.ofEpochMilli(epochMillis).atZone(BcbClient.ZONE).format(DateTimeFormatter.ofPattern("HH:mm",br));}
    private void label(Canvas c,String s,float x,float y,int color,float sp,Paint.Align align){paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTextSize(sp*scaled);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextAlign(align);c.drawText(s,x,y,paint);}
    private void line(Canvas c,float x1,float y1,float x2,float y2,int color,float stroke){paint.setShader(null);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(stroke));c.drawLine(x1,y1,x2,y2,paint);paint.setStyle(Paint.Style.FILL);}
    private void describe(){if(selected<0){setContentDescription("Gráfico sem observações publicadas no período.");return;}setContentDescription("Gráfico com "+points.size()+" observações reais. Ponto publicado "+(selected+1)+". "+IntradayMovement.summary(points,selected)+" A linha contínua liga apenas as observações recebidas; não representa preços intermediários nas lacunas.");}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();
        if(points.isEmpty()){label(c,"Sem publicações de hoje",w/2,h/2,muted,12,Paint.Align.CENTER);return;}
        left=dp(75);right=w-dp(14);top=dp(38);bottom=h-dp(32);if(right<=left||bottom<=top)return;
        double min=Double.MAX_VALUE,max=-Double.MAX_VALUE;for(MarketHistory.Point p:points){double v=p.ask.doubleValue();min=Math.min(min,v);max=Math.max(max,v);}double margin=Math.max((max-min)*.12,min*.0001);min-=margin;max+=margin;
        long first=points.get(0).at.toEpochMilli(),last=points.get(points.size()-1).at.toEpochMilli();long now=Instant.now().toEpochMilli();boolean observationsAreToday=extendCurrentDay&&points.get(points.size()-1).at.atZone(BcbClient.ZONE).toLocalDate().equals(BcbClient.today());long end=observationsAreToday?Math.max(last,now):last;float spanX=right-left,spanY=bottom-top;long spanTime=Math.max(1,end-first);
        for(int i=0;i<4;i++){float y=top+spanY*i/3f;line(c,left,y,right,y,grid,1);label(c,price(max-(max-min)*i/3d),left-dp(7),y+dp(4),muted,10,Paint.Align.RIGHT);}
        Path area=new Path();float[] xs=new float[points.size()],ys=new float[points.size()];
        for(int i=0;i<points.size();i++){MarketHistory.Point p=points.get(i);float x=left+spanX*(p.at.toEpochMilli()-first)/(float)spanTime;float y=(float)(bottom-(p.ask.doubleValue()-min)/(max-min)*spanY);xs[i]=x;ys[i]=y;if(i==0)area.moveTo(x,y);else area.lineTo(x,y);}
        area.lineTo(xs[xs.length-1],bottom);area.lineTo(xs[0],bottom);area.close();paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(0,top,0,bottom,fillTop,fillBottom,Shader.TileMode.CLAMP));c.drawPath(area,paint);paint.setShader(null);
        for(int i=1;i<points.size();i++){int direction=IntradayMovement.direction(points,i);paint.setShader(null);paint.setColor(direction>0?accent:direction<0?fall:steady);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2.8f));paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);c.drawLine(xs[i-1],ys[i-1],xs[i],ys[i],paint);}
        paint.setStyle(Paint.Style.FILL);
        if(end>last)line(c,xs[xs.length-1],ys[ys.length-1],right,ys[ys.length-1],muted,2.8f);
        for(int i=0;i<points.size();i++){int direction=IntradayMovement.direction(points,i);paint.setColor(direction>0?accent:direction<0?fall:steady);c.drawCircle(xs[i],ys[i],dp(points.size()>40?1.8f:2.6f),paint);}
        int idx=Math.max(0,Math.min(selected,points.size()-1));float sx=xs[idx],sy=ys[idx];line(c,sx,top,sx,bottom,muted,1);paint.setColor(accent);c.drawCircle(sx,sy,dp(5),paint);paint.setColor(ink);c.drawCircle(sx,sy,dp(2),paint);
        MarketHistory.Point chosen=points.get(idx);label(c,"R$ "+price(chosen.ask.doubleValue())+" · "+clock(chosen),left,dp(23),ink,14,Paint.Align.LEFT);
        boolean dateAxis=spanTime>24L*60*60*1000;String axisPattern=dateAxis?"dd/MM":"HH:mm";label(c,axis(first,axisPattern),left,bottom+dp(22),muted,10,Paint.Align.LEFT);label(c,axis(first+spanTime/2,axisPattern),(left+right)/2,bottom+dp(22),muted,10,Paint.Align.CENTER);label(c,axis(end,axisPattern),right,bottom+dp(22),muted,10,Paint.Align.RIGHT);
    }
    private String axis(long millis,String pattern){return Instant.ofEpochMilli(millis).atZone(BcbClient.ZONE).format(DateTimeFormatter.ofPattern(pattern,br));}
    @Override public boolean onTouchEvent(MotionEvent e){if(points.isEmpty())return super.onTouchEvent(e);int action=e.getAction();if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_MOVE||action==MotionEvent.ACTION_UP){float x=Math.max(left,Math.min(right,e.getX()));long first=points.get(0).at.toEpochMilli(),last=points.get(points.size()-1).at.toEpochMilli();long now=Instant.now().toEpochMilli();boolean today=extendCurrentDay&&points.get(points.size()-1).at.atZone(BcbClient.ZONE).toLocalDate().equals(BcbClient.today());long end=today?Math.max(last,now):last;long target=first+(long)((x-left)/Math.max(1,right-left)*(end-first));int best=0;long distance=Long.MAX_VALUE;for(int i=0;i<points.size();i++){long d=Math.abs(points.get(i).at.toEpochMilli()-target);if(d<distance){best=i;distance=d;}}selected=best;describe();invalidate();notifySelection();if(action==MotionEvent.ACTION_UP)performClick();return true;}return super.onTouchEvent(e);}
    @Override public boolean performClick(){super.performClick();return true;}
}
