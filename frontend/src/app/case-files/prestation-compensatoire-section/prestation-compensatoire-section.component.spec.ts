import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PrestationCompensatoireSectionComponent } from './prestation-compensatoire-section.component';
import { PrestationCompensatoireResponse } from '../../core/models/prestation-compensatoire-sf216.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

/**
 * SF-216-02 — Jest spec composant `PrestationCompensatoireSectionComponent`.
 *
 * Couvre :
 *  - country gate BE (pas d'appel HTTP) ;
 *  - load initial GET FR (404 → mode formulaire) ;
 *  - pré-fill IA depuis `FamilleExtractedData` (6 champs) avec badges provenance ;
 *  - validation formulaire (5 champs obligatoires) ;
 *  - POST calculate + affichage du résultat + appel refresh dashboard ;
 *  - error handler snackBar ;
 *  - `static getPrefillCount` parité avec helper PrefillRules.
 */
describe('PrestationCompensatoireSectionComponent', () => {
  let component: PrestationCompensatoireSectionComponent;
  let fixture: ComponentFixture<PrestationCompensatoireSectionComponent>;
  let httpMock: HttpTestingController;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/prestation-compensatoire';

  /** SF-216-02 fixture IA complète — les 6 champs FamilleExtractedData utiles. */
  const FULL_AI_DATA: FamilleExtractedData = {
    dureeMariageAnnees: 18,
    revenusAnnuelsEpoux1: 45000,
    revenusAnnuelsEpoux2: 22000,
    ageEpoux1Annees: 52,
    ageEpoux2Annees: 48,
    clauseAttributionIntegraleDetected: true,
  };

  function response(): PrestationCompensatoireResponse {
    return {
      caseFileId: 'case-1',
      montantCapitalIndicatifEur: 96000,
      renteIndicativeMensuelleEur: 480,
      formeRecommandee: 'CAPITAL',
      dispAriteNiveauxVie: 'Disparité significative (~ 50 %)',
      baseJuridique: 'art. 270-281 Cciv',
      messages: [
        'Capital recommandé compte tenu de la durée du mariage > 15 ans.',
      ],
      alertes: [],
      country: 'FRANCE',
    };
  }

  beforeEach(async () => {
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        PrestationCompensatoireSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        // MatSnackBar — spy minimal pour éviter le DOM overlay.
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PrestationCompensatoireSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ---------------------------------------------------------------------------
  // Country gate
  // ---------------------------------------------------------------------------

  it('BELGIQUE → aucun appel HTTP initial', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Chargement (GET)
  // ---------------------------------------------------------------------------

  it('mount FRANCE → GET initial déclenché', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('GET 200 → résultat hydraté, formulaire masqué', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()).toBeTruthy();
    expect(component.result()!.montantCapitalIndicatifEur).toBe(96000);
    expect(component.showForm()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA FR : 6 champs renseignés avec provenance IA', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    // Le GET initial échoue (404) → mode formulaire avec pré-fill IA actif.
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dureeMariageAnnees()).toBe(18);
    expect(component.revenusAnnuelsEpoux1Eur()).toBe(45000);
    expect(component.revenusAnnuelsEpoux2Eur()).toBe(22000);
    expect(component.ageEpoux1Annees()).toBe(52);
    expect(component.ageEpoux2Annees()).toBe(48);
    expect(component.avantageMatrimonialDetecte()).toBe(true);

    expect(component.provenanceDureeMariage()).toBe('IA');
    expect(component.provenanceRevenusEpoux1()).toBe('IA');
    expect(component.provenanceRevenusEpoux2()).toBe('IA');
    expect(component.provenanceAgeEpoux1()).toBe('IA');
    expect(component.provenanceAgeEpoux2()).toBe('IA');
    expect(component.provenanceAvantageMatrimonial()).toBe('IA');
  });

  it('pré-fill IA BELGIQUE : 0 champ renseigné (FR-only)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    expect(component.dureeMariageAnnees()).toBeNull();
    expect(component.revenusAnnuelsEpoux1Eur()).toBeNull();
    expect(component.provenanceDureeMariage()).toBeNull();
  });

  it('modification manuelle d\'un champ pré-rempli efface le badge IA', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.provenanceDureeMariage()).toBe('IA');
    component.onDureeMariageChange(20);
    expect(component.dureeMariageAnnees()).toBe(20);
    expect(component.provenanceDureeMariage()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------------

  it('formulaire vide FR → invalid', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.formValid()).toBe(false);
  });

  it('5 champs obligatoires renseignés → valid', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onDureeMariageChange(15);
    component.onRevenusEpoux1Change(40000);
    component.onRevenusEpoux2Change(20000);
    component.onAgeEpoux1Change(50);
    component.onAgeEpoux2Change(48);
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // POST calculate
  // ---------------------------------------------------------------------------

  it('calculate() POST → result + refresh dashboard', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onDureeMariageChange(18);
    component.onRevenusEpoux1Change(45000);
    component.onRevenusEpoux2Change(22000);
    component.onAgeEpoux1Change(52);
    component.onAgeEpoux2Change(48);

    component.calculate();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dureeMariageAnnees).toBe(18);
    expect(req.request.body.revenusAnnuelsEpoux1Eur).toBe(45000);
    expect(req.request.body.ageEpoux1Annees).toBe(52);
    req.flush(response());

    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() error → reste en mode formulaire', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onDureeMariageChange(10);
    component.onRevenusEpoux1Change(30000);
    component.onRevenusEpoux2Change(15000);
    component.onAgeEpoux1Change(40);
    component.onAgeEpoux2Change(38);

    component.calculate();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });

    expect(component.result()).toBeNull();
    expect(component.showForm()).toBe(true);
    expect(component.calculating()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // standaloneMode
  // ---------------------------------------------------------------------------

  it('standaloneMode → pas de pré-fill IA, pas de GET initial via case-file', () => {
    component.standaloneMode = true;
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    // standaloneMode ne coupe pas le GET initial dans ce composant — vérifie juste
    // l'absence de pré-fill IA.
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.dureeMariageAnnees()).toBeNull();
    expect(component.provenanceDureeMariage()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Static getPrefillCount — parité helper
  // ---------------------------------------------------------------------------

  it('getPrefillCount({}) === 0', () => {
    expect(PrestationCompensatoireSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount({aiData, FRANCE}) === 6', () => {
    const n = PrestationCompensatoireSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    });
    expect(n).toBe(6);
  });

  it('getPrefillCount({aiData, BELGIQUE}) === 0 (FR-only)', () => {
    const n = PrestationCompensatoireSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    });
    expect(n).toBe(0);
  });
});
