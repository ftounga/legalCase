package fr.ailegalcase.casefile;

/**
 * SF-219-01 : validateur d'éligibilité au RCC métiers lourds BE — fonction
 * <b>pure</b>, indépendante du contexte HTTP / persistance / horloge.
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>CCT n° 17</b> du 19/12/1974 (Conseil National du Travail) —
 *       régime d'indemnité complémentaire aux travailleurs âgés en cas de
 *       licenciement.</li>
 *   <li><b>AR du 3 mai 2007</b> fixant le RCC, <b>art. 3</b> — régime
 *       spécifique métiers lourds 58+/35 ans de carrière.</li>
 *   <li>Liste des métiers lourds — annexes AR + arrêtés ministériels (à
 *       vérifier annuellement par l'avocat).</li>
 * </ul>
 *
 * <h2>Conditions cumulatives</h2>
 * <ol>
 *   <li>Licenciement effectif (pas de démission).</li>
 *   <li>Âge ≥ {@value #AGE_MIN_METIERS_LOURDS} ans à la fin du contrat.</li>
 *   <li>Carrière professionnelle totale ≥ {@value #CARRIERE_MIN_ANNEES} ans.</li>
 *   <li>Au moins {@value #METIER_LOURD_MIN_RECENT_10} ans en métier lourd
 *       dans les 10 dernières années <b>OU</b>
 *       {@value #METIER_LOURD_MIN_RECENT_15} ans dans les 15 dernières.</li>
 * </ol>
 *
 * <h2>Ordre d'évaluation des inéligibilités</h2>
 * <p>Pour garantir un message stable et exploitable côté UI :
 * démission → âge → carrière → durée métier lourd. La première condition
 * non remplie produit le verdict {@link RccBeMetiersLourdsVerdictEnum#INELIGIBLE}
 * avec sa {@link RccBeMetiersLourdsRaisonIneligibilite} associée.</p>
 */
public final class RccBeMetiersLourdsValidator {

    static final int AGE_MIN_METIERS_LOURDS = 58;
    static final int CARRIERE_MIN_ANNEES = 35;
    static final int METIER_LOURD_MIN_RECENT_10 = 5;
    static final int METIER_LOURD_MIN_RECENT_15 = 7;

    static final String BASE_JURIDIQUE =
            "CCT n°17 du 19/12/1974 ; AR du 03/05/2007 art. 3";

    static final String AVERT_LISTE_METIERS_LOURDS =
            "Liste des métiers lourds à vérifier annuellement — "
                    + "voir AR + arrêtés ministériels en vigueur.";

    static final String SYNTHESE_ELIGIBLE =
            "Le travailleur est éligible au RCC métiers lourds "
                    + "(CCT 17 + AR 03/05/2007 art. 3).";

    static final String SYNTHESE_INELIGIBLE_DEMISSION =
            "Inéligible — RCC métiers lourds réservé au licenciement "
                    + "(démission exclue).";

    static final String SYNTHESE_INELIGIBLE_AGE =
            "Inéligible — âge < 58 ans à la fin du contrat (AR 03/05/2007 art. 3).";

    static final String SYNTHESE_INELIGIBLE_CARRIERE =
            "Inéligible — carrière professionnelle totale < 35 ans "
                    + "(AR 03/05/2007 art. 3).";

    static final String SYNTHESE_INELIGIBLE_METIER_LOURD =
            "Inéligible — durée de métier lourd insuffisante "
                    + "(ni 5 ans sur 10, ni 7 ans sur 15).";

    private RccBeMetiersLourdsValidator() {
    }

