package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-01 : réponse de l'endpoint d'analyse de qualification de faute
 * disciplinaire (FRANCE — faute grave / faute lourde).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les
 * sorties calculées (qualification, score, facteurs, indemnités, alerte
 * prescription, bases juridiques).</p>
 */
public record LicenciementFauteGraveLourdResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        String faitsReproches,
        List<LocalDate> datesFaits,
        LocalDate dateSaisineEmployeur,
        Boolean prescriptionFauteVerifiee,
        int ancienneteMois,
        double salaireMensuelBrutEuros,
        LicenciementFauteGraveLourdCalculator.QualificationFaute qualificationEmployeur,
        Boolean intentionNuireAlleeguee,
        String preuveIntentionNuire,
        Boolean attenteConvocation,
        Boolean motivationLettreAdequate,
        // --- Outputs calculés ---
        LicenciementFauteGraveLourdCalculator.QualificationFaute qualificationRetenue,
        int scoreQualification,
        List<LicenciementFauteGraveLourdCalculator.FacteurQualification> facteursQualification,
        double indemnitePreavisEuros,
        double indemniteLegaleEuros,
        double indemnitesCongesPayesEuros,
        double totalIndemnitesDuesEuros,
        boolean alertePrescriptionFaute,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
