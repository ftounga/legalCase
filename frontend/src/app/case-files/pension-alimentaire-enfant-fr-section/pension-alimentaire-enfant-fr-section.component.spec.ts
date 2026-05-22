import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PensionAlimentaireEnfantFrSectionComponent } from './pension-alimentaire-enfant-fr-section.component';
import { PensionAlimentaireEnfantFrResponse } from '../../core/models/pension-alimentaire-enfant-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

/**
 * SF-216-04 — Jest spec composant `PensionAlimentaireEnfantFrSectionComponent`.
 *
 * Couvre :
 *  - country gate BE (pas d'appel HTTP) ;
 *  - load initial GET FR (404 → mode formulaire) ;
 *  - pré-fill IA depuis `FamilleExtractedData` (5 champs) ;
 *  - validation formulaire (nombreEnfants + agesEnfants alignés + modeResidence) ;
 *  - POST calculate + affichage du résultat + appel refresh dashboard ;
 *  - error handler snackBar ;
 *  - `static getPrefillCount` parité avec helper PrefillRules.
 */
describe('PensionAlimentaireEnfantFrSectionComponent', () => {
  let component: PensionAlimentaireEnfantFrSectionComponent;
  let fixture: ComponentFixture<PensionAlimentaireEnfantFrSectionComponent>;
  let httpMock: HttpTestingController;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/pension-alimentaire-enfant-fr';

  /** SF-216-04 fixture IA — 2 revenus + 2 enfants + mode résidence. */
  const FULL_AI_DATA: FamilleExtractedData = {
    revenusAnnuelsEpoux1: 24_000, // → 2 000 €/mois
    revenusAnnuelsEpoux2: 36_000, // → 3 000 €/mois
    nbEnfantsACharge: 2,
    agesEnfantsDetectes: [8, 12],
    modeResidenceEnfantsDetecte: 'PRINCIPALE_PARENT1',
  };

  function response(): PensionAlimentaireEnfantFrResponse {
    return {
      caseFileId: 'case-1',
      montantParEnfantMensuelEur: [390, 390],
      totalMensuelEur: 780,
      tauxApplique: 0.26,
      coefficientResidence: 1.0,
      parentDebiteur: 'PARENT2',
      baseJuridique: 'art. 371-2 Cciv + barème indicatif Cour de cassation (2010, révisé)',
      messages: ['Montant indicatif — le JAF fixe librement la contribution.'],
      alertes: [],
      country: 'FRANCE',
    };
  }

  beforeEach(async () => {
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        PensionAlimentaireEnfantFrSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PensionAlimentaireEnfantFrSectionComponent);
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
    expect(component.result()!.totalMensuelEur).toBe(780);
    expect(component.showForm()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA FR : 5 champs remplis', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.revenusNetsParent1Eur()).toBe(2_000);
    expect(component.revenusNetsParent2Eur()).toBe(3_000);
    expect(component.nombreEnfants()).toBe(2);
    expect(component.agesEnfants()).toEqual([8, 12]);
    expect(component.modeResidence()).toBe('PRINCIPALE_PARENT1');

    expect(component.provenanceRevenus1()).toBe('IA');
    expect(component.provenanceRevenus2()).toBe('IA');
    expect(component.provenanceNombreEnfants()).toBe('IA');
    expect(component.provenanceAgesEnfants()).toBe('IA');
    expect(component.provenanceModeResidence()).toBe('IA');
  });

  it('pré-fill IA BELGIQUE : 0 champ renseigné (FR-only)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    expect(component.revenusNetsParent1Eur()).toBeNull();
    expect(component.nombreEnfants()).toBeNull();
    expect(component.modeResidence()).toBeNull();
  });

  it("modification manuelle d'un champ pré-rempli efface le badge IA", () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.provenanceRevenus2()).toBe('IA');
    component.onRevenus2Change(4_000);
    expect(component.revenusNetsParent2Eur()).toBe(4_000);
    expect(component.provenanceRevenus2()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------------

  it('formulaire vide FR → invalide (manque nombreEnfants + modeResidence)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.formValid()).toBe(false);
  });

  it('formulaire complet → valid', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onNombreEnfantsChange(1);
    component.onAgeEnfantChange(0, 8);
    component.onModeResidenceChange('PRINCIPALE_PARENT1');
    component.onRevenus2Change(3000);
    expect(component.formValid()).toBe(true);
  });

  it('ages length != nombreEnfants → invalid', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onNombreEnfantsChange(2);
    // 2 ages auto-created. On bidouille pour les rendre incohérents :
    component.agesEnfants.set([8]); // longueur 1 ≠ 2 enfants
    component.onModeResidenceChange('PRINCIPALE_PARENT1');
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // POST calculate
  // ---------------------------------------------------------------------------

  it('calculate() POST → result + refresh dashboard', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onRevenus2Change(3000);
    component.onNombreEnfantsChange(2);
    component.onAgeEnfantChange(0, 8);
    component.onAgeEnfantChange(1, 12);
    component.onModeResidenceChange('PRINCIPALE_PARENT1');

    component.calculate();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.revenusNetsParent2Eur).toBe(3000);
    expect(req.request.body.modeResidence).toBe('PRINCIPALE_PARENT1');
    req.flush(response());

    expect(component.result()).toBeTruthy();
    expect(component.result()!.totalMensuelEur).toBe(780);
    expect(component.showForm()).toBe(false);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() error → reste en mode formulaire', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onNombreEnfantsChange(1);
    component.onAgeEnfantChange(0, 8);
    component.onModeResidenceChange('PRINCIPALE_PARENT1');
    component.onRevenus2Change(3000);

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
    expect(component.revenusNetsParent2Eur()).toBeNull();
    expect(component.provenanceRevenus2()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Static getPrefillCount — parité helper
  // ---------------------------------------------------------------------------

  it('getPrefillCount({}) === 0', () => {
    expect(PensionAlimentaireEnfantFrSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount({aiData, FRANCE}) === 5', () => {
    const n = PensionAlimentaireEnfantFrSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    });
    expect(n).toBe(5);
  });

  it('getPrefillCount({aiData, BELGIQUE}) === 0 (FR-only)', () => {
    const n = PensionAlimentaireEnfantFrSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    });
    expect(n).toBe(0);
  });
});
