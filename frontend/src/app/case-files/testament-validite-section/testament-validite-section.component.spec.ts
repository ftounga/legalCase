import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { TestamentValiditeSectionComponent } from './testament-validite-section.component';
import { TestamentValiditeResponse } from '../../core/models/testament-validite.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('TestamentValiditeSectionComponent', () => {
  let component: TestamentValiditeSectionComponent;
  let fixture: ComponentFixture<TestamentValiditeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/testament-validite-analysis';

  function response(overrides: Partial<TestamentValiditeResponse> = {}): TestamentValiditeResponse {
    return {
      caseFileId: 'case-1',
      formeTestament: 'TESTAMENT_OLOGRAPHE',
      verdictValidite: 'VALIDE',
      vicesIdentifies: [],
      actionEnReductionPossible: false,
      delaiContestationAns: 5,
      scoreEligibilite: 100,
      baseJuridique: 'Art. 967, 970, 971-975, 976-980, 901-911, 1035-1038, 920 et s. Cciv',
      formule: 'Forme TESTAMENT_OLOGRAPHE + verdict VALIDE + 0 vice → score 100',
      messages: ['Testament olographe valide.'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        TestamentValiditeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(TestamentValiditeSectionComponent);
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
    req.flush(response({ verdictValidite: 'CONTESTABLE', scoreEligibilite: 70 }));

    expect(component.result()!.verdictValidite).toBe('CONTESTABLE');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceForme()).toBeNull();
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

  it('pré-fill IA : formeTestament ← aiData.formeTestamentDetectee + provenance IA', () => {
    component.aiData = { formeTestamentDetectee: 'OLOGRAPHE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.formeTestament()).toBe('TESTAMENT_OLOGRAPHE');
    expect(component.provenanceForme()).toBe('IA');
  });

  it('pré-fill IA : dateRedaction + saineDEsprit + legsExcedeQuotite', () => {
    component.aiData = {
      formeTestamentDetectee: 'TESTAMENT_AUTHENTIQUE',
      dateRedactionTestamentDetectee: '2024-03-15',
      saineDEspritTestateurDetected: true,
      legsExcedeQuotiteDisponibleDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.formeTestament()).toBe('TESTAMENT_AUTHENTIQUE');
    expect(component.provenanceForme()).toBe('IA');
    expect(component.dateRedaction()).toBe('2024-03-15');
    expect(component.provenanceDateRedaction()).toBe('IA');
    expect(component.saineDEsprit()).toBe(true);
    expect(component.provenanceSaineEsprit()).toBe('IA');
    expect(component.legsExcedeQuotiteDisponible()).toBe(true);
    expect(component.provenanceLegsExcedeQuotite()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.formeTestament()).toBeNull();
    expect(component.dateRedaction()).toBeNull();
    expect(component.saineDEsprit()).toBeNull();
    expect(component.provenanceForme()).toBeNull();
  });

  it('onFormeTestamentChange efface provenance IA + reset des champs des autres formes', () => {
    component.aiData = { formeTestamentDetectee: 'OLOGRAPHE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceForme()).toBe('IA');
    component.ecritureManuscritIntegrale.set(true);

    // Avocat passe à AUTHENTIQUE → reset des champs olographe.
    component.onFormeTestamentChange('TESTAMENT_AUTHENTIQUE');
    expect(component.formeTestament()).toBe('TESTAMENT_AUTHENTIQUE');
    expect(component.provenanceForme()).toBeNull();
    expect(component.ecritureManuscritIntegrale()).toBeNull();
    expect(component.dateComplete()).toBeNull();
    expect(component.signatureTestateur()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid : OLOGRAPHE requiert ecriture + date + signature + capacité', () => {
    component.formeTestament.set('TESTAMENT_OLOGRAPHE');
    component.dateRedaction.set('2024-03-15');
    component.ageTestateurAnsRedaction.set(72);
    component.saineDEsprit.set(true);

    // Sans champs olographe → invalide.
    expect(component.formValid()).toBe(false);

    component.ecritureManuscritIntegrale.set(true);
    component.dateComplete.set(true);
    component.signatureTestateur.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : AUTHENTIQUE requiert ses 4 champs spécifiques', () => {
    component.formeTestament.set('TESTAMENT_AUTHENTIQUE');
    component.dateRedaction.set('2024-03-15');
    component.ageTestateurAnsRedaction.set(72);
    component.saineDEsprit.set(true);

    component.presenceNotaireEtTemoinsConforme.set(true);
    component.dicteEnPresence.set(true);
    component.lectureFinaleAuTestateur.set(true);
    // signaturesCompletes manquant → invalide
    expect(component.formValid()).toBe(false);
    component.signaturesCompletes.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : MYSTIQUE requiert ses 3 champs spécifiques', () => {
    component.formeTestament.set('TESTAMENT_MYSTIQUE');
    component.dateRedaction.set('2024-03-15');
    component.ageTestateurAnsRedaction.set(72);
    component.saineDEsprit.set(true);

    component.remiseSousPliCache.set(true);
    component.declarationDevant2Temoins.set(true);
    expect(component.formValid()).toBe(false);
    component.acteSuscriptionNotaire.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : INTERNATIONAL requiert respecteFormeWashington + signaturesCompletes', () => {
    component.formeTestament.set('TESTAMENT_INTERNATIONAL');
    component.dateRedaction.set('2024-03-15');
    component.ageTestateurAnsRedaction.set(72);
    component.saineDEsprit.set(true);

    component.respecteFormeWashington.set(true);
    expect(component.formValid()).toBe(false);
    component.signaturesCompletes.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : âge invalide (< 0 ou > 130) rejette', () => {
    component.formeTestament.set('TESTAMENT_OLOGRAPHE');
    component.dateRedaction.set('2024-03-15');
    component.saineDEsprit.set(true);
    component.ecritureManuscritIntegrale.set(true);
    component.dateComplete.set(true);
    component.signatureTestateur.set(true);

    component.ageTestateurAnsRedaction.set(-5);
    expect(component.formValid()).toBe(false);
    component.ageTestateurAnsRedaction.set(140);
    expect(component.formValid()).toBe(false);
    component.ageTestateurAnsRedaction.set(72);
    expect(component.formValid()).toBe(true);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST envoie le body attendu + résultat + snackbar succès', () => {
    component.formeTestament.set('TESTAMENT_OLOGRAPHE');
    component.dateRedaction.set('2024-03-15');
    component.ageTestateurAnsRedaction.set(72);
    component.saineDEsprit.set(true);
    component.ecritureManuscritIntegrale.set(true);
    component.dateComplete.set(true);
    component.signatureTestateur.set(true);
    component.legsExcedeQuotiteDisponible.set(false);

    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.formeTestament).toBe('TESTAMENT_OLOGRAPHE');
    expect(req.request.body.dateRedaction).toBe('2024-03-15');
    expect(req.request.body.ageTestateurAnsRedaction).toBe(72);
    expect(req.request.body.saineDEsprit).toBe(true);
    expect(req.request.body.ecritureManuscritIntegrale).toBe(true);
    expect(req.request.body.dateComplete).toBe(true);
    expect(req.request.body.signatureTestateur).toBe(true);
    expect(req.request.body.legsExcedeQuotiteDisponible).toBe(false);
    req.flush(response());

    expect(component.result()!.verdictValidite).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Validité testament analysée', 'OK', jasmine.any(Object));
  });

  it("calculate() ignoré si form invalide (pas d'appel HTTP)", () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.formeTestament.set('TESTAMENT_OLOGRAPHE');
    component.dateRedaction.set('2024-03-15');
    component.ageTestateurAnsRedaction.set(72);
    component.saineDEsprit.set(true);
    component.ecritureManuscritIntegrale.set(true);
    component.dateComplete.set(true);
    component.signatureTestateur.set(true);

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

  it('coherenceAlerts.FORME présente si IA divergente de la saisie', () => {
    component.aiData = { formeTestamentDetectee: 'TESTAMENT_OLOGRAPHE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Pré-fill mit OLOGRAPHE puis avocat passe à AUTHENTIQUE → divergence.
    component.onFormeTestamentChange('TESTAMENT_AUTHENTIQUE');

    const alerts = component.coherenceAlerts();
    expect(alerts.FORME).toBeDefined();
    expect(alerts.FORME!.field).toBe('FORME');
    expect(alerts.FORME!.source).toBe('IA');
  });

  it('coherenceAlerts.LEGS_EXCEDE_QUOTITE indépendant des autres alertes', () => {
    component.aiData = {
      formeTestamentDetectee: 'TESTAMENT_OLOGRAPHE',
      legsExcedeQuotiteDisponibleDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Pré-fill mit la valeur IA legs=true. L'avocat décoche.
    component.onLegsExcedeQuotiteDisponibleChange(false);
    const alerts = component.coherenceAlerts();
    expect(alerts.LEGS_EXCEDE_QUOTITE).toBeDefined();
    expect(alerts.LEGS_EXCEDE_QUOTITE!.source).toBe('IA');
  });

  it('coherenceAlerts.SAINE_ESPRIT multi-sources F96 + IA → MULTI', () => {
    component.aiData = { saineDEspritTestateurDetected: true } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Sain d\'esprit',
        statut: 'NON_COMPLIANT',
        critereCode: 'TESTAMENT_SAINE_ESPRIT',
        expectedValue: 'OUI',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit true puis avocat dit false.
    component.onSaineDEspritChange(false);

    const alert = component.coherenceAlerts().SAINE_ESPRIT;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { formeTestamentDetectee: 'OLOGRAPHE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onFormeTestamentChange('TESTAMENT_AUTHENTIQUE');
    expect(component.coherenceAlerts().FORME).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().FORME).toBeUndefined();
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { formeTestamentDetectee: 'OLOGRAPHE' } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.formeTestament()).toBe('TESTAMENT_OLOGRAPHE');
    expect(component.provenanceForme()).toBe('IA');
  });

  it('ngOnChanges(aiData) post-saisie ne réécrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onFormeTestamentChange('TESTAMENT_MYSTIQUE');
    expect(component.provenanceForme()).toBeNull();

    const newAi = { formeTestamentDetectee: 'OLOGRAPHE' } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.formeTestament()).toBe('TESTAMENT_MYSTIQUE');
    expect(component.provenanceForme()).toBeNull();
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

  it('verdictBannerClass : NUL → critical, CONTESTABLE → warn, VALIDE → info', () => {
    expect(component.verdictBannerClass('NUL')).toContain('critical');
    expect(component.verdictBannerClass('CONTESTABLE')).toContain('warn');
    expect(component.verdictBannerClass('VALIDE')).toContain('info');
    expect(component.verdictBannerClass(null)).toContain('info');
  });

  it('verdictChipClass aligne palette header', () => {
    expect(component.verdictChipClass('NUL')).toContain('critical');
    expect(component.verdictChipClass('CONTESTABLE')).toContain('warn');
    expect(component.verdictChipClass('VALIDE')).toContain('info');
  });

  it('verdictIcon : NUL=gpp_bad, CONTESTABLE=warning, VALIDE=verified', () => {
    expect(component.verdictIcon('NUL')).toBe('gpp_bad');
    expect(component.verdictIcon('CONTESTABLE')).toBe('warning');
    expect(component.verdictIcon('VALIDE')).toBe('verified');
  });

  it('formeLabel + verdictLabel + viceLabel couvrent les enums', () => {
    expect(component.formeLabel('TESTAMENT_OLOGRAPHE')).toContain('Olographe');
    expect(component.formeLabel('TESTAMENT_AUTHENTIQUE')).toContain('Authentique');
    expect(component.formeLabel('TESTAMENT_MYSTIQUE')).toContain('Mystique');
    expect(component.formeLabel('TESTAMENT_INTERNATIONAL')).toContain('International');
    expect(component.formeLabel(null)).toBe('');

    expect(component.verdictLabel('VALIDE')).toContain('valide');
    expect(component.verdictLabel('CONTESTABLE')).toContain('contestable');
    expect(component.verdictLabel('NUL')).toContain('nul');

    expect(component.viceLabel('FORME_OLOGRAPHE_NON_MANUSCRITE')).toContain('non manuscrit');
    expect(component.viceLabel('INSANITE_ESPRIT')).toContain("Insanité");
    expect(component.viceLabel('REVOCATION_DECHIRURE')).toContain('Révocation');
  });

  it('isOlographe / isAuthentique / isMystique / isInternational correspondent', () => {
    component.formeTestament.set('TESTAMENT_OLOGRAPHE');
    expect(component.isOlographe()).toBe(true);
    expect(component.isAuthentique()).toBe(false);

    component.formeTestament.set('TESTAMENT_AUTHENTIQUE');
    expect(component.isAuthentique()).toBe(true);
    expect(component.isOlographe()).toBe(false);

    component.formeTestament.set('TESTAMENT_MYSTIQUE');
    expect(component.isMystique()).toBe(true);

    component.formeTestament.set('TESTAMENT_INTERNATIONAL');
    expect(component.isInternational()).toBe(true);
  });
});
