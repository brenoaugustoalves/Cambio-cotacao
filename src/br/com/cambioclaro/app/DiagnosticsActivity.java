package br.com.cambioclaro.app;
import android.app.Activity;
import android.os.*;
import android.content.*;
import android.view.*;
import android.widget.*;
public final class DiagnosticsActivity extends Activity {
    static final String SUPPORT_EMAIL="brenoaugustoalves@gmail.com";
    public static Intent supportEmailIntent(String report){return new Intent(Intent.ACTION_SENDTO,android.net.Uri.fromParts("mailto",SUPPORT_EMAIL,null)).putExtra(Intent.EXTRA_SUBJECT,"Diagnóstico do Câmbio Claro").putExtra(Intent.EXTRA_TEXT,report);}
    public static Intent shareReportIntent(String report){return new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT,"Diagnóstico do Câmbio Claro").putExtra(Intent.EXTRA_TEXT,report);}
    @Override public void onCreate(Bundle state){Preferences.theme(this);super.onCreate(state);UiStyle style=new UiStyle(this);
        ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(1);int pad=Math.round(20*getResources().getDisplayMetrics().density);body.setPadding(pad,pad,pad,pad);style.root(scroll);style.root(body);scroll.addView(body);
        TextView title=new TextView(this);title.setText("Diagnóstico do Câmbio Claro");title.setTextSize(22);style.text(title);body.addView(title);
        TextView note=new TextView(this);note.setText("Estado local das fontes, widgets e última falha, incluindo consultas que não concluíram. O relatório não contém valores de cotação, valores digitados ou limites de alerta. Mensagens técnicas podem revelar detalhes da falha; revise antes de compartilhar. Nada é enviado sem sua ação.\n");note.setTextSize(16);style.text(note);body.addView(note);
        TextView report=new TextView(this);report.setText(Diagnostics.report(this));report.setTextSize(14);style.text(report);report.setTextIsSelectable(true);
        Button refresh=new Button(this);refresh.setText("Atualizar diagnóstico local");style.button(refresh);body.addView(refresh);refresh.setOnClickListener(v->report.setText(Diagnostics.report(this)));
        Button share=new Button(this);share.setText("Compartilhar diagnóstico…");style.button(share);body.addView(share);share.setOnClickListener(v->{String text=Diagnostics.report(this);report.setText(text);try{startActivity(Intent.createChooser(shareReportIntent(text),"Compartilhar diagnóstico"));}catch(ActivityNotFoundException ex){Toast.makeText(this,"Não há aplicativo disponível para compartilhar. Copie o diagnóstico.",Toast.LENGTH_LONG).show();}});
        Button email=new Button(this);email.setText("Enviar diagnóstico por e-mail ao suporte");style.button(email);body.addView(email);email.setOnClickListener(v->{String text=Diagnostics.report(this);report.setText(text);try{startActivity(supportEmailIntent(text));}catch(ActivityNotFoundException ex){try{startActivity(Intent.createChooser(shareReportIntent("Para: "+SUPPORT_EMAIL+"\n\n"+text),"Escolha um aplicativo para enviar ao suporte"));}catch(ActivityNotFoundException noShare){Toast.makeText(this,"Nenhum aplicativo de e-mail disponível. Copie o diagnóstico e envie para "+SUPPORT_EMAIL,Toast.LENGTH_LONG).show();}}});
        Button copy=new Button(this);copy.setText("Copiar diagnóstico");style.button(copy);body.addView(copy);copy.setOnClickListener(v->{getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Diagnóstico Câmbio Claro",report.getText()));Toast.makeText(this,"Diagnóstico copiado. Você pode colar na conversa.",Toast.LENGTH_LONG).show();});
        Button back=new Button(this);back.setText("Voltar");style.button(back);body.addView(back);back.setOnClickListener(v->finish());
        Button clear=new Button(this);clear.setText("Apagar registro de falha local");style.button(clear);body.addView(clear);clear.setOnClickListener(v->{Diagnostics.clear(this);report.setText(Diagnostics.report(this));});body.addView(report);
        setContentView(scroll);UiBars.apply(this,style.light);
        if(Build.VERSION.SDK_INT>=30)scroll.setOnApplyWindowInsetsListener((v,in)->{android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return in;});
    }
}
