package fr.ailegalcase.jurisprudencemapping;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * F-JU-01 / SF-JU-01-01 — service de lecture des citations jurisprudentielles
 * mappées à une branche de calcul d'un outil décisionnel.
 *
 * <p>Pas d'isolation workspace (table globale {@code tool_jurisprudence_mappings}
 * — jurisprudence identique pour tous les avocats). L'authentification est
 * vérifiée en amont par {@code SecurityConfig} (rôle {@code MEMBER} minimum).</p>
 */
@Service
public class ToolJurisprudenceService {

    private final ToolJurisprudenceMappingRepository repository;

    public ToolJurisprudenceService(ToolJurisprudenceMappingRepository repository) {
        this.repository = repository;
    }

    /**
     * Récupère les 1 à 3 arrêts structurants pour une branche d'un outil.
     *
     * <p>Tri par {@code confidence_score DESC, date_arret DESC} ; exclut les
     * mappings archivés ; limite stricte à 3 résultats imposée par le préfixe
     * {@code findTop3} du repository.</p>
     *
     * @param toolId          identifiant de l'outil ({@code TOOL_REGISTRY})
     * @param brancheCalculId identifiant de la branche de calcul active
     * @return liste de 0 à 3 citations (vide si aucun mapping ; jamais {@code null})
     */
    @Transactional(readOnly = true)
    public List<ToolJurisprudenceCitationResponse> findByToolAndBranch(String toolId, String brancheCalculId) {
        if (toolId == null || brancheCalculId == null || brancheCalculId.isBlank()) {
            return List.of();
        }
        return repository
                .findTop3ByToolIdAndBrancheCalculIdAndArchivedFalseOrderByConfidenceScoreDescDateArretDesc(
                        toolId, brancheCalculId)
                .stream()
                .map(ToolJurisprudenceCitationResponse::from)
                .toList();
    }
}
