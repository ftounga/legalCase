package fr.ailegalcase.casefile;

/**
 * SF-218-17 : barème d'affiliation à l'assurance chômage de l'intermittent du
 * spectacle — règlement Unedic, annexes 8 (techniciens) et 10 (artistes). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Condition d'affiliation : 507 heures de travail (ou cachets convertis,
 * heures d'enseignement assimilées dans la limite d'un plafond) sur la période
 * de référence de 12 mois précédant la fin du dernier contrat. Conversion des
 * cachets (annexe 10) : 1 cachet = 12 heures.
 *
 * <p><b>CONSTANTES À ACTUALISER</b> : le seuil (507 h), la période de référence
 * (12 mois), la conversion du cachet (12 h), le plafond d'heures d'enseignement
 * assimilées et la marge de vérification sont fixés par la convention
 * d'assurance chômage — seuil et plafonds Unedic à actualiser à chaque
 * renégociation de la convention d'assurance chômage.
 */
public final class BaremeIntermittentSpectacleAre {

    /**
     * Seuil d'affiliation : 507 heures de travail sur la période de référence de
     * 12 mois (annexes 8 et 10 Unedic). À ACTUALISER à chaque renégociation de la
     * convention d'assurance chômage.
     */
    public static final int SEUIL_HEURES = 507;

    /**
     * Période de référence d'affiliation, en mois (12 mois glissants précédant la
     * fin du dernier contrat). À ACTUALISER.
     */
    public static final int PERIODE_REFERENCE_MOIS = 12;

    /**
     * Conversion d'un cachet artistique (annexe 10) en heures : 1 cachet = 12
     * heures (cachets isolés ou groupés). À ACTUALISER.
     */
    public static final int HEURES_PAR_CACHET = 12;

    /**
     * Plafond d'heures d'enseignement / de formation assimilables à des heures de
     * travail dans le décompte d'affiliation (90 heures par défaut ; 70 heures
     * pour les moins de 50 ans selon la convention). Valeur prudente retenue : 90.
     * À ACTUALISER.
     */
    public static final int PLAFOND_FORMATION = 90;

    /**
     * Marge de vérification autour du seuil, en heures (± 10 h) : dans cette
     * fourchette, le décompte exact est à confirmer auprès de France Travail
     * (statut A_VERIFIER). À ACTUALISER.
     */
    public static final int MARGE_VERIFICATION_HEURES = 10;

    private BaremeIntermittentSpectacleAre() {
    }
}
