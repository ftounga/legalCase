import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { DivorceFauteSectionComponent } from './divorce-faute-section.component';
import { DivorceFauteResponse } from '../../core/models/divorce-faute.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('DivorceFauteSectionComponent', () => {
  let component: DivorceFauteSectionComponent;
  let fixture: ComponentFixture<DivorceFauteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/divorce-faute';

  function defaultResponse(): DivorceFauteResponse {
    return {
      caseFileId: 'case-1',
      fautesInvoquees: ['ADULTERE', 'VIOLENCES'],
      preuvesDocumentaires: true,
      tortsAdverseInvoques: false,
      dureeMariageAnnees: 12,
      revenusAnnuelsDemandeurEur: 24000,
      revenusAnnuelsDefendeurEur: 60000,
      dateDepotAssignation: '2026-04-25',
      country: 'FRANCE',
      nombreFautesInvoquees: 2,
      solidariteeFautesOk: true,
      risqueTortsPartages: false,
      scoreGlobal: 72,
      verdictProbabiliteDivorceFaute: 'ELEVEE',
      verdictTortsEstimes: 'EXCLUSIF_DEFENDEUR',
      damagesInteretsArt266FourchetteMin: 2000,
      damagesInteretsArt266FourchetteMax: 8000,
      prestationCompensatoireFourchetteMin: 24000,
      prestationCompensatoireFourchetteMax: 60000,
      criteresNonRemplis: [],
      formule: 'score = 72/100',
      baseJuridique: 'Art. 242-246 + 266 + 270 Cciv',
      messages: ['Charge de la preuve sur le demandeur'],
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        DivorceFauteSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DivorceFauteSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + form validators
  // ---------------------------------------------------------------------------

  it('mount sans erreur (FRANCE)', () => {
    expect(component).toBeTruthy();
    expect(component.fautesOptions.length).toBe(8);
  });

  it('formValid faux si fautesInvoquees vide', () => {
    component.fautesInvoquees.set([]);
    component.dureeMariageAnnees.set(10);
    component.revenusAnnuelsDemandeurEur.set(20000);
    component.revenusAnnuelsDefendeurEur.set(50000);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si revenus null ou négatif', () => {
    component.fautesInvoquees.set(['ADULTERE']);
    component.dureeMariageAnnees.set(10);
    component.revenusAnnuelsDemandeurEur.set(null);
    component.revenusAnnuelsDefendeurEur.set(50000);
    expect(component.formValid()).toBe(false);

    component.revenusAnnuelsDemandeurEur.set(-10);
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai si au moins 1 faute + revenus ≥ 0 + durée ≥ 0', () => {
    component.fautesInvoquees.set(['ADULTERE']);
    component.dureeMariageAnnees.set(0);
    component.revenusAnnuelsDemandeurEur.set(0);
    component.revenusAnnuelsDefendeurEur.set(0);
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  it('GET 200 → form masqué, valeurs persistées affichées, pas de badge IA', () => {
    component.aiData = {
      revenusAnnuelsDemandeurEur: 9999,
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse());

    expect(component.result()!.scoreGlobal).toBe(72);
    expect(component.showForm()).toBe(false);
    expect(component.revenusAnnuelsDemandeurEur()).toBe(24000); // valeur persistée
    expect(component.provenanceRevenusDemandeur()).toBeNull();
  });

  it('GET 404 → reste en mode formulaire ; pré-fill IA appliqué', () => {
    component.aiData = {
      revenusAnnuelsDemandeurEur: 22000,
      revenusAnnuelsDefendeurEur: 55000,
      dureeMariageAnnees: 10,
      dateDepotAssignation: '2026-03-01',
      fautesDetectees: ['ADULTERE', 'VIOLENCES'],
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.showForm()).toBe(true);
    expect(component.revenusAnnuelsDemandeurEur()).toBe(22000);
    expect(component.revenusAnnuelsDefendeurEur()).toBe(55000);
    expect(component.dureeMariageAnnees()).toBe(10);
    expect(component.dateDepotAssignation()).toBe('2026-03-01');
    expect(component.fautesInvoquees()).toEqual(['ADULTERE', 'VIOLENCES']);
    expect(component.provenanceFautesInvoquees()).toBe('IA');
    expect(component.provenanceDureeMariage()).toBe('IA');
    expect(component.provenanceRevenusDemandeur()).toBe('IA');
    expect(component.provenanceRevenusDefendeur()).toBe('IA');
    expect(component.provenanceDateDepot()).toBe('IA');
  });

  it('calculate() POST → résultat affiché + snackbar succès + dashboardRefresh', () => {
    component.fautesInvoquees.set(['ADULTERE']);
    component.preuvesDocumentaires.set(true);
    component.tortsAdverseInvoques.set(false);
    component.dureeMariageAnnees.set(12);
    component.revenusAnnuelsDemandeurEur.set(24000);
    component.revenusAnnuelsDefendeurEur.set(60000);
    component.dateDepotAssignation.set('2026-04-25');

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      fautesInvoquees: ['ADULTERE'],
      preuvesDocumentaires: true,
      tortsAdverseInvoques: false,
      dureeMariageAnnees: 12,
      revenusAnnuelsDemandeurEur: 24000,
      revenusAnnuelsDefendeurEur: 60000,
      dateDepotAssignation: '2026-04-25',
    });
    req.flush(defaultResponse());

    expect(component.result()!.verdictProbabiliteDivorceFaute).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse divorce pour faute calculée',
      'OK',
      jasmine.any(Object),
    );
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur 400 → snackbar rouge', () => {
    component.fautesInvoquees.set(['ADULTERE']);
    component.dureeMariageAnnees.set(10);
    component.revenusAnnuelsDemandeurEur.set(20000);
    component.revenusAnnuelsDefendeurEur.set(50000);
    component.calculate();

    httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL)
      .flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.fautesInvoquees.set([]);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Provenance handlers (badge IA effacé sur edit manuel)
  // ---------------------------------------------------------------------------

  it('onRevenusDemandeurChange efface le badge IA', () => {
    component.aiData = {
      revenusAnnuelsDemandeurEur: 24000,
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceRevenusDemandeur()).toBe('IA');
    component.onRevenusDemandeurChange(30000);
    expect(component.revenusAnnuelsDemandeurEur()).toBe(30000);
    expect(component.provenanceRevenusDemandeur()).toBeNull();
  });

  it('onFautesChange efface le badge IA fautes', () => {
    component.aiData = {
      fautesDetectees: ['ADULTERE'],
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceFautesInvoquees()).toBe('IA');
    component.onFautesChange(['VIOLENCES']);
    expect(component.fautesInvoquees()).toEqual(['VIOLENCES']);
    expect(component.provenanceFautesInvoquees()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.REVENUS_DEMANDEUR présent si divergence > 10 %', () => {
    component.aiData = {
      revenusAnnuelsDemandeurEur: 20000,
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onRevenusDemandeurChange(35000); // écart 75 %

    const alerts = component.coherenceAlerts();
    expect(alerts.REVENUS_DEMANDEUR).toBeDefined();
    expect(alerts.REVENUS_DEMANDEUR!.field).toBe('REVENUS_DEMANDEUR');
    expect(alerts.REVENUS_DEMANDEUR!.expectedDisplay).toContain('20');
  });

  it('coherenceAlerts.REVENUS_DEMANDEUR absent si écart ≤ 10 %', () => {
    component.aiData = {
      revenusAnnuelsDemandeurEur: 20000,
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onRevenusDemandeurChange(20500); // écart 2.5 %

    expect(component.coherenceAlerts().REVENUS_DEMANDEUR).toBeUndefined();
  });

  it('coherenceAlerts.FAUTES_INVOQUEES présent si IA détecte des fautes et avocat sélectionne autres', () => {
    component.aiData = {
      fautesDetectees: ['ADULTERE'],
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Pré-fill a posé ['ADULTERE']. Avocat change manuellement vers VIOLENCES.
    component.onFautesChange(['VIOLENCES']);
    const alerts = component.coherenceAlerts();
    expect(alerts.FAUTES_INVOQUEES).toBeDefined();
    expect(alerts.FAUTES_INVOQUEES!.expectedDisplay).toContain('ADULTERE');
  });

  it('alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = {
      revenusAnnuelsDemandeurEur: 20000,
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onRevenusDemandeurChange(35000);
    expect(component.coherenceAlerts().REVENUS_DEMANDEUR).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().REVENUS_DEMANDEUR).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Gate workspaceCountry
  // ---------------------------------------------------------------------------

  it('gate BELGIQUE → form non rendu, GET non appelé', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    // Aucun GET attendu (load() n'est appelé que si isFrance()).
    httpMock.expectNone(BASE_URL);
    expect(component.isFrance()).toBe(false);
  });

  it('gate FRANCE → load() appelé', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.isFrance()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges + UI helpers
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = {
      revenusAnnuelsDemandeurEur: 22000,
      revenusAnnuelsDefendeurEur: 55000,
      dureeMariageAnnees: 8,
    } as unknown as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({
      aiData: new SimpleChange(null, newAi, false),
    });

    expect(component.revenusAnnuelsDemandeurEur()).toBe(22000);
    expect(component.dureeMariageAnnees()).toBe(8);
    expect(component.provenanceRevenusDemandeur()).toBe('IA');
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

  it('verdictBannerClass renvoie la classe attendue (strong/medium/weak)', () => {
    expect(component.verdictBannerClass('ELEVEE')).toContain('strong');
    expect(component.verdictBannerClass('MOYENNE')).toContain('medium');
    expect(component.verdictBannerClass('FAIBLE')).toContain('weak');
  });

  it('verdictTortsLabel mappe les 3 verdicts torts', () => {
    expect(component.verdictTortsLabel('EXCLUSIF_DEFENDEUR')).toContain('exclusif');
    expect(component.verdictTortsLabel('PARTAGES')).toContain('partagés');
    expect(component.verdictTortsLabel('IMPREDICTIBLE')).toContain('Imprédictible');
  });
});
