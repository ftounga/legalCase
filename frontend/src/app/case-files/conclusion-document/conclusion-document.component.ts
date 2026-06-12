import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  computed,
  inject,
  signal,
} from '@angular/core';
import { marked } from 'marked';
import {
  type ConclusionSection,
  parseMarkdownSections,
} from '../conclusions-section/conclusion-sections.util';

/**
 * F-276 — Sélecteur des titres de section rendus dans la feuille. `marked`
 * produit `##` → `<h2>` et `###` → `<h3>` **dans l'ordre du markdown**, le même
 * ordre que `parseMarkdownSections`. La i-ème entrée du sommaire correspond donc
 * au i-ème de ces titres dans le DOM — on cible la cible du saut **par index**
 * (et non par `id`, qu'Angular retire à la sanitization du `[innerHTML]`).
 */
export const SECTION_HEADING_SELECTOR = '.cd-content h2, .cd-content h3';

/**
 * F-276 — Entrée du sommaire : libellé (titre de section), niveau (2/3, pour
 * l'indentation) et `index` du titre cible dans l'ordre du document.
 */
export interface SummaryEntry {
  title: string;
  level: number;
  index: number;
}

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
  /** Hôte du composant — sert à retrouver les titres rendus pour le saut F-276. */
  private readonly host: ElementRef<HTMLElement> = inject(ElementRef);
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
   * F-276 — Sections de l'acte (titres `##`/`###`) par parsing **déterministe**
   * et partagé (`parseMarkdownSections`, même source que la co-rédaction F-265).
   * Sert à la fois à injecter les ancres dans le HTML et à bâtir le sommaire.
   */
  private readonly sections = computed<ConclusionSection[]>(() =>
    parseMarkdownSections(this._content() ?? ''),
  );

  /**
   * HTML issu du parsing Markdown (`marked`, mode synchrone). Rendu via
   * `[innerHTML]` → Angular sanitize la sortie (suppression des `<script>`,
   * handlers inline, etc. ; l'attribut `id` est conservé). Vide ⇒ rien n'est
   * rendu (état vide géré par le parent).
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

  /**
   * F-276 — Entrées du sommaire cliquable, dans l'ordre du document. **Affiché
   * seulement si l'acte a au moins 2 sections** (sinon la navigation n'apporte
   * rien — invariant anti-surcharge 0bis) : en deçà, liste vide ⇒ le bloc
   * sommaire n'est pas rendu. L'`index` désigne le i-ème titre `<h2>`/`<h3>`
   * rendu, cible du saut.
   */
  readonly summary = computed<SummaryEntry[]>(() => {
    const secs = this.sections();
    if (secs.length < 2) {
      return [];
    }
    return secs.map((s, i) => ({
      title: s.title,
      level: s.level,
      index: i,
    }));
  });

  /**
   * F-276 — Fait défiler la feuille jusqu'au i-ème titre de section rendu.
   *
   * <p>On cible le titre **par index** dans le DOM (et non par `id`, qu'Angular
   * retire à la sanitization du `[innerHTML]`). L'ordre des titres rendus par
   * `marked` est celui de `parseMarkdownSections`, donc l'index du sommaire
   * pointe le bon titre. Suit la convention produit (`scrollIntoView`
   * smooth/start ; cf. `case-file-detail`, `synthesis`). Sans effet si le titre
   * est absent (jamais d'erreur). Saut intra-page : aucun changement d'URL.</p>
   */
  jumpTo(index: number): void {
    const headings = this.host.nativeElement.querySelectorAll<HTMLElement>(
      SECTION_HEADING_SELECTOR,
    );
    headings.item(index)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
