package fr.ailegalcase.casefile;

/**
 * SF-219-25 : mode de saisine de l'auditorat du travail BE projeté
 * par l'avocat — détermine la procédure pratique recommandée.
 *
 * <ul>
 *   <li>{@link #PLAINTE_DIRECTE} — plainte directe à l'auditeur du
 *       travail (art. 63 CIC adapté + art. 138bis C. jud.) — voie
 *       privilégiée du plaignant qui se constitue partie civile,
 *       déclenche obligatoirement l'enquête (art. 28bis CIC).</li>
 *   <li>{@link #DENONCIATION_SIMPLE} — dénonciation sans
 *       constitution de partie civile (art. 29 al. 1<sup>er</sup>
 *       CIC). L'auditeur conserve l'opportunité des poursuites
 *       (art. 28quater § 1<sup>er</sup> CIC) ; le dénonciateur n'a
 *       pas qualité de partie au procès pénal.</li>
 *   <li>{@link #SIGNALEMENT_INSPECTION_SOCIALE} — signalement aux
 *       services d'inspection (Contrôle des lois sociales SPF
 *       Emploi, ONSS, INASTI, médecine du travail) qui transmettront
 *       leur PV à l'auditeur le cas échéant (art. 76 C. pén. soc.).
 *       Voie discrète et économe en moyens pour le plaignant.</li>
 *   <li>{@link #VOIE_CIVILE_PARALLELE} — saisine directe du tribunal
 *       du travail pour le contentieux civil (paiement, indemnité de
 *       rupture, dommages et intérêts) avec demande d'avis de
 *       l'auditeur si matière art. 580/581 C. jud. La voie pénale
 *       reste ouverte mais distincte.</li>
 *   <li>{@link #INDETERMINE} — mode non encore choisi (analyse
 *       préliminaire avocat).</li>
 * </ul>
 */
public enum AuditoratTravailBeModeSaisine {
    /** Plainte directe avec constitution partie civile — art. 63 CIC + art. 138bis C. jud. */
    PLAINTE_DIRECTE,

    /** Dénonciation simple sans constitution partie civile — art. 29 CIC. */
    DENONCIATION_SIMPLE,

    /** Signalement aux services d'inspection sociale — PV transmis art. 76 C. pén. soc. */
    SIGNALEMENT_INSPECTION_SOCIALE,

    /** Voie civile tribunal du travail (art. 578 C. jud.) en parallèle. */
    VOIE_CIVILE_PARALLELE,

    /** Mode non encore choisi — analyse préliminaire avocat. */
    INDETERMINE
}
