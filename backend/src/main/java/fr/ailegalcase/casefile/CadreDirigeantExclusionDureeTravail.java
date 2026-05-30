package fr.ailegalcase.casefile;

/**
 * SF-218-19 : conséquence de la qualification sur l'application des règles de
 * durée du travail (durée maximale, heures supplémentaires, repos, jours
 * fériés). Le cadre dirigeant est exclu de ces dispositions (art. L.3111-2 CT —
 * sauf congés payés). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>EXCLU : le salarié est exclu des dispositions sur la durée du travail
 *       (qualification de cadre dirigeant retenue).</li>
 *   <li>NON_EXCLU : le salarié reste soumis aux dispositions sur la durée du
 *       travail (qualification de cadre dirigeant écartée).</li>
 * </ul>
 */
public enum CadreDirigeantExclusionDureeTravail {
    EXCLU,
    NON_EXCLU
}
