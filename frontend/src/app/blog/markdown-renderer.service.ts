import { Injectable } from '@angular/core';

/**
 * Convertisseur markdown → HTML simple, sécurisé : ne génère que des balises
 * statiques (h2, h3, p, strong, em, a, ul, ol, li, code) et échappe le HTML
 * brut entrant (`<`, `>`, `&`).
 *
 * <p>Pas de support des constructions complexes (tables, footnotes, code blocks
 * triple-backtick, embeds), suffisant pour le rendu d'articles de blog
 * générés par SF-120-03.
 */
@Injectable({ providedIn: 'root' })
export class MarkdownRendererService {

  render(markdown: string): string {
    if (!markdown || markdown.trim().length === 0) {
      return '';
    }

    const escaped = this.escapeHtml(markdown);
    const lines = escaped.split('\n');
    const out: string[] = [];
    let i = 0;

    while (i < lines.length) {
      const line = lines[i].trimEnd();

      if (line.length === 0) {
        i++;
        continue;
      }

      if (line.startsWith('### ')) {
        out.push(`<h3>${this.renderInline(line.slice(4))}</h3>`);
        i++;
        continue;
      }

      if (line.startsWith('## ')) {
        out.push(`<h2>${this.renderInline(line.slice(3))}</h2>`);
        i++;
        continue;
      }

      if (line.startsWith('# ')) {
        out.push(`<h1>${this.renderInline(line.slice(2))}</h1>`);
        i++;
        continue;
      }

      if (this.isUnorderedListLine(line)) {
        const items: string[] = [];
        while (i < lines.length && this.isUnorderedListLine(lines[i].trimEnd())) {
          items.push(`<li>${this.renderInline(lines[i].trimEnd().replace(/^[-*]\s+/, ''))}</li>`);
          i++;
        }
        out.push(`<ul>${items.join('')}</ul>`);
        continue;
      }

      if (this.isOrderedListLine(line)) {
        const items: string[] = [];
        while (i < lines.length && this.isOrderedListLine(lines[i].trimEnd())) {
          items.push(`<li>${this.renderInline(lines[i].trimEnd().replace(/^\d+\.\s+/, ''))}</li>`);
          i++;
        }
        out.push(`<ol>${items.join('')}</ol>`);
        continue;
      }

      // paragraphe : agrège les lignes consécutives non vides
      const paragraphLines: string[] = [line];
      i++;
      while (i < lines.length) {
        const next = lines[i].trimEnd();
        if (next.length === 0 || next.startsWith('#') || this.isUnorderedListLine(next)
                || this.isOrderedListLine(next)) {
          break;
        }
        paragraphLines.push(next);
        i++;
      }
      out.push(`<p>${this.renderInline(paragraphLines.join(' '))}</p>`);
    }

    return out.join('\n');
  }

  private isUnorderedListLine(line: string): boolean {
    return /^[-*]\s+/.test(line);
  }

  private isOrderedListLine(line: string): boolean {
    return /^\d+\.\s+/.test(line);
  }

  private renderInline(text: string): string {
    // Bold **x** → <strong>x</strong>
    let out = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    // Italic *x* → <em>x</em>
    out = out.replace(/\*([^*]+)\*/g, '<em>$1</em>');
    // Inline code `x` → <code>x</code>
    out = out.replace(/`([^`]+)`/g, '<code>$1</code>');
    // Link [text](url) — url échappée et limitée à http(s):// / mailto: / chemins relatifs commençant par /
    out = out.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, label, url) => {
      const safe = this.sanitizeUrl(url);
      return `<a href="${safe}" rel="noopener">${label}</a>`;
    });
    return out;
  }

  private sanitizeUrl(url: string): string {
    const trimmed = url.trim();
    if (trimmed.startsWith('http://') || trimmed.startsWith('https://')
            || trimmed.startsWith('mailto:') || trimmed.startsWith('/')) {
      return trimmed;
    }
    return '#';
  }

  private escapeHtml(input: string): string {
    return input
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }
}
