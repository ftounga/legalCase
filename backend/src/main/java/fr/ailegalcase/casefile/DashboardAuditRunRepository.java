package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * F-180 SF-180-01 — accès aux runs d'audit dashboard historisés.
 */
public interface DashboardAuditRunRepository extends JpaRepository<DashboardAuditRun, UUID> {

    /** Dernier run produit — lu par {@code GET /dashboard-audit/latest}. */
    Optional<DashboardAuditRun> findFirstByOrderByRanAtDesc();
}
