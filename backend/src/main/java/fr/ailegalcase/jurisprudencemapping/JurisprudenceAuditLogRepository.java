package fr.ailegalcase.jurisprudencemapping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-01 — accès aux {@link JurisprudenceAuditLog}.
 *
 * <p>Interface créée vide dans cette SF — les méthodes d'écriture (cron) et
 * de lecture (dashboard audit) seront ajoutées en SF-JU-01-02 et SF-JU-01-05.</p>
 */
public interface JurisprudenceAuditLogRepository extends JpaRepository<JurisprudenceAuditLog, UUID> {
}
