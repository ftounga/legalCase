import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-213-09b — Helper partagé pour le pré-remplissage IA de
 * {@code LicenciementBeActeEquivalentSectionComponent} (F-213, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * `prefillFromAi()` et static `getPrefillCount()` consomment le MÊME
 * helper pur ; divergence impossible par construction.
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Le pipeline IA Travail BE actuel n'extrait <b>aucun</b> champ
 * directement exploitable pour piloter l'analyseur d'acte équipollent
 * à rupture :</p>
 *
 * <ul>
 *   <li>{@code typeModification} — la nature de la modification
 *       (SALAIRE, LIEU_TRAVAIL, FONCTION, HORAIRE, AUTRE) demande une
 *       qualification juridique sur la base du dossier (avenant refusé,
 *       courrier de l'employeur, échanges). Pas une extraction
 *       déterministe depuis les pièces contractuelles. Saisie avocat V1.</li>
 *   <li>{@code ampleurModification} (MINEURE / SUBSTANTIELLE /
 *       INDETERMINEES) — appréciation juridique fine, non extractible
 *       mécaniquement. Saisie avocat V1.</li>
 *   <li>{@code dateModification} — non extraite spécifiquement par le
 *       prompt Travail BE actuel (différente de {@code dateLicenciement} —
 *       il s'agit ici de la date à laquelle l'employeur a notifié ou
 *       imposé la modification, antérieure à toute rupture). À arbitrer
 *       dans une SF future d'enrichissement IA Travail BE.</li>
 *   <li>{@code elementEssentielDuContrat} — qualification juridique
 *       (essentialité d'un élément du contrat) saisie avocat
 *       systématique.</li>
 *   <li>{@code salarieAProteste} — fait procédural à constater
 *       (mise en demeure, lettre de protestation), saisie avocat
 *       systématique.</li>
 *   <li>{@code remunerationHebdomadaireBrute},
 *       {@code dureePreavisCalculeeSemaines} — données dérivées d'autres
 *       outils (SF-213-03 statut unique préavis, SF-213-04 formule Claeys)
 *       ; le pré-fill cross-outils n'est pas géré dans cette SF (à
 *       envisager via une orchestration F-IA-04 dédiée).</li>
 * </ul>
 *
 * <p>Décision alignée sur le pattern F-213 frontend (vagues 6b/7b/8b —
 * 0 champ pré-rempli). Enrichissement éventuel à arbitrer dans une SF
 * future dédiée.</p>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code licenciement-be-acte-equivalent-section-prefill-rules.spec.ts}.</p>
 */
export const LicenciementBeActeEquivalentSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * éléments du dossier (avenant refusé, courrier employeur, lettre de
   * protestation).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
