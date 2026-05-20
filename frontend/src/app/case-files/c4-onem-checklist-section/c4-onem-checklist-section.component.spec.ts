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

  const MOCK_CONFORME: C4OnemChecklistResponse = {
    caseFileId: CASE_FILE_ID,
    raisonSocialeEmployeur: 'ACME BVBA',
    numeroBce: '1234567890',
    nomSalarie: 'Jean Dupont',
    numeroNationalRegistre: null,
    dateEntreeService: '2022-01-15',
    dateSortieService: '2025-09-30',
    categorieOnem: '1',
    motifExplicite: 'Licenciement avec préavis',
    fauteGraveMentionnee: false,
    preavisPresteJours: 90,
    dernierSalaireMensuelBrut: 3500.0,
    verdict: 'CONFORME',
    mentionsManquantes: [],
    fauteGraveDetectee: false,
    exclusionOnemRange: null,
    lettreRectificativeProposee: null,
    baseJuridique: 'AR 25/11/1991 art. 92 ; loi du 3 juillet 1978',
    etapeSuivante: 'AUCUNE',
    country: 'BELGIQUE',
  };

  const MOCK_NON_CONFORME: C4OnemChecklistResponse = {
    ...MOCK_CONFORME,
    numeroBce: null,
    verdict: 'NON_CONFORME',
    mentionsManquantes: ['NUMERO_BCE'],
    lettreRectificativeProposee:
      'Madame, Monsieur,\n\nNous vous prions de bien vouloir rectifier...',
    etapeSuivante: 'RECTIFICATION_AUPRES_EMPLOYEUR',
  };

  const MOCK_FAUTE_GRAVE: C4OnemChecklistResponse = {
    ...MOCK_CONFORME,
    fauteGraveMentionnee: true,
    motifExplicite: 'Licenciement pour faute grave',
    verdict: 'RISQUE_EXCLUSION_FAUTE_GRAVE',
    mentionsManquantes: [],
    fauteGraveDetectee: true,
    exclusionOnemRange: { minSemaines: 4, maxSemaines: 52 },
    lettreRectificativeProposee: null,
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

  afterEach(() => httpMock.verify());

  function flushSE(): void {
    httpMock
      .match(r => r.url.endsWith('/source-explanations'))
      .forEach(r => r.flush([]));
  }

  function initWithNoExistingResult(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
    flushSE();
  }

  function initWithExistingResult(resp: C4OnemChecklistResponse = MOCK_CONFORME): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
    flushSE();
  }

  // -- Gate pays --

  it('should create and call GET when workspace is BELGIQUE', () => {
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

  // -- GET behavior --

  it('should show form when no existing result (404)', () => {
    initWithNoExistingResult();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('should display existing result from GET and hydrate all 11 fields', () => {
    initWithExistingResult();
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.nomSalarie()).toBe('Jean Dupont');
    expect(component.numeroBce()).toBe('1234567890');
    expect(component.dateEntreeService()).toBe('2022-01-15');
    expect(component.dateSortieService()).toBe('2025-09-30');
    expect(component.fauteGraveMentionnee()).toBe(false);
    expect(component.preavisPresteJours()).toBe(90);
    expect(component.dernierSalaireMensuelBrut()).toBe(3500.0);
    // Provenance jamais IA quand reload depuis backend.
    expect(component.provenanceNomSalarie()).toBeNull();
    expect(component.provenanceFauteGrave()).toBeNull();
  });

  // -- Pré-fill IA — champs directs --

  it('should prefill nomSalarie / dateEntreeService from aiData', () => {
    component.aiData = {
      nomSalarie: 'Marie Martin',
      dateEntree: '2020-03-01',
    } as any;
    initWithNoExistingResult();
    expect(component.nomSalarie()).toBe('Marie Martin');
    expect(component.provenanceNomSalarie()).toBe('IA');
    expect(component.dateEntreeService()).toBe('2020-03-01');
    expect(component.provenanceDateEntree()).toBe('IA');
  });

  it('should prefill numeroBce from aiData.numeroBce (10 digits) as IA', () => {
    component.aiData = { numeroBce: '0123456789' } as any;
    initWithNoExistingResult();
    expect(component.numeroBce()).toBe('0123456789');
    expect(component.provenanceNumeroBce()).toBe('IA');
  });

  it('should prefill numeroBce from fallback bceEmployeur as IA_DERIVE', () => {
    component.aiData = { bceEmployeur: '9876543210' } as any;
    initWithNoExistingResult();
    expect(component.numeroBce()).toBe('9876543210');
    expect(component.provenanceNumeroBce()).toBe('IA_DERIVE');
  });

  it('should prefill dateSortieService from dateRuptureContrat as IA', () => {
    component.aiData = { dateRuptureContrat: '2025-08-15' } as any;
    initWithNoExistingResult();
    expect(component.dateSortieService()).toBe('2025-08-15');
    expect(component.provenanceDateSortie()).toBe('IA');
  });

  it('should prefill dateSortieService fallback from dateLicenciement as IA_DERIVE', () => {
    component.aiData = { dateLicenciement: '2025-07-20' } as any;
    initWithNoExistingResult();
    expect(component.dateSortieService()).toBe('2025-07-20');
    expect(component.provenanceDateSortie()).toBe('IA_DERIVE');
  });

  it('should prefill raisonSociale fallback from nomEmployeur as IA_DERIVE', () => {
    component.aiData = { nomEmployeur: 'ACME BVBA' } as any;
    initWithNoExistingResult();
    expect(component.raisonSocialeEmployeur()).toBe('ACME BVBA');
    expect(component.provenanceRaisonSociale()).toBe('IA_DERIVE');
  });

  it('should prefill motifExplicite from motifLicenciement (≥5 char) as IA_DERIVE', () => {
    component.aiData = {
      motifLicenciement: 'Insuffisance professionnelle',
    } as any;
    initWithNoExistingResult();
    expect(component.motifExplicite()).toBe('Insuffisance professionnelle');
    expect(component.provenanceMotifExplicite()).toBe('IA_DERIVE');
  });

  it('should NOT prefill motifExplicite when text < 5 chars', () => {
    component.aiData = { motifLicenciement: 'ABC' } as any;
    initWithNoExistingResult();
    expect(component.motifExplicite()).toBeNull();
    expect(component.provenanceMotifExplicite()).toBeNull();
  });

  // -- Pré-fill IA — faute grave detection --

  it('should detect fauteGraveMentionnee from motifRupture containing "faute grave"', () => {
    component.aiData = { motifRupture: 'licenciement pour faute grave' } as any;
    initWithNoExistingResult();
    expect(component.fauteGraveMentionnee()).toBe(true);
    expect(component.provenanceFauteGrave()).toBe('IA');
  });

  it('should detect fauteGraveMentionnee from motifExplicite "FAUTE GRAVE" uppercase', () => {
    component.aiData = {
      motifExplicite: 'Licenciement avec FAUTE GRAVE',
    } as any;
    initWithNoExistingResult();
    expect(component.fauteGraveMentionnee()).toBe(true);
  });

  it('should NOT prefill fauteGrave when motif has no keyword', () => {
    component.aiData = { motifRupture: 'démission' } as any;
    initWithNoExistingResult();
    expect(component.fauteGraveMentionnee()).toBe(false);
    expect(component.provenanceFauteGrave()).toBeNull();
  });

  // -- Pré-fill IA — workspace FR --

  it('should NOT prefill anything when workspaceCountry is FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = {
      nomSalarie: 'Test',
      dateEntree: '2020-01-01',
      motifRupture: 'faute grave',
    } as any;
    fixture.detectChanges();
    httpMock.expectNone(API_URL);
    expect(component.nomSalarie()).toBeNull();
    expect(component.dateEntreeService()).toBeNull();
    expect(component.fauteGraveMentionnee()).toBe(false);
  });

  // -- Provenance cleared on manual edit --

  it('should clear provenance on manual nomSalarie change', () => {
    component.aiData = { nomSalarie: 'IA Name' } as any;
    initWithNoExistingResult();
    expect(component.provenanceNomSalarie()).toBe('IA');
    component.onNomSalarieChange('Manual Name');
    expect(component.provenanceNomSalarie()).toBeNull();
    expect(component.nomSalarie()).toBe('Manual Name');
  });

  it('should clear provenance on manual fauteGrave toggle off', () => {
    component.aiData = { motifRupture: 'faute grave' } as any;
    initWithNoExistingResult();
    expect(component.fauteGraveMentionnee()).toBe(true);
    expect(component.provenanceFauteGrave()).toBe('IA');
    component.onFauteGraveChange(false);
    expect(component.fauteGraveMentionnee()).toBe(false);
    expect(component.provenanceFauteGrave()).toBeNull();
  });

  // -- formValid --

  it('formValid should require nomSalarie + dateEntreeService + dateSortieService', () => {
    initWithNoExistingResult();
    expect(component.formValid()).toBe(false);
    component.nomSalarie.set('Test');
    expect(component.formValid()).toBe(false);
    component.dateEntreeService.set('2020-01-01');
    expect(component.formValid()).toBe(false);
    component.dateSortieService.set('2025-01-01');
    expect(component.formValid()).toBe(true);
  });

  it('formValid should reject dateSortie < dateEntree', () => {
    initWithNoExistingResult();
    component.nomSalarie.set('Test');
    component.dateEntreeService.set('2025-01-01');
    component.dateSortieService.set('2020-01-01');
    expect(component.formValid()).toBe(false);
  });

  // -- POST calculate --

  it('should POST with full payload and display CONFORME verdict', () => {
    initWithNoExistingResult();
    component.nomSalarie.set('Jean Dupont');
    component.dateEntreeService.set('2022-01-15');
    component.dateSortieService.set('2025-09-30');
    component.numeroBce.set('1234567890');
    component.raisonSocialeEmployeur.set('ACME BVBA');
    component.categorieOnem.set('1');
    component.motifExplicite.set('Licenciement avec préavis');
    component.fauteGraveMentionnee.set(false);
    component.preavisPresteJours.set(90);
    component.dernierSalaireMensuelBrut.set(3500);
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.nomSalarie).toBe('Jean Dupont');
    expect(req.request.body.dateEntreeService).toBe('2022-01-15');
    expect(req.request.body.dateSortieService).toBe('2025-09-30');
    expect(req.request.body.fauteGraveMentionnee).toBe(false);
    expect(req.request.body.preavisPresteJours).toBe(90);

    req.flush(MOCK_CONFORME);
    expect(component.result()).toBeTruthy();
    expect(component.result()!.verdict).toBe('CONFORME');
    expect(component.showForm()).toBe(false);
  });

  it('should POST and display NON_CONFORME with mentionsManquantes', () => {
    initWithNoExistingResult();
    component.nomSalarie.set('Jean Dupont');
    component.dateEntreeService.set('2022-01-15');
    component.dateSortieService.set('2025-09-30');
    component.calculate();
    httpMock.expectOne(API_URL).flush(MOCK_NON_CONFORME);
    expect(component.result()!.verdict).toBe('NON_CONFORME');
    expect(component.result()!.mentionsManquantes).toEqual(['NUMERO_BCE']);
    expect(component.result()!.lettreRectificativeProposee).toContain('Madame');
  });

  it('should POST and display RISQUE_EXCLUSION_FAUTE_GRAVE verdict + range 4-52', () => {
    initWithNoExistingResult();
    component.nomSalarie.set('Jean Dupont');
    component.dateEntreeService.set('2022-01-15');
    component.dateSortieService.set('2025-09-30');
    component.fauteGraveMentionnee.set(true);
    component.calculate();
    httpMock.expectOne(API_URL).flush(MOCK_FAUTE_GRAVE);
    expect(component.result()!.verdict).toBe('RISQUE_EXCLUSION_FAUTE_GRAVE');
    expect(component.result()!.exclusionOnemRange).toEqual({
      minSemaines: 4,
      maxSemaines: 52,
    });
    expect(component.result()!.etapeSuivante).toBe('CONTESTATION_C4');
  });

  it('should NOT POST when form is invalid', () => {
    initWithNoExistingResult();
    component.nomSalarie.set('Test');
    // dateEntree / dateSortie missing
    component.calculate();
    httpMock.expectNone(API_URL);
    expect(component.calculating()).toBe(false);
  });

  it('should trigger dashboard refresh on successful POST', () => {
    initWithNoExistingResult();
    component.nomSalarie.set('Jean');
    component.dateEntreeService.set('2022-01-01');
    component.dateSortieService.set('2025-01-01');
    component.calculate();
    httpMock.expectOne(API_URL).flush(MOCK_CONFORME);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  // -- Error handling --

  it('should show snackbar on 500', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.nomSalarie.set('Jean');
    component.dateEntreeService.set('2022-01-01');
    component.dateSortieService.set('2025-01-01');
    component.calculate();
    httpMock.expectOne(API_URL).flush('boom', { status: 500, statusText: 'Server Error' });
    expect(spy).toHaveBeenCalled();
    expect(component.calculating()).toBe(false);
  });

  it('should show "Dossier introuvable" on 404 POST', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.nomSalarie.set('Jean');
    component.dateEntreeService.set('2022-01-01');
    component.dateSortieService.set('2025-01-01');
    component.calculate();
    httpMock.expectOne(API_URL).flush('not found', { status: 404, statusText: 'Not Found' });
    expect(spy).toHaveBeenCalledWith('Dossier introuvable', 'Fermer', expect.any(Object));
  });

  it('should refuse calculate when workspaceCountry FR (defense in depth)', () => {
    initWithNoExistingResult();
    component.nomSalarie.set('Jean');
    component.dateEntreeService.set('2022-01-01');
    component.dateSortieService.set('2025-01-01');
    component.workspaceCountry = 'FRANCE';
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    component.calculate();
    httpMock.expectNone(API_URL);
    expect(spy).toHaveBeenCalled();
  });

  // -- Coherence alerts --

  it('should alert DATE_SORTIE_SERVICE when IA diverges > 15 days', () => {
    component.aiData = { dateRuptureContrat: '2025-06-15' } as any;
    initWithNoExistingResult();
    component.dateSortieService.set('2025-07-15'); // > 15 jours
    const alert = component.coherenceAlerts().DATE_SORTIE_SERVICE;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('2025-06-15');
  });

  it('should NOT alert DATE_SORTIE_SERVICE within tolerance', () => {
    component.aiData = { dateRuptureContrat: '2025-06-15' } as any;
    initWithNoExistingResult();
    component.dateSortieService.set('2025-06-20');
    expect(component.coherenceAlerts().DATE_SORTIE_SERVICE).toBeUndefined();
  });

  it('should alert FAUTE_GRAVE_MENTIONNEE when IA detects but user unchecks', () => {
    component.aiData = { motifRupture: 'faute grave' } as any;
    initWithNoExistingResult();
    // IA pré-coche le toggle, user décoche manuellement
    expect(component.fauteGraveMentionnee()).toBe(true);
    component.onFauteGraveChange(false);
    const alert = component.coherenceAlerts().FAUTE_GRAVE_MENTIONNEE;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('Faute grave détectée');
  });

  it('should freeze alerts when result is displayed (showForm=false)', () => {
    component.aiData = {
      dateRuptureContrat: '2025-06-15',
      motifRupture: 'faute grave',
    } as any;
    initWithExistingResult({ ...MOCK_CONFORME, dateSortieService: '2025-12-01' });
    expect(component.showForm()).toBe(false);
    expect(component.alertsSummary().total).toBe(0);
  });

  // -- editMode reopens form --

  it('editMode reopens form, alerts re-active', () => {
    component.aiData = { dateRuptureContrat: '2025-06-15' } as any;
    initWithExistingResult({ ...MOCK_CONFORME, dateSortieService: '2025-12-01' });
    expect(component.showForm()).toBe(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
    component.dateSortieService.set('2025-12-01');
    expect(component.coherenceAlerts().DATE_SORTIE_SERVICE?.source).toBe('IA');
  });
});

