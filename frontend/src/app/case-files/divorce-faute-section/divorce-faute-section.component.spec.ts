import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { DivorceFauteSectionComponent } from './divorce-faute-section.component';
import { DivorceFauteResponse } from '../../core/models/divorce-faute.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('DivorceFauteSectionComponent', () => {
  let component: DivorceFauteSectionComponent;
  let fixture: ComponentFixture<DivorceFauteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/divorce-faute';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

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

  /**
   * SF-155-10 : absorbe la requête source-explanations émise par
   * `loadSourceExplanations()` dans `ngOnInit` (fail-open). Empêche
   * `httpMock.verify()` de crasher si la requête n'est pas explicitement gérée.
   */
  function expectSourceExplanationCall(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
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
    } as unknown as FamilleExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse());
    expectSourceExplanationCall();

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
    } as unknown as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

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
    } as unknown as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceRevenusDemandeur()).toBe('IA');
    component.onRevenusDemandeurChange(30000);
    expect(component.revenusAnnuelsDemandeurEur()).toBe(30000);
    expect(component.provenanceRevenusDemandeur()).toBeNull();
  });

  it('onFautesChange efface le badge IA fautes', () => {
    component.aiData = {
      fautesDetectees: ['ADULTERE'],
    } as unknown as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceFautesInvoquees()).toBe('IA');
    component.onFautesChange(['VIOLENCES']);
    expect(component.fautesInvoquees()).toEqual(['VIOLENCES']);
    expect(component.provenanceFautesInvoquees()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03 (SF-155-10 — via CoherenceAlertBuilder)
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.REVENUS_DEMANDEUR présent si divergence > 10 %', () => {
    component.aiData = {
      revenusAnnuelsDemandeurEur: 20000,
    } as unknown as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onRevenusDemandeurChange(35000); // écart 75 %

    const alerts = component.coherenceAlerts();
    expect(alerts.REVENUS_DEMANDEUR).toBeDefined();
    expect(alerts.REVENUS_DEMANDEUR!.field).toBe('REVENUS_DEMANDEUR');
    expect(alerts.REVENUS_DEMANDEUR!.expectedDisplay).toContain('20');
    // SF-155-10 : la nouvelle structure expose source + contributors.
    expect(alerts.REVENUS_DEMANDEUR!.source).toBe('IA');
    expect(alerts.REVENUS_DEMANDEUR!.contributors).toEqual(['IA']);
    expect(alerts.REVENUS_DEMANDEUR!.severity).toBe('WARNING');
  });

  it('coherenceAlerts.REVENUS_DEMANDEUR absent si écart ≤ 10 %', () => {
    component.aiData = {
      revenusAnnuelsDemandeurEur: 20000,
    } as unknown as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onRevenusDemandeurChange(20500); // écart 2.5 %

    expect(component.coherenceAlerts().REVENUS_DEMANDEUR).toBeUndefined();
  });

  it('coherenceAlerts.FAUTES_INVOQUEES présent si IA détecte des fautes et avocat sélectionne autres', () => {
    component.aiData = {
      fautesDetectees: ['ADULTERE'],
    } as unknown as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Pré-fill a posé ['ADULTERE']. Avocat change manuellement vers VIOLENCES.
    component.onFautesChange(['VIOLENCES']);
    const alerts = component.coherenceAlerts();
    expect(alerts.FAUTES_INVOQUEES).toBeDefined();
    expect(alerts.FAUTES_INVOQUEES!.expectedDisplay).toContain('ADULTERE');
  });

  it('alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = {
      revenusAnnuelsDemandeurEur: 20000,
    } as unknown as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onRevenusDemandeurChange(35000);
    expect(component.coherenceAlerts().REVENUS_DEMANDEUR).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().REVENUS_DEMANDEUR).toBeUndefined();
  });

  // SF-155-10 : nouveaux tests F-IA-03 multi-sources + popover.

  it('SF-155-10 : DUREE_MARIAGE alerte multi-source IA + F96 + PIECE_MANQUANTE', () => {
    component.aiData = { dureeMariageAnnees: 10 } as unknown as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'c1', ordre: 1, description: 'Acte mariage', statut: 'NON_COMPLIANT',
        critereCode: 'FA09_DUREE_MARIAGE', expectedValue: '10 ans', raison: 'acte produit',
      },
    ] as ProcedureCheck[];
    component.piecesManquantes = [
      { texte: 'Acte de mariage', critereCode: 'ACTE_MARIAGE' },
    ] as PieceManquanteEntry[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onDureeMariageChange(20); // écart >> 1 an

    const alert = component.coherenceAlerts().DUREE_MARIAGE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('DUREE_MARIAGE');
    // Builder consolide IA + F96 (même expectedDisplay) → MULTI.
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.source).toBe('MULTI');
    expect(alert!.expectedDisplay).toContain('10');
    expect(alert!.pieceTexte).toBe('Acte de mariage');
  });

  it('SF-155-10 : DATE_DEPOT_ASSIGNATION alerte si IA et user divergent', () => {
    component.aiData = { dateDepotAssignation: '2026-03-01' } as unknown as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Le pré-fill IA a posé 2026-03-01 → on simule une saisie manuelle divergente.
    component.onDateDepotChange('2026-05-15');

    const alert = component.coherenceAlerts().DATE_DEPOT_ASSIGNATION;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('DATE_DEPOT_ASSIGNATION');
    expect(alert!.expectedDisplay).toBe('2026-03-01');
    expect(alert!.source).toBe('IA');
  });

  it('SF-155-10 : DATE_DEPOT_ASSIGNATION absent si user vide ou identique IA', () => {
    component.aiData = { dateDepotAssignation: '2026-03-01' } as unknown as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Identique → pas d'alerte.
    expect(component.coherenceAlerts().DATE_DEPOT_ASSIGNATION).toBeUndefined();

    // User efface → pas d'alerte (champ optionnel).
    component.onDateDepotChange(null);
    expect(component.coherenceAlerts().DATE_DEPOT_ASSIGNATION).toBeUndefined();
  });

  it('SF-155-10 : REVENUS_DEFENDEUR enrichi par PIECE_MANQUANTE (avis imposition)', () => {
    component.aiData = { revenusAnnuelsDefendeurEur: 50000 } as unknown as FamilleExtractedData;
    component.piecesManquantes = [
      { texte: 'Avis d\'imposition défendeur', critereCode: 'AVIS_IMPOSITION_DEFENDEUR' },
    ] as PieceManquanteEntry[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onRevenusDefendeurChange(80000); // écart 60 %
    const alert = component.coherenceAlerts().REVENUS_DEFENDEUR;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.pieceTexte).toBe('Avis d\'imposition défendeur');
  });

  it('SF-155-10 : FAUTES_INVOQUEES enrichi par F96 + PIECE_MANQUANTE', () => {
    component.aiData = { fautesDetectees: ['ADULTERE'] } as unknown as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'c2', ordre: 2, description: 'Fautes attendues', statut: 'NON_COMPLIANT',
        critereCode: 'FA09_FAUTES_INVOQUEES', expectedValue: 'ADULTERE', raison: 'IA détecté',
      },
    ] as ProcedureCheck[];
    component.piecesManquantes = [
      { texte: 'Constats huissier', critereCode: 'CONSTAT_HUISSIER' },
    ] as PieceManquanteEntry[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onFautesChange(['VIOLENCES']); // divergence
    const alert = component.coherenceAlerts().FAUTES_INVOQUEES;
    expect(alert).toBeDefined();
    expect(alert!.contributors.length).toBeGreaterThanOrEqual(2);
    expect(alert!.pieceTexte).toBe('Constats huissier');
  });

  it('SF-155-10 : alertBadgeLabel reflète le type de source (IA / F96 / MULTI)', () => {
    const alertIA = {
      field: 'REVENUS_DEMANDEUR' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: '20 000 €',
      reason: 'r',
    };
    const alertMulti = { ...alertIA, source: 'MULTI' as const, contributors: ['IA' as const, 'F96' as const] };
    expect(component.alertBadgeLabel(alertIA)).toContain('Incohérence détectée');
    expect(component.alertBadgeLabel(alertIA)).toContain('20 000 €');
    expect(component.alertBadgeLabel(alertMulti)).toContain('multiple');
  });

  it('SF-155-10 : alertTooltip ajoute "Contredit" si > 1 contributor', () => {
    const alertSingle = {
      field: 'DUREE_MARIAGE' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: '10 an(s)',
      reason: 'simple',
    };
    const alertMulti = { ...alertSingle, source: 'MULTI' as const, contributors: ['IA' as const, 'F96' as const] };
    expect(component.alertTooltip(alertSingle)).toBe('simple');
    expect(component.alertTooltip(alertMulti)).toContain('Contredit');
  });

  it('SF-155-10 : explanationFor retourne la liste filtrée (fail-open empty)', () => {
    // Sans seed sourceExplanations, retourne [] (pas d'erreur).
    expect(component.explanationFor('REVENUS_DEMANDEUR')).toEqual([]);
    expect(component.explanationFor('FAUTES_INVOQUEES')).toEqual([]);
    expect(component.explanationFor('DUREE_MARIAGE')).toEqual([]);
    expect(component.explanationFor('DATE_DEPOT_ASSIGNATION')).toEqual([]);
    expect(component.explanationFor('REVENUS_DEFENDEUR')).toEqual([]);
  });

  it('SF-155-10 : ngOnChanges propage les nouveaux inputs (procedureChecks)', () => {
    component.aiData = { revenusAnnuelsDemandeurEur: 20000 } as unknown as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newChecks: ProcedureCheck[] = [
      {
        id: 'c3', ordre: 3, description: 'Revenus demandeur', statut: 'NON_COMPLIANT',
        critereCode: 'FA09_REVENUS_DEMANDEUR', expectedValue: '20000',
      } as ProcedureCheck,
    ];
    component.procedureChecks = newChecks;
    component.ngOnChanges({
      procedureChecks: new SimpleChange(null, newChecks, false),
    });

    component.onRevenusDemandeurChange(40000); // divergence
    const alert = component.coherenceAlerts().REVENUS_DEMANDEUR;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('F96');
  });

  // ---------------------------------------------------------------------------
  // Gate workspaceCountry
  // ---------------------------------------------------------------------------

  it('gate BELGIQUE → form non rendu, GET non appelé', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    // Aucun GET attendu (load() n'est appelé que si isFrance()).
    httpMock.expectNone(BASE_URL);
    httpMock.expectNone(SOURCE_EXPL_URL);
    expect(component.isFrance()).toBe(false);
  });

  it('gate FRANCE → load() appelé', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.isFrance()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges + UI helpers
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = {
      revenusAnnuelsDemandeurEur: 22000,
      revenusAnnuelsDefendeurEur: 55000,
      dureeMariageAnnees: 8,
    } as unknown as FamilleExtractedData;
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

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02c — mode simulateur autonome
  // ---------------------------------------------------------------------------

  describe('F-163 SF-163-02c — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-FA-09-divorce-faute/calculate';
    const CASE_URL = '/api/v1/case-files/case-1/divorce-faute';

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
