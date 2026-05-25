import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ContestationC4OnemSectionComponent } from './contestation-c4-onem-section.component';
import { ContestationC4OnemResponse } from '../../core/models/contestation-c4-onem.model';

describe('ContestationC4OnemSectionComponent', () => {
  let component: ContestationC4OnemSectionComponent;
  let fixture: ComponentFixture<ContestationC4OnemSectionComponent>;
  let httpMock: HttpTestingController;
  let refreshSpy: { triggerRefresh: jest.Mock };

  const CASE_FILE_ID = '44444444-4444-4444-4444-444444444444';
  const API_URL =
    `/api/v1/case-files/${CASE_FILE_ID}/decision-tools/contestation-c4-onem`;

  // Cas A — recours admin pas encore formé : verdict ADMIN_OUVERT.
  const CAS_A_OUVERT: ContestationC4OnemResponse = {
    caseFileId: CASE_FILE_ID,
    dateNotificationDecisionOnem: '2026-05-10',
    dateActionEnvisagee: '2026-05-12',
    recoursAdminDejaForme: false,
    dateDecisionDirecteur: null,
    verdict: 'RECOURS_ADMIN_OUVERT',
    paliers: [
      { type: 'ADMIN', dateLimite: '2026-06-10', joursRestants: 29, baseJuridique: 'AR 25/11/1991 art. 144' },
      { type: 'TRIBUNAL', dateLimite: null, joursRestants: null, baseJuridique: 'CJ art. 580 2°' },
    ],
    etapeSuivante: 'RECOURS_ADMIN_DIRECTEUR',
    baseJuridique: 'AR 25/11/1991 art. 144 ; CJ art. 580 2° ; Loi 03/07/1978',
    formuleCalcul: 'Délai admin = dateNotificationDecisionOnem + 1 mois.',
    country: 'BELGIQUE',
  };

  // Cas A — admin prescrit (saut palier possible).
  const CAS_A_PRESCRIT: ContestationC4OnemResponse = {
    ...CAS_A_OUVERT,
    dateNotificationDecisionOnem: '2026-01-10',
    verdict: 'RECOURS_ADMIN_PRESCRIT',
    paliers: [
      { type: 'ADMIN', dateLimite: '2026-02-10', joursRestants: -100, baseJuridique: 'AR 25/11/1991 art. 144' },
      { type: 'TRIBUNAL', dateLimite: null, joursRestants: null, baseJuridique: 'CJ art. 580 2°' },
    ],
    etapeSuivante: 'RECOURS_TRIBUNAL_TRAVAIL',
  };

  // Cas B — recours admin déjà formé, tribunal prescrit → forclusion totale.
  const CAS_B_FORCLUSION: ContestationC4OnemResponse = {
    ...CAS_A_OUVERT,
    recoursAdminDejaForme: true,
    dateDecisionDirecteur: '2026-01-05',
    verdict: 'RECOURS_TRIBUNAL_PRESCRIT',
    paliers: [
      { type: 'ADMIN', dateLimite: '2026-06-10', joursRestants: 29, baseJuridique: 'AR 25/11/1991 art. 144' },
      { type: 'TRIBUNAL', dateLimite: '2026-04-05', joursRestants: -50, baseJuridique: 'CJ art. 580 2°' },
    ],
    etapeSuivante: 'FORCLUSION_TOTALE',
  };

  // Cas B — tribunal imminent.
  const CAS_B_IMMINENT: ContestationC4OnemResponse = {
    ...CAS_B_FORCLUSION,
    verdict: 'RECOURS_TRIBUNAL_IMMINENT',
    paliers: [
      { type: 'ADMIN', dateLimite: '2026-06-10', joursRestants: 29, baseJuridique: 'AR 25/11/1991 art. 144' },
      { type: 'TRIBUNAL', dateLimite: '2026-05-25', joursRestants: 5, baseJuridique: 'CJ art. 580 2°' },
    ],
    etapeSuivante: 'RECOURS_TRIBUNAL_TRAVAIL',
  };

  beforeEach(async () => {
    refreshSpy = { triggerRefresh: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [ContestationC4OnemSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ContestationC4OnemSectionComponent);
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

  function initWithExistingResult(resp: ContestationC4OnemResponse = CAS_A_OUVERT): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
    flushSE();
  }

  // ── Création / gate BE ───────────────────────────────────────────────

  it('should create and gate BE', () => {
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

  // ── Pré-fill IA (3 champs) ───────────────────────────────────────────

  it('should prefill all 3 instrumented fields from full aiData', () => {
    component.aiData = {
      dateNotificationDecisionOnem: '2026-05-10',
      dateDecisionDirecteur: '2026-06-15',
      recoursAdminDejaForme: true,
    } as any;
    initWithNoExistingResult();
    expect(component.dateNotificationDecisionOnem()).toBe('2026-05-10');
    expect(component.dateDecisionDirecteur()).toBe('2026-06-15');
    expect(component.recoursAdminDejaForme()).toBe(true);

    // Badges IA visibles.
    expect(component.provenanceDateNotification()).toBe('IA');
    expect(component.provenanceDateDirecteur()).toBe('IA');
    expect(component.provenanceRecoursAdmin()).toBe('IA');
  });

  it('should clear provenance on manual date notification change', () => {
    component.aiData = { dateNotificationDecisionOnem: '2026-05-10' } as any;
    initWithNoExistingResult();
    expect(component.provenanceDateNotification()).toBe('IA');
    component.onDateNotificationChange('2026-06-01');
    expect(component.provenanceDateNotification()).toBeNull();
    expect(component.dateNotificationDecisionOnem()).toBe('2026-06-01');
  });

  it('should NOT prefill anything if workspaceCountry FR', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = { dateNotificationDecisionOnem: '2026-05-10' } as any;
    fixture.detectChanges();
    httpMock.expectNone(API_URL);
    expect(component.dateNotificationDecisionOnem()).toBeNull();
  });

  // ── Toggle Cas A / Cas B ─────────────────────────────────────────────

  it('should hide dateDecisionDirecteur when recoursAdminDejaForme=false (Cas A)', () => {
    initWithNoExistingResult();
    component.onRecoursAdminChange(false);
    expect(component.recoursAdminDejaForme()).toBe(false);
    // Form invalide tant que date notif absente.
    expect(component.formValid()).toBe(false);
    component.dateNotificationDecisionOnem.set('2026-05-10');
    expect(component.formValid()).toBe(true); // Cas A : pas besoin de date directeur
  });

  it('should require dateDecisionDirecteur when recoursAdminDejaForme=true (Cas B)', () => {
    initWithNoExistingResult();
    component.onRecoursAdminChange(true);
    component.dateNotificationDecisionOnem.set('2026-05-10');
    expect(component.formValid()).toBe(false); // dateDirecteur manquante
    component.dateDecisionDirecteur.set('2026-06-15');
    expect(component.formValid()).toBe(true);
  });

  it('should clear dateDecisionDirecteur when toggling Cas B → Cas A', () => {
    initWithNoExistingResult();
    component.onRecoursAdminChange(true);
    component.dateDecisionDirecteur.set('2026-06-15');
    component.onRecoursAdminChange(false);
    expect(component.dateDecisionDirecteur()).toBeNull();
    expect(component.provenanceDateDirecteur()).toBeNull();
  });

  // ── POST calculate ────────────────────────────────────────────────────

  it('should NOT POST when required field missing', () => {
    initWithNoExistingResult();
    component.calculate();
    httpMock.expectNone(API_URL);
  });

  it('should POST Cas A (recoursAdmin=false) and show RECOURS_ADMIN_OUVERT verdict', () => {
    initWithNoExistingResult();
    component.dateNotificationDecisionOnem.set('2026-05-10');
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateNotificationDecisionOnem).toBe('2026-05-10');
    expect(req.request.body.recoursAdminDejaForme).toBe(false);
    expect(req.request.body.dateDecisionDirecteur).toBeNull();

    req.flush(CAS_A_OUVERT);
    expect(component.result()?.verdict).toBe('RECOURS_ADMIN_OUVERT');
    expect(component.showForm()).toBe(false);
  });

  it('should POST Cas B with dateDecisionDirecteur and show TRIBUNAL verdict', () => {
    initWithNoExistingResult();
    component.dateNotificationDecisionOnem.set('2026-05-10');
    component.onRecoursAdminChange(true);
    component.dateDecisionDirecteur.set('2026-06-15');
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.body.recoursAdminDejaForme).toBe(true);
    expect(req.request.body.dateDecisionDirecteur).toBe('2026-06-15');

    req.flush(CAS_B_IMMINENT);
    expect(component.result()?.verdict).toBe('RECOURS_TRIBUNAL_IMMINENT');
  });

  // ── Verdict 6 états — classes CSS ────────────────────────────────────

  it('verdictClass green for *_OUVERT', () => {
    expect(component.verdictClass('RECOURS_ADMIN_OUVERT')).toBe('verdict-ok');
    expect(component.verdictClass('RECOURS_TRIBUNAL_OUVERT')).toBe('verdict-ok');
  });

  it('verdictClass amber for *_IMMINENT', () => {
    expect(component.verdictClass('RECOURS_ADMIN_IMMINENT')).toBe('verdict-warn');
    expect(component.verdictClass('RECOURS_TRIBUNAL_IMMINENT')).toBe('verdict-warn');
  });

  it('verdictClass red for *_PRESCRIT', () => {
    expect(component.verdictClass('RECOURS_ADMIN_PRESCRIT')).toBe('verdict-critical');
    expect(component.verdictClass('RECOURS_TRIBUNAL_PRESCRIT')).toBe('verdict-critical');
  });

  // ── Forclusion totale & saut palier ──────────────────────────────────

  it('should detect FORCLUSION_TOTALE when tribunal prescrit', () => {
    initWithExistingResult(CAS_B_FORCLUSION);
    expect(component.isForclusionTotale()).toBe(true);
    expect(component.isSautPalierAdmin()).toBe(false);
  });

  it('should detect saut palier admin when verdict=RECOURS_ADMIN_PRESCRIT (Cas A)', () => {
    initWithExistingResult(CAS_A_PRESCRIT);
    expect(component.isSautPalierAdmin()).toBe(true);
    expect(component.isForclusionTotale()).toBe(false);
  });

  it('should NOT show forclusion alert on Cas A ouvert', () => {
    initWithExistingResult(CAS_A_OUVERT);
    expect(component.isForclusionTotale()).toBe(false);
    expect(component.isSautPalierAdmin()).toBe(false);
  });

  // ── Paliers — joursRestants coloration ───────────────────────────────

  it('joursRestantsClass red when ≤ 0', () => {
    expect(component.joursRestantsClass({
      type: 'TRIBUNAL', dateLimite: '2026-04-05', joursRestants: -10, baseJuridique: '',
    })).toBe('jours-critical');
  });

  it('joursRestantsClass amber when ≤ 7 ADMIN', () => {
    expect(component.joursRestantsClass({
      type: 'ADMIN', dateLimite: '2026-05-15', joursRestants: 5, baseJuridique: '',
    })).toBe('jours-warning');
  });

  it('joursRestantsClass amber when ≤ 14 TRIBUNAL', () => {
    expect(component.joursRestantsClass({
      type: 'TRIBUNAL', dateLimite: '2026-05-20', joursRestants: 10, baseJuridique: '',
    })).toBe('jours-warning');
  });

  it('joursRestantsClass green when > seuil', () => {
    expect(component.joursRestantsClass({
      type: 'ADMIN', dateLimite: '2026-06-10', joursRestants: 25, baseJuridique: '',
    })).toBe('jours-ok');
  });

  it('joursRestantsClass empty when null (palier indéterminé)', () => {
    expect(component.joursRestantsClass({
      type: 'TRIBUNAL', dateLimite: null, joursRestants: null, baseJuridique: '',
    })).toBe('');
  });

  // ── Refresh dashboard ────────────────────────────────────────────────

  it('should trigger dashboard refresh on successful POST', () => {
    initWithNoExistingResult();
    component.dateNotificationDecisionOnem.set('2026-05-10');
    component.calculate();
    httpMock.expectOne(API_URL).flush(CAS_A_OUVERT);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  // ── Erreurs ──────────────────────────────────────────────────────────

  it('should show snackbar on 500', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.dateNotificationDecisionOnem.set('2026-05-10');
    component.calculate();
    httpMock.expectOne(API_URL).flush('boom', { status: 500, statusText: 'Server Error' });
    expect(spy).toHaveBeenCalled();
    expect(component.calculating()).toBe(false);
  });

  it('should show snackbar with "Dossier introuvable" on 404 POST', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.dateNotificationDecisionOnem.set('2026-05-10');
    component.calculate();
    httpMock.expectOne(API_URL).flush('not found', { status: 404, statusText: 'Not Found' });
    expect(spy).toHaveBeenCalledWith('Dossier introuvable', 'Fermer', expect.any(Object));
  });

  it('should refuse calculate when workspaceCountry FR (defense in depth)', () => {
    initWithNoExistingResult();
    component.dateNotificationDecisionOnem.set('2026-05-10');
    component.workspaceCountry = 'FRANCE';
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    component.calculate();
    httpMock.expectNone(API_URL);
    expect(spy).toHaveBeenCalled();
  });

  // ── Cohérence alerts ──────────────────────────────────────────────────

  it('should alert DATE_NOTIFICATION when IA diverges > 15 days', () => {
    component.aiData = { dateNotificationDecisionOnem: '2026-01-15' } as any;
    initWithNoExistingResult();
    component.dateNotificationDecisionOnem.set('2026-03-01'); // > 15 jours
    const alert = component.coherenceAlerts().DATE_NOTIFICATION;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('2026-01-15');
  });

  it('should NOT alert DATE_NOTIFICATION within tolerance', () => {
    component.aiData = { dateNotificationDecisionOnem: '2026-01-15' } as any;
    initWithNoExistingResult();
    component.dateNotificationDecisionOnem.set('2026-01-20'); // < 15 jours
    expect(component.coherenceAlerts().DATE_NOTIFICATION).toBeUndefined();
  });

  it('should alert RECOURS_ADMIN when IA says true but avocat decoche', () => {
    component.aiData = { recoursAdminDejaForme: true } as any;
    initWithNoExistingResult();
    // IA prefill set true.
    expect(component.recoursAdminDejaForme()).toBe(true);
    component.onRecoursAdminChange(false);
    const alert = component.coherenceAlerts().RECOURS_ADMIN;
    expect(alert?.source).toBe('IA');
  });

  it('should freeze alerts when result is displayed (showForm=false)', () => {
    component.aiData = { dateNotificationDecisionOnem: '2026-01-15' } as any;
    initWithExistingResult({ ...CAS_A_OUVERT, dateNotificationDecisionOnem: '2026-05-10' });
    expect(component.showForm()).toBe(false);
    expect(component.alertsSummary().total).toBe(0);
  });

  // ── editMode ──────────────────────────────────────────────────────────

  it('editMode reopens form, alerts re-active', () => {
    component.aiData = { dateNotificationDecisionOnem: '2026-01-15' } as any;
    initWithExistingResult({ ...CAS_A_OUVERT, dateNotificationDecisionOnem: '2026-05-10' });
    expect(component.showForm()).toBe(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
    expect(component.coherenceAlerts().DATE_NOTIFICATION?.source).toBe('IA');
  });

  // ── Display existing result ──────────────────────────────────────────

  it('should display existing result from GET (Cas A OUVERT)', () => {
    initWithExistingResult(CAS_A_OUVERT);
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.dateNotificationDecisionOnem()).toBe('2026-05-10');
    expect(component.recoursAdminDejaForme()).toBe(false);
    // Reload backend = saisie avocat = jamais de badge IA.
    expect(component.provenanceDateNotification()).toBeNull();
  });
});

// Couvre le static `getPrefillCount` exposé pour la card du panel F-IA-04.
describe('ContestationC4OnemSectionComponent.getPrefillCount', () => {
  it('returns 0 when no aiData', () => {
    expect(ContestationC4OnemSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('returns 1 when only dateNotificationDecisionOnem set', () => {
    expect(
      ContestationC4OnemSectionComponent.getPrefillCount({
        aiData: { dateNotificationDecisionOnem: '2026-05-10' },
      }),
    ).toBe(1);
  });

  it('returns 3 nominal — tous les champs instrumentés', () => {
    expect(
      ContestationC4OnemSectionComponent.getPrefillCount({
        aiData: {
          dateNotificationDecisionOnem: '2026-05-10',
          dateDecisionDirecteur: '2026-06-15',
          recoursAdminDejaForme: true,
        },
      }),
    ).toBe(3);
  });
});
