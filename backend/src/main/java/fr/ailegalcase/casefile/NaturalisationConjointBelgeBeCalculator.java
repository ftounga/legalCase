package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-215-09 — calculateur d'éligibilité à la déclaration de nationalité belge par
 * mariage avec un(e) Belge au sens de l'art. 16 Code de la nationalité belge (loi
 * du 28/06/1984 consolidée, loi modificative 04/12/2012).
 *
 * <p>Sources :
 * <ul>
 *   <li>Code de la nationalité belge (loi 28/06/1984 consolidée) art. 16 §1 2° —
 *       déclaration de nationalité par mariage : cohabitation ininterrompue
 *       ≥ 5 ans.</li>
 *   <li>AR 14/01/2013 — modes de preuve du niveau de langue A2 (DELF, NT2,
 *       diplôme secondaire belge 6e, etc.).</li>
 *   <li>Loi 04/12/2012 modifiant le CNB — durcissement des conditions et
 *       harmonisation des exigences de langue / intégration.</li>
 * </ul>
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>F-IM-13-naturalisation (FR — Cciv art. 21-2 par mariage avec Français,
 *       art. 21-15 par décret) ;</li>
 *   <li>SF-215-07 (F-IM-28 naturalisation 12bis BE — voies résidence 5/10 ans,
 *       sans condition de mariage avec un Belge) ;</li>
 *   <li>F-221 (naturalisation par décret art. 19-21 CNB — très rare, P3).</li>
 * </ul>
 *
 * <p>Un outil = une situation (CLAUDE.md
 * {@code feedback_decision_tools_one_per_situation}). Voie unique de l'art. 16, donc
 * verdict binaire {@link NaturalisationConjointBelgeBeVerdict} ELIGIBLE / INELIGIBLE
 * (pas de verdict ternaire comme l'art. 12bis qui propose deux voies cumulables).
 *
 * <p>Conditions cumulatives (toutes requises pour ELIGIBLE) :
 * <ol>
 *   <li>dureeCohabitationMois ≥ 60 (5 ans — art. 16 §1 2°) <i>(à vérifier par avocat BE)</i> ;</li>
 *   <li>niveauLangue ≥ A2 (français, néerlandais ou allemand — AR 14/01/2013) ;</li>
 *   <li>preuveIntegration = true (cours d'intégration régional ou diplôme BE) ;</li>
 *   <li>!menaceOrdrePublic ;</li>
 *   <li>!condamnationPenale (≥ 3 mois ferme dans les 5 ans).</li>
 * </ol>
 *
 * <p>{@code dureeManquante} : nombre de mois restants à attendre pour atteindre les
 * 60 mois requis. {@code max(0, 60 - dureeCohabitationMois)}.
 *
 * <p>Le mariage avec un(e) Belge est implicite — c'est un pré-requis du dossier (l'outil
 * n'a de sens que dans ce contexte). La date du mariage n'est pas utilisée dans le
 * calcul, elle est conservée pour la traçabilité et l'affichage.
 */
public final class NaturalisationConjointBelgeBeCalculator {

    /** Durée minimale (mois) de cohabitation ininterrompue (art. 16 §1 2° CNB — à vérifier par avocat BE). */
    public static final int DUREE_COHABITATION_MINIMALE_MOIS = 60;

    private static final String DELAI_DECLARATION =
            "Déclaration devant officier d'état civil de la commune de résidence — "
                    + "délai d'instruction : 3-6 mois";

    private static final String BASE_JURIDIQUE =
            "Code de la nationalité belge art. 16 (déclaration par mariage) ; "
                    + "AR 14/01/2013 (preuve langue) ; loi modif. 04/12/2012";

    private NaturalisationConjointBelgeBeCalculator() {
    }

    /**
     * Calcule l'éligibilité à la déclaration de nationalité belge art. 16 par mariage.
     *
     * @throws IllegalArgumentException si l'un des paramètres est null, hors borne, ou
     *     si la date du mariage est future.
     */
    public static NaturalisationConjointBelgeBeResult compute(LocalDate dateMarriage,
                                                              Boolean cohabitationLegale,
                                                              Integer dureeCohabitationMois,
                                                              NaturalisationBeNiveauLangueEnum niveauLangue,
                                                              Boolean preuveIntegration,
                                                              Boolean menaceOrdrePublic,
                                                              Boolean condamnationPenale) {
        validate(dateMarriage, cohabitationLegale, dureeCohabitationMois, niveauLangue,
                preuveIntegration, menaceOrdrePublic, condamnationPenale);

        int duree = dureeCohabitationMois;
        boolean conditionCohabitation = duree >= DUREE_COHABITATION_MINIMALE_MOIS;
        boolean conditionLangue = niveauLangue == NaturalisationBeNiveauLangueEnum.A2
                || niveauLangue == NaturalisationBeNiveauLangueEnum.SUPERIEUR_A2;
        boolean conditionIntegration = preuveIntegration;
        boolean conditionPasMenace = !menaceOrdrePublic;
        boolean conditionPasCondamnation = !condamnationPenale;

        boolean eligible = conditionCohabitation
                && conditionLangue
                && conditionIntegration
                && conditionPasMenace
                && conditionPasCondamnation;

        NaturalisationConjointBelgeBeVerdict verdict = eligible
                ? NaturalisationConjointBelgeBeVerdict.ELIGIBLE
                : NaturalisationConjointBelgeBeVerdict.INELIGIBLE;

        int dureeManquante = Math.max(0, DUREE_COHABITATION_MINIMALE_MOIS - duree);

        List<String> criteresNonRemplis = new ArrayList<>();
        if (!conditionCohabitation) {
            criteresNonRemplis.add(
                    "Cohabitation < 5 ans (60 mois) requis — durée actuelle : "
                            + duree + " mois");
        }
        if (!conditionLangue) {
            criteresNonRemplis.add(
                    "Niveau de langue insuffisant (A2 minimum requis)");
        }
        if (!conditionIntegration) {
            criteresNonRemplis.add(
                    "Preuve d'intégration requise (cours intégration régional ou diplôme BE)");
        }
        if (menaceOrdrePublic) {
            criteresNonRemplis.add(
                    "Menace pour l'ordre public — naturalisation refusée");
        }
        if (condamnationPenale) {
            criteresNonRemplis.add(
                    "Condamnation pénale ≥ 3 mois ferme dans les 5 ans");
        }

        return new NaturalisationConjointBelgeBeResult(
                dateMarriage,
                cohabitationLegale,
                duree,
                niveauLangue,
                preuveIntegration,
                menaceOrdrePublic,
                condamnationPenale,
                eligible,
                verdict,
                dureeManquante,
                Collections.unmodifiableList(criteresNonRemplis),
                DELAI_DECLARATION,
                BASE_JURIDIQUE
        );
    }

    private static void validate(LocalDate dateMarriage,
                                 Boolean cohabitationLegale,
                                 Integer dureeCohabitationMois,
                                 NaturalisationBeNiveauLangueEnum niveauLangue,
                                 Boolean preuveIntegration,
                                 Boolean menaceOrdrePublic,
                                 Boolean condamnationPenale) {
        if (dateMarriage == null) {
            throw new IllegalArgumentException("dateMarriage est requis");
        }
        if (dateMarriage.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Mariage non encore célébré");
        }
        if (cohabitationLegale == null) {
            throw new IllegalArgumentException("cohabitationLegale est requis");
        }
        if (dureeCohabitationMois == null) {
            throw new IllegalArgumentException("dureeCohabitationMois est requis");
        }
        if (dureeCohabitationMois < 0 || dureeCohabitationMois > 600) {
            throw new IllegalArgumentException(
                    "dureeCohabitationMois doit être entre 0 et 600 mois");
        }
        if (niveauLangue == null) {
            throw new IllegalArgumentException("niveauLangue est requis");
        }
        if (preuveIntegration == null) {
            throw new IllegalArgumentException("preuveIntegration est requis");
        }
        if (menaceOrdrePublic == null) {
            throw new IllegalArgumentException("menaceOrdrePublic est requis");
        }
        if (condamnationPenale == null) {
            throw new IllegalArgumentException("condamnationPenale est requis");
        }
    }
}
