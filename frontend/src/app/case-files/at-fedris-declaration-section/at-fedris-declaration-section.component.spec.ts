import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { AtFedrisDeclarationSectionComponent } from './at-fedris-declaration-section.component';
import { AtFedrisDeclarationResponse } from '../../core/models/at-fedris-declaration.model';

describe('AtFedrisDeclarationSectionComponent', () => {
  let component: AtFedrisDeclarationSectionComponent;
  let fixture: ComponentFixture<AtFedrisDeclarationSectionComponent>;
  let httpMock: HttpTestingController;
  let refreshSpy: { triggerRefresh: jest.Mock };

  const CASE_FILE_ID = '55555555-5555-5555-5555-555555555555';
  const API_URL =
    `/api/v1/case-files/${CASE_FILE_ID}/decision-tools/at-fedris-declaration`;

  // Délai ouvert prospectif (joursRestants > 2).
  const DELAI_OUVERT: AtFedrisDeclarationResponse = {
    caseFileId: CASE_FILE_ID,
    dateAccident: '2026-05-15',
    dateConnaissanceEmployeur: '2026-05-15',
    dateActionEnvisagee: '2026-05-16',
    dateDeclarationEffectuee: null,
    verdict: 'DELAI_OUVERT',
    dateLimiteDeclaration: '2026-05-23',
    joursRestants: 7,
    regleAppliquee: 'AT_FEDRIS_DECLARATION_8_JOURS_PROSPECTIF',
    baseJuridique: 'Loi 10/04/1971 art. 62 ; AR 21/12/1971 art. 25',
    formuleCalcul: 'dateConnaissance (2026-05-15) + 8 jours = 2026-05-23.',
    consequencesNonRespect: 'Préjudice indemnisation du salarié, intérêts moratoires.',
    country: 'BELGIQUE',
  };

  // Délai imminent prospectif (joursRestants ∈ [0, 2]).
  const DELAI_IMMINENT: AtFedrisDeclarationResponse = {
    ...DELAI_OUVERT,
    verdict: 'DELAI_IMMINENT',
    joursRestants: 1,
  };

  // Délai dépassé prospectif (joursRestants < 0).
  const DELAI_DEPASSE: AtFedrisDeclarationResponse = {
    ...DELAI_OUVERT,
    verdict: 'DELAI_DEPASSE',
    joursRestants: -5,
  };

  // Mode rétrospectif — déclaration dans les temps.
  const DECLARATION_DANS_LES_TEMPS: AtFedrisDeclarationResponse = {
    ...DELAI_OUVERT,
    dateDeclarationEffectuee: '2026-05-20',
    verdict: 'DECLARATION_DANS_LES_TEMPS',
    joursRestants: 3,
  };

  // Mode rétrospectif — déclaration hors délai.
  const DECLARATION_HORS_DELAI: AtFedrisDeclarationResponse = {
    ...DELAI_OUVERT,
    dateDeclarationEffectuee: '2026-06-01',
    verdict: 'DECLARATION_HORS_DELAI',
    joursRestants: -9,
  };

  beforeEach(async () => {
    refreshSpy = { triggerRefresh: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [AtFedrisDeclarationSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AtFedrisDeclarationSectionComponent);
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

  function initWithExistingResult(resp: AtFedrisDeclarationResponse = DELAI_OUVERT): void {
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

  // ── Pré-fill IA (2 champs) ───────────────────────────────────────────

  it('should prefill both instrumented fields from full aiData', () => {
    component.aiData = {
      dateAccident: '2026-05-15',
      dateConnaissanceAccidentEmployeur: '2026-05-16',
    } as any;
    initWithNoExistingResult();
    expect(component.dateAccident()).toBe('2026-05-15');
    expect(component.dateConnaissanceEmployeur()).toBe('2026-05-16');

    // Badges IA visibles.
    expect(component.provenanceDateAccident()).toBe('IA');
    expect(component.provenanceDateConnaissance()).toBe('IA');
  });

  it('should clear provenance on manual date accident change', () => {
    component.aiData = { dateAccident: '2026-05-15' } as any;
    initWithNoExistingResult();
    expect(component.provenanceDateAccident()).toBe('IA');
    component.onDateAccidentChange('2026-06-01');
    expect(component.provenanceDateAccident()).toBeNull();
    expect(component.dateAccident()).toBe('2026-06-01');
  });

  it('should NOT prefill anything if workspaceCountry FR', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = { dateAccident: '2026-05-15' } as any;
    fixture.detectChanges();
    httpMock.expectNone(API_URL);
    expect(component.dateAccident()).toBeNull();
  });

  // ── Mode prospectif vs rétrospectif (checkbox) ───────────────────────

  it('should be in prospectif mode by default (checkbox off, no dateDeclarationEffectuee)', () => {
    initWithNoExistingResult();
    expect(component.declarationDejaEffectuee()).toBe(false);
    component.dateAccident.set('2026-05-15');
    expect(component.formValid()).toBe(true); // Pas besoin de dateDeclaration.
  });

  it('should require dateDeclarationEffectuee when checkbox is on (rétrospectif)', () => {
    initWithNoExistingResult();
    component.dateAccident.set('2026-05-15');
    component.onDeclarationDejaEffectueeChange(true);
    expect(component.formValid()).toBe(false); // dateDeclaration manquante
    component.dateDeclarationEffectuee.set('2026-05-20');
    expect(component.formValid()).toBe(true);
  });

  it('should clear dateDeclarationEffectuee when toggling checkbox off', () => {
    initWithNoExistingResult();
    component.onDeclarationDejaEffectueeChange(true);
    component.dateDeclarationEffectuee.set('2026-05-20');
    component.onDeclarationDejaEffectueeChange(false);
    expect(component.dateDeclarationEffectuee()).toBeNull();
  });

  // ── POST calculate ────────────────────────────────────────────────────

  it('should NOT POST when required field missing', () => {
    initWithNoExistingResult();
    component.calculate();
    httpMock.expectNone(API_URL);
  });

  it('should POST prospectif (checkbox off) and not send dateDeclarationEffectuee', () => {
    initWithNoExistingResult();
    component.dateAccident.set('2026-05-15');
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateAccident).toBe('2026-05-15');
    expect(req.request.body.dateDeclarationEffectuee).toBeNull();

    req.flush(DELAI_OUVERT);
    expect(component.result()?.verdict).toBe('DELAI_OUVERT');
    expect(component.showForm()).toBe(false);
  });

  it('should POST rétrospectif with dateDeclarationEffectuee and show HORS_DELAI verdict', () => {
    initWithNoExistingResult();
    component.dateAccident.set('2026-05-15');
    component.onDeclarationDejaEffectueeChange(true);
    component.dateDeclarationEffectuee.set('2026-06-01');
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.body.dateDeclarationEffectuee).toBe('2026-06-01');

    req.flush(DECLARATION_HORS_DELAI);
    expect(component.result()?.verdict).toBe('DECLARATION_HORS_DELAI');
  });

  // ── Verdict 5 états — classes CSS ────────────────────────────────────

  it('verdictClass green for DELAI_OUVERT and DECLARATION_DANS_LES_TEMPS', () => {
    expect(component.verdictClass('DELAI_OUVERT')).toBe('verdict-ok');
    expect(component.verdictClass('DECLARATION_DANS_LES_TEMPS')).toBe('verdict-ok');
  });

  it('verdictClass amber for DELAI_IMMINENT', () => {
    expect(component.verdictClass('DELAI_IMMINENT')).toBe('verdict-warn');
  });

  it('verdictClass red for DELAI_DEPASSE and DECLARATION_HORS_DELAI', () => {
    expect(component.verdictClass('DELAI_DEPASSE')).toBe('verdict-critical');
    expect(component.verdictClass('DECLARATION_HORS_DELAI')).toBe('verdict-critical');
  });

  // ── Conséquences encart — coloration ─────────────────────────────────

  it('consequencesClass red on DELAI_DEPASSE / HORS_DELAI', () => {
    expect(component.consequencesClass('DELAI_DEPASSE')).toBe('alert-critical');
    expect(component.consequencesClass('DECLARATION_HORS_DELAI')).toBe('alert-critical');
  });

  it('consequencesClass amber on DELAI_IMMINENT', () => {
    expect(component.consequencesClass('DELAI_IMMINENT')).toBe('alert-warning');
  });

  it('consequencesClass info on DELAI_OUVERT / DANS_LES_TEMPS', () => {
    expect(component.consequencesClass('DELAI_OUVERT')).toBe('alert-info');
    expect(component.consequencesClass('DECLARATION_DANS_LES_TEMPS')).toBe('alert-info');
  });

  // ── joursRestants — coloration & libellé retard ──────────────────────

  it('joursRestantsClass red when negative', () => {
    initWithExistingResult(DELAI_DEPASSE);
    expect(component.joursRestantsClass()).toBe('jours-critical');
    expect(component.isJoursEnRetard()).toBe(true);
    expect(component.joursDeRetard()).toBe(5);
  });

  it('joursRestantsClass amber when ≤ 2', () => {
    initWithExistingResult(DELAI_IMMINENT);
    expect(component.joursRestantsClass()).toBe('jours-warning');
    expect(component.isJoursEnRetard()).toBe(false);
  });

  it('joursRestantsClass green when > 2', () => {
    initWithExistingResult(DELAI_OUVERT);
    expect(component.joursRestantsClass()).toBe('jours-ok');
    expect(component.isJoursEnRetard()).toBe(false);
  });

  it('rétrospectif HORS_DELAI affiche le retard en valeur absolue', () => {
    initWithExistingResult(DECLARATION_HORS_DELAI);
    expect(component.isJoursEnRetard()).toBe(true);
    expect(component.joursDeRetard()).toBe(9);
  });

  // ── Refresh dashboard ────────────────────────────────────────────────

  it('should trigger dashboard refresh on successful POST', () => {
    initWithNoExistingResult();
    component.dateAccident.set('2026-05-15');
    component.calculate();
    httpMock.expectOne(API_URL).flush(DELAI_OUVERT);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  // ── Erreurs ──────────────────────────────────────────────────────────

  it('should show snackbar on 500', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.dateAccident.set('2026-05-15');
    component.calculate();
    httpMock.expectOne(API_URL).flush('boom', { status: 500, statusText: 'Server Error' });
    expect(spy).toHaveBeenCalled();
    expect(component.calculating()).toBe(false);
  });

  it('should show snackbar with "Dossier introuvable" on 404 POST', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.dateAccident.set('2026-05-15');
    component.calculate();
    httpMock.expectOne(API_URL).flush('not found', { status: 404, statusText: 'Not Found' });
    expect(spy).toHaveBeenCalledWith('Dossier introuvable', 'Fermer', expect.any(Object));
  });

  it('should refuse calculate when workspaceCountry FR (defense in depth)', () => {
    initWithNoExistingResult();
    component.dateAccident.set('2026-05-15');
    component.workspaceCountry = 'FRANCE';
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    component.calculate();
    httpMock.expectNone(API_URL);
    expect(spy).toHaveBeenCalled();
  });

  // ── Cohérence alerts ──────────────────────────────────────────────────

  it('should alert DATE_ACCIDENT when IA diverges > 15 days', () => {
    component.aiData = { dateAccident: '2026-01-15' } as any;
    initWithNoExistingResult();
    component.dateAccident.set('2026-03-01'); // > 15 jours
    const alert = component.coherenceAlerts().DATE_ACCIDENT;
    expect(alert?.source).toBe('IA');
    expect(alert?.expectedDisplay).toBe('2026-01-15');
  });

  it('should NOT alert DATE_ACCIDENT within tolerance', () => {
    component.aiData = { dateAccident: '2026-01-15' } as any;
    initWithNoExistingResult();
    component.dateAccident.set('2026-01-20'); // < 15 jours
    expect(component.coherenceAlerts().DATE_ACCIDENT).toBeUndefined();
  });

  it('should freeze alerts when result is displayed (showForm=false)', () => {
    component.aiData = { dateAccident: '2026-01-15' } as any;
    initWithExistingResult({ ...DELAI_OUVERT, dateAccident: '2026-05-15' });
    expect(component.showForm()).toBe(false);
    expect(component.alertsSummary().total).toBe(0);
  });

  // ── editMode ──────────────────────────────────────────────────────────

  it('editMode reopens form, alerts re-active', () => {
    component.aiData = { dateAccident: '2026-01-15' } as any;
    initWithExistingResult({ ...DELAI_OUVERT, dateAccident: '2026-05-15' });
    expect(component.showForm()).toBe(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
    expect(component.coherenceAlerts().DATE_ACCIDENT?.source).toBe('IA');
  });

  // ── Display existing result ──────────────────────────────────────────

  it('should display existing prospectif result from GET (DELAI_OUVERT)', () => {
    initWithExistingResult(DELAI_OUVERT);
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.dateAccident()).toBe('2026-05-15');
    expect(component.declarationDejaEffectuee()).toBe(false);
    // Reload backend = saisie avocat = jamais de badge IA.
    expect(component.provenanceDateAccident()).toBeNull();
  });

  it('should display existing rétrospectif result from GET (DANS_LES_TEMPS)', () => {
    initWithExistingResult(DECLARATION_DANS_LES_TEMPS);
    expect(component.declarationDejaEffectuee()).toBe(true);
    expect(component.dateDeclarationEffectuee()).toBe('2026-05-20');
  });
});

// Couvre le static `getPrefillCount` exposé pour la card du panel F-IA-04.
describe('AtFedrisDeclarationSectionComponent.getPrefillCount', () => {
  it('returns 0 when no aiData', () => {
    expect(AtFedrisDeclarationSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('returns 1 when only dateAccident set', () => {
    expect(
      AtFedrisDeclarationSectionComponent.getPrefillCount({
        aiData: { dateAccident: '2026-05-15' },
      }),
    ).toBe(1);
  });

  it('returns 2 nominal — tous les champs instrumentés', () => {
    expect(
      AtFedrisDeclarationSectionComponent.getPrefillCount({
        aiData: {
          dateAccident: '2026-05-15',
          dateConnaissanceAccidentEmployeur: '2026-05-16',
        },
      }),
    ).toBe(2);
  });
});
