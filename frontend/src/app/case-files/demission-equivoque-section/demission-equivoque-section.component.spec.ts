import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DemissionEquivoqueSectionComponent } from './demission-equivoque-section.component';
import {
  DemAnalyse,
  DemissionEquivoqueResponse,
} from '../../core/models/demission-equivoque.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('DemissionEquivoqueSectionComponent', () => {
  let component: DemissionEquivoqueSectionComponent;
  let fixture: ComponentFixture<DemissionEquivoqueSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/demission-validite-equivoque';

  function response(
    analyse: DemAnalyse,
  ): DemissionEquivoqueResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs.
      modeExpressionDemission: 'ECRIT_FORMEL',
      contexteAltercation: analyse === 'DEMISSION_EQUIVOQUE',
      pressionOuMenace: analyse === 'DEMISSION_EQUIVOQUE',
      retractationDansDelai: analyse === 'RETRACTATION_POSSIBLE',
      delaiRetractationJours: analyse === 'RETRACTATION_POSSIBLE' ? 2 : null,
      manquementsEmployeurContemporains: false,
      etatEmotionnelPerturbe: false,
      // Sorties calculées.
      analyseDemission: analyse,
      scoreEquivocite: analyse === 'DEMISSION_EQUIVOQUE' ? 80
                       : analyse === 'RETRACTATION_POSSIBLE' ? 30
                       : 0,
      facteursEquivocite: [
        {
          code: 'DT41_CONTEXTE_ALTERCATION',
          libelle: 'Démission donnée dans un contexte d\'altercation',
          fondement: 'Cass. soc. 09/05/2007',
          poids: 25,
          explication: 'Contexte tendu documenté.',
        },
      ],
      requalificationPossible: analyse !== 'VOLONTE_CLAIRE',
      basesJuridiques: ['L. 1237-1 CT', 'Cass. soc. 09/05/2007'],
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
        DemissionEquivoqueSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DemissionEquivoqueSectionComponent);
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
    const banner = fixture.nativeElement.querySelector('[data-testid="dem-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // getPrefillCount — SF-212-22 toujours 0 (sous-objet IA backend non livré)
  // ---------------------------------------------------------------------------

  it('getPrefillCount = 0 (pas de sous-objet IA backend SF-212-21)', () => {
    expect(DemissionEquivoqueSectionComponent.getPrefillCount({
      aiData: { conventionCollective: 'SYNTEC' } as never,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(DemissionEquivoqueSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(DemissionEquivoqueSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux
  // ---------------------------------------------------------------------------

  it('POST → verdict DEMISSION_EQUIVOQUE + refresh dashboard', () => {
    component.contexteAltercation.set(true);
    component.pressionOuMenace.set(true);
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.contexteAltercation).toBe(true);
    expect(req.request.body.pressionOuMenace).toBe(true);
    req.flush(response('DEMISSION_EQUIVOQUE'));

    expect(component.result()?.analyseDemission).toBe('DEMISSION_EQUIVOQUE');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict RETRACTATION_POSSIBLE', () => {
    component.retractationDansDelai.set(true);
    component.delaiRetractationJours.set(2);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('RETRACTATION_POSSIBLE'));
    expect(component.result()?.analyseDemission).toBe('RETRACTATION_POSSIBLE');
  });

  it('POST → verdict VOLONTE_CLAIRE', () => {
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('VOLONTE_CLAIRE'));
    expect(component.result()?.analyseDemission).toBe('VOLONTE_CLAIRE');
  });

  it('Verdict banner affiché après calcul', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('DEMISSION_EQUIVOQUE'));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('[data-testid="dem-verdict-banner"]');
    expect(banner).toBeTruthy();
  });

  it('Indicateur requalification visible après calcul', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('DEMISSION_EQUIVOQUE'));
    fixture.detectChanges();
    const figure = fixture.nativeElement.querySelector('[data-testid="dem-figure-requalification"]');
    expect(figure).toBeTruthy();
    expect(figure.textContent).toContain('OUI');
  });

  it('Indicateur requalification = NON pour VOLONTE_CLAIRE', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('VOLONTE_CLAIRE'));
    fixture.detectChanges();
    const figure = fixture.nativeElement.querySelector('[data-testid="dem-figure-requalification"]');
    expect(figure.textContent).toContain('NON');
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

  it('formValid = false si rétractation avec délai négatif', () => {
    component.retractationDansDelai.set(true);
    component.delaiRetractationJours.set(-1);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('VOLONTE_CLAIRE')).toContain('--claire');
    expect(component.verdictBannerClass('DEMISSION_EQUIVOQUE')).toContain('--equivoque');
    expect(component.verdictBannerClass('RETRACTATION_POSSIBLE')).toContain('--retract');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('VOLONTE_CLAIRE')).toContain('claire');
    expect(component.verdictBannerLabel('DEMISSION_EQUIVOQUE')).toContain('équivoque');
    expect(component.verdictBannerLabel('RETRACTATION_POSSIBLE')).toContain('Rétractation');
  });

  // ---------------------------------------------------------------------------
  // Handlers
  // ---------------------------------------------------------------------------

  it('onRetractationChange(false) réinitialise le délai', () => {
    component.delaiRetractationJours.set(5);
    component.onRetractationChange(false);
    expect(component.delaiRetractationJours()).toBeNull();
  });

  it('onDelaiChange accepte un entier', () => {
    component.onDelaiChange(7);
    expect(component.delaiRetractationJours()).toBe(7);
  });

  it('onDelaiChange tronque les décimales', () => {
    component.onDelaiChange(3.7);
    expect(component.delaiRetractationJours()).toBe(3);
  });

  it('onDelaiChange ramène un nombre négatif à 0', () => {
    component.onDelaiChange(-2);
    expect(component.delaiRetractationJours()).toBe(0);
  });

  it('onModeChange met à jour le mode', () => {
    component.onModeChange('EMAIL');
    expect(component.modeExpressionDemission()).toBe('EMAIL');
  });

  it('onAltercationChange met à jour le flag', () => {
    component.onAltercationChange(true);
    expect(component.contexteAltercation()).toBe(true);
  });

  it('onPressionChange met à jour le flag', () => {
    component.onPressionChange(true);
    expect(component.pressionOuMenace()).toBe(true);
  });

  it('onManquementsChange met à jour le flag', () => {
    component.onManquementsChange(true);
    expect(component.manquementsEmployeurContemporains()).toBe(true);
  });

  it('onEtatPerturbeChange met à jour le flag', () => {
    component.onEtatPerturbeChange(true);
    expect(component.etatEmotionnelPerturbe()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Erreur d'appel
  // ---------------------------------------------------------------------------

  it('POST erreur 500 → snackbar erreur affichée', () => {
    component.calculate();
    httpMock.expectOne(BASE_URL).flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalled();
  });
});
