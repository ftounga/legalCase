package fr.ailegalcase.casefile;

/**
 * SF-219-25 : verdict de l'outil <i>Auditorat du travail BE — saisine
 * du parquet spécialisé en droit social pénal</i> (Code judiciaire
 * art. 138bis + Code d'instruction criminelle art. 24 + Loi du
 * 03/08/1992 sur le Code judiciaire + Loi du 06/06/2010 introduisant
 * le Code pénal social).
 *
 * <h2>Routes de saisine de l'auditorat du travail</h2>
 * <p>L'auditorat du travail est le parquet spécialisé compétent pour
 * la poursuite des infractions au droit social pénal et l'avis dans
 * les contentieux civils du tribunal du travail (art. 138bis C. jud.).
 * Sa saisine peut prendre quatre formes principales :</p>
 *
 * <ul>
 *   <li>{@link #SAISINE_AUDITORAT_RECOMMANDEE} — voie directe par
 *       plainte ou dénonciation à l'auditeur du travail
 *       (art. 24 CIC + art. 138bis C. jud.). Recommandée pour les
 *       faits constitutifs d'infraction au Code pénal social
 *       caractérisés et documentés (travail non déclaré, faux
 *       social, accident grave non déclaré, discrimination pénale,
 *       harcèlement pénal art. 442bis C. pén., entrave inspection
 *       art. 209-212 C. pén. soc.). L'auditeur arbitrera entre
 *       voie pénale et voie administrative (« règle Una Via »
 *       art. 73 C. pén. soc., Cour const. arrêt 61/2014).</li>
 *   <li>{@link #DENONCIATION_INSPECTION_PREALABLE} — orientation vers
 *       l'inspection sociale (Contrôle des lois sociales SPF Emploi
 *       ou Inspection ONSS / INASTI) en amont de l'auditorat. Les
 *       services d'inspection disposent d'un pouvoir de constatation
 *       (PV faisant foi jusqu'à preuve contraire — art. 100 et 100bis
 *       C. pén. soc.) et leur PV est transmis à l'auditeur (art. 76
 *       C. pén. soc.). Voie recommandée lorsque les faits doivent
 *       être objectivés sur le terrain avant qualification pénale.</li>
 *   <li>{@link #SAISINE_NON_PERTINENTE} — les faits ne relèvent pas
 *       de la compétence de l'auditorat du travail (pur litige
 *       individuel de droit civil du travail — exécution du
 *       contrat, indemnités de rupture, arriérés salariaux —
 *       compétence du tribunal du travail art. 578 C. jud., pas du
 *       parquet ; ou faits prescrits art. 81 C. pén. soc. /
 *       art. 21 Titre prél. CIC ; ou compétence territoriale d'un
 *       autre auditorat).</li>
 *   <li>{@link #A_QUALIFIER} — qualification ouverte requérant un
 *       examen avocat préalable (nature du fait incertaine, dossier
 *       documentaire insuffisant, articulation pénal/civil/
 *       administratif à arbitrer).</li>
 * </ul>
 *
 * <h2>Articulation avec les voies civiles</h2>
 * <p>La saisine de l'auditorat ne fait pas obstacle aux voies
 * civiles parallèles devant le tribunal du travail (paiement
 * d'arriérés Loi 12/04/1965, indemnités de rupture Loi 03/07/1978,
 * dommages et intérêts art. 1382 anc. C. civ. / 5.83 nouveau
 * C. civ.). L'auditeur du travail rend obligatoirement un avis dans
 * les matières art. 580 et 581 C. jud. (art. 138bis § 1<sup>er</sup>
 * C. jud.) et peut intervenir d'office au civil.</p>
 *
 * <h2>« Règle Una Via » (art. 73 C. pén. soc.)</h2>
 * <p>Une fois saisi, l'auditeur choisit entre voie pénale
 * (citation tribunal correctionnel — art. 76 C. jud.) et voie
 * administrative (transmission au SPF Emploi pour amende
 * administrative — art. 84 C. pén. soc.). Ce choix est exclusif :
 * le non bis in idem strictement opposable interdit le cumul
 * (Cour const. arrêt 61/2014 du 03/04/2014, CEDH Grande Stevens
 * c. Italie 04/03/2014). L'outil signale ce choix sans le
 * trancher — l'auditeur reste seul maître de l'opportunité des
 * poursuites art. 28quater § 1<sup>er</sup> CIC.</p>
 */
public enum AuditoratTravailBeVerdict {
    /** Saisine directe de l'auditorat recommandée — plainte ou dénonciation art. 24 CIC + art. 138bis C. jud. */
    SAISINE_AUDITORAT_RECOMMANDEE,

    /** Voie inspection sociale préalable — PV constatation art. 100 C. pén. soc. transmis art. 76 à l'auditeur. */
    DENONCIATION_INSPECTION_PREALABLE,

    /** Saisine non pertinente — compétence tribunal du travail (art. 578 C. jud.) ou faits prescrits. */
    SAISINE_NON_PERTINENTE,

    /** Qualification ouverte — examen avocat préalable requis (dossier insuffisant ou pluri-qualifications). */
    A_QUALIFIER
}
