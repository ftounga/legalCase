package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-05 : résultat interne business de l'analyse d'un pourvoi en cassation
 * devant la chambre sociale de la Cour de cassation. Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Calcule le délai du pourvoi (2 mois à compter de la notification de
 * l'arrêt — art. 612 CPC), analyse la force probatoire de chaque cas
 * d'ouverture (art. 604 CPC), évalue le risque de non-admission (filtre NPC,
 * art. 1014 CPC) et produit la checklist des démarches (constitution d'un
 * avocat aux Conseils — art. 973 CPC).
 *
 * @param dateNotificationArret date de notification de l'arrêt de la Cour
 *        d'appel (point de départ du délai — art. 612 CPC).
 * @param dateLimitePourvoi date limite pour former le pourvoi (notification +
 *        2 mois).
 * @param joursRestants nombre de jours restants avant l'expiration (négatif si
 *        le délai est expiré).
 * @param verdictDelai verdict du délai (DELAI_OUVERT / DELAI_URGENT /
 *        DELAI_EXPIRE).
 * @param casOuvertureAnalyses analyse de chaque cas d'ouverture invoqué.
 * @param risqueNonAdmission risque de non-admission par la formation restreinte
 *        (ELEVE / MODERE / FAIBLE — art. 1014 CPC).
 * @param representationAvocatCassation true si un avocat aux Conseils est
 *        constitué (obligatoire — art. 973 CPC).
 * @param moyenSerieuxIdentifie true si un moyen sérieux de cassation est
 *        identifié (anti-filtre NPC).
 * @param verdict verdict global d'orientation du pourvoi.
 * @param checklist checklist des démarches du pourvoi.
 * @param baseJuridique fondements juridiques applicables.
 */
public record PourvoiCassationSocResult(
        LocalDate dateNotificationArret,
        LocalDate dateLimitePourvoi,
        long joursRestants,
        PourvoiCassationSocVerdictDelai verdictDelai,
        List<PourvoiCassationSocCasOuvertureAnalyse> casOuvertureAnalyses,
        PourvoiCassationSocRisqueNonAdmission risqueNonAdmission,
        boolean representationAvocatCassation,
        boolean moyenSerieuxIdentifie,
        PourvoiCassationSocVerdict verdict,
        List<PourvoiCassationSocChecklistItem> checklist,
        String baseJuridique
) {}
