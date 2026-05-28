package fr.ailegalcase.casefile;

/**
 * SF-219-19 : verdict de l'outil <i>droit à la déconnexion BE</i>
 * (Loi du 03/10/2022 « Deal pour l'emploi » M.B. 10/11/2022, art. 16
 * + AR du 19/02/2023 fixant la date d'entrée en vigueur au
 * 01/04/2023 + CCT n° 149 du Conseil national du travail).
 *
 * <h2>Conditions cumulatives pour qu'un employeur BE soit en
 * conformité avec l'obligation déconnexion</h2>
 * <ol>
 *   <li><b>Seuil 20 travailleurs</b> — art. 16 § 1 Loi 03/10/2022 :
 *       l'obligation ne pèse que sur les employeurs occupant au moins
 *       20 travailleurs. En deçà, l'obligation est inexistante (le
 *       régime du télétravail CCT n° 85 reste cependant applicable
 *       quel que soit l'effectif).</li>
 *   <li><b>Instrument formalisé</b> — l'obligation se matérialise par
 *       une <b>CCT d'entreprise</b> déposée au greffe du SPF Emploi
 *       (instrument privilégié) ou, à défaut, par une <b>modification
 *       du règlement de travail</b> selon la procédure ordinaire
 *       (Loi 08/04/1965, art. 11 et 12).</li>
 *   <li><b>Modalités pratiques de déconnexion</b> — art. 16 § 2 1° :
 *       l'instrument doit définir les modalités pratiques
 *       d'application du droit du travailleur de ne pas être joint
 *       en dehors de son horaire de travail (e-mails / appels /
 *       messages hors heures, jours fériés, congés).</li>
 *   <li><b>Sensibilisation et formation</b> — art. 16 § 2 2° :
 *       l'instrument doit prévoir des actions de sensibilisation et
 *       de formation à un usage raisonné des outils numériques et
 *       aux risques liés à la connexion permanente
 *       (épuisement professionnel / burn-out).</li>
 *   <li><b>Modalités d'organisation du travail</b> — art. 16 § 2 3° :
 *       l'instrument doit décrire les modalités d'organisation visant
 *       à garantir le respect du temps de repos
 *       (Loi 16/03/1971, art. 38 quinquies).</li>
 * </ol>
 *
 * <h2>Verdicts hiérarchisés</h2>
 * <ul>
 *   <li>{@link #HORS_CHAMP_SEUIL_NON_ATTEINT} — employeur &lt; 20
 *       travailleurs : obligation déconnexion non applicable.</li>
 *   <li>{@link #CONFORME_ACCORD_COMPLET} — CCT ou règlement de travail
 *       formalisé ET contenu complet (3 thèmes art. 16 § 2 traités).</li>
 *   <li>{@link #NON_CONFORME_CONTENU_INCOMPLET} — instrument formalisé
 *       mais un ou plusieurs thèmes obligatoires manquants.</li>
 *   <li>{@link #NON_CONFORME_INSTRUMENT_MANQUANT} — aucun instrument
 *       formalisé alors que la concertation est engagée : manquement
 *       à l'obligation de résultat (art. 16 § 1).</li>
 *   <li>{@link #MANQUEMENT_GRAVE_AUCUNE_INITIATIVE} — aucun accord ni
 *       amorce de concertation : manquement frontal exposant l'employeur
 *       aux sanctions du C. pén. social (art. 137 — sanction de niveau
 *       2) et à l'action en cessation par les organisations syndicales.</li>
 *   <li>{@link #A_ANALYSER} — configuration ambigüe (statut indéterminé,
 *       articulation avec la CCT n° 149 ou la CCT n° 85 télétravail) →
 *       analyse cas par cas.</li>
 * </ul>
 */
public enum DroitDeconnexionBeVerdict {
    /** Employeur &lt; 20 travailleurs : obligation non applicable. */
    HORS_CHAMP_SEUIL_NON_ATTEINT,

    /** Instrument formalisé + 3 thèmes art. 16 § 2 traités. */
    CONFORME_ACCORD_COMPLET,

    /** Instrument formalisé mais thèmes obligatoires manquants. */
    NON_CONFORME_CONTENU_INCOMPLET,

    /** Aucun instrument formalisé bien que concertation en cours. */
    NON_CONFORME_INSTRUMENT_MANQUANT,

    /** Aucun accord ni amorce de concertation — manquement frontal. */
    MANQUEMENT_GRAVE_AUCUNE_INITIATIVE,

    /** Configuration ambigüe — analyse cas par cas. */
    A_ANALYSER
}
