import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { PartageJudiciaireSectionComponent } from './partage-judiciaire-section.component';
import { PartageJudiciaireResponse } from '../../core/models/partage-judiciaire.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('PartageJudiciaireSectionComponent', () => {
  let component: PartageJudiciaireSectionComponent;
  let fixture: ComponentFixture<PartageJudiciaireSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/partage-judiciaire-analysis';

  function response(overrides: Partial<PartageJudiciaireResponse> = {}): PartageJudiciaireResponse {
    return {
      caseFileId: 'case-1',
      verdictRecevabilite: 'ELEVEE',
      dureeProcedureMois: 9,
      fraisEstimesEur: 5000,
      risqueLicitation: false,
      scoreEligibilite: 90,
      baseJuridique: 'Art. 840 et s. Cciv + 1364 et s. + 1366 CPC',
      formule: 'Tous critères ELEVEE + bien divisible → score 90 → ELEVEE',
      messages: ['PV difficultés établi (1366 CPC) ✓'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        PartageJudiciaireSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(PartageJudiciaireSectionComponent);
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

    expect(component.result()!.scoreEligibilite).toBe(90);
    expect(component.showForm()).toBe(false);
    expect(component.provenancePvDifficultes()).toBeNull();
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

  it('pré-fill IA : pvDifficultesEtabli ← aiData.pvDifficultesEtablisDetected (true)', () => {
    component.aiData = { pvDifficultesEtablisDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.pvDifficultesEtabli()).toBe(true);
    expect(component.provenancePvDifficultes()).toBe('IA');
  });

  it('pré-fill IA : tentativeAmiableEpuiseuee + nombreCoindivisaires + valeurBiens', () => {
    component.aiData = {
      tentativeAmiableEpuiseueeDetected: true,
      nombreCoindivisairesDetecte: 4,
      valeurBiensIndivisionEur: 250000,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.tentativeAmiableEpuiseuee()).toBe(true);
    expect(component.provenanceTentativeAmiable()).toBe('IA');
    expect(component.nombreCoindivisaires()).toBe(4);
    expect(component.provenanceNombreCoindivisaires()).toBe('IA');
    expect(component.valeurEstimeeBiensEur()).toBe(250000);
    expect(component.provenanceValeurBiens()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.pvDifficultesEtabli()).toBeNull();
    expect(component.tentativeAmiableEpuiseuee()).toBeNull();
    expect(component.provenancePvDifficultes()).toBeNull();
  });

  it('pré-fill ignore nombreCoindivisaires < 2 et valeur < 0 (no-op gracieux)', () => {
    component.aiData = {
      nombreCoindivisairesDetecte: 1,
      valeurBiensIndivisionEur: -100,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.nombreCoindivisaires()).toBeNull();
    expect(component.valeurEstimeeBiensEur()).toBeNull();
  });

  it('onPvDifficultesChange efface le badge IA', () => {
    component.aiData = { pvDifficultesEtablisDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenancePvDifficultes()).toBe('IA');
    component.onPvDifficultesChange(false);
    expect(component.pvDifficultesEtabli()).toBe(false);
    expect(component.provenancePvDifficultes()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true seulement quand tous les champs requis sont présents', () => {
    component.pvDifficultesEtabli.set(true);
    component.tentativeAmiableEpuiseuee.set(true);
    component.typeBienIndivision.set('IMMEUBLE_DIVISIBLE');
    component.nombreCoindivisaires.set(3);
    component.desaccordMotive.set(true);
    component.valeurEstimeeBiensEur.set(150000);
    expect(component.formValid()).toBe(true);

    component.nombreCoindivisaires.set(1);
    expect(component.formValid()).toBe(false);

    component.nombreCoindivisaires.set(3);
    component.valeurEstimeeBiensEur.set(-100);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST + résultat + snackbar succès + triggerRefresh', () => {
    component.pvDifficultesEtabli.set(true);
    component.tentativeAmiableEpuiseuee.set(true);
    component.typeBienIndivision.set('IMMEUBLE_DIVISIBLE');
    component.nombreCoindivisaires.set(3);
    component.desaccordMotive.set(true);
    component.valeurEstimeeBiensEur.set(200000);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.pvDifficultesEtabli).toBe(true);
    expect(req.request.body.typeBienIndivision).toBe('IMMEUBLE_DIVISIBLE');
    expect(req.request.body.nombreCoindivisaires).toBe(3);
    expect(req.request.body.valeurEstimeeBiensEur).toBe(200000);
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Partage judiciaire analysé', 'OK', jasmine.any(Object));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.pvDifficultesEtabli.set(true);
    component.tentativeAmiableEpuiseuee.set(true);
    component.typeBienIndivision.set('IMMEUBLE_INDIVISIBLE');
    component.nombreCoindivisaires.set(2);
    component.desaccordMotive.set(true);
    component.valeurEstimeeBiensEur.set(100000);
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

  it('coherenceAlerts.PV_DIFFICULTES présent si IA diverge de saisie', () => {
    component.aiData = { pvDifficultesEtablisDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit `true`. Avocat saisit `false` → divergence IA.
    component.onPvDifficultesChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.PV_DIFFICULTES).toBeDefined();
    expect(alerts.PV_DIFFICULTES!.field).toBe('PV_DIFFICULTES');
    expect(alerts.PV_DIFFICULTES!.source).toBe('IA');
    expect(alerts.PV_DIFFICULTES!.expectedDisplay).toContain('PV établi');
  });

  it('coherenceAlerts.PV_DIFFICULTES absent si IA convergent', () => {
    component.aiData = { pvDifficultesEtablisDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit `true`. Avocat conserve la même valeur.
    expect(component.pvDifficultesEtabli()).toBe(true);
    expect(component.coherenceAlerts().PV_DIFFICULTES).toBeUndefined();
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { pvDifficultesEtablisDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onPvDifficultesChange(false);
    expect(component.coherenceAlerts().PV_DIFFICULTES).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().PV_DIFFICULTES).toBeUndefined();
  });

  it('coherenceAlerts multi-sources F96 + IA convergents → MULTI', () => {
    component.aiData = { pvDifficultesEtablisDetected: true } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'PV difficultés partage judiciaire',
        statut: 'NON_COMPLIANT',
        critereCode: 'PARTAGE_JUDICIAIRE_PV',
        expectedValue: 'OUI',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit `true`. Avocat saisit `false` → divergence IA + F96 même valeur.
    component.onPvDifficultesChange(false);

    const alert = component.coherenceAlerts().PV_DIFFICULTES;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.reason).toContain(' ET ');
  });

  it('coherenceAlerts.TENTATIVE_AMIABLE indépendant de PV_DIFFICULTES', () => {
    component.aiData = {
      pvDifficultesEtablisDetected: true,
      tentativeAmiableEpuiseueeDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Avocat conserve PV `true` mais saisit Tentative `false`.
    component.onTentativeAmiableChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.PV_DIFFICULTES).toBeUndefined();
    expect(alerts.TENTATIVE_AMIABLE).toBeDefined();
    expect(alerts.TENTATIVE_AMIABLE!.source).toBe('IA');
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { pvDifficultesEtablisDetected: true } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.pvDifficultesEtabli()).toBe(true);
    expect(component.provenancePvDifficultes()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onPvDifficultesChange(false);
    expect(component.provenancePvDifficultes()).toBeNull();

    const newAi = { pvDifficultesEtablisDetected: true } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.pvDifficultesEtabli()).toBe(false);
    expect(component.provenancePvDifficultes()).toBeNull();
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

  it('bannerClass mappe verdict → classe CSS attendue', () => {
    expect(component.bannerClass('ELEVEE')).toContain('partage-jud-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('partage-jud-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('partage-jud-banner--critical');
    expect(component.bannerClass(null)).toContain('partage-jud-banner--info');
  });

  it('typeBienLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.typeBienLabel('IMMEUBLE_DIVISIBLE')).toContain('Immeuble divisible');
    expect(component.typeBienLabel('IMMEUBLE_INDIVISIBLE')).toContain('Immeuble indivisible');
    expect(component.typeBienLabel(null)).toBe('');
  });
});
