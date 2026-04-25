import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { DesaccordsParentauxSectionComponent } from './desaccords-parentaux-section.component';
import { DesaccordsParentauxResponse } from '../../core/models/desaccords-parentaux.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('DesaccordsParentauxSectionComponent', () => {
  let component: DesaccordsParentauxSectionComponent;
  let fixture: ComponentFixture<DesaccordsParentauxSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/desaccords-parentaux';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function defaultResponse(): DesaccordsParentauxResponse {
    return {
      caseFileId: 'case-1',
      domaineDesaccord: 'SCOLARITE',
      intensiteDesaccord: 'MAJEUR',
      tentativesMediation: ['MEDIATION_FAMILIALE', 'DISCUSSIONS_DIRECTES'],
      ageEnfantsConcernes: [12, 8],
      interetSuperieurInvoque: true,
      expertiseDejaRealisee: false,
      urgence: false,
      dateRequete: '2026-04-25',
      scoreEligibiliteJaf: 72,
      verdictProbabiliteAcceptation: 'ELEVEE',
      mediationPrealableRecommandee: false,
      auditionEnfantsRecommandee: true,
      expertisePsyRecommandee: true,
      delaiTraitementJoursPrevisionnel: 90,
      baseJuridique: 'Art. 373-2-10 + 388-1 Cciv',
      formule: 'score = 72/100',
      messages: ['Médiation déjà tentée — saisine JAF possible.'],
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
        DesaccordsParentauxSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DesaccordsParentauxSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + parsing helpers
  // ---------------------------------------------------------------------------

  it('mount sans erreur (FRANCE)', () => {
    expect(component).toBeTruthy();
    expect(component.domainesOptions.length).toBe(7);
    expect(component.intensitesOptions.length).toBe(3);
    expect(component.tentativesOptions.length).toBe(5);
  });

  it('parseAgeEnfants accepte CSV "12, 8, 5"', () => {
    expect(component.parseAgeEnfants('12, 8, 5')).toEqual([12, 8, 5]);
    expect(component.parseAgeEnfants('12;8 5')).toEqual([12, 8, 5]);
    expect(component.parseAgeEnfants('')).toEqual([]);
    expect(component.parseAgeEnfants('abc, 12, -1, 35')).toEqual([12]);
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid faux si domaineDesaccord null', () => {
    component.domaineDesaccord.set(null);
    component.intensiteDesaccord.set('MAJEUR');
    component.tentativesMediation.set(['AUCUNE']);
    component.ageEnfantsConcernes.set([10]);
    component.dateRequete.set('2026-04-25');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si intensiteDesaccord null', () => {
    component.domaineDesaccord.set('SCOLARITE');
    component.intensiteDesaccord.set(null);
    component.tentativesMediation.set(['AUCUNE']);
    component.ageEnfantsConcernes.set([10]);
    component.dateRequete.set('2026-04-25');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si tentativesMediation vide', () => {
    component.domaineDesaccord.set('SCOLARITE');
    component.intensiteDesaccord.set('MAJEUR');
    component.tentativesMediation.set([]);
    component.ageEnfantsConcernes.set([10]);
    component.dateRequete.set('2026-04-25');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si ageEnfantsConcernes vide', () => {
    component.domaineDesaccord.set('SCOLARITE');
    component.intensiteDesaccord.set('MAJEUR');
    component.tentativesMediation.set(['AUCUNE']);
    component.ageEnfantsConcernes.set([]);
    component.dateRequete.set('2026-04-25');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si dateRequete absente', () => {
    component.domaineDesaccord.set('SCOLARITE');
    component.intensiteDesaccord.set('MAJEUR');
    component.tentativesMediation.set(['AUCUNE']);
    component.ageEnfantsConcernes.set([10]);
    component.dateRequete.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai sur saisie minimale complète', () => {
    component.domaineDesaccord.set('SCOLARITE');
    component.intensiteDesaccord.set('MAJEUR');
    component.tentativesMediation.set(['AUCUNE']);
    component.ageEnfantsConcernes.set([10]);
    component.dateRequete.set('2026-04-25');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  it('GET 200 → form masqué + valeurs persistées + pas de badge IA', () => {
    component.aiData = {
      domaineDesaccordDetecte: 'SANTE',
    } as FamilleExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse());
    expectSourceExplanationCall();

    expect(component.result()!.scoreEligibiliteJaf).toBe(72);
    expect(component.showForm()).toBe(false);
    expect(component.domaineDesaccord()).toBe('SCOLARITE'); // persisté
    expect(component.provenanceDomaine()).toBeNull();
    expect(component.ageEnfantsRaw()).toBe('12, 8');
  });

  it('GET 404 → mode formulaire + pré-fill IA appliqué (5 fields)', () => {
    component.aiData = {
      domaineDesaccordDetecte: 'SCOLARITE',
      intensiteDesaccordDetecte: 'MAJEUR',
      tentativesMediationDetectees: ['MEDIATION_FAMILIALE'],
      ageEnfants: [10, 7],
      urgenceDetectee: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.showForm()).toBe(true);
    expect(component.domaineDesaccord()).toBe('SCOLARITE');
    expect(component.intensiteDesaccord()).toBe('MAJEUR');
    expect(component.tentativesMediation()).toEqual(['MEDIATION_FAMILIALE']);
    expect(component.ageEnfantsConcernes()).toEqual([10, 7]);
    expect(component.urgence()).toBe(true);
    expect(component.provenanceDomaine()).toBe('IA');
    expect(component.provenanceIntensite()).toBe('IA');
    expect(component.provenanceTentatives()).toBe('IA');
    expect(component.provenanceAgeEnfants()).toBe('IA');
    expect(component.provenanceUrgence()).toBe('IA');
  });

  it('GET 404 + aiData absent → no-op gracieux (pas de pré-fill)', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.showForm()).toBe(true);
    expect(component.domaineDesaccord()).toBeNull();
    expect(component.ageEnfantsConcernes()).toEqual([]);
  });

  it('calculate() POST → result + snackbar succès + dashboardRefresh', () => {
    component.domaineDesaccord.set('SCOLARITE');
    component.intensiteDesaccord.set('MAJEUR');
    component.tentativesMediation.set(['MEDIATION_FAMILIALE', 'DISCUSSIONS_DIRECTES']);
    component.onAgeEnfantsRawChange('12, 8');
    component.interetSuperieurInvoque.set(true);
    component.expertiseDejaRealisee.set(false);
    component.urgence.set(false);
    component.dateRequete.set('2026-04-25');

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      domaineDesaccord: 'SCOLARITE',
      intensiteDesaccord: 'MAJEUR',
      tentativesMediation: ['MEDIATION_FAMILIALE', 'DISCUSSIONS_DIRECTES'],
      ageEnfantsConcernes: [12, 8],
      interetSuperieurInvoque: true,
      expertiseDejaRealisee: false,
      urgence: false,
      dateRequete: '2026-04-25',
    });
    req.flush(defaultResponse());

    expect(component.result()!.verdictProbabiliteAcceptation).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse désaccords parentaux calculée',
      'OK',
      jasmine.any(Object),
    );
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur 400 → snackbar rouge', () => {
    component.domaineDesaccord.set('SCOLARITE');
    component.intensiteDesaccord.set('MAJEUR');
    component.tentativesMediation.set(['AUCUNE']);
    component.onAgeEnfantsRawChange('10');
    component.dateRequete.set('2026-04-25');
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
    component.domaineDesaccord.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Provenance handlers (badge IA effacé sur edit manuel)
  // ---------------------------------------------------------------------------

  it('onDomaineChange efface le badge IA', () => {
    component.aiData = { domaineDesaccordDetecte: 'SCOLARITE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceDomaine()).toBe('IA');
    component.onDomaineChange('SANTE');
    expect(component.domaineDesaccord()).toBe('SANTE');
    expect(component.provenanceDomaine()).toBeNull();
  });

  it('onAgeEnfantsRawChange efface le badge IA âges', () => {
    component.aiData = { ageEnfants: [12, 8] } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceAgeEnfants()).toBe('IA');
    component.onAgeEnfantsRawChange('15, 10');
    expect(component.ageEnfantsConcernes()).toEqual([15, 10]);
    expect(component.provenanceAgeEnfants()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.DOMAINE_DESACCORD si IA dit autre chose', () => {
    component.aiData = { domaineDesaccordDetecte: 'SANTE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDomaineChange('SCOLARITE');

    const alerts = component.coherenceAlerts();
    expect(alerts.DOMAINE_DESACCORD).toBeDefined();
    expect(alerts.DOMAINE_DESACCORD!.field).toBe('DOMAINE_DESACCORD');
    expect(alerts.DOMAINE_DESACCORD!.source).toBe('IA');
    expect(alerts.DOMAINE_DESACCORD!.expectedDisplay).toContain('Santé');
  });

  it('coherenceAlerts.AGE_ENFANTS_CONCERNES multi-source IA + F96 + PIECE_MANQUANTE', () => {
    component.aiData = { ageEnfants: [12, 8] } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'c1', ordre: 1, description: 'Âges enfants', statut: 'NON_COMPLIANT',
        critereCode: 'FA19_AGE_ENFANTS_CONCERNES', expectedValue: '8, 12', raison: 'IA détecté',
      },
    ] as ProcedureCheck[];
    component.piecesManquantes = [
      { texte: 'Acte de naissance enfant', critereCode: 'ACTE_NAISSANCE_ENFANT' },
    ] as PieceManquanteEntry[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onAgeEnfantsRawChange('15, 10'); // divergence ensembliste

    const alert = component.coherenceAlerts().AGE_ENFANTS_CONCERNES;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('AGE_ENFANTS_CONCERNES');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.source).toBe('MULTI');
    expect(alert!.pieceTexte).toBe('Acte de naissance enfant');
  });

  it('coherenceAlerts.URGENCE si IA détecte urgence et user ne coche pas', () => {
    component.aiData = { urgenceDetectee: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Pré-fill a coché true → on simule décocheage manuel.
    component.onUrgenceChange(false);
    const alert = component.coherenceAlerts().URGENCE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('URGENCE');
    expect(alert!.expectedDisplay).toContain('Urgence');
  });

  it('coherenceAlerts.TENTATIVES_MEDIATION divergence ensembliste IA', () => {
    component.aiData = {
      tentativesMediationDetectees: ['MEDIATION_FAMILIALE', 'DISCUSSIONS_DIRECTES'],
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // L'avocat saisit une seule tentative qui ne match pas l'analyse IA.
    component.onTentativesChange(['AUCUNE']);

    const alert = component.coherenceAlerts().TENTATIVES_MEDIATION;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('TENTATIVES_MEDIATION');
    expect(alert!.source).toBe('IA');
  });

  it('alertes masquées après résultat (showForm=false, anti-bug SF-IA-03-12)', () => {
    component.aiData = { domaineDesaccordDetecte: 'SANTE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDomaineChange('SCOLARITE');
    expect(component.coherenceAlerts().DOMAINE_DESACCORD).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DOMAINE_DESACCORD).toBeUndefined();
  });

  it('alertBadgeLabel reflète le type de source (IA / MULTI)', () => {
    const alertIA = {
      field: 'DOMAINE_DESACCORD' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: 'Scolarité',
      reason: 'r',
    };
    const alertMulti = {
      ...alertIA, source: 'MULTI' as const,
      contributors: ['IA' as const, 'F96' as const],
    };
    expect(component.alertBadgeLabel(alertIA)).toContain('Incohérence détectée');
    expect(component.alertBadgeLabel(alertIA)).toContain('Scolarité');
    expect(component.alertBadgeLabel(alertMulti)).toContain('multiple');
  });

  it('alertTooltip ajoute "Contredit" si > 1 contributor', () => {
    const alertSingle = {
      field: 'DOMAINE_DESACCORD' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: 'Scolarité',
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
    expect(component.explanationFor('DOMAINE_DESACCORD')).toEqual([]);
    expect(component.explanationFor('INTENSITE_DESACCORD')).toEqual([]);
    expect(component.explanationFor('TENTATIVES_MEDIATION')).toEqual([]);
    expect(component.explanationFor('AGE_ENFANTS_CONCERNES')).toEqual([]);
    expect(component.explanationFor('URGENCE')).toEqual([]);
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges — propagation des inputs
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = {
      domaineDesaccordDetecte: 'SCOLARITE',
      ageEnfants: [10, 7],
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({
      aiData: new SimpleChange(null, newAi, false),
    });

    expect(component.domaineDesaccord()).toBe('SCOLARITE');
    expect(component.ageEnfantsConcernes()).toEqual([10, 7]);
    expect(component.provenanceDomaine()).toBe('IA');
  });

  it('ngOnChanges propage les nouveaux procedureChecks', () => {
    component.aiData = { domaineDesaccordDetecte: 'SANTE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newChecks: ProcedureCheck[] = [
      {
        id: 'c2', ordre: 2, description: 'Domaine', statut: 'NON_COMPLIANT',
        critereCode: 'FA19_DOMAINE_DESACCORD', expectedValue: 'Santé',
      } as ProcedureCheck,
    ];
    component.procedureChecks = newChecks;
    component.ngOnChanges({
      procedureChecks: new SimpleChange(null, newChecks, false),
    });

    component.onDomaineChange('SCOLARITE'); // divergence
    const alert = component.coherenceAlerts().DOMAINE_DESACCORD;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('F96');
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

  it('verdictBannerClass renvoie la classe attendue (strong/medium/weak)', () => {
    expect(component.verdictBannerClass('ELEVEE')).toContain('strong');
    expect(component.verdictBannerClass('MOYENNE')).toContain('medium');
    expect(component.verdictBannerClass('FAIBLE')).toContain('weak');
  });

  it('GET 200 avec urgence true → délai préservé pour affichage', () => {
    const r = defaultResponse();
    r.urgence = true;
    r.delaiTraitementJoursPrevisionnel = 30;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(r);
    expectSourceExplanationCall();

    expect(component.result()!.urgence).toBe(true);
    expect(component.result()!.delaiTraitementJoursPrevisionnel).toBe(30);
  });
});
