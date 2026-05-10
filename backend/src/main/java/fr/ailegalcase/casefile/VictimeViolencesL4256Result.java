package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-208-04 : résultat de l'analyse d'éligibilité au titre de séjour vie privée
 * et familiale "victime de violences" (CESEDA L.425-6 ; ancien L.316-3).
 *
 * <p>Outil <b>single-country FR</b> (mécanisme spécifique français articulé avec
 * l'ordonnance de protection JAF, Cciv 515-9 à 515-13).
 */
public record VictimeViolencesL4256Result(
        LocalDate dateOrdonnanceProtection,
        String juridiction,
        int dureeProtectionMois,
        LocalDate dateExpirationProtectionEffective,
        int enfantsAcharge,
        String nationalite,
        String eligibiliteScore,
        List<String> criteresValides,
        List<String> criteresManquants,
        int dureeTitreSejourMois,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
