import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { AncienneteSectionComponent } from './anciennete-section.component';

describe('AncienneteSectionComponent', () => {
  let component: AncienneteSectionComponent;
  let fixture: ComponentFixture<AncienneteSectionComponent>;
  let httpMock: HttpTestingController;

  const CASE_FILE_ID = '44444444-4444-4444-4444-444444444444';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/anciennete`;

  const MOCK_RESPONSE = {
    caseFileId: CASE_FILE_ID,
    conventionCode: 'METALLURGIE',
    conventionLabel: 'Métallurgie',
    country: 'FRANCE',
    ancienneteAnnees: 10,
    ancienneteMois: 3,
    congesLegauxJours: 25,
    congesSupplementairesJours: 2,
    congesTotalJours: 27,
    primeAnciennetePourcentage: 9,
    primeAncienneteMontant: 270,
    ecarts: [
      { champ: 'Congés annuels', attendu: '27 jours', contractuel: '25 jours', verdict: 'ECART' },
      { champ: 'Prime ancienneté', attendu: '9%', contractuel: '5%', verdict: 'ECART' }
    ]
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AncienneteSectionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AncienneteSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => { httpMock.verify(); });

  function initNoExisting(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
  }

  function initWithExisting(resp = MOCK_RESPONSE): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
  }

  it('should create', () => {
    initNoExisting();
    expect(component).toBeTruthy();
  });

  it('should call GET on init', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush(null, { status: 404, statusText: 'Not Found' });
  });

  it('should show form when no existing', () => {
    initNoExisting();
    expect(component.showForm()).toBe(true);
  });

  it('should call POST when calculate() is called', () => {
    initNoExisting();
    component.conventionCode.set('METALLURGIE');
    component.dateEntree.set('2016-01-01');
    component.salaireBase.set(3000);
    component.congesContrat.set(25);
    component.primeContrat.set(5);
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    req.flush(MOCK_RESPONSE);

    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
  });

  it('should display existing result from GET', () => {
    initWithExisting();
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.conventionCode()).toBe('METALLURGIE');
  });

  // ---- Coherence alerts (SF-IA-03-04) ----

  function setAi(overrides: Partial<{
    conventionCollective: string | null; dateEntree: string | null;
    salaireBrutMensuel: number | null; congesContractuels: number | null;
    primeAncienneteContractuelle: number | null;
  }>) {
    component.aiData = {
      conventionCollective: null, dateEntree: null, salaireBrutMensuel: null,
      typeContrat: null, poste: null, motifLicenciement: null, dateLicenciement: null,
      congesContractuels: null, primeAncienneteContractuelle: null,
      ...overrides,
    } as any;
  }

  // CONVENTION
  it('should NOT alert when convention matches AI', () => {
    setAi({ conventionCollective: 'SYNTEC' });
    initNoExisting();
    component.conventionCode.set('SYNTEC');
    expect(component.coherenceAlerts().CONVENTION).toBeUndefined();
  });

  it('should alert CONVENTION mismatch', () => {
    setAi({ conventionCollective: 'SYNTEC' });
    initNoExisting();
    component.conventionCode.set('METALLURGIE');
    expect(component.coherenceAlerts().CONVENTION?.iaValue).toBe('SYNTEC');
  });

  it('should NOT alert convention when case differs (normalized)', () => {
    setAi({ conventionCollective: 'syntec' });
    initNoExisting();
    component.conventionCode.set('SYNTEC');
    expect(component.coherenceAlerts().CONVENTION).toBeUndefined();
  });

  it('should NOT alert convention when AI is null', () => {
    setAi({ conventionCollective: null });
    initNoExisting();
    component.conventionCode.set('METALLURGIE');
    expect(component.coherenceAlerts().CONVENTION).toBeUndefined();
  });

  // DATE_ENTREE
  it('should NOT alert date when gap < 15 days', () => {
    setAi({ dateEntree: '2018-01-01' });
    initNoExisting();
    component.dateEntree.set('2018-01-10');
    expect(component.coherenceAlerts().DATE_ENTREE).toBeUndefined();
  });

  it('should alert date when gap ≥ 15 days', () => {
    setAi({ dateEntree: '2018-01-01' });
    initNoExisting();
    component.dateEntree.set('2018-01-20');
    expect(component.coherenceAlerts().DATE_ENTREE?.iaValue).toBe('2018-01-01');
  });

  it('should NOT alert date when avocat empty', () => {
    setAi({ dateEntree: '2018-01-01' });
    initNoExisting();
    component.dateEntree.set('');
    expect(component.coherenceAlerts().DATE_ENTREE).toBeUndefined();
  });

  it('should NOT alert when AI date malformed', () => {
    setAi({ dateEntree: 'not-a-date' });
    initNoExisting();
    component.dateEntree.set('2018-01-20');
    expect(component.coherenceAlerts().DATE_ENTREE).toBeUndefined();
  });

  // SALAIRE
  it('should NOT alert salaire when diff < 5%', () => {
    setAi({ salaireBrutMensuel: 4000 });
    initNoExisting();
    component.salaireBase.set(4100);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('should alert salaire when diff ≥ 5%', () => {
    setAi({ salaireBrutMensuel: 4000 });
    initNoExisting();
    component.salaireBase.set(4300);
    expect(component.coherenceAlerts().SALAIRE?.iaValue).toBe('4000 €');
  });

  it('should NOT alert salaire when avocat = 0', () => {
    setAi({ salaireBrutMensuel: 4000 });
    initNoExisting();
    component.salaireBase.set(0);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  // CONGES
  it('should NOT alert conges when match', () => {
    setAi({ congesContractuels: 25 });
    initNoExisting();
    component.congesContrat.set(25);
    expect(component.coherenceAlerts().CONGES).toBeUndefined();
  });

  it('should alert conges when diff ≥ 1 day', () => {
    setAi({ congesContractuels: 25 });
    initNoExisting();
    component.congesContrat.set(27);
    expect(component.coherenceAlerts().CONGES?.iaValue).toBe('25 j');
  });

  it('should NOT alert conges when avocat = 0', () => {
    setAi({ congesContractuels: 25 });
    initNoExisting();
    component.congesContrat.set(0);
    expect(component.coherenceAlerts().CONGES).toBeUndefined();
  });

  // PRIME
  it('should NOT alert prime when diff < 0.5pt', () => {
    setAi({ primeAncienneteContractuelle: 5 });
    initNoExisting();
    component.primeContrat.set(5.3);
    expect(component.coherenceAlerts().PRIME).toBeUndefined();
  });

  it('should alert prime when diff ≥ 0.5pt', () => {
    setAi({ primeAncienneteContractuelle: 5 });
    initNoExisting();
    component.primeContrat.set(6);
    expect(component.coherenceAlerts().PRIME?.iaValue).toBe('5 %');
  });

  it('should NOT alert prime when AI null', () => {
    setAi({ primeAncienneteContractuelle: null });
    initNoExisting();
    component.primeContrat.set(5);
    expect(component.coherenceAlerts().PRIME).toBeUndefined();
  });

  // Transverses
  it('should produce no alerts when aiData is absent', () => {
    initNoExisting();
    component.conventionCode.set('METALLURGIE');
    component.salaireBase.set(9999);
    expect(component.alertsSummary().total).toBe(0);
  });

  it('should count multiple alerts correctly', () => {
    setAi({ conventionCollective: 'SYNTEC', salaireBrutMensuel: 4000, congesContractuels: 25 });
    initNoExisting();
    component.conventionCode.set('METALLURGIE');
    component.salaireBase.set(4500);
    component.congesContrat.set(27);
    expect(component.alertsSummary().total).toBe(3);
  });

  it('should suppress alerts once a result is loaded (result fige les alertes)', () => {
    setAi({ conventionCollective: 'SYNTEC' });
    initWithExisting();
    component.conventionCode.set('METALLURGIE');
    expect(component.coherenceAlerts().CONVENTION).toBeUndefined();
  });
});
