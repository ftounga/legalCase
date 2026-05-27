import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-213-07b — Helper partagé pour le pré-remplissage IA de
 * {@code HarcelementBeProcedureFormelleSectionComponent} (F-213, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * `prefillFromAi()` et static `getPrefillCount()` consomment le MÊME
 * helper pur ; divergence impossible par construction.
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Le pipeline IA Travail BE actuel n'extrait <b>aucun</b> champ
 * exploitable pour piloter une procédure interne de plainte :</p>
 *
 * <ul>
 *   <li>{@code typeHarcelement} — le flag {@code harcelementBeDetecte} (booléen
 *       F-204) ne distingue pas moral / sexuel / les deux. Saisie avocat.</li>
 *   <li>{@code etapeProcedure} — l'étape procédurale (avant dépôt, demande
 *       informelle, formelle…) ne fait pas partie de l'extraction IA Travail
 *       BE — c'est un état piloté par l'avocat selon les éléments du
 *       dossier.</li>
 *   <li>{@code dateDepotPlainte} — non extraite par le prompt actuel
 *       (les dates extraites sont dateRupture, dateLicenciement,
 *       dateEmbauche).</li>
 *   <li>{@code entreprisePossedeCPAP} / {@code entrepriseTaille} — données
 *       structurelles d'entreprise (effectif, dispositif RPS) non couvertes
 *       par l'extraction IA Travail BE.</li>
 *   <li>{@code mesureDefavorableApres} — appréciation juridique post-dépôt,
 *       saisie avocat systématique.</li>
 * </ul>
 *
 * <p>Décision alignée sur le pattern F-213 backend (note feedback) : pas
 * d'enrichissement IA dans cette vague. Enrichissement éventuel à arbitrer
 * dans une SF future dédiée (extraction des marqueurs harcèlement
 * détaillée).</p>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code harcelement-be-procedure-formelle-section-prefill-rules.spec.ts}.</p>
 */
export const HarcelementBeProcedureFormelleSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * éléments du dossier.</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
