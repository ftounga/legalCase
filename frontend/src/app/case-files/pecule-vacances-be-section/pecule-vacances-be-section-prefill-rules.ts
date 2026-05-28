import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-20b — Helper partagé pour le pré-remplissage IA de
 * {@code PeculeVacancesBeSectionComponent} (F-219, BE uniquement —
 * Lois coordonnées du 28/06/1971 relatives aux vacances annuelles des
 * travailleurs salariés + AR du 30/03/1967 déterminant les modalités
 * générales d'exécution).
 *
 * <p>Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * {@code prefillFromAi()} et static {@code getPrefillCount()} consomment
 * le MÊME helper pur ; divergence impossible par construction.</p>
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Alignement strict avec le pattern uniforme F-213 / F-219 vagues
 * antérieures (0 champ pré-rempli). Justifications par champ :</p>
 *
 * <ul>
 *   <li>{@code statut} — qualification administrative (employé /
 *       ouvrier / jeune travailleur 1re année) propre au régime des
 *       vacances annuelles BE (art. 19, 38, 9 AR 30/03/1967) ; le
 *       pipeline IA Travail BE n'extrait pas ce statut horaire
 *       commission paritaire.</li>
 *   <li>{@code typeCalcul} — choix de l'avocat (simple / double /
 *       départ) selon la situation contentieuse (réclamation pendant
 *       contrat vs sortie), pas dérivable mécaniquement du dossier
 *       salarié principal.</li>
 *   <li>{@code remunerationBruteAnnuelleExerciceEur} —
 *       {@code salaireBrutAnnuel} existe dans TravailExtractedData mais
 *       il s'agit de la rémunération courante, pas nécessairement de
 *       l'exercice de vacances (année civile précédente — art. 1 Lois
 *       coordonnées 28/06/1971). Lien causal V1 risqué → déclaratif
 *       avocat.</li>
 *   <li>{@code remunerationMensuelleBruteEur} — {@code salaireBrutMensuel}
 *       existe mais peut différer (sursalaire, primes, mois variables) ;
 *       sans pré-fill V1 pour éviter les faux positifs sur le calcul
 *       du double pécule employé (85 % du salaire mensuel — art. 38
 *       AR 30/03/1967).</li>
 *   <li>{@code remunerationBruteAnnuelleExercicePrecedentEur} — donnée
 *       historique de l'année N-2 pour le pécule de départ (art. 46
 *       al. 2 AR 30/03/1967), hors pipeline.</li>
 *   <li>{@code joursCongesPris} — historique RH employeur (compteur
 *       congés annuel), non extractible du dossier salarié.</li>
 *   <li>{@code peculeDejaPaye} — flag administratif (fiche de paie /
 *       quitus employeur), déclaratif avocat.</li>
 *   <li>{@code dateSortie} — bien que {@code dateRuptureContrat} existe
 *       dans TravailExtractedData, le lien causal avec un calcul
 *       PECULE_DEPART n'est ouvert que si {@code typeCalcul} le
 *       requiert ; pas de pré-fill mécanique V1 pour éviter d'imposer
 *       une date dans un calcul PECULE_SIMPLE / DOUBLE_PECULE.</li>
 *   <li>{@code dateFinContrat} — idem, alignement strict pattern
 *       uniforme V1.</li>
 *   <li>{@code dateReclamation} — date du courrier de réclamation /
 *       de la requête déposée au tribunal du travail, donnée
 *       contentieuse pure, hors pipeline.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code pecule-vacances-be-section-prefill-rules.spec.ts}.</p>
 */
export const PeculeVacancesBeSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * dossier RH employeur (fiches de paie, compteur congés, fiche
   * salaire annuelle, éventuelle preuve de versement antérieur du
   * pécule).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
