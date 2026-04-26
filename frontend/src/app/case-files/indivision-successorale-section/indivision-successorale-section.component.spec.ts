import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { IndivisionSuccessoraleSectionComponent } from
  './indivision-successorale-section.component';
import { IndivisionSuccessoraleResponse } from
  '../../core/models/indivision-successorale.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('IndivisionSuccessoraleSectionComponent', () => {
  let component: IndivisionSuccessoraleSectionComponent;
  let fixture: ComponentFixture<IndivisionSuccessoraleSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/indivision-successorale-analysis';

  function response(overrides: Partial<IndivisionSuccessoraleResponse> = {}):
      IndivisionSuccessoraleResponse {
    return {
      caseFileId: 'case-1',
      dateOuvertureSuccession: '2024-01-15',
      typeIndivision: 'INDIVISION_LEGALE',
      nbHeritiers: 3,
      valeurPatrimoineIndivisEur: 200_000,
      valeurBienOccupeEur: 0,
      consentementsTous: true,
      occupationExclusive: false,
      actesAdministrationContestes: false,
      demandePartage: false,
      dureeIndivisionMois: 18,
      verdictGestion: 'HARMONIEUSE',
      dispositifRecommande: 'CONVENTION_INDIVISION_5_ANS',
      indemniteOccupationDue: false,
      indemniteOccupationDueEur: 0,
      fraisGestionEstimesEur: 4_200,
      scoreConflictualite: 5,
      baseJuridique: 'Art. 815 à 832-2 + 1873-1 et s. Cciv',
      formule: 'Régime LEGALE → HARMONIEUSE → CONVENTION_INDIVISION_5_ANS',
      messages: ['Régime actuel : indivision légale.'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        IndivisionSuccessoraleSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(IndivisionSuccessoraleSectionComponent);
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
    req.flush(response({ verdictGestion: 'CONFLICTUELLE', scoreConflictualite: 60 }));

    expect(component.result()!.verdictGestion).toBe('CONFLICTUELLE');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceTypeIndivision()).toBeNull();
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

  it('pré-fill IA : typeIndivision + dateOuvertureSuccession + provenance IA', () => {
    component.aiData = {
      typeIndivisionSuccessoraleDetecte: 'LEGALE',
      dateOuvertureSuccessionDetectee: '2024-01-15',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.typeIndivision()).toBe('INDIVISION_LEGALE');
    expect(component.provenanceTypeIndivision()).toBe('IA');
    expect(component.dateOuvertureSuccession()).toBe('2024-01-15');
    expect(component.provenanceDateOuverture()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.typeIndivision()).toBeNull();
    expect(component.dateOuvertureSuccession()).toBeNull();
    expect(component.provenanceTypeIndivision()).toBeNull();
  });

  it('onTypeIndivisionChange efface provenance IA', () => {
    component.aiData = {
      typeIndivisionSuccessoraleDetecte: 'LEGALE',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceTypeIndivision()).toBe('IA');
    component.onTypeIndivisionChange('INDIVISION_CONVENTIONNELLE');
    expect(component.typeIndivision()).toBe('INDIVISION_CONVENTIONNELLE');
    expect(component.provenanceTypeIndivision()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true avec champs minimaux requis', () => {
    component.typeIndivision.set('INDIVISION_LEGALE');
    component.dateOuvertureSuccession.set('2024-01-15');
    component.nbHeritiers.set(3);
    component.valeurPatrimoineIndivisEur.set(200_000);
    component.consentementsTous.set(true);
    component.occupationExclusive.set(false);
    component.actesAdministrationContestes.set(false);
    component.demandePartage.set(false);

    expect(component.formValid()).toBe(true);
  });

  it('formValid : nbHeritiers < 2 ou > 50 rejette', () => {
    component.typeIndivision.set('INDIVISION_LEGALE');
    component.dateOuvertureSuccession.set('2024-01-15');
    component.valeurPatrimoineIndivisEur.set(200_000);
    component.consentementsTous.set(true);
    component.occupationExclusive.set(false);
    component.actesAdministrationContestes.set(false);
    component.demandePartage.set(false);

    component.nbHeritiers.set(1);
    expect(component.formValid()).toBe(false);
    component.nbHeritiers.set(60);
    expect(component.formValid()).toBe(false);
    component.nbHeritiers.set(3);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : valeurBienOccupeEur > valeurPatrimoineIndivisEur rejette', () => {
    component.typeIndivision.set('INDIVISION_LEGALE');
    component.dateOuvertureSuccession.set('2024-01-15');
    component.nbHeritiers.set(3);
    component.valeurPatrimoineIndivisEur.set(100_000);
    component.consentementsTous.set(true);
    component.occupationExclusive.set(true);
    component.actesAdministrationContestes.set(false);
    component.demandePartage.set(false);

    component.valeurBienOccupeEur.set(150_000);
    expect(component.formValid()).toBe(false);
    component.valeurBienOccupeEur.set(80_000);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : date future rejette', () => {
    component.typeIndivision.set('INDIVISION_LEGALE');
    component.nbHeritiers.set(3);
    component.valeurPatrimoineIndivisEur.set(100_000);
    component.consentementsTous.set(true);
    component.occupationExclusive.set(false);
    component.actesAdministrationContestes.set(false);
    component.demandePartage.set(false);

    component.dateOuvertureSuccession.set('2099-01-01');
    expect(component.formValid()).toBe(false);
    component.dateOuvertureSuccession.set('2024-01-15');
    expect(component.formValid()).toBe(true);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST envoie le body attendu + résultat + snackbar succès', () => {
    component.typeIndivision.set('INDIVISION_LEGALE');
    component.dateOuvertureSuccession.set('2024-01-15');
    component.nbHeritiers.set(3);
    component.valeurPatrimoineIndivisEur.set(200_000);
    component.valeurBienOccupeEur.set(0);
    component.consentementsTous.set(true);
    component.occupationExclusive.set(false);
    component.actesAdministrationContestes.set(false);
    component.demandePartage.set(false);

    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.typeIndivision).toBe('INDIVISION_LEGALE');
    expect(req.request.body.dateOuvertureSuccession).toBe('2024-01-15');
    expect(req.request.body.nbHeritiers).toBe(3);
    expect(req.request.body.valeurPatrimoineIndivisEur).toBe(200_000);
    expect(req.request.body.consentementsTous).toBe(true);
    expect(req.request.body.demandePartage).toBe(false);
    req.flush(response());

    expect(component.result()!.verdictGestion).toBe('HARMONIEUSE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Indivision successorale analysée', 'OK', jasmine.any(Object));
  });

  it("calculate() ignoré si form invalide (pas d'appel HTTP)", () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.typeIndivision.set('INDIVISION_LEGALE');
    component.dateOuvertureSuccession.set('2024-01-15');
    component.nbHeritiers.set(3);
    component.valeurPatrimoineIndivisEur.set(200_000);
    component.consentementsTous.set(true);
    component.occupationExclusive.set(false);
    component.actesAdministrationContestes.set(false);
    component.demandePartage.set(false);

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

  it('coherenceAlerts.TYPE_INDIVISION présente si IA divergente de la saisie', () => {
    component.aiData = {
      typeIndivisionSuccessoraleDetecte: 'INDIVISION_LEGALE',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Pré-fill mit LEGALE puis avocat passe à CONVENTIONNELLE → divergence.
    component.onTypeIndivisionChange('INDIVISION_CONVENTIONNELLE');

    const alerts = component.coherenceAlerts();
    expect(alerts.TYPE_INDIVISION).toBeDefined();
    expect(alerts.TYPE_INDIVISION!.field).toBe('TYPE_INDIVISION');
    expect(alerts.TYPE_INDIVISION!.source).toBe('IA');
  });

  it('coherenceAlerts.DATE_OUVERTURE_SUCCESSION multi-sources F96 + IA → MULTI', () => {
    component.aiData = {
      dateOuvertureSuccessionDetectee: '2024-01-15',
    } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: "Date d'ouverture",
        statut: 'NON_COMPLIANT',
        critereCode: 'INDIVISION_DATE_OUVERTURE',
        expectedValue: '2024-01-15',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit 2024-01-15 puis avocat saisit autre date.
    component.onDateOuvertureChange('2024-06-01');

    const alert = component.coherenceAlerts().DATE_OUVERTURE_SUCCESSION;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = {
      typeIndivisionSuccessoraleDetecte: 'INDIVISION_LEGALE',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onTypeIndivisionChange('INDIVISION_CONVENTIONNELLE');
    expect(component.coherenceAlerts().TYPE_INDIVISION).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().TYPE_INDIVISION).toBeUndefined();
  });

  // ============================================================
  // ngOnChanges
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { typeIndivisionSuccessoraleDetecte: 'LEGALE' } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.typeIndivision()).toBe('INDIVISION_LEGALE');
    expect(component.provenanceTypeIndivision()).toBe('IA');
  });

  it('ngOnChanges(aiData) post-saisie ne réécrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onTypeIndivisionChange('MAINTIEN_FORCE');
    expect(component.provenanceTypeIndivision()).toBeNull();

    const newAi = { typeIndivisionSuccessoraleDetecte: 'LEGALE' } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.typeIndivision()).toBe('MAINTIEN_FORCE');
    expect(component.provenanceTypeIndivision()).toBeNull();
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

  it('verdictBannerClass : BLOCAGE → critical, CONFLICTUELLE → warn, HARMONIEUSE → info', () => {
    expect(component.verdictBannerClass('BLOCAGE')).toContain('critical');
    expect(component.verdictBannerClass('CONFLICTUELLE')).toContain('warn');
    expect(component.verdictBannerClass('HARMONIEUSE')).toContain('info');
    expect(component.verdictBannerClass(null)).toContain('info');
  });

  it('verdictChipClass aligne palette header', () => {
    expect(component.verdictChipClass('BLOCAGE')).toContain('critical');
    expect(component.verdictChipClass('CONFLICTUELLE')).toContain('warn');
    expect(component.verdictChipClass('HARMONIEUSE')).toContain('info');
  });

  it('verdictIcon : BLOCAGE=gpp_bad, CONFLICTUELLE=warning, HARMONIEUSE=verified', () => {
    expect(component.verdictIcon('BLOCAGE')).toBe('gpp_bad');
    expect(component.verdictIcon('CONFLICTUELLE')).toBe('warning');
    expect(component.verdictIcon('HARMONIEUSE')).toBe('verified');
  });

  it('typeLabel + verdictLabel + dispositifLabel couvrent les enums', () => {
    expect(component.typeLabel('INDIVISION_LEGALE')).toContain('légale');
    expect(component.typeLabel('INDIVISION_CONVENTIONNELLE')).toContain('conventionnelle');
    expect(component.typeLabel('MAINTIEN_FORCE')).toContain('Maintien');
    expect(component.typeLabel(null)).toBe('');

    expect(component.verdictLabel('HARMONIEUSE')).toContain('harmonieuse');
    expect(component.verdictLabel('CONFLICTUELLE')).toContain('conflictuelle');
    expect(component.verdictLabel('BLOCAGE')).toContain('Blocage');

    expect(component.dispositifLabel('CONVENTION_INDIVISION_5_ANS')).toContain('Convention');
    expect(component.dispositifLabel('PARTAGE_AMIABLE')).toContain('amiable');
    expect(component.dispositifLabel('PARTAGE_JUDICIAIRE')).toContain('judiciaire');
    expect(component.dispositifLabel('MEDIATION_FAMILIALE')).toContain('Médiation');
    expect(component.dispositifLabel(null)).toBe('');
  });
});
