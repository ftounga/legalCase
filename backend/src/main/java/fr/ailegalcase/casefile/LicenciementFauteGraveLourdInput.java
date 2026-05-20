package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-212-01 : données d'entrée du calculateur de qualification de faute
 * disciplinaire (faute grave / faute lourde — FRANCE).
 */
public record LicenciementFauteGraveLourdInput(
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
        Boolean motivationLettreAdequate
) {}
