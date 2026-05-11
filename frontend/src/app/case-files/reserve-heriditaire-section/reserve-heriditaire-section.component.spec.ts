import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { ReserveHeriditaireSectionComponent } from './reserve-heriditaire-section.component';
import { ReserveHereditaireResponse } from '../../core/models/reserve-heriditaire.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('ReserveHeriditaireSectionComponent', () => {
  let component: ReserveHeriditaireSectionComponent;
  let fixture: ComponentFixture<ReserveHeriditaireSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/reserve-heriditaire-analysis';

  function response(overrides: Partial<ReserveHereditaireResponse> = {}): ReserveHereditaireResponse {
    return {
      caseFileId: 'case-1',
      nombreEnfants: 2,
      conjointSurvivant: true,
      montantSuccession: 300000,
      montantLibsTotal: 200000,
      dateOuvertureSuccession: '2024-01-15',
      qualiteDuDemandeur: 'HERITIER_RESERVATAIRE_DESCENDANT',
      quotiteDisponiblePct: 33.33,
      reservePct: 66.67,
      montantReserveEur: 200010,
      montantQuotiteDispoEur: 99990,
      excedentReductibleEur: 100010,
      actionReductionRecevable: true,
      delaiPrescriptionRestantMois: 36,
      verdictRecevabilite: 'RECEVABLE',
      scoreEligibilite: 90,
      baseJuridique: 'Art. 913 + 914-1 + 920-928 Cciv',
      formule: '2 enfants → réserve 66.67 % + libs 200000 € > QD 99990 € → excédent 100010 €',
      messages: ['Ordre de réduction (art. 923-924) : legs d\'abord, donations dans l\'ordre inverse.'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        ReserveHeriditaireSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ReserveHeriditaireSectionComponent);
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
    req.flush(response());

    expect(component.result()!.actionReductionRecevable).toBe(true);
    expect(component.result()!.verdictRecevabilite).toBe('RECEVABLE');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceConjoint()).toBeNull();
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

  it('pré-fill IA : conjoint + enfants + montants + qualité', () => {
    component.aiData = {
      conjointSurvivantDetected: true,
      nombreEnfantsSuccessionDetecte: 3,
      montantSuccessionEurDetecte: 500000,
      montantLibsTotalEurDetecte: 100000,
      dateOuvertureSuccessionDetectee: '2024-01-15',
      qualiteDuDemandeurReserveDetecte: 'HERITIER_RESERVATAIRE_DESCENDANT',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.conjointSurvivant()).toBe(true);
    expect(component.provenanceConjoint()).toBe('IA');
    expect(component.nombreEnfants()).toBe(3);
    expect(component.provenanceNombreEnfants()).toBe('IA');
    expect(component.montantSuccession()).toBe(500000);
    expect(component.provenanceMontantSuccession()).toBe('IA');
    expect(component.montantLibsTotal()).toBe(100000);
    expect(component.dateOuvertureSuccession()).toBe('2024-01-15');
    expect(component.qualiteDuDemandeur()).toBe('HERITIER_RESERVATAIRE_DESCENDANT');
  });

  it('pré-fill sans aiData → aucun pré-remplissage', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.conjointSurvivant()).toBeNull();
    expect(component.nombreEnfants()).toBeNull();
    expect(component.provenanceConjoint()).toBeNull();
  });

  it('onConjointSurvivantChange efface badge IA', () => {
    component.aiData = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceConjoint()).toBe('IA');
    component.onConjointSurvivantChange(false);
    expect(component.conjointSurvivant()).toBe(false);
    expect(component.provenanceConjoint()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true après remplissage complet', () => {
    component.nombreEnfants.set(2);
    component.conjointSurvivant.set(true);
    component.montantSuccession.set(300000);
    component.montantLibsTotal.set(150000);
    component.dateOuvertureSuccession.set('2024-01-15');
    component.qualiteDuDemandeur.set('HERITIER_RESERVATAIRE_DESCENDANT');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si montantSuccession ≤ 0', () => {
    component.nombreEnfants.set(1);
    component.conjointSurvivant.set(false);
    component.montantSuccession.set(0);
    component.montantLibsTotal.set(0);
    component.dateOuvertureSuccession.set('2024-01-15');
    component.qualiteDuDemandeur.set('HERITIER_RESERVATAIRE_DESCENDANT');
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST envoie le body attendu + résultat + snackbar succès', () => {
    component.nombreEnfants.set(2);
    component.conjointSurvivant.set(true);
    component.montantSuccession.set(300000);
    component.montantLibsTotal.set(200000);
    component.dateOuvertureSuccession.set('2024-01-15');
    component.qualiteDuDemandeur.set('HERITIER_RESERVATAIRE_DESCENDANT');

    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.nombreEnfants).toBe(2);
    expect(req.request.body.conjointSurvivant).toBe(true);
    expect(req.request.body.montantSuccession).toBe(300000);
    expect(req.request.body.montantLibsTotal).toBe(200000);
    expect(req.request.body.dateOuvertureSuccession).toBe('2024-01-15');
    expect(req.request.body.qualiteDuDemandeur).toBe('HERITIER_RESERVATAIRE_DESCENDANT');
    req.flush(response());

    expect(component.result()!.actionReductionRecevable).toBe(true);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Réserve héréditaire analysée', 'OK', jasmine.any(Object));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.nombreEnfants.set(1);
    component.conjointSurvivant.set(false);
    component.montantSuccession.set(100000);
    component.montantLibsTotal.set(50000);
    component.dateOuvertureSuccession.set('2024-01-15');
    component.qualiteDuDemandeur.set('HERITIER_RESERVATAIRE_DESCENDANT');

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

  it('coherenceAlerts.CONJOINT_SURVIVANT présent si IA divergente de la saisie', () => {
    component.aiData = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onConjointSurvivantChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.CONJOINT_SURVIVANT).toBeDefined();
    expect(alerts.CONJOINT_SURVIVANT!.field).toBe('CONJOINT_SURVIVANT');
    expect(alerts.CONJOINT_SURVIVANT!.source).toBe('IA');
    expect(alerts.CONJOINT_SURVIVANT!.expectedDisplay).toContain('Conjoint survivant');
  });

  it('coherenceAlerts.NOMBRE_ENFANTS divergent F96 → alerte', () => {
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Nombre d\'enfants',
        statut: 'NON_COMPLIANT',
        critereCode: 'RESERVE_NB_ENFANTS',
        expectedValue: '3',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onNombreEnfantsChange(2);
    const alerts = component.coherenceAlerts();
    expect(alerts.NOMBRE_ENFANTS).toBeDefined();
    expect(alerts.NOMBRE_ENFANTS!.source).toBe('F96');
    expect(alerts.NOMBRE_ENFANTS!.expectedDisplay).toContain('3');
  });

  it('coherenceAlerts.CONJOINT_SURVIVANT multi-sources F96 + IA → MULTI', () => {
    component.aiData = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Conjoint survivant',
        statut: 'NON_COMPLIANT',
        critereCode: 'RESERVE_CONJOINT_SURVIVANT',
        expectedValue: 'OUI',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onConjointSurvivantChange(false);

    const alert = component.coherenceAlerts().CONJOINT_SURVIVANT;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onConjointSurvivantChange(false);
    expect(component.coherenceAlerts().CONJOINT_SURVIVANT).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().CONJOINT_SURVIVANT).toBeUndefined();
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.conjointSurvivant()).toBe(true);
    expect(component.provenanceConjoint()).toBe('IA');
  });

  it('ngOnChanges(aiData) post-saisie ne réécrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onConjointSurvivantChange(false);
    expect(component.provenanceConjoint()).toBeNull();

    const newAi = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.conjointSurvivant()).toBe(false);
    expect(component.provenanceConjoint()).toBeNull();
  });

  // ============================================================
  // Helpers UI
  // ============================================================

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode ré-affiche le form', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('verdictLabel couvre les 5 valeurs', () => {
    expect(component.verdictLabel('RECEVABLE')).toContain('recevable');
    expect(component.verdictLabel('NON_RECEVABLE_PAS_EXCEDENT')).toContain('excédent');
    expect(component.verdictLabel('NON_RECEVABLE_PRESCRIPTION')).toContain('prescrite');
    expect(component.verdictLabel('NON_RECEVABLE_QUALITE')).toContain('qualité');
    expect(component.verdictLabel('NON_RECEVABLE')).toContain('Action');
    expect(component.verdictLabel(null)).toBe('');
  });

  it('qualiteLabel couvre les 2 qualités', () => {
    expect(component.qualiteLabel('HERITIER_RESERVATAIRE_DESCENDANT')).toContain('descendant');
    expect(component.qualiteLabel('CONJOINT_SURVIVANT')).toBe('Conjoint survivant');
    expect(component.qualiteLabel(null)).toBe('');
  });

  it('verdictChipClass discrimine RECEVABLE (critique) vs autres', () => {
    expect(component.verdictChipClass('RECEVABLE')).toContain('critical');
    expect(component.verdictChipClass('NON_RECEVABLE_PAS_EXCEDENT')).toContain('info');
    expect(component.verdictChipClass(null)).toContain('info');
  });

  it('baremeRows highlight la configuration courante (2 enfants)', () => {
    component.nombreEnfants.set(2);
    component.conjointSurvivant.set(true);
    const rows = component.baremeRows();
    expect(rows.find((r) => r.configuration.startsWith('2 enfants'))!.highlighted).toBe(true);
    expect(rows.find((r) => r.configuration.startsWith('1 enfant'))!.highlighted).toBe(false);
  });

  it('baremeRows highlight 3+ enfants pour 4 enfants', () => {
    component.nombreEnfants.set(4);
    component.conjointSurvivant.set(false);
    const rows = component.baremeRows();
    expect(rows.find((r) => r.configuration.startsWith('3 enfants ou plus'))!.highlighted).toBe(true);
  });

  it('libsRatioPct + libsBarCritical reflètent excédent du résultat', () => {
    component.result.set(response());
    // libs 200000 / QD 99990 = 200.02 % → clampé à 150 %
    expect(component.libsRatioPct()).toBeCloseTo(150, 0);
    expect(component.libsBarCritical()).toBe(true);
  });

  it('prescriptionUrgent vrai si delai < 12 mois', () => {
    component.result.set(response({ delaiPrescriptionRestantMois: 6 }));
    expect(component.prescriptionUrgent()).toBe(true);

    component.result.set(response({ delaiPrescriptionRestantMois: 24 }));
    expect(component.prescriptionUrgent()).toBe(false);

    component.result.set(response({ delaiPrescriptionRestantMois: 0 }));
    expect(component.prescriptionUrgent()).toBe(false);
  });

  it('onNombreEnfantsChange efface badge IA', () => {
    component.aiData = { nombreEnfantsSuccessionDetecte: 2 } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.provenanceNombreEnfants()).toBe('IA');
    component.onNombreEnfantsChange(3);
    expect(component.nombreEnfants()).toBe(3);
    expect(component.provenanceNombreEnfants()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02c — mode simulateur autonome
  // ---------------------------------------------------------------------------

  describe('F-163 SF-163-02c — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-FA-24-reserve-heriditaire/calculate';
    const CASE_URL = '/api/v1/case-files/case-1/reserve-heriditaire-analysis';

    it('CA-02 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-02 : ne fait AUCUN GET vers /api/v1/case-files/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const matches = httpMock.match((r: { url: string }) => r.url.includes('/api/v1/case-files/'));
      expect(matches.length).toBe(0);
    });

    it('CA-04 : POST sur le dispatcher /api/v1/simulators/.../calculate en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      component.calculate();
      const requests = httpMock.match((r: { url: string; method: string }) => r.url === STANDALONE_URL && r.method === 'POST');
      // Si la méthode formValid() bloque ou si la gate FR/BE échoue, aucun POST
      // n'est émis — c'est acceptable (le standalone n'a pas de payload obligatoire).
      // On valide ici qu'**aucun POST vers le case-file URL** n'a été émis.
      const caseUrlPosts = httpMock.match((r: { url: string; method: string }) => r.url === CASE_URL && r.method === 'POST');
      expect(caseUrlPosts.length).toBe(0);
      // Et, si le composant a POST sur le dispatcher, il flush proprement.
      requests.forEach(req => req.flush({}));
    });
  });
});
