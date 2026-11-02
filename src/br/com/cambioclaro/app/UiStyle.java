package br.com.cambioclaro.app;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
/** Explicit shared surface and control colors; do not inherit vendor defaults. */
public final class UiStyle {
    public final boolean light;
    public final int background,surface,text,muted,accent,control;
    private final Context context;
    public UiStyle(Context c){context=c;light=Preferences.light(c);background=light?0xFFF1F5F7:0xFF0A141C;surface=light?0xFFFFFFFF:0xFF12232E;text=light?0xFF132A35:0xFFF2F6F8;muted=light?0xFF476271:0xFF9FB3C1;accent=light?0xFF086A53:0xFF66E0BF;control=light?0xFFE1ECEF:0xFF203641;}
    public int dp(int value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
    public GradientDrawable shape(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(14));return g;}
    public void root(View v){v.setBackgroundColor(background);if(android.os.Build.VERSION.SDK_INT>=29)v.setForceDarkAllowed(false);}
    public void text(TextView v){v.setTextColor(text);v.setHintTextColor(muted);}
    public void button(Button b){b.setAllCaps(false);b.setTextColor(accent);b.setTextSize(15);b.setMinHeight(dp(48));b.setPadding(dp(14),dp(10),dp(14),dp(10));b.setBackground(shape(control));LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(8);b.setLayoutParams(params);}
    public void input(EditText e){text(e);e.setTextSize(18);e.setPadding(dp(14),dp(12),dp(14),dp(12));e.setBackground(shape(surface));}
    public <T> void spinner(Spinner spinner,T[] values){
        spinner.setBackground(shape(surface));spinner.setPopupBackgroundDrawable(shape(surface));
        spinner.setAdapter(new ArrayAdapter<T>(context,android.R.layout.simple_spinner_item,values){
            private View item(int position,boolean selected){TextView v=new TextView(context);v.setText(String.valueOf(getItem(position))+(selected?"   ▾":""));text(v);v.setTextSize(16);v.setTypeface(Typeface.DEFAULT,selected?Typeface.BOLD:Typeface.NORMAL);v.setPadding(dp(14),dp(14),dp(14),dp(14));v.setBackgroundColor(surface);return v;}
            @Override public View getView(int position,View old,ViewGroup parent){return item(position,true);}
            @Override public View getDropDownView(int position,View old,ViewGroup parent){return item(position,false);}
        });LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(10);spinner.setLayoutParams(params);
    }
}
