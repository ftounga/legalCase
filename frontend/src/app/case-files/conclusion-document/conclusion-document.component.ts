import {
  ChangeDetectionStrategy,
  Component,
  Input,
  computed,
  signal,
} from '@angular/core';
import { marked } from 'marked';

/**
 * F-266 / SF-266-01 — Référence minimale d'une pièce numérotée du dossier,
 * utilisée pour la traçabilité fait → pièce au survol. `number` = numéro
 * persistant (F-260) ; `label` / `typeLabel` alimentent l'info-bulle.
 */
export interface PieceRef {
  number: number;
  label: string | null;
  typeLabel: string;
}

/**
 * F-266 / SF-266-01 — Échappe les caractères dangereux d'une chaîne destinée à
 * un **attribut HTML** (`title="…"`). Empêche toute injection via le libellé de
 * pièce (qui provient de données utilisateur). Pur, testable.
 */
export function escapeHtmlAttribute(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

/**
 * F-266 / SF-266-01 — Décore, dans le **HTML rendu** des conclusions, chaque
 * renvoi « Pièce n° X » dont le numéro `X` correspond à une pièce **connue**
 * d'une info-bulle (`title`) révélant le libellé et le type de la pièce.
 *
 * <p>Ancrage **déterministe** : seul le motif « Pièce n° X » (X entier, espaces
 * tolérés) est reconnu ; un numéro sans pièce correspondante est laissé
 * **intact** (jamais d'info inventée). Le `content` markdown n'est jamais
 * modifié — seule la sortie HTML de l'aperçu est décorée → export Word/PDF et
 * versions restent inchangés (markdown-safe par construction).</p>
 *
 * <p>Sécurité : on n'opère que sur le texte des renvois reconnus (on insère un
 * `<span>` autour du libellé existant), et le `title` est échappé. La
 * réécriture évite de toucher l'intérieur des balises HTML (on ne remplace que
 * hors `<...>`).</p>
 */
export function annotatePieceReferences(
  html: string,
  piecesByNumber: ReadonlyMap<number, PieceRef>,
): string {
  if (!html || piecesByNumber.size === 0) {
    return html;
  }
  // « Pièce n° 4 » : « Pièce » + espaces + « n° » (ou « n ° » / « no ») + espaces + chiffres.
  // On ne remplace que dans les segments de TEXTE (hors balises) pour ne pas
  // corrompre les attributs HTML.
  const refPattern = /Pièce\s+n[°o]\s*(\d+)/gi;
  const segments = html.split(/(<[^>]+>)/);
  for (let i = 0; i < segments.length; i++) {
    const seg = segments[i];
    // Les segments impairs sont des balises (`<...>`) — on les laisse intacts.
    if (seg.startsWith('<')) {
      continue;
    }
    segments[i] = seg.replace(refPattern, (match, digits: string) => {
      const piece = piecesByNumber.get(Number(digits));
      if (!piece) {
        return match; // numéro inconnu → aucune décoration
      }
      const labelPart = piece.label && piece.label.trim().length > 0
        ? `${piece.typeLabel} — ${piece.label.trim()}`
        : piece.typeLabel;
      const title = escapeHtmlAttribute(labelPart);
      return `<span class="cd-piece-ref" title="${title}" data-testid="piece-ref">${match}</span>`;
    });
  }
  return segments.join('');
}

/**
 * F-259 / SF-259-02 — Rendu Markdown « document juridique » des conclusions.
 *
 * Le contenu généré par l'IA est en **Markdown** (`#`/`##`/`###` titres,
 * `**gras**`, `*italique*`, listes `-`/`1.`, `>` citations, `---` filets). En
 * mode LECTURE, on le parse avec `marked` → HTML, rendu via `[innerHTML]`
 * **sanitizé par Angular** (sécurité par défaut du binding : tout `<script>`
 * / handler inline est neutralisé) sur une « feuille » blanche. Le style riche
 * (titres Merriweather marine + filet or, gras marine, italique, souligné,
 * listes accent or, citations, filets) vient exclusivement du SCSS du composant
 * scopé sous `.cd-sheet` via `::ng-deep`, jamais du contenu.
 *
 * Aucun appel réseau, aucun effet de bord : composant de présentation pur,
 * OnPush. Le `data-testid="conclusions-content"` historique est porté par la
 * feuille pour préserver les specs du parent (`conclusions-section`).
 *
 * NB : SF-259-03 a aligné l'export Word/PDF sur le même Markdown (parsing
 * `marked.lexer` → `docx`/`pdfmake`), supprimant l'ancienne heuristique
 * MAJUSCULES (`core/utils/section-heading`, retirée).
 */
@Component({
  selector: 'app-conclusion-document',
  standalone: true,
  imports: [],
  templateUrl: './conclusion-document.component.html',
  styleUrl: './conclusion-document.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConclusionDocumentComponent {
  private readonly _content = signal<string | null | undefined>(null);
  private readonly _pieces = signal<readonly PieceRef[]>([]);

  /** Contenu Markdown des conclusions à rendre (mode lecture). */
  @Input()
  set content(value: string | null | undefined) {
    this._content.set(value);
  }
  get content(): string | null | undefined {
    return this._content();
  }

  /**
   * F-266 / SF-266-01 — Pièces numérotées du dossier (numéro persistant F-260).
   * Quand fournies, les renvois « Pièce n° X » du texte rendu sont décorés
   * d'une info-bulle (libellé + type) au survol. Absentes ⇒ aucun changement
   * de rendu (dégradation propre).
   */
  @Input()
  set pieces(value: readonly PieceRef[] | null | undefined) {
    this._pieces.set(value ?? []);
  }
  get pieces(): readonly PieceRef[] {
    return this._pieces();
  }

  /** Index {numéro → pièce} pour la décoration des renvois (déterministe). */
  private readonly piecesByNumber = computed<ReadonlyMap<number, PieceRef>>(() => {
    const map = new Map<number, PieceRef>();
    for (const piece of this._pieces()) {
      if (piece != null && Number.isInteger(piece.number)) {
        map.set(piece.number, piece);
      }
    }
    return map;
  });

  /**
   * HTML issu du parsing Markdown (`marked`, mode synchrone). Rendu via
   * `[innerHTML]` → Angular sanitize la sortie (suppression des `<script>`,
   * handlers inline, etc.). Vide ⇒ rien n'est rendu (état vide géré par le
   * parent).
   */
  readonly html = computed<string>(() => {
    const content = this._content();
    if (content == null || content.trim().length === 0) {
      return '';
    }
    // `marked.parse` est synchrone par défaut (pas d'extension async ici) ;
    // on borne le type au cas string.
    const parsed = (marked.parse(content, { async: false }) as string).trim();
    // F-266 / SF-266-01 — décoration fait → pièce (no-op si aucune pièce).
    return annotatePieceReferences(parsed, this.piecesByNumber());
  });

  /** Vrai s'il y a quelque chose à rendre. */
  readonly hasContent = computed<boolean>(() => this.html().length > 0);
}
