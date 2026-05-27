package fr.ailegalcase.casefile;

/**
 * SF-215-05 : constantes partagées entre les calculateurs de regroupement familial BE
 * (art. 10ter — séjour illimité — SF-215-03 ET art. 10bis — séjour limité — SF-215-05).
 *
 * <p>Refactor DRY introduit lors de la livraison SF-215-05 pour éviter la duplication de
 * la valeur du seuil de ressources entre {@link Regroupement10terBeCalculator} et
 * {@link Regroupement10bisBeCalculator}. La valeur reste paramétrable côté service via
 * la propriété {@code regroupement.seuil-ressources-be} (override application.properties).
 *
 * <p>Source : Loi du 15/12/1980 art. 10 § 5 + AR du 17/05/2007.
 * Référence : 120 % du revenu d'intégration sociale (RIS) × 1,5 ≈ 1 495 € — valeur
 * arrondie à 1 500 €. <i>(à vérifier par avocat BE 2026 — révision annuelle indexation
 * pivot)</i>.
 */
public final class RegroupementBeConstants {

    /** Seuil de ressources mensuelles nettes (€) — AR 17/05/2007 ~ 120 % RIS × 1,5. */
    public static final int SEUIL_RESSOURCES_MENSUEL = 1_500;

    /** Durée minimale de séjour ininterrompu du regroupant en mois (art. 10ter / 10bis). */
    public static final int DUREE_SEJOUR_MINIMALE_MOIS = 12;

    private RegroupementBeConstants() {}
}
