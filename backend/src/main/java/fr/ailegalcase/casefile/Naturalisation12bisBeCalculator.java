package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-215-07 — calculateur d'éligibilité à la déclaration de nationalité belge au
 * sens de l'art. 12bis CNB (loi du 28/06/1984 consolidée).
 *
 * <p>Sources :
 * <ul>
 *   <li>Code de la nationalité belge (loi 28/06/1984 consolidée) art. 12bis —
 *       déclaration de nationalité, voies 5 ans et 10 ans.</li>
 *   <li>AR 14/01/2013 — modes de preuve du niveau de langue A2 (DELF, NT2,
 *       diplôme secondaire belge 6e, etc.).</li>
 *   <li>AR 27/04/2018 — modes de preuve de l'intégration sociale (cours
 *       d'intégration régionalisés : Parcours d'intégration Wallonie,
 *       Inburgering Flandre, Bruxelles-Inburgering, ou équivalent).</li>
 * </ul>
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>F-IM-13-naturalisation (FR — Cciv art. 21-15+, 6 voies françaises) ;</li>
 *   <li>SF-215-09 (naturalisation conjoint Belge art. 16 CNB) ;</li>
 *   <li>F-221 (naturalisation par décret art. 19-21 CNB — très rare, P3).</li>
 * </ul>
 *
 * <p>Deux voies cumulatives (le calculateur priorise la voie 5 ans si les deux
 * sont satisfaites — la voie 5 ans étant strictement plus exigeante, satisfaire
 * la voie 10 ans seule n'est possible que si l'intégration / emploi n'est pas
 * démontré) :
 *
 * <ol>
 *   <li>Voie 5 ans : dureeSejour ≥ 60 mois + séjour illimité + niveau langue ≥ A2
 *       + (preuve intégration OU emploi) + !menaceOrdrePublic + !condamnationPenale.</li>
 *   <li>Voie 10 ans : dureeSejour ≥ 120 mois + séjour illimité + niveau langue ≥ A2
 *       + !menaceOrdrePublic + !condamnationPenale (sans condition emploi/intégration).</li>
 * </ol>
 *
 * <p>Verdict ternaire {@link Naturalisation12bisBeVoieEligible} :
 * VOIE_5_ANS / VOIE_10_ANS / AUCUNE.
 *
 * <p>{@code dureeManquante} : nombre de mois à attendre pour qu'une voie devienne
 * accessible, calculé uniquement si toutes les conditions <b>non temporelles</b>
 * sont satisfaites. 0 dans tous les autres cas.
 */
public final class Naturalisation12bisBeCalculator {

    /** Durée minimale (mois) pour la voie 5 ans (art. 12bis §1 1°). */
    public static final int DUREE_VOIE_5_ANS_MOIS = 60;

    /** Durée minimale (mois) pour la voie 10 ans (art. 12bis §1 5°). */
    public static final int DUREE_VOIE_10_ANS_MOIS = 120;

    private static final String PROCEDURE_REFERENCE =
            "Chambre des représentants — Commission des naturalisations — déclaration art. 12bis CNB";

    private static final String BASE_JURIDIQUE =
            "Code de la nationalité belge (loi 28/06/1984) art. 12bis ; "
                    + "AR 14/01/2013 (preuve langue) ; AR 27/04/2018 (preuve intégration)";

    private Naturalisation12bisBeCalculator() {
    }

