package fr.ailegalcase.casefile;

/**
 * SF-219-25 : qualification de la nature des faits soumis à l'auditorat
 * du travail BE — détermine la pertinence et la route de saisine.
 *
 * <p>L'auditorat du travail (art. 138bis C. jud.) n'est compétent que
 * pour les <b>infractions au droit social pénal</b> et l'avis civil
 * dans les matières art. 580/581 C. jud. Les litiges individuels purs
 * (contestation indemnité de préavis, paiement d'arriérés, prime,
 * etc.) relèvent du tribunal du travail directement (art. 578 C. jud.)
 * sans détour par l'auditorat.</p>
 *
 * <ul>
 *   <li>{@link #INFRACTION_PENALE_SOCIALE} — fait expressément
 *       qualifié dans le Livre 2 du Code pénal social (art. 119-235) :
 *       compétence directe de l'auditorat.</li>
 *   <li>{@link #ACCIDENT_TRAVAIL_GRAVE} — accident grave (mort,
 *       lésion permanente) déclenchant l'obligation de notification
 *       à l'inspection AT-MP (art. 26 Loi 04/08/1996 bien-être) et
 *       compétence d'enquête de l'auditeur du travail
 *       (art. 81 C. pén. soc.).</li>
 *   <li>{@link #TRAVAIL_NON_DECLARE_SUSPECTE} — suspicion de travail
 *       au noir (art. 181 C. pén. soc.) ; inspection préalable
 *       souvent nécessaire pour objectiver les faits.</li>
 *   <li>{@link #DISCRIMINATION_PENALE} — discrimination intentionnelle
 *       constituant l'infraction art. 22-24 Loi du 10/05/2007 tendant
 *       à lutter contre certaines formes de discrimination (en sus
 *       de l'action civile devant le tribunal du travail
 *       art. 587bis C. jud.).</li>
 *   <li>{@link #HARCELEMENT_PENAL} — harcèlement moral ou sexuel
 *       constituant l'infraction art. 442bis C. pén. (et non
 *       seulement le manquement à la Loi 04/08/1996 traité en
 *       voie civile + médecin du travail).</li>
 *   <li>{@link #ENTRAVE_INSPECTION} — entrave à la surveillance
 *       sociale (art. 209-212 C. pén. soc. : refus d'accès aux lieux
 *       de travail, refus de communication, fausse déclaration à
 *       l'inspecteur). Compétence directe auditorat.</li>
 *   <li>{@link #LITIGE_CIVIL_PUR} — pure contestation d'exécution du
 *       contrat / paiement / indemnité : <b>compétence tribunal du
 *       travail art. 578 C. jud.</b>, pas de l'auditorat. Saisine
 *       non pertinente.</li>
 *   <li>{@link #AUTRE} — qualification ouverte, examen avocat.</li>
 * </ul>
 */
public enum AuditoratTravailBeNatureFait {
    /** Infraction expressément qualifiée Code pénal social — Livre 2 art. 119-235. */
    INFRACTION_PENALE_SOCIALE,

    /** Accident grave au travail — Loi 04/08/1996 art. 26 + art. 124 C. pén. soc. */
    ACCIDENT_TRAVAIL_GRAVE,

    /** Suspicion travail non déclaré — art. 181 C. pén. soc., objectivation inspection. */
    TRAVAIL_NON_DECLARE_SUSPECTE,

    /** Discrimination pénale — art. 22-24 Loi 10/05/2007 (intentionnelle, dol spécial). */
    DISCRIMINATION_PENALE,

    /** Harcèlement moral ou sexuel pénal — art. 442bis C. pén. + Loi 04/08/1996. */
    HARCELEMENT_PENAL,

    /** Entrave à l'inspection sociale — art. 209-212 C. pén. soc. */
    ENTRAVE_INSPECTION,

    /** Litige civil pur — tribunal du travail art. 578 C. jud., pas auditorat. */
    LITIGE_CIVIL_PUR,

    /** Nature ouverte — qualification à établir par l'avocat. */
    AUTRE
}
