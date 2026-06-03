package fr.ailegalcase.jurisprudencemapping;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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

    /**
     * SF-JU-01-FIX (2026-06-03) — seuil de confiance minimal pour qu'une citation
     * soit affichée/citée. En dessous, l'arrêt est jugé trop incertain → masqué
     * (invariant « silence &gt; erreur »). Décision PO 2026-06-03.
     */
    private static final BigDecimal MIN_DISPLAY_CONFIDENCE = new BigDecimal("0.60");

    /** Limite : top 3 citations parmi les candidats « affichables ». */
    private static final Pageable TOP_3 = PageRequest.of(0, 3);

    private final ToolJurisprudenceMappingRepository repository;
    private final JurisprudenceWatchFlagRepository flagRepository;

    public ToolJurisprudenceService(ToolJurisprudenceMappingRepository repository,
                                    JurisprudenceWatchFlagRepository flagRepository) {
        this.repository = repository;
        this.flagRepository = flagRepository;
    }

    /**
     * Récupère les 1 à 3 arrêts structurants « affichables » pour une branche d'un
     * outil. SF-JU-01-FIX : exclut les citations à chapeau vide ou à confiance
     * &lt; {@link #MIN_DISPLAY_CONFIDENCE}. Point de lecture commun à l'affichage
     * outil (F-JU-01) et aux conclusions générées (F-JU-02).
     */
    @Transactional(readOnly = true)
    public List<ToolJurisprudenceCitationResponse> findByToolAndBranch(String toolId, String brancheCalculId) {
        if (toolId == null || brancheCalculId == null || brancheCalculId.isBlank()) {
            return List.of();
        }
        return repository
                .findDisplayableByToolAndBranch(toolId, brancheCalculId, MIN_DISPLAY_CONFIDENCE, TOP_3)
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
