package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-219-08 : résultat brut produit par {@link
 * TransfertEntrepriseCct32bisChecker} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par {@link
 * TransfertEntrepriseCct32bisService} dans {@link
 * TransfertEntrepriseCct32bisResponse}).</p>
 *
 * <p>Indicateurs dérivés : éligibilité CCT 32bis, conformité de la
 * procédure, droits effectivement maintenus (ancienneté, rémunération,
 * CCT), date de fin de responsabilité solidaire cédant/cessionnaire.</p>
 */
public record TransfertEntrepriseCct32bisResult(
        // Inputs (snapshot)
        LocalDate dateTransfert,
        TransfertEntrepriseCct32bisTypeOperation typeOperation,
        TransfertEntrepriseCct32bisIdentiteEconomique identiteEconomique,
        boolean infoConsultPrealableRespectee,
        boolean conseilEntrepriseConsulte,
        boolean maintienContratsAutomatique,
        boolean maintienAncienneteRespecte,
        boolean maintienRemunerationRespecte,
        boolean conventionsCollectivesMaintenues,

        // Outputs
        TransfertEntrepriseCct32bisVerdict verdict,
        boolean eligibleCct32bis,
        boolean procedureConforme,
        boolean droitsMaintenus,
        LocalDate dateFinResponsabiliteSolidaire,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
