package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-27-01 : résultat de l'analyse de validité procédurale du licenciement pour motif grave
 * en Belgique (art. 35 Loi 03/07/1978).
 */
public record MotifGraveBeResult(
        LocalDate dateConnaissanceFait,
        LocalDate dateNotificationRupture,
        LocalDate dateNotificationMotifs,
        int anciennetteAnnees,
        BigDecimal salaireMensuelReference,
        int delaiRuptureJoursOuvrables,
        int delaiMotifsJoursOuvrables,
        boolean motifGraveProceduralementValide,
        BigDecimal indemnitePreavisSiInvalide,
        BigDecimal indemniteManifestementDeraisonnableMin,
        BigDecimal indemniteManifestementDeraisonnableMax,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
