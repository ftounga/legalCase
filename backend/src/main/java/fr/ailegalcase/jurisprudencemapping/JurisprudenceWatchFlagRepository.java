package fr.ailegalcase.jurisprudencemapping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-01 — accès aux {@link JurisprudenceWatchFlag}.
 *
 * <p>Interface créée vide dans cette SF — les méthodes de lecture / arbitrage
 * seront ajoutées en SF-JU-01-02 (cron veille mensuelle) et SF-JU-01-05
 * (dashboard admin).</p>
 */
public interface JurisprudenceWatchFlagRepository extends JpaRepository<JurisprudenceWatchFlag, UUID> {
}
