package fr.ailegalcase.jurisprudencemapping;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * F-JU-01 — accès aux {@link JurisprudenceWatchFlag}.
 *
 * <p>SF-JU-01-05 ajoute les requêtes paginées pour le dashboard admin.</p>
 */
public interface JurisprudenceWatchFlagRepository extends JpaRepository<JurisprudenceWatchFlag, UUID> {

    Page<JurisprudenceWatchFlag> findByStatutOrderByCreatedAtDesc(
            JurisprudenceWatchFlagStatut statut, Pageable pageable);
}
