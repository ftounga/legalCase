package fr.ailegalcase.casefile;

/**
 * SF-216-09 : body POST /api/v1/case-files/{id}/delegation-ap-fr.
 *
 * <p>Outil single-country FRANCE — art. 376-1 Cciv (délégation volontaire et
 * judiciaire d'autorité parentale) + art. L. 228-1 CASF (associations
 * habilitées).</p>
 *
 * <p>Le service rejette en 400 : pays ≠ FRANCE, age &lt; 0 ou age &gt; 18,
 * type de délégation manquant.</p>
 */
public record DelegationApFrRequest(
        TypeDelegationApEnum typeDelegation,
        TiersLienFamilialEnum tiersLienFamilial,
        Boolean accordParents,
        String motivationDelegation,
        Integer ageEnfant,
        Boolean dangerCaracterise,
        Boolean decisionsJudiciairesPrecedentes,
        Boolean desinteretParentPlusUnAn
) {}
