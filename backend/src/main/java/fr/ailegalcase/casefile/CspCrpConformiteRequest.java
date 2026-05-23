package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-212-07 : requête HTTP pour l'endpoint de conformité CSP/CRP
 * (FRANCE — L. 1233-65 à L. 1233-70 CT ; ANI CSP 19/07/2011 ; DARES).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record CspCrpConformiteRequest(
        int effectifEntreprise,
        Boolean cspPropose,
        Boolean documentInformationRemis,
        Boolean delaiReflexionMentionne,
        LocalDate dateRemise,
        LocalDate dateEntretienPrealable,
        Boolean adhesionSalarie,
        double salaireMensuelBrutEuros,
        double remunerationBrute12MoisEuros
) {

    CspCrpConformiteInput toInput() {
        return new CspCrpConformiteInput(
                effectifEntreprise,
                cspPropose,
                documentInformationRemis,
                delaiReflexionMentionne,
                dateRemise,
                dateEntretienPrealable,
                adhesionSalarie,
                salaireMensuelBrutEuros,
                remunerationBrute12MoisEuros
        );
    }
}
