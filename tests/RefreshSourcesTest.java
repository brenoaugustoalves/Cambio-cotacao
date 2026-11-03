import br.com.cambioclaro.app.market.application.RefreshSources;
import java.util.Arrays;
import java.util.Collections;

public final class RefreshSourcesTest {
    private static int checks;
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
    private static RefreshSources.Source source(String id,boolean fail,StringBuilder events){return new RefreshSources.Source(){public String id(){return id;}public void refresh()throws Exception{events.append(id).append(';');if(fail)throw new java.io.IOException(id);}};}
    public static void main(String[] args){
        RefreshSources useCase=new RefreshSources();StringBuilder events=new StringBuilder();
        RefreshSources.Result result=useCase.execute(Arrays.asList(source("selic",false,events),source("ipca",true,events),source("focus",false,events)));
        check(events.toString().equals("selic;ipca;focus;"),"Independent sources are all attempted in stable order");
        check(result.refreshed.size()==2&&result.refreshed.get(0).equals("selic")&&result.refreshed.get(1).equals("focus"),"Successful sources remain available after a partial failure");
        check(result.hasFailures()&&result.failures.size()==1&&result.failures.get(0).sourceId.equals("ipca")&&result.failures.get(0).cause instanceof java.io.IOException,"Failure preserves source identity and cause");
        check(useCase.execute(Collections.emptyList()).refreshed.isEmpty(),"Empty source batch is a valid no-op");
        boolean duplicate=false;try{useCase.execute(Arrays.asList(source("same",false,new StringBuilder()),source("same",false,new StringBuilder())));}catch(IllegalArgumentException expected){duplicate=true;}
        check(duplicate,"Duplicate source IDs are rejected");
        boolean nullSource=false;try{useCase.execute(Collections.singletonList(null));}catch(IllegalArgumentException expected){nullSource=true;}
        check(nullSource,"Null sources are rejected");
        boolean nullBatch=false;try{useCase.execute(null);}catch(IllegalArgumentException expected){nullBatch=true;}
        check(nullBatch,"Missing source list is rejected");
        System.out.println(checks+" independent source refresh checks passed");
    }
}
