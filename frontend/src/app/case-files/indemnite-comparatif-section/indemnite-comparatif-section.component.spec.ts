import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { IndemniteComparatifSectionComponent } from './indemnite-comparatif-section.component';

describe('IndemniteComparatifSectionComponent', () => {
  let component: IndemniteComparatifSectionComponent;
  let fixture: ComponentFixture<IndemniteComparatifSectionComponent>;
  let httpMock: HttpTestingController;

  const CASE_FILE_ID = '66666666-6666-6666-6666-666666666666';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/indemnite-comparatif`;

  const MOCK = {
    caseFileId: CASE_FILE_ID, country: 'FRANCE', ancienneteAnnees: 10, age: 40, salaireMensuel: 3000,
    baremePlancherMois: 3, baremePlafondMois: 10,
    fourchetteBasseMois: 4.75, fourchetteMedMois: 6.85, fourhetteHauteMois: 8.95,
    fourchetteBasseMontant: 14250, fourchetteMedMontant: 20550, fourhetteHauteMontant: 26850,
    baremeSource: 'Barème Macron', commentaire: 'Fourchette indicative'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IndemniteComparatifSectionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(IndemniteComparatifSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => { httpMock.verify(); });

  function initNo(): void { fixture.detectChanges(); httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'NF' }); }
  function initWith(r = MOCK): void { fixture.detectChanges(); httpMock.expectOne(API_URL).flush(r); }

  it('should create', () => { initNo(); expect(component).toBeTruthy(); });
  it('should call GET on init', () => { fixture.detectChanges(); const r = httpMock.expectOne(API_URL); expect(r.request.method).toBe('GET'); r.flush(null, { status: 404, statusText: 'NF' }); });
  it('should show form when no existing', () => { initNo(); expect(component.showForm()).toBe(true); });

  it('should call POST when calculate()', () => {
    initNo();
    component.calculate();
    const r = httpMock.expectOne(API_URL);
    expect(r.request.method).toBe('POST');
    r.flush(MOCK);
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
  });

  it('should display existing from GET', () => {
    initWith();
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
  });

  // ---- Type de rupture (SF-DT-09-04) ----

  it('should list 3 FR options when country is FRANCE', () => {
    initNo();
    component.country.set('FRANCE');
    expect(component.typeRuptureOptions().map(o => o.value)).toEqual([
      'LICENCIEMENT', 'LICENCIEMENT_ECONOMIQUE', 'RUPTURE_CONVENTIONNELLE'
    ]);
  });

  it('should list 2 BE options when country is BELGIQUE', () => {
    initNo();
    component.country.set('BELGIQUE');
    expect(component.typeRuptureOptions().map(o => o.value)).toEqual([
      'LICENCIEMENT_ORDINAIRE', 'RUPTURE_AMIABLE'
    ]);
  });

  it('should reset typeRupture when country changes and current value is incompatible', () => {
    initNo();
    component.country.set('FRANCE');
    component.typeRupture.set('RUPTURE_CONVENTIONNELLE');
    component.country.set('BELGIQUE');
    component.onCountryChange();
    expect(component.typeRupture()).toBe('LICENCIEMENT_ORDINAIRE');
  });

  it('should send typeRupture in POST payload', () => {
    initNo();
    component.typeRupture.set('RUPTURE_CONVENTIONNELLE');
    component.calculate();
    const r = httpMock.expectOne(API_URL);
    expect(r.request.body.typeRupture).toBe('RUPTURE_CONVENTIONNELLE');
    r.flush({ ...MOCK, typeRupture: 'RUPTURE_CONVENTIONNELLE', displayMode: 'INDEMNITE_SPECIFIQUE', indemniteLegaleMontant: 12500, contextualMessages: [] });
  });

  it('should prefill typeRupture from compensationEstimate', () => {
    component.synthesis = {
      compensationEstimate: { typeRupture: 'RUPTURE_CONVENTIONNELLE' }
    } as any;
    initNo();
    expect(component.typeRupture()).toBe('RUPTURE_CONVENTIONNELLE');
    expect(component.typeRuptureNote()).toBeNull();
  });

  it('should set note when AI type is unsupported', () => {
    component.synthesis = {
      compensationEstimate: { typeRupture: 'DEMISSION' }
    } as any;
    initNo();
    expect(component.typeRupture()).toBe('LICENCIEMENT');
    expect(component.typeRuptureNote()).toContain('DEMISSION');
  });

  it('should fallback typeRupture for legacy result without type', () => {
    const legacyResp = { ...MOCK, typeRupture: null, displayMode: 'MACRON', indemniteLegaleMontant: null, contextualMessages: [] };
    initWith(legacyResp);
    expect(component.typeRupture()).toBe('LICENCIEMENT');
  });

  it('should restore typeRupture from existing result', () => {
    const resp = { ...MOCK, typeRupture: 'LICENCIEMENT_ECONOMIQUE', displayMode: 'MACRON', indemniteLegaleMontant: null, contextualMessages: [] };
    initWith(resp);
    expect(component.typeRupture()).toBe('LICENCIEMENT_ECONOMIQUE');
  });

  // ---- Coherence alerts (SF-IA-03-05) ----

  function f96(overrides: Partial<{ id: string; statut: string; critereCode: string; expectedValue: string; raison: string }>) {
    return {
      id: overrides.id ?? 'c-' + Math.random(),
      ordre: 0,
      description: 'point',
      statut: overrides.statut ?? 'TO_CHECK',
      raison: overrides.raison ?? null,
      critereCode: overrides.critereCode ?? null,
      expectedValue: overrides.expectedValue ?? null,
    } as any;
  }

  function question(overrides: Partial<{ questionText: string; answerText: string; critereCode: string; expectedValue: string }>) {
    return {
      id: 'q-' + Math.random(),
      orderIndex: 0,
      questionText: overrides.questionText ?? 'Q?',
      answerText: overrides.answerText ?? null,
      critereCode: overrides.critereCode ?? null,
      expectedValue: overrides.expectedValue ?? null,
    } as any;
  }

  // TYPE_RUPTURE
  it('should alert blocker F96 on TYPE_RUPTURE mismatch', () => {
    component.procedureChecks = [f96({ statut: 'VERIFIED', critereCode: 'DT09_TYPE_RUPTURE', expectedValue: 'RUPTURE_CONVENTIONNELLE', raison: 'Convention jointe' })];
    initNo();
    component.typeRupture.set('LICENCIEMENT');
    const alert = component.coherenceAlerts().TYPE_RUPTURE;
    expect(alert?.source).toBe('F96');
    expect(alert?.level).toBe('blocker');
    expect(alert?.expectedDisplay).toBe('RUPTURE_CONVENTIONNELLE');
  });

  it('should ignore F-96 NON_COMPLIANT on enum critere', () => {
    component.procedureChecks = [f96({ statut: 'NON_COMPLIANT', critereCode: 'DT09_TYPE_RUPTURE', expectedValue: 'RUPTURE_CONVENTIONNELLE' })];
    initNo();
    component.typeRupture.set('LICENCIEMENT');
    expect(component.coherenceAlerts().TYPE_RUPTURE).toBeUndefined();
  });

  it('should alert blocker QUESTION_IA on "oui" + expected', () => {
    component.aiQuestions = [question({ answerText: 'oui', critereCode: 'DT09_TYPE_RUPTURE', expectedValue: 'LICENCIEMENT_ECONOMIQUE' })];
    initNo();
    component.typeRupture.set('LICENCIEMENT');
    const alert = component.coherenceAlerts().TYPE_RUPTURE;
    expect(alert?.source).toBe('QUESTION_IA');
    expect(alert?.expectedDisplay).toBe('LICENCIEMENT_ECONOMIQUE');
  });

  it('should ignore QUESTION_IA "non" on enum critere', () => {
    component.aiQuestions = [question({ answerText: 'non', critereCode: 'DT09_TYPE_RUPTURE', expectedValue: 'RUPTURE_CONVENTIONNELLE' })];
    initNo();
    component.typeRupture.set('LICENCIEMENT');
    expect(component.coherenceAlerts().TYPE_RUPTURE).toBeUndefined();
  });

  it('should prefer F96 over QUESTION_IA on TYPE_RUPTURE', () => {
    component.procedureChecks = [f96({ statut: 'VERIFIED', critereCode: 'DT09_TYPE_RUPTURE', expectedValue: 'RUPTURE_CONVENTIONNELLE' })];
    component.aiQuestions = [question({ answerText: 'oui', critereCode: 'DT09_TYPE_RUPTURE', expectedValue: 'LICENCIEMENT_ECONOMIQUE' })];
    initNo();
    component.typeRupture.set('LICENCIEMENT');
    const alert = component.coherenceAlerts().TYPE_RUPTURE;
    expect(alert?.expectedDisplay).toBe('RUPTURE_CONVENTIONNELLE');
  });

  it('should combine sources into MULTI when they align', () => {
    component.procedureChecks = [f96({ statut: 'VERIFIED', critereCode: 'DT09_TYPE_RUPTURE', expectedValue: 'RUPTURE_CONVENTIONNELLE' })];
    component.aiQuestions = [question({ answerText: 'oui', critereCode: 'DT09_TYPE_RUPTURE', expectedValue: 'RUPTURE_CONVENTIONNELLE' })];
    component.synthesis = { compensationEstimate: { typeRupture: 'RUPTURE_CONVENTIONNELLE' } } as any;
    initNo();
    component.typeRupture.set('LICENCIEMENT');
    const alert = component.coherenceAlerts().TYPE_RUPTURE;
    expect(alert?.source).toBe('MULTI');
    expect(alert?.contributors).toEqual(expect.arrayContaining(['F96', 'QUESTION_IA', 'IA']));
  });

  it('should alert blocker IA alone when no F96/question', () => {
    component.synthesis = { compensationEstimate: { typeRupture: 'RUPTURE_CONVENTIONNELLE' } } as any;
    initNo();
    component.typeRupture.set('LICENCIEMENT');
    const alert = component.coherenceAlerts().TYPE_RUPTURE;
    expect(alert?.source).toBe('IA');
    expect(alert?.level).toBe('blocker');
  });

  it('should not alert when IA type is unknown enum value', () => {
    component.synthesis = { compensationEstimate: { typeRupture: 'DEMISSION' } } as any;
    initNo();
    component.typeRupture.set('LICENCIEMENT');
    expect(component.coherenceAlerts().TYPE_RUPTURE).toBeUndefined();
  });

  // ANCIENNETE
  it('should NOT alert anciennete when within 0.5 year', () => {
    component.synthesis = { compensationEstimate: { ancienneteAnnees: 10, ancienneteMois: 3 } } as any;
    initNo();
    component.ancienneteAnnees.set(10);
    expect(component.coherenceAlerts().ANCIENNETE).toBeUndefined();
  });

  it('should alert anciennete when gap ≥ 0.5 year', () => {
    component.synthesis = { compensationEstimate: { ancienneteAnnees: 10, ancienneteMois: 0 } } as any;
    initNo();
    component.ancienneteAnnees.set(11);
    const alert = component.coherenceAlerts().ANCIENNETE;
    expect(alert?.level).toBe('warning');
    expect(alert?.source).toBe('IA');
  });

  it('should NOT alert anciennete when user = 0', () => {
    component.synthesis = { compensationEstimate: { ancienneteAnnees: 10 } } as any;
    initNo();
    component.ancienneteAnnees.set(0);
    expect(component.coherenceAlerts().ANCIENNETE).toBeUndefined();
  });

  // SALAIRE
  it('should NOT alert salaire when diff < 5%', () => {
    component.synthesis = { compensationEstimate: { salaireReference: 4000 } } as any;
    initNo();
    component.salaireMensuel.set(4100);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('should alert salaire when diff ≥ 5%', () => {
    component.synthesis = { compensationEstimate: { salaireReference: 4000 } } as any;
    initNo();
    component.salaireMensuel.set(4300);
    const alert = component.coherenceAlerts().SALAIRE;
    expect(alert?.level).toBe('warning');
  });

  // Summary
  it('should count blockers and warnings in summary', () => {
    component.synthesis = {
      compensationEstimate: { typeRupture: 'RUPTURE_CONVENTIONNELLE', ancienneteAnnees: 10, ancienneteMois: 0, salaireReference: 4000 }
    } as any;
    initNo();
    component.typeRupture.set('LICENCIEMENT');
    component.ancienneteAnnees.set(12);
    component.salaireMensuel.set(5000);
    expect(component.alertsSummary()).toEqual({ total: 3, blockers: 1 });
  });

  it('SF-IA-03-12: alertes actives après Comparer → editForm → modification', () => {
    // Résultat chargé via GET, l'avocat clique Modifier pour corriger le type,
    // le badge de cohérence doit réapparaître en temps réel.
    component.synthesis = { compensationEstimate: { typeRupture: 'RUPTURE_CONVENTIONNELLE' } } as any;
    initWith();
    component.editForm();
    component.typeRupture.set('LICENCIEMENT');
    const alert = component.coherenceAlerts().TYPE_RUPTURE;
    expect(alert).toBeTruthy();
    expect(alert?.expectedDisplay).toBe('RUPTURE_CONVENTIONNELLE');
  });

  it('SF-IA-03-12: pas d\'alertes quand le bloc résultat est affiché (showForm=false)', () => {
    component.synthesis = { compensationEstimate: { typeRupture: 'RUPTURE_CONVENTIONNELLE' } } as any;
    initWith();
    expect(component.showForm()).toBe(false);
    expect(component.coherenceAlerts().TYPE_RUPTURE).toBeUndefined();
  });
});
