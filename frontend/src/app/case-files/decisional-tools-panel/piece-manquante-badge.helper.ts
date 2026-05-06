import { PieceManquanteAlignment } from '../../core/models/piece-manquante-alignment.model';

/**
 * F-194 SF-194-02 — Verdict utilisé par la card du panel F-IA-04 pour afficher
 * un pill `📎 Pièces (D/O/N)` à côté des autres pills (auto_awesome,
 * retained-pistes, procedure-checks). Calculé via static
 * `getPiecesBadge` exposé par chaque composant outil instrumenté — pattern
 * miroir `getRetainedPistesBadge` (F-192 SF-192-02) et
 * `getProcedureChecksBadge` (F-193 SF-193-02).
 *
 * <p>Hiérarchie des kinds (priorité décroissante) :
 * <ol>
 *   <li><b>missing</b>     : ≥ 1 pièce A_DEMANDER (priorité absolue —
 *       l'avocat doit voir qu'il manque encore des pièces avant tout).</li>
 *   <li><b>partial</b>     : 0 A_DEMANDER, ≥ 1 OBTENUE et ≥ 1
 *       NON_APPLICABLE.</li>
 *   <li><b>obtained</b>    : 0 A_DEMANDER, 0 NON_APPLICABLE, ≥ 1 OBTENUE.</li>
 *   <li><b>not_applicable</b> : 0 A_DEMANDER, 0 OBTENUE, ≥ 1 NON_APPLICABLE.</li>
 *   <li><b>none</b>        : aucune pièce mappée à cet outil.</li>
 * </ol></p>
 */
export type PiecesBadgeKind =
  | 'missing'
  | 'partial'
  | 'obtained'
  | 'not_applicable'
  | 'none';

export interface PiecesBadgeCounts {
  /** Pièces A_DEMANDER (ou par défaut tant que pas taggées). */
  aDemander: number;
  /** Pièces OBTENUE. */
  obtenues: number;
  /** Pièces NON_APPLICABLE. */
  nonApplicable: number;
}

export interface PiecesBadge {
  kind: PiecesBadgeKind;
  counts: PiecesBadgeCounts;
}

export interface PiecesBadgeInput {
  piecesAlignment?: PieceManquanteAlignment[] | null;
}

/**
 * Calcule le badge à partir de la liste filtrée d'alignements (déjà restreinte
 * au toolId courant par le panel via `inputs(ctx)`). Pure ; sans effet de bord.
 *
 * Règles :
 * <ul>
 *   <li>Si la liste est vide → `kind = 'none'`, counts à 0.</li>
 *   <li>aDemander = nb A_DEMANDER, obtenues = nb OBTENUE,
 *       nonApplicable = nb NON_APPLICABLE.</li>
 *   <li>kind par priorité visuelle (cf. JSDoc supra).</li>
 * </ul>
 */
export function computePiecesBadge(
  filtered: PieceManquanteAlignment[],
): PiecesBadge {
  const counts: PiecesBadgeCounts = {
    aDemander: filtered.filter(p => p.statut === 'A_DEMANDER').length,
    obtenues: filtered.filter(p => p.statut === 'OBTENUE').length,
    nonApplicable: filtered.filter(p => p.statut === 'NON_APPLICABLE').length,
  };
  const total = counts.aDemander + counts.obtenues + counts.nonApplicable;
  if (total === 0) {
    return { kind: 'none', counts };
  }
  if (counts.aDemander > 0) return { kind: 'missing', counts };
  if (counts.obtenues > 0 && counts.nonApplicable > 0) {
    return { kind: 'partial', counts };
  }
  if (counts.obtenues > 0) return { kind: 'obtained', counts };
  return { kind: 'not_applicable', counts };
}

/**
 * Variante avec filtre par `toolId` — utilisée quand la liste reçue est
 * globale (cas du tableau `piecesAlignment` non pré-filtré côté panel).
 */
export function getPiecesBadgeFor(
  input: PiecesBadgeInput,
  toolId: string,
): PiecesBadge {
  const all = Array.isArray(input.piecesAlignment) ? input.piecesAlignment : [];
  const filtered = all.filter(p => Array.isArray(p.toolIdsCibles) && p.toolIdsCibles.includes(toolId));
  return computePiecesBadge(filtered);
}

/**
 * Helper utilitaire : extrait les libellés des pièces statut OBTENUE pour un
 * outil donné. Consommé par les composants outils via `@Input() piecesObtenues`
 * pour pré-cocher leurs cases correspondantes.
 */
export function piecesObtenuesFor(
  alignment: PieceManquanteAlignment[] | null | undefined,
  toolId: string,
): string[] {
  if (!Array.isArray(alignment)) return [];
  return alignment
    .filter(p => p.statut === 'OBTENUE'
      && Array.isArray(p.toolIdsCibles)
      && p.toolIdsCibles.includes(toolId))
    .map(p => p.pieceLibelle);
}
