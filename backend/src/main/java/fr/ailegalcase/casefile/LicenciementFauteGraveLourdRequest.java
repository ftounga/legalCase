package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-212-01 : requête HTTP pour l'endpoint d'analyse de qualification de faute
 * disciplinaire (FRANCE — L. 1234-1 CT ; L. 1234-9 CT).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record LicenciementFauteGraveLourdRequest(
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
) {

    LicenciementFauteGraveLourdInput toInput() {
        return new LicenciementFauteGraveLourdInput(
                faitsReproches,
                datesFaits,
                dateSaisineEmployeur,
                prescriptionFauteVerifiee,
                ancienneteMois,
                salaireMensuelBrutEuros,
                qualificationEmployeur,
                intentionNuireAlleeguee,
                preuveIntentionNuire,
                attenteConvocation,
                motivationLettreAdequate
        );
    }
}
