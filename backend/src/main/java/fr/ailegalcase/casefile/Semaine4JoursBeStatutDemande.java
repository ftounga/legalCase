package fr.ailegalcase.casefile;

/**
 * SF-219-18 : statut de la demande de semaine de 4 jours BE (Loi
 * du 03/10/2022 « Deal pour l'emploi », art. 5 § 4 et § 5).
 *
 * <p>Le statut est utilisé pour aiguiller le verdict :</p>
 * <ul>
 *   <li>{@link #ACCORDE_AVENANT_SIGNE} — demande acceptée, avenant
 *       écrit signé : on évalue les conditions de fond.</li>
 *   <li>{@link #REFUSE_MOTIVE_PAR_ECRIT} — refus régulièrement
 *       motivé par écrit dans le délai d'1 mois — refus
 *       <i>opposable</i>, l'avocat valide la régularité de la
 *       procédure mais le régime n'est pas en place.</li>
 *   <li>{@link #REFUSE_SANS_MOTIVATION_ECRITE} — refus sans
 *       motivation écrite ou hors délai — refus juridiquement
 *       fragile, exposition à l'action en abus de droit.</li>
 *   <li>{@link #EN_ATTENTE_REPONSE_EMPLOYEUR} — demande déposée,
 *       l'employeur n'a pas (encore) répondu — délai d'1 mois en
 *       cours, à surveiller.</li>
 *   <li>{@link #LICENCIE_APRES_DEMANDE} — le travailleur a été
 *       licencié après le dépôt de la demande — bascule sur le
 *       régime de protection (art. 5 § 6) avec présomption de
 *       représailles.</li>
 *   <li>{@link #INDETERMINE} — pas encore d'instruction (analyse
 *       cas par cas).</li>
 * </ul>
 */
public enum Semaine4JoursBeStatutDemande {
    /** Accord employeur acquis, avenant au contrat signé. */
    ACCORDE_AVENANT_SIGNE,

    /** Refus régulièrement motivé par écrit dans le délai d'1 mois. */
    REFUSE_MOTIVE_PAR_ECRIT,

    /** Refus sans motivation écrite ou hors délai. */
    REFUSE_SANS_MOTIVATION_ECRITE,

    /** Demande déposée, en attente de réponse employeur (≤ 1 mois). */
    EN_ATTENTE_REPONSE_EMPLOYEUR,

    /** Licenciement intervenu après le dépôt de la demande. */
    LICENCIE_APRES_DEMANDE,

    /** Statut non encore déterminé — analyse cas par cas. */
    INDETERMINE
}
