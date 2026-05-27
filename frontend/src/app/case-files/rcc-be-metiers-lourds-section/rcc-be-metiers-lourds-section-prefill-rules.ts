import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-01b — Helper partagé pour le pré-remplissage IA de
 * {@code RccBeMetiersLourdsSectionComponent} (F-219, BE uniquement).
 *
 * <p>Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * {@code prefillFromAi()} et static {@code getPrefillCount()} consomment
 * le MÊME helper pur ; divergence impossible par construction.</p>
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Alignement strict avec le pattern uniforme F-213 vagues 6b/7b/8b/9b/10b
 * (0 champ pré-rempli) — décision pilote pour les outils BE-only V1, qui
 * privilégie une saisie avocat assumée plutôt qu'une dérivation IA
 * incomplète. Justifications par champ :</p>
 *
 * <ul>
 *   <li>{@code ageFinContrat} — l'IA Travail BE extrait
 *       {@code dateNaissanceSalarie} et {@code dateRuptureContrat}, mais
 *       le calcul de l'âge à la fin du contrat est sensible (arrondi mois,
 *       cas pivot autour du 58e anniversaire vs date d'effet). Saisie
 *       directe avocat évite tout faux positif d'inéligibilité
 *       {@code AGE_INSUFFISANT} sur un dossier limite. À renvoyer dans
 *       une SF d'enrichissement IA dédiée si signal terrain.</li>
 *   <li>{@code anneesCarriereTotal} — qualification carrière complète
 *       (auto-entrepreneur, salariat partiel, périodes assimilées) qui
 *       relève de l'avocat sur la base des relevés ONP / extraits de
 *       carrière. Non extractible par le prompt Travail BE actuel.</li>
 *   <li>{@code anneesMetierLourdRecent10} /
 *       {@code anneesMetierLourdRecent15} — qualification métier lourd
 *       par fenêtre récente (10/15 ans) demande une analyse poste par
 *       poste, non automatisable sans données employeur précises
 *       (postes successifs, périodes équipes nuit, etc.).</li>
 *   <li>{@code typeMetierLourd} — qualification juridique 4 cas
 *       (EQUIPES_SUCCESSIVES_NUIT / INTERRUPTIONS_HORAIRES_VARIABLES /
 *       TRAVAIL_PENIBLE / AUTRE) saisie avocat systématique.</li>
 *   <li>{@code licenciementEffectif} — le flag {@code motifRupture} IA
 *       est texte libre court (« licenciement » / « démission » / etc.)
 *       et la qualification finale (licenciement effectif vs démission
 *       requalifiée) relève de l'avocat. Saisie manuelle V1 plutôt que
 *       valeur par défaut hasardeuse — pattern uniforme F-213.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code rcc-be-metiers-lourds-section-prefill-rules.spec.ts}.</p>
 */
export const RccBeMetiersLourdsSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * éléments du dossier (contrat de travail, lettre de licenciement,
   * extraits de carrière ONP, attestations employeur).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
