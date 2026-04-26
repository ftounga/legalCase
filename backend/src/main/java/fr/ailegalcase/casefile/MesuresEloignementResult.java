package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-IM-20-01 : résultat de l'analyse d'une mesure d'éloignement administrative française
 * (Expulsion / IRTF / IAT — distincte de l'OQTF F-IM-08).
 *
 * @param dispositif            code dispositif analysé (normalisé)
 * @param dispositifRecommande  libellé enrichi (ex. "Expulsion préfectorale art. L.631-1")
 * @param motifMenace           code motif normalisé
 * @param verdictLegalite       VALIDE / CONTESTABLE / NUL
 * @param risqueAnnulation      liste des risques d'annulation identifiés
 * @param delaiRecoursJours     délai de recours en jours (15 / 30 / 60)
 * @param juridictionRecours    TA / CE
 * @param documentsRequis       pièces à produire pour le recours
 * @param baseJuridique         articles CESEDA applicables
 * @param formule               résumé en une ligne
 * @param messages              conseils pratiques (procédure, calendrier, etc.)
 */
public record MesuresEloignementResult(
        String dispositif,
        String dispositifRecommande,
        String motifMenace,
        String verdictLegalite,
        List<String> risqueAnnulation,
        int delaiRecoursJours,
        String juridictionRecours,
        List<String> documentsRequis,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
