import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  ConclusionDocumentComponent,
  PieceRef,
  annotatePieceReferences,
  escapeHtmlAttribute,
} from './conclusion-document.component';

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

  // ── F-266 / SF-266-01 — traçabilité fait → pièce au survol ─────────────
  describe('F-266 SF-266-01 — annotatePieceReferences', () => {
    const map = (refs: PieceRef[]): ReadonlyMap<number, PieceRef> =>
      new Map(refs.map((r) => [r.number, r]));

    it('décore « Pièce n° X » quand la pièce est connue (title = type + libellé)', () => {
      const html = '<p>comme en atteste la Pièce n° 4.</p>';
      const out = annotatePieceReferences(
        html,
        map([{ number: 4, label: 'Janvier 2024', typeLabel: 'Bulletin de paie' }]),
      );
      expect(out).toContain('class="cd-piece-ref"');
      expect(out).toContain('title="Bulletin de paie — Janvier 2024"');
      expect(out).toContain('Pièce n° 4');
    });

    it('pièce connue sans libellé → title = type seul', () => {
      const out = annotatePieceReferences(
        '<p>Pièce n° 2</p>',
        map([{ number: 2, label: null, typeLabel: 'Contrat' }]),
      );
      expect(out).toContain('title="Contrat"');
    });

    it('numéro inconnu → AUCUNE décoration (jamais d\'info inventée)', () => {
      const html = '<p>Pièce n° 9 absente</p>';
      const out = annotatePieceReferences(
        html,
        map([{ number: 4, label: 'x', typeLabel: 'Contrat' }]),
      );
      expect(out).toBe(html);
      expect(out).not.toContain('cd-piece-ref');
    });

    it('map vide → HTML inchangé', () => {
      const html = '<p>Pièce n° 4</p>';
      expect(annotatePieceReferences(html, new Map())).toBe(html);
    });

    it('tolère les variantes d\'espaces (« Pièce n°4 » / « Pièce n° 4 »)', () => {
      const pieces = map([{ number: 4, label: 'l', typeLabel: 'Contrat' }]);
      expect(annotatePieceReferences('Pièce n°4', pieces)).toContain('cd-piece-ref');
      expect(annotatePieceReferences('Pièce n°  4', pieces)).toContain('cd-piece-ref');
    });

    it('ne touche pas au contenu des balises HTML (remplace hors « <...> »)', () => {
      // Un attribut contenant « Pièce n° 4 » ne doit pas être réécrit.
      const html = '<a data-x="Pièce n° 4">lien</a> et Pièce n° 4 dans le texte';
      const out = annotatePieceReferences(
        html,
        map([{ number: 4, label: 'l', typeLabel: 'Contrat' }]),
      );
      // L'attribut d'origine reste intact (une seule réécriture, dans le texte).
      expect(out).toContain('data-x="Pièce n° 4"');
      expect(out.match(/cd-piece-ref/g)?.length).toBe(1);
    });

    it('échappe le libellé dans le title (pas d\'injection d\'attribut)', () => {
      const out = annotatePieceReferences(
        '<p>Pièce n° 1</p>',
        map([{ number: 1, label: 'a"><img src=x>', typeLabel: 'Contrat' }]),
      );
      expect(out).not.toContain('<img src=x>');
      expect(out).toContain('&quot;');
    });
  });

  describe('F-266 SF-266-01 — escapeHtmlAttribute', () => {
    it('échappe &, ", \', <, >', () => {
      expect(escapeHtmlAttribute('a&b"c\'d<e>f')).toBe(
        'a&amp;b&quot;c&#39;d&lt;e&gt;f',
      );
    });
  });

  describe('F-266 SF-266-01 — input [pieces]', () => {
    it('sans pièces → rendu identique (non-régression), pas de décoration', () => {
      setContent('Voir Pièce n° 3 du dossier.');
      expect(contentEl().querySelector('.cd-piece-ref')).toBeNull();
      expect(contentEl().textContent).toContain('Pièce n° 3');
    });

    it('avec pièces → le renvoi connu est décoré dans le DOM', () => {
      component.pieces = [
        { number: 3, label: 'Mars 2024', typeLabel: 'Bulletin de paie' },
      ];
      setContent('Voir Pièce n° 3 du dossier.');
      const ref = contentEl().querySelector(
        '.cd-piece-ref',
      ) as HTMLElement | null;
      expect(ref).not.toBeNull();
      expect(ref!.getAttribute('title')).toBe('Bulletin de paie — Mars 2024');
    });

    it('le content (markdown) n\'est pas muté par la décoration', () => {
      const src = 'Voir Pièce n° 3.';
      component.pieces = [{ number: 3, label: 'x', typeLabel: 'Contrat' }];
      setContent(src);
      expect(component.content).toBe(src);
    });
  });

  // ── F-276 — Sommaire / navigation cliquable par section ──────────────────
  describe('F-276 — sommaire de navigation', () => {
    const SUMMARY = '[data-testid="conclusions-summary"]';
    const ITEM = '[data-testid="summary-item"]';

    function summaryEl(): HTMLElement | null {
      return fixture.nativeElement.querySelector(SUMMARY);
    }
    function items(): HTMLButtonElement[] {
      return Array.from(
        fixture.nativeElement.querySelectorAll(ITEM),
      ) as HTMLButtonElement[];
    }

    const TWO_SECTIONS =
      '# Tribunal\n\n## FAITS\n\nExposé des faits.\n\n## DISCUSSION\n\nArgumentation.';

    it('≥ 2 sections → un sommaire avec une entrée par section, dans l\'ordre', () => {
      setContent(TWO_SECTIONS);
      expect(summaryEl()).not.toBeNull();
      const labels = items().map((b) => b.textContent?.trim());
      expect(labels).toEqual(['FAITS', 'DISCUSSION']);
    });

    it('0 section (que du texte) → aucun sommaire', () => {
      setContent('Un simple paragraphe sans titre de section.');
      expect(summaryEl()).toBeNull();
    });

    it('1 seule section → aucun sommaire (navigation inutile)', () => {
      setContent('# Tribunal\n\n## FAITS\n\nExposé.');
      expect(summaryEl()).toBeNull();
    });

    it('le sommaire est aligné, dans l\'ordre, sur les titres <h2>/<h3> rendus', () => {
      setContent('## FAITS\n\nx\n\n### Sous-point\n\ny\n\n## DISCUSSION\n\nz');
      // 3 entrées de sommaire = 3 titres rendus dans le corps, même ordre.
      const labels = items().map((b) => b.textContent?.trim());
      const headings = Array.from(
        contentEl().querySelectorAll('h2, h3'),
      ) as HTMLElement[];
      expect(labels).toEqual(['FAITS', 'Sous-point', 'DISCUSSION']);
      expect(headings.map((h) => h.textContent?.trim())).toEqual(labels);
    });

    it('clic sur une entrée → scrollIntoView smooth/start sur le BON titre (par index)', () => {
      setContent(TWO_SECTIONS);
      const headings = Array.from(
        contentEl().querySelectorAll('h2, h3'),
      ) as HTMLElement[];
      // jsdom n'implémente pas scrollIntoView → on l'installe sur chaque titre.
      const spy0 = jest.fn();
      const spy1 = jest.fn();
      headings[0].scrollIntoView = spy0;
      headings[1].scrollIntoView = spy1;

      items()[1].click();

      expect(spy1).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' });
      expect(spy0).not.toHaveBeenCalled();
    });

    it('clic alors que le titre cible est absent → aucune erreur', () => {
      setContent(TWO_SECTIONS);
      // jumpTo(index) hors bornes ne doit jamais lever (item?.scrollIntoView).
      expect(() => component.jumpTo(99)).not.toThrow();
    });

    it('une entrée de niveau ### est marquée sous-section (indentation)', () => {
      setContent('## FAITS\n\nx\n\n### Détail\n\ny');
      const subItem = items()[1];
      expect(subItem.getAttribute('data-level')).toBe('3');
      expect(
        subItem.closest('.cd-summary__item')?.classList.contains(
          'cd-summary__item--sub',
        ),
      ).toBe(true);
    });

    it('le content (markdown) n\'est pas muté par le sommaire', () => {
      setContent(TWO_SECTIONS);
      expect(component.content).toBe(TWO_SECTIONS);
    });

    it('le sommaire ne casse pas le rendu du corps (titres toujours présents)', () => {
      setContent(TWO_SECTIONS);
      expect(contentEl().querySelectorAll('h2').length).toBe(2);
      expect(contentEl().textContent).toContain('Exposé des faits.');
    });
  });
});
