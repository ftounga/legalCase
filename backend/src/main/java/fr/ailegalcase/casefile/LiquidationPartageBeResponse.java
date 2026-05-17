package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-217-02 : réponse de l'endpoint de suivi de la procédure de liquidation-partage
 * belge (notaire commis).
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire — leçon F-DT-36) ET les sorties calculées (verdict,
 * checklist d'étapes, délais, bases juridiques, messages).</p>
 */
public record LiquidationPartageBeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        Boolean notaireDesigne,
        LocalDate dateDesignationNotaire,
        Boolean operationsOuvertes,
        LocalDate dateOuvertureOperations,
        Boolean inventaireEtabli,
        Boolean projetLiquidationEtabli,
        LocalDate dateNotificationProjet,
        Boolean contreditsDeposes,
        Boolean procesVerbalDiresEtabli,
        Boolean homologationDemandee,
        LocalDate dateHomologation,
        String commentaire,
        // --- Outputs calculés ---
        LiquidationPartageBeCalculator.LiquidationPartageBeVerdict verdict,
        List<LiquidationPartageBeCalculator.EtapeProcedure> etapes,
        List<LiquidationPartageBeCalculator.DelaiProcedure> delais,
        String prochaineEtape,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