    /**
     * Calcule l'éligibilité à la déclaration de nationalité belge 12bis.
     *
     * @throws IllegalArgumentException si l'un des paramètres est null ou hors borne.
     */
    public static Naturalisation12bisBeResult compute(Integer dureeSejour,
                                                     NaturalisationBeTypeSejourEnum typeSejour,
                                                     NaturalisationBeNiveauLangueEnum niveauLangue,
                                                     Boolean preuveIntegration,
                                                     Boolean preuveEmploi,
                                                     Boolean menaceOrdrePublic,
                                                     Boolean condamnationPenale) {
        validate(dureeSejour, typeSejour, niveauLangue, preuveIntegration, preuveEmploi,
                menaceOrdrePublic, condamnationPenale);

        int duree = dureeSejour;
        boolean conditionSejourIllimite = typeSejour == NaturalisationBeTypeSejourEnum.ILLIMITE;
        boolean conditionLangueA2 = niveauLangue == NaturalisationBeNiveauLangueEnum.A2
                || niveauLangue == NaturalisationBeNiveauLangueEnum.SUPERIEUR_A2;
        boolean conditionPasMenace = !menaceOrdrePublic;
        boolean conditionPasCondamnation = !condamnationPenale;

        boolean voie5Ans = duree >= DUREE_VOIE_5_ANS_MOIS
                && conditionSejourIllimite
                && conditionLangueA2
                && (preuveIntegration || preuveEmploi)
                && conditionPasMenace
                && conditionPasCondamnation;

        boolean voie10Ans = duree >= DUREE_VOIE_10_ANS_MOIS
                && conditionSejourIllimite
                && conditionLangueA2
                && conditionPasMenace
                && conditionPasCondamnation;

        Naturalisation12bisBeVoieEligible voieEligible;
        if (voie5Ans) {
            voieEligible = Naturalisation12bisBeVoieEligible.VOIE_5_ANS;
        } else if (voie10Ans) {
            voieEligible = Naturalisation12bisBeVoieEligible.VOIE_10_ANS;
        } else {
            voieEligible = Naturalisation12bisBeVoieEligible.AUCUNE;
        }

        int dureeManquante = 0;
        if (voieEligible == Naturalisation12bisBeVoieEligible.AUCUNE
                && conditionSejourIllimite
                && conditionLangueA2
                && conditionPasMenace
                && conditionPasCondamnation) {
            // Toutes les conditions non-temporelles sont remplies : seule la durée bloque.
            if (preuveIntegration || preuveEmploi) {
                // Voie 5 ans accessible si on attend.
                dureeManquante = Math.max(0, DUREE_VOIE_5_ANS_MOIS - duree);
            } else {
                // Pas d'intégration/emploi : seule la voie 10 ans reste atteignable.
                dureeManquante = Math.max(0, DUREE_VOIE_10_ANS_MOIS - duree);
            }
        }

        List<String> criteresNonRemplis = new ArrayList<>();
        if (!conditionSejourIllimite) {
            criteresNonRemplis.add(
                    "Séjour limité (carte A) — séjour illimité (carte B/C/K) requis");
        }
        if (!conditionLangueA2) {
            criteresNonRemplis.add(
                    "Niveau de langue < A2 — A2 minimum requis (DELF / NT2 / 6e secondaire)");
        }
        if (!conditionPasMenace) {
            criteresNonRemplis.add(
                    "Menace pour l'ordre public — naturalisation refusée");
        }
        if (!conditionPasCondamnation) {
            criteresNonRemplis.add(
                    "Condamnation pénale ≥ 3 mois ferme dans les 5 ans");
        }
        if (voieEligible == Naturalisation12bisBeVoieEligible.AUCUNE
                && duree < DUREE_VOIE_5_ANS_MOIS) {
            criteresNonRemplis.add("Durée de séjour insuffisante (< 5 ans)");
        }
        if (voieEligible == Naturalisation12bisBeVoieEligible.AUCUNE
                && duree >= DUREE_VOIE_5_ANS_MOIS
                && duree < DUREE_VOIE_10_ANS_MOIS
                && !preuveIntegration
                && !preuveEmploi) {
            criteresNonRemplis.add(
                    "Voie 5 ans : preuve d'intégration OU d'emploi requise "
                            + "(468h activité dans 5 ans, ou cours d'intégration régional validé)");
        }

        return new Naturalisation12bisBeResult(
                duree,
                typeSejour,
                niveauLangue,
                preuveIntegration,
                preuveEmploi,
                menaceOrdrePublic,
                condamnationPenale,
                voie5Ans,
                voie10Ans,
                voieEligible,
                dureeManquante,
                Collections.unmodifiableList(criteresNonRemplis),
                PROCEDURE_REFERENCE,
                BASE_JURIDIQUE
        );
    }

    private static void validate(Integer dureeSejour,
                                 NaturalisationBeTypeSejourEnum typeSejour,
                                 NaturalisationBeNiveauLangueEnum niveauLangue,
                                 Boolean preuveIntegration,
                                 Boolean preuveEmploi,
                                 Boolean menaceOrdrePublic,
                                 Boolean condamnationPenale) {
        if (dureeSejour == null) {
            throw new IllegalArgumentException("dureeSejour est requis");
        }
        if (dureeSejour < 0 || dureeSejour > 600) {
            throw new IllegalArgumentException("dureeSejour doit être entre 0 et 600 mois");
        }
        if (typeSejour == null) {
            throw new IllegalArgumentException("typeSejour est requis");
        }
        if (niveauLangue == null) {
            throw new IllegalArgumentException("niveauLangue est requis");
        }
        if (preuveIntegration == null) {
            throw new IllegalArgumentException("preuveIntegration est requis");
        }
        if (preuveEmploi == null) {
            throw new IllegalArgumentException("preuveEmploi est requis");
        }
        if (menaceOrdrePublic == null) {
            throw new IllegalArgumentException("menaceOrdrePublic est requis");
        }
        if (condamnationPenale == null) {
            throw new IllegalArgumentException("condamnationPenale est requis");
        }
    }
}
