package fr.ailegalcase.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    long countByPlanCode(String planCode);

    long countByCreatedAtAfter(Instant since);

    /**
     * SF-122-01 + SF-122-02 : incrémente les compteurs OCR atomiquement avec
     * auto-reset journalier ET mensuel.
     *
     * - Compteur journalier : reset à {@code :pages} si {@code ocr_usage_last_reset_date} ≠ {@code :today}
     * - Compteur mensuel : reset à {@code :pages} si {@code ocr_usage_last_reset_date} est
     *   dans un mois différent de {@code :today}
     *
     * Pas de scheduled task nécessaire — le reset est intégré dans l'UPDATE.
     *
     * @return nombre de lignes affectées (1 si OK, 0 si workspace introuvable)
     */
    /**
     * SF-122-01/02/04 + fix staging 2026-04-19 : remplace EXTRACT(YEAR/MONTH)
     * par une comparaison de date directe (>= monthStart) pour éviter l'erreur
     * PostgreSQL "function pg_catalog.extract(unknown, unknown) is not unique".
     * Sémantique identique : si last_reset_date est ≥ 1er du mois courant, c'est
     * le même mois, on incrémente ; sinon on reset.
     */
    @Modifying
    @Query("""
            UPDATE Workspace w
            SET w.ocrPagesUsedCurrentMonth = CASE
                    WHEN w.ocrUsageLastResetDate IS NOT NULL
                     AND w.ocrUsageLastResetDate >= :monthStart
                    THEN w.ocrPagesUsedCurrentMonth + :pages
                    ELSE :pages
                END,
                w.ocrPagesUsedCurrentDay = CASE
                    WHEN w.ocrUsageLastResetDate = :today THEN w.ocrPagesUsedCurrentDay + :pages
                    ELSE :pages
                END,
                w.ocrPagesUsedAllTime = w.ocrPagesUsedAllTime + :pages,
                w.ocrUsageLastResetDate = :today
            WHERE w.id = :workspaceId
            """)
    int incrementOcrUsage(@Param("workspaceId") UUID workspaceId,
                           @Param("pages") int pages,
                           @Param("today") LocalDate today,
                           @Param("monthStart") LocalDate monthStart);
}
