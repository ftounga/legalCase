package fr.ailegalcase.jurisprudencemapping;

import java.util.Set;

/**
 * F-JU-01 / SF-JU-01-03 — interface que les outils décisionnels implémentent
 * pour déclarer leurs branches de calcul valides.
 *
 * <p>Le {@link JurisprudenceDriftService} compare l'union de tous les beans
 * {@code ToolBranchRegistry} aux mappings actifs de
 * {@code tool_jurisprudence_mappings}. Chaque mapping dont la clé
 * {@code toolId:brancheCalculId} ne figure pas dans le registry est
 * automatiquement archivé (la branche a été supprimée / renommée côté code).</p>
 *
 * <p>Format de clé attendu : {@code toolId + ":" + brancheCalculId}.</p>
 */
public interface ToolBranchRegistry {

    /**
     * Retourne les clés {@code toolId:brancheCalculId} déclarées par cet
     * outil. Jamais {@code null}.
     */
    Set<String> knownBranches();
}
