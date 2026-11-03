package br.com.cambioclaro.app;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import br.com.cambioclaro.app.market.domain.Comparison;
import java.math.BigDecimal;
import java.util.*;
import java.time.format.DateTimeFormatter;
public final class ComparisonView extends View {
    public static final int[] COLORS={0xFF087F67,0xFF4779D8,0xFFB76520};
    public static int color(Context c,int index){return (Preferences.light(c)?COLORS:new int[]{0xFF66E0BF,0xFF9BBEFF,0xFFFFC583})[index%3];}
    private final Comparison data;private final Paint p=new Paint(3);private final float d;
    public ComparisonView(Context c,Comparison data){super(c);this.data=data;d=getResources().getDisplayMetrics().density;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override public void onDraw(Canvas c){super.onDraw(c);if(data.dates.size()<2)return;
        float min=0,max=0;for(List<BigDecimal> line:data.series.values())for(BigDecimal v:line){min=Math.min(min,v.floatValue());max=Math.max(max,v.floatValue());}float margin=Math.max(.1f,(max-min)*.1f);min-=margin;max+=margin;
        float left=64*d,right=getWidth()-15*d,top=18*d,bottom=getHeight()-35*d;p.setTextSize(11*d);p.setStrokeWidth(d);
        for(int i=0;i<4;i++){float y=top+i*(bottom-top)/3;float v=max-i*(max-min)/3;p.setColor((Preferences.light(getContext())?0xFF476271:0xFF9FB3C1));p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.RIGHT);c.drawText(String.format(new Locale("pt","BR"),"%.1f%%",v),left-6*d,y+4*d,p);p.setColor(0x558796A0);c.drawLine(left,y,right,y,p);}
        long first=data.dates.get(0).toEpochDay(),span=data.dates.get(data.dates.size()-1).toEpochDay()-first;int color=0;
        for(List<BigDecimal> line:data.series.values()){Path path=new Path();for(int i=0;i<line.size();i++){float x=left+(data.dates.get(i).toEpochDay()-first)*(right-left)/span,y=bottom-(line.get(i).floatValue()-min)*(bottom-top)/(max-min);if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*d);p.setColor(color(getContext(),color++));c.drawPath(path,p);}
        p.setStyle(Paint.Style.FILL);p.setColor((Preferences.light(getContext())?0xFF476271:0xFF9FB3C1));p.setTextAlign(Paint.Align.LEFT);c.drawText(data.dates.get(0).format(DateTimeFormatter.ofPattern("dd/MM")),left,bottom+23*d,p);p.setTextAlign(Paint.Align.RIGHT);c.drawText(data.dates.get(data.dates.size()-1).format(DateTimeFormatter.ofPattern("dd/MM")),right,bottom+23*d,p);
    }
}
