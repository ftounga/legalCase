package fr.ailegalcase.casefile;

/**
 * SF-219-23 : nature de la réponse de l'employeur à la demande
 * d'aménagement raisonnable formulée par le travailleur ou
 * suggérée par le médecin du travail SEPP.
 *
 * <p>La nature de la réponse aiguille le verdict :</p>
 * <ul>
 *   <li>{@link #ACCORDE} — aménagement accordé intégralement —
 *       conformité art. 14 Loi 10/05/2007.</li>
 *   <li>{@link #CONTRE_PROPOSITION} — l'employeur propose une mesure
 *       alternative — analyse au cas par cas du caractère
 *       raisonnable de la contre-proposition.</li>
 *   <li>{@link #REFUSE_MOTIVE_DETAILLE} — refus assorti d'une
 *       motivation détaillée écrite invoquant la charge
 *       disproportionnée (coût, organisation, sécurité, taille
 *       entreprise) — analyse du bien-fondé des motifs.</li>
 *   <li>{@link #REFUSE_NON_MOTIVE} — refus sans motivation détaillée
 *       écrite — présomption de discrimination, renversement de la
 *       charge de la preuve devant le juge (art. 28 § 3
 *       Loi 10/05/2007 + jurisprudence CJUE Coleman).</li>
 *   <li>{@link #NON_REPONDU_RAISONNABLE} — silence de l'employeur
 *       au-delà d'un délai raisonnable (typiquement 1-2 mois selon
 *       complexité) — équivaut à un refus de fait.</li>
 *   <li>{@link #EN_ATTENTE} — délai raisonnable non encore écoulé,
 *       réponse pouvant encore intervenir.</li>
 *   <li>{@link #INDETERMINE} — pas encore d'instruction.</li>
 * </ul>
 */
public enum DiscriminationBeHandicapAmenagementReponseEmployeur {
    /** Aménagement accordé intégralement. */
    ACCORDE,

    /** Contre-proposition employeur — caractère raisonnable à apprécier. */
    CONTRE_PROPOSITION,

    /** Refus avec motivation détaillée écrite — analyse charge disproportionnée. */
    REFUSE_MOTIVE_DETAILLE,

    /** Refus sans motivation détaillée — présomption discrimination. */
    REFUSE_NON_MOTIVE,

    /** Silence employeur > délai raisonnable — équivaut à refus de fait. */
    NON_REPONDU_RAISONNABLE,

    /** Délai raisonnable encore en cours. */
    EN_ATTENTE,

    /** Statut indéterminé. */
    INDETERMINE
}
