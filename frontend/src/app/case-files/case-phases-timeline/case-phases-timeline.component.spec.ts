import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { CasePhasesTimelineComponent } from './case-phases-timeline.component';
import { CasePhaseTimeline } from '../../core/models/case-phase.model';

describe('CasePhasesTimelineComponent', () => {
  let fixture: ComponentFixture<CasePhasesTimelineComponent>;
  let component: CasePhasesTimelineComponent;
  let httpMock: HttpTestingController;

  const CASE_ID = 'case-1';
  const URL = `/api/v1/case-files/${CASE_ID}/phases`;

  function timeline(overrides: Partial<CasePhaseTimeline> = {}): CasePhaseTimeline {
    return {
      phases: [
        {
          id: 'p1', phase: 'SAISINE', label: 'Requête déposée',
          enteredAt: '2026-01-10', note: null, createdAt: '', updatedAt: '',
        },
        {
          id: 'p2', phase: 'FOND', label: null,
          enteredAt: '2026-04-01', note: 'renvoi au fond', createdAt: '', updatedAt: '',
        },
      ],
      currentPhase: 'FOND',
      ...overrides,
    };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CasePhasesTimelineComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CasePhasesTimelineComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_ID;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('charge et rend les phases + la phase courante', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(timeline());
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="phase-step-0"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="phase-step-1"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="phase-current"]')!.textContent).toContain('Jugement au fond');
  });

  it('met en exergue la dernière phase comme phase courante', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(timeline());
    fixture.detectChanges();

    const last = (fixture.nativeElement as HTMLElement)
      .querySelector('[data-testid="phase-step-1"]') as HTMLElement;
    expect(last.classList).toContain('phase-node--current');
  });

  it('état initial vide : montre « Phase 1 — Saisine »', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({ phases: [], currentPhase: null } as CasePhaseTimeline);
    fixture.detectChanges();

    const txt = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(txt).toContain('Phase 1');
    expect(txt).toContain('Saisine');
  });

  it('ajoute une phase (POST) puis recharge', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({ phases: [], currentPhase: null } as CasePhaseTimeline);

    component.openAdd();
    component.form.patchValue({ phase: 'CONCILIATION', enteredAt: '2026-02-15' });
    component.save();

    const post = httpMock.expectOne((r) => r.method === 'POST' && r.url === URL);
    expect(post.request.body.phase).toBe('CONCILIATION');
    post.flush({ id: 'p3', phase: 'CONCILIATION', label: null, enteredAt: '2026-02-15', note: null, createdAt: '', updatedAt: '' });

    // rechargement
    httpMock.expectOne(URL).flush(timeline());
    expect(component.showForm()).toBe(false);
  });

  it('le formulaire est invalide sans date d’entrée', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({ phases: [], currentPhase: null } as CasePhaseTimeline);

    component.openAdd();
    component.form.patchValue({ phase: 'SAISINE', enteredAt: '' });
    expect(component.form.invalid).toBe(true);
  });
});
