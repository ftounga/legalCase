package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-11 : réponse de l'endpoint d'analyse de la modification du contrat
 * refusée par le salarié (F-DT-70, FRANCE — Cass. soc. ; L. 1222-6 CT).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors du
 * GET) ET les sorties calculées (verdict, score, conséquences du refus,
 * alerte délai L. 1222-6, bases juridiques).</p>
 */
public record ModificationContratRefusResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        ModificationContratRefusInput.ElementModifie elementModifie,
        boolean elementExplicitementContractualise,
        boolean motifEconomique,
        Boolean notificationEcriteL1222_6,
        Integer delaiReflexionRespecteMois,
        ModificationContratRefusInput.ReponseSalarie reponseSalarie,
        // --- Outputs calculés ---
        ModificationContratRefusCalculator.AnalyseModificationContrat analyseModification,
        int scoreModificationContrat,
        List<ModificationContratRefusCalculator.Consequence> consequences,
        boolean alerteDelaiReflexion,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
