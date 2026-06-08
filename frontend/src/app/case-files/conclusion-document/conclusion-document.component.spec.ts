import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConclusionDocumentComponent } from './conclusion-document.component';

describe('ConclusionDocumentComponent', () => {
  let component: ConclusionDocumentComponent;
  let fixture: ComponentFixture<ConclusionDocumentComponent>;

  const SHEET = '[data-testid="conclusions-content"]';
  const TITLE = '[data-testid="cd-section-title"]';
  const BODY = '[data-testid="cd-body"]';
  const FOOTER = '[data-testid="cd-footer"]';
  const DISPOSITIF = '[data-testid="cd-dispositif"]';

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

  it('content null → ne rend rien (état vide géré par le parent)', () => {
    setContent(null);
    expect(fixture.nativeElement.querySelector(SHEET)).toBeNull();
  });

  it('content vide → ne rend rien', () => {
    setContent('   ');
    expect(fixture.nativeElement.querySelector(SHEET)).toBeNull();
  });

  it('rend la feuille avec data-testid="conclusions-content" (testid préservé)', () => {
    setContent('FAITS ET PROCÉDURE\nLe salarié…');
    const sheet = fixture.nativeElement.querySelector(SHEET);
    expect(sheet).not.toBeNull();
    expect(sheet.tagName.toLowerCase()).toBe('article');
  });

  it('rend N sections avec titres et corps', () => {
    setContent(
      [
        'FAITS ET PROCÉDURE',
        'Le salarié a été licencié.',
        '',
        'DISCUSSION',
        'Le licenciement est sans cause.',
      ].join('\n'),
    );

    const titles = fixture.nativeElement.querySelectorAll(TITLE);
    expect(titles.length).toBe(2);
    expect(titles[0].textContent).toContain('FAITS ET PROCÉDURE');
    expect(titles[1].textContent).toContain('DISCUSSION');

    const bodies = fixture.nativeElement.querySelectorAll(BODY);
    expect(bodies.length).toBe(2);
    expect(bodies[0].textContent).toContain('Le salarié a été licencié.');
  });

  it('met en valeur POUR / CONTRE comme titres de section', () => {
    setContent('POUR\nM. Dupont\nCONTRE\nLa société Y');
    const titles = Array.from(
      fixture.nativeElement.querySelectorAll(TITLE),
    ).map((el) => (el as HTMLElement).textContent?.trim());
    expect(titles).toContain('POUR');
    expect(titles).toContain('CONTRE');
  });

  it('content sans titre → fallback paragraphes (pas de titre, pas de crash)', () => {
    setContent('Première ligne.\n\nDeuxième ligne.');
    expect(fixture.nativeElement.querySelector(SHEET)).not.toBeNull();
    expect(fixture.nativeElement.querySelectorAll(TITLE).length).toBe(0);
    const bodies = fixture.nativeElement.querySelectorAll(BODY);
    expect(bodies.length).toBe(2);
  });

  it('PAR CES MOTIFS avec items → liste numérotée <ol>', () => {
    setContent(
      [
        'PAR CES MOTIFS',
        'DIRE le licenciement sans cause.',
        'CONDAMNER la société à 10 000 €.',
      ].join('\n'),
    );
    const ol = fixture.nativeElement.querySelector(DISPOSITIF);
    expect(ol).not.toBeNull();
    expect(ol.tagName.toLowerCase()).toBe('ol');
    expect(ol.querySelectorAll('li').length).toBe(2);
  });

  it('pied de page « Document de travail » présent quand il y a du contenu', () => {
    setContent('DISCUSSION\nArgument.');
    const footer = fixture.nativeElement.querySelector(FOOTER);
    expect(footer).not.toBeNull();
    expect(footer.textContent).toContain(
      'Document de travail — à vérifier par l\'avocat',
    );
  });

  it('aucun branding LegalCase dans le document rendu', () => {
    setContent('DISCUSSION\nArgument.');
    const sheet = fixture.nativeElement.querySelector(SHEET) as HTMLElement;
    expect(sheet.textContent).not.toMatch(/LegalCase/i);
  });
});
