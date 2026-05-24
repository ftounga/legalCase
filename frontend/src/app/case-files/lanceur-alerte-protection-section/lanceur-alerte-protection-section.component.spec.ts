import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LanceurAlerteProtectionSectionComponent } from './lanceur-alerte-protection-section.component';
import {
  LanceurAlerteAnalyse,
  LanceurAlerteProtectionResponse,
} from '../../core/models/lanceur-alerte-protection.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('LanceurAlerteProtectionSectionComponent', () => {
  let component: LanceurAlerteProtectionSectionComponent;
  let fixture: ComponentFixture<LanceurAlerteProtectionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/lanceur-alerte-protection';

  /** Fixture IA complète — les 4 champs `lanceurAlerte*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    lanceurAlerteNatureSignalement: 'VIOLATION_DROIT_UE',
    lanceurAlerteProcedure: 'EXTERNE',
    lanceurAlerteMesureRepresaille: true,
    lanceurAlerteNatureMesure: 'LICENCIEMENT',
  };

  function response(
    verdict: LanceurAlerteAnalyse,
    montantDi = 10_000,
    nulliteMesure = true,
  ): LanceurAlerteProtectionResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs (ré-édition du formulaire après recharge).
      natureSignalement: 'CRIME_DELIT',
      contrepartieFinanciere: false,
      procedureSignalement: 'INTERNE',
      referentInterneSaisi: true,
      mesureRepresailleDetectee: nulliteMesure,
      natureMesureRepresaille: nulliteMesure ? 'LICENCIEMENT' : 'AUCUNE',
      // Sorties calculées.
      analyseLanceurAlerte: verdict,
      scoreProtection: verdict === 'PROTECTION_FORTE' ? 90
                       : verdict === 'PROTECTION_PARTIELLE' ? 55
                       : 0,
      pointsAnalyse: [
        {
          code: 'DT61_NATURE_SIGNALEMENT',
          libelle: 'Crime ou délit',
          fondement: 'Loi Waserman 2022',
          conclusion: 'Test conclusion.',
        },
      ],
      nulliteMesureRepresaille: nulliteMesure,
      montantMinDommagesInterets: montantDi,
      basesJuridiques: ['L. 1132-3-3 CT', 'Loi Waserman 2022'],
      messages: ['Analyse calculée.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-24T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        LanceurAlerteProtectionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LanceurAlerteProtectionSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Chargement (GET) et gate pays
  // ---------------------------------------------------------------------------

  it('mount FRANCE → GET initial déclenché', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it('mount BELGIQUE → pas d\'appel HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('BELGIQUE → bannière info pays affichée', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="lap-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 4 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.natureSignalement()).toBe('VIOLATION_DROIT_UE');
    expect(component.procedureSignalement()).toBe('EXTERNE');
    expect(component.mesureRepresailleDetectee()).toBe(true);
    expect(component.natureMesureRepresaille()).toBe('LICENCIEMENT');
    expect(component.provenanceNatureSignalement()).toBe('IA');
    expect(component.provenanceProcedure()).toBe('IA');
    expect(component.provenanceMesureRepresaille()).toBe('IA');
    expect(component.provenanceNatureMesure()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    // Valeurs par défaut conservées.
    expect(component.natureSignalement()).toBe('CRIME_DELIT');
    expect(component.procedureSignalement()).toBe('INTERNE');
    expect(component.provenanceNatureSignalement()).toBeNull();
  });

  it('getPrefillCount = 4 sur fixture IA FR complète', () => {
    expect(LanceurAlerteProtectionSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(LanceurAlerteProtectionSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(LanceurAlerteProtectionSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux + montant DI Waserman
  // ---------------------------------------------------------------------------

  it('POST → verdict PROTECTION_FORTE + refresh dashboard', () => {
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.natureSignalement).toBe('CRIME_DELIT');
    req.flush(response('PROTECTION_FORTE'));

    expect(component.result()?.analyseLanceurAlerte).toBe('PROTECTION_FORTE');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict PROTECTION_PARTIELLE', () => {
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('PROTECTION_PARTIELLE'));
    expect(component.result()?.analyseLanceurAlerte).toBe('PROTECTION_PARTIELLE');
  });

  it('POST → verdict HORS_CHAMP + montant DI = 0', () => {
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('HORS_CHAMP', 0, false));
    expect(component.result()?.analyseLanceurAlerte).toBe('HORS_CHAMP');
    expect(component.result()?.montantMinDommagesInterets).toBe(0);
  });

  it('Montant DI 10 000 € affiché si PROTECTION_FORTE', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('PROTECTION_FORTE', 10_000));
    fixture.detectChanges();

    expect(component.result()?.montantMinDommagesInterets).toBe(10_000);
    const montantBlock = fixture.nativeElement.querySelector('[data-testid="lap-figure-montant-di"]');
    expect(montantBlock).toBeTruthy();
  });

  it('Chip nullité de la mesure affiché si protection forte + nullité', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('PROTECTION_FORTE', 10_000, true));
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('[data-testid="lap-nullite-chip"]');
    expect(chip).toBeTruthy();
    expect(chip.textContent).toContain('Nullité de plein droit');
  });

  // ---------------------------------------------------------------------------
  // formValid + cohérence inputs
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si mesure=true mais nature=AUCUNE', () => {
    component.mesureRepresailleDetectee.set(true);
    component.natureMesureRepresaille.set('AUCUNE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si mesure=false mais nature!=AUCUNE', () => {
    component.mesureRepresailleDetectee.set(false);
    component.natureMesureRepresaille.set('LICENCIEMENT');
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true sur baseline cohérent', () => {
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Handlers
  // ---------------------------------------------------------------------------

  it('onMesureRepresailleChange(true) bascule nature à LICENCIEMENT', () => {
    component.onMesureRepresailleChange(true);
    expect(component.mesureRepresailleDetectee()).toBe(true);
    expect(component.natureMesureRepresaille()).toBe('LICENCIEMENT');
  });

  it('onMesureRepresailleChange(false) bascule nature à AUCUNE', () => {
    component.mesureRepresailleDetectee.set(true);
    component.natureMesureRepresaille.set('SANCTION');
    component.onMesureRepresailleChange(false);
    expect(component.mesureRepresailleDetectee()).toBe(false);
    expect(component.natureMesureRepresaille()).toBe('AUCUNE');
  });

  it('onProcedureChange autre que INTERNE → referent reset', () => {
    component.referentInterneSaisi.set(true);
    component.onProcedureChange('EXTERNE');
    expect(component.referentInterneSaisi()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('PROTECTION_FORTE')).toContain('--forte');
    expect(component.verdictBannerClass('PROTECTION_PARTIELLE')).toContain('--partielle');
    expect(component.verdictBannerClass('HORS_CHAMP')).toContain('--hors-champ');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('PROTECTION_FORTE')).toBe('Protection forte');
    expect(component.verdictBannerLabel('PROTECTION_PARTIELLE')).toBe('Protection partielle');
    expect(component.verdictBannerLabel('HORS_CHAMP')).toBe('Hors champ');
  });

  it('formatEuros formate selon la locale FR', () => {
    expect(component.formatEuros(10000)).toContain('10');
    expect(component.formatEuros(null)).toBe('—');
  });
});
