import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { CreditTempsBeSectionComponent } from './credit-temps-be-section.component';
import { CreditTempsBeResponse } from '../../core/models/credit-temps-be.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('CreditTempsBeSectionComponent', () => {
  let component: CreditTempsBeSectionComponent;
  let fixture: ComponentFixture<CreditTempsBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/credit-temps-be-analysis';

  function response(overrides: Partial<CreditTempsBeResponse> = {}): CreditTempsBeResponse {
    return {
      caseFileId: 'case-1',
      regime: 'AVEC_MOTIF',
      eligible: true,
      scoreGlobal: 80,
      verdictEligibilite: 'ELEVEE',
      criteresNonRemplis: [],
      indemniteOnemMensuelle: 425.50,
      dureeMaximaleMois: 51,
      baseJuridique: 'CCT 103 art. 2 + AR 12/12/2001 indemnité ONEM',
      formule: 'durée max = 51 mois (CCT 103 motif soins enfant)',
      messages: ['Crédit-temps avec motif accordé sous réserve de l\'accord employeur.'],
      ...overrides,
    };
  }

  function flushSourceExplanations(): void {
    httpMock.match((r) => r.url.endsWith('/source-explanations')).forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        CreditTempsBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CreditTempsBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ============================================================
  // Gate pays + init
  // ============================================================

  it('BELGIQUE → isBelgium() true, GET appelé au ngOnInit', () => {
    expect(component.isBelgium()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
  });

  it('FRANCE → isBelgium() false, aucun appel HTTP au ngOnInit (gate pays)', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isBelgium()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
    httpMock.expectNone((r) => r.url.endsWith('/source-explanations'));
  });

  it('charge le résultat existant si GET 200 (mode résultat hydraté)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    flushSourceExplanations();

    expect(component.result()!.indemniteOnemMensuelle).toBe(425.50);
    expect(component.showForm()).toBe(false);
    expect(component.regime()).toBe('AVEC_MOTIF');
    expect(component.provenanceAnciennete()).toBeNull();
    expect(component.provenanceAge()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // Pré-fill IA
  // ============================================================

  it('pré-fill IA : ageDemandeurAnnees → champ + badge IA', () => {
    component.aiData = { ageDemandeurAnnees: 58 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.ageDemandeurAnnees()).toBe(58);
    expect(component.provenanceAge()).toBe('IA');
  });

  it('pré-fill IA : dateEntree → ancienneteEntrepriseMois calculée', () => {
    // 24 mois en arrière exactement.
    const now = new Date();
    const dateEntree = new Date(now.getFullYear() - 2, now.getMonth(), 15);
    const isoEntree = dateEntree.toISOString().slice(0, 10);
    component.aiData = { dateEntree: isoEntree } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.ancienneteEntrepriseMois()).toBe(24);
    expect(component.provenanceAnciennete()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.ancienneteEntrepriseMois()).toBeNull();
    expect(component.ageDemandeurAnnees()).toBeNull();
    expect(component.provenanceAnciennete()).toBeNull();
    expect(component.provenanceAge()).toBeNull();
  });

  it('pré-fill IA : ageDemandeurAnnees > 75 → ignoré (gracieux)', () => {
    component.aiData = { ageDemandeurAnnees: 99 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.ageDemandeurAnnees()).toBeNull();
    expect(component.provenanceAge()).toBeNull();
  });

  it('onAncienneteChange efface le badge IA ancienneté', () => {
    const now = new Date();
    const dateEntree = new Date(now.getFullYear() - 1, now.getMonth(), 15);
    component.aiData = { dateEntree: dateEntree.toISOString().slice(0, 10) } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.provenanceAnciennete()).toBe('IA');
    component.onAncienneteChange(36);
    expect(component.ancienneteEntrepriseMois()).toBe(36);
    expect(component.provenanceAnciennete()).toBeNull();
  });

  it('onAgeChange efface le badge IA âge', () => {
    component.aiData = { ageDemandeurAnnees: 58 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.provenanceAge()).toBe('IA');
    component.onAgeChange(60);
    expect(component.ageDemandeurAnnees()).toBe(60);
    expect(component.provenanceAge()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement (régime non défini)', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si AVEC_MOTIF sans motif', () => {
    component.regime.set('AVEC_MOTIF');
    component.ancienneteEntrepriseMois.set(36);
    component.tailleEntrepriseEtp.set(50);
    component.dureeReductionType.set('MOITIE');
    component.ageDemandeurAnnees.set(40);
    component.dateDemande.set('2026-04-25');
    expect(component.formValid()).toBe(false);
    component.motif.set('SOINS_ENFANT_LT_8_ANS');
    expect(component.formValid()).toBe(true);
  });

  it('formValid true si SANS_MOTIF (motif optionnel)', () => {
    component.regime.set('SANS_MOTIF');
    component.ancienneteEntrepriseMois.set(36);
    component.tailleEntrepriseEtp.set(50);
    component.dureeReductionType.set('CINQUIEME');
    component.ageDemandeurAnnees.set(40);
    component.dateDemande.set('2026-04-25');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si âge > 75', () => {
    component.regime.set('FIN_CARRIERE');
    component.ancienneteEntrepriseMois.set(360);
    component.tailleEntrepriseEtp.set(20);
    component.dureeReductionType.set('TEMPS_PLEIN');
    component.ageDemandeurAnnees.set(80);
    component.dateDemande.set('2026-04-25');
    expect(component.formValid()).toBe(false);
  });

  it('onRegimeChange : passer de AVEC_MOTIF → SANS_MOTIF efface le motif', () => {
    component.regime.set('AVEC_MOTIF');
    component.motif.set('FORMATION');
    component.onRegimeChange('SANS_MOTIF');
    expect(component.regime()).toBe('SANS_MOTIF');
    expect(component.motif()).toBeNull();
  });

  it('motifRequired() true ssi régime AVEC_MOTIF', () => {
    component.regime.set('SANS_MOTIF');
    expect(component.motifRequired()).toBe(false);
    component.regime.set('AVEC_MOTIF');
    expect(component.motifRequired()).toBe(true);
    component.regime.set('FIN_CARRIERE');
    expect(component.motifRequired()).toBe(false);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST + résultat + snackbar succès', () => {
    component.regime.set('AVEC_MOTIF');
    component.motif.set('SOINS_ENFANT_LT_8_ANS');
    component.ancienneteEntrepriseMois.set(36);
    component.tailleEntrepriseEtp.set(50);
    component.dureeReductionType.set('MOITIE');
    component.ageDemandeurAnnees.set(40);
    component.dateDemande.set('2026-04-25');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      regime: 'AVEC_MOTIF',
      motif: 'SOINS_ENFANT_LT_8_ANS',
      ancienneteEntrepriseMois: 36,
      tailleEntrepriseEtp: 50,
      dureeReductionType: 'MOITIE',
      ageDemandeurAnnees: 40,
      dateDemande: '2026-04-25',
    });
    req.flush(response());

    expect(component.result()!.scoreGlobal).toBe(80);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Crédit-temps analysé', 'OK', jasmine.any(Object));
  });

  it('calculate() FIN_CARRIERE sans motif n\'envoie pas motif dans le body', () => {
    component.regime.set('FIN_CARRIERE');
    component.ancienneteEntrepriseMois.set(360);
    component.tailleEntrepriseEtp.set(50);
    component.dureeReductionType.set('CINQUIEME');
    component.ageDemandeurAnnees.set(60);
    component.dateDemande.set('2026-04-25');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.motif).toBeUndefined();
    req.flush(response({ regime: 'FIN_CARRIERE' }));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.regime.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.regime.set('SANS_MOTIF');
    component.ancienneteEntrepriseMois.set(36);
    component.tailleEntrepriseEtp.set(50);
    component.dureeReductionType.set('MOITIE');
    component.ageDemandeurAnnees.set(40);
    component.dateDemande.set('2026-04-25');
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

  it('coherenceAlerts.AGE présent si écart IA > 1 an', () => {
    component.aiData = { ageDemandeurAnnees: 60 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // Pré-fill mit 60 ; avocat saisit 50 → divergence 10 ans.
    component.onAgeChange(50);

    const alerts = component.coherenceAlerts();
    expect(alerts.AGE).toBeDefined();
    expect(alerts.AGE!.field).toBe('AGE');
    expect(alerts.AGE!.source).toBe('IA');
    expect(alerts.AGE!.expectedDisplay).toContain('60');
  });

  it('coherenceAlerts.AGE absent si écart ≤ 1 an', () => {
    component.aiData = { ageDemandeurAnnees: 60 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onAgeChange(60);

    expect(component.coherenceAlerts().AGE).toBeUndefined();
  });

  it('coherenceAlerts.ANCIENNETE présent si écart IA > 6 mois', () => {
    const now = new Date();
    const dateEntree = new Date(now.getFullYear() - 3, now.getMonth(), 15); // 36 mois
    component.aiData = { dateEntree: dateEntree.toISOString().slice(0, 10) } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // IA ~ 36 mois ; avocat saisit 60 mois → écart 24 mois > 6.
    component.onAncienneteChange(60);

    const alerts = component.coherenceAlerts();
    expect(alerts.ANCIENNETE).toBeDefined();
    expect(alerts.ANCIENNETE!.field).toBe('ANCIENNETE');
    expect(alerts.ANCIENNETE!.source).toBe('IA');
  });

  it('coherenceAlerts.ANCIENNETE multi-sources F96 + IA convergents → MULTI', () => {
    const now = new Date();
    const dateEntree = new Date(now.getFullYear() - 3, now.getMonth(), 15); // 36 mois
    component.aiData = { dateEntree: dateEntree.toISOString().slice(0, 10) } as TravailExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Ancienneté entreprise',
        statut: 'NON_COMPLIANT',
        critereCode: 'CREDIT_TEMPS_ANCIENNETE',
        expectedValue: '36',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // Avocat saisit 60 mois → divergence IA (36) + F96 (36).
    component.onAncienneteChange(60);

    const alert = component.coherenceAlerts().ANCIENNETE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.reason).toContain(' ET ');
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { ageDemandeurAnnees: 60 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onAgeChange(50);
    expect(component.coherenceAlerts().AGE).toBeDefined();

    // Après calcul, form masqué → alertes doivent disparaître.
    component.showForm.set(false);
    expect(component.coherenceAlerts().AGE).toBeUndefined();
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    const newAi = { ageDemandeurAnnees: 55 } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.ageDemandeurAnnees()).toBe(55);
    expect(component.provenanceAge()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onAgeChange(45);
    expect(component.provenanceAge()).toBeNull();

    const newAi = { ageDemandeurAnnees: 60 } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    // Saisie avocat préservée — provenance null sur âge → pas de ré-application.
    expect(component.ageDemandeurAnnees()).toBe(45);
    expect(component.provenanceAge()).toBeNull();
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
    expect(component.bannerClass('ELEVEE')).toContain('credit-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('credit-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('credit-banner--neutre');
    expect(component.bannerClass(null)).toContain('credit-banner--neutre');
  });

  it('regimeLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.regimeLabel('AVEC_MOTIF')).toContain('Avec motif');
    expect(component.regimeLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.regimeLabel(null)).toBe('');
  });
});
