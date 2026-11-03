package br.com.cambioclaro.app.economy.presentation;

import br.com.cambioclaro.app.economy.application.EconomicScenario;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formats cost simulation totals alongside their real quote source and publication time. */
public final class EconomicScenarioPresenter {
    private static final Locale BR=new Locale("pt","BR");
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm",BR);
    public EconomicScenarioDisplay present(EconomicScenario scenario,ZoneId zone){
        if(scenario==null||scenario.reference==null||scenario.totals==null||zone==null)throw new IllegalArgumentException("Resultado do cenário inválido");
        String published=DATE.format(scenario.reference.publishedAt.atZone(zone))+" (Brasília)";
        String status=scenario.reference.source+" · referência publicada em "+published+(scenario.reference.warning==null?"":" · "+scenario.reference.warning);
        String details="Base: R$ "+number(scenario.totals.base)+"\nSpread informado: R$ "+number(scenario.totals.spread)+"\nOutros encargos: R$ "+number(scenario.totals.extra)+"\nTarifa fixa: R$ "+number(scenario.totals.fixedFee)+"\nTotal estimado: R$ "+number(scenario.totals.total)+"\nPercentuais calculados sobre o valor-base, sem capitalização.";
        return new EconomicScenarioDisplay(status,details);
    }
    public EconomicScenarioDisplay unavailable(){return new EconomicScenarioDisplay("Fonte sem dado válido; confira Diagnóstico e tente novamente.","Cálculo indisponível sem uma taxa publicada válida.");}
    private String number(BigDecimal value){NumberFormat f=NumberFormat.getNumberInstance(BR);f.setMinimumFractionDigits(2);f.setMaximumFractionDigits(2);return f.format(value.setScale(2,RoundingMode.HALF_UP));}
}
