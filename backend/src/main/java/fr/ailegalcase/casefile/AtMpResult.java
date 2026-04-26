package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-DT-33-01 : résultat de l'analyse d'une procédure AT/MP française.
 *
 * @param dispositif            code dispositif analysé (normalisé)
 * @param dispositifLibelle     libellé enrichi (ex. "Reconnaissance AT (CSS L.411-1)")
 * @param verdictRecevabilite   ELEVEE / MOYENNE / FAIBLE
 * @param delaiInstructionJours délai d'instruction prévisionnel (90 / 120 / 60+60)
 * @param competence            CPAM / CRRMP / CMRA / TJ_POLE_SOCIAL
 * @param expertiseRequise      true si expertise médicale obligatoire
 * @param documentsRequis       pièces à produire pour la procédure
 * @param risqueRefus           liste des risques de refus / motifs faibles identifiés
 * @param baseJuridique         articles CSS applicables
 * @param formule               résumé en une ligne
 * @param messages              conseils pratiques (calendrier, étapes, etc.)
 */
public record AtMpResult(
        String dispositif,
        String dispositifLibelle,
        String verdictRecevabilite,
        int delaiInstructionJours,
        String competence,
        boolean expertiseRequise,
        List<String> documentsRequis,
        List<String> risqueRefus,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
