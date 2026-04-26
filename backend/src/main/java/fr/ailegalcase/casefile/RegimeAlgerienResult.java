package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-IM-17-01 : résultat de l'analyse du régime franco-algérien (accord 27/12/1968).
 *
 * @param voieDemande            code voie analysée (normalisé)
 * @param voieRecommandee        libellé de la voie (CRA 1 an art. 5, etc.)
 * @param verdictRecevabilite    ELEVEE / MOYENNE / FAIBLE
 * @param titreApplicable        code du titre délivré (équivalent voie)
 * @param dureeTitreAnnees       durée du titre (1 ou 10 ans, ou 0 pour regroupement / changement)
 * @param criteresNonRemplis     critères non remplis bloquants
 * @param documentsRequis        liste des pièces à produire pour cette voie
 * @param delaiInstructionMois   délai d'instruction estimatif (mois)
 * @param baseJuridique          accord + article applicable
 * @param formule                résumé en une ligne
 * @param messages               conseils pratiques
 */
public record RegimeAlgerienResult(
        String voieDemande,
        String voieRecommandee,
        String verdictRecevabilite,
        String titreApplicable,
        int dureeTitreAnnees,
        List<String> criteresNonRemplis,
        List<String> documentsRequis,
        int delaiInstructionMois,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
