package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-215-13 : résultat interne business du calcul du délai de recours en
 * annulation devant le Conseil du Contentieux des Étrangers (CCE) — 30 jours
 * calendaires depuis la notification (art. 39/82 §4 al. 1 Loi 15/12/1980).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>F-IM-32 (recours en extrême urgence — 5 jours ouvrables, SF-215-15) ;</li>
 *   <li>F-IM-08 (Annexe 13 — calculateur OQT simple) ;</li>
 *   <li>F-IM-06 (générateur générique du document de recours).</li>
 * </ul>
 */
public record CceAnnulationBeResult(
        LocalDate dateNotificationDecision,
        CceAnnulationBeTypeDecisionEnum typeDecision,
        boolean recoursForme,
        LocalDate dateRecours,
        LocalDate dateLimiteRecours,
        long joursRestants,
        CceAnnulationBeStatut statut,
        String recommandation,
        String delaisMemoire,
        String baseJuridique
) {}
