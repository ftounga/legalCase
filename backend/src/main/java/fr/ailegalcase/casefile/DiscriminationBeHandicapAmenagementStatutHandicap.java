package fr.ailegalcase.casefile;

/**
 * SF-219-23 : statut de reconnaissance du handicap du travailleur,
 * élément de qualification préalable à l'analyse de l'obligation
 * d'aménagement raisonnable (Loi du 10/05/2007 art. 4 4° + CJUE
 * 11/04/2013 C-335/11 et C-337/11 HK Danmark / Ring et Werge —
 * notion fonctionnelle de handicap incluant la maladie de longue
 * durée entraînant une limitation durable et substantielle).
 *
 * <ul>
 *   <li>{@link #RECONNU_OFFICIEL} — reconnaissance officielle par
 *       AVIQ (Wallonie), VAPH (Flandre), PHARE (Bruxelles
 *       francophone), DGPH (handicap fédéral) ou avis CARSAT/INAMI
 *       équivalent — preuve la plus solide.</li>
 *   <li>{@link #MEDECIN_TRAVAIL_AVIS} — avis du médecin du travail
 *       SEPP (service externe de prévention) constatant une
 *       limitation durable et substantielle — admis par
 *       jurisprudence comme suffisant.</li>
 *   <li>{@link #INVALIDITE_MUTUELLE} — invalidité reconnue par la
 *       mutualité (au-delà de 12 mois d'incapacité) — preuve d'une
 *       limitation durable, admise par jurisprudence.</li>
 *   <li>{@link #DURABLE_FACTUEL} — situation factuelle durable
 *       (maladie longue, séquelles d'accident) sans reconnaissance
 *       officielle ni avis SEPP — la jurisprudence CJUE Ring/Werge
 *       admet ce critère fonctionnel mais la preuve est plus lourde
 *       pour le travailleur.</li>
 *   <li>{@link #CONTESTE} — l'employeur conteste la qualification
 *       handicap — l'analyse doit alors prioritairement se centrer
 *       sur la preuve de la limitation durable et substantielle.</li>
 *   <li>{@link #INDETERMINE} — pas encore d'instruction (préalable
 *       à clarifier).</li>
 * </ul>
 */
public enum DiscriminationBeHandicapAmenagementStatutHandicap {
    /** Reconnaissance officielle AVIQ / VAPH / PHARE / DGPH. */
    RECONNU_OFFICIEL,

    /** Avis SEPP médecin du travail constatant la limitation. */
    MEDECIN_TRAVAIL_AVIS,

    /** Invalidité reconnue par la mutualité (> 12 mois). */
    INVALIDITE_MUTUELLE,

    /** Situation factuelle durable sans reconnaissance officielle. */
    DURABLE_FACTUEL,

    /** Qualification handicap contestée par l'employeur. */
    CONTESTE,

    /** Statut non encore déterminé — préalable à clarifier. */
    INDETERMINE
}
