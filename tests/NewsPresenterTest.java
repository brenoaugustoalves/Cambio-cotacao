import br.com.cambioclaro.app.economy.presentation.NewsDisplay;
import br.com.cambioclaro.app.economy.presentation.NewsPresenter;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class NewsPresenterTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        NewsPresenter presenter=new NewsPresenter();ZoneId zone=ZoneId.of("America/Sao_Paulo");long fetched=Instant.parse("2026-10-30T13:00:00Z").toEpochMilli();
        NewsPresenter.Publication first=new NewsPresenter.Publication("Banco Central","Decisão publicada","https://bcb.gov.br/noticia",Instant.parse("2026-10-30T13:15:00Z"));
        NewsPresenter.Publication second=new NewsPresenter.Publication("Federal Reserve","Statement released","https://federalreserve.gov/news",Instant.parse("2026-10-30T14:00:00Z"));
        NewsDisplay bcb=presenter.present(Collections.singletonList(first),5,fetched,null,"Sem itens","publicações do BC",zone,fetched+1000);
        check(bcb.items.size()==1&&bcb.items.get(0).text.contains("Banco Central\nDecisão publicada"),"Headline retains the publisher and original title");
        check(bcb.items.get(0).text.contains("30/10/2026 10:15 (Brasília)")&&bcb.items.get(0).url.equals(first.url),"Original publication time and verified article link are preserved");
        check(bcb.status.contains("Consulta 30/10")&&bcb.status.contains("publicações do BC"),"Fetch time and source category are explicit");
        NewsDisplay international=presenter.present(Arrays.asList(first,second),1,fetched,null,"Sem itens","notícias internacionais",zone,fetched+1000);
        check(international.items.size()==1&&international.items.get(0).text.startsWith("Banco Central"),"Display limit is applied without reordering source items");
        NewsDisplay partial=presenter.present(Collections.singletonList(first),5,fetched,"Feed Fed indisponível","Sem itens","notícias internacionais",zone,fetched+1000);
        check(partial.status.contains("Uma fonte falhou")&&partial.items.size()==1,"Partial source failure keeps valid saved news visible");
        NewsDisplay empty=presenter.present(Collections.emptyList(),5,0,"Feed indisponível","Sem itens","notícias internacionais",zone,fetched+1000);
        check(empty.status.contains("Feed indisponível")&&empty.status.contains("nenhuma cópia válida")&&empty.items.isEmpty(),"Empty result clearly reports the unavailable cache");
        NewsPresenter.Publication malformed=new NewsPresenter.Publication("Fonte"," ","https://example.com",Instant.EPOCH);
        check(presenter.present(Collections.singletonList(malformed),5,fetched,null,"Sem itens","feed",zone,fetched).items.isEmpty(),"Blank headlines are not displayed");
        check(presenter.present(Collections.singletonList(first),5,fetched+1,null,"Sem itens","feed",zone,fetched).status.contains("Horário da consulta indisponível"),"Future cache timestamp is not presented as current");
        boolean invalidLimit=false;try{presenter.present(Collections.emptyList(),-1,fetched,null,"Sem itens","feed",zone,fetched);}catch(IllegalArgumentException expected){invalidLimit=true;}
        check(invalidLimit,"Negative display limit is rejected");
        System.out.println(checks+" news presentation checks passed");
    }
}
