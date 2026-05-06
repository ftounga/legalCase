import { ProcedureCheckAlignment } from '../../core/models/procedure-check-alignment.model';

/**
 * F-193 SF-193-02 — Verdict utilisé par la card du panel F-IA-04 pour afficher
 * un pill `🔍 Procédure (V/N/T)` à côté des autres pills (auto_awesome,
 * retained-pistes). Calculé via `getProcedureChecksBadge()` exposé par chaque
 * composant outil instrumenté — pattern miroir `getRetainedPistesBadge`
 * (F-192 SF-192-02) et `getPrefillCount` (F-177 SF-177-12).
 *
 * <p>Hiérarchie des kinds (priorité décroissante) :
 * <ol>
 *   <li><b>non_compliant</b> : ≥ 1 NON_COMPLIANT_FLAG (priorité absolue —
 *       l'avocat doit voir l'alerte avant tout).</li>
 *   <li><b>to_verify</b>     : 0 NON_COMPLIANT et ≥ 1 TO_VERIFY_FLAG.</li>
 *   <li><b>verified</b>      : 0 NON_COMPLIANT, 0 TO_VERIFY, ≥ 1 ALIGNED.</li>
 *   <li><b>mixed</b>         : combinaison incluant ALIGNED + (NON_COMPLIANT
 *       OU TO_VERIFY) — priorité visuelle gagne (cf. helper
 *       {@link mixedKindFor}).</li>
 *   <li><b>none</b>          : aucun check pertinent (aucun
 *       ALIGNED/NON_COMPLIANT/TO_VERIFY pour cet outil).</li>
 * </ol></p>
 *
 * <p>Note : la sémantique "mixed" est utilisée quand >= 2 statuts non-vides
 * coexistent. Dans ce cas, on retombe sur la priorité visuelle (NON_COMPLIANT
 * > TO_VERIFY > VERIFIED) pour le `kind` mais on signale `mixed=true` via le
 * `counts` non-trivial — le pill reste rouge subtil ou gris ou or selon le
 * statut dominant pour rester lisible. La V1 du contrat retourne toutefois
 * un `kind` non-mixed à priorité, ce qui simplifie l'expression CSS.</p>
 */
export type ProcedureChecksBadgeKind =
  | 'verified'
  | 'non_compliant'
  | 'to_verify'
  | 'mixed'
  | 'none';

export interface ProcedureChecksBadgeCounts {
  verified: number;
  nonCompliant: number;
  toVerify: number;
}

export interface ProcedureChecksBadge {
  kind: ProcedureChecksBadgeKind;
  counts: ProcedureChecksBadgeCounts;
}

export interface ProcedureChecksBadgeInput {
  proceduresChecksAlignment?: ProcedureCheckAlignment[] | null;
}

/**
 * Calcule le badge à partir de la liste filtrée d'alignements (déjà restreinte
 * au toolId courant par le panel via `inputs(ctx)`). Pure ; sans effet de bord.
 *
 * Règles :
 * <ul>
 *   <li>Si la liste est vide → `kind = 'none'`, counts à 0.</li>
 *   <li>verified = nb ALIGNED, nonCompliant = nb NON_COMPLIANT_FLAG,
 *   toVerify = nb TO_VERIFY_FLAG.</li>
 *   <li>kind :
 *     <ul>
 *       <li>mixed si ≥ 2 catégories non-zéro,</li>
 *       <li>sinon non_compliant > to_verify > verified > none.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
export function getProcedureChecksBadgeFor(
  input: ProcedureChecksBadgeInput,
  toolId: string,
): ProcedureChecksBadge {
  const all = Array.isArray(input.proceduresChecksAlignment)
    ? input.proceduresChecksAlignment
    : [];
  const filtered = all.filter(c => c.toolIdCible === toolId);
  return computeBadge(filtered);
}

/**
 * Variante du helper appelée quand la liste est déjà filtrée par le panel.
 * Utile dans les composants outils dont le static helper reçoit la liste
 * pré-restreinte au toolId courant.
 */
export function computeBadge(
  filtered: ProcedureCheckAlignment[],
): ProcedureChecksBadge {
  const counts: ProcedureChecksBadgeCounts = {
    verified: filtered.filter(c => c.matchStatus === 'ALIGNED').length,
    nonCompliant: filtered.filter(c => c.matchStatus === 'NON_COMPLIANT_FLAG').length,
    toVerify: filtered.filter(c => c.matchStatus === 'TO_VERIFY_FLAG').length,
  };
  const total = counts.verified + counts.nonCompliant + counts.toVerify;
  if (total === 0) {
    return { kind: 'none', counts };
  }
  const nonZeroCategories =
    (counts.verified > 0 ? 1 : 0)
    + (counts.nonCompliant > 0 ? 1 : 0)
    + (counts.toVerify > 0 ? 1 : 0);
  if (nonZeroCategories >= 2) {
    return { kind: 'mixed', counts };
  }
  if (counts.nonCompliant > 0) return { kind: 'non_compliant', counts };
  if (counts.toVerify > 0) return { kind: 'to_verify', counts };
  return { kind: 'verified', counts };
}
