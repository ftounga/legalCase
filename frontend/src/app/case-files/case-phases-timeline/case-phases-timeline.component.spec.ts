import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { CasePhasesTimelineComponent } from './case-phases-timeline.component';
import {
  CasePhaseSuggestion,
  CasePhaseTimeline,
} from '../../core/models/case-phase.model';

describe('CasePhasesTimelineComponent', () => {
  let fixture: ComponentFixture<CasePhasesTimelineComponent>;
  let component: CasePhasesTimelineComponent;
  let httpMock: HttpTestingController;

  const CASE_ID = 'case-1';
  const URL = `/api/v1/case-files/${CASE_ID}/phases`;
  const SUGGESTIONS_URL = `${URL}/suggestions`;

  const SUGGESTIONS: CasePhaseSuggestion[] = [
    { type: 'RECOURS_PREALABLE', defaultLabel: 'Recours gracieux / hiérarchique' },
    { type: 'TRIBUNAL_ADMINISTRATIF', defaultLabel: 'Recours contentieux (Tribunal administratif)' },
    { type: 'CONSEIL_ETAT', defaultLabel: 'Cassation (Conseil d’État)' },
  ];

  /** Vide la requête de suggestions déclenchée par ngOnInit (ordre indépendant du timeline). */
  function flushSuggestions(s: CasePhaseSuggestion[] = SUGGESTIONS): void {
    httpMock.expectOne(SUGGESTIONS_URL).flush(s);
  }

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
    flushSuggestions();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="phase-step-0"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="phase-step-1"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="phase-current"]')!.textContent).toContain('Jugement au fond');
  });

  it('met en exergue la dernière phase comme phase courante', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(timeline());
    flushSuggestions();
    fixture.detectChanges();

    const last = (fixture.nativeElement as HTMLElement)
      .querySelector('[data-testid="phase-step-1"]') as HTMLElement;
    expect(last.classList).toContain('phase-node--current');
  });

  it('état initial vide : montre « Phase 1 — Saisine »', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({ phases: [], currentPhase: null } as CasePhaseTimeline);
    flushSuggestions();
    fixture.detectChanges();

    const txt = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(txt).toContain('Phase 1');
    expect(txt).toContain('Saisine');
  });

  it('ajoute une phase (POST) puis recharge', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({ phases: [], currentPhase: null } as CasePhaseTimeline);
    flushSuggestions();

    component.openAdd();
    component.form.patchValue({ phase: 'CONCILIATION', enteredAt: '2026-02-15' });
    component.save();

    const post = httpMock.expectOne((r) => r.method === 'POST' && r.url === URL);
    expect(post.request.body.phase).toBe('CONCILIATION');
    post.flush({ id: 'p3', phase: 'CONCILIATION', label: null, enteredAt: '2026-02-15', note: null, createdAt: '', updatedAt: '' });

    // rechargement (timeline seul ; les suggestions ne sont pas rechargées au save)
    httpMock.expectOne(URL).flush(timeline());
    expect(component.showForm()).toBe(false);
  });

  it('le formulaire est invalide sans date d’entrée', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({ phases: [], currentPhase: null } as CasePhaseTimeline);
    flushSuggestions();

    component.openAdd();
    component.form.patchValue({ phase: 'SAISINE', enteredAt: '' });
    expect(component.form.invalid).toBe(true);
  });

  it('SF-283-03 : peuple le sélecteur avec les suggestions (domaine × pays)', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({ phases: [], currentPhase: null } as CasePhaseTimeline);
    flushSuggestions();
    fixture.detectChanges();

    const opts = component.phaseOptions();
    expect(opts.map((o) => o.value)).toEqual([
      'RECOURS_PREALABLE',
      'TRIBUNAL_ADMINISTRATIF',
      'CONSEIL_ETAT',
    ]);
    expect(opts[0].label).toBe('Recours gracieux / hiérarchique');
  });

  it('SF-283-03 : sélectionner un type pré-remplit le libellé (éditable)', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({ phases: [], currentPhase: null } as CasePhaseTimeline);
    flushSuggestions();

    component.openAdd();
    // openAdd pré-remplit déjà avec la 1re suggestion
    expect(component.form.controls.label.value).toBe('Recours gracieux / hiérarchique');

    // changer de type → met à jour le libellé par défaut
    component.form.controls.phase.setValue('CONSEIL_ETAT');
    component.onPhaseTypeChange('CONSEIL_ETAT');
    expect(component.form.controls.label.value).toBe('Cassation (Conseil d’État)');
  });

  it('SF-283-03 : ne réécrit pas un libellé personnalisé par l’avocat', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({ phases: [], currentPhase: null } as CasePhaseTimeline);
    flushSuggestions();

    component.openAdd();
    component.form.controls.label.setValue('Mon libellé à moi');
    component.onPhaseTypeChange('CONSEIL_ETAT');
    expect(component.form.controls.label.value).toBe('Mon libellé à moi');
  });

  it('SF-283-03 : fallback options statiques si suggestions vides', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({ phases: [], currentPhase: null } as CasePhaseTimeline);
    flushSuggestions([]);
    fixture.detectChanges();

    // CASE_PHASE_OPTIONS = 8 phases civiles FR travail
    expect(component.phaseOptions().length).toBe(8);
    expect(component.phaseOptions()[0].value).toBe('SAISINE');
  });
});
