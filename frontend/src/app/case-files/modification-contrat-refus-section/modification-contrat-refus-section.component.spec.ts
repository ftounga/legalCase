import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ModificationContratRefusSectionComponent } from './modification-contrat-refus-section.component';
import {
  ModificationContratAnalyse,
  ModificationContratRefusResponse,
} from '../../core/models/modification-contrat-refus.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('ModificationContratRefusSectionComponent', () => {
  let component: ModificationContratRefusSectionComponent;
  let fixture: ComponentFixture<ModificationContratRefusSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/modification-contrat-refus';

  /** Fixture IA complète — les 4 champs `modifContrat*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    modifContratElementModifie: 'REMUNERATION',
    modifContratContractualise: true,
    modifContratMotifEco: true,
    modifContratNotifEcrite: false,
  };

  function response(
    analyse: ModificationContratAnalyse,
    alerteDelaiReflexion = false,
  ): ModificationContratRefusResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs (ré-édition du formulaire après recharge).
      elementModifie: 'REMUNERATION',
      elementExplicitementContractualise: true,
      motifEconomique: analyse === 'MODIFICATION_CONTRAT',
      notificationEcriteL1222_6: !alerteDelaiReflexion,
      delaiReflexionRespecteMois: 1,
      reponseSalarie: 'REFUS',
      // Sorties calculées.
      analyseModification: analyse,
      scoreModificationContrat: analyse === 'MODIFICATION_CONTRAT' ? 90
                                : analyse === 'CHANGEMENT_CONDITIONS_TRAVAIL' ? 20
                                : 40,
      consequences: [
        {
          type: 'LICENCIEMENT_ECONOMIQUE_L1233_3',
          description: 'Refus + motif éco → licenciement économique.',
          fondement: 'L. 1222-6 CT ; L. 1233-3 CT',
        },
      ],
      alerteDelaiReflexion,
      basesJuridiques: ['Cass. soc. — distinction', 'L. 1222-6 CT'],
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
        ModificationContratRefusSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ModificationContratRefusSectionComponent);
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
    const banner = fixture.nativeElement.querySelector('[data-testid="mcr-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 4 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.elementModifie()).toBe('REMUNERATION');
    expect(component.elementExplicitementContractualise()).toBe(true);
    expect(component.motifEconomique()).toBe(true);
    expect(component.notificationEcriteL1222_6()).toBe(false);
    expect(component.provenanceElementModifie()).toBe('IA');
    expect(component.provenanceNotifEcrite()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    expect(component.provenanceElementModifie()).toBeNull();
  });

  it('getPrefillCount = 4 sur fixture IA FR complète', () => {
    expect(ModificationContratRefusSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(ModificationContratRefusSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(ModificationContratRefusSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux + alerte délai
  // ---------------------------------------------------------------------------

  it('POST → verdict MODIFICATION_CONTRAT + refresh dashboard', () => {
    component.elementModifie.set('REMUNERATION');
    component.elementExplicitementContractualise.set(true);
    component.reponseSalarie.set('REFUS');
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.elementModifie).toBe('REMUNERATION');
    req.flush(response('MODIFICATION_CONTRAT'));

    expect(component.result()?.analyseModification).toBe('MODIFICATION_CONTRAT');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict CHANGEMENT_CONDITIONS_TRAVAIL', () => {
    component.elementModifie.set('HORAIRES');
    component.elementExplicitementContractualise.set(false);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CHANGEMENT_CONDITIONS_TRAVAIL'));
    expect(component.result()?.analyseModification).toBe('CHANGEMENT_CONDITIONS_TRAVAIL');
  });

  it('POST → verdict INCERTAIN', () => {
    component.elementModifie.set('AUTRE');
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('INCERTAIN'));
    expect(component.result()?.analyseModification).toBe('INCERTAIN');
  });

  it('Alerte délai L. 1222-6 visible si alerteDelaiReflexion=true', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.elementModifie.set('REMUNERATION');
    component.elementExplicitementContractualise.set(true);
    component.motifEconomique.set(true);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('MODIFICATION_CONTRAT', true));
    fixture.detectChanges();

    expect(component.result()?.alerteDelaiReflexion).toBe(true);
    const alerte = fixture.nativeElement.querySelector('[data-testid="mcr-alerte-banner"]');
    expect(alerte).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true avec valeurs par défaut', () => {
    expect(component.formValid()).toBe(true);
  });

  it('formValid = false si delai négatif', () => {
    component.delaiReflexionRespecteMois.set(-1);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('MODIFICATION_CONTRAT')).toContain('--modification');
    expect(component.verdictBannerClass('CHANGEMENT_CONDITIONS_TRAVAIL')).toContain('--changement');
    expect(component.verdictBannerClass('INCERTAIN')).toContain('--incertain');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('MODIFICATION_CONTRAT')).toContain('Modification du contrat');
    expect(component.verdictBannerLabel('CHANGEMENT_CONDITIONS_TRAVAIL')).toContain('Changement des conditions');
    expect(component.verdictBannerLabel('INCERTAIN')).toContain('incertaine');
  });

  it('Handler élément modifié efface la provenance IA', () => {
    component.provenanceElementModifie.set('IA');
    component.onElementChange('HORAIRES');
    expect(component.elementModifie()).toBe('HORAIRES');
    expect(component.provenanceElementModifie()).toBeNull();
  });

  it('Handler delai accepte une valeur entière', () => {
    component.onDelaiChange(2);
    expect(component.delaiReflexionRespecteMois()).toBe(2);
  });

  it('Handler delai rejette les valeurs négatives (ramène à null)', () => {
    component.onDelaiChange(-1);
    expect(component.delaiReflexionRespecteMois()).toBeNull();
  });

  it('Handler delai accepte null (vide le champ)', () => {
    component.delaiReflexionRespecteMois.set(2);
    component.onDelaiChange(null);
    expect(component.delaiReflexionRespecteMois()).toBeNull();
  });
});
