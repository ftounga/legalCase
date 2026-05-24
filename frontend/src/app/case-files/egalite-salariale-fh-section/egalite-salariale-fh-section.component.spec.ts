import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EgaliteSalarialeFhSectionComponent } from './egalite-salariale-fh-section.component';
import {
  EgsAnalyse,
  EgaliteSalarialeFhResponse,
} from '../../core/models/egalite-salariale-fh.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('EgaliteSalarialeFhSectionComponent', () => {
  let component: EgaliteSalarialeFhSectionComponent;
  let fixture: ComponentFixture<EgaliteSalarialeFhSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/egalite-salariale-fh';

  /** Fixture IA complète — les 4 champs du sous-objet `egaliteSalarialeDetail`. */
  const FULL_AI_DATA: TravailExtractedData = {
    egaliteSalarialeDetail: {
      sexeSalarie: 'FEMME',
      salaireBrut: 2500,
      anciennete: 36,
      ecartPourcentage: 22.5,
    },
  };

  function response(
    analyse: EgsAnalyse,
  ): EgaliteSalarialeFhResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs.
      sexeSalarie: 'FEMME',
      salaireMensuelBrutSalarieEuros: 2500.0,
      ancienneteMois: 36,
      qualification: 'Chef de projet',
      nombreComparantsMieuxPayes: analyse === 'DISCRIMINATION_PROBABLE' ? 3 : 1,
      ecartSalaireMoyenComparantsEuros: analyse === 'DISCRIMINATION_PROBABLE' ? 600 : 100,
      ecartPourcentage: analyse === 'DISCRIMINATION_PROBABLE' ? 22.5
                       : analyse === 'DISCRIMINATION_POSSIBLE' ? 10
                       : 2,
      indexEgaliteConnu: false,
      scoreIndexEgalite: null,
      justificationsEmployeurObjectives: analyse === 'PAS_DE_DISCRIMINATION_APPARENTE',
      // Sorties calculées.
      analyseEgaliteSalariale: analyse,
      scoreDiscrimination: analyse === 'DISCRIMINATION_PROBABLE' ? 80
                         : analyse === 'DISCRIMINATION_POSSIBLE' ? 45
                         : 10,
      facteursDisparite: [
        {
          code: 'DT56_ECART_SALAIRE',
          libelle: 'Écart salarial constaté',
          fondement: 'L. 3221-2 CT',
          poids: 40,
          explication: 'Écart constaté significatif.',
        },
      ],
      prescriptionActionAns: 5,
      alerteNonPlafonnement: 'Dommages-intérêts non plafonnés — hors barème Macron (L. 1235-3 CT).',
      basesJuridiques: ['L. 1142-7 CT', 'L. 1144-1 CT', 'Loi 05/09/2018'],
      messages: ['Analyse calculée.', 'Dommages-intérêts non plafonnés — hors barème Macron (L. 1235-3 CT).'],
      country: 'FRANCE',
      calculatedAt: '2026-05-24T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        EgaliteSalarialeFhSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EgaliteSalarialeFhSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // F-JU-03 — absorbe les requêtes /jurisprudence-citations émises par
    // ToolJurisprudenceCitationsComponent (imports[] du composant).
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

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
    const banner = fixture.nativeElement.querySelector('[data-testid="egs-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 4 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.sexeSalarie()).toBe('FEMME');
    expect(component.salaireMensuelBrutSalarieEuros()).toBe(2500);
    expect(component.ancienneteMois()).toBe(36);
    expect(component.ecartPourcentage()).toBe(22.5);
    expect(component.provenanceSexe()).toBe('IA');
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.provenanceAnciennete()).toBe('IA');
    expect(component.provenanceEcartPct()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    expect(component.provenanceSexe()).toBeNull();
  });

  it('getPrefillCount = 4 sur fixture IA FR complète', () => {
    expect(EgaliteSalarialeFhSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(EgaliteSalarialeFhSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(EgaliteSalarialeFhSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux
  // ---------------------------------------------------------------------------

  it('POST → verdict DISCRIMINATION_PROBABLE + refresh dashboard', () => {
    component.sexeSalarie.set('FEMME');
    component.salaireMensuelBrutSalarieEuros.set(2500);
    component.nombreComparantsMieuxPayes.set(3);
    component.ecartPourcentage.set(25);
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.sexeSalarie).toBe('FEMME');
    req.flush(response('DISCRIMINATION_PROBABLE'));

    expect(component.result()?.analyseEgaliteSalariale).toBe('DISCRIMINATION_PROBABLE');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict DISCRIMINATION_POSSIBLE', () => {
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('DISCRIMINATION_POSSIBLE'));
    expect(component.result()?.analyseEgaliteSalariale).toBe('DISCRIMINATION_POSSIBLE');
  });

  it('POST → verdict PAS_DE_DISCRIMINATION_APPARENTE', () => {
    component.justificationsEmployeurObjectives.set(true);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('PAS_DE_DISCRIMINATION_APPARENTE'));
    expect(component.result()?.analyseEgaliteSalariale).toBe('PAS_DE_DISCRIMINATION_APPARENTE');
  });

  it('Verdict banner affiché après calcul', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('DISCRIMINATION_PROBABLE'));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('[data-testid="egs-verdict-banner"]');
    expect(banner).toBeTruthy();
  });

  // ── INVARIANT produit : alerte non-plafonnement TOUJOURS visible ──────────

  it('Alerte non-plafonnement affichée après calcul (invariant produit)', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('DISCRIMINATION_PROBABLE'));
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('[data-testid="egs-alert-non-plafonnement"]');
    expect(alert).toBeTruthy();
    expect(alert.textContent).toContain('non plafonné');
  });

  it('Alerte non-plafonnement visible aussi pour PAS_DE_DISCRIMINATION_APPARENTE', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.justificationsEmployeurObjectives.set(true);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('PAS_DE_DISCRIMINATION_APPARENTE'));
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('[data-testid="egs-alert-non-plafonnement"]');
    expect(alert).toBeTruthy();
  });

  it('Prescription 5 ans affichée', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('DISCRIMINATION_PROBABLE'));
    fixture.detectChanges();
    const figure = fixture.nativeElement.querySelector('[data-testid="egs-figure-prescription"]');
    expect(figure).toBeTruthy();
    expect(figure.textContent).toContain('5');
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

  it('formValid = false si écart > 100', () => {
    component.ecartPourcentage.set(150);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si score index hors bornes', () => {
    component.scoreIndexEgalite.set(150);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('DISCRIMINATION_PROBABLE')).toContain('--probable');
    expect(component.verdictBannerClass('DISCRIMINATION_POSSIBLE')).toContain('--possible');
    expect(component.verdictBannerClass('PAS_DE_DISCRIMINATION_APPARENTE')).toContain('--apparent');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('DISCRIMINATION_PROBABLE')).toContain('probable');
    expect(component.verdictBannerLabel('DISCRIMINATION_POSSIBLE')).toContain('possible');
    expect(component.verdictBannerLabel('PAS_DE_DISCRIMINATION_APPARENTE')).toContain('apparente');
  });

  it('Handler sexe efface la provenance IA', () => {
    component.provenanceSexe.set('IA');
    component.onSexeChange('HOMME');
    expect(component.sexeSalarie()).toBe('HOMME');
    expect(component.provenanceSexe()).toBeNull();
  });

  it('Handler salaire accepte un nombre', () => {
    component.onSalaireChange(3000);
    expect(component.salaireMensuelBrutSalarieEuros()).toBe(3000);
  });

  it('Handler salaire rejette une valeur négative (ramène à 0)', () => {
    component.onSalaireChange(-100);
    expect(component.salaireMensuelBrutSalarieEuros()).toBe(0);
  });

  it('Handler ancienneté tronque en entier', () => {
    component.onAncienneteChange(36.7);
    expect(component.ancienneteMois()).toBe(36);
  });

  it('Handler écart % rejette une valeur > 100', () => {
    component.onEcartPctChange(150);
    expect(component.ecartPourcentage()).toBe(0);
  });

  it('onIndexConnuChange(false) réinitialise le score', () => {
    component.scoreIndexEgalite.set(80);
    component.onIndexConnuChange(false);
    expect(component.scoreIndexEgalite()).toBeNull();
  });

  it('onScoreIndexChange clamp dans [0, 100]', () => {
    component.onScoreIndexChange(150);
    expect(component.scoreIndexEgalite()).toBe(100);
    component.onScoreIndexChange(-10);
    expect(component.scoreIndexEgalite()).toBe(0);
  });
});
