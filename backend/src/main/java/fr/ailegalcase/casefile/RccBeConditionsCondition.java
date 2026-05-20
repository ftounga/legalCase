package fr.ailegalcase.casefile;

/**
 * SF-207-06 : conditions individuelles évaluées par le
 * {@link RccBeConditionsCalculator} et susceptibles d'être listées dans
 * {@link RccBeConditionsResult#conditionsManquantes()} pour orienter
 * l'avocat (renforcer le dossier, attendre l'âge requis, demander la
 * reconnaissance entreprise en difficulté, etc.).
 *
 * <p>La liste {@code conditionsManquantes} est rendue UNIQUEMENT lorsque
 * le verdict n'est pas {@code ELIGIBLE} ; elle détaille les manques du
 * régime LE PLUS PROCHE des seuils (au sens de la distance euclidienne
 * âge × carrière). En cas d'égalité de proximité, la priorité régime
 * (GENERAL > LONGUE_CARRIERE > METIERS_LOURDS > ENTREPRISE_DIFFICULTE)
 * tranche.</p>
 */
public enum RccBeConditionsCondition {

    /** Âge ≥ 60 ans (régimes {@code GENERAL} et {@code ENTREPRISE_DIFFICULTE}). */
    AGE_GENERAL_60,

    /** Carrière professionnelle salariée ≥ 40 ans. */
    CARRIERE_40,

    /** Métier lourd reconnu (régime {@code METIERS_LOURDS}). */
    METIER_LOURD_FLAG,

    /** Âge ≥ 58 ans (régime {@code METIERS_LOURDS}). */
    AGE_METIERS_LOURDS_58,

    /** Carrière professionnelle salariée ≥ 35 ans (régime {@code METIERS_LOURDS}). */
    CARRIERE_METIERS_LOURDS_35,

    /** Flag longue carrière (régime {@code LONGUE_CARRIERE}). */
    LONGUE_CARRIERE_FLAG,

    /**
     * Entreprise reconnue en difficulté par arrêté ministériel
     * (régime {@code ENTREPRISE_DIFFICULTE}, AR du 03/05/2007 art. 8).
     */
    ENTREPRISE_DIFFICULTE_FLAG
}
