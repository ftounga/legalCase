package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-218-11 : résultat interne business de l'analyse de la rupture d'un VRP
 * statutaire (statut, préavis, indemnité de clientèle). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateEntree début du contrat VRP.
 * @param dateRupture date de notification de la rupture.
 * @param causeRupture cause de la rupture.
 * @param typeVrp type de VRP (exclusif / multicartes).
 * @param commissionsAnnuellesMoyennes assiette de l'indemnité de clientèle.
 * @param salaireMensuelMoyen base de l'indemnité légale comparée.
 * @param clienteleDeveloppee condition de fond L. 7313-13 CT.
 * @param ancienneteMois ancienneté en mois à la rupture.
 * @param dureePreavisMois préavis VRP (art. L. 7313-9 CT).
 * @param eligibiliteClientele verdict d'éligibilité à l'indemnité de clientèle.
 * @param motifNonDue motif d'exclusion de l'indemnité de clientèle ; null si DUE.
 * @param indemniteClienteleMin borne basse indicative (1 × commissions) ; 0 si NON_DUE.
 * @param indemniteClienteleMax borne haute indicative (2 × commissions) ; 0 si NON_DUE.
 * @param indemniteLegaleLicenciement indemnité légale comparée (art. R. 1234-2 CT).
 * @param optionRecommandee option la plus favorable (non-cumul) — à confirmer par l'avocat.
 * @param baseJuridique fondements juridiques applicables (à vérifier par avocat).
 */
public record VrpIndemniteClienteleResult(
        LocalDate dateEntree,
        LocalDate dateRupture,
        VrpCauseRupture causeRupture,
        VrpTypeVrp typeVrp,
        BigDecimal commissionsAnnuellesMoyennes,
        BigDecimal salaireMensuelMoyen,
        boolean clienteleDeveloppee,
        long ancienneteMois,
        int dureePreavisMois,
        VrpEligibiliteClientele eligibiliteClientele,
        String motifNonDue,
        BigDecimal indemniteClienteleMin,
        BigDecimal indemniteClienteleMax,
        BigDecimal indemniteLegaleLicenciement,
        VrpOptionRecommandee optionRecommandee,
        String baseJuridique
) {}
