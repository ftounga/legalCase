package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-215-15 : résultat interne business du calcul du délai de recours en
 * <b>extrême urgence</b> devant le Conseil du Contentieux des Étrangers (CCE) —
 * 5 jours <b>ouvrables</b> depuis l'acte exécutoire imminent
 * (art. 39/82 §4 al. 2-3 Loi 15/12/1980).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>F-IM-31 (recours en annulation — 30 jours calendaires, SF-215-13) ;</li>
 *   <li>F-IM-08 (Annexe 13 — calculateur OQT simple) ;</li>
 *   <li>F-IM-06 (générateur générique du document de recours).</li>
 * </ul>
 */
public record CceExtremeUrgenceBeResult(
        LocalDate dateActeExecutoire,
        CceExtremeUrgenceBeTypeActeEnum typeActe,
        boolean recoursForme,
        LocalDate dateRecours,
        LocalDate dateLimiteRecours,
        long joursOuvrablesRestants,
        CceExtremeUrgenceBeStatut statut,
        LocalDate audienceEstimee,
        String actionImmediate,
        String recommandation,
        String baseJuridique
) {}
