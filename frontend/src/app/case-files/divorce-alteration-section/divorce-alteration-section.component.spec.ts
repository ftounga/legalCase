import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { DivorceAlterationSectionComponent } from './divorce-alteration-section.component';
import {
  DivorceAlterationResponse,
  FamilleAiData,
} from '../../core/models/divorce-alteration.model';

describe('DivorceAlterationSectionComponent', () => {
  let component: DivorceAlterationSectionComponent;
  let fixture: ComponentFixture<DivorceAlterationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/divorce-alteration';

  function frResponse(): DivorceAlterationResponse {
    return {
      caseFileId: 'case-1',
      dateCessationVieCommune: '2024-06-01',
      preuvesSeparationDocumentaires: true,
      tentativesReconciliation: false,
      dureeMariageAnnees: 10,
      revenusAnnuelsEpoux1Eur: 50000,
      revenusAnnuelsEpoux2Eur: 30000,
      patrimoineCommunSignificatif: false,
      dateAssignation: '2025-08-01',
      country: 'FRANCE',
      dureeSeparationAnnees: 1.17,
      delaiObjectifOk: true,
      absencePreuveReconciliation: true,
      conditionsReunies: true,
      scoreGlobal: 100,
      verdictProbabilite: 'ELEVEE',
      criteresNonRemplis: [],
      prestationCompensatoireFourchetteMin: 30000,
      prestationCompensatoireFourchetteMax: 90000,
      formule: 'Divorce altération définitive (art. 237 Cciv) : probabilité ELEVEE (score 100/100)',
      baseJuridique: 'Art. 237-238 Cciv (loi 23/03/2019)',
      messages: ['Conditions objectives réunies'],
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        DivorceAlterationSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(DivorceAlterationSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

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

  it('formValid: false when required fields missing', () => {
    expect(component.formValid()).toBe(false);

    component.dateCessationVieCommune.set('2024-06-01');
    component.dureeMariageAnnees.set(10);
    component.revenusAnnuelsEpoux1Eur.set(50000);
    component.revenusAnnuelsEpoux2Eur.set(30000);
    expect(component.formValid()).toBe(true);

    // Negative duration KO
    component.dureeMariageAnnees.set(-1);
    expect(component.formValid()).toBe(false);

    // Negative revenue KO
    component.dureeMariageAnnees.set(10);
    component.revenusAnnuelsEpoux1Eur.set(-100);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Load (GET)
  // ---------------------------------------------------------------------------

  it('GET 200 → form hidden + result populated', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());

    expect(component.result()!.scoreGlobal).toBe(100);
    expect(component.showForm()).toBe(false);
    expect(component.dureeMariageAnnees()).toBe(10);
    expect(component.dateCessationVieCommune()).toBe('2024-06-01');
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
    component.dateCessationVieCommune.set('2024-06-01');
    component.dureeMariageAnnees.set(10);
    component.revenusAnnuelsEpoux1Eur.set(50000);
    component.revenusAnnuelsEpoux2Eur.set(30000);
    component.preuvesSeparationDocumentaires.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateCessationVieCommune).toBe('2024-06-01');
    expect(req.request.body.dureeMariageAnnees).toBe(10);
    expect(req.request.body.preuvesSeparationDocumentaires).toBe(true);
    req.flush(frResponse());

    expect(component.result()!.verdictProbabilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse divorce calculée', 'OK', jasmine.any(Object));
  });

  it('calculate() error → red snackbar', () => {
    component.dateCessationVieCommune.set('2024-06-01');
    component.dureeMariageAnnees.set(10);
    component.revenusAnnuelsEpoux1Eur.set(50000);
    component.revenusAnnuelsEpoux2Eur.set(30000);
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
    component.dateCessationVieCommune.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA on GET 404 → values + IA badges', () => {
    const ai: FamilleAiData = {
      dateCessationVieCommune: '2023-01-15',
      dureeMariageAnnees: 12,
      revenusAnnuelsEpoux1Eur: 48000,
      revenusAnnuelsEpoux2Eur: 28000,
      patrimoineCommunSignificatif: true,
    };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateCessationVieCommune()).toBe('2023-01-15');
    expect(component.dureeMariageAnnees()).toBe(12);
    expect(component.revenusAnnuelsEpoux1Eur()).toBe(48000);
    expect(component.revenusAnnuelsEpoux2Eur()).toBe(28000);
    expect(component.patrimoineCommunSignificatif()).toBe(true);
    expect(component.provenanceDateCessation()).toBe('IA');
    expect(component.provenanceDureeMariage()).toBe('IA');
    expect(component.provenanceRevenus1()).toBe('IA');
    expect(component.provenanceRevenus2()).toBe('IA');
    expect(component.provenancePatrimoine()).toBe('IA');
  });

  it('pré-fill IA without aiData → no values, no badges', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateCessationVieCommune()).toBeNull();
    expect(component.dureeMariageAnnees()).toBeNull();
    expect(component.provenanceDureeMariage()).toBeNull();
    expect(component.provenanceRevenus1()).toBeNull();
  });

  it('manual change clears IA badge', () => {
    const ai: FamilleAiData = { revenusAnnuelsEpoux1Eur: 50000 };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceRevenus1()).toBe('IA');
    component.onRevenus1Change(60000);
    expect(component.revenusAnnuelsEpoux1Eur()).toBe(60000);
    expect(component.provenanceRevenus1()).toBeNull();
  });

  it('GET 200 → no IA badges (persisted values prevail)', () => {
    const ai: FamilleAiData = {
      dureeMariageAnnees: 99,
      revenusAnnuelsEpoux1Eur: 99999,
    };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());

    expect(component.dureeMariageAnnees()).toBe(10);
    expect(component.revenusAnnuelsEpoux1Eur()).toBe(50000);
    expect(component.provenanceDureeMariage()).toBeNull();
    expect(component.provenanceRevenus1()).toBeNull();
  });

  it('ngOnChanges(aiData) post-mount refreshes pré-fill if form empty', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi: FamilleAiData = { dureeMariageAnnees: 7, revenusAnnuelsEpoux1Eur: 35000 };
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.dureeMariageAnnees()).toBe(7);
    expect(component.revenusAnnuelsEpoux1Eur()).toBe(35000);
    expect(component.provenanceDureeMariage()).toBe('IA');
    expect(component.provenanceRevenus1()).toBe('IA');
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherence alert REVENUS_EPOUX1 if divergence > 10 % vs IA', () => {
    component.aiData = { revenusAnnuelsEpoux1Eur: 50000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // IA = 50000 ; user types 70000 → 40 % gap
    component.onRevenus1Change(70000);

    const alerts = component.coherenceAlerts();
    expect(alerts.REVENUS_EPOUX1).toBeDefined();
    expect(alerts.REVENUS_EPOUX1!.field).toBe('REVENUS_EPOUX1');
    expect(alerts.REVENUS_EPOUX1!.source).toBe('IA');
    expect(alerts.REVENUS_EPOUX1!.expectedDisplay).toContain('€');
  });

  it('no coherence alert if revenue gap ≤ 10 %', () => {
    component.aiData = { revenusAnnuelsEpoux1Eur: 50000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // IA = 50000 ; user types 52000 → 4 % gap
    component.onRevenus1Change(52000);

    expect(component.coherenceAlerts().REVENUS_EPOUX1).toBeUndefined();
  });

  it('coherence alert DUREE_MARIAGE if IA gap ≥ 1 year', () => {
    component.aiData = { dureeMariageAnnees: 5 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit 5. Avocat passe à 8 → écart de 3 ans.
    component.onDureeMariageChange(8);

    const alerts = component.coherenceAlerts();
    expect(alerts.DUREE_MARIAGE).toBeDefined();
    expect(alerts.DUREE_MARIAGE!.expectedDisplay).toContain('5');
  });

  it('alerts hidden once result rendered (showForm=false)', () => {
    component.aiData = { revenusAnnuelsEpoux1Eur: 50000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onRevenus1Change(70000);
    expect(component.coherenceAlerts().REVENUS_EPOUX1).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().REVENUS_EPOUX1).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Gate workspaceCountry
  // ---------------------------------------------------------------------------

  it('workspaceCountry BELGIQUE → no HTTP call + isFranceGate false', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.isFranceGate()).toBe(false);
    expect(component.formValid()).toBe(false);
  });

  it('workspaceCountry FRANCE → HTTP GET issued', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.isFranceGate()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // F-IA-03 multi-source (F96 + IA)
  // ---------------------------------------------------------------------------

  it('coherence alert MULTI when F96 + IA both diverge on REVENUS_EPOUX2', () => {
    component.aiData = { revenusAnnuelsEpoux2Eur: 30000 };
    component.procedureChecks = [{
      id: 'chk-1', ordre: 1, description: 'Revenus époux 2 attendus',
      statut: 'NON_COMPLIANT',
      critereCode: 'DA_REVENUS_EPOUX2',
      expectedValue: '30000',
    }];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Avocat saisit 50k → divergent IA et F96 (qui s'accordent à 30k).
    component.onRevenus2Change(50000);

    const alert = component.coherenceAlerts().REVENUS_EPOUX2;
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
});
