package fr.ailegalcase.casefile;

/**
 * SF-219-19 : statut de l'accord (CCT d'entreprise ou modification du
 * règlement de travail) qui formalise les modalités du droit à la
 * déconnexion BE (Loi du 03/10/2022 « Deal pour l'emploi », art. 16
 * — applicable aux employeurs occupant ≥ 20 travailleurs depuis le
 * 01/04/2023).
 *
 * <p>Le statut aiguille le verdict :</p>
 * <ul>
 *   <li>{@link #CCT_ENTREPRISE_DEPOSEE} — CCT d'entreprise négociée et
 *       déposée au greffe du SPF Emploi (instrument privilégié par la
 *       Loi 03/10/2022). On évalue les conditions de fond.</li>
 *   <li>{@link #REGLEMENT_TRAVAIL_MODIFIE} — modification du règlement
 *       de travail par procédure ordinaire (Loi 08/04/1965) — option
 *       subsidiaire lorsque la voie CCT n'a pas abouti.</li>
 *   <li>{@link #NEGOCIATION_EN_COURS} — concertation engagée
 *       (consultation CE / CPPT / DS) mais aucun instrument formalisé
 *       — l'employeur dépasse le délai mais peut justifier d'efforts
 *       de bonne foi.</li>
 *   <li>{@link #AUCUNE_INITIATIVE} — aucun accord ni amorce de
 *       concertation : manquement frontal à l'obligation légale.</li>
 *   <li>{@link #INDETERMINE} — pas encore d'instruction (analyse cas
 *       par cas).</li>
 * </ul>
 */
public enum DroitDeconnexionBeStatutAccord {
    /** CCT d'entreprise négociée et déposée au greffe du SPF Emploi. */
    CCT_ENTREPRISE_DEPOSEE,

    /** Modification du règlement de travail (option subsidiaire). */
    REGLEMENT_TRAVAIL_MODIFIE,

    /** Concertation en cours, aucun instrument formalisé. */
    NEGOCIATION_EN_COURS,

    /** Aucun accord ni amorce de concertation. */
    AUCUNE_INITIATIVE,

    /** Statut non encore déterminé — analyse cas par cas. */
    INDETERMINE
}
