import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { PseSectionComponent } from './pse-section.component';
import { PseResponse } from '../../core/models/pse.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('PseSectionComponent', () => {
  let component: PseSectionComponent;
  let fixture: ComponentFixture<PseSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/pse-analysis';

  function response(overrides: Partial<PseResponse> = {}): PseResponse {
    return {
      caseFileId: 'case-1',
      pseRequis: true,
      scoreConformite: 80,
      verdictValidite: 'CONTESTABLE',
      criteresRemplis: ['CSE_CONSULTE', 'DREETS_VALIDE_OU_HOMOLOGUE'],
      criteresManquants: ['MESURES_MULTIPLES'],
      delaiContestationJours: 60,
      baseJuridique: 'Art. L.1233-24-1 + L.1233-30 + L.1233-57-2 + L.1233-61 + L.1235-7-1 Code du travail',
      formule: 'PSE requis (12 lic. / 30 j / 80 sal.) + 4 critères / 1 manquant = score 80 → CONTESTABLE',
      messages: ['PSE requis (L.1233-24-1) — la procédure spéciale s\'applique.'],
      country: 'FRANCE',
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
        PseSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(PseSectionComponent);
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
    flushSourceExplanations();
  });

  it('BELGIQUE → isFrance() false, aucun appel HTTP au ngOnInit (gate pays)', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isFrance()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
    httpMock.expectNone((r) => r.url.endsWith('/source-explanations'));
  });

  it('charge le résultat existant si GET 200 (mode résultat hydraté)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    flushSourceExplanations();

    expect(component.result()!.scoreConformite).toBe(80);
    expect(component.showForm()).toBe(false);
    expect(component.provenanceDateProjet()).toBeNull();
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

  it('pré-fill IA : dateProjet ← aiData.dateLicenciement', () => {
    component.aiData = { dateLicenciement: '2026-03-15' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.dateProjet()).toBe('2026-03-15');
    expect(component.provenanceDateProjet()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.dateProjet()).toBeNull();
    expect(component.provenanceDateProjet()).toBeNull();
  });

  it('onDateProjetChange efface le badge IA', () => {
    component.aiData = { dateLicenciement: '2026-03-15' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.provenanceDateProjet()).toBe('IA');
    component.onDateProjetChange('2026-04-01');
    expect(component.dateProjet()).toBe('2026-04-01');
    expect(component.provenanceDateProjet()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true seulement quand tous les champs requis sont présents', () => {
    component.tailleEntrepriseSalaries.set(80);
    component.nombreLicenciementsEnvisages.set(12);
    component.periodeJours.set(30);
    component.modeAdoption.set('ACCORD_COLLECTIF_MAJORITAIRE');
    component.csaeConsulteAvis.set('FAVORABLE');
    component.dreetsStatut.set('VALIDE');
    component.dateProjet.set('2026-04-25');
    expect(component.formValid()).toBe(true);
    component.csaeConsulteAvis.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('pseRequisPreview() reflète les seuils L.1233-24-1', () => {
    // En dessous des seuils.
    component.tailleEntrepriseSalaries.set(40);
    component.nombreLicenciementsEnvisages.set(5);
    component.periodeJours.set(30);
    expect(component.pseRequisPreview()).toBe(false);
    // Au seuil.
    component.tailleEntrepriseSalaries.set(50);
    component.nombreLicenciementsEnvisages.set(10);
    component.periodeJours.set(30);
    expect(component.pseRequisPreview()).toBe(true);
  });

  it('onMesureToggle ajoute / retire correctement', () => {
    expect(component.contenuMesures()).toEqual([]);
    component.onMesureToggle('RECLASSEMENT_INTERNE', true);
    expect(component.contenuMesures()).toEqual(['RECLASSEMENT_INTERNE']);
    component.onMesureToggle('FORMATION', true);
    expect(component.contenuMesures()).toContain('FORMATION');
    component.onMesureToggle('RECLASSEMENT_INTERNE', false);
    expect(component.contenuMesures()).toEqual(['FORMATION']);
    expect(component.isMesureChecked('FORMATION')).toBe(true);
    expect(component.isMesureChecked('RECLASSEMENT_INTERNE')).toBe(false);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST + résultat + snackbar succès + triggerRefresh', () => {
    component.tailleEntrepriseSalaries.set(80);
    component.nombreLicenciementsEnvisages.set(12);
    component.periodeJours.set(30);
    component.modeAdoption.set('ACCORD_COLLECTIF_MAJORITAIRE');
    component.csaeConsulteAvis.set('FAVORABLE');
    component.dreetsStatut.set('VALIDE');
    component.dateProjet.set('2026-04-25');
    component.contenuMesures.set(['RECLASSEMENT_INTERNE', 'FORMATION']);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.tailleEntrepriseSalaries).toBe(80);
    expect(req.request.body.contenuMesures).toEqual(['RECLASSEMENT_INTERNE', 'FORMATION']);
    expect(req.request.body.dateNotificationDreets).toBeUndefined();
    req.flush(response());

    expect(component.result()!.scoreConformite).toBe(80);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('PSE analysé', 'OK', jasmine.any(Object));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.tailleEntrepriseSalaries.set(80);
    component.nombreLicenciementsEnvisages.set(12);
    component.periodeJours.set(30);
    component.modeAdoption.set('ACCORD_COLLECTIF_MAJORITAIRE');
    component.csaeConsulteAvis.set('FAVORABLE');
    component.dreetsStatut.set('VALIDE');
    component.dateProjet.set('2026-04-25');
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

  it('coherenceAlerts.DATE_PROJET présent si écart IA > 30 jours', () => {
    component.aiData = { dateLicenciement: '2026-01-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // Pré-fill mit '2026-01-01'. Avocat saisit '2026-04-15' → 104 j d'écart.
    component.onDateProjetChange('2026-04-15');

    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_PROJET).toBeDefined();
    expect(alerts.DATE_PROJET!.field).toBe('DATE_PROJET');
    expect(alerts.DATE_PROJET!.source).toBe('IA');
    expect(alerts.DATE_PROJET!.expectedDisplay).toBe('2026-01-01');
  });

  it('coherenceAlerts.DATE_PROJET absent si écart ≤ 30 jours', () => {
    component.aiData = { dateLicenciement: '2026-04-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // Pré-fill mit '2026-04-01' → onDateProjetChange efface badge mais ≤ 30 j → pas d'alerte.
    component.onDateProjetChange('2026-04-15');
    expect(component.coherenceAlerts().DATE_PROJET).toBeUndefined();
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { dateLicenciement: '2026-01-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onDateProjetChange('2026-04-15');
    expect(component.coherenceAlerts().DATE_PROJET).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DATE_PROJET).toBeUndefined();
  });

  it('coherenceAlerts multi-sources F96 + IA convergents → MULTI', () => {
    component.aiData = { dateLicenciement: '2026-01-01' } as TravailExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Date projet PSE',
        statut: 'NON_COMPLIANT',
        critereCode: 'PSE_DATE_PROJET',
        expectedValue: '2026-01-01',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // Avocat saisit '2026-04-15' → divergence IA + F96 même valeur.
    component.onDateProjetChange('2026-04-15');

    const alert = component.coherenceAlerts().DATE_PROJET;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.reason).toContain(' ET ');
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    const newAi = { dateLicenciement: '2026-05-01' } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.dateProjet()).toBe('2026-05-01');
    expect(component.provenanceDateProjet()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onDateProjetChange('2026-03-20');
    expect(component.provenanceDateProjet()).toBeNull();

    const newAi = { dateLicenciement: '2026-05-01' } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.dateProjet()).toBe('2026-03-20');
    expect(component.provenanceDateProjet()).toBeNull();
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
    expect(component.bannerClass('VALIDE')).toContain('pse-banner--info');
    expect(component.bannerClass('CONTESTABLE')).toContain('pse-banner--warning');
    expect(component.bannerClass('NUL')).toContain('pse-banner--critical');
    expect(component.bannerClass(null)).toContain('pse-banner--info');
  });

  it('critereLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.critereLabel('CSE_CONSULTE')).toContain('CSE consulté');
    expect(component.critereLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.critereLabel(null)).toBe('');
  });
});
