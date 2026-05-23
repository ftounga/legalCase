import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CspCrpFrSectionComponent } from './csp-crp-fr-section.component';
import {
  CspCrpConformiteCsp,
  CspCrpConformiteResponse,
} from '../../core/models/csp-crp-fr.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('CspCrpFrSectionComponent', () => {
  let component: CspCrpFrSectionComponent;
  let fixture: ComponentFixture<CspCrpFrSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/csp-crp-conformite';

  /** Fixture IA complète — les 6 champs `csp*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    cspEffectifEntreprise: 250,
    cspProposeDetail: true,
    cspDocumentRemis: true,
    cspDateRemise: '2026-04-01',
    cspAdhesion: true,
    cspSalaireMensuelBrut: 3000.0,
  };

  function response(
    conformite: CspCrpConformiteCsp,
    obligationApplicable = true,
  ): CspCrpConformiteResponse {
    return {
      caseFileId: 'case-1',
      effectifEntreprise: obligationApplicable ? 250 : 1500,
      cspPropose: conformite !== 'NON_CONFORME',
      documentInformationRemis: conformite === 'CONFORME',
      delaiReflexionMentionne: conformite === 'CONFORME',
      dateRemise: '2026-04-01',
      dateEntretienPrealable: '2026-04-01',
      adhesionSalarie: true,
      salaireMensuelBrutEuros: 3000,
      remunerationBrute12MoisEuros: 36000,
      obligationCspApplicable: obligationApplicable,
      conformiteCsp: conformite,
      scoreConformite: conformite === 'CONFORME' ? 100
                    : conformite === 'PARTIELLEMENT_CONFORME' ? 60
                    : 20,
      pointsNonConformite: conformite === 'CONFORME' ? [] : [
        {
          code: 'DT44_OBLIGATION_CSP',
          libelle: 'Obligation non satisfaite',
          fondement: 'L. 1233-66 CT',
          poids: 50,
          explication: 'Test point.',
        },
      ],
      aspEstimeeJournaliereEuros: obligationApplicable ? 73.97 : null,
      aspEstimeeAnnuelleEuros: obligationApplicable ? 27_000 : null,
      dureeAspMois: obligationApplicable ? 12 : 0,
      basesJuridiques: ['L. 1233-65 CT', 'ANI CSP 19/07/2011'],
      messages: ['Proposition CSP analysée.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-24T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        CspCrpFrSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CspCrpFrSectionComponent);
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
    req.flush(response('CONFORME'));

    expect(component.result()?.conformiteCsp).toBe('CONFORME');
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

  it('pré-remplit 6 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.effectifEntreprise()).toBe(250);
    expect(component.cspPropose()).toBe(true);
    expect(component.documentInformationRemis()).toBe(true);
    expect(component.dateRemise()).toBe('2026-04-01');
    expect(component.adhesionSalarie()).toBe('OUI');
    expect(component.salaireMensuelBrutEuros()).toBe(3000);
    expect(component.provenanceEffectif()).toBe('IA');
    expect(component.provenanceCspPropose()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    expect(component.effectifEntreprise()).toBeNull();
    expect(component.cspPropose()).toBe(false);
    expect(component.provenanceEffectif()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // getPrefillCount (parité stricte avec prefillFromAi)
  // ---------------------------------------------------------------------------

  it('getPrefillCount = 6 sur fixture IA FR complète', () => {
    expect(CspCrpFrSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(6);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(CspCrpFrSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(CspCrpFrSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Effectif hors champ
  // ---------------------------------------------------------------------------

  it('effectifHorsChamp = true si ≥ 1 000', () => {
    component.effectifEntreprise.set(1500);
    expect(component.effectifHorsChamp()).toBe(true);
  });

  it('effectifHorsChamp = false si < 1 000', () => {
    component.effectifEntreprise.set(500);
    expect(component.effectifHorsChamp()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux + encadré ASP
  // ---------------------------------------------------------------------------

  it('POST avec proposition conforme → verdict CONFORME + encadré ASP', () => {
    component.effectifEntreprise.set(250);
    component.cspPropose.set(true);
    component.documentInformationRemis.set(true);
    component.delaiReflexionMentionne.set(true);
    component.salaireMensuelBrutEuros.set(3000);
    component.remunerationBrute12MoisEuros.set(36000);
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.cspPropose).toBe(true);
    req.flush(response('CONFORME'));

    expect(component.result()?.conformiteCsp).toBe('CONFORME');
    expect(component.result()?.aspEstimeeAnnuelleEuros).toBeGreaterThan(0);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict NON_CONFORME', () => {
    component.effectifEntreprise.set(250);
    component.salaireMensuelBrutEuros.set(3000);
    component.remunerationBrute12MoisEuros.set(36000);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('NON_CONFORME'));

    expect(component.result()?.conformiteCsp).toBe('NON_CONFORME');
    expect(component.result()?.pointsNonConformite.length).toBeGreaterThan(0);
  });

  it('POST → verdict PARTIELLEMENT_CONFORME', () => {
    component.effectifEntreprise.set(250);
    component.salaireMensuelBrutEuros.set(3000);
    component.remunerationBrute12MoisEuros.set(36000);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('PARTIELLEMENT_CONFORME'));

    expect(component.result()?.conformiteCsp).toBe('PARTIELLEMENT_CONFORME');
  });

  it('POST → obligationCspApplicable=false si effectif >= 1 000', () => {
    component.effectifEntreprise.set(1500);
    component.salaireMensuelBrutEuros.set(3000);
    component.remunerationBrute12MoisEuros.set(36000);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('NON_CONFORME', false));

    expect(component.result()?.obligationCspApplicable).toBe(false);
    expect(component.result()?.aspEstimeeAnnuelleEuros).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si salaire ≤ 0', () => {
    component.effectifEntreprise.set(250);
    component.salaireMensuelBrutEuros.set(0);
    component.remunerationBrute12MoisEuros.set(36000);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true avec valeurs valides', () => {
    component.effectifEntreprise.set(250);
    component.salaireMensuelBrutEuros.set(3000);
    component.remunerationBrute12MoisEuros.set(36000);
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Verdict UI helpers
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('CONFORME')).toContain('csp-verdict-banner--conforme');
    expect(component.verdictBannerClass('PARTIELLEMENT_CONFORME')).toContain('csp-verdict-banner--partielle');
    expect(component.verdictBannerClass('NON_CONFORME')).toContain('csp-verdict-banner--non-conforme');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('CONFORME')).toBe('Proposition CSP conforme');
    expect(component.verdictBannerLabel('PARTIELLEMENT_CONFORME')).toBe('Proposition CSP partiellement conforme');
    expect(component.verdictBannerLabel('NON_CONFORME')).toBe('Proposition CSP non conforme');
  });

  it('formatEuros formate selon la locale FR', () => {
    expect(component.formatEuros(27_000)).toContain('27');
    expect(component.formatEuros(null)).toBe('—');
  });
});
