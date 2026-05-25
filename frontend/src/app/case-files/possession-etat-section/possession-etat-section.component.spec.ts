import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { PossessionEtatSectionComponent } from './possession-etat-section.component';
import { PossessionEtatResponse } from '../../core/models/possession-etat.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('PossessionEtatSectionComponent', () => {
  let component: PossessionEtatSectionComponent;
  let fixture: ComponentFixture<PossessionEtatSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/possession-etat-analysis';

  function response(overrides: Partial<PossessionEtatResponse> = {}): PossessionEtatResponse {
    return {
      caseFileId: 'case-1',
      verdictRecevabilite: 'ELEVEE',
      dispositifApplicable: 'CONSTAT_NOTAIRE',
      scoreRecevabilite: 95,
      dureePossessionAnnees: 6,
      delaiContestationActeAns: 5,
      delaiContestationCessationAns: 10,
      criteresRemplis: ['Tractatus', 'Fama'],
      criteresManquants: [],
      baseJuridique: 'Art. 311-1 + 311-2 + 317 Cciv',
      formule: 'Verdict ELEVEE → CONSTAT_NOTAIRE',
      messages: ['Possession solidement caractérisée'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        PossessionEtatSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(PossessionEtatSectionComponent);
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

    expect(component.result()!.scoreRecevabilite).toBe(95);
    expect(component.showForm()).toBe(false);
    expect(component.provenanceTractatus()).toBeNull();
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

  it('pré-fill IA depuis possessionEtatConforme5AnsDetected=true → coche faisceau', () => {
    component.aiData = {
      possessionEtatConforme5AnsDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.tractatus()).toBe(true);
    expect(component.provenanceTractatus()).toBe('IA');
    expect(component.fama()).toBe(true);
    expect(component.provenanceFama()).toBe('IA');
    expect(component.continueCondition()).toBe(true);
    expect(component.provenanceContinue()).toBe('IA');
    expect(component.paisible()).toBe(true);
    expect(component.provenancePaisible()).toBe('IA');
    expect(component.nonEquivoque()).toBe(true);
    expect(component.provenanceNonEquivoque()).toBe('IA');
    // Nomen reste false (facultatif depuis 2005, non pré-rempli).
    expect(component.nomen()).toBe(false);
    expect(component.provenanceNomen()).toBeNull();
  });

  it('pré-fill sans aiData → aucun pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.tractatus()).toBe(false);
    expect(component.provenanceTractatus()).toBeNull();
    expect(component.fama()).toBe(false);
  });

  it('pré-fill non déclenché si possessionEtatConforme5AnsDetected=false', () => {
    component.aiData = {
      possessionEtatConforme5AnsDetected: false,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.tractatus()).toBe(false);
    expect(component.provenanceTractatus()).toBeNull();
  });

  it('onTractatusChange efface le badge IA', () => {
    component.aiData = {
      possessionEtatConforme5AnsDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceTractatus()).toBe('IA');
    component.onTractatusChange(false);
    expect(component.tractatus()).toBe(false);
    expect(component.provenanceTractatus()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true ssi dateDebut + dateFin valides + ordre cohérent', () => {
    component.dateDebutPossession.set('2018-04-15');
    component.dateFinPossession.set('2024-04-15');
    expect(component.formValid()).toBe(true);

    component.dateDebutPossession.set('2024-04-15');
    component.dateFinPossession.set('2018-04-15');
    expect(component.formValid()).toBe(false);

    component.dateDebutPossession.set('2018-04-15');
    component.dateFinPossession.set(null);
    expect(component.formValid()).toBe(false);

    component.dateDebutPossession.set(null);
    component.dateFinPossession.set('2024-04-15');
    expect(component.formValid()).toBe(false);
  });

  it('dureePossessionAnnees calculé live (signal computed)', () => {
    component.dateDebutPossession.set('2018-04-15');
    component.dateFinPossession.set('2024-04-15');
    expect(component.dureePossessionAnnees()).toBe(6);

    component.dateFinPossession.set('2020-04-15');
    expect(component.dureePossessionAnnees()).toBe(2);

    component.dateDebutPossession.set(null);
    expect(component.dureePossessionAnnees()).toBeNull();
  });

  it('dureePossessionAnnees null si dates incohérentes', () => {
    component.dateDebutPossession.set('2024-04-15');
    component.dateFinPossession.set('2018-04-15');
    expect(component.dureePossessionAnnees()).toBeNull();
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST + résultat + snackbar succès + dashboardRefresh', () => {
    const refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    (component as any).dashboardRefresh = refreshSpy;

    component.dateDebutPossession.set('2018-04-15');
    component.dateFinPossession.set('2024-04-15');
    component.tractatus.set(true);
    component.fama.set(true);
    component.continueCondition.set(true);
    component.paisible.set(true);
    component.nonEquivoque.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateDebutPossession).toBe('2018-04-15');
    expect(req.request.body.dateFinPossession).toBe('2024-04-15');
    expect(req.request.body.tractatus).toBe(true);
    expect(req.request.body.fama).toBe(true);
    expect(req.request.body.nomen).toBe(false);
    expect(req.request.body.continueCondition).toBe(true);
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.result()!.dispositifApplicable).toBe('CONSTAT_NOTAIRE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Possession d\'état analysée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.dateDebutPossession.set('2018-04-15');
    component.dateFinPossession.set('2024-04-15');
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

  it('coherenceAlerts.TRACTATUS présent si IA diverge de saisie', () => {
    component.aiData = {
      possessionEtatConforme5AnsDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit tractatus=true. Avocat décoche → divergence IA.
    component.onTractatusChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.TRACTATUS).toBeDefined();
    expect(alerts.TRACTATUS!.field).toBe('TRACTATUS');
    expect(alerts.TRACTATUS!.source).toBe('IA');
    expect(alerts.TRACTATUS!.expectedDisplay).toContain('Tractatus');
  });

  it('coherenceAlerts.CONTINUE présent si IA diverge', () => {
    component.aiData = {
      possessionEtatConforme5AnsDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onContinueChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.CONTINUE).toBeDefined();
    expect(alerts.CONTINUE!.source).toBe('IA');
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = {
      possessionEtatConforme5AnsDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onPaisibleChange(false);
    expect(component.coherenceAlerts().PAISIBLE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().PAISIBLE).toBeUndefined();
  });

  // ============================================================
  // Délai contestation chip alert
  // ============================================================

  it('delaiContestationAlert null si CONSTAT_NOTAIRE', () => {
    component.result.set(response({ dispositifApplicable: 'CONSTAT_NOTAIRE' }));
    expect(component.delaiContestationAlert()).toBeNull();
  });

  it('delaiContestationAlert warning si PREUVE_JUSTICE', () => {
    component.result.set(response({ dispositifApplicable: 'PREUVE_JUSTICE' }));
    expect(component.delaiContestationAlert()).toBe('warning');
  });

  it('delaiContestationAlert critical si AUCUN', () => {
    component.result.set(response({
      dispositifApplicable: 'AUCUN',
      verdictRecevabilite: 'FAIBLE',
    }));
    expect(component.delaiContestationAlert()).toBe('critical');
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = {
      possessionEtatConforme5AnsDetected: true,
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.tractatus()).toBe(true);
    expect(component.provenanceTractatus()).toBe('IA');
  });

  it('ngOnChanges(aiData) ne réécrit pas un champ déjà coché par l\'avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Avocat coche tractatus manuellement (provenance reste null).
    component.onTractatusChange(true);
    expect(component.provenanceTractatus()).toBeNull();
    expect(component.tractatus()).toBe(true);

    const newAi = {
      possessionEtatConforme5AnsDetected: true,
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    // tractatus = true déjà saisi par l'avocat — la condition
    // `!this.tractatus()` est false donc l'IA ne réécrit pas. Provenance
    // reste null (saisie avocat préservée).
    expect(component.tractatus()).toBe(true);
    expect(component.provenanceTractatus()).toBeNull();
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
    expect(component.bannerClass('ELEVEE')).toContain('pos-etat-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('pos-etat-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('pos-etat-banner--critical');
    expect(component.bannerClass(null)).toContain('pos-etat-banner--info');
  });

  it('dispositifLabel renvoie le libellé humain', () => {
    expect(component.dispositifLabel('CONSTAT_NOTAIRE')).toContain('notaire');
    expect(component.dispositifLabel('PREUVE_JUSTICE')).toContain('justice');
    expect(component.dispositifLabel('AUCUN')).toContain('Aucun');
    expect(component.dispositifLabel(null)).toBe('');
  });
});
