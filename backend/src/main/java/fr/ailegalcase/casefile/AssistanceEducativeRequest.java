package fr.ailegalcase.casefile;

/**
 * SF-222-04 : body POST /api/v1/case-files/{id}/assistance-educative-analysis.
 *
 * <p>Outil décisionnel assistance éducative — mineur en danger (art. 375 et s.
 * Cciv), Famille FRANCE uniquement. UN SEUL outil oriente vers les 4 issues
 * d'UNE situation (mineur en danger) : AED (administrative, ASE), AEMO
 * (judiciaire, milieu ouvert), OPP / placement (judiciaire, retrait) ou pas de
 * mesure.</p>
 *
 * <p>Champs (art. 375 et s. Cciv) :</p>
 * <ul>
 *   <li>{@code dangerCaracterise} — santé, sécurité ou moralité du mineur en
 *       danger, ou conditions d'éducation / développement gravement compromis
 *       (art. 375 Cciv) ;</li>
 *   <li>{@code urgence} — danger immédiat justifiant une mesure provisoire de
 *       placement (art. 375-5 Cciv) ;</li>
 *   <li>{@code adhesionFamille} — adhésion des titulaires de l'autorité
 *       parentale à une mesure ;</li>
 *   <li>{@code maintienMilieuFamilialPossible} — le mineur peut être maintenu
 *       dans son milieu familial actuel (art. 375-2 Cciv) ;</li>
 *   <li>{@code mesureAmiableASEEnvisageable} — une mesure administrative amiable
 *       (AED, ASE) est envisageable (art. L. 222-3 CASF).</li>
 * </ul>
 *
 * <p>Anti-doublon (invariant « 1 situation = 1 outil ») : cet outil NE découpe
 * PAS la situation en 3 outils distincts (AED / AEMO / OPP) — il évalue le
 * danger et oriente vers la mesure adaptée.</p>
 */
public record AssistanceEducativeRequest(
        Boolean dangerCaracterise,
        Boolean urgence,
        Boolean adhesionFamille,
        Boolean maintienMilieuFamilialPossible,
        Boolean mesureAmiableASEEnvisageable
) {}
