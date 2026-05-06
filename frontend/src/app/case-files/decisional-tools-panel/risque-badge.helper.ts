import { RisqueAlignment } from '../../core/models/risque-alignment.model';

/**
 * F-195 SF-195-02 — Verdict utilisé par la card du panel F-IA-04 pour afficher
 * un pill `⚠️ Risques (V/E)` à côté des autres pills (auto_awesome, retained,
 * procedures, pieces). Calculé via le helper partagé exposé via static
 * {@code getRisquesBadge} (ou alimenté directement par le panel via
 * {@link getRisquesBadgeFor}).
 *
 * Pattern miroir `getRetainedPistesBadge` (F-192 SF-192-02),
 * `getProcedureChecksBadge` (F-193 SF-193-02) et
 * `getPiecesBadge` (F-194 SF-194-02).
 *
 * <p>Hiérarchie des kinds (priorité décroissante) :
 * <ol>
 *   <li><b>validated</b>     : ≥ 1 risque VALIDE (priorité absolue —
 *       l'avocat doit voir qu'il y a un risque confirmé qui pré-flagge
 *       l'outil).</li>
 *   <li><b>mixed</b>         : ≥ 1 VALIDE et ≥ 1 ECARTE (rare mais
 *       possible).</li>
 *   <li><b>discarded</b>     : 0 VALIDE, ≥ 1 ECARTE (l'outil a été
 *       potentiellement masqué par F-IA-04).</li>
 *   <li><b>to_explore</b>    : 0 VALIDE, 0 ECARTE, ≥ 1 A_CREUSER.</li>
 *   <li><b>none</b>          : aucun risque mappé à cet outil.</li>
 * </ol></p>
 *
 * <p>Cohérence DESIGN_SYSTEM.md : palette navy/or/gris. Le rouge plein est
 * réservé aux risques VALIDÉ critiques (keyword "harcèlement", "violence",
 * "expulsion", "dilapidation") — voir variante `validated_critical` exposée
 * via {@link CRITICAL_RISK_KEYWORDS}.</p>
 */
export type RisquesBadgeKind =
  | 'validated'
  | 'validated_critical'
  | 'mixed'
  | 'discarded'
  | 'to_explore'
  | 'none';

export interface RisquesBadgeCounts {
  /** Risques A_CREUSER (statut implicite par défaut tant que pas taggés). */
  aCreuser: number;
  /** Risques VALIDE. */
  valides: number;
  /** Risques ECARTE. */
  ecartes: number;
}

export interface RisquesBadge {
  kind: RisquesBadgeKind;
  counts: RisquesBadgeCounts;
}

export interface RisquesBadgeInput {
  risquesAlignment?: RisqueAlignment[] | null;
}

/**
 * Keywords critiques qui font basculer le kind d'un risque VALIDE en
 * `validated_critical` (pill rouge subtil — seul usage justifié dans F-195
 * cohérent DESIGN_SYSTEM.md). Liste restreinte, alignée backend mini-spec
 * SF-195-01 (mapping `RisqueToolMatcher`).
 */
export const CRITICAL_RISK_KEYWORDS: readonly string[] = [
  'harcèlement',
  'harcelement',
  'violence',
  'expulsion',
  'dilapidation',
  'oqtf',
] as const;

function isCriticalLibelle(libelle: string): boolean {
  const lower = (libelle ?? '').toLowerCase();
  return CRITICAL_RISK_KEYWORDS.some(kw => lower.includes(kw));
}

/**
 * Calcule le badge à partir de la liste filtrée d'alignements (déjà
 * restreinte au toolId courant par le panel via `inputs(ctx)`). Pure ;
 * sans effet de bord.
 *
 * Règles :
 * <ul>
 *   <li>Si la liste est vide → `kind = 'none'`, counts à 0.</li>
 *   <li>aCreuser = nb A_CREUSER, valides = nb VALIDE, ecartes = nb ECARTE.</li>
 *   <li>kind par priorité visuelle (cf. JSDoc supra).</li>
 *   <li>Si ≥ 1 VALIDE avec libellé critique → kind = `validated_critical`
 *       (rouge subtil — seul usage justifié hors palette navy/or).</li>
 * </ul>
 */
export function computeRisquesBadge(
  filtered: RisqueAlignment[],
): RisquesBadge {
  const counts: RisquesBadgeCounts = {
    aCreuser: filtered.filter(r => r.statut === 'A_CREUSER').length,
    valides: filtered.filter(r => r.statut === 'VALIDE').length,
    ecartes: filtered.filter(r => r.statut === 'ECARTE').length,
  };
  const total = counts.aCreuser + counts.valides + counts.ecartes;
  if (total === 0) {
    return { kind: 'none', counts };
  }
  if (counts.valides > 0) {
    const hasCritical = filtered.some(
      r => r.statut === 'VALIDE' && isCriticalLibelle(r.risqueLibelle),
    );
    if (hasCritical) return { kind: 'validated_critical', counts };
    if (counts.ecartes > 0) return { kind: 'mixed', counts };
    return { kind: 'validated', counts };
  }
  if (counts.ecartes > 0) return { kind: 'discarded', counts };
  return { kind: 'to_explore', counts };
}

/**
 * Variante avec filtre par `toolId` — utilisée quand la liste reçue est
 * globale (cas du tableau `risquesAlignment` non pré-filtré côté panel).
 */
export function getRisquesBadgeFor(
  input: RisquesBadgeInput,
  toolId: string,
): RisquesBadge {
  const all = Array.isArray(input.risquesAlignment) ? input.risquesAlignment : [];
  const filtered = all.filter(
    r => Array.isArray(r.toolIdsCibles) && r.toolIdsCibles.includes(toolId),
  );
  return computeRisquesBadge(filtered);
}

/**
 * Helper utilitaire : extrait les libellés des risques statut VALIDE pour un
 * outil donné. Consommé par les composants outils via
 * `@Input() risquesValides` pour adapter leur comportement (ex. pré-flag
 * "Risque validé" sur F-DT-12 quand "harcèlement" VALIDÉ).
 */
export function risquesValidesFor(
  alignment: RisqueAlignment[] | null | undefined,
  toolId: string,
): string[] {
  if (!Array.isArray(alignment)) return [];
  return alignment
    .filter(r => r.statut === 'VALIDE'
      && Array.isArray(r.toolIdsCibles)
      && r.toolIdsCibles.includes(toolId))
    .map(r => r.risqueLibelle);
}
