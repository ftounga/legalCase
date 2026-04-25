package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-IM-11-01 : résultat d'une analyse de changement de statut CESEDA.
 *
 * @param titreActuel              code du titre actuel
 * @param titreEnvisage            code du titre envisagé
 * @param nouveauTitreEnvisage     code normalisé du nouveau titre (peut différer de titreEnvisage si APS)
 * @param dureeRestanteMois        durée restante sur titre actuel
 * @param documentJustificatifFourni justificatif fourni
 * @param remunerationContratEur   rémunération du contrat (peut être null)
 * @param casierJudiciaireVierge   casier vierge
 * @param verdictTransition        ELEVEE / MOYENNE / FAIBLE
 * @param documentsRequis          liste des pièces à fournir
 * @param risqueRefus              liste des risques de refus identifiés
 * @param delaiInstructionMois     délai estimatif d'instruction (mois)
 * @param baseJuridique            articles CESEDA / Code du travail applicables
 * @param formule                  résumé en une ligne
 * @param messages                 conseils pratiques
 */
public record ChangementStatutResult(
        String titreActuel,
        String titreEnvisage,
        String nouveauTitreEnvisage,
        int dureeRestanteMois,
        boolean documentJustificatifFourni,
        BigDecimal remunerationContratEur,
        boolean casierJudiciaireVierge,
        String verdictTransition,
        List<String> documentsRequis,
        List<String> risqueRefus,
        int delaiInstructionMois,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
