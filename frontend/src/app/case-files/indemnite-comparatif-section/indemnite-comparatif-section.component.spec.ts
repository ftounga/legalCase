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
});
