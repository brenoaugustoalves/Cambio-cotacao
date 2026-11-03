package br.com.cambioclaro.app.economy.presentation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Formats already-validated publication metadata without synthesizing headlines or dates. */
public final class NewsPresenter {
    public static final class Publication {
        public final String source,title,url;
        public final Instant publishedAt;
        public Publication(String source,String title,String url,Instant publishedAt){this.source=source;this.title=title;this.url=url;this.publishedAt=publishedAt;}
    }
    private static final DateTimeFormatter FETCHED=DateTimeFormatter.ofPattern("dd/MM HH:mm",new Locale("pt","BR"));
    private static final DateTimeFormatter PUBLISHED=DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm",new Locale("pt","BR"));

    public NewsDisplay present(List<Publication> publications,int limit,long fetchedAt,String failure,
            String emptyMessage,String contentLabel,ZoneId zone,long now){
        if(publications==null||limit<0||emptyMessage==null||contentLabel==null||zone==null)
            throw new IllegalArgumentException("Conteúdo de apresentação de notícias inválido");
        List<NewsDisplay.Item> items=new ArrayList<>();
        for(Publication item:publications){
            if(item==null||item.title==null||item.title.trim().isEmpty()||item.url==null||item.publishedAt==null)continue;
            if(items.size()>=limit)break;
            String source=item.source==null||item.source.trim().isEmpty()?contentLabel:item.source;
            String date=PUBLISHED.format(item.publishedAt.atZone(zone));
            items.add(new NewsDisplay.Item(source+"\n"+item.title+"\nPublicado em "+date+" (Brasília)",item.url));
        }
        String status;
        if(items.isEmpty())status=failure==null?emptyMessage:failure+"; nenhuma cópia válida disponível.";
        else if(fetchedAt<=0||fetchedAt>now)status="Horário da consulta indisponível · "+contentLabel;
        else{
            String fetched=FETCHED.format(Instant.ofEpochMilli(fetchedAt).atZone(zone))+" (Brasília)";
            status=(failure==null?"Consulta ":"Uma fonte falhou · cópia salva da consulta ")+fetched+" · "+contentLabel;
        }
        return new NewsDisplay(status,items);
    }
}
