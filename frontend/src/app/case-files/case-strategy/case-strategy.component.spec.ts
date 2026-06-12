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

  it('READY → renders markdown reco + meta + date', () => {
    initWith({
      status: 'READY',
      content: '## Voie procédurale\nSaisir au fond.',
      generatedAt: '2026-06-12T10:00:00Z',
      toolsConsidered: 3,
      modelUsed: 'claude-sonnet',
    });
    const reco = el('strat-reco');
    expect(reco).toBeTruthy();
    expect(reco!.innerHTML).toContain('Voie procédurale');
    expect(reco!.innerHTML).toContain('Saisir au fond');
    expect(el('strat-meta')!.textContent).toContain('3 verdicts');
    expect(el('strat-date')).toBeTruthy();
    expect(el('strat-to-conclusions')).toBeTruthy();
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

  it('generate → POST READY renders the reco', () => {
    initWith({ status: 'EMPTY', content: null, generatedAt: null, toolsConsidered: 0, modelUsed: null });
    el('strat-generate')!.click();
    fixture.detectChanges();
    httpMock.expectOne((r) => r.method === 'POST' && r.url === URL).flush({
      status: 'READY',
      content: '## Posture\nTransiger.',
      generatedAt: '2026-06-12T10:00:00Z',
      toolsConsidered: 1,
      modelUsed: 'm',
    } as CaseStrategy);
    fixture.detectChanges();
    expect(el('strat-reco')!.innerHTML).toContain('Transiger');
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
