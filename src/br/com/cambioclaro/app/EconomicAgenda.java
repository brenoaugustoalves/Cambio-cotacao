package br.com.cambioclaro.app;

import java.net.URI;

/** Deep links to maintained BCB agenda pages; event dates are not copied into the app. */
public final class EconomicAgenda {
    public static final String COPOM="https://www.bcb.gov.br/acessoinformacao/calendariobc?agenda=Reuni%C3%B5es+do+Copom&tituloAgenda=Calend%C3%A1rio+do+Copom";
    public static final String RELEASES="https://www.bcb.gov.br/acessoinformacao/calendariobc?agenda=Estat%C3%ADsticas+macroecon%C3%B4micas&tituloAgenda=Calend%C3%A1rio+de+divulga%C3%A7%C3%B5es";
    private EconomicAgenda(){}
    public static boolean isOfficial(String value){try{URI uri=URI.create(value);return "https".equalsIgnoreCase(uri.getScheme())&&"www.bcb.gov.br".equalsIgnoreCase(uri.getHost())&&"/acessoinformacao/calendariobc".equals(uri.getPath())&&uri.getUserInfo()==null;}catch(Exception ex){return false;}}
}
