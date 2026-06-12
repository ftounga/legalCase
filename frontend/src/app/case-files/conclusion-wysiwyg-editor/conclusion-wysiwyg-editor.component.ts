import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  ViewChild,
  inject,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import type { MarkType, NodeType, Schema } from 'prosemirror-model';
import type { Command, EditorState } from 'prosemirror-state';
import type { EditorView } from 'prosemirror-view';

import { docToMarkdown, markdownToDoc } from './conclusion-markdown-serializer';

/**
 * SF-277-01 — Actions de mise en forme de la barre d'outils WYSIWYG. Mêmes
 * gestes que la barre markdown F-264 (gras, italique, titres ##/###, liste,
 * citation) — câblés sur des commandes ProseMirror (cf. invariant écran : pas
 * d'inflation de boutons).
 */
export type WysiwygAction =
  | 'h2'
  | 'h3'
  | 'bold'
  | 'italic'
  | 'list'
  | 'quote';

/**
 * SF-277-01 — Délai de stabilisation (ms) avant d'émettre `markdownChange`.
 * Évite une sérialisation + un cycle de CD à chaque frappe ; assez court pour
 * que le pipeline aval (autosave F-279, diff F-280) reste réactif.
 */
const MARKDOWN_EMIT_DEBOUNCE_MS = 300;

/**
 * SF-277-01 — Surface d'édition **WYSIWYG** d'un acte de conclusions.
 *
 * <p>Monte un <code>EditorView</code> ProseMirror sur le schéma
 * <code>prosemirror-markdown</code>. L'acte est rendu mis en forme (titres,
 * gras, listes, citations) — <b>aucune syntaxe markdown visible</b> — mais le
 * <b>markdown reste la source de vérité</b> : <code>@Input() markdown</code>
 * alimente l'éditeur, et chaque édition ré-émet le markdown sérialisé via
 * <code>@Output() markdownChange</code> (débattu). Aucun changement de stockage
 * ni d'export en aval (cf. mini-spec SF-277-01).</p>
 *
 * <p><b>Lazy-load</b> : toutes les dépendances <code>prosemirror-*</code> sont
 * importées dynamiquement (<code>import()</code>) au montage de la vue → elles
 * ne sont pas dans le bundle <code>main</code> (invariant poids, étape 0 §7).</p>
 *
 * <p><b>Fallback</b> : si le markdown n'est pas parsable ou si la lib ne se
 * charge pas (réseau), <code>parseError</code> passe à vrai → l'hôte
 * (<code>conclusions-section</code>) réaffiche le textarea markdown brut (jamais
 * de perte de contenu).</p>
 *
 * <p>Standalone, OnPush. L'état ProseMirror vit hors du cycle Angular ; on
 * appelle <code>markForCheck()</code> après chaque mutation asynchrone et
 * <code>ChangeDetectorRef.detach()</code> n'est pas utilisé (la vue PM est un
 * nœud DOM imperatif).</p>
 */
