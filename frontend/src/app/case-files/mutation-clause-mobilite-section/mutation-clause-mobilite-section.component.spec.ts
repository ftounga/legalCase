import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MutationClauseMobiliteSectionComponent } from './mutation-clause-mobilite-section.component';
import {
  MutationAnalyse,
  MutationClauseMobiliteResponse,
} from '../../core/models/mutation-clause-mobilite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('MutationClauseMobiliteSectionComponent', () => {
  let component: MutationClauseMobiliteSectionComponent;
  let fixture: ComponentFixture<MutationClauseMobiliteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/mutation-clause-mobilite';

  /** Fixture IA complète — les 6 champs `mutation*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    mutationClausePresente: true,
    mutationZoneGeographiquePrecise: true,
    mutationInteretLegitimeEmployeur: true,
    mutationDelaiPrevenanceSemaines: 4,
    mutationSituationFamilialeContraingnante: false,
    mutationMotifProfessionnel: true,
  };

  function response(
    analyse: MutationAnalyse,
  ): MutationClauseMobiliteResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs (ré-édition du formulaire après recharge).
      clauseMobilitePresente: analyse !== 'ABSENCE_CLAUSE',
      zoneGeographiquePrecise: analyse === 'CLAUSE_VALIDE',
      interetLegitimeEmployeur: true,
      delaiPrevenanceSemaines: analyse === 'CLAUSE_VALIDE' ? 4 : 1,
      situationFamilialeSalarieContraignante: false,
      motifMutationProfessionnel: true,
      reponseSalarie: 'REFUS',
      // Sorties calculées.
      analyseMutation: analyse,
      scoreMutation: analyse === 'CLAUSE_VALIDE' ? 85
                    : analyse === 'CLAUSE_INVALIDE' ? 30
                    : 10,
      pointsAnalyse: [
        {
          code: 'DT71_CLAUSE_PRESENTE',
          libelle: 'Clause de mobilité présente',
          fondement: 'Cass. soc. 03/06/2003 n°01-40.376',
          conclusion: 'OK — clause documentée.',
        },
      ],
      consequences: [
        {
          type: 'REFUS_FAUTE_LICENCIEMENT_POSSIBLE',
          description: 'Refus + clause valide → faute.',
          fondement: 'Cass. soc. 04/01/2000 n°97-44.566',
        },
      ],
      consequencesRefus: 'Refus du salarié → faute possible si clause valide.',
      basesJuridiques: ['Cass. soc. constante — clause de mobilité', 'L. 1221-1 CT'],
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
        MutationClauseMobiliteSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MutationClauseMobiliteSectionComponent);
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
    const banner = fixture.nativeElement.querySelector('[data-testid="mcm-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 6 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.clauseMobilitePresente()).toBe(true);
    expect(component.zoneGeographiquePrecise()).toBe(true);
    expect(component.interetLegitimeEmployeur()).toBe(true);
    expect(component.delaiPrevenanceSemaines()).toBe(4);
    expect(component.situationFamilialeSalarieContraignante()).toBe(false);
    expect(component.motifMutationProfessionnel()).toBe(true);
    expect(component.provenanceClausePresente()).toBe('IA');
    expect(component.provenanceDelai()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    expect(component.provenanceClausePresente()).toBeNull();
  });

  it('getPrefillCount = 6 sur fixture IA FR complète', () => {
    expect(MutationClauseMobiliteSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(6);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(MutationClauseMobiliteSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(MutationClauseMobiliteSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux
  // ---------------------------------------------------------------------------

  it('POST → verdict CLAUSE_VALIDE + refresh dashboard', () => {
    component.clauseMobilitePresente.set(true);
    component.zoneGeographiquePrecise.set(true);
    component.interetLegitimeEmployeur.set(true);
    component.delaiPrevenanceSemaines.set(4);
    component.motifMutationProfessionnel.set(true);
    component.reponseSalarie.set('REFUS');
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.clauseMobilitePresente).toBe(true);
    req.flush(response('CLAUSE_VALIDE'));

    expect(component.result()?.analyseMutation).toBe('CLAUSE_VALIDE');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict CLAUSE_INVALIDE', () => {
    component.clauseMobilitePresente.set(true);
    component.zoneGeographiquePrecise.set(false);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CLAUSE_INVALIDE'));
    expect(component.result()?.analyseMutation).toBe('CLAUSE_INVALIDE');
  });

  it('POST → verdict ABSENCE_CLAUSE', () => {
    component.clauseMobilitePresente.set(false);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('ABSENCE_CLAUSE'));
    expect(component.result()?.analyseMutation).toBe('ABSENCE_CLAUSE');
  });

  it('Verdict banner affiché après calcul', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.clauseMobilitePresente.set(true);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CLAUSE_VALIDE'));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('[data-testid="mcm-verdict-banner"]');
    expect(banner).toBeTruthy();
  });

  it('Affiche les conséquences du refus (chapeau)', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CLAUSE_VALIDE'));
    fixture.detectChanges();
    const cs = fixture.nativeElement.querySelector('[data-testid="mcm-consequences-refus"]');
    expect(cs).toBeTruthy();
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
    component.delaiPrevenanceSemaines.set(-1);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('CLAUSE_VALIDE')).toContain('--valide');
    expect(component.verdictBannerClass('CLAUSE_INVALIDE')).toContain('--invalide');
    expect(component.verdictBannerClass('ABSENCE_CLAUSE')).toContain('--absence');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('CLAUSE_VALIDE')).toContain('valide');
    expect(component.verdictBannerLabel('CLAUSE_INVALIDE')).toContain('invalide');
    expect(component.verdictBannerLabel('ABSENCE_CLAUSE')).toContain('Absence');
  });

  it('Handler clause présente efface la provenance IA', () => {
    component.provenanceClausePresente.set('IA');
    component.onClausePresenteChange(false);
    expect(component.clauseMobilitePresente()).toBe(false);
    expect(component.provenanceClausePresente()).toBeNull();
  });

  it('Handler delai accepte une valeur entière', () => {
    component.onDelaiChange(2);
    expect(component.delaiPrevenanceSemaines()).toBe(2);
  });

  it('Handler delai rejette les valeurs négatives (ramène à null)', () => {
    component.onDelaiChange(-1);
    expect(component.delaiPrevenanceSemaines()).toBeNull();
  });

  it('Handler delai accepte null (vide le champ)', () => {
    component.delaiPrevenanceSemaines.set(2);
    component.onDelaiChange(null);
    expect(component.delaiPrevenanceSemaines()).toBeNull();
  });
});
