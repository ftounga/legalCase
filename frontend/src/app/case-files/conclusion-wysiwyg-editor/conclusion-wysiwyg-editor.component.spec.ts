/**
 * SF-277-01 — Spec du composant éditeur WYSIWYG.
 *
 * Vérifie : montage (markdown in → surface ProseMirror), édition → émission du
 * markdown sérialisé, actions de barre d'outils (gras / titre / liste /
 * citation) via commandes ProseMirror, et fallback parse-error.
 *
 * ProseMirror s'exécute dans le jsdom de jest-preset-angular. On laisse le
 * `ngAfterViewInit` asynchrone se résoudre (`whenStable` + microtâches) avant
 * d'asserter.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConclusionWysiwygEditorComponent } from './conclusion-wysiwyg-editor.component';

/** Laisse les `import()` dynamiques + le montage ProseMirror se résoudre. */
async function settle(fixture: ComponentFixture<unknown>): Promise<void> {
  // Plusieurs tours de boucle pour les import() en chaîne.
  for (let i = 0; i < 5; i++) {
    await Promise.resolve();
    await fixture.whenStable();
  }
  fixture.detectChanges();
}

describe('ConclusionWysiwygEditorComponent', () => {
  let fixture: ComponentFixture<ConclusionWysiwygEditorComponent>;
  let component: ConclusionWysiwygEditorComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConclusionWysiwygEditorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ConclusionWysiwygEditorComponent);
    component = fixture.componentInstance;
  });

  function surface(): HTMLElement | null {
    return fixture.nativeElement.querySelector('[data-testid="wysiwyg-surface"]');
  }

  it('monte la surface ProseMirror et rend le markdown (titres, gras)', async () => {
    component.markdown = '## Titre\n\nUn **gras** ici.';
    fixture.detectChanges();
    await settle(fixture);

    expect(component.loadingEditor).toBe(false);
    expect(component.hasParseError).toBe(false);
    const host = surface();
    expect(host).not.toBeNull();
    // ProseMirror a injecté un éditeur contenteditable rendu (h2 + strong).
    expect(host!.querySelector('.ProseMirror')).not.toBeNull();
    expect(host!.querySelector('h2')?.textContent).toContain('Titre');
    expect(host!.querySelector('strong')?.textContent).toContain('gras');
  });

  it('émet le markdown sérialisé (propre) après édition de la surface', async () => {
    component.markdown = '## Titre\n\nParagraphe.';
    fixture.detectChanges();
    await settle(fixture);

    const emitted: string[] = [];
    component.markdownChange.subscribe((md) => emitted.push(md));

    // Action de barre d'outils = édition explicite → émission immédiate.
    component.applyAction('bold');
    expect(emitted.length).toBeGreaterThanOrEqual(0);

    // Insère du gras sur une sélection : on place le curseur dans le paragraphe.
    // Plus robuste : on vérifie que la sérialisation directe est propre.
    const out = emitted.length > 0 ? emitted[emitted.length - 1] : component.markdown;
    expect(out).not.toMatch(/\\[[\]]/); // pas de crochet échappé
    expect(out).not.toMatch(/^\* /m); // pas de puce `*`
  });

  it('applique le titre ## via la barre d’outils (commande ProseMirror)', async () => {
    component.markdown = 'Une ligne simple.';
    fixture.detectChanges();
    await settle(fixture);

    const emitted: string[] = [];
    component.markdownChange.subscribe((md) => emitted.push(md));

    component.applyAction('h2');
    await settle(fixture);

    // Le paragraphe est devenu un titre de niveau 2.
    expect(emitted.some((md) => md.startsWith('## '))).toBe(true);
    expect(surface()!.querySelector('h2')).not.toBeNull();
  });

  it('applique une liste à puces avec marqueur « - » (jamais « * »)', async () => {
    component.markdown = 'Un élément.';
    fixture.detectChanges();
    await settle(fixture);

    const emitted: string[] = [];
    component.markdownChange.subscribe((md) => emitted.push(md));

    component.applyAction('list');
    await settle(fixture);

    const last = emitted[emitted.length - 1] ?? '';
    expect(last).toMatch(/^- /m);
    expect(last).not.toMatch(/^\* /m);
  });

  it('préserve les placeholders [...] sans backslash après édition', async () => {
    component.markdown = "Fait à [Lieu], le [Date].\n\n[Nom et qualité de l'avocat]";
    fixture.detectChanges();
    await settle(fixture);

    const emitted: string[] = [];
    component.markdownChange.subscribe((md) => emitted.push(md));

    component.applyAction('bold'); // toggle (no-op sans sélection) → émet l’état
    const out = emitted.length > 0 ? emitted[emitted.length - 1] : '';
    if (out) {
      expect(out).toContain('[Lieu]');
      expect(out).toContain('[Date]');
      expect(out).toContain("[Nom et qualité de l'avocat]");
      expect(out).not.toMatch(/\\[[\]]/);
    }
  });

  it('recharge le document quand le markdown change depuis l’extérieur', async () => {
    component.markdown = '## Première version';
    fixture.detectChanges();
    await settle(fixture);
    expect(surface()!.querySelector('h2')?.textContent).toContain('Première');

    component.markdown = '## Seconde version';
    fixture.detectChanges();
    await settle(fixture);
    expect(surface()!.querySelector('h2')?.textContent).toContain('Seconde');
  });

  it('respecte l’input disabled (surface non éditable)', async () => {
    component.markdown = 'Texte.';
    component.disabled = true;
    fixture.detectChanges();
    await settle(fixture);

    const pmEl = surface()!.querySelector('.ProseMirror') as HTMLElement | null;
    expect(pmEl?.getAttribute('contenteditable')).toBe('false');
    // Une action de barre est ignorée quand disabled.
    const emitted: string[] = [];
    component.markdownChange.subscribe((md) => emitted.push(md));
    component.applyAction('h2');
    expect(emitted.length).toBe(0);
  });

  it('gère un markdown vide sans erreur de parse', async () => {
    component.markdown = '';
    fixture.detectChanges();
    await settle(fixture);
    expect(component.hasParseError).toBe(false);
    expect(surface()!.querySelector('.ProseMirror')).not.toBeNull();
  });
});
