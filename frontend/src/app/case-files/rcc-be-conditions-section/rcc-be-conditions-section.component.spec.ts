import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { RccBeConditionsSectionComponent } from './rcc-be-conditions-section.component';
import { RccBeConditionsResponse } from '../../core/models/rcc-be-conditions.model';

describe('RccBeConditionsSectionComponent', () => {
  let component: RccBeConditionsSectionComponent;
  let fixture: ComponentFixture<RccBeConditionsSectionComponent>;
  let httpMock: HttpTestingController;
  let refreshSpy: { triggerRefresh: jest.Mock };

  const CASE_FILE_ID = '55555555-5555-5555-5555-555555555555';
  const API_URL =
    `/api/v1/case-files/${CASE_FILE_ID}/decision-tools/rcc-be-conditions`;

  const ELIGIBLE_RESPONSE: RccBeConditionsResponse = {
    caseFileId: CASE_FILE_ID,
    dateNaissance: '1962-06-12',
    anneesCarriereProfessionnelle: 41,
    metierLourd: false,
    longueCarriere: false,
    entrepriseEnDifficulte: false,
    dateLicenciementEnvisagee: '2026-06-12',
    verdict: 'ELIGIBLE',
    regimeApplicable: 'GENERAL',
    regimesEligibles: ['GENERAL'],
    ageALaDateLicenciement: 64,
    anneesCarriereCalculees: 41,
    conditionsManquantes: [],
    baseJuridique: 'CCT 17 art. 3 ; AR 03/05/2007 art. 3',
    formuleCalcul: 'âge(64) >= 60 ET carrière(41) >= 40 → GENERAL',
    country: 'BELGIQUE',
  };

  const ELIGIBLE_CUMULE_RESPONSE: RccBeConditionsResponse = {
    ...ELIGIBLE_RESPONSE,
    metierLourd: true,
    regimeApplicable: 'GENERAL',
    regimesEligibles: ['GENERAL', 'METIERS_LOURDS'],
  };

  const INCERTAIN_RESPONSE: RccBeConditionsResponse = {
    ...ELIGIBLE_RESPONSE,
    dateNaissance: '1968-06-12',
    anneesCarriereProfessionnelle: 32,
    verdict: 'INCERTAIN',
    regimeApplicable: null,
    regimesEligibles: [],
    ageALaDateLicenciement: 57,
    anneesCarriereCalculees: 32,
    conditionsManquantes: ['AGE_GENERAL_60', 'CARRIERE_40'],
    formuleCalcul: 'âge(57) < 60 — proche des seuils',
  };

  const NON_ELIGIBLE_RESPONSE: RccBeConditionsResponse = {
    ...ELIGIBLE_RESPONSE,
    dateNaissance: '1980-06-12',
    anneesCarriereProfessionnelle: 15,
    verdict: 'NON_ELIGIBLE',
    regimeApplicable: null,
    regimesEligibles: [],
    ageALaDateLicenciement: 45,
    anneesCarriereCalculees: 15,
    conditionsManquantes: ['AGE_GENERAL_60', 'CARRIERE_40'],
    formuleCalcul: 'âge(45) < 55 — pas proche des seuils',
  };

  beforeEach(async () => {
    refreshSpy = { triggerRefresh: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [RccBeConditionsSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RccBeConditionsSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
    component.workspaceCountry = 'BELGIQUE';
  });

  afterEach(() => httpMock.verify());

  function initWithNoExistingResult(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
  }

  function initWithExistingResult(resp: RccBeConditionsResponse = ELIGIBLE_RESPONSE): void {
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

  it('should display existing ELIGIBLE result from GET', () => {
    initWithExistingResult();
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.dateNaissance()).toBe('1962-06-12');
    expect(component.anneesCarriereProfessionnelle()).toBe(41);
    // Reload backend = saisie avocat = jamais de badge IA.
    expect(component.provenanceDateNaissance()).toBeNull();
    expect(component.provenanceAnneesCarriere()).toBeNull();
    expect(component.provenanceMetierLourd()).toBeNull();
    expect(component.provenanceEntrepriseEnDifficulte()).toBeNull();
  });

  // ── Pré-fill IA — 4 champs ──────────────────────────────────────────

  it('should prefill all 4 instrumented fields from full aiData', () => {
    component.aiData = {
      dateNaissanceSalarie: '1962-06-12',
      anneesCarriereSalarie: 41,
      metierLourdDetecte: true,
      entrepriseEnDifficulteDetectee: true,
    } as any;
    initWithNoExistingResult();
    expect(component.dateNaissance()).toBe('1962-06-12');
    expect(component.anneesCarriereProfessionnelle()).toBe(41);
    expect(component.metierLourd()).toBe(true);
    expect(component.entrepriseEnDifficulte()).toBe(true);
    expect(component.provenanceDateNaissance()).toBe('IA');
    expect(component.provenanceAnneesCarriere()).toBe('IA');
    expect(component.provenanceMetierLourd()).toBe('IA');
    expect(component.provenanceEntrepriseEnDifficulte()).toBe('IA');
  });

  it('should NOT prefill metierLourd when aiData says false', () => {
    component.aiData = {
      dateNaissanceSalarie: '1962-06-12',
      metierLourdDetecte: false,
    } as any;
    initWithNoExistingResult();
    expect(component.metierLourd()).toBe(false);
    expect(component.provenanceMetierLourd()).toBeNull();
    // Mais dateNaissance OUI.
    expect(component.dateNaissance()).toBe('1962-06-12');
    expect(component.provenanceDateNaissance()).toBe('IA');
  });

  it('should NOT prefill anything if workspaceCountry is FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = { dateNaissanceSalarie: '1962-06-12' } as any;
    fixture.detectChanges();
    httpMock.expectNone(API_URL);
    expect(component.dateNaissance()).toBeNull();
  });

  it('should clear provenance on manual date naissance change', () => {
    component.aiData = { dateNaissanceSalarie: '1962-06-12' } as any;
    initWithNoExistingResult();
    expect(component.provenanceDateNaissance()).toBe('IA');
    component.onDateNaissanceChange('1965-01-01');
    expect(component.provenanceDateNaissance()).toBeNull();
    expect(component.dateNaissance()).toBe('1965-01-01');
  });

  it('should clear provenance on manual annees carriere change', () => {
    component.aiData = { anneesCarriereSalarie: 40 } as any;
    initWithNoExistingResult();
    expect(component.provenanceAnneesCarriere()).toBe('IA');
    component.onAnneesCarriereChange(38);
    expect(component.provenanceAnneesCarriere()).toBeNull();
    expect(component.anneesCarriereProfessionnelle()).toBe(38);
  });

  it('should clear provenance on manual metier lourd toggle', () => {
    component.aiData = { metierLourdDetecte: true } as any;
    initWithNoExistingResult();
    expect(component.provenanceMetierLourd()).toBe('IA');
    expect(component.metierLourd()).toBe(true);
    component.onMetierLourdChange(false);
    expect(component.provenanceMetierLourd()).toBeNull();
    expect(component.metierLourd()).toBe(false);
  });

  // ── formValid ─────────────────────────────────────────────────────────

  it('formValid requires dateNaissance + anneesCarriere in [0, 60]', () => {
    initWithNoExistingResult();
    expect(component.formValid()).toBe(false);
    component.dateNaissance.set('1962-06-12');
    expect(component.formValid()).toBe(false);
    component.anneesCarriereProfessionnelle.set(40);
    expect(component.formValid()).toBe(true);
  });

  it('formValid rejects anneesCarriere out of [0, 60]', () => {
    initWithNoExistingResult();
    component.dateNaissance.set('1962-06-12');
    component.anneesCarriereProfessionnelle.set(-1);
    expect(component.formValid()).toBe(false);
    component.anneesCarriereProfessionnelle.set(61);
    expect(component.formValid()).toBe(false);
    component.anneesCarriereProfessionnelle.set(0);
    expect(component.formValid()).toBe(true);
    component.anneesCarriereProfessionnelle.set(60);
    expect(component.formValid()).toBe(true);
  });

  // ── POST calculate ────────────────────────────────────────────────────

  it('should NOT POST when required fields missing', () => {
    initWithNoExistingResult();
    component.calculate();
    httpMock.expectNone(API_URL);
  });

  it('should POST and show ELIGIBLE verdict on success', () => {
    initWithNoExistingResult();
    component.dateNaissance.set('1962-06-12');
    component.anneesCarriereProfessionnelle.set(41);
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateNaissance).toBe('1962-06-12');
    expect(req.request.body.anneesCarriereProfessionnelle).toBe(41);
    expect(req.request.body.metierLourd).toBe(false);

    req.flush(ELIGIBLE_RESPONSE);
    expect(component.result()?.verdict).toBe('ELIGIBLE');
    expect(component.showForm()).toBe(false);
  });

  it('should display ELIGIBLE cumulé verdict + 2 régimes listés', () => {
    initWithExistingResult(ELIGIBLE_CUMULE_RESPONSE);
    expect(component.result()?.verdict).toBe('ELIGIBLE');
    expect(component.regimesEligiblesHumanises().length).toBe(2);
    const codes = component.regimesEligiblesHumanises().map(r => r.code);
    expect(codes).toContain('GENERAL');
    expect(codes).toContain('METIERS_LOURDS');
  });

  it('should display INCERTAIN verdict + conditions manquantes list', () => {
    initWithExistingResult(INCERTAIN_RESPONSE);
    expect(component.result()?.verdict).toBe('INCERTAIN');
    expect(component.result()?.regimeApplicable).toBeNull();
    expect(component.conditionsManquantesHumanisees().length).toBe(2);
    const codes = component.conditionsManquantesHumanisees().map(c => c.code);
    expect(codes).toContain('AGE_GENERAL_60');
    expect(codes).toContain('CARRIERE_40');
  });

  it('should display NON_ELIGIBLE verdict + all conditions listed', () => {
    initWithExistingResult(NON_ELIGIBLE_RESPONSE);
    expect(component.result()?.verdict).toBe('NON_ELIGIBLE');
    expect(component.result()?.regimeApplicable).toBeNull();
    expect(component.conditionsManquantesHumanisees().length).toBe(2);
  });

  it('should trigger dashboard refresh on successful POST', () => {
    initWithNoExistingResult();
    component.dateNaissance.set('1962-06-12');
    component.anneesCarriereProfessionnelle.set(41);
    component.calculate();
    httpMock.expectOne(API_URL).flush(ELIGIBLE_RESPONSE);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  // ── Erreurs ──────────────────────────────────────────────────────────

  it('should show snackbar on 500', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.dateNaissance.set('1962-06-12');
    component.anneesCarriereProfessionnelle.set(41);
    component.calculate();
    httpMock.expectOne(API_URL).flush('boom', { status: 500, statusText: 'Server Error' });
    expect(spy).toHaveBeenCalled();
    expect(component.calculating()).toBe(false);
  });

  it('should show snackbar with "Dossier introuvable" on 404 POST', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.dateNaissance.set('1962-06-12');
    component.anneesCarriereProfessionnelle.set(41);
    component.calculate();
    httpMock.expectOne(API_URL).flush('not found', { status: 404, statusText: 'Not Found' });
    expect(spy).toHaveBeenCalledWith('Dossier introuvable', 'Fermer', expect.any(Object));
  });

  it('should refuse calculate when workspaceCountry FR (defense in depth)', () => {
    initWithNoExistingResult();
    component.dateNaissance.set('1962-06-12');
    component.anneesCarriereProfessionnelle.set(41);
    component.workspaceCountry = 'FRANCE';
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    component.calculate();
    httpMock.expectNone(API_URL);
    expect(spy).toHaveBeenCalled();
  });

  // ── Verdict helpers ──────────────────────────────────────────────────

  it('verdictClass + verdictIcon couvrent les 3 verdicts', () => {
    expect(component.verdictClass('ELIGIBLE')).toBe('verdict-ok');
    expect(component.verdictClass('INCERTAIN')).toBe('verdict-warn');
    expect(component.verdictClass('NON_ELIGIBLE')).toBe('verdict-critical');
    expect(component.verdictIcon('ELIGIBLE')).toBe('check_circle');
    expect(component.verdictIcon('INCERTAIN')).toBe('warning');
    expect(component.verdictIcon('NON_ELIGIBLE')).toBe('block');
  });

  // ── editMode ──────────────────────────────────────────────────────────

  it('editMode reopens form', () => {
    initWithExistingResult(ELIGIBLE_RESPONSE);
    expect(component.showForm()).toBe(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });
});

// Couvre le static `getPrefillCount` exposé pour la card du panel F-IA-04.
describe('RccBeConditionsSectionComponent.getPrefillCount', () => {
  it('returns 0 when no aiData', () => {
    expect(RccBeConditionsSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('returns 1 when only dateNaissanceSalarie is set', () => {
    expect(
      RccBeConditionsSectionComponent.getPrefillCount({
        aiData: { dateNaissanceSalarie: '1962-06-12' } as any,
      }),
    ).toBe(1);
  });

  it('returns 4 nominal — tous les champs instrumentés', () => {
    expect(
      RccBeConditionsSectionComponent.getPrefillCount({
        aiData: {
          dateNaissanceSalarie: '1962-06-12',
          anneesCarriereSalarie: 41,
          metierLourdDetecte: true,
          entrepriseEnDifficulteDetectee: true,
        } as any,
      }),
    ).toBe(4);
  });
});
