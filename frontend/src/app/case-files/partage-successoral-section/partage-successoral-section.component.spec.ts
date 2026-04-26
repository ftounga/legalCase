import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { PartageSuccessoralSectionComponent } from './partage-successoral-section.component';
import { PartageSuccessoralResponse } from '../../core/models/partage-successoral.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('PartageSuccessoralSectionComponent', () => {
  let component: PartageSuccessoralSectionComponent;
  let fixture: ComponentFixture<PartageSuccessoralSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/partage-successoral-analysis';

  function response(overrides: Partial<PartageSuccessoralResponse> = {}): PartageSuccessoralResponse {
    return {
      caseFileId: 'case-1',
      verdictRecevabilite: 'ELEVEE',
      modeRecommande: 'PARTAGE_AMIABLE',
      basculeMode: false,
      scoreEligibilite: 90,
      delaiInstructionMois: 3,
      fraisEstimesPct: 0.01,
      fraisEstimesEur: 0,
      risqueLicitation: false,
      baseJuridique: 'Art. 815-840 Cciv + 1364 CPC',
      formule: 'Mode demandé PARTAGE_AMIABLE → recommandé PARTAGE_AMIABLE',
      messages: ['Partage amiable simple.'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        PartageSuccessoralSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(PartageSuccessoralSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

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
    req.flush(response({ verdictRecevabilite: 'MOYENNE', basculeMode: true, modeRecommande: 'PARTAGE_JUDICIAIRE' }));

    expect(component.result()!.verdictRecevabilite).toBe('MOYENNE');
    expect(component.result()!.basculeMode).toBe(true);
    expect(component.showForm()).toBe(false);
    expect(component.provenanceMode()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // Pré-fill IA
  // ============================================================

  it('pré-fill IA : modePartageDemande ← aiData + provenance IA', () => {
    component.aiData = { modePartageDemandeDetecte: 'AMIABLE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.modePartageDemande()).toBe('PARTAGE_AMIABLE');
    expect(component.provenanceMode()).toBe('IA');
  });

  it('pré-fill IA : nombreCoheritiers + dateDeces (avec valeur ≥ 2)', () => {
    component.aiData = {
      modePartageDemandeDetecte: 'JUDICIAIRE',
      nombreCoheritiersDetecte: 4,
      dateDecesDetectee: '2024-09-01',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.modePartageDemande()).toBe('PARTAGE_JUDICIAIRE');
    expect(component.provenanceMode()).toBe('IA');
    expect(component.nombreCoheritiers()).toBe(4);
    expect(component.provenanceNombreCoheritiers()).toBe('IA');
    expect(component.dateDeces()).toBe('2024-09-01');
    expect(component.provenanceDateDeces()).toBe('IA');
  });

  it('pré-fill IA : fallback dateDeces ← dateOuvertureSuccessionDetectee si absent', () => {
    component.aiData = {
      dateOuvertureSuccessionDetectee: '2023-12-12',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateDeces()).toBe('2023-12-12');
    expect(component.provenanceDateDeces()).toBe('IA');
  });

  it('pré-fill : nombreCoheritiers ignoré si < 2', () => {
    component.aiData = {
      nombreCoheritiersDetecte: 1,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.nombreCoheritiers()).toBeNull();
    expect(component.provenanceNombreCoheritiers()).toBeNull();
  });

  it('pré-fill sans aiData → aucun pré-remplissage', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.modePartageDemande()).toBeNull();
    expect(component.nombreCoheritiers()).toBeNull();
    expect(component.dateDeces()).toBeNull();
    expect(component.provenanceMode()).toBeNull();
  });

  it('onModePartageChange efface provenance IA', () => {
    component.aiData = { modePartageDemandeDetecte: 'AMIABLE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceMode()).toBe('IA');
    component.onModePartageChange('PARTAGE_JUDICIAIRE');
    expect(component.modePartageDemande()).toBe('PARTAGE_JUDICIAIRE');
    expect(component.provenanceMode()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid : tous champs requis remplis → true', () => {
    component.modePartageDemande.set('PARTAGE_AMIABLE');
    component.nombreCoheritiers.set(3);
    component.consentementsTous.set(true);
    component.presenceImmeubles.set(false);
    component.accordsValuation.set(true);
    component.desaccordPersistant.set(false);
    component.dateDeces.set('2024-06-15');
    expect(component.formValid()).toBe(true);
  });

  it('formValid : nombreCoheritiers < 2 rejette', () => {
    component.modePartageDemande.set('PARTAGE_AMIABLE');
    component.nombreCoheritiers.set(1);
    component.consentementsTous.set(true);
    component.presenceImmeubles.set(false);
    component.accordsValuation.set(true);
    component.desaccordPersistant.set(false);
    component.dateDeces.set('2024-06-15');
    expect(component.formValid()).toBe(false);

    component.nombreCoheritiers.set(2);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : mode null rejette', () => {
    component.nombreCoheritiers.set(3);
    component.consentementsTous.set(true);
    component.presenceImmeubles.set(false);
    component.accordsValuation.set(true);
    component.desaccordPersistant.set(false);
    component.dateDeces.set('2024-06-15');
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST envoie le body attendu + résultat + snackbar succès', () => {
    component.modePartageDemande.set('PARTAGE_AMIABLE');
    component.nombreCoheritiers.set(3);
    component.consentementsTous.set(true);
    component.presenceImmeubles.set(false);
    component.accordsValuation.set(true);
    component.desaccordPersistant.set(false);
    component.dateDeces.set('2024-06-15');
    component.valeurMasseEur.set(350000);

    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.modePartageDemande).toBe('PARTAGE_AMIABLE');
    expect(req.request.body.nombreCoheritiers).toBe(3);
    expect(req.request.body.consentementsTous).toBe(true);
    expect(req.request.body.presenceImmeubles).toBe(false);
    expect(req.request.body.accordsValuation).toBe(true);
    expect(req.request.body.desaccordPersistant).toBe(false);
    expect(req.request.body.dateDeces).toBe('2024-06-15');
    expect(req.request.body.valeurMasseEur).toBe(350000);
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Partage successoral analysé', 'OK', jasmine.any(Object));
  });

  it("calculate() ignoré si form invalide (pas d'appel HTTP)", () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.modePartageDemande.set('PARTAGE_AMIABLE');
    component.nombreCoheritiers.set(3);
    component.consentementsTous.set(true);
    component.presenceImmeubles.set(false);
    component.accordsValuation.set(true);
    component.desaccordPersistant.set(false);
    component.dateDeces.set('2024-06-15');

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

  it('coherenceAlerts.MODE_PARTAGE présente si IA divergente de la saisie', () => {
    component.aiData = { modePartageDemandeDetecte: 'PARTAGE_AMIABLE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Pré-fill mit AMIABLE puis avocat passe à JUDICIAIRE → divergence.
    component.onModePartageChange('PARTAGE_JUDICIAIRE');

    const alerts = component.coherenceAlerts();
    expect(alerts.MODE_PARTAGE).toBeDefined();
    expect(alerts.MODE_PARTAGE!.field).toBe('MODE_PARTAGE');
    expect(alerts.MODE_PARTAGE!.source).toBe('IA');
  });

  it('coherenceAlerts.PRESENCE_IMMEUBLES F96 → alerte', () => {
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: "Immeubles présents",
        statut: 'NON_COMPLIANT',
        critereCode: 'PARTAGE_PRESENCE_IMMEUBLES',
        expectedValue: 'OUI',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // L'avocat dit non, le F-96 dit oui → divergence.
    component.onPresenceImmeublesChange(false);

    const alert = component.coherenceAlerts().PRESENCE_IMMEUBLES;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
    expect(alert!.contributors).toContain('F96');
  });

  it('coherenceAlerts.CONSENTEMENTS multi-sources F96 + QUESTION_IA → MULTI', () => {
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Tous consentent',
        statut: 'NON_COMPLIANT',
        critereCode: 'PARTAGE_CONSENTEMENTS',
        expectedValue: 'OUI',
      },
    ];
    component.aiQuestions = [
      {
        id: 'q-1',
        questionText: 'Tous les héritiers consentent ?',
        critereCode: 'PARTAGE_CONSENTEMENTS',
        expectedValue: 'OUI',
        answerText: 'Oui',
      } as any,
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onConsentementsTousChange(false);

    const alert = component.coherenceAlerts().CONSENTEMENTS;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('QUESTION_IA');
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { modePartageDemandeDetecte: 'AMIABLE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onModePartageChange('PARTAGE_JUDICIAIRE');
    expect(component.coherenceAlerts().MODE_PARTAGE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().MODE_PARTAGE).toBeUndefined();
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { modePartageDemandeDetecte: 'JUDICIAIRE' } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.modePartageDemande()).toBe('PARTAGE_JUDICIAIRE');
    expect(component.provenanceMode()).toBe('IA');
  });

  it('ngOnChanges(aiData) post-saisie ne réécrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onModePartageChange('PARTAGE_PARTIEL');
    expect(component.provenanceMode()).toBeNull();

    const newAi = { modePartageDemandeDetecte: 'AMIABLE' } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.modePartageDemande()).toBe('PARTAGE_PARTIEL');
    expect(component.provenanceMode()).toBeNull();
  });

  // ============================================================
  // Helpers UI
  // ============================================================

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

  it('verdictBannerClass : FAIBLE → critical, MOYENNE → warn, ELEVEE → info', () => {
    expect(component.verdictBannerClass('FAIBLE')).toContain('critical');
    expect(component.verdictBannerClass('MOYENNE')).toContain('warn');
    expect(component.verdictBannerClass('ELEVEE')).toContain('info');
    expect(component.verdictBannerClass(null)).toContain('info');
  });

  it('verdictChipClass aligne palette header', () => {
    expect(component.verdictChipClass('FAIBLE')).toContain('critical');
    expect(component.verdictChipClass('MOYENNE')).toContain('warn');
    expect(component.verdictChipClass('ELEVEE')).toContain('info');
  });

  it('verdictIcon : FAIBLE=gpp_bad, MOYENNE=warning, ELEVEE=verified', () => {
    expect(component.verdictIcon('FAIBLE')).toBe('gpp_bad');
    expect(component.verdictIcon('MOYENNE')).toBe('warning');
    expect(component.verdictIcon('ELEVEE')).toBe('verified');
  });

  it('modeLabel + verdictLabel couvrent les enums', () => {
    expect(component.modeLabel('PARTAGE_AMIABLE')).toContain('amiable');
    expect(component.modeLabel('PARTAGE_JUDICIAIRE')).toContain('judiciaire');
    expect(component.modeLabel('PARTAGE_PARTIEL')).toContain('partiel');
    expect(component.modeLabel(null)).toBe('');

    expect(component.verdictLabel('ELEVEE')).toContain('élevée');
    expect(component.verdictLabel('MOYENNE')).toContain('moyenne');
    expect(component.verdictLabel('FAIBLE')).toContain('faible');
    expect(component.verdictLabel(null)).toBe('');
  });
});
