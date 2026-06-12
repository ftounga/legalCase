/**
 * F-265 / F-276 — Découpe **déterministe** d'un acte markdown en sections.
 *
 * <p>Extrait dans un module utilitaire pur (sans dépendance Angular) afin d'être
 * partagé sans cycle d'import : `conclusions-section` (co-rédaction F-265) **et**
 * `conclusion-document` (sommaire / navigation F-276) le consomment. La logique
 * historique (parsing des titres `##` / `###`) est strictement préservée ;
 * `conclusions-section.component.ts` ré-exporte ces symboles pour compatibilité.</p>
 */

/**
 * F-265 / SF-265-02 — Section de l'acte détectée par parsing markdown.
 *
 * @property title    texte du titre (sans les `#`), pour le libellé du menu
 * @property start    index de début du bloc dans le `content` (clé stable)
 * @property level    niveau du titre (2 pour `##`, 3 pour `###`) — F-276,
 *                    utilisé pour l'indentation du sommaire
 * @property markdown bloc markdown complet de la section (titre + corps,
 *                    jusqu'au titre suivant de niveau ≤ ou la fin du document)
 */
export interface ConclusionSection {
  title: string;
  start: number;
  level: number;
  markdown: string;
}

/**
 * F-265 / SF-265-02 — Découpe un markdown en sections délimitées par les titres
 * `##` / `###`. Déterministe, sans dépendance. Une section couvre du début de
 * son titre jusqu'au début du **titre suivant de niveau inférieur ou égal**
 * (un `###` enfant reste DANS la section `##` parente). Le préambule éventuel
 * avant le premier titre n'est pas une section régénérable (on cible les
 * moyens/parties titrés).
 */
export function parseMarkdownSections(content: string): ConclusionSection[] {
  if (!content) {
    return [];
  }
  const lines = content.split('\n');
  // Repère chaque titre : offset de début de ligne + niveau (2 ou 3).
  const headings: { start: number; level: number; title: string }[] = [];
  let offset = 0;
  for (const line of lines) {
    const match = /^(#{2,3})\s+(.*)$/.exec(line);
    if (match) {
      headings.push({
        start: offset,
        level: match[1].length,
        title: match[2].trim(),
      });
    }
    offset += line.length + 1; // +1 pour le '\n' retiré par split
  }
  const sections: ConclusionSection[] = [];
  for (let i = 0; i < headings.length; i++) {
    const h = headings[i];
    // Fin = début du prochain titre de niveau ≤ au courant, sinon fin du texte.
    let end = content.length;
    for (let j = i + 1; j < headings.length; j++) {
      if (headings[j].level <= h.level) {
        end = headings[j].start;
        break;
      }
    }
    sections.push({
      title: h.title,
      start: h.start,
      level: h.level,
      markdown: content.slice(h.start, end).replace(/\n+$/, ''),
    });
  }
  return sections;
}

/**
 * F-265 / SF-265-02 — Remplace **en place** le bloc `originalSection` par
 * `regenerated` dans `draft`. Round-trip markdown : seul ce bloc change, le
 * reste est byte-identique. Retourne `null` si le bloc original n'est pas
 * retrouvé tel quel (édition manuelle entre la sélection et la régénération) —
 * on n'effectue alors aucun remplacement hasardeux.
 */
export function replaceSectionInDraft(
  draft: string,
  originalSection: string,
  regenerated: string,
): string | null {
  const index = draft.indexOf(originalSection);
  if (index === -1) {
    return null;
  }
  return (
    draft.slice(0, index) +
    regenerated.replace(/\n+$/, '') +
    draft.slice(index + originalSection.length)
  );
}
