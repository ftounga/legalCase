package fr.ailegalcase.jurisprudencemapping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-01 — accès aux {@link ToolJurisprudenceMapping}.
 */
public interface ToolJurisprudenceMappingRepository extends JpaRepository<ToolJurisprudenceMapping, UUID> {

    /**
     * Récupère les 3 arrêts les plus pertinents pour une branche d'un outil :
     * tri par {@code confidence_score DESC, date_arret DESC}, exclut les
     * mappings archivés. Spring Data JPA garantit la limite stricte à 3
     * résultats via le préfixe {@code findTop3}.
     */
    List<ToolJurisprudenceMapping> findTop3ByToolIdAndBrancheCalculIdAndArchivedFalseOrderByConfidenceScoreDescDateArretDesc(
            String toolId, String brancheCalculId);
}
