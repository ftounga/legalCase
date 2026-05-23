import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ForfaitJoursFrSectionComponent } from './forfait-jours-fr-section.component';
import {
  ForfaitJoursValiditeForfait,
  ForfaitJoursValiditeResponse,
} from '../../core/models/forfait-jours-fr.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('ForfaitJoursFrSectionComponent', () => {
  let component: ForfaitJoursFrSectionComponent;
  let fixture: ComponentFixture<ForfaitJoursFrSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/forfait-jours-validite';

  /** Fixture IA complète — les 5 champs `forfaitJours*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    forfaitJoursAccordCollectifExiste: true,
    forfaitJoursEntretienAnnuelRealise: false,
    forfaitJoursDocumentControle: false,
    forfaitJoursCategorieAutonome: true,
    forfaitJoursNbJours: 220,
  };

  function response(
    validite: ForfaitJoursValiditeForfait,
  ): ForfaitJoursValiditeResponse {
    return {
      caseFileId: 'case-1',
      accordCollectifExiste: validite !== 'NULLE',
      accordGarantitSuiviCharge: validite === 'VALIDE',
      entretienAnnuelRealise: validite === 'VALIDE',
      documentControleMensuelExiste: validite === 'VALIDE',
      categorieAutonomeConfirmee: true,
      nbJoursForfait: 218,
      ancienneteMois: 36,
      salaireMensuelBrutEuros: 4500,
      hsEstimeesParSemaine: 10,
      nbSemainesParAn: 45,
      validiteForfait: validite,
      scoreValidite: validite === 'VALIDE' ? 100
                  : validite === 'PARTIELLEMENT_NULLE' ? 60
                  : 20,
      facteursInvalidite: validite === 'VALIDE' ? [] : [
        {
          code: 'DT50_ACCORD_COLLECTIF',
          libelle: 'Absence accord',
          fondement: 'L. 3121-63 CT',
          poids: 50,
          explication: 'Test facteur.',
        },
      ],
      rappelHsEstimeEuros: validite === 'VALIDE' ? null : 52070.5,
      prescriptionRappelAns: 3,
      basesJuridiques: ['L. 3121-58 CT', 'L. 3245-1 CT'],
      messages: ['Forfait jours analysé.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-23T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        ForfaitJoursFrSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ForfaitJoursFrSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Chargement (GET) et gate pays
  // ---------------------------------------------------------------------------

  it('charge le snapshot existant au démarrage (FRANCE) puis affiche le verdict', () => {
    fixture.detectChanges(); // ngOnInit
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response('VALIDE'));

    expect(component.result()?.validiteForfait).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
  });

  it('ne fait pas de GET au démarrage si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-remplit 5 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.accordCollectifExiste()).toBe(true);
    expect(component.entretienAnnuelRealise()).toBe(false);
    expect(component.documentControleMensuelExiste()).toBe(false);
    expect(component.categorieAutonomeConfirmee()).toBe(true);
    expect(component.nbJoursForfait()).toBe(220);
    expect(component.provenanceAccordCollectif()).toBe('IA');
    expect(component.provenanceNbJours()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    // Valeurs par défaut conservées (toggles false, nbJours 218).
    expect(component.accordCollectifExiste()).toBe(false);
    expect(component.nbJoursForfait()).toBe(218);
    expect(component.provenanceAccordCollectif()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // getPrefillCount (parité stricte avec prefillFromAi)
  // ---------------------------------------------------------------------------

  it('getPrefillCount = 5 sur fixture IA FR complète', () => {
    expect(ForfaitJoursFrSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(5);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(ForfaitJoursFrSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(ForfaitJoursFrSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  it('getPrefillCount ignore les nb jours hors plage [0, 235]', () => {
    expect(ForfaitJoursFrSectionComponent.getPrefillCount({
      aiData: { ...FULL_AI_DATA, forfaitJoursNbJours: 400 },
      workspaceCountry: 'FRANCE',
    })).toBe(4);  // 4 booléens, nb jours rejeté
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux + tableau rappel HS
  // ---------------------------------------------------------------------------

  it('POST avec valeurs valides → verdict VALIDE, pas de rappel HS', () => {
    component.ancienneteMois.set(36);
    component.salaireMensuelBrutEuros.set(4500);
    component.accordCollectifExiste.set(true);
    component.accordGarantitSuiviCharge.set(true);
    component.entretienAnnuelRealise.set(true);
    component.documentControleMensuelExiste.set(true);
    component.categorieAutonomeConfirmee.set(true);
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.accordCollectifExiste).toBe(true);
    req.flush(response('VALIDE'));

    expect(component.result()?.validiteForfait).toBe('VALIDE');
    expect(component.result()?.rappelHsEstimeEuros).toBeNull();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict NULLE → rappel HS retourné', () => {
    component.ancienneteMois.set(36);
    component.salaireMensuelBrutEuros.set(4500);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('NULLE'));

    expect(component.result()?.validiteForfait).toBe('NULLE');
    expect(component.result()?.rappelHsEstimeEuros).toBeCloseTo(52070.5, 2);
  });

  it('POST → verdict PARTIELLEMENT_NULLE → rappel HS retourné', () => {
    component.ancienneteMois.set(36);
    component.salaireMensuelBrutEuros.set(4500);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('PARTIELLEMENT_NULLE'));

    expect(component.result()?.validiteForfait).toBe('PARTIELLEMENT_NULLE');
    expect(component.result()?.rappelHsEstimeEuros).not.toBeNull();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si salaire ≤ 0', () => {
    component.ancienneteMois.set(12);
    component.salaireMensuelBrutEuros.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true avec valeurs valides', () => {
    component.ancienneteMois.set(12);
    component.salaireMensuelBrutEuros.set(4500);
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Verdict UI helpers
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('VALIDE')).toContain('fj-verdict-banner--valide');
    expect(component.verdictBannerClass('PARTIELLEMENT_NULLE')).toContain('fj-verdict-banner--partielle');
    expect(component.verdictBannerClass('NULLE')).toContain('fj-verdict-banner--nulle');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('VALIDE')).toBe('Forfait jours valide');
    expect(component.verdictBannerLabel('PARTIELLEMENT_NULLE')).toBe('Forfait jours partiellement nul');
    expect(component.verdictBannerLabel('NULLE')).toBe('Forfait jours nul');
  });

  it('formatEuros formate selon la locale FR', () => {
    expect(component.formatEuros(52070.5)).toContain('52');
    expect(component.formatEuros(null)).toBe('—');
  });
});
