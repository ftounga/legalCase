import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LiquidationCommunauteFrSectionComponent } from './liquidation-communaute-fr-section.component';
import { LiquidationCommunauteFrResponse } from '../../core/models/liquidation-communaute-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

/**
 * SF-216-06 — Jest spec composant `LiquidationCommunauteFrSectionComponent`.
 *
 * Couvre :
 *  - country gate BE (pas d'appel HTTP) ;
 *  - load initial GET FR (404 → mode formulaire) ;
 *  - pré-fill IA depuis `FamilleExtractedData` (2 champs récompenses + signaux) ;
 *  - signal régime incompatible (bannière + formValid false) ;
 *  - signal clause attribution intégrale détectée ;
 *  - validation formulaire (tous champs facultatifs ≥ 0) ;
 *  - POST calculate + affichage du résultat + appel refresh dashboard ;
 *  - error handler snackBar ;
 *  - `static getPrefillCount` parité avec helper PrefillRules.
 */
describe('LiquidationCommunauteFrSectionComponent', () => {
  let component: LiquidationCommunauteFrSectionComponent;
  let fixture: ComponentFixture<LiquidationCommunauteFrSectionComponent>;
  let httpMock: HttpTestingController;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/liquidation-communaute-fr';

  /** SF-216-06 fixture IA — régime + clause + 2 récompenses. */
  const FULL_AI_DATA: FamilleExtractedData = {
    regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE',
    valeurCommunauteEurDetectee: 320_000,
    clauseAttributionIntegraleDetected: false,
    recompensesEpoux1Eur: 5_000,
    recompensesEpoux2Eur: 12_000,
  };

  function response(): LiquidationCommunauteFrResponse {
    return {
      caseFileId: 'case-1',
      masseCommuneEur: 300_000,
      quotaPartEpoux1Eur: 150_000,
      quotaPartEpoux2Eur: 150_000,
      soulteEur: 0,
      recompensesNettesEpoux1Eur: 0,
      recompensesNettesEpoux2Eur: 0,
      alerteIndivision: false,
      baseJuridique: 'art. 1467-1517 Cciv + art. 1433-1435 Cciv',
      messages: [
        'Montant indicatif — la liquidation définitive relève du notaire désigné (art. 1364 CPC).',
      ],
      alertes: [],
      country: 'FRANCE',
    };
  }

  beforeEach(async () => {
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        LiquidationCommunauteFrSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LiquidationCommunauteFrSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

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
    expect(component.result()!.masseCommuneEur).toBe(300_000);
    expect(component.showForm()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA FR : récompenses 1/2 + régime + clause lus', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.recompensesEpoux1Eur()).toBe(5_000);
    expect(component.recompensesEpoux2Eur()).toBe(12_000);
    expect(component.regimeMatrimonialDetecte()).toBe('COMMUNAUTE_LEGALE');
    expect(component.clauseAttributionIntegraleDetectee()).toBe(false);

    expect(component.provenanceRecompensesEpoux1()).toBe('IA');
    expect(component.provenanceRecompensesEpoux2()).toBe('IA');
  });

  it('pré-fill IA BELGIQUE : 0 champ renseigné (FR-only)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    expect(component.recompensesEpoux1Eur()).toBeNull();
    expect(component.recompensesEpoux2Eur()).toBeNull();
    expect(component.regimeMatrimonialDetecte()).toBeNull();
  });

  it('modification manuelle d\'un champ pré-rempli efface le badge IA', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.provenanceRecompensesEpoux1()).toBe('IA');
    component.onRecompenses1Change(8_000);
    expect(component.recompensesEpoux1Eur()).toBe(8_000);
    expect(component.provenanceRecompensesEpoux1()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Régime incompatible
  // ---------------------------------------------------------------------------

  it('régime SEPARATION_BIENS → formulaire invalide + bannière', () => {
    component.aiData = {
      regimeMatrimonialDetecte: 'SEPARATION_BIENS',
    };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.regimeIncompatible()).toBe(true);
    expect(component.formValid()).toBe(false);
  });

  it('régime COMMUNAUTE_LEGALE → formulaire valide', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.regimeIncompatible()).toBe(false);
    expect(component.formValid()).toBe(true);
  });

  it('régime null (non documenté) → formulaire valide', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.regimeIncompatible()).toBe(false);
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------------

  it('formulaire vide FR → valid (tous champs facultatifs)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.formValid()).toBe(true);
  });

  it('valeur immobilière + crédit renseignés → valid', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onImmeuble1Change(300_000);
    component.onCapitalRestantDuChange(0);
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // POST calculate
  // ---------------------------------------------------------------------------

  it('calculate() POST → result + refresh dashboard', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onImmeuble1Change(300_000);
    component.onCapitalRestantDuChange(0);

    component.calculate();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.valeurImmeubleCommun1Eur).toBe(300_000);
    expect(req.request.body.capitalRestantDuEur).toBe(0);
    req.flush(response());

    expect(component.result()).toBeTruthy();
    expect(component.result()!.masseCommuneEur).toBe(300_000);
    expect(component.showForm()).toBe(false);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() error → reste en mode formulaire', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onImmeuble1Change(100_000);

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

  it('standaloneMode → pas de pré-fill IA', () => {
    component.standaloneMode = true;
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.recompensesEpoux1Eur()).toBeNull();
    expect(component.provenanceRecompensesEpoux1()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Static getPrefillCount — parité helper
  // ---------------------------------------------------------------------------

  it('getPrefillCount({}) === 0', () => {
    expect(LiquidationCommunauteFrSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount({aiData, FRANCE}) === 2', () => {
    const n = LiquidationCommunauteFrSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    });
    expect(n).toBe(2);
  });

  it('getPrefillCount({aiData, BELGIQUE}) === 0 (FR-only)', () => {
    const n = LiquidationCommunauteFrSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    });
    expect(n).toBe(0);
  });
});
