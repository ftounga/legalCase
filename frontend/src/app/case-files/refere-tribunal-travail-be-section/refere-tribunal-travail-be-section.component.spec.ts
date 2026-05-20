import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { RefereTribunalTravailBeSectionComponent } from './refere-tribunal-travail-be-section.component';
import { RefereTribunalTravailBeResponse } from '../../core/models/refere-tribunal-travail-be.model';

describe('RefereTribunalTravailBeSectionComponent', () => {
  let component: RefereTribunalTravailBeSectionComponent;
  let fixture: ComponentFixture<RefereTribunalTravailBeSectionComponent>;
  let httpMock: HttpTestingController;
  let refreshSpy: { triggerRefresh: jest.Mock };

  const CASE_FILE_ID = '44444444-4444-4444-4444-444444444444';
  const API_URL =
    `/api/v1/case-files/${CASE_FILE_ID}/decision-tools/refere-tribunal-travail-be`;

  const ELIGIBLE_RESPONSE: RefereTribunalTravailBeResponse = {
    caseFileId: CASE_FILE_ID,
    motifUrgence: 'HARCELEMENT',
    motifUrgenceDescription: 'Harcèlement moral persistant attesté par 3 témoignages.',
    dateFaitGenerateur: '2026-04-01',
    dateDemarcheAmiable: '2026-04-15',
    preuveUrgenceJointe: true,
    mesureProvisoireDemandee: 'Suspension immédiate du harceleur de l\'organigramme.',
    perilEnDemeure: true,
    competenceTerritorialeIdentifiee: true,
    verdict: 'REFERE_ELIGIBLE',
    conditionsNonRemplies: [],
    scoreConditions: 5,
    requeteSquelette: 'REQUÊTE EN RÉFÉRÉ\nDevant Monsieur le Président du Tribunal du travail...',
    baseJuridique: 'CJ art. 584 ; CJ art. 627, 9° ; Loi du 3 juillet 1978',
    etapeSuivante: 'DEPOSER_REQUETE',
    country: 'BELGIQUE',
  };

  const INCERTAIN_RESPONSE: RefereTribunalTravailBeResponse = {
    ...ELIGIBLE_RESPONSE,
    competenceTerritorialeIdentifiee: false,
    preuveUrgenceJointe: false,
    verdict: 'REFERE_INCERTAIN',
    conditionsNonRemplies: ['PEUVE_URGENCE_JOINTE', 'COMPETENCE_IDENTIFIEE'],
    scoreConditions: 3,
    requeteSquelette: 'REQUÊTE EN RÉFÉRÉ (à compléter)\n...',
    etapeSuivante: 'RENFORCER_DOSSIER',
  };

  const NON_ELIGIBLE_RESPONSE: RefereTribunalTravailBeResponse = {
    ...ELIGIBLE_RESPONSE,
    motifUrgence: 'AUTRE',
    motifUrgenceDescription: 'court',
    preuveUrgenceJointe: false,
    perilEnDemeure: false,
    competenceTerritorialeIdentifiee: false,
    mesureProvisoireDemandee: 'flou',
    verdict: 'REFERE_NON_ELIGIBLE',
    conditionsNonRemplies: [
      'URGENCE_QUALIFIABLE',
      'PEUVE_URGENCE_JOINTE',
      'PERIL_EN_DEMEURE',
      'MESURE_PROVISOIRE_PRECISE',
      'COMPETENCE_IDENTIFIEE',
    ],
    scoreConditions: 0,
    requeteSquelette: null,
    etapeSuivante: 'ALTERNATIVE_PROCEDURE_FOND',
  };

  beforeEach(async () => {
    refreshSpy = { triggerRefresh: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [RefereTribunalTravailBeSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RefereTribunalTravailBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
    component.workspaceCountry = 'BELGIQUE';
  });

  afterEach(() => httpMock.verify());

  function initWithNoExistingResult(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
  }

  function initWithExistingResult(resp: RefereTribunalTravailBeResponse = ELIGIBLE_RESPONSE): void {
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
    expect(component.motifUrgence()).toBe('HARCELEMENT');
    expect(component.dateFaitGenerateur()).toBe('2026-04-01');
    expect(component.perilEnDemeure()).toBe(true);
    // Reload backend = saisie avocat = jamais de badge IA.
    expect(component.provenanceMotif()).toBeNull();
    expect(component.provenanceDate()).toBeNull();
    expect(component.provenancePeril()).toBeNull();
  });

  // ── Pré-fill IA — 3 champs ───────────────────────────────────────────

  it('should prefill all 3 instrumented fields from full aiData', () => {
    component.aiData = {
      motifUrgenceDetecte: 'SALAIRE_IMPAYE',
      dateFaitGenerateurUrgence: '2026-03-10',
      perilImmediatPresume: true,
    } as any;
    initWithNoExistingResult();
    expect(component.motifUrgence()).toBe('SALAIRE_IMPAYE');
    expect(component.dateFaitGenerateur()).toBe('2026-03-10');
    expect(component.perilEnDemeure()).toBe(true);
    expect(component.provenanceMotif()).toBe('IA');
    expect(component.provenanceDate()).toBe('IA');
    expect(component.provenancePeril()).toBe('IA');
  });

  it('should NOT prefill anything if workspaceCountry is FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = { motifUrgenceDetecte: 'HARCELEMENT' } as any;
    fixture.detectChanges();
    httpMock.expectNone(API_URL);
    expect(component.motifUrgence()).toBeNull();
  });

  it('should clear provenance on manual motif change', () => {
    component.aiData = { motifUrgenceDetecte: 'HARCELEMENT' } as any;
    initWithNoExistingResult();
    expect(component.provenanceMotif()).toBe('IA');
    component.onMotifUrgenceChange('MODIFICATION_UNILATERALE');
    expect(component.provenanceMotif()).toBeNull();
    expect(component.motifUrgence()).toBe('MODIFICATION_UNILATERALE');
  });

  it('should clear provenance on manual peril toggle', () => {
    component.aiData = { perilImmediatPresume: true } as any;
    initWithNoExistingResult();
    expect(component.provenancePeril()).toBe('IA');
    expect(component.perilEnDemeure()).toBe(true);
    component.onPerilEnDemeureChange(false);
    expect(component.provenancePeril()).toBeNull();
    expect(component.perilEnDemeure()).toBe(false);
  });

  it('should ignore prefill for motif hors whitelist', () => {
    component.aiData = { motifUrgenceDetecte: 'LICENCIEMENT_ABUSIF' } as any;
    initWithNoExistingResult();
    expect(component.motifUrgence()).toBeNull();
    expect(component.provenanceMotif()).toBeNull();
  });

  // ── formValid ─────────────────────────────────────────────────────────

  it('formValid requires motif + dateFaitGenerateur + description + mesure (non-AUTRE)', () => {
    initWithNoExistingResult();
    expect(component.formValid()).toBe(false);
    component.motifUrgence.set('HARCELEMENT');
    component.motifUrgenceDescription.set('description courte');
    component.dateFaitGenerateur.set('2026-04-01');
    expect(component.formValid()).toBe(false); // mesure manquante
    component.mesureProvisoireDemandee.set('Suspension du harceleur.');
    expect(component.formValid()).toBe(true);
  });

  it('formValid impose ≥ 30 caractères de description pour motif AUTRE', () => {
    initWithNoExistingResult();
    component.motifUrgence.set('AUTRE');
    component.dateFaitGenerateur.set('2026-04-01');
    component.mesureProvisoireDemandee.set('Suspension immédiate');
    component.motifUrgenceDescription.set('court'); // < 30
    expect(component.formValid()).toBe(false);
    component.motifUrgenceDescription.set('Description suffisamment longue pour qualifier l\'urgence.');
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
    component.motifUrgence.set('HARCELEMENT');
    component.motifUrgenceDescription.set('Harcèlement attesté par 3 témoignages.');
    component.dateFaitGenerateur.set('2026-04-01');
    component.mesureProvisoireDemandee.set('Suspension immédiate du harceleur.');
    component.preuveUrgenceJointe.set(true);
    component.perilEnDemeure.set(true);
    component.competenceTerritorialeIdentifiee.set(true);
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.motifUrgence).toBe('HARCELEMENT');
    expect(req.request.body.dateFaitGenerateur).toBe('2026-04-01');
    expect(req.request.body.perilEnDemeure).toBe(true);

    req.flush(ELIGIBLE_RESPONSE);
    expect(component.result()?.verdict).toBe('REFERE_ELIGIBLE');
    expect(component.showForm()).toBe(false);
  });

  it('should display INCERTAIN verdict + conditions list + squelette', () => {
    initWithExistingResult(INCERTAIN_RESPONSE);
    expect(component.result()?.verdict).toBe('REFERE_INCERTAIN');
    expect(component.conditionsNonRempliesHumanisees().length).toBe(2);
    const codes = component.conditionsNonRempliesHumanisees().map(c => c.code);
    expect(codes).toContain('PEUVE_URGENCE_JOINTE');
    expect(codes).toContain('COMPETENCE_IDENTIFIEE');
    expect(component.result()?.requeteSquelette).toContain('RÉFÉRÉ');
  });

  it('should display NON_ELIGIBLE verdict — squelette null, conditions all listed', () => {
    initWithExistingResult(NON_ELIGIBLE_RESPONSE);
    expect(component.result()?.verdict).toBe('REFERE_NON_ELIGIBLE');
    expect(component.result()?.requeteSquelette).toBeNull();
    expect(component.conditionsNonRempliesHumanisees().length).toBe(5);
    expect(component.result()?.scoreConditions).toBe(0);
  });

  it('should trigger dashboard refresh on successful POST', () => {
    initWithNoExistingResult();
    component.motifUrgence.set('HARCELEMENT');
    component.motifUrgenceDescription.set('Harcèlement attesté.');
    component.dateFaitGenerateur.set('2026-04-01');
    component.mesureProvisoireDemandee.set('Suspension immédiate du harceleur.');
    component.calculate();
    httpMock.expectOne(API_URL).flush(ELIGIBLE_RESPONSE);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  // ── Erreurs ──────────────────────────────────────────────────────────

  it('should show snackbar on 500', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.motifUrgence.set('HARCELEMENT');
    component.motifUrgenceDescription.set('Harcèlement attesté.');
    component.dateFaitGenerateur.set('2026-04-01');
    component.mesureProvisoireDemandee.set('Suspension immédiate du harceleur.');
    component.calculate();
    httpMock.expectOne(API_URL).flush('boom', { status: 500, statusText: 'Server Error' });
    expect(spy).toHaveBeenCalled();
    expect(component.calculating()).toBe(false);
  });

  it('should show snackbar with "Dossier introuvable" on 404 POST', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.motifUrgence.set('HARCELEMENT');
    component.motifUrgenceDescription.set('Harcèlement attesté.');
    component.dateFaitGenerateur.set('2026-04-01');
    component.mesureProvisoireDemandee.set('Suspension immédiate du harceleur.');
    component.calculate();
    httpMock.expectOne(API_URL).flush('not found', { status: 404, statusText: 'Not Found' });
    expect(spy).toHaveBeenCalledWith('Dossier introuvable', 'Fermer', expect.any(Object));
  });

  it('should refuse calculate when workspaceCountry FR (defense in depth)', () => {
    initWithNoExistingResult();
    component.motifUrgence.set('HARCELEMENT');
    component.motifUrgenceDescription.set('Harcèlement attesté.');
    component.dateFaitGenerateur.set('2026-04-01');
    component.mesureProvisoireDemandee.set('Suspension immédiate du harceleur.');
    component.workspaceCountry = 'FRANCE';
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    component.calculate();
    httpMock.expectNone(API_URL);
    expect(spy).toHaveBeenCalled();
  });

  // ── Verdict helpers ──────────────────────────────────────────────────

  it('verdictClass + verdictIcon couvrent les 3 verdicts', () => {
    expect(component.verdictClass('REFERE_ELIGIBLE')).toBe('verdict-ok');
    expect(component.verdictClass('REFERE_INCERTAIN')).toBe('verdict-warn');
    expect(component.verdictClass('REFERE_NON_ELIGIBLE')).toBe('verdict-critical');
    expect(component.verdictIcon('REFERE_ELIGIBLE')).toBe('check_circle');
    expect(component.verdictIcon('REFERE_INCERTAIN')).toBe('warning');
    expect(component.verdictIcon('REFERE_NON_ELIGIBLE')).toBe('block');
  });

  it('scorePercent = scoreConditions * 20 (5 / 5 → 100)', () => {
    initWithExistingResult(ELIGIBLE_RESPONSE);
    expect(component.scorePercent()).toBe(100);
    initWithNoExistingResult.bind(component); // reset not needed; new fixture below
  });

  it('scoreBarColor coloriée selon verdict', () => {
    expect(component.scoreBarColor('REFERE_ELIGIBLE')).toBe('primary');
    expect(component.scoreBarColor('REFERE_INCERTAIN')).toBe('accent');
    expect(component.scoreBarColor('REFERE_NON_ELIGIBLE')).toBe('warn');
    expect(component.scoreBarColor(undefined)).toBe('primary');
  });

  it('etapeClass + etapeIcon couvrent les 3 étapes', () => {
    expect(component.etapeClass('DEPOSER_REQUETE')).toBe('etape-ok');
    expect(component.etapeClass('RENFORCER_DOSSIER')).toBe('etape-warn');
    expect(component.etapeClass('ALTERNATIVE_PROCEDURE_FOND')).toBe('etape-info');
    expect(component.etapeIcon('DEPOSER_REQUETE')).toBe('send');
    expect(component.etapeIcon('RENFORCER_DOSSIER')).toBe('build');
    expect(component.etapeIcon('ALTERNATIVE_PROCEDURE_FOND')).toBe('info');
  });

  // ── Clipboard / squelette de requête ─────────────────────────────────

  it('copyRequete writes squelette to clipboard (success path)', async () => {
    const writeSpy = jest
      .fn<Promise<void>, [string]>()
      .mockReturnValue(Promise.resolve());
    Object.assign(navigator, { clipboard: { writeText: writeSpy } });
    initWithExistingResult(ELIGIBLE_RESPONSE);

    component.copyRequete();
    await Promise.resolve();
    expect(writeSpy).toHaveBeenCalledWith(ELIGIBLE_RESPONSE.requeteSquelette);
    expect(component.copying()).toBe(false);
  });

  it('copyRequete no-op when no squelette (verdict NON_ELIGIBLE)', () => {
    const writeSpy = jest.fn();
    Object.assign(navigator, { clipboard: { writeText: writeSpy } });
    initWithExistingResult(NON_ELIGIBLE_RESPONSE);
    component.copyRequete();
    expect(writeSpy).not.toHaveBeenCalled();
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
describe('RefereTribunalTravailBeSectionComponent.getPrefillCount', () => {
  it('returns 0 when no aiData', () => {
    expect(RefereTribunalTravailBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('returns 1 when only motifUrgenceDetecte is set', () => {
    expect(
      RefereTribunalTravailBeSectionComponent.getPrefillCount({
        aiData: { motifUrgenceDetecte: 'HARCELEMENT' } as any,
      }),
    ).toBe(1);
  });

  it('returns 3 nominal — tous les champs instrumentés', () => {
    expect(
      RefereTribunalTravailBeSectionComponent.getPrefillCount({
        aiData: {
          motifUrgenceDetecte: 'HARCELEMENT',
          dateFaitGenerateurUrgence: '2026-04-01',
          perilImmediatPresume: true,
        } as any,
      }),
    ).toBe(3);
  });
});
