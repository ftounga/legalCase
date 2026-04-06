package fr.ailegalcase.referential;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LegalReferentialRepository extends JpaRepository<LegalReferential, UUID> {

    /**
     * Retourne les entrées actives pour un domaine + type donnés.
     * Inclut les données système (workspace_id IS NULL) et les données custom
     * du workspace courant.
     */
    @Query("""
            SELECT r FROM LegalReferential r
            WHERE r.legalDomain = :domain
              AND r.referentialType = :type
              AND r.active = true
              AND (r.workspaceId IS NULL OR r.workspaceId = :workspaceId)
            ORDER BY r.system DESC, r.entryKey ASC
            """)
    List<LegalReferential> findActiveByDomainAndType(
            @Param("domain") String domain,
            @Param("type") String type,
            @Param("workspaceId") UUID workspaceId);

    /**
     * Retourne toutes les entrées actives pour un domaine.
     * Inclut données système + données custom du workspace.
     */
    @Query("""
            SELECT r FROM LegalReferential r
            WHERE r.legalDomain = :domain
              AND r.active = true
              AND (r.workspaceId IS NULL OR r.workspaceId = :workspaceId)
            ORDER BY r.referentialType ASC, r.system DESC, r.entryKey ASC
            """)
    List<LegalReferential> findActiveByDomain(
            @Param("domain") String domain,
            @Param("workspaceId") UUID workspaceId);

    /**
     * Retourne les données système uniquement (workspace_id IS NULL) pour un
     * domaine + type + clé donnés. Utilisé pour le fallback des services métier.
     */
    @Query("""
            SELECT r FROM LegalReferential r
            WHERE r.legalDomain = :domain
              AND r.referentialType = :type
              AND r.entryKey = :key
              AND r.workspaceId IS NULL
              AND r.active = true
            """)
    List<LegalReferential> findSystemEntry(
            @Param("domain") String domain,
            @Param("type") String type,
            @Param("key") String key);

    @Query("""
            SELECT r FROM LegalReferential r
            WHERE r.legalDomain = :domain
              AND r.referentialType = :type
              AND r.entryKey = :key
              AND r.country = :country
              AND r.workspaceId IS NULL
              AND r.active = true
            """)
    List<LegalReferential> findSystemEntryByCountry(
            @Param("domain") String domain,
            @Param("type") String type,
            @Param("key") String key,
            @Param("country") String country);

    @Query("""
            SELECT r FROM LegalReferential r
            WHERE r.legalDomain = :domain
              AND r.referentialType = :type
              AND r.country = :country
              AND r.workspaceId IS NULL
              AND r.active = true
            """)
    List<LegalReferential> findSystemEntriesByTypeAndCountry(
            @Param("domain") String domain,
            @Param("type") String type,
            @Param("country") String country);

    /**
     * Retourne l'override workspace pour une clé donnée (workspaceId non NULL).
     * Utilisé pour l'upsert lors d'une modification OWNER/ADMIN.
     */
    @Query("SELECT r FROM LegalReferential r WHERE r.workspaceId IS NULL AND r.active = true")
    List<LegalReferential> findAllSystemEntries();

    /**
     * Retourne l'override workspace pour une clé donnée (workspaceId non NULL).
     * Utilisé pour l'upsert lors d'une modification OWNER/ADMIN.
     */
    @Query("""
            SELECT r FROM LegalReferential r
            WHERE r.workspaceId = :workspaceId
              AND r.legalDomain = :domain
              AND r.referentialType = :type
              AND r.entryKey = :key
              AND (:country IS NULL AND r.country IS NULL OR r.country = :country)
              AND r.active = true
            """)
    List<LegalReferential> findWorkspaceEntry(
            @Param("workspaceId") UUID workspaceId,
            @Param("domain") String domain,
            @Param("type") String type,
            @Param("key") String key,
            @Param("country") String country);
}
