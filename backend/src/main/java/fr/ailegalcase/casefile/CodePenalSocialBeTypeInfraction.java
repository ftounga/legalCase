package fr.ailegalcase.casefile;

/**
 * SF-219-24 : type d'infraction sociale qualifiée au sens du Code
 * pénal social du 06/06/2010 (Livre 2 — Les infractions et leur
 * répression en particulier).
 *
 * <p>Les exemples ci-dessous reflètent la lecture standard de la
 * doctrine (notamment Van Eeckhoutte, <i>Compendium de droit social
 * pénal</i> ; Henrard, <i>Code pénal social commenté</i>) et la
 * jurisprudence Cour cass. ch. néerl. 21/11/2017 P.17.0532.N
 * (qualification du travail non déclaré comme infraction de niveau
 * 4). Toute qualification finale relève de l'auditorat du travail
 * et du tribunal de première instance / police selon l'orientation
 * de l'affaire (art. 81 C. pén. soc.).</p>
 *
 * <ul>
 *   <li>{@link #TRAVAIL_NON_DECLARE} — art. 181 C. pén. soc.
 *       (Dimona, déclaration immédiate de l'emploi à l'ONSS) :
 *       <b>niveau 4</b> par défaut. Cass. P.17.0532.N.</li>
 *   <li>{@link #ABSENCE_DIMONA} — art. 181 C. pén. soc. : variante
 *       formelle du travail non déclaré, niveau 4.</li>
 *   <li>{@link #ABSENCE_DMFA} — art. 223 C. pén. soc. (déclaration
 *       multifonctionnelle ONSS) : niveau 4 si manquement délibéré,
 *       sinon niveau 2.</li>
 *   <li>{@link #NON_PAIEMENT_REMUNERATION} — art. 162-167 C. pén.
 *       soc. : niveau 2 (manquement à la Loi 12/04/1965 sur la
 *       protection de la rémunération).</li>
 *   <li>{@link #DEPASSEMENT_TEMPS_TRAVAIL} — art. 137-138 C. pén.
 *       soc. : niveau 3 (heures supplémentaires hors dérogations
 *       Loi 16/03/1971).</li>
 *   <li>{@link #INFRACTION_BIEN_ETRE} — art. 119-132 C. pén. soc. :
 *       niveau 3 (manquement aux dispositions du Code du
 *       bien-être au travail, AR 28/05/2003).</li>
 *   <li>{@link #ACCIDENT_GRAVE_NON_DECLARE} — art. 124 C. pén. soc. :
 *       niveau 4 (omission de déclaration accident grave à
 *       l'auditorat / inspection).</li>
 *   <li>{@link #DISCRIMINATION_EMPLOI} — art. 145 C. pén. soc. +
 *       Loi 10/05/2007 : niveau 2.</li>
 *   <li>{@link #ENTRAVE_INSPECTION_SOCIALE} — art. 209-212 C. pén.
 *       soc. : niveau 3 (entrave aux pouvoirs d'enquête, refus
 *       d'accès, fausse déclaration à l'inspecteur).</li>
 *   <li>{@link #FAUX_SOCIAL} — art. 232-235 C. pén. soc. : niveau 4
 *       (faux en écritures sociales — Dimona / DmfA / registres
 *       personnel falsifiés).</li>
 *   <li>{@link #ABSENCE_REGISTRE_PERSONNEL} — art. 185 C. pén. soc. :
 *       niveau 3 (registre général du personnel AR 08/08/1980).</li>
 *   <li>{@link #INFRACTION_INTERIM} — art. 178 C. pén. soc. : niveau
 *       3 (Loi 24/07/1987 — motifs non autorisés, durée dépassée).</li>
 *   <li>{@link #INFRACTION_DETACHEMENT} — art. 182-184 C. pén. soc. +
 *       Loi 05/03/2002 LIMOSA : niveau 3 ou 4 selon gravité.</li>
 *   <li>{@link #AUTRE_QUALIFICATION} — qualification précise à
 *       établir par l'avocat ; renvoie {@link
 *       CodePenalSocialBeVerdict#A_QUALIFIER} sauf si le niveau
 *       proposé est explicitement renseigné.</li>
 * </ul>
 */
public enum CodePenalSocialBeTypeInfraction {
    /** Travail non déclaré (Dimona) — art. 181 C. pén. soc., niveau 4. */
    TRAVAIL_NON_DECLARE,

    /** Absence de déclaration Dimona — art. 181 C. pén. soc., niveau 4. */
    ABSENCE_DIMONA,

    /** Absence de DmfA ONSS — art. 223 C. pén. soc., niveau 4 (intentionnel) / 2. */
    ABSENCE_DMFA,

    /** Non-paiement de la rémunération — art. 162-167 C. pén. soc., niveau 2. */
    NON_PAIEMENT_REMUNERATION,

    /** Dépassement durée du travail — art. 137-138 C. pén. soc., niveau 3. */
    DEPASSEMENT_TEMPS_TRAVAIL,

    /** Manquement bien-être au travail — art. 119-132 C. pén. soc., niveau 3. */
    INFRACTION_BIEN_ETRE,

    /** Accident grave non déclaré — art. 124 C. pén. soc., niveau 4. */
    ACCIDENT_GRAVE_NON_DECLARE,

    /** Discrimination à l'emploi — art. 145 C. pén. soc., niveau 2. */
    DISCRIMINATION_EMPLOI,

    /** Entrave à l'inspection sociale — art. 209-212 C. pén. soc., niveau 3. */
    ENTRAVE_INSPECTION_SOCIALE,

    /** Faux en écritures sociales — art. 232-235 C. pén. soc., niveau 4. */
    FAUX_SOCIAL,

    /** Absence registre du personnel — art. 185 C. pén. soc., niveau 3. */
    ABSENCE_REGISTRE_PERSONNEL,

    /** Infraction au travail intérimaire — art. 178 C. pén. soc., niveau 3. */
    INFRACTION_INTERIM,

    /** Infraction détachement / LIMOSA — art. 182-184 C. pén. soc., niveau 3 ou 4. */
    INFRACTION_DETACHEMENT,

    /** Qualification ouverte — bascule en A_QUALIFIER si niveau non précisé. */
    AUTRE_QUALIFICATION
}