// F-177 SF-177-12 — couvre le static `getPrefillCount` exposé pour la card.
describe('C4OnemChecklistSectionComponent.getPrefillCount', () => {
  it('returns 0 when no aiData', () => {
    expect(C4OnemChecklistSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('returns 1 when only nomSalarie is set', () => {
    expect(
      C4OnemChecklistSectionComponent.getPrefillCount({
        aiData: { nomSalarie: 'Jean' },
      }),
    ).toBe(1);
  });

  it('returns 3 when nomSalarie + dateEntree + dateRuptureContrat are set', () => {
    expect(
      C4OnemChecklistSectionComponent.getPrefillCount({
        aiData: {
          nomSalarie: 'Jean',
          dateEntree: '2020-01-01',
          dateRuptureContrat: '2025-01-01',
        },
      }),
    ).toBe(3);
  });

  it('returns 10 nominal (all 10 fields extracted)', () => {
    expect(
      C4OnemChecklistSectionComponent.getPrefillCount({
        aiData: {
          raisonSocialeEmployeur: 'ACME',
          numeroBce: '1234567890',
          nomSalarie: 'Jean',
          dateEntree: '2020-01-01',
          dateRuptureContrat: '2025-01-01',
          categorieOnem: '1',
          motifExplicite: 'Licenciement avec préavis',
          motifRupture: 'faute grave',
          preavisPresteJours: 90,
          dernierSalaireMensuelBrut: 3500,
        },
      }),
    ).toBe(10);
  });

  it('counts fallback fields (bceEmployeur, dateLicenciement, motifLicenciement, salaireBrutMensuel)', () => {
    expect(
      C4OnemChecklistSectionComponent.getPrefillCount({
        aiData: {
          nomEmployeur: 'ACME',
          bceEmployeur: '0123456789',
          dateLicenciement: '2025-01-01',
          motifLicenciement: 'Insuffisance professionnelle',
          salaireBrutMensuel: 3500,
        },
      }),
    ).toBe(5);
  });
});
