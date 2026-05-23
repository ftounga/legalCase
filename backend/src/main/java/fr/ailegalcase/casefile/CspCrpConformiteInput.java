package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-212-07 : données d'entrée du calculateur de conformité de la proposition
 * CSP (FRANCE — L. 1233-65 à L. 1233-70 CT ; ANI CSP 19/07/2011).
 */
public record CspCrpConformiteInput(
        int effectifEntreprise,
        Boolean cspPropose,
        Boolean documentInformationRemis,
        Boolean delaiReflexionMentionne,
        LocalDate dateRemise,
        LocalDate dateEntretienPrealable,
        Boolean adhesionSalarie,
        double salaireMensuelBrutEuros,
        double remunerationBrute12MoisEuros
) {}