@Component({
  selector: 'app-conclusion-wysiwyg-editor',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './conclusion-wysiwyg-editor.component.html',
  styleUrl: './conclusion-wysiwyg-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConclusionWysiwygEditorComponent
  implements AfterViewInit, OnDestroy
{
  private readonly cdr = inject(ChangeDetectorRef);

  /** Conteneur DOM où monter l'`EditorView` ProseMirror. */
  @ViewChild('host') hostRef?: ElementRef<HTMLDivElement>;

  /**
   * Markdown source de l'acte (source de vérité). Mis à jour par l'hôte ; un
   * changement **externe** (régénération F-265, restauration brouillon F-279)
   * recharge le document de l'éditeur. Une frappe interne n'est PAS réinjectée
   * (on ne ré-applique pas notre propre sortie → pas de boucle / saut de curseur).
   */
  @Input()
  set markdown(value: string) {
    const next = value ?? '';
    this._markdown = next;
    // Ne recharge le doc que si la valeur diffère de ce que l'éditeur a produit
    // en dernier (changement venu de l'extérieur), et seulement une fois monté.
    if (this.view && next !== this.lastEmitted) {
      this.loadMarkdown(next);
    }
  }
  get markdown(): string {
    return this._markdown;
  }
  private _markdown = '';

  /** Désactive l'édition (ex. pendant un enregistrement / une régénération). */
  @Input()
  set disabled(value: boolean) {
    this._disabled = value;
    this.view?.setProps({ editable: () => !value });
  }
  get disabled(): boolean {
    return this._disabled;
  }
  private _disabled = false;

  /** Émet le markdown sérialisé (débattu) à chaque édition de la surface. */
  @Output() readonly markdownChange = new EventEmitter<string>();

  /**
   * Émet `true` si le markdown n'est pas parsable ou si la lib ne se charge pas
   * → l'hôte bascule sur le textarea markdown brut (jamais de perte de contenu).
   */
  @Output() readonly parseError = new EventEmitter<boolean>();

  /** Vrai tant que la lib n'est pas chargée / l'éditeur pas monté. */
  loadingEditor = true;

  /** Vrai si le parse a échoué (l'hôte affiche le fallback textarea). */
  hasParseError = false;

  // ── État interne ProseMirror (hors cycle Angular) ────────────────────────
  private view?: EditorView;
  private schema?: Schema;
  /** Dernier markdown émis par l'éditeur (anti-boucle avec le setter `markdown`). */
  private lastEmitted = '';
  private emitTimer?: ReturnType<typeof setTimeout>;
  /** Modules ProseMirror lazy-loadés (figés après le 1er montage). */
  private pm?: {
    EditorState: typeof import('prosemirror-state').EditorState;
    EditorView: typeof import('prosemirror-view').EditorView;
    toggleMark: typeof import('prosemirror-commands').toggleMark;
    setBlockType: typeof import('prosemirror-commands').setBlockType;
    wrapIn: typeof import('prosemirror-commands').wrapIn;
    keymap: typeof import('prosemirror-keymap').keymap;
    baseKeymap: typeof import('prosemirror-commands').baseKeymap;
    history: typeof import('prosemirror-history').history;
    undo: typeof import('prosemirror-history').undo;
    redo: typeof import('prosemirror-history').redo;
    wrapInList: typeof import('prosemirror-schema-list').wrapInList;
    schema: Schema;
  };

  async ngAfterViewInit(): Promise<void> {
    try {
      // Lazy-load : toutes les deps ProseMirror hors bundle `main`.
      const [
        pmState,
        pmView,
        pmCommands,
        pmKeymap,
        pmHistory,
        pmMarkdown,
        pmListMod,
      ] = await Promise.all([
        import('prosemirror-state'),
        import('prosemirror-view'),
        import('prosemirror-commands'),
        import('prosemirror-keymap'),
        import('prosemirror-history'),
        import('prosemirror-markdown'),
        import('prosemirror-schema-list'),
      ]);
      this.pm = {
        EditorState: pmState.EditorState,
        EditorView: pmView.EditorView,
        toggleMark: pmCommands.toggleMark,
        setBlockType: pmCommands.setBlockType,
        wrapIn: pmCommands.wrapIn,
        keymap: pmKeymap.keymap,
        baseKeymap: pmCommands.baseKeymap,
        history: pmHistory.history,
        undo: pmHistory.undo,
        redo: pmHistory.redo,
        wrapInList: pmListMod.wrapInList,
        schema: pmMarkdown.schema,
      };
      this.schema = pmMarkdown.schema;
      this.mountEditor(this._markdown);
    } catch {
      // Lib non chargée (réseau) → fallback textarea côté hôte.
      this.fail();
    }
  }

  ngOnDestroy(): void {
    if (this.emitTimer) {
      clearTimeout(this.emitTimer);
    }
    this.view?.destroy();
  }

  /** Monte l'`EditorView` à partir d'un markdown (ou bascule en fallback). */
  private mountEditor(markdown: string): void {
    const host = this.hostRef?.nativeElement;
    const pm = this.pm;
    if (!host || !pm) {
      this.fail();
      return;
    }
    const state = this.buildState(markdown);
    if (!state) {
      this.fail();
      return;
    }
    this.view = new pm.EditorView(host, {
      state,
      editable: () => !this._disabled,
      dispatchTransaction: (tr) => {
        if (!this.view) {
          return;
        }
        const newState = this.view.state.apply(tr);
        this.view.updateState(newState);
        if (tr.docChanged) {
          this.scheduleEmit();
        }
      },
    });
    this.loadingEditor = false;
    this.hasParseError = false;
    this.parseError.emit(false);
    this.cdr.markForCheck();
  }

  /** Construit un `EditorState` (doc + plugins) à partir d'un markdown. */
  private buildState(markdown: string): EditorState | null {
    const pm = this.pm;
    if (!pm) {
      return null;
    }
    try {
      const doc = markdownToDoc(markdown);
      return pm.EditorState.create({
        doc,
        plugins: [
          pm.history(),
          pm.keymap({
            'Mod-z': pm.undo,
            'Mod-y': pm.redo,
            'Mod-Shift-z': pm.redo,
          }),
          pm.keymap(pm.baseKeymap),
        ],
      });
    } catch {
      return null;
    }
  }

  /** Recharge le document de l'éditeur depuis un markdown externe. */
  private loadMarkdown(markdown: string): void {
    const state = this.buildState(markdown);
    if (!state || !this.view) {
      this.fail();
      return;
    }
    this.view.updateState(state);
    this.hasParseError = false;
    this.parseError.emit(false);
    this.cdr.markForCheck();
  }

  /** Bascule en mode parse-error (l'hôte affiche le textarea markdown brut). */
  private fail(): void {
    this.loadingEditor = false;
    this.hasParseError = true;
    this.parseError.emit(true);
    this.cdr.markForCheck();
  }

  /** Programme l'émission débattue du markdown sérialisé. */
  private scheduleEmit(): void {
    if (this.emitTimer) {
      clearTimeout(this.emitTimer);
    }
    this.emitTimer = setTimeout(() => this.emitMarkdown(), MARKDOWN_EMIT_DEBOUNCE_MS);
  }

  /** Sérialise l'état courant et émet le markdown (si changé). */
  private emitMarkdown(): void {
    if (!this.view) {
      return;
    }
    const markdown = docToMarkdown(this.view.state.doc);
    if (markdown === this.lastEmitted) {
      return;
    }
    this.lastEmitted = markdown;
    this._markdown = markdown;
    this.markdownChange.emit(markdown);
  }

  /**
   * Applique une action de la barre d'outils via une commande ProseMirror, puis
   * redonne le focus à la surface. Sérialise immédiatement (l'avocat voit le
   * résultat tout de suite, le debounce ne concerne que la frappe).
   */
  applyAction(action: WysiwygAction): void {
    if (this._disabled || !this.view || !this.schema || !this.pm) {
      return;
    }
    const command = this.commandFor(action);
    if (!command) {
      return;
    }
    command(this.view.state, this.view.dispatch, this.view);
    this.view.focus();
    // Émission immédiate (le clic est une action explicite).
    if (this.emitTimer) {
      clearTimeout(this.emitTimer);
    }
    this.emitMarkdown();
  }

  /** Mappe une action de barre d'outils sur une commande ProseMirror. */
  private commandFor(action: WysiwygAction): Command | null {
    const schema = this.schema;
    const pm = this.pm;
    if (!schema || !pm) {
      return null;
    }
    const marks = schema.marks as Record<string, MarkType | undefined>;
    const nodes = schema.nodes as Record<string, NodeType | undefined>;
    switch (action) {
      case 'bold':
        return marks['strong'] ? pm.toggleMark(marks['strong']!) : null;
      case 'italic':
        return marks['em'] ? pm.toggleMark(marks['em']!) : null;
      case 'h2':
        return nodes['heading']
          ? pm.setBlockType(nodes['heading']!, { level: 2 })
          : null;
      case 'h3':
        return nodes['heading']
          ? pm.setBlockType(nodes['heading']!, { level: 3 })
          : null;
      case 'list':
        return nodes['bullet_list']
          ? pm.wrapInList(nodes['bullet_list']!)
          : null;
      case 'quote':
        return nodes['blockquote'] ? pm.wrapIn(nodes['blockquote']!) : null;
      default:
        return null;
    }
  }
}
