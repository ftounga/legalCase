package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-33 : réponse de l'endpoint de requalification d'un contrat
 * à temps partiel en temps complet
 * (F-DT-49-temps-partiel-requalification, FRANCE — L. 3123-1 à
 * L. 3123-20 CT ; L. 3123-6 mentions obligatoires ; L. 3123-9 plafond
 * heures complémentaires ; L. 3245-1 CT prescription triennale du
 * rappel de salaire ; Cass. soc. 22/01/1992 présomption de temps
 * complet réfragable).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors
 * du GET) ET les sorties calculées (verdict 3 niveaux, score, facteurs
 * d'analyse F-IA-03, rappel de salaire estimé, bases juridiques,
 * messages).</p>
 */
public record TempsPartielRequalificationResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        Double dureeContractuelleHeures,
        Boolean mentionsDureePresentes,
        Boolean mentionsRepartitionPresentes,
        Boolean mentionsHCPresentes,
        Double heuresComplementairesReelleMoyenneHeures,
        Boolean modificationRepartitionUnilaterale,
        Integer ancienneteMois,
        Double salaireMensuelContractuelEuros,
        // --- Outputs calculés ---
        TempsPartielRequalificationCalculator.AnalyseTempsPartielRequalification analyseRequalification,
        int scoreRequalification,
        List<TempsPartielRequalificationCalculator.FacteurRequalification> facteursRequalification,
        Double rappelSalaire3AnsEstimeEuros,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
