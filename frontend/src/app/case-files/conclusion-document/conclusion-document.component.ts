import {
  ChangeDetectionStrategy,
  Component,
  Input,
  computed,
  signal,
} from '@angular/core';
import {
  ConclusionSection,
  isDispositifSection,
  parseConclusion,
  toDispositifItems,
  toParagraphs,
} from './conclusion-parser';

/**
 * F-259 / SF-259-01 — Rendu « document juridique » des conclusions générées.
 *
 * Remplace, en mode LECTURE uniquement, le `<pre>` de texte brut (« look
 * markdown/Claude ») par un document structuré : titres de section en
 * Merriweather marine soulignés d'un filet or, corps Inter justifié sur une
 * « feuille » blanche, dispositif « PAR CES MOTIFS » en liste numérotée.
 *
 * Parsing par l'heuristique MAJUSCULES partagée avec l'export Word
 * (`core/utils/section-heading`) → convergence écran / Word. Aucun appel
 * réseau, aucun effet de bord : composant de présentation pur, OnPush.
 *
 * Le `data-testid="conclusions-content"` historique est porté par la feuille
 * pour préserver les specs du parent (`conclusions-section`).
 */
interface RenderSection {
  readonly title: string | null;
  readonly paragraphs: string[];
  readonly dispositifItems: string[] | null;
}

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

  /** Texte brut des conclusions à rendre (mode lecture). */
  @Input()
  set content(value: string | null | undefined) {
    this._content.set(value);
  }
  get content(): string | null | undefined {
    return this._content();
  }

  /** Sections prêtes au rendu (vide ⇒ rien n'est rendu, état vide parent). */
  readonly sections = computed<RenderSection[]>(() =>
    parseConclusion(this._content()).map((section) => this.toRender(section)),
  );

  /** Vrai s'il y a quelque chose à rendre (au moins une section). */
  readonly hasContent = computed<boolean>(() => this.sections().length > 0);

  private toRender(section: ConclusionSection): RenderSection {
    if (isDispositifSection(section)) {
      const items = toDispositifItems(section.lines);
      // Si le dispositif n'a pas d'items exploitables, on retombe en
      // paragraphes normaux plutôt que de rendre une liste vide.
      if (items.length > 0) {
        return { title: section.title, paragraphs: [], dispositifItems: items };
      }
    }
    return {
      title: section.title,
      paragraphs: toParagraphs(section.lines),
      dispositifItems: null,
    };
  }
}
