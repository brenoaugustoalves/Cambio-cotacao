package br.com.cambioclaro.app;

import java.net.URI;

/** Fixed direct links to official B3 and BLS news pages; no scraping or republication. */
public final class MarketNewsLinks {
    public static final String B3="https://www.b3.com.br/pt_br/noticias/";
    public static final String BLS_CPI="https://www.bls.gov/cpi/news.htm";
    public static final String BLS_EMPLOYMENT="https://www.bls.gov/ces/";
    private MarketNewsLinks(){}
    public static boolean official(String value){try{URI uri=URI.create(value);if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getUserInfo()!=null)return false;String host=uri.getHost(),path=uri.getPath();return ("www.b3.com.br".equalsIgnoreCase(host)&&"/pt_br/noticias/".equals(path))||("www.bls.gov".equalsIgnoreCase(host)&&("/cpi/news.htm".equals(path)||"/ces/".equals(path)));}catch(Exception ex){return false;}}
}
