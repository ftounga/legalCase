import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { IndivisionSectionComponent } from './indivision-section.component';
import { IndivisionResponse } from '../../core/models/indivision.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';

describe('IndivisionSectionComponent', () => {
  let component: IndivisionSectionComponent;
  let fixture: ComponentFixture<IndivisionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/indivision';

  function exampleResponse(): IndivisionResponse {
    return {
      caseFileId: 'case-1',
      dateOrigineIndivision: '2025-01-15',
      natureBiens: ['IMMOBILIER', 'COMPTES_BANCAIRES'],
      valeurEstimeeTotaleEur: 350000,
      nbIndivisaires: 2,
      quotesPart: [50, 50],
      tentativesPartageAmiable: ['PROPOSITION_NOTAIRE'],
      consentementPartageGlobal: false,
      occupationBienParUnIndivisaire: true,
      indivisionDureeAnnees: 1,
      demandeMesuresConservatoires: false,
      conflitOuvertEntreIndivisaires: true,
      scoreEligibilitePartageJudiciaire: 78,
      verdictRecommandation: 'PARTAGE_JUDICIAIRE_RECOMMANDE',
      indemniteOccupationDueEur: 12000,
      expertiseNotarialeRecommandee: true,
      licitationRecommandee: false,
      delaiProcedurePartageJudiciaireMois: 18,
      baseJuridique: 'Art. 815 et s. + 815-9 al. 2 + 1377 CPC',
      formule: 'score = 78/100',
      messages: ['Partage judiciaire recommandé compte tenu du conflit ouvert.'],
      country: 'FRANCE',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        IndivisionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IndivisionSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + GET initial
  // ---------------------------------------------------------------------------

  it('mount FRANCE → GET initial déclenché, 404 → form visible', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('mount FRANCE → GET 200 → restore inputs + result + masque form', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(exampleResponse());

    expect(component.result()!.scoreEligibilitePartageJudiciaire).toBe(78);
    expect(component.dateOrigineIndivision()).toBe('2025-01-15');
    expect(component.natureBiens()).toEqual(['IMMOBILIER', 'COMPTES_BANCAIRES']);
    expect(component.nbIndivisaires()).toBe(2);
    expect(component.quotesPart()).toEqual([50, 50]);
    expect(component.showForm()).toBe(false);
    // Valeurs persistées ≠ IA
    expect(component.provenanceDateOrigine()).toBeNull();
    expect(component.provenanceOccupation()).toBeNull();
  });

  it('mount BELGIQUE → pas de GET, gate bannière (form masqué via template)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    // showForm initial reste true (la bannière country masque côté template).
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Helpers : parseQuotesPart + formValid
  // ---------------------------------------------------------------------------

  it('parseQuotesPart accepte CSV "60, 40" → [60, 40]', () => {
    expect(component.parseQuotesPart('60, 40')).toEqual([60, 40]);
    expect(component.parseQuotesPart('33.33; 33.33; 33.34')).toEqual([33.33, 33.33, 33.34]);
    expect(component.parseQuotesPart('')).toEqual([]);
    expect(component.parseQuotesPart('abc, 50, -1, 150')).toEqual([50]);
  });

  it('formValid : exige date + natureBiens + nbIndivisaires>=2 + quotesPart', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.formValid()).toBe(false);
    component.onDateOrigineChange('2025-01-15');
    expect(component.formValid()).toBe(false); // natureBiens vide
    component.onNatureBiensChange(['IMMOBILIER']);
    expect(component.formValid()).toBe(false); // quotesPart vide
    component.onQuotesPartRawChange('50, 50');
    expect(component.formValid()).toBe(true);

    // nbIndivisaires < 2 → invalid
    component.onNbIndivisairesChange(1);
    expect(component.formValid()).toBe(false);
    component.onNbIndivisairesChange(2);
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Analyser (POST)
  // ---------------------------------------------------------------------------

  it('analyser : POST émis avec payload complet + triggerRefresh + showForm=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onDateOrigineChange('2025-01-15');
    component.onNatureBiensChange(['IMMOBILIER']);
    component.onValeurEstimeeChange(350000);
    component.onNbIndivisairesChange(2);
    component.onQuotesPartRawChange('50, 50');
    component.onTentativesChange(['PROPOSITION_NOTAIRE']);
    component.onOccupationChange(true);
    component.onIndivisionDureeChange(2);

    component.analyser();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateOrigineIndivision).toBe('2025-01-15');
    expect(req.request.body.natureBiens).toEqual(['IMMOBILIER']);
    expect(req.request.body.valeurEstimeeTotaleEur).toBe(350000);
    expect(req.request.body.nbIndivisaires).toBe(2);
    expect(req.request.body.quotesPart).toEqual([50, 50]);
    expect(req.request.body.tentativesPartageAmiable).toEqual(['PROPOSITION_NOTAIRE']);
    expect(req.request.body.occupationBienParUnIndivisaire).toBe(true);
    req.flush(exampleResponse());

    expect(component.showForm()).toBe(false);
    expect(component.result()).not.toBeNull();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyser : erreur 400 → snackBar message + calculating=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onDateOrigineChange('2025-01-15');
    component.onNatureBiensChange(['IMMOBILIER']);
    component.onQuotesPartRawChange('50, 50');
    component.analyser();

    httpMock.expectOne(BASE_URL).flush(
      { message: 'Quotes-part invalides' },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(component.calculating()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('editMode : repasse en showForm=true', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(exampleResponse());
    expect(component.showForm()).toBe(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('prefillFromAi : dateSeparation IA → provenanceDateOrigine = IA', () => {
    component.aiData = { dateSeparation: '2025-01-15' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateOrigineIndivision()).toBe('2025-01-15');
    expect(component.provenanceDateOrigine()).toBe('IA');
  });

  it('prefillFromAi : logementCommunDetected=true → occupation pré-cochée + provenance', () => {
    component.aiData = { logementCommunDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.occupationBienParUnIndivisaire()).toBe(true);
    expect(component.provenanceOccupation()).toBe('IA');
  });

  it('prefillFromAi : aiData absent → no-op gracieux', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateOrigineIndivision()).toBeNull();
    expect(component.occupationBienParUnIndivisaire()).toBe(false);
    expect(component.provenanceDateOrigine()).toBeNull();
    expect(component.provenanceOccupation()).toBeNull();
  });

  it('onDateOrigineChange manuel : provenance → null', () => {
    component.aiData = { dateSeparation: '2025-01-15' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceDateOrigine()).toBe('IA');
    component.onDateOrigineChange('2024-06-01');
    expect(component.provenanceDateOrigine()).toBeNull();
    expect(component.dateOrigineIndivision()).toBe('2024-06-01');
  });

  it('ngOnChanges : aiData arrive après mount → re-prefill', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateOrigineIndivision()).toBeNull();

    component.aiData = { dateSeparation: '2025-03-20' } as FamilleExtractedData;
    component.ngOnChanges({
      aiData: new SimpleChange(null, component.aiData, false),
    });

    expect(component.dateOrigineIndivision()).toBe('2025-03-20');
    expect(component.provenanceDateOrigine()).toBe('IA');
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts : divergence IA vs avocat sur DATE_ORIGINE → alerte', () => {
    component.aiData = { dateSeparation: '2025-01-15' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Avocat saisit une date différente → divergence.
    component.onDateOrigineChange('2024-06-01');

    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_ORIGINE_INDIVISION).toBeDefined();
    expect(alerts.DATE_ORIGINE_INDIVISION!.contributors).toContain('IA');
    expect(alerts.DATE_ORIGINE_INDIVISION!.expectedDisplay).toBe('2025-01-15');
  });

  it('coherenceAlerts : pas d\'alerte si IA == avocat (post pré-fill)', () => {
    component.aiData = { dateSeparation: '2025-01-15' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateOrigineIndivision()).toBe('2025-01-15');
    expect(component.coherenceAlerts().DATE_ORIGINE_INDIVISION).toBeUndefined();
  });

  it('coherenceAlerts : showForm=false → vide (anti-bug SF-IA-03-12)', () => {
    component.aiData = { dateSeparation: '2025-01-15' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(exampleResponse()); // GET 200 → showForm=false

    expect(component.showForm()).toBe(false);
    expect(component.coherenceAlerts()).toEqual({});
  });

  it('coherenceAlerts : OCCUPATION_BIEN si IA détecte logement commun + avocat n\'a pas coché', () => {
    component.aiData = { logementCommunDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Pré-fill a coché — l'avocat décoche manuellement.
    expect(component.occupationBienParUnIndivisaire()).toBe(true);
    component.onOccupationChange(false);

    const alert = component.coherenceAlerts().OCCUPATION_BIEN;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('IA');
  });

  it('coherenceAlerts : DATE_ORIGINE F-96 + IA convergent → MULTI', () => {
    const aiData = { dateSeparation: '2025-01-15' } as FamilleExtractedData;
    const procedureChecks: ProcedureCheck[] = [{
      critereCode: 'FA22_DATE_ORIGINE',
      statut: 'VERIFIED',
      raison: 'jugement de divorce',
      expectedValue: '2025-01-15',
    } as ProcedureCheck];

    component.aiData = aiData;
    component.procedureChecks = procedureChecks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onDateOrigineChange('2024-06-01'); // diverge des 2 sources
    const alert = component.coherenceAlerts().DATE_ORIGINE_INDIVISION;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors.length).toBeGreaterThanOrEqual(2);
  });

  // ---------------------------------------------------------------------------
  // Toggle collapse + warnings
  // ---------------------------------------------------------------------------

  it('quotesPartSumWarning : true si somme ≠ 100 (±0.5)', () => {
    component.onQuotesPartRawChange('50, 40');
    expect(component.quotesPartSumWarning()).toBe(true);
    component.onQuotesPartRawChange('50, 50');
    expect(component.quotesPartSumWarning()).toBe(false);
  });

  it('toggleCollapse : inverse l\'état collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });
});
