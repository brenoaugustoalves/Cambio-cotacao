package br.com.cambioclaro.app;

import org.json.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class BcbClient {
    public static final ZoneId ZONE=ZoneId.of("America/Sao_Paulo");
    public static final String INFO="https://dadosabertos.bcb.gov.br/dataset/taxas-de-cambio-todos-os-boletins-diarios";
    public static LocalDate today() {return LocalDate.now(ZONE);}
    public static String url(LocalDate today) {
        return url(today,FxCurrency.USD);
    }
    public static String url(LocalDate today,FxCurrency currency) {
        if(currency==null)throw new IllegalArgumentException("Moeda obrigatória");
        DateTimeFormatter f=DateTimeFormatter.ofPattern("MM-dd-uuuu",Locale.US);
        if(currency!=FxCurrency.USD)return "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?@moeda='"+currency.name()+"'&@dataInicial='"+today.minusDays(370).format(f)+"'&@dataFinalCotacao='"+today.format(f)+"'&$format=json&$filter=tipoBoletim%20eq%20'Fechamento'&$top=1000&$orderby=dataHoraCotacao%20asc";
        return "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?@dataInicial='"+today.minusDays(370).format(f)+"'&@dataFinalCotacao='"+today.format(f)+"'&$format=json&$top=1000&$orderby=dataHoraCotacao%20asc";
    }
    public static String fetch(LocalDate today) throws Exception {
        return fetch(today,FxCurrency.USD);
    }
    public static String fetch(LocalDate today,FxCurrency currency) throws Exception {
        final long deadline=System.nanoTime()+45000000000L;
        HttpsURLConnection connection=(HttpsURLConnection)new URL(url(today,currency)).openConnection();
        connection.setConnectTimeout(15000);connection.setReadTimeout(20000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept","application/json");
        connection.setRequestProperty("User-Agent","CambioClaro/1.0 (Android; PTAX)");
        try {
            int code=connection.getResponseCode();
            if(code!=200) throw new IOException("O Banco Central respondeu HTTP "+code+". Tente novamente mais tarde.");
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=connection.getInputStream()) {
                byte[] buffer=new byte[8192];int count;
                while((count=in.read(buffer))!=-1) {
                    if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Consulta cancelada.");
                    if(System.nanoTime()>deadline)throw new java.net.SocketTimeoutException("Tempo total de consulta excedido.");
                    if(out.size()+count>2000000) throw new IOException("Resposta maior que o esperado.");
                    out.write(buffer,0,count);
                }
            }
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        } finally {connection.disconnect();}
    }
    public static List<Rate> parse(String json,LocalDate today) throws Exception {
        return parse(json,today,FxCurrency.USD);
    }
    public static List<Rate> parse(String json,LocalDate today,FxCurrency currency) throws Exception {
        if(json==null||json.length()>2000000)throw new IOException("Resposta fora do limite esperado.");
        // Bound parser recursion before passing an untrusted response to JSONObject.
        boolean quoted=false,escaped=false;int depth=0;
        for(int j=0;j<json.length();j++){
            char ch=json.charAt(j);
            if(quoted){if(escaped)escaped=false;else if(ch=='\\')escaped=true;else if(ch=='\"')quoted=false;}
            else if(ch=='\"')quoted=true;
            else if(ch=='{'||ch=='['){if(++depth>8)throw new IOException("Estrutura de resposta inesperada.");}
            else if(ch=='}'||ch==']'){if(--depth<0)throw new IOException("Estrutura de resposta inválida.");}
        }
        if(quoted||depth!=0)throw new IOException("Resposta incompleta.");
        JSONObject root=new JSONObject(json);
        if(root.has("@odata.nextLink") || root.has("odata.nextLink")) throw new IOException("A fonte retornou uma série incompleta.");
        JSONArray rows=root.getJSONArray("value");
        if(rows.length()>1000)throw new IOException("Resposta fora do limite esperado.");
        List<Rate> list=new ArrayList<>();
        for(int i=0;i<rows.length();i++) {
            JSONObject r=rows.getJSONObject(i);
            if(currency!=FxCurrency.USD && !"Fechamento".equals(r.optString("tipoBoletim")))throw new IOException("Boletim diferente do fechamento solicitado.");
            String buyText=r.getString("cotacaoCompra"),sellText=r.getString("cotacaoVenda");
            if(!(r.get("cotacaoCompra") instanceof Number) || !(r.get("cotacaoVenda") instanceof Number) || !buyText.matches("[0-9]{1,8}(\\.[0-9]{1,8})?") || !sellText.matches("[0-9]{1,8}(\\.[0-9]{1,8})?"))throw new IOException("Formato numérico inesperado na fonte.");
            BigDecimal buy=new BigDecimal(buyText);
            BigDecimal sell=new BigDecimal(sellText);
            if(buy.precision()>18 || sell.precision()>18)throw new IOException("Precisão inesperada.");
            LocalDateTime at=LocalDateTime.parse(r.getString("dataHoraCotacao").replace(' ','T'));
            if(at.toLocalDate().isBefore(today.minusDays(370)))throw new IOException("Publicação fora do intervalo solicitado.");
            list.add(new Rate(buy,sell,at));
        }
        List<Rate> result=Rate.normalize(list,today);
        if(result.isEmpty())throw new IOException("Nenhuma cotação publicada no período consultado.");
        return result;
    }
}
