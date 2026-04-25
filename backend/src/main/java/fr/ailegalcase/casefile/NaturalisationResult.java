package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-IM-13-01 : résultat d'une analyse de naturalisation française (Code civil 21+).
 *
 * @param voieNaturalisation     code voie analysée (normalisé)
 * @param voieRecommandee        libellé de la voie (Décret art. 21-15, etc.)
 * @param verdictRecevabilite    ELEVEE / MOYENNE / FAIBLE
 * @param criteresNonRemplis     liste des critères non remplis (avocat doit corriger)
 * @param documentsAFournir      liste des pièces à produire pour cette voie
 * @param delaiInstructionMois   délai estimatif d'instruction (mois)
 * @param baseJuridique          articles Code civil applicables
 * @param formule                résumé en une ligne
 * @param messages               conseils pratiques (discrétion gouvernementale, etc.)
 */
public record NaturalisationResult(
        String voieNaturalisation,
        String voieRecommandee,
        String verdictRecevabilite,
        List<String> criteresNonRemplis,
        List<String> documentsAFournir,
        int delaiInstructionMois,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
