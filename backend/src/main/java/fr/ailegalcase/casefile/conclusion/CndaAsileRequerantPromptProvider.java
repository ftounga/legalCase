package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-23 — cellule de matrice : recours du <strong>requérant</strong>
 * (demandeur d'asile) devant la Cour nationale du droit d'asile (CNDA), au stade
 * recours asile, droit de l'immigration, France.
 *
 * <p>Recours contre une décision de l'OFPRA rejetant une demande de protection.
 * La CNDA statue en <strong>plein contentieux</strong> : elle se prononce
 * elle-même sur le droit à la protection (reconnaissance de la qualité de
 * réfugié ou octroi de la protection subsidiaire). Ancrage : CESEDA (livre V —
 * asile) et convention de Genève du 28 juillet 1951.</p>
 */
@Component
public class CndaAsileRequerantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — CNDA / recours asile / requérant / droit de l'immigration FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du requérant (demandeur d'asile) devant la Cour nationale du droit d'asile (CNDA).
            Rédige un PROJET DE RECOURS contre la décision de l'OFPRA rejetant la demande de protection.
            Ancrage juridique : CESEDA (livre V — asile) et convention de Genève du 28 juillet 1951.
            La CNDA statue en PLEIN CONTENTIEUX : elle ne se borne pas à annuler la décision de l'OFPRA,
            elle se prononce elle-même sur le droit à la protection.
            Structure le recours :
            - en-tête (POUR [requérant] / recours contre la décision de l'OFPRA),
            - exposé du PARCOURS et des CRAINTES du demandeur,
            - RECEVABILITÉ du recours (délai, intérêt à agir),
            - DISCUSSION : éligibilité au STATUT DE RÉFUGIÉ (craintes de persécution pour un motif
              conventionnel — race, religion, nationalité, opinions politiques, appartenance à un certain
              groupe social) et, subsidiairement, à la PROTECTION SUBSIDIAIRE ; apprécie la crédibilité
              du récit, l'actualité et le caractère personnel des craintes,
            - PAR CES MOTIFS (annuler la décision de l'OFPRA et reconnaître la qualité de réfugié
              ou, à défaut, accorder le bénéfice de la protection subsidiaire).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les éléments exacts des analyses fournies — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_IMMIGRATION,
                ProcedureStageCatalog.FRANCE, "CNDA", "RECOURS_ASILE", "REQUERANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
