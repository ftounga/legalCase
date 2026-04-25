import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { PacsDissolutionSectionComponent } from './pacs-dissolution-section.component';
import { PacsDissolutionResponse } from '../../core/models/pacs-dissolution.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('PacsDissolutionSectionComponent', () => {
  let component: PacsDissolutionSectionComponent;
  let fixture: ComponentFixture<PacsDissolutionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/pacs-dissolution';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function defaultResponse(): PacsDissolutionResponse {
    return {
      caseFileId: 'case-1',
      dateConclusionPacs: '2018-04-15',
      modeDissolution: 'DECLARATION_UNILATERALE',
      dateDissolution: '2026-04-25',
      dureeUnionAnnees: 8,
      regimeBiens: 'INDIVISION_PAR_DEFAUT',
      patrimoineCommunSignificatif: true,
      creancesAlleguees: ['CONTRIBUTION_DESEQUILIBRE', 'INVESTISSEMENT_BIEN_PROPRE'],
      enfantsCommuns: 1,
      dateNotificationPartenaire: '2026-04-20',
      dissolutionValide: true,
      delaiNotificationOk: true,
      dureeUnionEligibleCreances: true,
      scoreCreancesProbables: 65,
      verdictRecommandation: 'MEDIATION_OU_JAF',
      creancesPotentielleVisibles: ['CONTRIBUTION_DESEQUILIBRE', 'INVESTISSEMENT_BIEN_PROPRE'],
      delaiPrescriptionAnnees: 5,
      baseJuridique: 'Art. 515-7 + 2224 Cciv',
      formule: 'score = 65/100',
      messages: ['Médiation préalable recommandée avant saisine JAF.'],
      country: 'FRANCE',
    };
  }

  /**
   * Absorbe la requête source-explanations émise par `loadSourceExplanations()`
   * dans `ngOnInit` (fail-open). Empêche `httpMock.verify()` de crasher.
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
        PacsDissolutionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PacsDissolutionSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + options
  // ---------------------------------------------------------------------------

  it('mount sans erreur (FRANCE)', () => {
    expect(component).toBeTruthy();
    expect(component.modesOptions.length).toBe(5);
    expect(component.regimesOptions.length).toBe(3);
    expect(component.creancesOptions.length).toBe(5);
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  function setMinimalValidForm(): void {
    component.dateConclusionPacs.set('2018-04-15');
    component.modeDissolution.set('DECLARATION_UNILATERALE');
    component.dateDissolution.set('2026-04-25');
    component.dureeUnionAnnees.set(8);
    component.regimeBiens.set('INDIVISION_PAR_DEFAUT');
    component.patrimoineCommunSignificatif.set(false);
    component.creancesAlleguees.set(['AUCUNE']);
    component.enfantsCommuns.set(0);
    component.dateNotificationPartenaire.set('2026-04-20');
  }

  it('formValid faux si dateConclusionPacs manquante', () => {
    setMinimalValidForm();
    component.dateConclusionPacs.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si modeDissolution null', () => {
    setMinimalValidForm();
    component.modeDissolution.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si dureeUnionAnnees négative', () => {
    setMinimalValidForm();
    component.dureeUnionAnnees.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si regimeBiens null', () => {
    setMinimalValidForm();
    component.regimeBiens.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si creancesAlleguees vide', () => {
    setMinimalValidForm();
    component.creancesAlleguees.set([]);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si enfantsCommuns null', () => {
    setMinimalValidForm();
    component.enfantsCommuns.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si dateNotificationPartenaire absente', () => {
    setMinimalValidForm();
    component.dateNotificationPartenaire.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai sur saisie minimale complète', () => {
    setMinimalValidForm();
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  it('GET 200 → form masqué + valeurs persistées + pas de badge IA', () => {
    component.aiData = {
      dateConclusionPacs: '2015-01-01',
      modeDissolutionPacsDetecte: 'DECES',
    } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse());
    expectSourceExplanationCall();

    expect(component.result()!.scoreCreancesProbables).toBe(65);
    expect(component.showForm()).toBe(false);
    expect(component.dateConclusionPacs()).toBe('2018-04-15'); // persisté
    expect(component.modeDissolution()).toBe('DECLARATION_UNILATERALE');
    expect(component.provenanceDateConclusionPacs()).toBeNull();
    expect(component.provenanceModeDissolution()).toBeNull();
  });

  it('GET 404 → mode formulaire + pré-fill IA appliqué (5 fields)', () => {
    component.aiData = {
      dateConclusionPacs: '2018-04-15',
      modeDissolutionPacsDetecte: 'DECLARATION_UNILATERALE',
      regimeBiensPacsDetecte: 'INDIVISION_PAR_DEFAUT',
      creancesAllegueesDetectees: ['CONTRIBUTION_DESEQUILIBRE'],
      patrimoineCommunSignificatifDetecte: true,
    } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.showForm()).toBe(true);
    expect(component.dateConclusionPacs()).toBe('2018-04-15');
    expect(component.modeDissolution()).toBe('DECLARATION_UNILATERALE');
    expect(component.regimeBiens()).toBe('INDIVISION_PAR_DEFAUT');
    expect(component.creancesAlleguees()).toEqual(['CONTRIBUTION_DESEQUILIBRE']);
    expect(component.patrimoineCommunSignificatif()).toBe(true);
    expect(component.provenanceDateConclusionPacs()).toBe('IA');
    expect(component.provenanceModeDissolution()).toBe('IA');
    expect(component.provenanceRegimeBiens()).toBe('IA');
    expect(component.provenanceCreancesAlleguees()).toBe('IA');
    expect(component.provenancePatrimoineCommun()).toBe('IA');
  });

  it('GET 404 + aiData absent → no-op gracieux (pas de pré-fill)', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.showForm()).toBe(true);
    expect(component.dateConclusionPacs()).toBeNull();
    expect(component.modeDissolution()).toBeNull();
    expect(component.creancesAlleguees()).toEqual([]);
  });

  it('calculate() POST → result + snackbar succès + dashboardRefresh', () => {
    setMinimalValidForm();
    component.creancesAlleguees.set(['CONTRIBUTION_DESEQUILIBRE']);

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateConclusionPacs: '2018-04-15',
      modeDissolution: 'DECLARATION_UNILATERALE',
      dateDissolution: '2026-04-25',
      dureeUnionAnnees: 8,
      regimeBiens: 'INDIVISION_PAR_DEFAUT',
      patrimoineCommunSignificatif: false,
      creancesAlleguees: ['CONTRIBUTION_DESEQUILIBRE'],
      enfantsCommuns: 0,
      dateNotificationPartenaire: '2026-04-20',
    });
    req.flush(defaultResponse());

    expect(component.result()!.verdictRecommandation).toBe('MEDIATION_OU_JAF');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse dissolution PACS calculée',
      'OK',
      jasmine.any(Object),
    );
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur 400 → snackbar rouge', () => {
    setMinimalValidForm();
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
    component.modeDissolution.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Provenance handlers (badge IA effacé sur edit manuel)
  // ---------------------------------------------------------------------------

  it('onModeDissolutionChange efface le badge IA', () => {
    component.aiData = { modeDissolutionPacsDetecte: 'DECLARATION_UNILATERALE' } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceModeDissolution()).toBe('IA');
    component.onModeDissolutionChange('DECLARATION_CONJOINTE');
    expect(component.modeDissolution()).toBe('DECLARATION_CONJOINTE');
    expect(component.provenanceModeDissolution()).toBeNull();
  });

  it('onRegimeBiensChange efface le badge IA', () => {
    component.aiData = { regimeBiensPacsDetecte: 'SEPARATION_BIENS' } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceRegimeBiens()).toBe('IA');
    component.onRegimeBiensChange('INDIVISION_AMENAGEE');
    expect(component.regimeBiens()).toBe('INDIVISION_AMENAGEE');
    expect(component.provenanceRegimeBiens()).toBeNull();
  });

  it('onCreancesAlleguueesChange efface le badge IA', () => {
    component.aiData = {
      creancesAllegueesDetectees: ['CONTRIBUTION_DESEQUILIBRE'],
    } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceCreancesAlleguees()).toBe('IA');
    component.onCreancesAlleguueesChange(['AUCUNE']);
    expect(component.creancesAlleguees()).toEqual(['AUCUNE']);
    expect(component.provenanceCreancesAlleguees()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.MODE_DISSOLUTION si IA dit autre chose', () => {
    component.aiData = { modeDissolutionPacsDetecte: 'DECES' } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onModeDissolutionChange('DECLARATION_UNILATERALE');

    const alerts = component.coherenceAlerts();
    expect(alerts.MODE_DISSOLUTION).toBeDefined();
    expect(alerts.MODE_DISSOLUTION!.field).toBe('MODE_DISSOLUTION');
    expect(alerts.MODE_DISSOLUTION!.source).toBe('IA');
    expect(alerts.MODE_DISSOLUTION!.expectedDisplay).toContain('Décès');
  });

  it('coherenceAlerts.CREANCES_ALLEGUEES divergence ensembliste IA', () => {
    component.aiData = {
      creancesAllegueesDetectees: ['CONTRIBUTION_DESEQUILIBRE', 'ENRICHISSEMENT_INJUSTE'],
    } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onCreancesAlleguueesChange(['AUCUNE']);

    const alert = component.coherenceAlerts().CREANCES_ALLEGUEES;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('CREANCES_ALLEGUEES');
    expect(alert!.source).toBe('IA');
  });

  it('coherenceAlerts.CREANCES multi-source IA + F96 + PIECE_MANQUANTE', () => {
    component.aiData = {
      creancesAllegueesDetectees: ['CONTRIBUTION_DESEQUILIBRE'],
    } as Partial<FamilleExtractedData>;
    component.procedureChecks = [
      {
        id: 'c1', ordre: 1, description: 'Créances', statut: 'NON_COMPLIANT',
        critereCode: 'FA20_CREANCES_ALLEGUEES', expectedValue: 'Contribution déséquilibre', raison: 'IA détecté',
      },
    ] as ProcedureCheck[];
    component.piecesManquantes = [
      { texte: 'Justificatif investissement', critereCode: 'JUSTIFICATIF_INVESTISSEMENT' },
    ] as PieceManquanteEntry[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onCreancesAlleguueesChange(['AUCUNE']);

    const alert = component.coherenceAlerts().CREANCES_ALLEGUEES;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.source).toBe('MULTI');
    expect(alert!.pieceTexte).toBe('Justificatif investissement');
  });

  it('coherenceAlerts.PATRIMOINE_COMMUN si IA détecte true et user à false', () => {
    component.aiData = { patrimoineCommunSignificatifDetecte: true } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Pré-fill a coché true → on simule décocheage manuel.
    component.onPatrimoineCommunChange(false);
    const alert = component.coherenceAlerts().PATRIMOINE_COMMUN;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('PATRIMOINE_COMMUN');
    expect(alert!.expectedDisplay).toContain('Patrimoine');
  });

  it('alertes masquées après résultat (showForm=false, anti-bug SF-IA-03-12)', () => {
    component.aiData = { modeDissolutionPacsDetecte: 'DECES' } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onModeDissolutionChange('DECLARATION_UNILATERALE');
    expect(component.coherenceAlerts().MODE_DISSOLUTION).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().MODE_DISSOLUTION).toBeUndefined();
  });

  it('alertBadgeLabel reflète le type de source (IA / MULTI)', () => {
    const alertIA = {
      field: 'MODE_DISSOLUTION' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: 'Décès',
      reason: 'r',
    };
    const alertMulti = {
      ...alertIA, source: 'MULTI' as const,
      contributors: ['IA' as const, 'F96' as const],
    };
    expect(component.alertBadgeLabel(alertIA)).toContain('Incohérence détectée');
    expect(component.alertBadgeLabel(alertIA)).toContain('Décès');
    expect(component.alertBadgeLabel(alertMulti)).toContain('multiple');
  });

  it('alertTooltip ajoute "Contredit" si > 1 contributor', () => {
    const alertSingle = {
      field: 'MODE_DISSOLUTION' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: 'Décès',
      reason: 'simple',
    };
    const alertMulti = {
      ...alertSingle, source: 'MULTI' as const,
      contributors: ['IA' as const, 'F96' as const],
    };
    expect(component.alertTooltip(alertSingle)).toBe('simple');
    expect(component.alertTooltip(alertMulti)).toContain('Contredit');
  });

  it('explanationFor retourne [] si aucune source explanation chargée (fail-open)', () => {
    expect(component.explanationFor('DATE_CONCLUSION_PACS')).toEqual([]);
    expect(component.explanationFor('MODE_DISSOLUTION')).toEqual([]);
    expect(component.explanationFor('REGIME_BIENS')).toEqual([]);
    expect(component.explanationFor('CREANCES_ALLEGUEES')).toEqual([]);
    expect(component.explanationFor('PATRIMOINE_COMMUN')).toEqual([]);
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges — propagation des inputs
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi: Partial<FamilleExtractedData> = {
      dateConclusionPacs: '2018-04-15',
      modeDissolutionPacsDetecte: 'DECLARATION_UNILATERALE',
    };
    component.aiData = newAi;
    component.ngOnChanges({
      aiData: new SimpleChange(null, newAi, false),
    });

    expect(component.dateConclusionPacs()).toBe('2018-04-15');
    expect(component.modeDissolution()).toBe('DECLARATION_UNILATERALE');
    expect(component.provenanceDateConclusionPacs()).toBe('IA');
  });

  // ---------------------------------------------------------------------------
  // Gate workspaceCountry
  // ---------------------------------------------------------------------------

  it('gate BELGIQUE → form non rendu, GET non appelé', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
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
  // UI helpers
  // ---------------------------------------------------------------------------

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

  it('verdictBannerClass renvoie la classe attendue', () => {
    expect(component.verdictBannerClass('LIQUIDATION_AMIABLE')).toContain('strong');
    expect(component.verdictBannerClass('RIEN_A_FAIRE')).toContain('strong');
    expect(component.verdictBannerClass('MEDIATION_OU_JAF')).toContain('medium');
    expect(component.verdictBannerClass('CONTENTIEUX_INEVITABLE')).toContain('weak');
  });

  it('verdictChipClass renvoie la classe attendue', () => {
    expect(component.verdictChipClass('LIQUIDATION_AMIABLE')).toContain('strong');
    expect(component.verdictChipClass('CONTENTIEUX_INEVITABLE')).toContain('weak');
    expect(component.verdictChipClass('MEDIATION_OU_JAF')).toContain('medium');
  });

  it('verdictBannerIcon renvoie l\'icône Material attendue', () => {
    expect(component.verdictBannerIcon('LIQUIDATION_AMIABLE')).toBe('handshake');
    expect(component.verdictBannerIcon('MEDIATION_OU_JAF')).toBe('balance');
    expect(component.verdictBannerIcon('CONTENTIEUX_INEVITABLE')).toBe('gavel');
    expect(component.verdictBannerIcon('RIEN_A_FAIRE')).toBe('check_circle');
  });
});
