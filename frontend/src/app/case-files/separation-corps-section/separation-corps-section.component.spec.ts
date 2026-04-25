import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { SeparationCorpsSectionComponent } from './separation-corps-section.component';
import { SeparationCorpsResponse } from '../../core/models/separation-corps.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('SeparationCorpsSectionComponent', () => {
  let component: SeparationCorpsSectionComponent;
  let fixture: ComponentFixture<SeparationCorpsSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/separation-corps';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function defaultResponse(): SeparationCorpsResponse {
    return {
      caseFileId: 'case-1',
      modeProcedure: 'CONSENTEMENT_MUTUEL',
      dateJugementSeparationCorps: '2023-01-10',
      dateRequeteConversion: '2026-04-25',
      dureeSeparationAnnees: 3,
      consentementMutuelConversion: true,
      patrimoineCommun: true,
      enfantsMineurs: 1,
      demandeReconciliationFormulee: false,
      dureeSeparationOk: true,
      delaiConversion2AnsAtteint: true,
      conversionAutomatiquePossible: true,
      scoreEligibiliteConversion: 90,
      verdictConversion: 'POSSIBLE',
      baseJuridique: 'Art. 296 + 306 Cciv',
      formule: 'durée=3 ans, délai 2 ans atteint, consentement mutuel → POSSIBLE',
      messages: ['Conversion automatique possible — délai 2 ans atteint.'],
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
        SeparationCorpsSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SeparationCorpsSectionComponent);
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
    expect(component.modesOptions.length).toBe(4);
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  function setMinimalValidForm(): void {
    component.modeProcedure.set('CONSENTEMENT_MUTUEL');
    component.dureeSeparationAnnees.set(3);
    component.enfantsMineurs.set(0);
  }

  it('formValid faux si modeProcedure null', () => {
    setMinimalValidForm();
    component.modeProcedure.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si dureeSeparationAnnees négative', () => {
    setMinimalValidForm();
    component.dureeSeparationAnnees.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si dureeSeparationAnnees non entier', () => {
    setMinimalValidForm();
    component.dureeSeparationAnnees.set(2.5);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si enfantsMineurs null', () => {
    setMinimalValidForm();
    component.enfantsMineurs.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si enfantsMineurs non entier', () => {
    setMinimalValidForm();
    component.enfantsMineurs.set(1.7);
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai sur saisie minimale (dates nullable)', () => {
    setMinimalValidForm();
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  it('GET 200 → form masqué + valeurs persistées + pas de badge IA', () => {
    component.aiData = {
      patrimoineCommun: false,
      dateSeparation: '2024-06-01',
    } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse());
    expectSourceExplanationCall();

    expect(component.result()!.scoreEligibiliteConversion).toBe(90);
    expect(component.showForm()).toBe(false);
    expect(component.modeProcedure()).toBe('CONSENTEMENT_MUTUEL');
    expect(component.dateJugementSeparationCorps()).toBe('2023-01-10'); // persisté
    expect(component.patrimoineCommun()).toBe(true); // persisté (pas l'IA)
    expect(component.provenanceDateJugementSeparation()).toBeNull();
    expect(component.provenancePatrimoineCommun()).toBeNull();
  });

  it('GET 404 → mode formulaire + pré-fill IA appliqué (2 fields)', () => {
    component.aiData = {
      dateSeparation: '2023-01-10',
      patrimoineCommun: true,
    } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.showForm()).toBe(true);
    expect(component.dateJugementSeparationCorps()).toBe('2023-01-10');
    expect(component.patrimoineCommun()).toBe(true);
    expect(component.provenanceDateJugementSeparation()).toBe('IA');
    expect(component.provenancePatrimoineCommun()).toBe('IA');
  });

  it('GET 404 + aiData absent → no-op gracieux (pas de pré-fill)', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.showForm()).toBe(true);
    expect(component.dateJugementSeparationCorps()).toBeNull();
    expect(component.patrimoineCommun()).toBe(false);
    expect(component.provenanceDateJugementSeparation()).toBeNull();
    expect(component.provenancePatrimoineCommun()).toBeNull();
  });

  it('calculate() POST → result + snackbar succès + dashboardRefresh', () => {
    setMinimalValidForm();
    component.dateJugementSeparationCorps.set('2023-01-10');
    component.dateRequeteConversion.set('2026-04-25');
    component.consentementMutuelConversion.set(true);
    component.patrimoineCommun.set(true);
    component.demandeReconciliationFormulee.set(false);
    component.enfantsMineurs.set(1);

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      modeProcedure: 'CONSENTEMENT_MUTUEL',
      dateJugementSeparationCorps: '2023-01-10',
      dateRequeteConversion: '2026-04-25',
      dureeSeparationAnnees: 3,
      consentementMutuelConversion: true,
      patrimoineCommun: true,
      enfantsMineurs: 1,
      demandeReconciliationFormulee: false,
    });
    req.flush(defaultResponse());

    expect(component.result()!.verdictConversion).toBe('POSSIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse séparation de corps calculée',
      'OK',
      jasmine.any(Object),
    );
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() body : dates null si non saisies', () => {
    setMinimalValidForm();
    // dates pas saisies → null
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateJugementSeparationCorps).toBeNull();
    expect(req.request.body.dateRequeteConversion).toBeNull();
    req.flush(defaultResponse());
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
    component.modeProcedure.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Provenance handlers (badge IA effacé sur edit manuel)
  // ---------------------------------------------------------------------------

  it('onDateJugementSeparationChange efface le badge IA', () => {
    component.aiData = { dateSeparation: '2023-01-10' } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceDateJugementSeparation()).toBe('IA');
    component.onDateJugementSeparationChange('2024-12-15');
    expect(component.dateJugementSeparationCorps()).toBe('2024-12-15');
    expect(component.provenanceDateJugementSeparation()).toBeNull();
  });

  it('onPatrimoineCommunChange efface le badge IA', () => {
    component.aiData = { patrimoineCommun: true } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenancePatrimoineCommun()).toBe('IA');
    component.onPatrimoineCommunChange(false);
    expect(component.patrimoineCommun()).toBe(false);
    expect(component.provenancePatrimoineCommun()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.PATRIMOINE_COMMUN si IA dit autre chose', () => {
    component.aiData = { patrimoineCommun: true } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onPatrimoineCommunChange(false);

    const alert = component.coherenceAlerts().PATRIMOINE_COMMUN;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('PATRIMOINE_COMMUN');
    expect(alert!.source).toBe('IA');
    expect(alert!.expectedDisplay).toContain('Patrimoine commun');
  });

  it('coherenceAlerts.DATE_JUGEMENT_SEPARATION multi-source IA + F96 + PIECE', () => {
    component.aiData = { dateSeparation: '2023-01-10' } as Partial<FamilleExtractedData>;
    component.procedureChecks = [
      {
        id: 'c1', ordre: 1, description: 'Date séparation', statut: 'NON_COMPLIANT',
        critereCode: 'FA21_DATE_JUGEMENT_SEPARATION',
        expectedValue: '2023-01-10', raison: 'IA détecté',
      },
    ] as ProcedureCheck[];
    component.piecesManquantes = [
      { texte: 'Jugement de séparation manquant', critereCode: 'JUGEMENT_SEPARATION_CORPS' },
    ] as PieceManquanteEntry[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onDateJugementSeparationChange('2024-12-15');

    const alert = component.coherenceAlerts().DATE_JUGEMENT_SEPARATION;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.source).toBe('MULTI');
    expect(alert!.pieceTexte).toBe('Jugement de séparation manquant');
  });

  it('alertes masquées après résultat (showForm=false, anti-bug SF-IA-03-12)', () => {
    component.aiData = { patrimoineCommun: true } as Partial<FamilleExtractedData>;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onPatrimoineCommunChange(false);
    expect(component.coherenceAlerts().PATRIMOINE_COMMUN).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().PATRIMOINE_COMMUN).toBeUndefined();
  });

  it('alertBadgeLabel reflète le type de source (IA / MULTI)', () => {
    const alertIA = {
      field: 'PATRIMOINE_COMMUN' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: 'Patrimoine commun',
      reason: 'r',
    };
    const alertMulti = {
      ...alertIA, source: 'MULTI' as const,
      contributors: ['IA' as const, 'F96' as const],
    };
    expect(component.alertBadgeLabel(alertIA)).toContain('Incohérence détectée');
    expect(component.alertBadgeLabel(alertMulti)).toContain('multiple');
  });

  it('alertTooltip ajoute "Contredit" si > 1 contributor', () => {
    const alertSingle = {
      field: 'PATRIMOINE_COMMUN' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: 'Patrimoine commun',
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
    expect(component.explanationFor('DATE_JUGEMENT_SEPARATION')).toEqual([]);
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
      dateSeparation: '2023-01-10',
      patrimoineCommun: true,
    };
    component.aiData = newAi;
    component.ngOnChanges({
      aiData: new SimpleChange(null, newAi, false),
    });

    expect(component.dateJugementSeparationCorps()).toBe('2023-01-10');
    expect(component.patrimoineCommun()).toBe(true);
    expect(component.provenanceDateJugementSeparation()).toBe('IA');
    expect(component.provenancePatrimoineCommun()).toBe('IA');
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
    expect(component.verdictBannerClass('POSSIBLE')).toContain('strong');
    expect(component.verdictBannerClass('PREMATUREE')).toContain('medium');
    expect(component.verdictBannerClass('RECONCILIATION_BLOQUE')).toContain('weak');
  });

  it('verdictChipClass renvoie la classe attendue', () => {
    expect(component.verdictChipClass('POSSIBLE')).toContain('strong');
    expect(component.verdictChipClass('PREMATUREE')).toContain('medium');
    expect(component.verdictChipClass('RECONCILIATION_BLOQUE')).toContain('weak');
  });

  it('verdictBannerIcon renvoie l\'icône Material attendue', () => {
    expect(component.verdictBannerIcon('POSSIBLE')).toBe('check_circle');
    expect(component.verdictBannerIcon('PREMATUREE')).toBe('schedule');
    expect(component.verdictBannerIcon('RECONCILIATION_BLOQUE')).toBe('error');
  });
});
