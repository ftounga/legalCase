package fr.ailegalcase.casefile;

/**
 * SF-219-23 : verdict de l'outil <i>discrimination handicap — refus
 * d'aménagements raisonnables BE</i> (Loi du 10/05/2007 tendant à
 * lutter contre certaines formes de discrimination M.B. 30/05/2007 +
 * CCT n° 95 du 10/10/2008 du CNT NAR + Directive 2000/78/CE art. 5 +
 * Convention ONU 13/12/2006 art. 5 et 27).
 *
 * <h2>Cadre juridique condensé</h2>
 * <p>Le refus de mettre en place un aménagement raisonnable au
 * bénéfice d'une personne en situation de handicap constitue une
 * <b>discrimination indirecte fondée sur le handicap</b> (Loi
 * 10/05/2007 art. 14 + art. 4 9°). L'employeur ne peut justifier ce
 * refus qu'en démontrant la <b>charge disproportionnée</b> au regard
 * du coût (compte tenu des subsides AVIQ / VDAB / PHARE / Actiris
 * mobilisables), de la taille de l'entreprise, de l'organisation, de
 * la sécurité et de la faisabilité technique. La sanction principale
 * est l'indemnité forfaitaire de 6 mois de rémunération brute (art. 28
 * § 2 1° Loi 10/05/2007) ou l'indemnisation du préjudice réellement
 * subi (art. 28 § 2 2°). En cas de licenciement consécutif à la
 * demande d'aménagement, l'art. 17 instaure une présomption de
 * représailles renversant la charge de la preuve.</p>
 *
 * <h2>Verdicts hiérarchisés</h2>
 * <ul>
 *   <li>{@link #DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE} — refus
 *       motivé mais la charge disproportionnée n'est pas démontrée
 *       (absence de demande de subside, motifs vagues, alternatives
 *       non explorées).</li>
 *   <li>{@link #DISCRIMINATION_PRESUMEE_NON_MOTIVATION} — refus sans
 *       motivation détaillée écrite — renversement de charge,
 *       l'employeur devra apporter la preuve devant le juge.</li>
 *   <li>{@link #REPRESAILLES_PRESUMEES_LICENCIEMENT} — licenciement
 *       ou régression substantielle après demande d'aménagement —
 *       indemnité forfaitaire 6 mois cumulable avec les indemnités
 *       de rupture de droit commun.</li>
 *   <li>{@link #CONFORME_AMENAGEMENT_ACCORDE} — aménagement accordé
 *       intégralement ou contre-proposition acceptable par les deux
 *       parties.</li>
 *   <li>{@link #CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE} — refus
 *       fondé sur une charge disproportionnée objectivement
 *       démontrée (devis externes, refus de subside motivé, taille
 *       de l'entreprise notoirement insuffisante).</li>
 *   <li>{@link #FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE} — la
 *       qualification handicap est contestable (statut indéterminé,
 *       absence d'avis médecin du travail, simple maladie de courte
 *       durée) — préalable à trancher avant toute analyse
 *       discrimination.</li>
 *   <li>{@link #A_ANALYSER} — configuration ambigüe (procédure en
 *       cours, attente avis SEPP, articulation avec procédure
 *       harcèlement / reclassement médical) → analyse cas par cas.</li>
 * </ul>
 */
public enum DiscriminationBeHandicapAmenagementVerdict {
    /** Refus motivé mais charge disproportionnée non démontrée. */
    DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE,

    /** Refus sans motivation détaillée — présomption discrimination. */
    DISCRIMINATION_PRESUMEE_NON_MOTIVATION,

    /** Licenciement ou régression après demande — représailles présumées. */
    REPRESAILLES_PRESUMEES_LICENCIEMENT,

    /** Aménagement accordé / contre-proposition acceptable. */
    CONFORME_AMENAGEMENT_ACCORDE,

    /** Refus fondé sur charge disproportionnée objectivement démontrée. */
    CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE,

    /** Qualification handicap incertaine — préalable à trancher. */
    FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE,

    /** Configuration ambigüe — analyse cas par cas. */
    A_ANALYSER
}
