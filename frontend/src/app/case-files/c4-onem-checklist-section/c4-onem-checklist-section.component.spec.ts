import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { C4OnemChecklistSectionComponent } from './c4-onem-checklist-section.component';
import { C4OnemChecklistResponse } from '../../core/models/c4-onem-checklist.model';

describe('C4OnemChecklistSectionComponent', () => {
  let component: C4OnemChecklistSectionComponent;
  let fixture: ComponentFixture<C4OnemChecklistSectionComponent>;
  let httpMock: HttpTestingController;
  let refreshSpy: { triggerRefresh: jest.Mock };

  const CASE_FILE_ID = '33333333-3333-3333-3333-333333333333';
  const API_URL =
    `/api/v1/case-files/${CASE_FILE_ID}/decision-tools/c4-onem-checklist`;

  const CONFORME_RESPONSE: C4OnemChecklistResponse = {
    caseFileId: CASE_FILE_ID,
    raisonSocialeEmployeur: 'ACME BE SA',
    numeroBce: '0123456789',
    nomSalarie: 'Jean Dupont',
    numeroNationalRegistre: null,
    dateEntreeService: '2020-01-15',
    dateSortieService: '2025-06-30',
    categorieOnem: '1',
    motifExplicite: 'Restructuration économique',
    fauteGraveMentionnee: false,
    preavisPresteJours: 30,
    dernierSalaireMensuelBrut: 3500,
    verdict: 'CONFORME',
    mentionsManquantes: [],
    fauteGraveDetectee: false,
    exclusionOnemRange: null,
    lettreRectificativeProposee: null,
    baseJuridique: 'AR du 25 novembre 1991 art. 92',
    etapeSuivante: 'AUCUNE',
    country: 'BELGIQUE',
  };

  const NON_CONFORME_RESPONSE: C4OnemChecklistResponse = {
    ...CONFORME_RESPONSE,
    numeroBce: null,
    motifExplicite: null,
    verdict: 'NON_CONFORME',
    mentionsManquantes: ['NUMERO_BCE', 'MOTIF_EXPLICITE'],
    lettreRectificativeProposee:
      'Objet : demande de rectification du C4 ONEM\n\nMadame, Monsieur,\n\nLe document C4 ONEM remis omet : numéro BCE (10 chiffres), motif explicite...',
    baseJuridique: 'AR du 25 novembre 1991 art. 92 ; loi du 3 juillet 1978',
    etapeSuivante: 'RECTIFICATION_AUPRES_EMPLOYEUR',
  };

  const FAUTE_GRAVE_RESPONSE: C4OnemChecklistResponse = {
    ...CONFORME_RESPONSE,
    fauteGraveMentionnee: true,
    verdict: 'RISQUE_EXCLUSION_FAUTE_GRAVE',
    mentionsManquantes: [],
    fauteGraveDetectee: true,
    exclusionOnemRange: { minSemaines: 4, maxSemaines: 52 },
    lettreRectificativeProposee: null,
    baseJuridique: 'AR du 25 novembre 1991 art. 92 ; art. 144 (exclusion faute grave)',
    etapeSuivante: 'CONTESTATION_C4',
  };

  beforeEach(async () => {
    refreshSpy = { triggerRefresh: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [C4OnemChecklistSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(C4OnemChecklistSectionComponent);
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

  function initWithExistingResult(resp: C4OnemChecklistResponse = CONFORME_RESPONSE): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
    flushSE();
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

  it('should display existing result from GET (CONFORME)', () => {
    initWithExistingResult();
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.nomSalarie()).toBe('Jean Dupont');
    expect(component.numeroBce()).toBe('0123456789');
    expect(component.fauteGraveMentionnee()).toBe(false);
    // Reload backend = saisie avocat = jamais de badge IA.
    expect(component.provenanceNomSalarie()).toBeNull();
    expect(component.provenanceDateEntree()).toBeNull();
  });

  // ── Pré-fill IA — 10 champs ───────────────────────────────────────────

  it('should prefill all 10 instrumented fields from full aiData', () => {
    component.aiData = {
      raisonSocialeEmployeur: 'ACME BE SA',
      numeroBce: '0123456789',
      nomSalarie: 'Dupont',
      prenomSalarie: 'Jean',
      dateEntree: '2020-01-15',
      dateRuptureContrat: '2025-06-30',
      categorieOnem: '9',
      motifExplicite: 'Restructuration',
      motifRupture: 'Licenciement pour faute grave',
      preavisPresteJours: 30,
      dernierSalaireMensuelBrut: 3500,
    } as any;
    initWithNoExistingResult();
    expect(component.raisonSocialeEmployeur()).toBe('ACME BE SA');
    expect(component.numeroBce()).toBe('0123456789');
    expect(component.nomSalarie()).toBe('Jean Dupont');
    expect(component.dateEntreeService()).toBe('2020-01-15');
    expect(component.dateSortieService()).toBe('2025-06-30');
    expect(component.categorieOnem()).toBe('9');
    expect(component.motifExplicite()).toBe('Restructuration');
    expect(component.fauteGraveMentionnee()).toBe(true);
    expect(component.preavisPresteJours()).toBe(30);
    expect(component.dernierSalaireMensuelBrut()).toBe(3500);

    // Badges IA visibles pour les 10 champs pré-remplis.
    expect(component.provenanceRaisonSociale()).toBe('IA');
    expect(component.provenanceNumeroBce()).toBe('IA');
    expect(component.provenanceNomSalarie()).toBe('IA');
    expect(component.provenanceDateEntree()).toBe('IA');
    expect(component.provenanceDateSortie()).toBe('IA');
    expect(component.provenanceCategorieOnem()).toBe('IA');
    expect(component.provenanceMotif()).toBe('IA');
    expect(component.provenanceFauteGrave()).toBe('IA');
    expect(component.provenancePreavis()).toBe('IA');
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('should NOT prefill anything if workspaceCountry is FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = { dateEntree: '2020-01-15' } as any;
    fixture.detectChanges();
    httpMock.expectNone(API_URL);
    expect(component.dateEntreeService()).toBeNull();
  });

  it('should clear provenance on manual nomSalarie change', () => {
    component.aiData = { nomSalarie: 'Dupont' } as any;
    initWithNoExistingResult();
    expect(component.provenanceNomSalarie()).toBe('IA');
    component.onNomSalarieChange('Martin');
    expect(component.provenanceNomSalarie()).toBeNull();
    expect(component.nomSalarie()).toBe('Martin');
  });

  it('should clear provenance on manual fauteGrave toggle', () => {
    component.aiData = { motifRupture: 'faute grave' } as any;
    initWithNoExistingResult();
    expect(component.provenanceFauteGrave()).toBe('IA');
    expect(component.fauteGraveMentionnee()).toBe(true);
    component.onFauteGraveChange(false);
    expect(component.provenanceFauteGrave()).toBeNull();
    expect(component.fauteGraveMentionnee()).toBe(false);
  });

  // ── POST calculate ────────────────────────────────────────────────────

  it('should NOT POST when required fields missing', () => {
    initWithNoExistingResult();
    // pas de nom, ni de dates
    component.calculate();
    httpMock.expectNone(API_URL);
  });

  it('should POST and show CONFORME verdict on success', () => {
    initWithNoExistingResult();
    component.nomSalarie.set('Jean Dupont');
    component.dateEntreeService.set('2020-01-15');
    component.dateSortieService.set('2025-06-30');
    component.fauteGraveMentionnee.set(false);
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.nomSalarie).toBe('Jean Dupont');
    expect(req.request.body.dateEntreeService).toBe('2020-01-15');
    expect(req.request.body.dateSortieService).toBe('2025-06-30');
    expect(req.request.body.fauteGraveMentionnee).toBe(false);

    req.flush(CONFORME_RESPONSE);
    expect(component.result()?.verdict).toBe('CONFORME');
    expect(component.showForm()).toBe(false);
  });

  it('should display NON_CONFORME verdict + mentions list + lettre', () => {
    initWithExistingResult(NON_CONFORME_RESPONSE);
    expect(component.result()?.verdict).toBe('NON_CONFORME');
    expect(component.mentionsManquantesHumanisees().length).toBe(2);
    const codes = component.mentionsManquantesHumanisees().map(m => m.code);
    expect(codes).toContain('NUMERO_BCE');
    expect(codes).toContain('MOTIF_EXPLICITE');
    // lettre rectificative exposée
    expect(component.result()?.lettreRectificativeProposee).toContain('Objet');
  });

  it('should display RISQUE_EXCLUSION_FAUTE_GRAVE verdict + range + NO lettre', () => {
    initWithExistingResult(FAUTE_GRAVE_RESPONSE);
    expect(component.result()?.verdict).toBe('RISQUE_EXCLUSION_FAUTE_GRAVE');
    expect(component.result()?.exclusionOnemRange).toEqual({ minSemaines: 4, maxSemaines: 52 });
    expect(component.result()?.lettreRectificativeProposee).toBeNull();
  });

  it('should trigger dashboard refresh on successful POST', () => {
    initWithNoExistingResult();
    component.nomSalarie.set('Jean Dupont');
    component.dateEntreeService.set('2020-01-15');
    component.dateSortieService.set('2025-06-30');
    component.calculate();
    httpMock.expectOne(API_URL).flush(CONFORME_RESPONSE);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  // ── formValid ─────────────────────────────────────────────────────────

  it('formValid requires nom + 2 dates AND sortie ≥ entree', () => {
    initWithNoExistingResult();
    expect(component.formValid()).toBe(false);
    component.nomSalarie.set('Jean');
    component.dateEntreeService.set('2025-06-30');
    component.dateSortieService.set('2020-01-15'); // sortie < entree
    expect(component.formValid()).toBe(false);
    component.dateSortieService.set('2025-07-30'); // sortie ≥ entree
    expect(component.formValid()).toBe(true);
  });

  // ── Erreurs ──────────────────────────────────────────────────────────

  it('should show snackbar on 500', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.nomSalarie.set('Jean Dupont');
    component.dateEntreeService.set('2020-01-15');
    component.dateSortieService.set('2025-06-30');
    component.calculate();
    httpMock.expectOne(API_URL).flush('boom', { status: 500, statusText: 'Server Error' });
    expect(spy).toHaveBeenCalled();
    expect(component.calculating()).toBe(false);
  });

  it('should show snackbar with "Dossier introuvable" on 404 POST', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.nomSalarie.set('Jean Dupont');
    component.dateEntreeService.set('2020-01-15');
    component.dateSortieService.set('2025-06-30');
    component.calculate();
    httpMock.expectOne(API_URL).flush('not found', { status: 404, statusText: 'Not Found' });
    expect(spy).toHaveBeenCalledWith('Dossier introuvable', 'Fermer', expect.any(Object));
  });

  it('should refuse calculate when workspaceCountry FR (defense in depth)', () => {
    initWithNoExistingResult();
    component.nomSalarie.set('Jean Dupont');
    component.dateEntreeService.set('2020-01-15');
    component.dateSortieService.set('2025-06-30');
    component.workspaceCountry = 'FRANCE';
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    component.calculate();
    httpMock.expectNone(API_URL);
    expect(spy).toHaveBeenCalled();
  });

  // ── Cohérence alerts ──────────────────────────────────────────────────

  it('should alert DATE_ENTREE when IA diverges > 15 days', () => {
    component.aiData = { dateEntree: '2020-01-15' } as any;
    initWithNoExistingResult();
    component.dateEntreeService.set('2020-03-01'); // > 15 jours
    const alert = component.coherenceAlerts().DATE_ENTREE;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('2020-01-15');
  });

  it('should NOT alert DATE_ENTREE within tolerance', () => {
    component.aiData = { dateEntree: '2020-01-15' } as any;
    initWithNoExistingResult();
    component.dateEntreeService.set('2020-01-20'); // < 15 jours
    expect(component.coherenceAlerts().DATE_ENTREE).toBeUndefined();
  });

  it('should alert FAUTE_GRAVE when IA detects faute grave but avocat decoche', () => {
    component.aiData = { motifRupture: 'licenciement pour faute grave' } as any;
    initWithNoExistingResult();
    // IA detected, prefill set fauteGrave to true.
    expect(component.fauteGraveMentionnee()).toBe(true);
    component.onFauteGraveChange(false); // avocat décoche
    const alert = component.coherenceAlerts().FAUTE_GRAVE;
    expect(alert?.source).toBe('IA');
  });

  it('should freeze alerts when result is displayed (showForm=false)', () => {
    component.aiData = { dateEntree: '2020-01-15' } as any;
    initWithExistingResult({ ...CONFORME_RESPONSE, dateEntreeService: '2025-01-01' });
    expect(component.showForm()).toBe(false);
    expect(component.alertsSummary().total).toBe(0);
  });

  // ── Clipboard / lettre rectificative ─────────────────────────────────

  it('copyLettre writes lettre to clipboard (success path)', async () => {
    const writeSpy = jest
      .fn<Promise<void>, [string]>()
      .mockReturnValue(Promise.resolve());
    Object.assign(navigator, { clipboard: { writeText: writeSpy } });
    initWithExistingResult(NON_CONFORME_RESPONSE);

    component.copyLettre();
    await Promise.resolve();
    expect(writeSpy).toHaveBeenCalledWith(NON_CONFORME_RESPONSE.lettreRectificativeProposee);
    expect(component.copying()).toBe(false);
  });

  it('copyLettre no-op when no lettre present', () => {
    const writeSpy = jest.fn();
    Object.assign(navigator, { clipboard: { writeText: writeSpy } });
    initWithExistingResult(CONFORME_RESPONSE); // lettre null
    component.copyLettre();
    expect(writeSpy).not.toHaveBeenCalled();
  });

  // ── editMode ──────────────────────────────────────────────────────────

  it('editMode reopens form, alerts re-active', () => {
    component.aiData = { dateEntree: '2020-01-15' } as any;
    initWithExistingResult({ ...CONFORME_RESPONSE, dateEntreeService: '2025-01-01' });
    expect(component.showForm()).toBe(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
    component.dateEntreeService.set('2025-01-01');
    expect(component.coherenceAlerts().DATE_ENTREE?.source).toBe('IA');
  });
});

// Couvre le static `getPrefillCount` exposé pour la card du panel F-IA-04.
describe('C4OnemChecklistSectionComponent.getPrefillCount', () => {
  it('returns 0 when no aiData', () => {
    expect(C4OnemChecklistSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('returns 1 when only dateEntree is set', () => {
    expect(
      C4OnemChecklistSectionComponent.getPrefillCount({
        aiData: { dateEntree: '2020-01-15' },
      }),
    ).toBe(1);
  });

  it('returns 10 nominal — tous les champs instrumentés', () => {
    expect(
      C4OnemChecklistSectionComponent.getPrefillCount({
        aiData: {
          raisonSocialeEmployeur: 'ACME',
          numeroBce: '0123456789',
          nomSalarie: 'Dupont',
          prenomSalarie: 'Jean',
          dateEntree: '2020-01-15',
          dateRuptureContrat: '2025-06-30',
          categorieOnem: '9',
          motifExplicite: 'Restructuration',
          motifRupture: 'Licenciement pour faute grave',
          preavisPresteJours: 30,
          dernierSalaireMensuelBrut: 3500,
        },
      }),
    ).toBe(10);
  });
});
