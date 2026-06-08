import { isSectionHeading } from '../../core/utils/section-heading';

/**
 * F-259 / SF-259-01 — Une section de conclusions parsée depuis le texte brut.
 *
 * `title` vaut `null` pour le bloc de texte qui précède le 1er en-tête de
 * section (préambule éventuel) ; sinon c'est la ligne d'en-tête MAJUSCULES.
 * `lines` contient les lignes de corps de la section (lignes vides comprises,
 * pour préserver les sauts de paragraphe), titre exclu.
 */
export interface ConclusionSection {
  /** En-tête de section (MAJUSCULES) ou `null` pour le préambule. */
  title: string | null;
  /** Lignes de corps de la section (titre exclu). */
  lines: string[];
}

/**
 * F-259 / SF-259-01 — Découpe le texte brut des conclusions en sections.
 *
 * Réutilise l'heuristique partagée `isSectionHeading` (même logique que
 * l'export Word, `DocxExportService`) : une ligne entièrement en MAJUSCULES,
 * courte et porteuse d'au moins une lettre ouvre une nouvelle section. Le
 * texte précédant le 1er titre forme une section `title: null`.
 *
 * Cas limites :
 *  - `content` vide / `null` / `undefined` → `[]` (le parent gère l'état vide) ;
 *  - aucun titre détecté → une seule section `title: null` contenant tout le
 *    corps (fallback paragraphes) ;
 *  - une section sans aucune ligne de corps (titre suivi d'un autre titre) est
 *    conservée avec `lines: []`.
 */
export function parseConclusion(content: string | null | undefined): ConclusionSection[] {
  if (content == null || content.trim().length === 0) {
    return [];
  }

  const sections: ConclusionSection[] = [];
  let current: ConclusionSection | null = null;

  for (const line of content.split('\n')) {
    if (isSectionHeading(line)) {
      current = { title: line.trim(), lines: [] };
      sections.push(current);
    } else {
      if (current === null) {
        // Préambule : texte avant le 1er en-tête de section.
        current = { title: null, lines: [] };
        sections.push(current);
      }
      current.lines.push(line);
    }
  }

  return sections;
}

/**
 * F-259 / SF-259-01 — Vrai si la section est le dispositif « PAR CES MOTIFS »
 * (titre normalisé sans accents/ponctuation, casse ignorée).
 */
export function isDispositifSection(section: ConclusionSection): boolean {
  if (section.title === null) {
    return false;
  }
  const normalized = section.title
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-zA-Z]/g, '')
    .toUpperCase();
  return normalized === 'PARCESMOTIFS';
}

/**
 * F-259 / SF-259-01 — Découpe les lignes d'une section en paragraphes :
 * une ou plusieurs lignes vides successives séparent deux paragraphes ; les
 * lignes non vides d'un même bloc sont jointes par un espace.
 */
export function toParagraphs(lines: string[]): string[] {
  const paragraphs: string[] = [];
  let buffer: string[] = [];

  const flush = () => {
    if (buffer.length > 0) {
      paragraphs.push(buffer.join(' '));
      buffer = [];
    }
  };

  for (const line of lines) {
    if (line.trim().length === 0) {
      flush();
    } else {
      buffer.push(line.trim());
    }
  }
  flush();

  return paragraphs;
}

/**
 * F-259 / SF-259-01 — Items du dispositif « PAR CES MOTIFS ».
 *
 * Chaque ligne non vide de la section devient un item de liste numérotée. Une
 * éventuelle puce / numérotation source de tête (« 1. », « - », « • »…) est
 * retirée pour laisser la liste HTML gérer la numérotation.
 */
export function toDispositifItems(lines: string[]): string[] {
  return lines
    .map((l) => l.trim())
    .filter((l) => l.length > 0)
    .map((l) => l.replace(/^(?:[-–•*]\s+|\d+[.)]\s+)/, '').trim());
}
