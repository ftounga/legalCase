package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-IM-12-01 : résultat d'une analyse d'asile avancé (CESEDA Livre V).
 *
 * @param dispositifAsile        code dispositif analysé (normalisé)
 * @param dispositifLibelle      libellé du dispositif
 * @param verdictRecevabilite    RECEVABLE_TRANSFERT / FRANCE_COMPETENTE / ACCELEREE_APPLICABLE
 *                               / ACCELEREE_NON_APPLICABLE / RECEVABLE_REEXAMEN / IRRECEVABLE
 *                               / RECEVABLE_APATRIDIE / RECEVABLE_PROTECTION_SUBSIDIAIRE
 * @param delaiInstructionMois   délai estimatif d'instruction (en mois ; peut être fractionnaire :
 *                               1.5 procédure accélérée, 0.3 réexamen = 8 jours)
 * @param recoursPossible        description du recours (suspensif Dublin III ; CNDA pour les autres)
 * @param documentsRequis        liste des pièces à produire
 * @param risqueRefus            liste des risques identifiés bloquant la procédure
 * @param baseJuridique          articles applicables (Règlement UE 604/2013 ou CESEDA L.512+)
 * @param formule                résumé en une ligne
 * @param messages               conseils pratiques
 */
public record AsileAvanceResult(
        String dispositifAsile,
        String dispositifLibelle,
        String verdictRecevabilite,
        double delaiInstructionMois,
        String recoursPossible,
        List<String> documentsRequis,
        List<String> risqueRefus,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
