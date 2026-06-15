import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { CaseStrategyComponent } from './case-strategy.component';
import { CaseStrategy } from '../../core/models/case-strategy.model';

describe('CaseStrategyComponent', () => {
  let fixture: ComponentFixture<CaseStrategyComponent>;
  let component: CaseStrategyComponent;
  let httpMock: HttpTestingController;

  const CASE_ID = 'case-1';
  const URL = `/api/v1/case-files/${CASE_ID}/strategy`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CaseStrategyComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseStrategyComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_ID;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function el(testId: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(`[data-testid="${testId}"]`);
  }

  function initWith(s: CaseStrategy) {
    fixture.detectChanges(); // ngOnInit → GET
    httpMock.expectOne((r) => r.method === 'GET' && r.url === URL).flush(s);
    fixture.detectChanges();
  }

  it('EMPTY → initial empty state with generate CTA', () => {
    initWith({ status: 'EMPTY', content: null, generatedAt: null, toolsConsidered: 0, modelUsed: null });
    expect(el('strat-empty')).toBeTruthy();
    expect(el('strat-generate')).toBeTruthy();
    expect(el('strat-reco')).toBeFalsy();
  });

  // Markdown aux 4 titres `##` contractuels du prompt F-286.
  const FULL_MD = [
    '## Voie procédurale',
    'Saisir au fond. Le référé est inadapté faute d’urgence.',
    '',
    '## Posture',
    'Plaider plutôt que transiger. Le rapport de force est favorable.',
    '',
    '## Priorisation des chefs de demande',
    '- Nullité du licenciement',
    '- Indemnité de préavis',
    '- Dommages et intérêts',
    '',
    '## Séquencement',
    '- Saisir le conseil de prud’hommes',
    '- Communiquer les pièces',
  ].join('\n');

  function readyStrategy(content: string, toolsConsidered = 3): CaseStrategy {
    return {
      status: 'READY',
      content,
      generatedAt: '2026-06-12T10:00:00Z',
      toolsConsidered,
      modelUsed: 'claude-sonnet',
    };
  }

  // CA1 — carte repliée par défaut : markdown complet absent, résumé présent.
  it('READY → compact by default: full markdown absent, summary + meta + date present', () => {
    initWith(readyStrategy(FULL_MD));
    expect(component.expanded()).toBe(false);
    expect(el('strat-reco')).toBeFalsy(); // markdown complet non rendu
    expect(el('strat-summary')).toBeTruthy();
    expect(component.parseOk()).toBe(true);
    expect(el('strat-meta')!.textContent).toContain('3 verdicts');
    expect(el('strat-date')).toBeTruthy();
    expect(el('strat-to-conclusions')).toBeTruthy();
    expect(el('strat-regenerate')).toBeTruthy();
    expect(el('strat-toggle-detail')).toBeTruthy();
  });

  // CA2 — résumé : 1ʳᵉ phrase Voie + Posture + compte des chefs.
  it('summary() extracts first sentence of Voie/Posture + counts demand heads', () => {
    initWith(readyStrategy(FULL_MD));
    const lines = component.summary();
    const byLabel = (l: string) => lines.find((x) => x.label === l);
    expect(byLabel('Voie procédurale')!.text).toBe('Saisir au fond.');
    expect(byLabel('Posture')!.text).toBe('Plaider plutôt que transiger.');
    expect(byLabel('Priorisation')!.text).toBe('3 chefs de demande priorisés');
    const summary = el('strat-summary')!;
    expect(summary.textContent).toContain('Saisir au fond.');
    expect(summary.textContent).toContain('Plaider plutôt que transiger.');
    expect(summary.textContent).toContain('3 chefs de demande priorisés');
  });

  // CA3 — toggle déplie / replie le markdown complet in situ.
  it('toggle shows then hides the full markdown detail in situ', () => {
    initWith(readyStrategy(FULL_MD));
    expect(el('strat-reco')).toBeFalsy();
    el('strat-toggle-detail')!.click();
    fixture.detectChanges();
    expect(component.expanded()).toBe(true);
    const reco = el('strat-reco');
    expect(reco).toBeTruthy();
    expect(reco!.innerHTML).toContain('Saisir au fond');
    expect(el('strat-summary')).toBeFalsy(); // résumé masqué une fois déplié
    el('strat-toggle-detail')!.click();
    fixture.detectChanges();
    expect(component.expanded()).toBe(false);
    expect(el('strat-reco')).toBeFalsy();
    expect(el('strat-summary')).toBeTruthy();
  });

  // CA4 — date + conclusions + régénérer présents dans les deux états.
  it('date + conclusions link + regenerate present collapsed AND expanded', () => {
    initWith(readyStrategy(FULL_MD));
    expect(el('strat-date')).toBeTruthy();
    expect(el('strat-to-conclusions')).toBeTruthy();
    expect(el('strat-regenerate')).toBeTruthy();
    el('strat-toggle-detail')!.click();
    fixture.detectChanges();
    expect(el('strat-date')).toBeTruthy();
    expect(el('strat-to-conclusions')).toBeTruthy();
    expect(el('strat-regenerate')).toBeTruthy();
  });

  // CA5 — markdown sans les titres attendus → parseOk false + fallback tronqué, pas d'erreur.
  it('markdown without expected headings → fallback truncated, parseOk false, no crash', () => {
    initWith(readyStrategy('Un texte libre sans aucun titre de section.'));
    expect(component.parseOk()).toBe(false);
    expect(el('strat-summary')).toBeFalsy();
    const collapsed = el('strat-reco-collapsed');
    expect(collapsed).toBeTruthy();
    expect(collapsed!.innerHTML).toContain('Un texte libre');
    expect(el('strat-toggle-detail')).toBeTruthy();
    // détail reste accessible
    el('strat-toggle-detail')!.click();
    fixture.detectChanges();
    expect(el('strat-reco')!.innerHTML).toContain('Un texte libre');
  });

  it('generate → POST, then EMPTY_INPUT shows honest empty (no invented reco)', () => {
    initWith({ status: 'EMPTY', content: null, generatedAt: null, toolsConsidered: 0, modelUsed: null });
    el('strat-generate')!.click();
    fixture.detectChanges();
    httpMock.expectOne((r) => r.method === 'POST' && r.url === URL).flush({
      status: 'EMPTY_INPUT',
      content: null,
      generatedAt: null,
      toolsConsidered: 0,
      modelUsed: null,
    } as CaseStrategy);
    fixture.detectChanges();
    expect(el('strat-empty-input')).toBeTruthy();
    expect(el('strat-reco')).toBeFalsy();
  });

  it('generate → POST READY renders the reco (compact, full content reachable on expand)', () => {
    initWith({ status: 'EMPTY', content: null, generatedAt: null, toolsConsidered: 0, modelUsed: null });
    el('strat-generate')!.click();
    fixture.detectChanges();
    httpMock.expectOne((r) => r.method === 'POST' && r.url === URL).flush(readyStrategy(FULL_MD, 1));
    fixture.detectChanges();
    // compact par défaut : résumé visible, markdown complet caché
    expect(el('strat-summary')).toBeTruthy();
    expect(el('strat-reco')).toBeFalsy();
    // dépli → contenu complet accessible
    el('strat-toggle-detail')!.click();
    fixture.detectChanges();
    expect(el('strat-reco')!.innerHTML).toContain('Plaider plutôt que transiger');
  });

  // CA6 — états generating / emptyInput inchangés (pas de toggle, pas de résumé).
  it('generating state unchanged: spinner shown, no summary/toggle', () => {
    initWith({ status: 'EMPTY', content: null, generatedAt: null, toolsConsidered: 0, modelUsed: null });
    el('strat-generate')!.click();
    fixture.detectChanges();
    expect(el('strat-generating')).toBeTruthy();
    expect(el('strat-summary')).toBeFalsy();
    expect(el('strat-toggle-detail')).toBeFalsy();
    expect(el('strat-reco')).toBeFalsy();
    httpMock.expectOne((r) => r.method === 'POST' && r.url === URL).flush(readyStrategy(FULL_MD, 1));
    fixture.detectChanges();
  });

  it('generate error → snackbar, no crash', () => {
    initWith({ status: 'EMPTY', content: null, generatedAt: null, toolsConsidered: 0, modelUsed: null });
    el('strat-generate')!.click();
    fixture.detectChanges();
    httpMock
      .expectOne((r) => r.method === 'POST' && r.url === URL)
      .flush('boom', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(component.generating()).toBe(false);
  });
});
