package fr.ailegalcase.analysis;

import java.util.UUID;

/**
 * Message RabbitMQ déclenchant la synthèse globale d'un dossier.
 *
 * <p>F-185 SF-185-03 : ajout du flag {@code provisional}. {@code true} =
 * déclenchement automatique post-DocumentAnalysis (synthèse incrémentale,
 * verdict masqué côté UX), {@code false} = déclenchement manuel par l'avocat
 * (verdict pleinement affiché). Le constructeur 1-arg historique reste
 * disponible pour les call sites manuels et la rétrocompat des messages
 * RabbitMQ déjà sérialisés (Jackson désérialise les anciens payloads avec
 * provisional=false par défaut).
 */
public record CaseAnalysisMessage(UUID caseFileId, boolean provisional) {

    public CaseAnalysisMessage(UUID caseFileId) {
        this(caseFileId, false);
    }
}
