import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-10b — Helper partagé pour le pré-remplissage IA de
 * {@code DelegueSyndicalCct5SectionComponent} (F-219, BE uniquement —
 * CCT n° 5 du 24/05/1971 + CCT 5bis/5ter + AR 26/01/1972 + CCT
 * sectorielles).
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
 *   <li>{@code dateDesignation} — date à laquelle l'OS a notifié la
 *       désignation à l'employeur. Information employeur / OS, pas
 *       extractible depuis le dossier salarié individuel ; saisie avocat
 *       depuis le procès-verbal de désignation / courrier OS.</li>
 *   <li>{@code effectifEntreprise} — donnée employeur (DmfA ONSS), non
 *       extractible du dossier salarié.</li>
 *   <li>{@code seuilSectorielRequis} — paramétrage juridique de la CCT
 *       sectorielle applicable (20 ou 50 le plus souvent). Choix avocat
 *       en fonction de la commission paritaire — non factualisable
 *       depuis la pipeline IA Travail BE.</li>
 *   <li>{@code presenceOsRepresentative} — qualification syndicale,
 *       saisie avocat depuis le ROI / dossier syndical interne.</li>
 *   <li>{@code statutDesignation} — qualification juridique 4 cas
 *       (régulière, notification manquante, pas par OS, contestée) —
 *       relève du jugement avocat sur la base des actes (procès-verbal
 *       de désignation, courrier de notification, éventuelle action en
 *       contestation).</li>
 *   <li>{@code ceExistant} / {@code cpptExistant} — qualification
 *       institutionnelle employeur (résultat des élections sociales
 *       précédentes), saisie avocat depuis les procès-verbaux d'élection
 *       ou le ROI — non extractible du dossier salarié.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code delegue-syndical-cct-5-section-prefill-rules.spec.ts}.</p>
 */
export const DelegueSyndicalCct5SectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * éléments du dossier procédural (procès-verbal de désignation OS,
   * courrier de notification employeur, procès-verbal d'élection
   * CE/CPPT, ROI).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
