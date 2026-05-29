package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-13 : résultat interne business du calcul du délai de dépôt du
 * renouvellement du titre de séjour — 2 mois avant l'expiration
 * (art. R. 433-1 CESEDA).
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>F-IM-28 (validation VLS-TS OFII 3 mois après l'entrée) ;</li>
 *   <li>F-IM-01 / F-IM-21 (checklist et procédure de renouvellement détaillée).</li>
 * </ul>
 *
 * @param dateOptimalDepot date conseillée de dépôt = expiration − 2 mois (R. 433-1).
 * @param dateDepotImperatif seuil critique = expiration − 1 mois.
 * @param joursRestantsAvantOptimal jours calendaires avant la date optimale
 *        (négatif si dépassée) ; {@code null} lorsque le dépôt est déjà effectué.
 * @param joursRestantsAvantImperatif jours calendaires avant le seuil impératif
 *        (négatif si dépassé) ; {@code null} lorsque le dépôt est déjà effectué.
 * @param risqueIrruption {@code true} si le titre est expiré sans dépôt — risque
 *        d'interruption des droits.
 * @param alerteRetard {@code true} si le dépôt a été effectué plus de 15 jours
 *        après l'expiration (dépôt accepté pour info mais hors délai).
 */
public record RenouvellementDelaiResult(
        LocalDate dateExpirationTitre,
        LocalDate dateDepotDossier,
        String typeTitre,
        LocalDate dateOptimalDepot,
        LocalDate dateDepotImperatif,
        Long joursRestantsAvantOptimal,
        Long joursRestantsAvantImperatif,
        RenouvellementDelaiStatut statut,
        boolean risqueIrruption,
        boolean alerteRetard,
        String recommandation,
        String baseJuridique
) {}
