import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { LicenciementEconomiqueSectionComponent } from './licenciement-economique-section.component';
import { LicenciementEconomiqueResponse } from '../../core/models/licenciement-economique.model';
import { TravailExtractedData, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('LicenciementEconomiqueSectionComponent', () => {
  let component: LicenciementEconomiqueSectionComponent;
  let fixture: ComponentFixture<LicenciementEconomiqueSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/licenciement-economique';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function defaultResponse(): LicenciementEconomiqueResponse {
    return {
      caseFileId: 'case-1',
      motifEconomiqueInvoque: 'DIFFICULTES_ECONOMIQUES',
      preuvesMotif: ['BAISSE_CHIFFRE_AFFAIRES', 'PERTES_EXPLOITATION'],
      criteresOrdreAppliques: ['AGE', 'ANCIENNETE', 'CHARGES_FAMILLE', 'QUALITES_PROFESSIONNELLES'],
      salarieAge: 45,
      salarieAncienneteMois: 96,
      salarieChargesFamille: 2,
      salarieQualitesProf: 'BON',
      tentativesReclassement: ['FORMATION_INTERNE', 'MUTATION_GROUPE'],
      prioriteReembauchePropose: true,
      congeReclassementPropose: false,
      dateNotification: '2026-04-25',
      country: 'FRANCE',
      scoreCausalite: 50,
      scoreCriteresOrdre: 100,
      scoreReclassement: 100,
      scoreGlobal: 83,
      verdictRisqueRequalification: 'FAIBLE',
      criteresOrdreManquants: [],
      criteresOrdreObligatoiresOk: true,
      obligationReclassementRespectee: true,
      baseJuridique: 'Art. L.1233-3 + L.1233-4 + L.1233-5 + L.1233-45 Code du travail',
      formule: 'Causalité 50 + Critères ordre 100 + Reclassement 100 = global 83',
      messages: ['Critères d\'ordre obligatoires OK'],
    };
  }

  /** Absorbe la requête source-explanations émise par `loadSourceExplanations()`. */
  function expectSourceExplanationCall(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        LicenciementEconomiqueSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LicenciementEconomiqueSectionComponent);
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
    expect(component.motifsOptions.length).toBe(5);
    expect(component.preuvesOptions.length).toBe(8);
    expect(component.criteresOptions.length).toBe(5);
    expect(component.tentativesOptions.length).toBe(5);
  });

  it('formValid faux si motif null', () => {
    component.motifEconomiqueInvoque.set(null);
    component.dateNotification.set('2026-04-25');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si dateNotification vide', () => {
    component.motifEconomiqueInvoque.set('DIFFICULTES_ECONOMIQUES');
    component.dateNotification.set(null);
    expect(component.formValid()).toBe(false);

    component.dateNotification.set('');
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai avec motif + date renseignés', () => {
    component.motifEconomiqueInvoque.set('DIFFICULTES_ECONOMIQUES');
    component.dateNotification.set('2026-04-25');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  it('GET 200 → mode résultat, valeurs persistées affichées, pas de badge IA', () => {
    component.aiData = {
      motifLicenciement: 'LICENCIEMENT_ECONOMIQUE',
      dateLicenciement: '2026-01-01',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse());
    expectSourceExplanationCall();

    expect(component.result()!.scoreGlobal).toBe(83);
    expect(component.showForm()).toBe(false);
    expect(component.dateNotification()).toBe('2026-04-25'); // valeur persistée
    expect(component.provenanceMotif()).toBeNull();
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('GET 404 → mode formulaire, pré-fill IA appliqué (motif + date)', () => {
    component.aiData = {
      motifLicenciement: 'LICENCIEMENT_ECONOMIQUE',
      dateLicenciement: '2026-03-15',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.showForm()).toBe(true);
    expect(component.motifEconomiqueInvoque()).toBe('DIFFICULTES_ECONOMIQUES');
    expect(component.provenanceMotif()).toBe('IA');
    expect(component.dateNotification()).toBe('2026-03-15');
    expect(component.provenanceDateNotification()).toBe('IA');
  });

  it('calculate() POST → résultat affiché + snackbar succès + dashboardRefresh', () => {
    component.motifEconomiqueInvoque.set('DIFFICULTES_ECONOMIQUES');
    component.preuvesMotif.set(['BAISSE_CHIFFRE_AFFAIRES']);
    component.criteresOrdreAppliques.set(['AGE', 'ANCIENNETE', 'CHARGES_FAMILLE', 'QUALITES_PROFESSIONNELLES']);
    component.salarieAge.set(45);
    component.salarieAncienneteMois.set(96);
    component.salarieChargesFamille.set(2);
    component.salarieQualitesProf.set('BON');
    component.tentativesReclassement.set(['FORMATION_INTERNE', 'MUTATION_GROUPE']);
    component.prioriteReembauchePropose.set(true);
    component.congeReclassementPropose.set(false);
    component.dateNotification.set('2026-04-25');

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.motifEconomiqueInvoque).toBe('DIFFICULTES_ECONOMIQUES');
    expect(req.request.body.preuvesMotif).toEqual(['BAISSE_CHIFFRE_AFFAIRES']);
    expect(req.request.body.criteresOrdreAppliques.length).toBe(4);
    expect(req.request.body.salarieAge).toBe(45);
    expect(req.request.body.tentativesReclassement.length).toBe(2);
    expect(req.request.body.prioriteReembauchePropose).toBe(true);
    expect(req.request.body.dateNotification).toBe('2026-04-25');
    req.flush(defaultResponse());

    expect(component.result()!.verdictRisqueRequalification).toBe('FAIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Risque de requalification analysé',
      'OK',
      jasmine.any(Object),
    );
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur 400 → snackbar rouge', () => {
    component.motifEconomiqueInvoque.set('DIFFICULTES_ECONOMIQUES');
    component.dateNotification.set('2026-04-25');
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
    component.motifEconomiqueInvoque.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Provenance handlers (badge IA effacé sur edit manuel)
  // ---------------------------------------------------------------------------

  it('onMotifChange efface le badge IA', () => {
    component.aiData = {
      motifLicenciement: 'LICENCIEMENT_ECONOMIQUE',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceMotif()).toBe('IA');
    component.onMotifChange('CESSATION_ACTIVITE');
    expect(component.motifEconomiqueInvoque()).toBe('CESSATION_ACTIVITE');
    expect(component.provenanceMotif()).toBeNull();
  });

  it('onDateNotificationChange efface le badge IA', () => {
    component.aiData = {
      dateLicenciement: '2026-03-15',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceDateNotification()).toBe('IA');
    component.onDateNotificationChange('2026-05-01');
    expect(component.dateNotification()).toBe('2026-05-01');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03 (via CoherenceAlertBuilder)
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.MOTIF_ECONOMIQUE présent si IA détecte motif différent', () => {
    component.aiData = {
      motifLicenciement: 'LICENCIEMENT_ECONOMIQUE', // mappe → DIFFICULTES_ECONOMIQUES
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Pré-fill a posé DIFFICULTES_ECONOMIQUES. Avocat change vers CESSATION.
    component.onMotifChange('CESSATION_ACTIVITE');

    const alerts = component.coherenceAlerts();
    expect(alerts.MOTIF_ECONOMIQUE).toBeDefined();
    expect(alerts.MOTIF_ECONOMIQUE!.field).toBe('MOTIF_ECONOMIQUE');
    expect(alerts.MOTIF_ECONOMIQUE!.source).toBe('IA');
    expect(alerts.MOTIF_ECONOMIQUE!.contributors).toEqual(['IA']);
    expect(alerts.MOTIF_ECONOMIQUE!.expectedDisplay).toContain('Difficultés');
  });

  it('coherenceAlerts.MOTIF_ECONOMIQUE absent si même motif IA et user', () => {
    component.aiData = {
      motifLicenciement: 'LICENCIEMENT_ECONOMIQUE',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // pré-fill a posé DIFFICULTES_ECONOMIQUES = motif user.
    expect(component.coherenceAlerts().MOTIF_ECONOMIQUE).toBeUndefined();
  });

  it('coherenceAlerts.DATE_NOTIFICATION présent si IA et user divergent', () => {
    component.aiData = {
      dateLicenciement: '2026-03-15',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onDateNotificationChange('2026-05-01'); // divergence
    const alert = component.coherenceAlerts().DATE_NOTIFICATION;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('DATE_NOTIFICATION');
    expect(alert!.expectedDisplay).toBe('2026-03-15');
    expect(alert!.source).toBe('IA');
  });

  it('coherenceAlerts.DATE_NOTIFICATION absent si user vide', () => {
    component.aiData = {
      dateLicenciement: '2026-03-15',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // pré-fill a posé 2026-03-15 = user, pas d'alerte.
    component.onDateNotificationChange(null);
    expect(component.coherenceAlerts().DATE_NOTIFICATION).toBeUndefined();
  });

  it('alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = {
      motifLicenciement: 'LICENCIEMENT_ECONOMIQUE',
    } as unknown as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onMotifChange('CESSATION_ACTIVITE');
    expect(component.coherenceAlerts().MOTIF_ECONOMIQUE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().MOTIF_ECONOMIQUE).toBeUndefined();
  });

  it('coherenceAlerts.MOTIF_ECONOMIQUE multi-source IA + F96 + PIECE_MANQUANTE', () => {
    component.aiData = {
      motifLicenciement: 'LICENCIEMENT_ECONOMIQUE', // mappe → DIFFICULTES_ECONOMIQUES
    } as unknown as TravailExtractedData;
    component.procedureChecks = [
      {
        id: 'c1', ordre: 1, description: 'Motif vérifié', statut: 'NON_COMPLIANT',
        critereCode: 'DT13_MOTIF_ECONOMIQUE', expectedValue: 'DIFFICULTES_ECONOMIQUES', raison: 'comptabilité produite',
      },
    ] as ProcedureCheck[];
    component.piecesManquantes = [
      { texte: 'Comptabilité (3 derniers exercices)', critereCode: 'COMPTABILITE' },
    ] as PieceManquanteEntry[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Avocat saisit motif différent (REORGANISATION_COMPETITIVITE).
    component.onMotifChange('REORGANISATION_COMPETITIVITE');

    const alert = component.coherenceAlerts().MOTIF_ECONOMIQUE;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.source).toBe('MULTI');
    expect(alert!.pieceTexte).toBe('Comptabilité (3 derniers exercices)');
  });

  it('alertBadgeLabel reflète le type de source (IA / MULTI)', () => {
    const alertIA = {
      field: 'MOTIF_ECONOMIQUE' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: 'Difficultés économiques',
      reason: 'r',
    };
    const alertMulti = { ...alertIA, source: 'MULTI' as const, contributors: ['IA' as const, 'F96' as const] };
    expect(component.alertBadgeLabel(alertIA)).toContain('Incohérence détectée');
    expect(component.alertBadgeLabel(alertIA)).toContain('Difficultés');
    expect(component.alertBadgeLabel(alertMulti)).toContain('multiple');
  });

  it('alertTooltip ajoute "Contredit" si > 1 contributor', () => {
    const alertSingle = {
      field: 'DATE_NOTIFICATION' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: '2026-03-15',
      reason: 'simple',
    };
    const alertMulti = { ...alertSingle, source: 'MULTI' as const, contributors: ['IA' as const, 'F96' as const] };
    expect(component.alertTooltip(alertSingle)).toBe('simple');
    expect(component.alertTooltip(alertMulti)).toContain('Contredit');
  });

  it('explanationFor retourne [] si pas de source seedée', () => {
    expect(component.explanationFor('MOTIF_ECONOMIQUE')).toEqual([]);
    expect(component.explanationFor('DATE_NOTIFICATION')).toEqual([]);
  });

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = {
      motifLicenciement: 'CESSATION_ACTIVITE',
      dateLicenciement: '2026-02-15',
    } as unknown as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({
      aiData: new SimpleChange(null, newAi, false),
    });

    expect(component.motifEconomiqueInvoque()).toBe('CESSATION_ACTIVITE');
    expect(component.provenanceMotif()).toBe('IA');
    expect(component.dateNotification()).toBe('2026-02-15');
    expect(component.provenanceDateNotification()).toBe('IA');
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

  it('verdictBannerClass renvoie la classe attendue (info/warning/danger)', () => {
    expect(component.verdictBannerClass('FAIBLE')).toContain('info');
    expect(component.verdictBannerClass('MOYENNE')).toContain('warning');
    expect(component.verdictBannerClass('ELEVEE')).toContain('danger');
  });

  it('verdictChipClass renvoie la classe chip attendue', () => {
    expect(component.verdictChipClass('FAIBLE')).toContain('info');
    expect(component.verdictChipClass('MOYENNE')).toContain('warning');
    expect(component.verdictChipClass('ELEVEE')).toContain('danger');
  });

  it('verdictIcon renvoie l\'icône matérielle attendue', () => {
    expect(component.verdictIcon('FAIBLE')).toBe('info_outline');
    expect(component.verdictIcon('MOYENNE')).toBe('warning');
    expect(component.verdictIcon('ELEVEE')).toBe('gavel');
  });

  it('verdictRisqueLabel mappe les 3 verdicts', () => {
    expect(component.verdictRisqueLabel('FAIBLE')).toContain('faible');
    expect(component.verdictRisqueLabel('MOYENNE')).toContain('moyen');
    expect(component.verdictRisqueLabel('ELEVEE')).toContain('élevé');
  });
});
