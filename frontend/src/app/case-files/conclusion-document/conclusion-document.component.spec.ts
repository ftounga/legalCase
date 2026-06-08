import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConclusionDocumentComponent } from './conclusion-document.component';

/**
 * F-259 / SF-259-02 — Le composant rend le **Markdown** du contenu (titres,
 * gras, italique, listes, citations, filets) en HTML stylisé, et plus aucun
 * marqueur Markdown littéral (`#`, `**`, `*`, `-`, `>`, `---`).
 */
describe('ConclusionDocumentComponent', () => {
  let component: ConclusionDocumentComponent;
  let fixture: ComponentFixture<ConclusionDocumentComponent>;

  const SHEET = '[data-testid="conclusions-content"]';
  const CONTENT = '[data-testid="cd-content"]';
  const FOOTER = '[data-testid="cd-footer"]';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConclusionDocumentComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ConclusionDocumentComponent);
    component = fixture.componentInstance;
  });

  function setContent(content: string | null | undefined): void {
    component.content = content;
    fixture.detectChanges();
  }

  function sheet(): HTMLElement | null {
    return fixture.nativeElement.querySelector(SHEET);
  }

  function contentEl(): HTMLElement {
    return fixture.nativeElement.querySelector(CONTENT) as HTMLElement;
  }

  // ── État vide ──────────────────────────────────────────────────────────
  it('content null → ne rend rien (état vide géré par le parent)', () => {
    setContent(null);
    expect(sheet()).toBeNull();
  });

  it('content vide → ne rend rien', () => {
    setContent('   ');
    expect(sheet()).toBeNull();
  });

  // ── Feuille + testid + pied de page conservés ──────────────────────────
  it('rend la feuille avec data-testid="conclusions-content" (testid préservé)', () => {
    setContent('# Titre\n\nUn paragraphe.');
    const el = sheet();
    expect(el).not.toBeNull();
    expect(el!.tagName.toLowerCase()).toBe('article');
  });

  it('pied de page « Document de travail » présent quand il y a du contenu', () => {
    setContent('## Discussion\n\nArgument.');
    const footer = fixture.nativeElement.querySelector(FOOTER);
    expect(footer).not.toBeNull();
    expect(footer.textContent).toContain(
      'Document de travail — à vérifier par l\'avocat',
    );
  });

  // ── Titres : #/##/### → h1/h2/h3, sans marqueur littéral ───────────────
  it('# → <h1> sans « # » littéral', () => {
    setContent('# CONSEIL DE PRUD\'HOMMES DE PARIS');
    const h1 = contentEl().querySelector('h1');
    expect(h1).not.toBeNull();
    expect(h1!.textContent).toContain('CONSEIL DE PRUD\'HOMMES DE PARIS');
    expect(contentEl().textContent).not.toContain('#');
  });

  it('## → <h2> et ### → <h3>, sans « # » littéral', () => {
    setContent('## Section Introductive\n\n### Sous-section');
    expect(contentEl().querySelector('h2')).not.toBeNull();
    expect(contentEl().querySelector('h2')!.textContent).toContain(
      'Section Introductive',
    );
    expect(contentEl().querySelector('h3')).not.toBeNull();
    expect(contentEl().textContent).not.toContain('#');
  });

  // ── Gras / italique ────────────────────────────────────────────────────
  it('**gras** → <strong> sans astérisques littéraux', () => {
    setContent('**POUR** Madame Dupont');
    const strong = contentEl().querySelector('strong');
    expect(strong).not.toBeNull();
    expect(strong!.textContent).toBe('POUR');
    expect(contentEl().textContent).not.toContain('*');
  });

  it('*italique* → <em> sans astérisques littéraux', () => {
    setContent('Texte *en italique* ici.');
    const em = contentEl().querySelector('em');
    expect(em).not.toBeNull();
    expect(em!.textContent).toBe('en italique');
    expect(contentEl().textContent).not.toContain('*');
  });

  // ── Listes ─────────────────────────────────────────────────────────────
  it('liste « - » → <ul> avec <li>, sans tiret littéral', () => {
    setContent('- Premier point\n- Deuxième point');
    const ul = contentEl().querySelector('ul');
    expect(ul).not.toBeNull();
    expect(ul!.querySelectorAll('li').length).toBe(2);
    expect(ul!.textContent).not.toMatch(/^\s*-/);
  });

  it('liste « 1. » → <ol> avec <li>', () => {
    setContent('1. Dire le licenciement sans cause\n2. Condamner à 10 000 €');
    const ol = contentEl().querySelector('ol');
    expect(ol).not.toBeNull();
    expect(ol!.querySelectorAll('li').length).toBe(2);
  });

  // ── Citation / filet ───────────────────────────────────────────────────
  it('« > » → <blockquote>', () => {
    setContent('> Article L1235-3 du Code du travail.');
    const bq = contentEl().querySelector('blockquote');
    expect(bq).not.toBeNull();
    expect(bq!.textContent).toContain('Article L1235-3');
  });

  it('« --- » → <hr>', () => {
    setContent('Avant.\n\n---\n\nAprès.');
    expect(contentEl().querySelector('hr')).not.toBeNull();
  });

  // ── Souligné : <u> dans le contenu est préservé ────────────────────────
  it('<u> présent dans le contenu → souligné rendu (balise <u> conservée)', () => {
    setContent('Texte <u>souligné</u> ici.');
    const u = contentEl().querySelector('u');
    expect(u).not.toBeNull();
    expect(u!.textContent).toBe('souligné');
  });

  // ── Texte brut ─────────────────────────────────────────────────────────
  it('texte brut sans markdown → paragraphes <p> (pas de crash, pas de marqueur)', () => {
    setContent('Première ligne.\n\nDeuxième ligne.');
    expect(sheet()).not.toBeNull();
    const paragraphs = contentEl().querySelectorAll('p');
    expect(paragraphs.length).toBe(2);
    expect(paragraphs[0].textContent).toContain('Première ligne.');
  });

  // ── Sanitization Angular ───────────────────────────────────────────────
  it('un <script> injecté n\'est pas exécuté (sanitization Angular)', () => {
    setContent('Avant<script>window.__pwned = true;</script>Après');
    // Angular retire l'élément <script> du DOM rendu via [innerHTML].
    expect(contentEl().querySelector('script')).toBeNull();
    expect(
      (window as unknown as { __pwned?: boolean }).__pwned,
    ).toBeUndefined();
  });

  it('un handler inline (onerror) est neutralisé par la sanitization', () => {
    setContent('<img src="x" onerror="window.__xss = true">');
    const img = contentEl().querySelector('img');
    if (img) {
      expect(img.getAttribute('onerror')).toBeNull();
    }
    expect(
      (window as unknown as { __xss?: boolean }).__xss,
    ).toBeUndefined();
  });

  // ── Aucun branding ─────────────────────────────────────────────────────
  it('aucun branding LegalCase dans le document rendu', () => {
    setContent('## Discussion\n\nArgument.');
    expect(sheet()!.textContent).not.toMatch(/LegalCase/i);
  });
});
