package fr.ailegalcase.casefile;

/**
 * SF-213-05 : verdict de l'analyseur de protection contre le licenciement
 * pendant la grossesse / maternité en droit belge (Loi du 16/03/1971 art. 40).
 *
 * <h2>4 états possibles</h2>
 * <ul>
 *   <li>{@link #HORS_PERIODE_PROTECTION} — date de licenciement hors de la
 *       fenêtre [début grossesse ; fin congé maternité + 1 mois].</li>
 *   <li>{@link #PROTECTION_APPLICABLE_NON_NOTIFIEE} — licenciement intervenu
 *       pendant la période protégée mais sans notification écrite préalable
 *       de la grossesse à l'employeur. La protection s'applique mais la
 *       présomption d'inversion de charge ne joue pas.</li>
 *   <li>{@link #PROTECTION_APPLICABLE} — licenciement intervenu pendant la
 *       période protégée, notification écrite faite mais hors fenêtre de
 *       10 semaines post début grossesse — protection applicable, charge
 *       de preuve normale.</li>
 *   <li>{@link #PROTECTION_PRESUMEE} — notification écrite faite ET
 *       licenciement intervenu dans les 10 premières semaines suivant le
 *       début de la grossesse : présomption irréfragable que le licenciement
 *       est lié à la grossesse, charge de la preuve renversée — l'employeur
 *       doit prouver le motif étranger.</li>
 * </ul>
 *
 * <p>Sauf {@link #HORS_PERIODE_PROTECTION}, l'indemnité forfaitaire de
 * 6 mois de rémunération brute est due (art. 40 al. 3 Loi 16/03/1971),
 * cumulable avec les dommages prouvés.</p>
 */
public enum LicenciementBeProtectionGrossesseVerdict {
    /** Date de licenciement hors fenêtre protégée — pas d'indemnité forfaitaire. */
    HORS_PERIODE_PROTECTION,

    /** Période protégée + notification écrite absente — protection sans inversion. */
    PROTECTION_APPLICABLE_NON_NOTIFIEE,

    /** Période protégée + notification écrite hors fenêtre 10 sem — protection normale. */
    PROTECTION_APPLICABLE,

    /** Notification écrite + licenciement ≤ 10 sem post début grossesse — charge inversée. */
    PROTECTION_PRESUMEE
}
