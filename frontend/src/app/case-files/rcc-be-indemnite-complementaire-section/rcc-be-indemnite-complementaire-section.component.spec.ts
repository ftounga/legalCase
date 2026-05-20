import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { RccBeIndemniteComplementaireSectionComponent } from './rcc-be-indemnite-complementaire-section.component';
import { RccBeIndemniteComplementaireResponse } from '../../core/models/rcc-be-indemnite-complementaire.model';

describe('RccBeIndemniteComplementaireSectionComponent', () => {
  let component: RccBeIndemniteComplementaireSectionComponent;
  let fixture: ComponentFixture<RccBeIndemniteComplementaireSectionComponent>;
  let httpMock: HttpTestingController;
  let refreshSpy: { triggerRefresh: jest.Mock };

  const CASE_FILE_ID = '66666666-6666-6666-6666-666666666666';
  const API_URL =
    `/api/v1/case-files/${CASE_FILE_ID}/decision-tools/rcc-be-indemnite-complementaire`;

  const NOMINAL_RESPONSE: RccBeIndemniteComplementaireResponse = {
    caseFileId: CASE_FILE_ID,
    remunerationNetteReference: 2500,
    allocationOnemMensuelle: 1500,
    dateNaissanceSalarie: '1962-06-12',
    dateDebutRcc: '2026-09-01',
    ageLegalPensionApplique: 66,
    planchersSectoriel: null,
    indemniteMensuelleAvantPlancher: 500,
    indemniteMensuelleEmployeur: 500,
    planchersSectorielApplique: false,
    dateAgeLegalPension: '2028-06-12',
    moisRestantsJusquaPension: 21,
    montantTotalEmployeur: 10500,
    baseJuridique: 'CCT 17 art. 5 ; loi du 3 juillet 1978 ; arrêté royal du 3 mai 2007',
    formuleCalcul: '(2500.00 − 1500.00) / 2 = 500.00 €/mois × 21 mois = 10500.00 €',
    country: 'BELGIQUE',
  };

  const PLANCHER_APPLIQUE_RESPONSE: RccBeIndemniteComplementaireResponse = {
    ...NOMINAL_RESPONSE,
    indemniteMensuelleAvantPlancher: 500,
    planchersSectoriel: 750,
    indemniteMensuelleEmployeur: 750,
    planchersSectorielApplique: true,
    montantTotalEmployeur: 15750,
  };

  const AUCUNE_INDEMNITE_RESPONSE: RccBeIndemniteComplementaireResponse = {
    ...NOMINAL_RESPONSE,
    remunerationNetteReference: 1500,
    allocationOnemMensuelle: 1500,
    indemniteMensuelleAvantPlancher: 0,
    indemniteMensuelleEmployeur: 0,
    montantTotalEmployeur: 0,
    formuleCalcul: '(1500.00 − 1500.00) / 2 = 0 €/mois (plancher 0)',
  };

  const RCC_APRES_PENSION_RESPONSE: RccBeIndemniteComplementaireResponse = {
    ...NOMINAL_RESPONSE,
    dateDebutRcc: '2030-01-01',
    moisRestantsJusquaPension: 0,
    montantTotalEmployeur: 0,
    formuleCalcul: 'RCC après âge légal de pension — moisRestants = 0',
  };

  beforeEach(async () => {
    refreshSpy = { triggerRefresh: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [RccBeIndemniteComplementaireSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RccBeIndemniteComplementaireSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
    component.workspaceCountry = 'BELGIQUE';
  });

  afterEach(() => httpMock.verify());

  function initWithNoExistingResult(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
  }

  function initWithExistingResult(resp: RccBeIndemniteComplementaireResponse = NOMINAL_RESPONSE): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
  }

  // ── Création / gate BE ───────────────────────────────────────────────

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

  it('should display existing result from GET (no IA badge on reload)', () => {
    initWithExistingResult();
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.remunerationNetteReference()).toBe(2500);
    expect(component.allocationOnemMensuelle()).toBe(1500);
    expect(component.dateNaissanceSalarie()).toBe('1962-06-12');
    expect(component.dateDebutRcc()).toBe('2026-09-01');
    expect(component.ageLegalPension()).toBe(66);
    // Reload backend = saisie avocat = jamais de badge IA.
    expect(component.provenanceRemunerationNette()).toBeNull();
    expect(component.provenanceAllocationOnem()).toBeNull();
    expect(component.provenanceDateNaissance()).toBeNull();
    expect(component.provenanceDateDebutRcc()).toBeNull();
  });

  // ── Pré-fill IA — 4 champs ──────────────────────────────────────────

  it('should prefill all 4 instrumented fields from full aiData', () => {
    component.aiData = {
      remunerationNetteReferenceRccDetectee: 2500,
      allocationOnemMensuelleEstimee: 1500,
      dateNaissanceSalarie: '1962-06-12',
      dateDebutRccEnvisagee: '2026-09-01',
    } as any;
    initWithNoExistingResult();
    expect(component.remunerationNetteReference()).toBe(2500);
    expect(component.allocationOnemMensuelle()).toBe(1500);
    expect(component.dateNaissanceSalarie()).toBe('1962-06-12');
    expect(component.dateDebutRcc()).toBe('2026-09-01');
    expect(component.provenanceRemunerationNette()).toBe('IA');
    expect(component.provenanceAllocationOnem()).toBe('IA');
    expect(component.provenanceDateNaissance()).toBe('IA');
    expect(component.provenanceDateDebutRcc()).toBe('IA');
  });

  it('should NOT prefill remuneration when aiData says 0 (rejected)', () => {
    component.aiData = {
      remunerationNetteReferenceRccDetectee: 0,
      dateNaissanceSalarie: '1962-06-12',
    } as any;
    initWithNoExistingResult();
    expect(component.remunerationNetteReference()).toBeNull();
    expect(component.provenanceRemunerationNette()).toBeNull();
    // Mais dateNaissance OUI.
    expect(component.dateNaissanceSalarie()).toBe('1962-06-12');
    expect(component.provenanceDateNaissance()).toBe('IA');
  });

  it('should NOT prefill anything if workspaceCountry is FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = { dateNaissanceSalarie: '1962-06-12' } as any;
    fixture.detectChanges();
    httpMock.expectNone(API_URL);
    expect(component.dateNaissanceSalarie()).toBeNull();
  });

  it('should clear provenance on manual remuneration change', () => {
    component.aiData = { remunerationNetteReferenceRccDetectee: 2500 } as any;
    initWithNoExistingResult();
    expect(component.provenanceRemunerationNette()).toBe('IA');
    component.onRemunerationNetteChange(2800);
    expect(component.provenanceRemunerationNette()).toBeNull();
    expect(component.remunerationNetteReference()).toBe(2800);
  });

  it('should clear provenance on manual allocation onem change', () => {
    component.aiData = { allocationOnemMensuelleEstimee: 1500 } as any;
    initWithNoExistingResult();
    expect(component.provenanceAllocationOnem()).toBe('IA');
    component.onAllocationOnemChange(1600);
    expect(component.provenanceAllocationOnem()).toBeNull();
    expect(component.allocationOnemMensuelle()).toBe(1600);
  });

  it('should clear provenance on manual date début RCC change', () => {
    component.aiData = { dateDebutRccEnvisagee: '2026-09-01' } as any;
    initWithNoExistingResult();
    expect(component.provenanceDateDebutRcc()).toBe('IA');
    component.onDateDebutRccChange('2027-01-01');
    expect(component.provenanceDateDebutRcc()).toBeNull();
    expect(component.dateDebutRcc()).toBe('2027-01-01');
  });

  // ── formValid ─────────────────────────────────────────────────────────

  it('formValid requires 4 required fields', () => {
    initWithNoExistingResult();
    expect(component.formValid()).toBe(false);
    component.remunerationNetteReference.set(2500);
    expect(component.formValid()).toBe(false);
    component.allocationOnemMensuelle.set(1500);
    expect(component.formValid()).toBe(false);
    component.dateNaissanceSalarie.set('1962-06-12');
    expect(component.formValid()).toBe(false);
    component.dateDebutRcc.set('2026-09-01');
    expect(component.formValid()).toBe(true);
  });

  it('formValid rejects remuneration <= 0', () => {
    initWithNoExistingResult();
    component.allocationOnemMensuelle.set(1500);
    component.dateNaissanceSalarie.set('1962-06-12');
    component.dateDebutRcc.set('2026-09-01');
    component.remunerationNetteReference.set(0);
    expect(component.formValid()).toBe(false);
    component.remunerationNetteReference.set(-1);
    expect(component.formValid()).toBe(false);
    component.remunerationNetteReference.set(0.01);
    expect(component.formValid()).toBe(true);
  });

  it('formValid rejects allocation onem < 0 (0 toléré)', () => {
    initWithNoExistingResult();
    component.remunerationNetteReference.set(2500);
    component.dateNaissanceSalarie.set('1962-06-12');
    component.dateDebutRcc.set('2026-09-01');
    component.allocationOnemMensuelle.set(0);
    expect(component.formValid()).toBe(true);
    component.allocationOnemMensuelle.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid rejects ageLegalPension hors [60, 75]', () => {
    initWithNoExistingResult();
    component.remunerationNetteReference.set(2500);
    component.allocationOnemMensuelle.set(1500);
    component.dateNaissanceSalarie.set('1962-06-12');
    component.dateDebutRcc.set('2026-09-01');
    expect(component.formValid()).toBe(true);
    component.ageLegalPension.set(59);
    expect(component.formValid()).toBe(false);
    component.ageLegalPension.set(76);
    expect(component.formValid()).toBe(false);
    component.ageLegalPension.set(66);
    expect(component.formValid()).toBe(true);
  });

  // ── POST calculate ────────────────────────────────────────────────────

  it('should NOT POST when required fields missing', () => {
    initWithNoExistingResult();
    component.calculate();
    httpMock.expectNone(API_URL);
  });

  it('should POST and show result on success', () => {
    initWithNoExistingResult();
    component.remunerationNetteReference.set(2500);
    component.allocationOnemMensuelle.set(1500);
    component.dateNaissanceSalarie.set('1962-06-12');
    component.dateDebutRcc.set('2026-09-01');
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.remunerationNetteReference).toBe(2500);
    expect(req.request.body.allocationOnemMensuelle).toBe(1500);
    expect(req.request.body.dateNaissanceSalarie).toBe('1962-06-12');
    expect(req.request.body.dateDebutRcc).toBe('2026-09-01');
    expect(req.request.body.ageLegalPension).toBeNull();
    expect(req.request.body.planchersSectoriel).toBeNull();

    req.flush(NOMINAL_RESPONSE);
    expect(component.result()?.indemniteMensuelleEmployeur).toBe(500);
    expect(component.result()?.planchersSectorielApplique).toBe(false);
    expect(component.showForm()).toBe(false);
  });

  it('should display plancher sectoriel applied + ambre badge data', () => {
    initWithExistingResult(PLANCHER_APPLIQUE_RESPONSE);
    expect(component.result()?.planchersSectorielApplique).toBe(true);
    expect(component.result()?.indemniteMensuelleEmployeur).toBe(750);
    expect(component.result()?.indemniteMensuelleAvantPlancher).toBe(500);
  });

  it('should display 0 indemnité when remun = allocation (plancher 0)', () => {
    initWithExistingResult(AUCUNE_INDEMNITE_RESPONSE);
    expect(component.result()?.indemniteMensuelleEmployeur).toBe(0);
    expect(component.result()?.montantTotalEmployeur).toBe(0);
  });

  it('should display RCC après pension (moisRestants = 0) — info alerte', () => {
    initWithExistingResult(RCC_APRES_PENSION_RESPONSE);
    expect(component.result()?.moisRestantsJusquaPension).toBe(0);
    expect(component.result()?.montantTotalEmployeur).toBe(0);
  });

  it('should trigger dashboard refresh on successful POST', () => {
    initWithNoExistingResult();
    component.remunerationNetteReference.set(2500);
    component.allocationOnemMensuelle.set(1500);
    component.dateNaissanceSalarie.set('1962-06-12');
    component.dateDebutRcc.set('2026-09-01');
    component.calculate();
    httpMock.expectOne(API_URL).flush(NOMINAL_RESPONSE);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  // ── Erreurs ──────────────────────────────────────────────────────────

  it('should show snackbar on 500', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.remunerationNetteReference.set(2500);
    component.allocationOnemMensuelle.set(1500);
    component.dateNaissanceSalarie.set('1962-06-12');
    component.dateDebutRcc.set('2026-09-01');
    component.calculate();
    httpMock.expectOne(API_URL).flush('boom', { status: 500, statusText: 'Server Error' });
    expect(spy).toHaveBeenCalled();
    expect(component.calculating()).toBe(false);
  });

  it('should show snackbar with "Dossier introuvable" on 404 POST', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.remunerationNetteReference.set(2500);
    component.allocationOnemMensuelle.set(1500);
    component.dateNaissanceSalarie.set('1962-06-12');
    component.dateDebutRcc.set('2026-09-01');
    component.calculate();
    httpMock.expectOne(API_URL).flush('not found', { status: 404, statusText: 'Not Found' });
    expect(spy).toHaveBeenCalledWith('Dossier introuvable', 'Fermer', expect.any(Object));
  });

  it('should refuse calculate when workspaceCountry FR (defense in depth)', () => {
    initWithNoExistingResult();
    component.remunerationNetteReference.set(2500);
    component.allocationOnemMensuelle.set(1500);
    component.dateNaissanceSalarie.set('1962-06-12');
    component.dateDebutRcc.set('2026-09-01');
    component.workspaceCountry = 'FRANCE';
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    component.calculate();
    httpMock.expectNone(API_URL);
    expect(spy).toHaveBeenCalled();
  });

  // ── Formatters ────────────────────────────────────────────────────────

  it('formatEuro renders fr-BE currency', () => {
    initWithNoExistingResult();
    const out = component.formatEuro(2500);
    // fr-BE → "2.500,00 €" ou variante NBSP — on cherche le symbole € et la virgule décimale.
    expect(out).toContain('€');
    expect(out).toContain(',');
  });

  it('formatEuro handles null / NaN gracefully', () => {
    initWithNoExistingResult();
    expect(component.formatEuro(null)).toBe('—');
    expect(component.formatEuro(undefined)).toBe('—');
    expect(component.formatEuro(NaN)).toBe('—');
  });

  it('formatDuree covers 0 / mois only / 1 année / N années + reste', () => {
    initWithNoExistingResult();
    expect(component.formatDuree(0)).toBe('0 mois');
    expect(component.formatDuree(5)).toBe('5 mois');
    expect(component.formatDuree(12)).toBe('1 année');
    expect(component.formatDuree(15)).toBe('1 année 3 mois');
    expect(component.formatDuree(24)).toBe('2 années');
    expect(component.formatDuree(30)).toBe('2 années 6 mois');
    expect(component.formatDuree(null)).toBe('—');
  });

  // ── editMode ──────────────────────────────────────────────────────────

  it('editMode reopens form', () => {
    initWithExistingResult();
    expect(component.showForm()).toBe(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });
});

// Couvre le static `getPrefillCount` exposé pour la card du panel F-IA-04.
describe('RccBeIndemniteComplementaireSectionComponent.getPrefillCount', () => {
  it('returns 0 when no aiData', () => {
    expect(RccBeIndemniteComplementaireSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('returns 1 when only dateNaissanceSalarie is set', () => {
    expect(
      RccBeIndemniteComplementaireSectionComponent.getPrefillCount({
        aiData: { dateNaissanceSalarie: '1962-06-12' } as any,
      }),
    ).toBe(1);
  });

  it('returns 4 nominal — tous les champs instrumentés', () => {
    expect(
      RccBeIndemniteComplementaireSectionComponent.getPrefillCount({
        aiData: {
          remunerationNetteReferenceRccDetectee: 2500,
          allocationOnemMensuelleEstimee: 1500,
          dateNaissanceSalarie: '1962-06-12',
          dateDebutRccEnvisagee: '2026-09-01',
        } as any,
      }),
    ).toBe(4);
  });
});
