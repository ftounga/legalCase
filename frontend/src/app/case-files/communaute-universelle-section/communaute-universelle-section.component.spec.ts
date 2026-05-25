import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { CommunauteUniverselleSectionComponent } from './communaute-universelle-section.component';
import { CommunauteUniverselleResponse } from '../../core/models/communaute-universelle.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('CommunauteUniverselleSectionComponent', () => {
  let component: CommunauteUniverselleSectionComponent;
  let fixture: ComponentFixture<CommunauteUniverselleSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/communaute-universelle-analysis';

  function response(overrides: Partial<CommunauteUniverselleResponse> = {}): CommunauteUniverselleResponse {
    return {
      caseFileId: 'case-1',
      dispositifAnalyse: 'VALIDITE_CONVENTION',
      verdictValidite: 'VALIDE',
      actionRetranchementPossible: false,
      partAttributionConjointPct: 100,
      valeurAttributionEur: 0,
      scoreValidite: 90,
      risquesIdentifies: [],
      baseJuridique: 'Art. 1526 Cciv + 1527 al. 2 Cciv',
      formule: 'Tous critères présents → score 90 → VALIDE',
      messages: ['Contrat notarié confirmé (1394 Cciv) ✓'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        CommunauteUniverselleSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CommunauteUniverselleSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ============================================================
  // Gate pays + init
  // ============================================================

  it('FRANCE → isFrance() true, GET appelé au ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('BELGIQUE → isFrance() false, aucun appel HTTP au ngOnInit (gate pays)', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isFrance()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('charge le résultat existant si GET 200 (mode résultat hydraté)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());

    expect(component.result()!.scoreValidite).toBe(90);
    expect(component.showForm()).toBe(false);
    expect(component.provenanceContratNotarie()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // Switch dispositif
  // ============================================================

  it('switch dispositif LIQUIDATION_DECES réinitialise les champs VALIDITE_CONVENTION', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.dispositifAnalyse.set('VALIDITE_CONVENTION');
    component.inscriptionEtatCivil.set(true);
    component.consentementLibreDesEpoux.set(true);
    component.respectReserveHereditaire.set(true);

    component.onDispositifChange('LIQUIDATION_DECES');

    expect(component.dispositifAnalyse()).toBe('LIQUIDATION_DECES');
    expect(component.inscriptionEtatCivil()).toBeNull();
    expect(component.consentementLibreDesEpoux()).toBeNull();
    expect(component.respectReserveHereditaire()).toBeNull();
  });

  it('switch dispositif VALIDITE_CONVENTION réinitialise les champs LIQUIDATION_DECES', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.dispositifAnalyse.set('LIQUIDATION_DECES');
    component.clauseAttributionIntegrale.set(true);
    component.enfantsNonCommuns.set(true);
    component.valeurCommunauteEur.set(800000);
    component.provenanceClauseAttributionIntegrale.set('IA');

    component.onDispositifChange('VALIDITE_CONVENTION');

    expect(component.dispositifAnalyse()).toBe('VALIDITE_CONVENTION');
    expect(component.clauseAttributionIntegrale()).toBeNull();
    expect(component.enfantsNonCommuns()).toBeNull();
    expect(component.valeurCommunauteEur()).toBeNull();
    expect(component.provenanceClauseAttributionIntegrale()).toBeNull();
  });

  // ============================================================
  // Pré-fill IA
  // ============================================================

  it('pré-fill IA : contratNotarie ← aiData.contratNotarieDetected', () => {
    component.aiData = { contratNotarieDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.contratNotarie()).toBe(true);
    expect(component.provenanceContratNotarie()).toBe('IA');
  });

  it('pré-fill IA : enfantsNonCommuns + clauseAttributionIntegrale + valeurCommunaute', () => {
    component.aiData = {
      enfantsNonCommunsDetected: true,
      clauseAttributionIntegraleDetected: true,
      valeurCommunauteEurDetectee: 500000,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.enfantsNonCommuns()).toBe(true);
    expect(component.provenanceEnfantsNonCommuns()).toBe('IA');
    expect(component.clauseAttributionIntegrale()).toBe(true);
    expect(component.provenanceClauseAttributionIntegrale()).toBe('IA');
    expect(component.valeurCommunauteEur()).toBe(500000);
    expect(component.provenanceValeurCommunaute()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.contratNotarie()).toBeNull();
    expect(component.enfantsNonCommuns()).toBeNull();
    expect(component.provenanceContratNotarie()).toBeNull();
  });

  it('pré-fill ignore valeurCommunauteEur < 0 (no-op gracieux)', () => {
    component.aiData = {
      valeurCommunauteEurDetectee: -100,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.valeurCommunauteEur()).toBeNull();
  });

  it('onContratNotarieChange efface le badge IA', () => {
    component.aiData = { contratNotarieDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceContratNotarie()).toBe('IA');
    component.onContratNotarieChange(false);
    expect(component.contratNotarie()).toBe(false);
    expect(component.provenanceContratNotarie()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true VALIDITE_CONVENTION quand tous les champs requis sont saisis', () => {
    component.dispositifAnalyse.set('VALIDITE_CONVENTION');
    component.contratNotarie.set(true);
    component.inscriptionEtatCivil.set(true);
    component.consentementLibreDesEpoux.set(true);
    component.respectReserveHereditaire.set(true);
    expect(component.formValid()).toBe(true);

    component.consentementLibreDesEpoux.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid true LIQUIDATION_DECES quand tous les champs liquidation sont saisis', () => {
    component.dispositifAnalyse.set('LIQUIDATION_DECES');
    component.contratNotarie.set(true);
    component.clauseAttributionIntegrale.set(true);
    component.enfantsNonCommuns.set(true);
    component.valeurCommunauteEur.set(500000);
    expect(component.formValid()).toBe(true);

    component.valeurCommunauteEur.set(-100);
    expect(component.formValid()).toBe(false);

    component.valeurCommunauteEur.set(500000);
    component.enfantsNonCommuns.set(null);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() VALIDITE_CONVENTION POST + résultat + snackbar succès', () => {
    component.dispositifAnalyse.set('VALIDITE_CONVENTION');
    component.contratNotarie.set(true);
    component.inscriptionEtatCivil.set(true);
    component.consentementLibreDesEpoux.set(true);
    component.respectReserveHereditaire.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dispositifAnalyse).toBe('VALIDITE_CONVENTION');
    expect(req.request.body.contratNotarie).toBe(true);
    expect(req.request.body.inscriptionEtatCivil).toBe(true);
    // Champs liquidation doivent rester null pour ce dispositif
    expect(req.request.body.clauseAttributionIntegrale).toBeNull();
    expect(req.request.body.enfantsNonCommuns).toBeNull();
    expect(req.request.body.valeurCommunauteEur).toBeNull();
    req.flush(response());

    expect(component.result()!.verdictValidite).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Communauté universelle analysée', 'OK', jasmine.any(Object));
  });

  it('calculate() LIQUIDATION_DECES envoie les champs liquidation et pas les champs validité', () => {
    component.dispositifAnalyse.set('LIQUIDATION_DECES');
    component.contratNotarie.set(true);
    component.clauseAttributionIntegrale.set(true);
    component.enfantsNonCommuns.set(true);
    component.valeurCommunauteEur.set(800000);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dispositifAnalyse).toBe('LIQUIDATION_DECES');
    expect(req.request.body.clauseAttributionIntegrale).toBe(true);
    expect(req.request.body.enfantsNonCommuns).toBe(true);
    expect(req.request.body.valeurCommunauteEur).toBe(800000);
    expect(req.request.body.inscriptionEtatCivil).toBeNull();
    expect(req.request.body.consentementLibreDesEpoux).toBeNull();
    expect(req.request.body.respectReserveHereditaire).toBeNull();
    req.flush(response({
      dispositifAnalyse: 'LIQUIDATION_DECES',
      actionRetranchementPossible: true,
      partAttributionConjointPct: 100,
      valeurAttributionEur: 800000,
    }));

    expect(component.result()!.actionRetranchementPossible).toBe(true);
    expect(component.result()!.valeurAttributionEur).toBe(800000);
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.dispositifAnalyse.set('VALIDITE_CONVENTION');
    component.contratNotarie.set(false);
    component.inscriptionEtatCivil.set(true);
    component.consentementLibreDesEpoux.set(true);
    component.respectReserveHereditaire.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  // ============================================================
  // F-IA-03 alertes de cohérence
  // ============================================================

  it('coherenceAlerts.CONTRAT_NOTARIE présent si IA diverge de saisie', () => {
    component.aiData = { contratNotarieDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit `true`. Avocat saisit `false` → divergence IA.
    component.onContratNotarieChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.CONTRAT_NOTARIE).toBeDefined();
    expect(alerts.CONTRAT_NOTARIE!.field).toBe('CONTRAT_NOTARIE');
    expect(alerts.CONTRAT_NOTARIE!.source).toBe('IA');
    expect(alerts.CONTRAT_NOTARIE!.expectedDisplay).toContain('Contrat notarié');
  });

  it('coherenceAlerts.CONTRAT_NOTARIE absent si IA convergent', () => {
    component.aiData = { contratNotarieDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.contratNotarie()).toBe(true);
    expect(component.coherenceAlerts().CONTRAT_NOTARIE).toBeUndefined();
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { contratNotarieDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onContratNotarieChange(false);
    expect(component.coherenceAlerts().CONTRAT_NOTARIE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().CONTRAT_NOTARIE).toBeUndefined();
  });

  it('coherenceAlerts multi-sources F96 + IA convergents → MULTI', () => {
    component.aiData = { contratNotarieDetected: true } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Contrat de mariage notarié',
        statut: 'NON_COMPLIANT',
        critereCode: 'COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE',
        expectedValue: 'OUI',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onContratNotarieChange(false);

    const alert = component.coherenceAlerts().CONTRAT_NOTARIE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.reason).toContain(' ET ');
  });

  it('coherenceAlerts.ENFANTS_NON_COMMUNS indépendant de CONTRAT_NOTARIE', () => {
    component.aiData = {
      contratNotarieDetected: true,
      enfantsNonCommunsDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Avocat conserve contratNotarie `true` mais saisit enfants `false`.
    component.onEnfantsNonCommunsChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.CONTRAT_NOTARIE).toBeUndefined();
    expect(alerts.ENFANTS_NON_COMMUNS).toBeDefined();
    expect(alerts.ENFANTS_NON_COMMUNS!.source).toBe('IA');
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { contratNotarieDetected: true } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.contratNotarie()).toBe(true);
    expect(component.provenanceContratNotarie()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onContratNotarieChange(false);
    expect(component.provenanceContratNotarie()).toBeNull();

    const newAi = { contratNotarieDetected: true } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.contratNotarie()).toBe(false);
    expect(component.provenanceContratNotarie()).toBeNull();
  });

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode ré-affiche le form', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('bannerClass mappe verdict + actionRetranchement → classe CSS attendue', () => {
    expect(component.bannerClass('VALIDE', false)).toContain('comm-univ-banner--info');
    expect(component.bannerClass('VALIDE', true)).toContain('comm-univ-banner--warning');
    expect(component.bannerClass('CONTESTABLE', false)).toContain('comm-univ-banner--warning');
    expect(component.bannerClass('NUL', false)).toContain('comm-univ-banner--critical');
    expect(component.bannerClass(null)).toContain('comm-univ-banner--info');
  });

  it('dispositifLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.dispositifLabel('VALIDITE_CONVENTION')).toContain('Validité de la convention');
    expect(component.dispositifLabel('LIQUIDATION_DECES')).toContain('Liquidation au décès');
    expect(component.dispositifLabel(null)).toBe('');
  });
});
