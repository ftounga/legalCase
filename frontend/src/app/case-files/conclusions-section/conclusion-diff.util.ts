/**
 * F-280 / SF-280-01 — Diff lisible ligne-à-ligne entre deux versions de
 * conclusions.
 *
 * Algorithme : plus longue sous-séquence commune (LCS) sur les lignes, pur et
 * déterministe. Aucune dépendance externe (choix de cadrage SF-280-00 §4 :
 * pas de nouvelle dépendance npm — risque build/CI nul). Le niveau « ligne »
 * suffit pour des conclusions structurées en paragraphes (intra-ligne hors
 * périmètre V1).
 *
 * Lecture seule : ce module ne modifie jamais le content ni l'état serveur.
 */

/** Type d'une ligne dans le diff unifié. */
export type DiffLineType = 'unchanged' | 'added' | 'removed';

/** Une ligne du diff unifié : son type et son texte. */
export interface DiffLine {
  type: DiffLineType;
  text: string;
}

/** Compteurs de synthèse d'un diff. */
export interface DiffSummary {
  added: number;
  removed: number;
}

/**
 * Découpe un content en lignes pour le diff. `null`/`undefined` → document
 * vide. On normalise les fins de ligne CRLF → LF pour ne pas générer de faux
 * écarts entre deux versions saisies sur des plateformes différentes.
 */
function toLines(content: string | null | undefined): string[] {
  if (content == null || content === '') {
    return [];
  }
  return content.replace(/\r\n/g, '\n').split('\n');
}

/**
 * Calcule la matrice LCS (longueurs) entre deux tableaux de lignes.
 * `lcs[i][j]` = longueur de la LCS de `a[i..]` et `b[j..]`.
 */
function lcsLengths(a: string[], b: string[]): number[][] {
  const n = a.length;
  const m = b.length;
  // (n+1) x (m+1), initialisé à 0 (dernière ligne/colonne = cas de base).
  const lcs: number[][] = Array.from({ length: n + 1 }, () =>
    new Array<number>(m + 1).fill(0),
  );
  for (let i = n - 1; i >= 0; i--) {
    for (let j = m - 1; j >= 0; j--) {
      if (a[i] === b[j]) {
        lcs[i][j] = lcs[i + 1][j + 1] + 1;
      } else {
        lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
      }
    }
  }
  return lcs;
}

/**
 * Diff unifié ligne-à-ligne entre `base` (avant) et `target` (après).
 *
 * - ligne présente dans les deux → `unchanged`
 * - ligne présente seulement dans `target` → `added`
 * - ligne présente seulement dans `base` → `removed`
 *
 * Ordre de sortie : ordre de lecture du document. À iso-position, une
 * suppression (issue de `base`) précède un ajout (issu de `target`), ce qui
 * rend une modification de ligne comme « − ancienne / + nouvelle ».
 */
export function diffLines(
  base: string | null | undefined,
  target: string | null | undefined,
): DiffLine[] {
  const a = toLines(base);
  const b = toLines(target);
  const lcs = lcsLengths(a, b);

  const result: DiffLine[] = [];
  let i = 0;
  let j = 0;
  while (i < a.length && j < b.length) {
    if (a[i] === b[j]) {
      result.push({ type: 'unchanged', text: a[i] });
      i++;
      j++;
    } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
      result.push({ type: 'removed', text: a[i] });
      i++;
    } else {
      result.push({ type: 'added', text: b[j] });
      j++;
    }
  }
  while (i < a.length) {
    result.push({ type: 'removed', text: a[i] });
    i++;
  }
  while (j < b.length) {
    result.push({ type: 'added', text: b[j] });
    j++;
  }
  return result;
}

/** Compte les ajouts et suppressions d'un diff. */
export function summarizeDiff(lines: DiffLine[]): DiffSummary {
  let added = 0;
  let removed = 0;
  for (const line of lines) {
    if (line.type === 'added') {
      added++;
    } else if (line.type === 'removed') {
      removed++;
    }
  }
  return { added, removed };
}
