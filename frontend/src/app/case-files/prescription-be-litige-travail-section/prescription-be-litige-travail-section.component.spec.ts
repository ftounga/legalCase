import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { PrescriptionBeLitigeTravailSectionComponent } from './prescription-be-litige-travail-section.component';
import { PrescriptionBeLitigeTravailResponse } from '../../core/models/prescription-be-litige-travail.model';

describe('PrescriptionBeLitigeTravailSectionComponent', () => {
  let component: PrescriptionBeLitigeTravailSectionComponent;
  let fixture: ComponentFixture<PrescriptionBeLitigeTravailSectionComponent>;
  let httpMock: HttpTestingController;
  let refreshSpy: { triggerRefresh: jest.Mock };

  const CASE_FILE_ID = '22222222-2222-2222-2222-222222222222';
  const API_URL =
    `/api/v1/case-files/${CASE_FILE_ID}/decision-tools/prescription-be-litige-travail`;

  const MOCK_RESPONSE: PrescriptionBeLitigeTravailResponse = {
    caseFileId: CASE_FILE_ID,
    dateRupture: '2025-06-15',
    typeCreance: 'EX_CONTRAT_GENERAL',
    dateActionEnvisagee: '2026-03-10',
    verdict: 'NON_PRESCRIT',
    dateLimitePrescription: '2026-06-15',
    joursRestants: 97,
    regleAppliquee: '1 an post-rupture — Loi 03/07/1978 art. 15 al. 1',
    baseJuridique: 'Loi 03/07/1978 art. 15 al. 1',
    formuleCalcul: 'dateRupture + 1 an',
    country: 'BELGIQUE',
  };

  beforeEach(async () => {
    refreshSpy = { triggerRefresh: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [PrescriptionBeLitigeTravailSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PrescriptionBeLitigeTravailSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
    component.workspaceCountry = 'BELGIQUE';
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  function flushSE(): void {
    httpMock.match(r => r.url.endsWith('/source-explanations')).forEach(r => r.flush([]));
  }

  function initWithNoExistingResult(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
    flushSE();
  }

  function initWithExistingResult(resp: PrescriptionBeLitigeTravailResponse = MOCK_RESPONSE): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
    flushSE();
  }

  it('should create', () => {
    initWithNoExistingResult();
    expect(component).toBeTruthy();
    expect(component.isAvailable()).toBe(true);
  });

  it('should NOT call GET when workspace is FRANCE (gate strict BE)', () => {
    component.workspaceCountry = 'FRANCE';
    fixture.detectChanges();
    httpMock.expectNone(API_URL);
    expect(component.isAvailable()).toBe(false);
  });

  it('should show form when no existing result (404)', () => {
    initWithNoExistingResult();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('should display existing result from GET', () => {
    initWithExistingResult();
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.dateRupture()).toBe('2025-06-15');
    expect(component.typeCreance()).toBe('EX_CONTRAT_GENERAL');
    // Provenance jamais IA quand reload depuis backend.
    expect(component.provenanceDateRupture()).toBeNull();
    expect(component.provenanceTypeCreance()).toBeNull();
  });

  // -- Pré-fill IA --

  it('should prefill dateRupture from aiData.dateRuptureContrat', () => {
    component.aiData = { dateRuptureContrat: '2025-09-30' } as any;
    initWithNoExistingResult();
    expect(component.dateRupture()).toBe('2025-09-30');
    expect(component.provenanceDateRupture()).toBe('IA');
  });

  it('should prefill typeCreance from aiData.motifRupture (mapping licenciement)', () => {
    component.aiData = { motifRupture: 'licenciement pour motif grave' } as any;
    initWithNoExistingResult();
    expect(component.typeCreance()).toBe('EX_CONTRAT_GENERAL');
    expect(component.provenanceTypeCreance()).toBe('IA');
  });

  it('should NOT prefill typeCreance when motifRupture is unmapped', () => {
    component.aiData = { motifRupture: 'fin de CDD' } as any;
    initWithNoExistingResult();
    expect(component.typeCreance()).toBeNull();
    expect(component.provenanceTypeCreance()).toBeNull();
  });

  it('should prefill both fields nominal case', () => {
    component.aiData = {
      dateRuptureContrat: '2025-06-15',
      motifRupture: 'Démission',
    } as any;
    initWithNoExistingResult();
    expect(component.dateRupture()).toBe('2025-06-15');
    expect(component.typeCreance()).toBe('EX_CONTRAT_GENERAL');
    expect(component.provenanceDateRupture()).toBe('IA');
    expect(component.provenanceTypeCreance()).toBe('IA');
  });

  it('should NOT prefill if workspaceCountry is FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = {
      dateRuptureContrat: '2025-06-15',
      motifRupture: 'licenciement',
    } as any;
    fixture.detectChanges();
    httpMock.expectNone(API_URL);
    expect(component.dateRupture()).toBeNull();
    expect(component.typeCreance()).toBeNull();
  });

  // -- Provenance cleared on manual edit --

  it('should clear provenance on manual dateRupture change', () => {
    component.aiData = { dateRuptureContrat: '2025-06-15' } as any;
    initWithNoExistingResult();
    expect(component.provenanceDateRupture()).toBe('IA');
    component.onDateRuptureChange('2025-07-01');
    expect(component.provenanceDateRupture()).toBeNull();
    expect(component.dateRupture()).toBe('2025-07-01');
  });

  it('should clear provenance on manual typeCreance change', () => {
    component.aiData = { motifRupture: 'licenciement' } as any;
    initWithNoExistingResult();
    expect(component.provenanceTypeCreance()).toBe('IA');
    component.onTypeCreanceChange('PENDANT_CONTRAT');
    expect(component.provenanceTypeCreance()).toBeNull();
    expect(component.typeCreance()).toBe('PENDANT_CONTRAT');
  });

  // -- POST calculate --

  it('should POST with form payload and display verdict on success', () => {
    initWithNoExistingResult();
    component.dateRupture.set('2025-06-15');
    component.typeCreance.set('EX_CONTRAT_GENERAL');
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateRupture).toBe('2025-06-15');
    expect(req.request.body.typeCreance).toBe('EX_CONTRAT_GENERAL');
    expect(req.request.body.dateActionEnvisagee).toBeUndefined();

    req.flush(MOCK_RESPONSE);
    expect(component.result()).toBeTruthy();
    expect(component.result()!.verdict).toBe('NON_PRESCRIT');
    expect(component.showForm()).toBe(false);
  });

  it('should send dateActionEnvisagee when provided', () => {
    initWithNoExistingResult();
    component.dateRupture.set('2025-06-15');
    component.typeCreance.set('EX_CONTRAT_GENERAL');
    component.dateActionEnvisagee.set('2026-03-10');
    component.calculate();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.body.dateActionEnvisagee).toBe('2026-03-10');
    req.flush(MOCK_RESPONSE);
  });

  it('should NOT POST when form is invalid', () => {
    initWithNoExistingResult();
    // dateRupture missing
    component.typeCreance.set('EX_CONTRAT_GENERAL');
    component.calculate();
    httpMock.expectNone(API_URL);
    expect(component.calculating()).toBe(false);
  });

  // -- Refresh dashboard on success --

  it('should trigger dashboard refresh on successful POST', () => {
    initWithNoExistingResult();
    component.dateRupture.set('2025-06-15');
    component.typeCreance.set('EX_CONTRAT_GENERAL');
    component.calculate();
    httpMock.expectOne(API_URL).flush(MOCK_RESPONSE);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  // -- Error handling --

  it('should show snackbar on 500', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.dateRupture.set('2025-06-15');
    component.typeCreance.set('EX_CONTRAT_GENERAL');
    component.calculate();
    httpMock.expectOne(API_URL).flush('boom', { status: 500, statusText: 'Server Error' });
    expect(spy).toHaveBeenCalled();
    expect(component.calculating()).toBe(false);
  });

  it('should show snackbar with "Dossier introuvable" on 404 POST', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.dateRupture.set('2025-06-15');
    component.typeCreance.set('EX_CONTRAT_GENERAL');
    component.calculate();
    httpMock.expectOne(API_URL).flush('not found', { status: 404, statusText: 'Not Found' });
    expect(spy).toHaveBeenCalledWith('Dossier introuvable', 'Fermer', expect.any(Object));
  });

  it('should refuse calculate when workspaceCountry FR (defense in depth)', () => {
    component.workspaceCountry = 'BELGIQUE';
    initWithNoExistingResult();
    component.dateRupture.set('2025-06-15');
    component.typeCreance.set('EX_CONTRAT_GENERAL');
    // force the gate
    component.workspaceCountry = 'FRANCE';
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    component.calculate();
    httpMock.expectNone(API_URL);
    expect(spy).toHaveBeenCalled();
  });

  // -- Coherence alerts --

  it('should alert DATE_RUPTURE when IA diverges > 15 days', () => {
    component.aiData = { dateRuptureContrat: '2025-06-15' } as any;
    initWithNoExistingResult();
    // user typed a date 30 days later
    component.dateRupture.set('2025-07-15');
    const alert = component.coherenceAlerts().DATE_RUPTURE;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('2025-06-15');
  });

  it('should NOT alert DATE_RUPTURE when within tolerance', () => {
    component.aiData = { dateRuptureContrat: '2025-06-15' } as any;
    initWithNoExistingResult();
    component.dateRupture.set('2025-06-20'); // < 15 jours
    expect(component.coherenceAlerts().DATE_RUPTURE).toBeUndefined();
  });

  it('should alert TYPE_CREANCE when IA mapping diverges from user choice', () => {
    component.aiData = { motifRupture: 'licenciement' } as any;
    initWithNoExistingResult();
    // IA → EX_CONTRAT_GENERAL ; user picks PENDANT_CONTRAT
    component.typeCreance.set('PENDANT_CONTRAT');
    const alert = component.coherenceAlerts().TYPE_CREANCE;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('EX_CONTRAT_GENERAL');
  });

  it('should freeze alerts when result is displayed (showForm=false)', () => {
    component.aiData = {
      dateRuptureContrat: '2025-06-15',
      motifRupture: 'licenciement',
    } as any;
    initWithExistingResult({ ...MOCK_RESPONSE, dateRupture: '2025-12-01' });
    // Even though dateRupture diverges, alerts are frozen (showForm=false).
    expect(component.showForm()).toBe(false);
    expect(component.alertsSummary().total).toBe(0);
  });

  // -- editMode reopens the form --

  it('editMode reopens form, alerts re-active', () => {
    component.aiData = { dateRuptureContrat: '2025-06-15' } as any;
    initWithExistingResult({ ...MOCK_RESPONSE, dateRupture: '2025-12-01' });
    expect(component.showForm()).toBe(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
    // Diff > 15 jours → alerte attendue
    component.dateRupture.set('2025-12-01');
    expect(component.coherenceAlerts().DATE_RUPTURE?.source).toBe('IA');
  });
});

// F-177 SF-177-12 / F-236 SF-236-02 — couvre le static `getPrefillCount`
// exposé pour la card du panel F-IA-04.
describe('PrescriptionBeLitigeTravailSectionComponent.getPrefillCount', () => {
  it('returns 0 when no aiData', () => {
    expect(PrescriptionBeLitigeTravailSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('returns 1 when only dateRuptureContrat is set (ISO)', () => {
    expect(
      PrescriptionBeLitigeTravailSectionComponent.getPrefillCount({
        aiData: { dateRuptureContrat: '2025-06-15' },
      }),
    ).toBe(1);
  });

  it('returns 2 nominal — date + motif mapped', () => {
    expect(
      PrescriptionBeLitigeTravailSectionComponent.getPrefillCount({
        aiData: { dateRuptureContrat: '2025-06-15', motifRupture: 'licenciement' },
      }),
    ).toBe(2);
  });
});
