import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MiseAPiedDisciplinaireSectionComponent } from './mise-a-pied-disciplinaire-section.component';
import {
  MapAnalyse,
  MiseAPiedDisciplinaireResponse,
} from '../../core/models/mise-a-pied-disciplinaire.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('MiseAPiedDisciplinaireSectionComponent', () => {
  let component: MiseAPiedDisciplinaireSectionComponent;
  let fixture: ComponentFixture<MiseAPiedDisciplinaireSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/mise-a-pied-disciplinaire';

  /** Fixture IA complète — les 7 champs `mapDisciplinaire*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    mapDisciplinaireNature: 'DISCIPLINAIRE',
    mapDisciplinaireProcedureSuivie: true,
    mapDisciplinairePrescriptionFaute: true,
    mapDisciplinaireDureeRi: true,
    mapDisciplinaireDureeJours: 3,
    mapDisciplinaireSalaireSuspendu: true,
    mapDisciplinaireSanctionsAnterieures: false,
  };

  function response(
    analyse: MapAnalyse,
  ): MiseAPiedDisciplinaireResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs (ré-édition du formulaire après recharge).
      natureMiseAPied: analyse === 'CONSERVATOIRE' ? 'CONSERVATOIRE' : 'DISCIPLINAIRE',
      procedureEntretienSuivie: analyse !== 'IRREGULIERE_FORME',
      prescriptionFauteVerifiee: analyse !== 'IRREGULIERE_FOND',
      dureeDefiniedansRI: analyse !== 'IRREGULIERE_FORME',
      dureeJours: 3,
      salaireSuspendu: analyse === 'REGULIERE',
      sancionsAnterieuresMemesFaits: false,
      salaireMensuelBrutEuros: 3000.0,
      // Sorties calculées.
      analyseMiseAPied: analyse,
      scoreMiseAPied: analyse === 'REGULIERE' ? 85
                    : analyse === 'IRREGULIERE_FORME' ? 30
                    : analyse === 'IRREGULIERE_FOND' ? 15
                    : 50,
      pointsRegularite: [
        {
          code: 'DT48_NATURE_MISE_A_PIED',
          libelle: 'Mise à pied qualifiée de DISCIPLINAIRE',
          fondement: 'L. 1331-1 CT',
          conclusion: 'OK',
        },
      ],
      salaireDuPeriodeMiseAPiedEuros: analyse === 'REGULIERE' ? 0.0 : 415.32,
      basesJuridiques: ['L. 1331-1 CT', 'L. 1332-4 CT', 'Cass. soc.'],
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
        MiseAPiedDisciplinaireSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MiseAPiedDisciplinaireSectionComponent);
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
    const banner = fixture.nativeElement.querySelector('[data-testid="mapd-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 7 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.natureMiseAPied()).toBe('DISCIPLINAIRE');
    expect(component.procedureEntretienSuivie()).toBe(true);
    expect(component.prescriptionFauteVerifiee()).toBe(true);
    expect(component.dureeDefiniedansRI()).toBe(true);
    expect(component.dureeJours()).toBe(3);
    expect(component.salaireSuspendu()).toBe(true);
    expect(component.sancionsAnterieuresMemesFaits()).toBe(false);
    expect(component.provenanceNature()).toBe('IA');
    expect(component.provenanceDureeJours()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    expect(component.provenanceNature()).toBeNull();
  });

  it('getPrefillCount = 7 sur fixture IA FR complète', () => {
    expect(MiseAPiedDisciplinaireSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(7);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(MiseAPiedDisciplinaireSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(MiseAPiedDisciplinaireSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 4 niveaux
  // ---------------------------------------------------------------------------

  it('POST → verdict REGULIERE + refresh dashboard', () => {
    component.natureMiseAPied.set('DISCIPLINAIRE');
    component.procedureEntretienSuivie.set(true);
    component.prescriptionFauteVerifiee.set(true);
    component.dureeDefiniedansRI.set(true);
    component.dureeJours.set(3);
    component.salaireSuspendu.set(true);
    component.sancionsAnterieuresMemesFaits.set(false);
    component.salaireMensuelBrutEuros.set(3000.0);
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.natureMiseAPied).toBe('DISCIPLINAIRE');
    req.flush(response('REGULIERE'));

    expect(component.result()?.analyseMiseAPied).toBe('REGULIERE');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict IRREGULIERE_FORME', () => {
    component.procedureEntretienSuivie.set(false);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('IRREGULIERE_FORME'));
    expect(component.result()?.analyseMiseAPied).toBe('IRREGULIERE_FORME');
  });

  it('POST → verdict IRREGULIERE_FOND', () => {
    component.prescriptionFauteVerifiee.set(false);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('IRREGULIERE_FOND'));
    expect(component.result()?.analyseMiseAPied).toBe('IRREGULIERE_FOND');
  });

  it('POST → verdict CONSERVATOIRE', () => {
    component.natureMiseAPied.set('CONSERVATOIRE');
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CONSERVATOIRE'));
    expect(component.result()?.analyseMiseAPied).toBe('CONSERVATOIRE');
  });

  it('Verdict banner affiché après calcul', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('REGULIERE'));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('[data-testid="mapd-verdict-banner"]');
    expect(banner).toBeTruthy();
  });

  it('Affiche salaire dû dans le résultat', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.natureMiseAPied.set('CONSERVATOIRE');
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CONSERVATOIRE'));
    fixture.detectChanges();
    const figure = fixture.nativeElement.querySelector('[data-testid="mapd-figure-salaire-du"]');
    expect(figure).toBeTruthy();
    // L'environnement Jest n'a pas de locale FR active — le pipe number
    // produit "415.32" (séparateur point). On vérifie la présence des chiffres.
    expect(figure.textContent).toMatch(/415[.,]32/);
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

  it('formValid = false si durée négative', () => {
    component.dureeJours.set(-1);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 4 niveaux', () => {
    expect(component.verdictBannerClass('REGULIERE')).toContain('--reguliere');
    expect(component.verdictBannerClass('IRREGULIERE_FORME')).toContain('--forme');
    expect(component.verdictBannerClass('IRREGULIERE_FOND')).toContain('--fond');
    expect(component.verdictBannerClass('CONSERVATOIRE')).toContain('--conservatoire');
  });

  it('verdictBannerLabel mappe les 4 niveaux', () => {
    expect(component.verdictBannerLabel('REGULIERE')).toContain('régulière');
    expect(component.verdictBannerLabel('IRREGULIERE_FORME')).toContain('forme');
    expect(component.verdictBannerLabel('IRREGULIERE_FOND')).toContain('fond');
    expect(component.verdictBannerLabel('CONSERVATOIRE')).toContain('conservatoire');
  });

  it('Handler nature efface la provenance IA', () => {
    component.provenanceNature.set('IA');
    component.onNatureChange('CONSERVATOIRE');
    expect(component.natureMiseAPied()).toBe('CONSERVATOIRE');
    expect(component.provenanceNature()).toBeNull();
  });

  it('Handler durée accepte une valeur entière', () => {
    component.onDureeJoursChange(5);
    expect(component.dureeJours()).toBe(5);
  });

  it('Handler durée rejette les valeurs négatives (ramène à null)', () => {
    component.onDureeJoursChange(-1);
    expect(component.dureeJours()).toBeNull();
  });

  it('Handler durée accepte null (vide le champ)', () => {
    component.dureeJours.set(3);
    component.onDureeJoursChange(null);
    expect(component.dureeJours()).toBeNull();
  });

  it('Handler salaire mensuel accepte un nombre', () => {
    component.onSalaireMensuelChange(2500);
    expect(component.salaireMensuelBrutEuros()).toBe(2500);
  });

  it('Handler salaire mensuel rejette une valeur négative (ramène à 0)', () => {
    component.onSalaireMensuelChange(-100);
    expect(component.salaireMensuelBrutEuros()).toBe(0);
  });
});
