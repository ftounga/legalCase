package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DecisionToolVisibilityRuleRepository extends JpaRepository<DecisionToolVisibilityRule, UUID> {

    /**
     * Charge toutes les règles d'un domaine applicables à un pays donné :
     * règles du pays + règles transversales (country IS NULL).
     */
    @Query("""
            SELECT r FROM DecisionToolVisibilityRule r
            WHERE r.legalDomain = :legalDomain
              AND (r.country = :country OR r.country IS NULL)
            """)
    List<DecisionToolVisibilityRule> findForDomainAndCountry(
            @Param("legalDomain") String legalDomain,
            @Param("country") String country);
}
