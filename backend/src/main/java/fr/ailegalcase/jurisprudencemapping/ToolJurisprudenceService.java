package fr.ailegalcase.jurisprudencemapping;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-01 — service de lecture des citations jurisprudentielles
 * mappées à une branche de calcul d'un outil décisionnel.
 *
 * <p>Pas d'isolation workspace (table globale {@code tool_jurisprudence_mappings}
 * — jurisprudence identique pour tous les avocats). L'authentification est
 * vérifiée en amont par {@code SecurityConfig} (rôle {@code MEMBER} minimum).</p>
 *
 * <p>SF-JU-01-04 ajoute {@link #signalProblem} qui crée un
 * {@link JurisprudenceWatchFlag} source {@code USER_SIGNAL}.</p>
 */
@Service
public class ToolJurisprudenceService {

    private final ToolJurisprudenceMappingRepository repository;
    private final JurisprudenceWatchFlagRepository flagRepository;

    public ToolJurisprudenceService(ToolJurisprudenceMappingRepository repository,
                                    JurisprudenceWatchFlagRepository flagRepository) {
        this.repository = repository;
        this.flagRepository = flagRepository;
    }

    /**
     * Récupère les 1 à 3 arrêts structurants pour une branche d'un outil.
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

    /**
     * F-JU-01 / SF-JU-01-04 — signale un problème sur une citation. Crée un
     * {@link JurisprudenceWatchFlag} source {@code USER_SIGNAL}, statut
     * {@code PENDING}.
     *
     * @throws ResponseStatusException 404 si la citation n'existe pas ou est archivée
     */
    @Transactional
    public void signalProblem(String toolId, UUID citationId, String comment) {
        ToolJurisprudenceMapping mapping = repository.findById(citationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Citation not found"));
        if (mapping.isArchived() || !mapping.getToolId().equals(toolId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Citation not found");
        }

        JurisprudenceWatchFlag flag = new JurisprudenceWatchFlag();
        flag.setToolId(mapping.getToolId());
        flag.setBrancheCalculId(mapping.getBrancheCalculId());
        flag.setArretEntrantRef(mapping.getArretRef());
        flag.setMappingActuel(mapping);
        flag.setSource(JurisprudenceWatchFlagSource.USER_SIGNAL);
        flag.setStatut(JurisprudenceWatchFlagStatut.PENDING);
        if (comment != null && !comment.isBlank()) {
            flag.setCommentUser(comment.trim());
        }
        flagRepository.save(flag);
    }
}
