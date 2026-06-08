/**
 * F-259 / SF-259-01 — Heuristique partagée de détection des titres de section
 * dans le texte brut des conclusions générées.
 *
 * Source de vérité UNIQUE, importée à la fois par :
 *  - `DocxExportService.isSectionHeading` (export Word — convergence Word/écran) ;
 *  - `ConclusionParser` (rendu « document juridique » à l'écran).
 *
 * Une ligne est un en-tête de section si, une fois trimmée :
 *  - elle n'est pas vide et fait au plus 60 caractères ;
 *  - ses lettres (Unicode) sont toutes en MAJUSCULES ;
 *  - elle contient au moins une lettre.
 *
 * Robuste pour la structure produite par `CaseConclusionPromptBuilder`
 * (`POUR`, `CONTRE`, `FAITS ET PROCÉDURE`, `DISCUSSION`, `PAR CES MOTIFS`,
 * `INVENTAIRE DES PIÈCES`…).
 */
export function isSectionHeading(line: string): boolean {
  const trimmed = line.trim();
  if (trimmed.length === 0 || trimmed.length > 60) {
    return false;
  }
  // Garde uniquement les lettres (Unicode) pour comparer casse et présence.
  const letters = trimmed.replace(/[^\p{L}]/gu, '');
  if (letters.length === 0) {
    return false;
  }
  return letters === letters.toUpperCase() && letters !== letters.toLowerCase();
}
