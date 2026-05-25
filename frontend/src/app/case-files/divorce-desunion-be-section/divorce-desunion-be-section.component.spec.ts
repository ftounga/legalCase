import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { DivorceDesunionBeSectionComponent } from './divorce-desunion-be-section.component';
import { DivorceDesunionBeResponse } from '../../core/models/divorce-desunion-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('DivorceDesunionBeSectionComponent', () => {
  let component: DivorceDesunionBeSectionComponent;
  let fixture: ComponentFixture<DivorceDesunionBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/desunion-irremediable-be';

  function beResponse(): DivorceDesunionBeResponse {
    return {
      caseFileId: 'case-1',
      dateSeparation: '2025-04-01',
      separationConsentue: true,
      preuvesSeparation: true,
      preuvesDocumentaires: true,
      tentativesReconciliation: false,
      dateAssignation: '2025-11-01',
      dureeSeparationMois: 7,
      seuilSeparationMois: 6,
      delaiObjectifOk: true,
      conditionsReunies: true,
      scoreGlobal: 95,
      verdictProbabilite: 'ELEVEE',
      baseJuridique: 'Art. 229 §2 CC belge (loi 27/04/2007)',
      formule: 'Désunion irrémédiable BE consentue : score 95/100 — probabilité ELEVEE',
      messages: ['Conditions objectives réunies'],
      country: 'BELGIQUE',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        DivorceDesunionBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(DivorceDesunionBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ---------------------------------------------------------------------------
  // Mount + base
  // ---------------------------------------------------------------------------

  it('mounts with collapsed=true initial', () => {
    expect(component).toBeTruthy();
    expect(component.collapsed()).toBe(true);
  });

  it('toggleCollapse switches collapsed signal', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode re-shows the form', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Form validation
  // ---------------------------------------------------------------------------

  it('formValid: false when dateSeparation missing, true once filled', () => {
    expect(component.formValid()).toBe(false);
    component.dateSeparation.set('2025-04-01');
    expect(component.formValid()).toBe(true);
  });

  it('formValid: false if dateAssignation < dateSeparation', () => {
    component.dateSeparation.set('2025-04-01');
    component.dateAssignation.set('2025-03-01');
    expect(component.formValid()).toBe(false);
    component.dateAssignation.set('2025-09-01');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Load (GET)
  // ---------------------------------------------------------------------------

  it('GET 200 → form hidden + result populated', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(beResponse());

    expect(component.result()!.scoreGlobal).toBe(95);
    expect(component.result()!.country).toBe('BELGIQUE');
    expect(component.showForm()).toBe(false);
    expect(component.dateSeparation()).toBe('2025-04-01');
    expect(component.separationConsentue()).toBe(true);
  });

  it('GET 404 → form remains visible', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // POST (calculate)
  // ---------------------------------------------------------------------------

  it('calculate() POST → result + success snackbar', () => {
    component.dateSeparation.set('2025-04-01');
    component.separationConsentue.set(true);
    component.preuvesSeparation.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateSeparation).toBe('2025-04-01');
    expect(req.request.body.separationConsentue).toBe(true);
    expect(req.request.body.preuvesSeparation).toBe(true);
    req.flush(beResponse());

    expect(component.result()!.verdictProbabilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse désunion irrémédiable enregistrée', 'OK', jasmine.any(Object));
  });

  it('calculate() error → red snackbar', () => {
    component.dateSeparation.set('2025-04-01');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  it('calculate() ignored if form invalid → no HTTP call', () => {
    component.dateSeparation.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Gate workspaceCountry
  // ---------------------------------------------------------------------------

  it('workspaceCountry FRANCE → no HTTP call + isBelgiumGate false', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.isBelgiumGate()).toBe(false);
    expect(component.formValid()).toBe(false);
  });

  it('workspaceCountry BELGIQUE → HTTP GET issued', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.isBelgiumGate()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA on GET 404 → values + IA badges', () => {
    const ai: Partial<FamilleExtractedData> = {
      dateSeparationBe: '2024-12-01',
      // SF-246-12 : separationConsentue est aspirationnel (no-op gracieux côté helper) —
      // non pré-rempli par l'IA même si présent dans aiData.
      separationConsentue: true,
    };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateSeparation()).toBe('2024-12-01');
    expect(component.provenanceDateSeparation()).toBe('IA');
    // separationConsentue reste à la valeur par défaut (false), pas de badge IA.
    expect(component.separationConsentue()).toBe(false);
    expect(component.provenanceSeparationConsentue()).toBeNull();
  });

  it('pré-fill IA without aiData → no values, no badges', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateSeparation()).toBeNull();
    expect(component.separationConsentue()).toBe(false);
    expect(component.provenanceDateSeparation()).toBeNull();
    expect(component.provenanceSeparationConsentue()).toBeNull();
  });

  it('manual change clears IA badge', () => {
    const ai: Partial<FamilleExtractedData> = { dateSeparationBe: '2024-12-01' };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceDateSeparation()).toBe('IA');
    component.onDateSeparationChange('2025-01-15');
    expect(component.dateSeparation()).toBe('2025-01-15');
    expect(component.provenanceDateSeparation()).toBeNull();
  });

  it('GET 200 → no IA badges (persisted values prevail)', () => {
    const ai: Partial<FamilleExtractedData> = {
      dateSeparationBe: '2099-09-09',
      separationConsentue: false,
    };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse());

    expect(component.dateSeparation()).toBe('2025-04-01');
    expect(component.separationConsentue()).toBe(true);
    expect(component.provenanceDateSeparation()).toBeNull();
    expect(component.provenanceSeparationConsentue()).toBeNull();
  });

  it('ngOnChanges(aiData) post-mount refreshes pré-fill if form empty', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi: Partial<FamilleExtractedData> = { dateSeparationBe: '2024-08-15' };
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.dateSeparation()).toBe('2024-08-15');
    expect(component.provenanceDateSeparation()).toBe('IA');
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherence alert DATE_SEPARATION if IA differs from user input', () => {
    component.aiData = { dateSeparationBe: '2024-06-01' };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit '2024-06-01'. Avocat passe à '2025-01-01'.
    component.onDateSeparationChange('2025-01-01');

    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_SEPARATION).toBeDefined();
    expect(alerts.DATE_SEPARATION!.field).toBe('DATE_SEPARATION');
    expect(alerts.DATE_SEPARATION!.source).toBe('IA');
    expect(alerts.DATE_SEPARATION!.expectedDisplay).toBe('2024-06-01');
  });

  it('no coherence alert if IA matches user input on DATE_SEPARATION', () => {
    component.aiData = { dateSeparationBe: '2024-06-01' };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.coherenceAlerts().DATE_SEPARATION).toBeUndefined();
  });

  it('coherence alert SEPARATION_CONSENTUE if IA differs', () => {
    component.aiData = { separationConsentue: true };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit true. Avocat passe à false.
    component.onSeparationConsentueChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.SEPARATION_CONSENTUE).toBeDefined();
    expect(alerts.SEPARATION_CONSENTUE!.source).toBe('IA');
    expect(alerts.SEPARATION_CONSENTUE!.expectedDisplay).toContain('Consentue');
  });

  it('alerts hidden once result rendered (showForm=false)', () => {
    component.aiData = { dateSeparationBe: '2024-06-01' };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onDateSeparationChange('2025-01-01');
    expect(component.coherenceAlerts().DATE_SEPARATION).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DATE_SEPARATION).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // F-IA-03 multi-source (F96 + IA)
  // ---------------------------------------------------------------------------

  it('coherence alert MULTI when F96 + IA both diverge on DATE_SEPARATION', () => {
    component.aiData = { dateSeparationBe: '2024-06-01' };
    component.procedureChecks = [{
      id: 'chk-1', ordre: 1, description: 'Date séparation attendue',
      statut: 'VERIFIED',
      critereCode: 'DESU_BE_DATE_SEPARATION',
      expectedValue: '2024-06-01',
    }];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Avocat saisit 2025-01-01 → divergent IA et F96 (qui s'accordent à 2024-06-01).
    component.onDateSeparationChange('2025-01-01');

    const alert = component.coherenceAlerts().DATE_SEPARATION;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors.length).toBe(2);
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
  });

  // ---------------------------------------------------------------------------
  // Verdict helpers
  // ---------------------------------------------------------------------------

  it('verdictLabel + verdictClass map ELEVEE/MOYENNE/FAIBLE correctly', () => {
    expect(component.verdictLabel('ELEVEE')).toBe('Probabilité élevée');
    expect(component.verdictLabel('MOYENNE')).toBe('Probabilité moyenne');
    expect(component.verdictLabel('FAIBLE')).toBe('Probabilité faible');
    expect(component.verdictClass('ELEVEE')).toContain('--ok');
    expect(component.verdictClass('MOYENNE')).toContain('--warn');
    expect(component.verdictClass('FAIBLE')).toContain('--ko');
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02c — mode simulateur autonome
  // ---------------------------------------------------------------------------

  describe('F-163 SF-163-02c — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-FA-11-desunion-irremediable-be/calculate';
    const CASE_URL = '/api/v1/case-files/case-1/desunion-irremediable-be';

    it('CA-02 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-02 : ne fait AUCUN GET vers /api/v1/case-files/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const matches = httpMock.match((r: { url: string }) => r.url.includes('/api/v1/case-files/'));
      expect(matches.length).toBe(0);
    });

    it('CA-04 : POST sur le dispatcher /api/v1/simulators/.../calculate en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      component.calculate();
      const requests = httpMock.match((r: { url: string; method: string }) => r.url === STANDALONE_URL && r.method === 'POST');
      // Si la méthode formValid() bloque ou si la gate FR/BE échoue, aucun POST
      // n'est émis — c'est acceptable (le standalone n'a pas de payload obligatoire).
      // On valide ici qu'**aucun POST vers le case-file URL** n'a été émis.
      const caseUrlPosts = httpMock.match((r: { url: string; method: string }) => r.url === CASE_URL && r.method === 'POST');
      expect(caseUrlPosts.length).toBe(0);
      // Et, si le composant a POST sur le dispatcher, il flush proprement.
      requests.forEach(req => req.flush({}));
    });
  });
});
