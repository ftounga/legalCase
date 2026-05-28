import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-27b — Helper partagé pour le pré-remplissage IA de
 * {@code InastriStatutTravailleurIndependantSectionComponent} (F-219,
 * BE uniquement — Loi du 27/06/1969 + AR n° 38 du 27/07/1967 + AR
 * du 19/12/1967 + Loi-programme I du 27/12/2006 art. 333 « doctrine
 * Bart Buysse » + art. 337/1 à 337/3 critères sectoriels).
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
 *   <li>{@code volonteParties} — qualification écrite ou implicite de
 *       la relation (contrat d'entreprise, contrat de prestations,
 *       statut INASTI déclaré, contrat de travail Loi 03/07/1978),
 *       relève de l'analyse contractuelle in concreto par l'avocat.</li>
 *   <li>{@code liberteOrganisationTemps} — modalité d'exécution
 *       réelle (horaires imposés, congés, jours non travaillés),
 *       appréciation factuelle ne se déduisant pas mécaniquement
 *       des données salariées extraites.</li>
 *   <li>{@code liberteOrganisationTravail} — choix méthodes / outils /
 *       sous-traitants, appréciation factuelle in concreto.</li>
 *   <li>{@code controleHierarchique} — pouvoir de direction et de
 *       sanction, appréciation factuelle in concreto — Cass. BE
 *       23/12/2002 S.02.0021.F primauté de l'exécution.</li>
 *   <li>{@code secteur} — qualification économique (construction,
 *       transport, gardiennage, nettoyage, agriculture-horticulture,
 *       autre), donnée externe (NACE / BCE / KBO).</li>
 *   <li>{@code nbCriteresSectorielsSalarie} — décompte des 9 critères
 *       sectoriels art. 337/2 § 1, appréciation factuelle multi-
 *       paramètres (immobilisations, statut gérant, exclusivité, etc.).</li>
 *   <li>{@code statutDeclare} — affiliation administrative
 *       (INASTI / DIMONA / aucune), donnée externe non extractible
 *       du dossier salarié individuel.</li>
 *   <li>{@code dimonaPresente} — vérification ONSS, donnée externe.</li>
 *   <li>{@code autreClientPresent} — indice d'autonomie économique
 *       (mono-client = indice salariat — Cass. BE 23/12/2002),
 *       donnée externe à constater (extraits bancaires, contrats
 *       commerciaux, factures).</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code inastri-statut-travailleur-independant-section-prefill-rules.spec.ts}.</p>
 */
export const InastriStatutTravailleurIndependantSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis
   * l'analyse contractuelle (qualification, exécution réelle,
   * critères de subordination, secteur, statut administratif,
   * présence d'autres clients).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
