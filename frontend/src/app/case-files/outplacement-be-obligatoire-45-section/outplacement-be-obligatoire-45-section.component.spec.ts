import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { OutplacementBeObligatoire45SectionComponent } from './outplacement-be-obligatoire-45-section.component';
import { OutplacementBeResponse } from '../../core/models/outplacement-be-obligatoire-45.model';

describe('OutplacementBeObligatoire45SectionComponent', () => {
  let component: OutplacementBeObligatoire45SectionComponent;
  let fixture: ComponentFixture<OutplacementBeObligatoire45SectionComponent>;
  let httpMock: HttpTestingController;
  let refreshSpy: { triggerRefresh: jest.Mock };

  const CASE_FILE_ID = '77777777-7777-7777-7777-777777777777';
  const API_URL =
    `/api/v1/case-files/${CASE_FILE_ID}/decision-tools/outplacement-be-obligatoire-45`;

  const OUTPLACEMENT_NON_DU_RESPONSE: OutplacementBeResponse = {
    caseFileId: CASE_FILE_ID,
    dateLicenciement: '2025-03-15',
    dateNaissanceSalarie: '1985-06-12',
    ancienneteAnnees: 5,
    motifLicenciement: 'LICENCIEMENT_AUTRE',
    contratTempsPlein: true,
    offreOutplacementRecue: false,
    dateOffreOutplacement: null,
    offreConformeCCT82: null,
    salarieAcceptantOffre: null,
    verdict: 'OUTPLACEMENT_NON_DU',
    ageALaDateLicenciement: 39,
    obligationEmployeurApplicable: false,
    raisonNonObligation: 'AGE_INFERIEUR_45',
    sanctionEmployeurEuros: 0,
    sanctionSalarieRange: null,
    dateLimiteOffre: null,
    delaiOffreRespecte: null,
    etapeSuivante: 'AUCUNE',
    baseJuridique: 'CCT n°82 ; CCT n°82 bis ; Loi 05/09/2001 art. 13',
    formuleCalcul: 'Âge 39 ans < 45 → obligation employeur non applicable.',
    country: 'BELGIQUE',
  };

  const SANCTION_EMPLOYEUR_RESPONSE: OutplacementBeResponse = {
    ...OUTPLACEMENT_NON_DU_RESPONSE,
    dateNaissanceSalarie: '1975-06-12',
    ancienneteAnnees: 12,
    offreOutplacementRecue: false,
    verdict: 'OFFRE_NON_FAITE_SANCTION_EMPLOYEUR',
    ageALaDateLicenciement: 49,
    obligationEmployeurApplicable: true,
    raisonNonObligation: null,
    sanctionEmployeurEuros: 1800,
    sanctionSalarieRange: null,
    dateLimiteOffre: '2025-03-30',
    delaiOffreRespecte: null,
    etapeSuivante: 'PLAINTE_INSPECTION_LOIS_SOCIALES',
    baseJuridique: 'CCT n°82 ; AR 30/05/2018',
    formuleCalcul: 'Obligation applicable + aucune offre reçue → amende 1 800 €.',
  };

  const SANCTION_SALARIE_RESPONSE: OutplacementBeResponse = {
    ...SANCTION_EMPLOYEUR_RESPONSE,
    offreOutplacementRecue: true,
    dateOffreOutplacement: '2025-03-20',
    offreConformeCCT82: true,
    salarieAcceptantOffre: false,
    verdict: 'OFFRE_REFUSEE_SANCTION_SALARIE',
    sanctionEmployeurEuros: 0,
    sanctionSalarieRange: { minSemaines: 4, maxSemaines: 52 },
    delaiOffreRespecte: true,
    etapeSuivante: 'PRESENTATION_C4_ONEM',
    baseJuridique: 'CCT n°82 ; AR 25/11/1991 art. 154',
    formuleCalcul: 'Offre conforme refusée → exclusion ONEM 4-52 sem.',
  };

  const OFFRE_CONFORME_RESPECTEE_RESPONSE: OutplacementBeResponse = {
    ...SANCTION_EMPLOYEUR_RESPONSE,
    offreOutplacementRecue: true,
    dateOffreOutplacement: '2025-03-20',
    offreConformeCCT82: true,
    salarieAcceptantOffre: true,
    verdict: 'OFFRE_CONFORME_RESPECTEE',
    sanctionEmployeurEuros: 0,
    sanctionSalarieRange: null,
    delaiOffreRespecte: true,
    etapeSuivante: 'AUCUNE',
    baseJuridique: 'CCT n°82',
    formuleCalcul: 'Offre conforme acceptée → conforme.',
  };

  beforeEach(async () => {
    refreshSpy = { triggerRefresh: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [OutplacementBeObligatoire45SectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OutplacementBeObligatoire45SectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
    component.workspaceCountry = 'BELGIQUE';
  });

  afterEach(() => httpMock.verify());

  function initWithNoExistingResult(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
  }

  function initWithExistingResult(resp: OutplacementBeResponse = OUTPLACEMENT_NON_DU_RESPONSE): void {
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
    initWithExistingResult(OFFRE_CONFORME_RESPECTEE_RESPONSE);
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.dateLicenciement()).toBe('2025-03-15');
    expect(component.dateNaissanceSalarie()).toBe('1975-06-12');
    expect(component.motifLicenciement()).toBe('LICENCIEMENT_AUTRE');
    expect(component.offreOutplacementRecue()).toBe(true);
    expect(component.dateOffreOutplacement()).toBe('2025-03-20');
    expect(component.offreConformeCCT82()).toBe(true);
    expect(component.salarieAcceptantOffre()).toBe(true);
    // Reload backend = saisie avocat = jamais de badge IA.
    expect(component.provenanceDateLicenciement()).toBeNull();
    expect(component.provenanceMotif()).toBeNull();
    expect(component.provenanceOffreRecue()).toBeNull();
  });

  // ── Pré-fill IA — 5 champs ──────────────────────────────────────────

  it('should prefill all 5 instrumented fields from full aiData', () => {
    component.aiData = {
      dateLicenciement: '2025-03-15',
      dateNaissanceSalarie: '1975-06-12',
      ancienneteSalarie: 12.5,
      motifLicenciementDetecte: 'LICENCIEMENT_AUTRE',
      offreOutplacementMentionnee: true,
    } as any;
    initWithNoExistingResult();
    expect(component.dateLicenciement()).toBe('2025-03-15');
    expect(component.dateNaissanceSalarie()).toBe('1975-06-12');
    expect(component.ancienneteAnnees()).toBe(12.5);
    expect(component.motifLicenciement()).toBe('LICENCIEMENT_AUTRE');
    expect(component.offreOutplacementRecue()).toBe(true);

    expect(component.provenanceDateLicenciement()).toBe('IA');
    expect(component.provenanceDateNaissance()).toBe('IA');
    expect(component.provenanceAnciennete()).toBe('IA');
    expect(component.provenanceMotif()).toBe('IA');
    expect(component.provenanceOffreRecue()).toBe('IA');
  });

  it('should NOT prefill anything if workspaceCountry is FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = {
      dateLicenciement: '2025-03-15',
      dateNaissanceSalarie: '1975-06-12',
    } as any;
    fixture.detectChanges();
    httpMock.expectNone(API_URL);
    expect(component.dateLicenciement()).toBeNull();
  });

  it('should NOT prefill offreOutplacementRecue when aiData is false (case décochée par défaut)', () => {
    component.aiData = {
      offreOutplacementMentionnee: false,
    } as any;
    initWithNoExistingResult();
    expect(component.offreOutplacementRecue()).toBe(false);
    expect(component.provenanceOffreRecue()).toBeNull();
  });

  it('should clear provenance on manual motif change', () => {
    component.aiData = { motifLicenciementDetecte: 'LICENCIEMENT_AUTRE' } as any;
    initWithNoExistingResult();
    expect(component.provenanceMotif()).toBe('IA');
    component.onMotifChange('FAUTE_GRAVE');
    expect(component.provenanceMotif()).toBeNull();
    expect(component.motifLicenciement()).toBe('FAUTE_GRAVE');
  });

  // ── Visibilité conditionnelle ─────────────────────────────────────────

  it('toggling offreOutplacementRecue off should reset the 3 conditional fields', () => {
    initWithNoExistingResult();
    component.onOffreRecueChange(true);
    component.dateOffreOutplacement.set('2025-03-20');
    component.offreConformeCCT82.set(true);
    component.salarieAcceptantOffre.set(false);
    component.onOffreRecueChange(false);
    expect(component.offreOutplacementRecue()).toBe(false);
    expect(component.dateOffreOutplacement()).toBeNull();
    expect(component.offreConformeCCT82()).toBeNull();
    expect(component.salarieAcceptantOffre()).toBeNull();
  });

  // ── formValid ─────────────────────────────────────────────────────────

  it('formValid requires the 6 base fields', () => {
    initWithNoExistingResult();
    expect(component.formValid()).toBe(false);
    component.dateLicenciement.set('2025-03-15');
    expect(component.formValid()).toBe(false);
    component.dateNaissanceSalarie.set('1975-06-12');
    expect(component.formValid()).toBe(false);
    component.ancienneteAnnees.set(12);
    expect(component.formValid()).toBe(false);
    component.motifLicenciement.set('LICENCIEMENT_AUTRE');
    // offreOutplacementRecue=false par défaut → suffisant
    expect(component.formValid()).toBe(true);
  });

  it('formValid rejects negative anciennete', () => {
    initWithNoExistingResult();
    component.dateLicenciement.set('2025-03-15');
    component.dateNaissanceSalarie.set('1975-06-12');
    component.motifLicenciement.set('LICENCIEMENT_AUTRE');
    component.ancienneteAnnees.set(-0.1);
    expect(component.formValid()).toBe(false);
    component.ancienneteAnnees.set(0);
    expect(component.formValid()).toBe(true);
  });

  it('formValid requires dateOffre + conforme when offre received', () => {
    initWithNoExistingResult();
    component.dateLicenciement.set('2025-03-15');
    component.dateNaissanceSalarie.set('1975-06-12');
    component.ancienneteAnnees.set(12);
    component.motifLicenciement.set('LICENCIEMENT_AUTRE');
    component.onOffreRecueChange(true);
    expect(component.formValid()).toBe(false);
    component.dateOffreOutplacement.set('2025-03-20');
    expect(component.formValid()).toBe(false);
    component.offreConformeCCT82.set(true);
    expect(component.formValid()).toBe(true);
    // salarieAcceptantOffre reste tri-état nullable → pas requis.
  });

  // ── POST calculate ────────────────────────────────────────────────────

  it('should NOT POST when required fields missing', () => {
    initWithNoExistingResult();
    component.calculate();
    httpMock.expectNone(API_URL);
  });

  it('should POST with conditional fields nulled when offre not received', () => {
    initWithNoExistingResult();
    component.dateLicenciement.set('2025-03-15');
    component.dateNaissanceSalarie.set('1985-06-12');
    component.ancienneteAnnees.set(5);
    component.motifLicenciement.set('LICENCIEMENT_AUTRE');
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateLicenciement).toBe('2025-03-15');
    expect(req.request.body.ancienneteAnnees).toBe(5);
    expect(req.request.body.motifLicenciement).toBe('LICENCIEMENT_AUTRE');
    expect(req.request.body.contratTempsPlein).toBe(true);
    expect(req.request.body.offreOutplacementRecue).toBe(false);
    expect(req.request.body.dateOffreOutplacement).toBeNull();
    expect(req.request.body.offreConformeCCT82).toBeNull();
    expect(req.request.body.salarieAcceptantOffre).toBeNull();

    req.flush(OUTPLACEMENT_NON_DU_RESPONSE);
    expect(component.result()?.verdict).toBe('OUTPLACEMENT_NON_DU');
    expect(component.showForm()).toBe(false);
  });

  it('should POST with conditional fields populated when offre received', () => {
    initWithNoExistingResult();
    component.dateLicenciement.set('2025-03-15');
    component.dateNaissanceSalarie.set('1975-06-12');
    component.ancienneteAnnees.set(12);
    component.motifLicenciement.set('LICENCIEMENT_AUTRE');
    component.onOffreRecueChange(true);
    component.dateOffreOutplacement.set('2025-03-20');
    component.offreConformeCCT82.set(true);
    component.salarieAcceptantOffre.set(false);
    component.calculate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.body.offreOutplacementRecue).toBe(true);
    expect(req.request.body.dateOffreOutplacement).toBe('2025-03-20');
    expect(req.request.body.offreConformeCCT82).toBe(true);
    expect(req.request.body.salarieAcceptantOffre).toBe(false);

    req.flush(SANCTION_SALARIE_RESPONSE);
    expect(component.result()?.verdict).toBe('OFFRE_REFUSEE_SANCTION_SALARIE');
  });

  // ── Verdict color classes ────────────────────────────────────────────

  it('verdictClass maps the 5 verdicts to the 3 colors', () => {
    initWithNoExistingResult();
    expect(component.verdictClass('OUTPLACEMENT_NON_DU')).toBe('verdict-ok');
    expect(component.verdictClass('OFFRE_CONFORME_RESPECTEE')).toBe('verdict-ok');
    expect(component.verdictClass('OFFRE_NON_FAITE_SANCTION_EMPLOYEUR')).toBe('verdict-critical');
    expect(component.verdictClass('OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR')).toBe('verdict-critical');
    expect(component.verdictClass('OFFRE_REFUSEE_SANCTION_SALARIE')).toBe('verdict-warn');
  });

  it('isSanctionEmployeur / isSanctionSalarie discriminate the rouge / ambre verdicts', () => {
    initWithNoExistingResult();
    expect(component.isSanctionEmployeur('OFFRE_NON_FAITE_SANCTION_EMPLOYEUR')).toBe(true);
    expect(component.isSanctionEmployeur('OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR')).toBe(true);
    expect(component.isSanctionEmployeur('OFFRE_REFUSEE_SANCTION_SALARIE')).toBe(false);
    expect(component.isSanctionSalarie('OFFRE_REFUSEE_SANCTION_SALARIE')).toBe(true);
    expect(component.isSanctionSalarie('OFFRE_NON_FAITE_SANCTION_EMPLOYEUR')).toBe(false);
  });

  // ── Affichage verdict + sanctions ─────────────────────────────────────

  it('should display OUTPLACEMENT_NON_DU with raison humanisée', () => {
    initWithExistingResult(OUTPLACEMENT_NON_DU_RESPONSE);
    expect(component.result()?.verdict).toBe('OUTPLACEMENT_NON_DU');
    expect(component.result()?.raisonNonObligation).toBe('AGE_INFERIEUR_45');
    expect(component.humanizeRaisonNonObligation('AGE_INFERIEUR_45')).toBe('âge inférieur à 45 ans');
    expect(component.humanizeRaisonNonObligation('ANCIENNETE_INFERIEURE_1AN')).toBe('ancienneté inférieure à 1 an');
    expect(component.humanizeRaisonNonObligation('FAUTE_GRAVE')).toBe('faute grave (exemption employeur)');
    expect(component.humanizeRaisonNonObligation('DEMISSION')).toBe('démission');
  });

  it('should display SANCTION_EMPLOYEUR with euros + dateLimiteOffre', () => {
    initWithExistingResult(SANCTION_EMPLOYEUR_RESPONSE);
    expect(component.result()?.verdict).toBe('OFFRE_NON_FAITE_SANCTION_EMPLOYEUR');
    expect(component.result()?.sanctionEmployeurEuros).toBe(1800);
    expect(component.result()?.sanctionSalarieRange).toBeNull();
    expect(component.result()?.dateLimiteOffre).toBe('2025-03-30');
    expect(component.result()?.etapeSuivante).toBe('PLAINTE_INSPECTION_LOIS_SOCIALES');
  });

  it('should display SANCTION_SALARIE with semaines range + delaiOffreRespecte ✓', () => {
    initWithExistingResult(SANCTION_SALARIE_RESPONSE);
    expect(component.result()?.verdict).toBe('OFFRE_REFUSEE_SANCTION_SALARIE');
    expect(component.result()?.sanctionSalarieRange).toEqual({ minSemaines: 4, maxSemaines: 52 });
    expect(component.result()?.sanctionEmployeurEuros).toBe(0);
    expect(component.result()?.delaiOffreRespecte).toBe(true);
  });

  it('should trigger dashboard refresh on successful POST', () => {
    initWithNoExistingResult();
    component.dateLicenciement.set('2025-03-15');
    component.dateNaissanceSalarie.set('1985-06-12');
    component.ancienneteAnnees.set(5);
    component.motifLicenciement.set('LICENCIEMENT_AUTRE');
    component.calculate();
    httpMock.expectOne(API_URL).flush(OUTPLACEMENT_NON_DU_RESPONSE);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  // ── Erreurs ──────────────────────────────────────────────────────────

  it('should show snackbar on 500', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.dateLicenciement.set('2025-03-15');
    component.dateNaissanceSalarie.set('1985-06-12');
    component.ancienneteAnnees.set(5);
    component.motifLicenciement.set('LICENCIEMENT_AUTRE');
    component.calculate();
    httpMock.expectOne(API_URL).flush('boom', { status: 500, statusText: 'Server Error' });
    expect(spy).toHaveBeenCalled();
    expect(component.calculating()).toBe(false);
  });

  it('should show snackbar with "Dossier introuvable" on 404 POST', () => {
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    initWithNoExistingResult();
    component.dateLicenciement.set('2025-03-15');
    component.dateNaissanceSalarie.set('1985-06-12');
    component.ancienneteAnnees.set(5);
    component.motifLicenciement.set('LICENCIEMENT_AUTRE');
    component.calculate();
    httpMock.expectOne(API_URL).flush('not found', { status: 404, statusText: 'Not Found' });
    expect(spy).toHaveBeenCalledWith('Dossier introuvable', 'Fermer', expect.any(Object));
  });

  it('should refuse calculate when workspaceCountry FR (defense in depth)', () => {
    initWithNoExistingResult();
    component.dateLicenciement.set('2025-03-15');
    component.dateNaissanceSalarie.set('1985-06-12');
    component.ancienneteAnnees.set(5);
    component.motifLicenciement.set('LICENCIEMENT_AUTRE');
    component.workspaceCountry = 'FRANCE';
    const snack = TestBed.inject(MatSnackBar);
    const spy = jest.spyOn(snack, 'open');
    component.calculate();
    httpMock.expectNone(API_URL);
    expect(spy).toHaveBeenCalled();
  });

  // ── formatEuro ────────────────────────────────────────────────────────

  it('formatEuro renders fr-BE currency', () => {
    initWithNoExistingResult();
    const out = component.formatEuro(1800);
    expect(out).toContain('€');
    expect(out).toContain(',');
  });

  it('formatEuro handles null / NaN gracefully', () => {
    initWithNoExistingResult();
    expect(component.formatEuro(null)).toBe('—');
    expect(component.formatEuro(undefined)).toBe('—');
    expect(component.formatEuro(NaN)).toBe('—');
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
describe('OutplacementBeObligatoire45SectionComponent.getPrefillCount', () => {
  it('returns 0 when no aiData', () => {
    expect(OutplacementBeObligatoire45SectionComponent.getPrefillCount({})).toBe(0);
  });

  it('returns 1 when only dateLicenciement is set', () => {
    expect(
      OutplacementBeObligatoire45SectionComponent.getPrefillCount({
        aiData: { dateLicenciement: '2025-03-15' },
      }),
    ).toBe(1);
  });

  it('returns 5 nominal — tous les champs instrumentés', () => {
    expect(
      OutplacementBeObligatoire45SectionComponent.getPrefillCount({
        aiData: {
          dateLicenciement: '2025-03-15',
          dateNaissanceSalarie: '1975-06-12',
          ancienneteSalarie: 12.5,
          motifLicenciementDetecte: 'LICENCIEMENT_AUTRE',
          offreOutplacementMentionnee: true,
        },
      }),
    ).toBe(5);
  });
});
