package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-13 : réponse de l'analyse de la rupture du contrat d'un salarié du
 * particulier employeur (CESU / assistant maternel) — préavis et indemnité de
 * licenciement / de rupture. Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record ParticulierEmployeurCesuResponse(
        UUID caseFileId,
        LocalDate dateEntree,
        LocalDate dateRupture,
        ParticulierEmployeurCesuCategorie categorieEmploye,
        ParticulierEmployeurCesuCause causeRupture,
        BigDecimal salaireMensuelMoyen,
        int ancienneteMois,
        int dureePreavisJours,
        String dureePreavisLibelle,
        ParticulierEmployeurCesuEligibilite eligibiliteIndemnite,
        String motifNonDue,
        BigDecimal indemniteLicenciement,
        ParticulierEmployeurCesuMethode methodeCalcul,
        ParticulierEmployeurCesuVerdict verdict,
        String country,
        String baseJuridique
) {}
