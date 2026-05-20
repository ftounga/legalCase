package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-207-06 : résultat brut produit par {@link RccBeConditionsCalculator}.
 *
 * <p>Snapshot canonique (inputs normalisés + outputs calculés) sérialisé en
 * JSON dans la colonne {@code rcc_be_conditions_analyses.result_data}.</p>
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code verdict} — {@link Verdict#ELIGIBLE} (au moins un régime
 *       matche), {@link Verdict#INCERTAIN} (proche des seuils — âge ≥ 55 et
 *       carrière ≥ 30) ou {@link Verdict#NON_ELIGIBLE}.</li>
 *   <li>{@code regimeApplicable} — régime retenu selon la priorité
 *       (GENERAL > LONGUE_CARRIERE > METIERS_LOURDS > ENTREPRISE_DIFFICULTE) ;
 *       {@code null} si aucun régime ne matche.</li>
 *   <li>{@code regimesEligibles} — liste de tous les régimes qui matchent
 *       (ordre = ordre d'évaluation).</li>
 *   <li>{@code ageALaDateLicenciement} — âge en années pleines à la date
 *       envisagée (fuseau Europe/Brussels).</li>
 *   <li>{@code anneesCarriereCalculees} — carrière saisie par l'avocat
 *       (entier ≥ 0).</li>
 *   <li>{@code conditionsManquantes} — liste de
 *       {@link RccBeConditionsCondition} non remplies dans le régime LE
 *       PLUS PROCHE des seuils (vide si verdict ELIGIBLE).</li>
 *   <li>Champs <i>input</i> normalisés persistés pour pré-fill GET.</li>
 * </ul>
 */
public record RccBeConditionsResult(
        // Inputs normalisés (persistés pour survie au reload)
        LocalDate dateNaissance,
        int anneesCarriereProfessionnelle,
        boolean metierLourd,
        boolean longueCarriere,
        boolean entrepriseEnDifficulte,
        LocalDate dateLicenciementEnvisagee,
        // Outputs calculés
        Verdict verdict,
        RccBeConditionsRegime regimeApplicable,
        List<RccBeConditionsRegime> regimesEligibles,
        int ageALaDateLicenciement,
        int anneesCarriereCalculees,
        List<RccBeConditionsCondition> conditionsManquantes,
        String baseJuridique,
        String formuleCalcul
) {

    /**
     * Verdict d'éligibilité au RCC.
     *
     * <ul>
     *   <li>{@link #ELIGIBLE} — au moins un régime matche ;</li>
     *   <li>{@link #INCERTAIN} — aucun régime ne matche complètement, mais
     *       le candidat est proche des seuils (âge ≥ 55 et carrière ≥ 30)
     *       → l'avocat doit affiner les données ou attendre ;</li>
     *   <li>{@link #NON_ELIGIBLE} — pas suffisamment proche des seuils
     *       pour envisager le RCC à court terme.</li>
     * </ul>
     */
    public enum Verdict {
        ELIGIBLE,
        INCERTAIN,
        NON_ELIGIBLE
    }
}
