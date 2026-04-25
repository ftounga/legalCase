import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { RecompensesSectionComponent } from './recompenses-section.component';
import { RecompensesResponse } from '../../core/models/recompenses.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';

describe('RecompensesSectionComponent', () => {
  let component: RecompensesSectionComponent;
  let fixture: ComponentFixture<RecompensesSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/recompenses';

  function exampleResponse(): RecompensesResponse {
    return {
      caseFileId: 'case-1',
      regimeMatrimonial: 'COMMUNAUTE_LEGALE',
      recompenses: [
        {
          operationId: 'op-1',
          type: 'DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE',
          natureBien: 'AMELIORATION_BIEN_PROPRE',
          regleApplicable: 'PROFIT_SUBSISTANT_PLUS_FORTE',
          depenseFaiteEur: 50000,
          profitSubsistantEur: 150000,
          montantRecompenseEur: 150000,
          directionRecompense: 'PROPRE_DOIT_COMMUNAUTE',
          baseJuridiqueOperation: 'Art. 1469 al. 3 Cciv',
          formule: 'max(50000 ; 150000) = 150000',
          description: 'Travaux d’agrandissement maison',
        },
      ],
      totalRecompensesDuesParCommunauteEur: 0,
      totalRecompensesDuesParEpouxEur: 150000,
      soldeNetPourEpouxEur: -150000,
      baseJuridiqueGenerale: 'Art. 1437 et 1469 Cciv',
      messages: ['Récompenses calculées sur la base des art. 1437 et 1469 Cciv'],
      country: 'FRANCE',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        RecompensesSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RecompensesSectionComponent);
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

  it('mount FRANCE → GET 200 → restore régime + result + masque form', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(exampleResponse());

    expect(component.result()!.recompenses[0].montantRecompenseEur).toBe(150000);
    expect(component.regimeMatrimonial()).toBe('COMMUNAUTE_LEGALE');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceRegime()).toBeNull(); // valeurs persistées ≠ IA
  });

  it('mount BELGIQUE → pas de GET, form masqué (gate bannière)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.showForm()).toBe(true); // état initial — la bannière masque côté template
  });

  // ---------------------------------------------------------------------------
  // formValid + opérations
  // ---------------------------------------------------------------------------

  it('formValid : exige un régime sélectionné', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.formValid()).toBe(false);
    component.regimeMatrimonial.set('COMMUNAUTE_LEGALE');
    expect(component.formValid()).toBe(true);
  });

  it('addOperation / removeOperation : taille de la liste mise à jour', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.operations().length).toBe(0);
    component.addOperation();
    component.addOperation();
    expect(component.operations().length).toBe(2);
    const id = component.operations()[0].id;
    component.removeOperation(id);
    expect(component.operations().length).toBe(1);
  });

  // ---------------------------------------------------------------------------
  // Calculate
  // ---------------------------------------------------------------------------

  it('calculate : POST émis avec régime + opérations + triggerRefresh + showForm=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.regimeMatrimonial.set('COMMUNAUTE_LEGALE');
    component.addOperation();
    const opId = component.operations()[0].id;
    component.onOperationFieldChange(opId, 'montantDepenseEur', 50000);
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.regimeMatrimonial).toBe('COMMUNAUTE_LEGALE');
    expect(req.request.body.operations.length).toBe(1);
    expect(req.request.body.operations[0].montantDepenseEur).toBe(50000);
    req.flush(exampleResponse());

    expect(component.showForm()).toBe(false);
    expect(component.result()).not.toBeNull();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate : erreur 400 → snackBar message', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.regimeMatrimonial.set('COMMUNAUTE_LEGALE');
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(
      { message: 'Régime invalide' },
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
  // Pré-fill IA + provenance
  // ---------------------------------------------------------------------------

  it('prefillFromAi : regimeMatrimonialDetecte → provenanceRegime = IA', () => {
    component.aiData = { regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.regimeMatrimonial()).toBe('COMMUNAUTE_LEGALE');
    expect(component.provenanceRegime()).toBe('IA');
  });

  it('prefillFromAi : SEPARATION_BIENS → skip (no-op gracieux)', () => {
    component.aiData = { regimeMatrimonialDetecte: 'SEPARATION_BIENS' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.regimeMatrimonial()).toBeNull();
    expect(component.provenanceRegime()).toBeNull();
  });

  it('onRegimeChange manuel : provenanceRegime → null', () => {
    component.aiData = { regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceRegime()).toBe('IA');
    component.onRegimeChange('COMMUNAUTE_UNIVERSELLE');
    expect(component.provenanceRegime()).toBeNull();
    expect(component.regimeMatrimonial()).toBe('COMMUNAUTE_UNIVERSELLE');
  });

  it('ngOnChanges : aiData arrive après mount → re-prefill si form encore vide', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.regimeMatrimonial()).toBeNull();

    component.aiData = { regimeMatrimonialDetecte: 'PARTICIPATION_AUX_ACQUETS' } as FamilleExtractedData;
    component.ngOnChanges({
      aiData: new SimpleChange(null, component.aiData, false),
    });

    expect(component.regimeMatrimonial()).toBe('PARTICIPATION_AUX_ACQUETS');
    expect(component.provenanceRegime()).toBe('IA');
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts : divergence IA vs avocat → alerte REGIME_MATRIMONIAL', () => {
    component.aiData = { regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Avocat change manuellement vers un autre régime → divergence.
    component.onRegimeChange('PARTICIPATION_AUX_ACQUETS');

    const alerts = component.coherenceAlerts();
    expect(alerts.REGIME_MATRIMONIAL).toBeDefined();
    expect(alerts.REGIME_MATRIMONIAL!.contributors).toContain('IA');
  });

  it('coherenceAlerts : pas d’alerte si IA == avocat', () => {
    component.aiData = { regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.regimeMatrimonial()).toBe('COMMUNAUTE_LEGALE'); // pré-fill IA
    expect(component.coherenceAlerts().REGIME_MATRIMONIAL).toBeUndefined();
  });

  it('coherenceAlerts : showForm=false → vide (anti-bug SF-IA-03-12)', () => {
    component.aiData = { regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(exampleResponse()); // GET 200 → showForm = false

    expect(component.showForm()).toBe(false);
    expect(component.coherenceAlerts()).toEqual({});
  });

  it('coherenceAlerts : IA détecte SEPARATION_BIENS + avocat saisit régime → REGIME_APPLICABILITE', () => {
    component.aiData = { regimeMatrimonialDetecte: 'SEPARATION_BIENS' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.regimeMatrimonial.set('COMMUNAUTE_LEGALE'); // avocat saisit malgré la séparation
    const alert = component.coherenceAlerts().REGIME_APPLICABILITE;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('IA');
  });

  it('coherenceAlerts : F-96 + IA convergent → MULTI', () => {
    const aiData = { regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE' } as FamilleExtractedData;
    const procedureChecks: ProcedureCheck[] = [{
      critereCode: 'FA15_REGIME_MATRIMONIAL',
      statut: 'VERIFIED',
      raison: 'contrat de mariage scanné',
      expectedValue: 'COMMUNAUTE_LEGALE',
    } as ProcedureCheck];

    component.aiData = aiData;
    component.procedureChecks = procedureChecks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onRegimeChange('COMMUNAUTE_UNIVERSELLE'); // diverge des 2 sources
    const alert = component.coherenceAlerts().REGIME_MATRIMONIAL;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors.length).toBeGreaterThanOrEqual(2);
  });

  // ---------------------------------------------------------------------------
  // Toggle collapse
  // ---------------------------------------------------------------------------

  it('toggleCollapse : inverse l’état collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });
});
