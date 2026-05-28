import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-08b — Helper partagé pour le pré-remplissage IA de
 * {@code TransfertEntrepriseCct32bisSectionComponent} (F-219, BE
 * uniquement — CCT n° 32bis du 07/06/1985 + Loi 17/03/1965 + Directive
 * 2001/23/CE).
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
 *   <li>{@code dateTransfert} — date juridique de l'opération de transfert
 *       (acte de cession, jugement de fusion, AG de scission) ≠ date de
 *       rupture du contrat de travail extraite par l'IA. Saisie avocat
 *       depuis les actes.</li>
 *   <li>{@code typeOperation} — qualification juridique 9 cas (vente fonds,
 *       fusion, scission, apport, démembrement, faillite, liquidation,
 *       cession actions, intra-groupe) — relève du jugement avocat sur
 *       la base des actes officiels (acte authentique, projet de fusion,
 *       jugement d'ouverture).</li>
 *   <li>{@code identiteEconomique} — appréciation factuelle de la
 *       préservation de l'entité économique (CJUE Süzen, Abler) — saisie
 *       avocat après examen de la continuité d'activité.</li>
 *   <li>{@code infoConsultPrealableRespectee} / {@code conseilEntrepriseConsulte}
 *       — déroulement de la procédure d'information-consultation préalable
 *       (CE, DS, CPPT) — données employeur (PV CE, accord syndical) non
 *       extractibles du dossier salarié.</li>
 *   <li>{@code maintienContratsAutomatique} / {@code maintienAncienneteRespecte}
 *       / {@code maintienRemunerationRespecte} / {@code conventionsCollectivesMaintenues}
 *       — déclarations du cessionnaire post-transfert (lettre du
 *       cessionnaire, avenant au contrat) — saisie avocat depuis les
 *       pièces de reprise.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code transfert-entreprise-cct-32bis-section-prefill-rules.spec.ts}.</p>
 */
export const TransfertEntrepriseCct32bisSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * actes du transfert (acte de cession, projet de fusion, lettre du
   * cessionnaire, PV du conseil d'entreprise).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
