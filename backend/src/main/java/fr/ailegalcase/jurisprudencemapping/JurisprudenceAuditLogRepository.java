package fr.ailegalcase.jurisprudencemapping;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * F-JU-01 — accès aux {@link JurisprudenceAuditLog}.
 *
 * <p>SF-JU-01-05 ajoute la lecture paginée pour le dashboard audit.</p>
 */
public interface JurisprudenceAuditLogRepository extends JpaRepository<JurisprudenceAuditLog, UUID> {

    Page<JurisprudenceAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