    public static RccBeMetiersLourdsResult analyze(RccBeMetiersLourdsRequest request) {
        validateInputs(request);

        // 1. Licenciement effectif (RCC = régime de chômage, requiert
        //    un licenciement — démission strictement exclue)
        if (!request.licenciementEffectif()) {
            return ineligible(request,
                    RccBeMetiersLourdsRaisonIneligibilite.DEMISSION_NON_ELIGIBLE,
                    SYNTHESE_INELIGIBLE_DEMISSION);
        }

        // 2. Âge ≥ 58 ans à la fin du contrat
        if (request.ageFinContrat() < AGE_MIN_METIERS_LOURDS) {
            return ineligible(request,
                    RccBeMetiersLourdsRaisonIneligibilite.AGE_INSUFFISANT,
                    SYNTHESE_INELIGIBLE_AGE);
        }

        // 3. Carrière ≥ 35 ans
        if (request.anneesCarriereTotal() < CARRIERE_MIN_ANNEES) {
            return ineligible(request,
                    RccBeMetiersLourdsRaisonIneligibilite.CARRIERE_INSUFFISANTE,
                    SYNTHESE_INELIGIBLE_CARRIERE);
        }

        // 4. Métier lourd : 5/10 OU 7/15 (alternative — il suffit qu'une
        //    des deux conditions soit satisfaite)
        boolean cond5sur10 = request.anneesMetierLourdRecent10()
                >= METIER_LOURD_MIN_RECENT_10;
        boolean cond7sur15 = request.anneesMetierLourdRecent15()
                >= METIER_LOURD_MIN_RECENT_15;
        if (!cond5sur10 && !cond7sur15) {
            return ineligible(request,
                    RccBeMetiersLourdsRaisonIneligibilite.DUREE_METIER_LOURD_INSUFFISANTE,
                    SYNTHESE_INELIGIBLE_METIER_LOURD);
        }

        // ── Toutes conditions OK → ELIGIBLE ────────────────────────────────
        return new RccBeMetiersLourdsResult(
                request.ageFinContrat(),
                request.anneesCarriereTotal(),
                request.anneesMetierLourdRecent10(),
                request.anneesMetierLourdRecent15(),
                request.typeMetierLourd(),
                request.licenciementEffectif(),
                RccBeMetiersLourdsVerdictEnum.ELIGIBLE,
                null,
                SYNTHESE_ELIGIBLE,
                BASE_JURIDIQUE,
                AVERT_LISTE_METIERS_LOURDS);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static RccBeMetiersLourdsResult ineligible(
            RccBeMetiersLourdsRequest request,
            RccBeMetiersLourdsRaisonIneligibilite raison,
            String synthese) {
        return new RccBeMetiersLourdsResult(
                request.ageFinContrat(),
                request.anneesCarriereTotal(),
                request.anneesMetierLourdRecent10(),
                request.anneesMetierLourdRecent15(),
                request.typeMetierLourd(),
                request.licenciementEffectif(),
                RccBeMetiersLourdsVerdictEnum.INELIGIBLE,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_LISTE_METIERS_LOURDS);
    }

    private static void validateInputs(RccBeMetiersLourdsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.ageFinContrat() == null) {
            throw new IllegalArgumentException("ageFinContrat est requis");
        }
        if (request.ageFinContrat() < 18 || request.ageFinContrat() > 80) {
            throw new IllegalArgumentException(
                    "ageFinContrat invalide (doit être entre 18 et 80)");
        }
        if (request.anneesCarriereTotal() == null) {
            throw new IllegalArgumentException("anneesCarriereTotal est requis");
        }
        if (request.anneesCarriereTotal() < 0
                || request.anneesCarriereTotal() > 60) {
            throw new IllegalArgumentException(
                    "anneesCarriereTotal invalide (doit être entre 0 et 60)");
        }
        if (request.anneesMetierLourdRecent10() == null) {
            throw new IllegalArgumentException(
                    "anneesMetierLourdRecent10 est requis");
        }
        if (request.anneesMetierLourdRecent10() < 0
                || request.anneesMetierLourdRecent10() > 10) {
            throw new IllegalArgumentException(
                    "anneesMetierLourdRecent10 invalide (doit être entre 0 et 10)");
        }
        if (request.anneesMetierLourdRecent15() == null) {
            throw new IllegalArgumentException(
                    "anneesMetierLourdRecent15 est requis");
        }
        if (request.anneesMetierLourdRecent15() < 0
                || request.anneesMetierLourdRecent15() > 15) {
            throw new IllegalArgumentException(
                    "anneesMetierLourdRecent15 invalide (doit être entre 0 et 15)");
        }
        if (request.typeMetierLourd() == null) {
            throw new IllegalArgumentException("typeMetierLourd est requis");
        }
        if (request.licenciementEffectif() == null) {
            throw new IllegalArgumentException(
                    "licenciementEffectif est requis");
        }
        // Cohérence : la durée 10 ans ne peut pas dépasser la durée 15 ans
        if (request.anneesMetierLourdRecent10()
                > request.anneesMetierLourdRecent15()) {
            throw new IllegalArgumentException(
                    "anneesMetierLourdRecent10 ne peut pas être supérieur "
                            + "à anneesMetierLourdRecent15 (cohérence "
                            + "période courte incluse dans la longue)");
        }
    }
}
