package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-207-03 : projection complète (snapshot) renvoyée au client pour l'outil
 * de contestation d'une décision C4 ONEM.
 *
 * <p>Inclut les inputs (pour pré-fill UI) et les outputs calculés (verdict,
 * paliers, étape suivante, base juridique, formule de calcul).</p>
 */
public record ContestationC4OnemResponse(
        UUID caseFileId,
        // Inputs normalisés (pré-fill UI)
        LocalDate dateNotificationDecisionOnem,
        LocalDate dateActionEnvisagee,
        boolean recoursAdminDejaForme,
        LocalDate dateDecisionDirecteur,
        // Outputs calculés
        ContestationC4OnemResult.Verdict verdict,
        List<ContestationC4OnemResult.Palier> paliers,
        ContestationC4OnemResult.EtapeSuivante etapeSuivante,
        String baseJuridique,
        String formuleCalcul,
        // Workspace context (toujours BELGIQUE pour cet outil)
        String country
) {}
