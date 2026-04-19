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
     * SF-122-01 : incrémente le compteur OCR mensuel et journalier atomiquement.
     * Si {@code today} est différent de {@code ocr_usage_last_reset_date}, le
     * compteur journalier est d'abord remis à 0 puis incrémenté. Le compteur
     * mensuel est incrémenté sans reset (le reset mensuel est géré ailleurs, SF-122-02).
     *
     * @return nombre de lignes affectées (1 si OK, 0 si workspace introuvable)
     */
    @Modifying
    @Query("""
            UPDATE Workspace w
            SET w.ocrPagesUsedCurrentMonth = w.ocrPagesUsedCurrentMonth + :pages,
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
