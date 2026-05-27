import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-213-06b — Helper partagé pour le pré-remplissage IA de
 * {@code TransactionBeTravailSectionComponent} (F-213, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * `prefillFromAi()` et static `getPrefillCount()` consomment le MÊME
 * helper pur ; divergence impossible par construction.
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>L'analyse fine d'un document de transaction (lecture sémantique des
 * clauses, détection des concessions employeur, des renonciations
 * expresses, des références à un litige né ou à naître…) est explicitement
 * <b>hors scope SF-213-06</b> selon la mini-spec. Le pipeline IA Travail BE
 * actuel n'extrait <b>aucun</b> champ exploitable pour cet outil :</p>
 *
 * <ul>
 *   <li>{@code montantTransactionBrut} — le montant figure dans le
 *       document de transaction lui-même, mais il n'existe pas de
 *       champ IA fiable et factuel. Le {@code salaireBrutAnnuel} qui
 *       sert ailleurs (clause non-concurrence, RCC, ICP) n'a aucun lien
 *       sémantique avec le montant d'une transaction. Saisie avocat
 *       systématique.</li>
 *   <li>{@code indemniteLegaleEtimee} — calcul juridique dérivé (ICP +
 *       CCT 109…) qui dépend d'éléments hétérogènes (ancienneté,
 *       motif, RCC…). V1 : saisie avocat à partir des outils ICP /
 *       CCT 109 dédiés.</li>
 *   <li>{@code concessionsEmployeurDescrites},
 *       {@code renonciationOrdrePublicDetectee},
 *       {@code mentionContestation},
 *       {@code renonciationsListees} — booléens / liste résultant
 *       d'une analyse sémantique du document non couverte par le
 *       prompt Travail BE actuel (les 5 flags F-204 BE existants —
 *       harcelement, discrimination, inaptitude, heures sup, motif
 *       grave — n'incluent pas la transaction).</li>
 * </ul>
 *
 * <p>Décision validée — aligne sur la note feedback F-213 backend
 * pattern : pas d'enrichissement IA dans cette vague. Enrichissement
 * éventuel à arbitrer dans une SF future dédiée (analyse sémantique
 * documents de transaction).</p>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 — même si
 * en V1 cette gate n'a pas d'effet (le count est toujours 0).</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input
 * — testé par {@code transaction-be-travail-section-prefill-rules.spec.ts}.</p>
 */
export const TransactionBeTravailSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les 6 champs depuis
   * le document de transaction.</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
