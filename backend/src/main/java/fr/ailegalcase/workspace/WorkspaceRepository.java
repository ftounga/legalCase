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
    @Modifying
    @Query("""
            UPDATE Workspace w
            SET w.ocrPagesUsedCurrentMonth = CASE
                    WHEN EXTRACT(YEAR FROM w.ocrUsageLastResetDate) = EXTRACT(YEAR FROM :today)
                     AND EXTRACT(MONTH FROM w.ocrUsageLastResetDate) = EXTRACT(MONTH FROM :today)
                    THEN w.ocrPagesUsedCurrentMonth + :pages
                    ELSE :pages
                END,
                w.ocrPagesUsedCurrentDay = CASE
                    WHEN w.ocrUsageLastResetDate = :today THEN w.ocrPagesUsedCurrentDay + :pages
                    ELSE :pages
                END,
                w.ocrUsageLastResetDate = :today
            WHERE w.id = :workspaceId
            """)
    int incrementOcrUsage(@Param("workspaceId") UUID workspaceId,
                           @Param("pages") int pages,
                           @Param("today") LocalDate today);
}
