import {
  ChangeDetectionStrategy,
  Component,
  Input,
  computed,
  signal,
} from '@angular/core';
import { marked } from 'marked';

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

  /** Contenu Markdown des conclusions à rendre (mode lecture). */
  @Input()
  set content(value: string | null | undefined) {
    this._content.set(value);
  }
  get content(): string | null | undefined {
    return this._content();
  }

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
    const parsed = marked.parse(content, { async: false }) as string;
    return parsed.trim();
  });

  /** Vrai s'il y a quelque chose à rendre. */
  readonly hasContent = computed<boolean>(() => this.html().length > 0);
}
