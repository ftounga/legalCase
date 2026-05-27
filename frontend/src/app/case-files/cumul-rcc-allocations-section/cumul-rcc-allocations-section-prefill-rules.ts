import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-04b — Helper partagé pour le pré-remplissage IA de
 * {@code CumulRccAllocationsSectionComponent} (F-219, BE uniquement).
 *
 * <p>Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * {@code prefillFromAi()} et static {@code getPrefillCount()} consomment
 * le MÊME helper pur ; divergence impossible par construction.</p>
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Alignement strict avec le pattern uniforme F-213 / F-219 vagues
 * antérieures (0 champ pré-rempli) — décision pilote pour les outils
 * BE-only V1 qui privilégie une saisie avocat assumée plutôt qu'une
 * dérivation IA incomplète. Justifications par champ :</p>
 *
 * <ul>
 *   <li>{@code dateNaissance} — l'IA Travail BE extrait certes
 *       {@code dateNaissanceSalarie}, mais la transposition vers le calcul
 *       d'âge précis à la date d'entrée RCC est sensible (cas pivot
 *       autour du seuil pension 65 ans / disponibilité 62 ans). Saisie
 *       directe avocat évite tout faux positif d'éligibilité.</li>
 *   <li>{@code dateDebutRcc} — la date de prise d'effet du RCC (≠ date
 *       de licenciement ≠ date de fin du préavis) n'est pas extraite par
 *       le prompt Travail BE actuel et relève typiquement d'une décision
 *       postérieure de l'ONEM / convention CCT 17. Saisie avocat
 *       systématique.</li>
 *   <li>{@code allocationChomageMensuelle} — montant ONEM calculé par
 *       l'organisme de paiement (CAPAC ou syndicat) sur la rémunération
 *       brute, non disponible dans l'extraction Travail. Saisie avocat
 *       depuis l'attestation ONEM C4.</li>
 *   <li>{@code indemniteComplementaireMensuelle} — montant CCT 17 négocié
 *       avec l'employeur, propre à chaque dossier (varie selon ancienneté,
 *       commission paritaire, accord d'entreprise). Non extractible.</li>
 *   <li>{@code remunerationNetteReference} — dernière rémunération nette,
 *       relevée du dernier bulletin de paie ou attestation employeur. Le
 *       prompt Travail BE extrait des montants bruts ; la conversion
 *       brut/net dépend du précompte professionnel et de la situation
 *       familiale du salarié — qualification avocat.</li>
 *   <li>{@code anneesCarriereTotale} — qualification carrière complète
 *       (ETP ONSS, périodes assimilées, jobs étudiants, périodes
 *       d'incapacité) qui relève de l'avocat sur la base des relevés
 *       My Career / extraits ONP. Non extractible V1.</li>
 *   <li>{@code activiteComplementaire} — qualification juridique 4 cas
 *       (AUCUNE / INDEPENDANT_ACCESSOIRE / BENEVOLE_DECLAREE /
 *       NON_DECLAREE) — saisie avocat systématique, élément déclaratif
 *       du client.</li>
 *   <li>{@code ageLegalPension} — paramètre légal (défaut backend = 65)
 *       maintenu en saisie optionnelle avocat pour permettre de simuler
 *       la progression 66 ans en 2027 / 67 ans en 2030 sans recompiler.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code cumul-rcc-allocations-section-prefill-rules.spec.ts}.</p>
 */
export const CumulRccAllocationsSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * éléments du dossier (attestation ONEM C4, bulletins de paie,
   * extraits de carrière ONP, accord CCT 17 employeur).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
