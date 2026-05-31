package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-218-25 : requête POST pour l'analyse du licenciement pour fin de chantier /
 * d'opération du CDI de chantier (art. L.1223-8 et s. ; L.1236-8 CT, F-DT-37).
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateEntree début du contrat de chantier (requise).
 * @param dateRupture date de notification du licenciement (requise,
 *        ≥ dateEntree).
 * @param fondementRecours fondement juridique du recours au CDI de chantier
 *        (ACCORD_BRANCHE_ETENDU | USAGE_CONSTANT_SECTEUR | AUCUN, requis).
 * @param secteur secteur d'activité de l'employeur (BTP | INGENIERIE | AUTRE,
 *        requis).
 * @param chantierAcheve true si le chantier / l'opération pour lequel le salarié
 *        a été engagé est réalisé (requis).
 * @param salaireMensuelMoyen salaire mensuel moyen de référence, base de
 *        l'indemnité de licenciement (requis, ≥ 0).
 * @param reclassementAutreChantierPropose true si une poursuite sur un autre
 *        chantier a été proposée au salarié (défaut false).
 */
public record CdiChantierRequest(
        LocalDate dateEntree,
        LocalDate dateRupture,
        CdiChantierFondementRecours fondementRecours,
        CdiChantierSecteur secteur,
        Boolean chantierAcheve,
        BigDecimal salaireMensuelMoyen,
        Boolean reclassementAutreChantierPropose
) {}
