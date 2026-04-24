import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Wraps French / Belgian legal article citations in `<code>` tags so they render with
 * JetBrains Mono (design-system convention for legal references).
 *
 * Recognised patterns (case-insensitive, anchored on typical prefixes):
 *   - `Art. L.1235-3-1`, `art. R.776-18`, `art. L.614-5 al. 2 CESEDA`
 *   - `L.3245-1`, `R.1234-2`, `D.3121-28`
 *   - `Loi 15/12/1980`, `Loi 15.12.1980`, `Loi du 15 décembre 1980`
 *   - `art. 39/82 §4`
 *
 * Used by the 6 decisional section components (F-DT-11/15/19, F-IM-08) — F-155 harmonisation.
 */
@Pipe({ name: 'legalCitations', standalone: true })
export class LegalCitationsPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  // Regex matches the common French / Belgian legal references.
  // Ordered from most specific to least to avoid over-eager matches.
  private static readonly PATTERNS: RegExp[] = [
    // "Art. L.614-5 al. 2 CESEDA" / "art. R.776-18 CJA" / "Art. L.1235-3-1 Code du travail"
    /\bart\.?\s?[LRDA]\.?\s?\d+(?:[-.]\d+)*(?:\s?al\.?\s?\d+)?(?:\s?§\s?\d+)?(?:\s(?:CESEDA|CJA|CGI|Code\sdu\stravail|Code\scivil|Cciv|C\.civ\.|Ccom|C\.com\.))?/gi,
    // "art. 39/82 §4 Loi 15/12/1980"
    /\bart\.?\s?\d+\/\d+(?:\s?§\s?\d+)?(?:\s?Loi\s\d{1,2}[\/\.]\d{1,2}[\/\.]\d{4})?/gi,
    // "L.1235-3-1 al. 2" / "R.776-18" / "L.3245-1" without "art." prefix
    /\b[LRDA]\.\s?\d+(?:-\d+)+(?:\s?al\.?\s?\d+)?/g,
    // "Loi 15/12/1980", "Loi 15.12.1980", "Loi du 15 décembre 1980"
    /\bLoi\s(?:du\s\d{1,2}\s[a-zéû]+\s\d{4}|\d{1,2}[\/\.]\d{1,2}[\/\.]\d{4})/gi,
  ];

  transform(value: string | null | undefined): SafeHtml {
    if (!value) return '';
    const escaped = this.escapeHtml(value);

    // Collecte TOUS les matches de tous les patterns avant d'en wrapper aucun —
    // évite qu'un pattern suivant matche à l'intérieur d'un <code> déjà injecté
    // (bug rencontré sur "art. L.614-5 al. 2 CESEDA" où le pattern bare "L.xxx"
    // re-wrappait l'intérieur d'une balise produite par le pattern "art. … CESEDA").
    type Match = { start: number; end: number };
    const matches: Match[] = [];
    for (const pattern of LegalCitationsPipe.PATTERNS) {
      pattern.lastIndex = 0;
      let m: RegExpExecArray | null;
      while ((m = pattern.exec(escaped)) !== null) {
        matches.push({ start: m.index, end: m.index + m[0].length });
        if (m.index === pattern.lastIndex) pattern.lastIndex++;
      }
    }

    // Garder uniquement les matches qui ne sont pas contenus dans un autre.
    // En cas de chevauchement, le plus long (plus spécifique) gagne.
    matches.sort((a, b) => a.start - b.start || (b.end - b.start) - (a.end - a.start));
    const kept: Match[] = [];
    let lastEnd = -1;
    for (const match of matches) {
      if (match.start < lastEnd) continue; // contenu dans un précédent
      kept.push(match);
      lastEnd = match.end;
    }

    // Reconstruit la chaîne en wrappant les matches retenus.
    let out = '';
    let cursor = 0;
    for (const { start, end } of kept) {
      out += escaped.slice(cursor, start) + '<code>' + escaped.slice(start, end) + '</code>';
      cursor = end;
    }
    out += escaped.slice(cursor);
    return this.sanitizer.bypassSecurityTrustHtml(out);
  }

  private escapeHtml(s: string): string {
    return s
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }
}
