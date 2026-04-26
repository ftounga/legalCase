import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { DonationSectionComponent } from './donation-section.component';
import { DonationResponse } from '../../core/models/donation.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('DonationSectionComponent', () => {
  let component: DonationSectionComponent;
  let fixture: ComponentFixture<DonationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/donation-analysis';

  function response(overrides: Partial<DonationResponse> = {}): DonationResponse {
    return {
      caseFileId: 'case-1',
      formeDonation: 'DONATION_NOTARIEE',
      verdictValidite: 'VALIDE',
      risquesRequalification: [],
      actionEnReductionPossible: false,
      revocationPossible: false,
      delaiContestationAns: 5,
      scoreEligibilite: 100,
      baseJuridique: 'Art. 893-958, 902-906, 920 et s., 931, 953-958 Cciv',
      formule: 'Forme DONATION_NOTARIEE + verdict VALIDE + 0 risque → score 100',
      messages: ['Donation notariée valide.'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        DonationSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(DonationSectionComponent);
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

  it('pré-fill IA : formeDonation ← aiData.formeDonationDetectee + provenance IA', () => {
    component.aiData = { formeDonationDetectee: 'NOTARIEE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.formeDonation()).toBe('DONATION_NOTARIEE');
    expect(component.provenanceForme()).toBe('IA');
  });

  it('pré-fill IA : dateDonation + saineDEsprit + respectQuotiteDisponible', () => {
    component.aiData = {
      formeDonationDetectee: 'DONATION_MANUELLE',
      dateDonationDetectee: '2024-03-15',
      saineDEspritDonateurDetected: true,
      respectQuotiteDisponibleDetected: false,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.formeDonation()).toBe('DONATION_MANUELLE');
    expect(component.provenanceForme()).toBe('IA');
    expect(component.dateDonation()).toBe('2024-03-15');
    expect(component.provenanceDateDonation()).toBe('IA');
    expect(component.saineDEsprit()).toBe(true);
    expect(component.provenanceSaineEsprit()).toBe('IA');
    expect(component.respectQuotiteDisponible()).toBe(false);
    expect(component.provenanceRespectQuotite()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.formeDonation()).toBeNull();
    expect(component.dateDonation()).toBeNull();
    expect(component.saineDEsprit()).toBeNull();
    expect(component.provenanceForme()).toBeNull();
  });

  it('onFormeDonationChange efface provenance IA + reset des champs des autres formes', () => {
    component.aiData = { formeDonationDetectee: 'NOTARIEE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceForme()).toBe('IA');
    component.acteAuthentique.set(true);
    component.acceptationExpresse.set(true);

    // Avocat passe à MANUELLE → reset des champs notariée.
    component.onFormeDonationChange('DONATION_MANUELLE');
    expect(component.formeDonation()).toBe('DONATION_MANUELLE');
    expect(component.provenanceForme()).toBeNull();
    expect(component.acteAuthentique()).toBeNull();
    expect(component.acceptationExpresse()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid : NOTARIEE requiert acte authentique + acceptation expresse + capacités', () => {
    component.formeDonation.set('DONATION_NOTARIEE');
    component.dateDonation.set('2024-03-15');
    component.ageDonateurAns.set(65);
    component.saineDEsprit.set(true);
    component.capaciteDonateur.set(true);
    component.capaciteRecipiendaire.set(true);
    component.consentementLibre.set(true);
    component.objetDetermine.set(true);
    component.respectFormalisme.set(true);
    component.respectQuotiteDisponible.set(true);

    // Sans champs notariée → invalide.
    expect(component.formValid()).toBe(false);

    component.acteAuthentique.set(true);
    component.acceptationExpresse.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : MANUELLE requiert remiseEffective + bienMeuble', () => {
    component.formeDonation.set('DONATION_MANUELLE');
    component.dateDonation.set('2024-03-15');
    component.ageDonateurAns.set(65);
    component.saineDEsprit.set(true);
    component.capaciteDonateur.set(true);
    component.capaciteRecipiendaire.set(true);
    component.consentementLibre.set(true);
    component.objetDetermine.set(true);
    component.respectFormalisme.set(true);
    component.respectQuotiteDisponible.set(true);

    component.remiseEffective.set(true);
    expect(component.formValid()).toBe(false);
    component.bienMeuble.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : DON_INDIRECT requiert intentionLiberale + actePrincipalNeutre', () => {
    component.formeDonation.set('DON_INDIRECT');
    component.dateDonation.set('2024-03-15');
    component.ageDonateurAns.set(65);
    component.saineDEsprit.set(true);
    component.capaciteDonateur.set(true);
    component.capaciteRecipiendaire.set(true);
    component.consentementLibre.set(true);
    component.objetDetermine.set(true);
    component.respectFormalisme.set(true);
    component.respectQuotiteDisponible.set(true);

    component.intentionLiberale.set(true);
    expect(component.formValid()).toBe(false);
    component.actePrincipalNeutre.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : DEGUISEE requiert apparenceOnerueuse + prixIncoherent', () => {
    component.formeDonation.set('DONATION_DEGUISEE');
    component.dateDonation.set('2024-03-15');
    component.ageDonateurAns.set(65);
    component.saineDEsprit.set(true);
    component.capaciteDonateur.set(true);
    component.capaciteRecipiendaire.set(true);
    component.consentementLibre.set(true);
    component.objetDetermine.set(true);
    component.respectFormalisme.set(true);
    component.respectQuotiteDisponible.set(true);

    component.apparenceOnerueuse.set(true);
    expect(component.formValid()).toBe(false);
    component.prixIncoherent.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : âge invalide (< 0 ou > 130) rejette', () => {
    component.formeDonation.set('DONATION_NOTARIEE');
    component.dateDonation.set('2024-03-15');
    component.saineDEsprit.set(true);
    component.capaciteDonateur.set(true);
    component.capaciteRecipiendaire.set(true);
    component.consentementLibre.set(true);
    component.objetDetermine.set(true);
    component.respectFormalisme.set(true);
    component.respectQuotiteDisponible.set(true);
    component.acteAuthentique.set(true);
    component.acceptationExpresse.set(true);

    component.ageDonateurAns.set(-5);
    expect(component.formValid()).toBe(false);
    component.ageDonateurAns.set(140);
    expect(component.formValid()).toBe(false);
    component.ageDonateurAns.set(65);
    expect(component.formValid()).toBe(true);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST envoie le body attendu + résultat + snackbar succès', () => {
    component.formeDonation.set('DONATION_NOTARIEE');
    component.dateDonation.set('2024-03-15');
    component.ageDonateurAns.set(65);
    component.saineDEsprit.set(true);
    component.capaciteDonateur.set(true);
    component.capaciteRecipiendaire.set(true);
    component.consentementLibre.set(true);
    component.objetDetermine.set(true);
    component.respectFormalisme.set(true);
    component.respectQuotiteDisponible.set(true);
    component.acteAuthentique.set(true);
    component.acceptationExpresse.set(true);

    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.formeDonation).toBe('DONATION_NOTARIEE');
    expect(req.request.body.dateDonation).toBe('2024-03-15');
    expect(req.request.body.ageDonateurAns).toBe(65);
    expect(req.request.body.saineDEsprit).toBe(true);
    expect(req.request.body.acteAuthentique).toBe(true);
    expect(req.request.body.acceptationExpresse).toBe(true);
    expect(req.request.body.respectQuotiteDisponible).toBe(true);
    expect(req.request.body.vicesConsentementDol).toBe(false);
    req.flush(response());

    expect(component.result()!.verdictValidite).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Validité donation analysée', 'OK', jasmine.any(Object));
  });

  it("calculate() ignoré si form invalide (pas d'appel HTTP)", () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.formeDonation.set('DONATION_NOTARIEE');
    component.dateDonation.set('2024-03-15');
    component.ageDonateurAns.set(65);
    component.saineDEsprit.set(true);
    component.capaciteDonateur.set(true);
    component.capaciteRecipiendaire.set(true);
    component.consentementLibre.set(true);
    component.objetDetermine.set(true);
    component.respectFormalisme.set(true);
    component.respectQuotiteDisponible.set(true);
    component.acteAuthentique.set(true);
    component.acceptationExpresse.set(true);

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
    component.aiData = { formeDonationDetectee: 'DONATION_NOTARIEE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Pré-fill mit NOTARIEE puis avocat passe à MANUELLE → divergence.
    component.onFormeDonationChange('DONATION_MANUELLE');

    const alerts = component.coherenceAlerts();
    expect(alerts.FORME).toBeDefined();
    expect(alerts.FORME!.field).toBe('FORME');
    expect(alerts.FORME!.source).toBe('IA');
  });

  it('coherenceAlerts.RESPECT_QUOTITE indépendant des autres alertes', () => {
    component.aiData = {
      formeDonationDetectee: 'DONATION_NOTARIEE',
      respectQuotiteDisponibleDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Pré-fill mit la valeur IA quotite=true. L'avocat passe à false.
    component.onRespectQuotiteDisponibleChange(false);
    const alerts = component.coherenceAlerts();
    expect(alerts.RESPECT_QUOTITE).toBeDefined();
    expect(alerts.RESPECT_QUOTITE!.source).toBe('IA');
  });

  it('coherenceAlerts.SAINE_ESPRIT multi-sources F96 + IA → MULTI', () => {
    component.aiData = { saineDEspritDonateurDetected: true } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: "Sain d'esprit",
        statut: 'NON_COMPLIANT',
        critereCode: 'DONATION_SAINE_ESPRIT',
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
    component.aiData = { formeDonationDetectee: 'NOTARIEE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onFormeDonationChange('DONATION_MANUELLE');
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

    const newAi = { formeDonationDetectee: 'NOTARIEE' } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.formeDonation()).toBe('DONATION_NOTARIEE');
    expect(component.provenanceForme()).toBe('IA');
  });

  it('ngOnChanges(aiData) post-saisie ne réécrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onFormeDonationChange('DON_INDIRECT');
    expect(component.provenanceForme()).toBeNull();

    const newAi = { formeDonationDetectee: 'NOTARIEE' } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.formeDonation()).toBe('DON_INDIRECT');
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

  it('formeLabel + verdictLabel + risqueLabel couvrent les enums', () => {
    expect(component.formeLabel('DONATION_NOTARIEE')).toContain('notariée');
    expect(component.formeLabel('DONATION_MANUELLE')).toContain('manuelle');
    expect(component.formeLabel('DON_INDIRECT')).toContain('indirect');
    expect(component.formeLabel('DONATION_DEGUISEE')).toContain('déguisée');
    expect(component.formeLabel(null)).toBe('');

    expect(component.verdictLabel('VALIDE')).toContain('valide');
    expect(component.verdictLabel('CONTESTABLE')).toContain('contestable');
    expect(component.verdictLabel('NUL')).toContain('nulle');

    expect(component.risqueLabel('FORME_NOTARIEE_NON_AUTHENTIQUE')).toContain('authentique');
    expect(component.risqueLabel('INSANITE_ESPRIT')).toContain("Insanité");
    expect(component.risqueLabel('REVOCATION_INGRATITUDE')).toContain('ingratitude');
    expect(component.risqueLabel('EXCES_QUOTITE_DISPONIBLE')).toContain('quotité');
  });

  it('isNotariee / isManuelle / isIndirect / isDeguisee correspondent', () => {
    component.formeDonation.set('DONATION_NOTARIEE');
    expect(component.isNotariee()).toBe(true);
    expect(component.isManuelle()).toBe(false);

    component.formeDonation.set('DONATION_MANUELLE');
    expect(component.isManuelle()).toBe(true);
    expect(component.isNotariee()).toBe(false);

    component.formeDonation.set('DON_INDIRECT');
    expect(component.isIndirect()).toBe(true);

    component.formeDonation.set('DONATION_DEGUISEE');
    expect(component.isDeguisee()).toBe(true);
  });
});
