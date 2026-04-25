import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { MajeursProtegesSectionComponent } from './majeurs-proteges-section.component';
import { MajeursProtegesResponse } from '../../core/models/majeurs-proteges.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('MajeursProtegesSectionComponent', () => {
  let component: MajeursProtegesSectionComponent;
  let fixture: ComponentFixture<MajeursProtegesSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/majeurs-proteges';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function defaultResponse(): MajeursProtegesResponse {
    return {
      caseFileId: 'case-1',
      regimeProtectionDemande: 'CURATELLE_RENFORCEE',
      altertationFacultesMentales: true,
      altertationFacultesPhysiques: false,
      certificatMedicalCirconstancie: true,
      dateCertificatMedical: '2026-04-15',
      consentementPersonneAProteger: false,
      demandeurFamilial: 'ENFANT_MAJEUR',
      actesEnvisages: ['GESTION_PATRIMOINE', 'DECISIONS_LOGEMENT'],
      urgencePatrimoniale: false,
      patrimoineSignificatif: true,
      isolementSocial: false,
      // SF-FA-25-06 : 4 nouveaux champs (defaults backward-compat).
      incapaciteGestionQuotidienne: true,
      altertationGrave: false,
      mandatPrealableSigne: false,
      formeMandatProtection: null,
      scoreEligibilite: 78,
      regimeOptimalRecommande: 'TUTELLE',
      verdictAcceptabiliteJaf: 'ELEVEE',
      delaiProcedureMoisPrevisionnel: 4,
      auditionPersonneObligatoire: true,
      expertisePsyComplementaireRecommandee: false,
      eligible: true,
      criteresNonRemplis: [],
      baseJuridique: 'Art. 425-494 Cciv + 494-1 Cciv',
      formule: 'score = 78/100',
      messages: [
        'Audition obligatoire (art. 432 Cciv) sauf dispense motivée.',
        'Tutelle recommandée plutôt que curatelle renforcée — altération mentale grave.',
      ],
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
        MajeursProtegesSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MajeursProtegesSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + form validators
  // ---------------------------------------------------------------------------

  it('mount sans erreur (FRANCE) — options enums correctes', () => {
    expect(component).toBeTruthy();
    expect(component.regimesOptions.length).toBe(6);
    expect(component.demandeursOptions.length).toBe(6);
    expect(component.actesOptions.length).toBe(6);
  });

  it('formValid faux si régime null', () => {
    component.regimeProtectionDemande.set(null);
    component.demandeurFamilial.set('ENFANT_MAJEUR');
    component.actesEnvisages.set(['GESTION_PATRIMOINE']);
    component.dateCertificatMedical.set('2026-04-15');
    component.altertationFacultesMentales.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si demandeur null', () => {
    component.regimeProtectionDemande.set('CURATELLE_SIMPLE');
    component.demandeurFamilial.set(null);
    component.actesEnvisages.set(['GESTION_PATRIMOINE']);
    component.dateCertificatMedical.set('2026-04-15');
    component.altertationFacultesMentales.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si actesEnvisages vide', () => {
    component.regimeProtectionDemande.set('CURATELLE_SIMPLE');
    component.demandeurFamilial.set('ENFANT_MAJEUR');
    component.actesEnvisages.set([]);
    component.dateCertificatMedical.set('2026-04-15');
    component.altertationFacultesMentales.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si date certificat invalide', () => {
    component.regimeProtectionDemande.set('CURATELLE_SIMPLE');
    component.demandeurFamilial.set('ENFANT_MAJEUR');
    component.actesEnvisages.set(['GESTION_PATRIMOINE']);
    component.dateCertificatMedical.set(null);
    component.altertationFacultesMentales.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si aucune altération', () => {
    component.regimeProtectionDemande.set('CURATELLE_SIMPLE');
    component.demandeurFamilial.set('ENFANT_MAJEUR');
    component.actesEnvisages.set(['GESTION_PATRIMOINE']);
    component.dateCertificatMedical.set('2026-04-15');
    component.altertationFacultesMentales.set(false);
    component.altertationFacultesPhysiques.set(false);
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai si toutes conditions OK', () => {
    component.regimeProtectionDemande.set('CURATELLE_SIMPLE');
    component.demandeurFamilial.set('ENFANT_MAJEUR');
    component.actesEnvisages.set(['GESTION_PATRIMOINE']);
    component.dateCertificatMedical.set('2026-04-15');
    component.altertationFacultesMentales.set(true);
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  it('GET 200 → form masqué, valeurs persistées affichées, pas de badge IA', () => {
    component.aiData = {
      altertationFacultesMentales: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse());
    expectSourceExplanationCall();

    expect(component.result()!.scoreEligibilite).toBe(78);
    expect(component.result()!.regimeOptimalRecommande).toBe('TUTELLE');
    expect(component.showForm()).toBe(false);
    expect(component.regimeProtectionDemande()).toBe('CURATELLE_RENFORCEE');
    expect(component.actesEnvisages()).toEqual(['GESTION_PATRIMOINE', 'DECISIONS_LOGEMENT']);
    expect(component.provenanceAltertationFacultesMentales()).toBeNull();
  });

  it('GET 404 → reste en mode formulaire ; pré-fill IA appliqué', () => {
    component.aiData = {
      regimeProtectionDemande: 'CURATELLE_RENFORCEE',
      altertationFacultesMentales: true,
      altertationFacultesPhysiques: false,
      certificatMedicalCirconstancieDetected: true,
      dateCertificatMedicalDetected: '2026-04-10',
      consentementPersonneAProtegerDetected: false,
      demandeurFamilialDetected: 'CONJOINT',
      actesEnvisagesDetected: ['GESTION_PATRIMOINE', 'DECISIONS_SANTE'],
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.showForm()).toBe(true);
    expect(component.regimeProtectionDemande()).toBe('CURATELLE_RENFORCEE');
    expect(component.altertationFacultesMentales()).toBe(true);
    expect(component.dateCertificatMedical()).toBe('2026-04-10');
    expect(component.demandeurFamilial()).toBe('CONJOINT');
    expect(component.actesEnvisages()).toEqual(['GESTION_PATRIMOINE', 'DECISIONS_SANTE']);
    expect(component.provenanceRegimeProtectionDemande()).toBe('IA');
    expect(component.provenanceAltertationFacultesMentales()).toBe('IA');
    expect(component.provenanceDateCertificatMedical()).toBe('IA');
    expect(component.provenanceDemandeurFamilial()).toBe('IA');
    expect(component.provenanceActesEnvisages()).toBe('IA');
  });

  it('calculate() POST → résultat affiché + snackbar succès + dashboardRefresh', () => {
    component.regimeProtectionDemande.set('CURATELLE_RENFORCEE');
    component.altertationFacultesMentales.set(true);
    component.altertationFacultesPhysiques.set(false);
    component.certificatMedicalCirconstancie.set(true);
    component.dateCertificatMedical.set('2026-04-15');
    component.consentementPersonneAProteger.set(false);
    component.demandeurFamilial.set('ENFANT_MAJEUR');
    component.actesEnvisages.set(['GESTION_PATRIMOINE', 'DECISIONS_LOGEMENT']);
    component.urgencePatrimoniale.set(false);
    component.patrimoineSignificatif.set(true);
    component.isolementSocial.set(false);

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.regimeProtectionDemande).toBe('CURATELLE_RENFORCEE');
    expect(req.request.body.demandeurFamilial).toBe('ENFANT_MAJEUR');
    expect(req.request.body.actesEnvisages).toEqual(['GESTION_PATRIMOINE', 'DECISIONS_LOGEMENT']);
    expect(req.request.body.altertationFacultesMentales).toBe(true);
    expect(req.request.body.dateCertificatMedical).toBe('2026-04-15');
    req.flush(defaultResponse());

    expect(component.result()!.verdictAcceptabiliteJaf).toBe('ELEVEE');
    expect(component.result()!.regimeOptimalRecommande).toBe('TUTELLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse majeurs protégés calculée',
      'OK',
      jasmine.any(Object),
    );
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur 400 → snackbar rouge', () => {
    component.regimeProtectionDemande.set('CURATELLE_SIMPLE');
    component.demandeurFamilial.set('ENFANT_MAJEUR');
    component.actesEnvisages.set(['GESTION_PATRIMOINE']);
    component.dateCertificatMedical.set('2026-04-15');
    component.altertationFacultesMentales.set(true);
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
    component.regimeProtectionDemande.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Provenance handlers (badge IA effacé sur edit manuel)
  // ---------------------------------------------------------------------------

  it('onRegimeChange efface le badge IA', () => {
    component.aiData = {
      regimeProtectionDemande: 'TUTELLE',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceRegimeProtectionDemande()).toBe('IA');
    component.onRegimeChange('CURATELLE_SIMPLE');
    expect(component.regimeProtectionDemande()).toBe('CURATELLE_SIMPLE');
    expect(component.provenanceRegimeProtectionDemande()).toBeNull();
  });

  it('onAltMentalesChange efface le badge IA', () => {
    component.aiData = {
      altertationFacultesMentales: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceAltertationFacultesMentales()).toBe('IA');
    component.onAltMentalesChange(false);
    expect(component.altertationFacultesMentales()).toBe(false);
    expect(component.provenanceAltertationFacultesMentales()).toBeNull();
  });

  it('onDemandeurChange efface le badge IA', () => {
    component.aiData = {
      demandeurFamilialDetected: 'CONJOINT',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceDemandeurFamilial()).toBe('IA');
    component.onDemandeurChange('FRERE_SOEUR');
    expect(component.demandeurFamilial()).toBe('FRERE_SOEUR');
    expect(component.provenanceDemandeurFamilial()).toBeNull();
  });

  it('onActesChange efface le badge IA', () => {
    component.aiData = {
      actesEnvisagesDetected: ['GESTION_PATRIMOINE'],
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceActesEnvisages()).toBe('IA');
    component.onActesChange(['DECISIONS_SANTE']);
    expect(component.actesEnvisages()).toEqual(['DECISIONS_SANTE']);
    expect(component.provenanceActesEnvisages()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03 (via CoherenceAlertBuilder)
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.DATE_CERTIFICAT alerte multi-source IA + F96 + PIECE_MANQUANTE', () => {
    component.aiData = { dateCertificatMedicalDetected: '2026-04-10' } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'c1', ordre: 1, description: 'Date certificat', statut: 'NON_COMPLIANT',
        critereCode: 'FA25_DATE_CERTIFICAT', expectedValue: '2026-04-10', raison: 'date confirmée',
      },
    ] as ProcedureCheck[];
    component.piecesManquantes = [
      { texte: 'Certificat médical circonstancié', critereCode: 'CERTIFICAT_MEDICAL_CIRCONSTANCIE' },
    ] as PieceManquanteEntry[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onDateCertificatChange('2026-05-15');

    const alert = component.coherenceAlerts().DATE_CERTIFICAT;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('DATE_CERTIFICAT');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.source).toBe('MULTI');
    expect(alert!.expectedDisplay).toBe('2026-04-10');
    expect(alert!.pieceTexte).toBe('Certificat médical circonstancié');
  });

  it('coherenceAlerts.ALT_MENTALES si IA dit true et avocat false', () => {
    component.aiData = { altertationFacultesMentales: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Pré-fill a posé true. Avocat saisit false.
    component.onAltMentalesChange(false);
    const alert = component.coherenceAlerts().ALT_MENTALES;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('ALT_MENTALES');
    expect(alert!.expectedDisplay).toContain('Altération facultés mentales');
    expect(alert!.source).toBe('IA');
  });

  it('coherenceAlerts.CONSENTEMENT divergence boolean', () => {
    component.aiData = {
      consentementPersonneAProtegerDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Pré-fill a posé true. Avocat saisit false.
    component.onConsentementChange(false);
    const alert = component.coherenceAlerts().CONSENTEMENT;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('CONSENTEMENT');
    expect(alert!.expectedDisplay).toBe('Consentement détecté');
    expect(alert!.source).toBe('IA');
  });

  it('coherenceAlerts.DEMANDEUR_FAMILIAL divergence enum', () => {
    component.aiData = {
      demandeurFamilialDetected: 'CONJOINT',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onDemandeurChange('FRERE_SOEUR');
    const alert = component.coherenceAlerts().DEMANDEUR_FAMILIAL;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('DEMANDEUR_FAMILIAL');
    expect(alert!.expectedDisplay).toContain('Conjoint');
    expect(alert!.source).toBe('IA');
  });

  it('alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = {
      altertationFacultesMentales: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onAltMentalesChange(false);
    expect(component.coherenceAlerts().ALT_MENTALES).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().ALT_MENTALES).toBeUndefined();
  });

  it('alertBadgeLabel reflète le type de source (IA / MULTI)', () => {
    const alertIA = {
      field: 'ALT_MENTALES' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: 'Altération détectée',
      reason: 'r',
    };
    const alertMulti = { ...alertIA, source: 'MULTI' as const, contributors: ['IA' as const, 'F96' as const] };
    expect(component.alertBadgeLabel(alertIA)).toContain('Incohérence détectée');
    expect(component.alertBadgeLabel(alertIA)).toContain('Altération détectée');
    expect(component.alertBadgeLabel(alertMulti)).toContain('multiple');
  });

  it('alertTooltip ajoute "Contredit" si > 1 contributor', () => {
    const alertSingle = {
      field: 'DATE_CERTIFICAT' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: '2026-04-10',
      reason: 'simple',
    };
    const alertMulti = { ...alertSingle, source: 'MULTI' as const, contributors: ['IA' as const, 'F96' as const] };
    expect(component.alertTooltip(alertSingle)).toBe('simple');
    expect(component.alertTooltip(alertMulti)).toContain('Contredit');
  });

  it('explanationFor retourne la liste filtrée (fail-open empty)', () => {
    expect(component.explanationFor('DATE_CERTIFICAT')).toEqual([]);
    expect(component.explanationFor('ALT_MENTALES')).toEqual([]);
    expect(component.explanationFor('CONSENTEMENT')).toEqual([]);
    expect(component.explanationFor('DEMANDEUR_FAMILIAL')).toEqual([]);
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
  // ngOnChanges + UI helpers
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = {
      regimeProtectionDemande: 'TUTELLE',
      altertationFacultesMentales: true,
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({
      aiData: new SimpleChange(null, newAi, false),
    });

    expect(component.regimeProtectionDemande()).toBe('TUTELLE');
    expect(component.altertationFacultesMentales()).toBe(true);
    expect(component.provenanceRegimeProtectionDemande()).toBe('IA');
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

  it('verdictMajeursLabel mappe les 3 verdicts', () => {
    expect(component.verdictMajeursLabel('ELEVEE')).toContain('élevée');
    expect(component.verdictMajeursLabel('MOYENNE')).toContain('moyenne');
    expect(component.verdictMajeursLabel('FAIBLE')).toContain('faible');
  });

  it('regimeProtectionLabel mappe les codes régimes FR', () => {
    expect(component.regimeProtectionLabel('TUTELLE')).toContain('Tutelle');
    expect(component.regimeProtectionLabel('SAUVEGARDE_JUSTICE')).toContain('Sauvegarde');
    expect(component.regimeProtectionLabel('HABILITATION_FAMILIALE')).toContain('Habilitation');
  });

  it('demandeurFamilialLabel mappe les codes demandeurs FR', () => {
    expect(component.demandeurFamilialLabel('CONJOINT')).toContain('Conjoint');
    expect(component.demandeurFamilialLabel('ENFANT_MAJEUR')).toContain('Enfant');
    expect(component.demandeurFamilialLabel('MINISTERE_PUBLIC')).toContain('Ministère');
  });

  it('acteEnvisageLabel mappe les codes actes FR', () => {
    expect(component.acteEnvisageLabel('GESTION_PATRIMOINE')).toContain('patrimoine');
    expect(component.acteEnvisageLabel('DECISIONS_SANTE')).toContain('santé');
  });

  it('isRegimeRecommandationDifferente vrai si recommandé ≠ demandé', () => {
    component.result.set(defaultResponse()); // demandé=CURATELLE_RENFORCEE, recommandé=TUTELLE
    expect(component.isRegimeRecommandationDifferente()).toBe(true);
  });

  it('isRegimeRecommandationDifferente faux si recommandé === demandé', () => {
    const r = defaultResponse();
    r.regimeOptimalRecommande = r.regimeProtectionDemande;
    component.result.set(r);
    expect(component.isRegimeRecommandationDifferente()).toBe(false);
  });

  it('isRegimeRecommandationDifferente faux si pas de result', () => {
    component.result.set(null);
    expect(component.isRegimeRecommandationDifferente()).toBe(false);
  });

  it('prefillFromAi ignore régime IA hors enum', () => {
    component.aiData = { regimeProtectionDemande: 'INEXISTANT' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.regimeProtectionDemande()).toBeNull();
    expect(component.provenanceRegimeProtectionDemande()).toBeNull();
  });

  it('prefillFromAi filtre actes IA hors enum', () => {
    component.aiData = {
      actesEnvisagesDetected: ['GESTION_PATRIMOINE', 'BOGUS', 'DECISIONS_SANTE'],
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.actesEnvisages()).toEqual(['GESTION_PATRIMOINE', 'DECISIONS_SANTE']);
  });

  // ---------------------------------------------------------------------------
  // SF-FA-25-06 : champs conditionnels selon régime + 4 nouveaux fields.
  // ---------------------------------------------------------------------------

  describe('SF-FA-25-06 : visibilité conditionnelle selon régime', () => {
    beforeEach(() => {
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      expectSourceExplanationCall();
    });

    it('SAUVEGARDE_JUSTICE → aucun champ conditionnel', () => {
      component.onRegimeChange('SAUVEGARDE_JUSTICE');
      expect(component.showIncapaciteGestionQuotidienne()).toBe(false);
      expect(component.showAltertationGrave()).toBe(false);
      expect(component.showMandatPrealableSigne()).toBe(false);
      expect(component.showFormeMandatProtection()).toBe(false);
    });

    it('HABILITATION_FAMILIALE → aucun champ conditionnel', () => {
      component.onRegimeChange('HABILITATION_FAMILIALE');
      expect(component.showIncapaciteGestionQuotidienne()).toBe(false);
      expect(component.showAltertationGrave()).toBe(false);
      expect(component.showMandatPrealableSigne()).toBe(false);
    });

    it('CURATELLE_SIMPLE → aucun champ conditionnel', () => {
      component.onRegimeChange('CURATELLE_SIMPLE');
      expect(component.showIncapaciteGestionQuotidienne()).toBe(false);
      expect(component.showAltertationGrave()).toBe(false);
      expect(component.showMandatPrealableSigne()).toBe(false);
    });

    it('CURATELLE_RENFORCEE → toggle incapaciteGestionQuotidienne visible', () => {
      component.onRegimeChange('CURATELLE_RENFORCEE');
      expect(component.showIncapaciteGestionQuotidienne()).toBe(true);
      expect(component.showAltertationGrave()).toBe(false);
      expect(component.showMandatPrealableSigne()).toBe(false);
    });

    it('TUTELLE → toggles altertationGrave + incapaciteGestionQuotidienne visibles', () => {
      component.onRegimeChange('TUTELLE');
      expect(component.showAltertationGrave()).toBe(true);
      expect(component.showIncapaciteGestionQuotidienne()).toBe(true);
      expect(component.showMandatPrealableSigne()).toBe(false);
    });

    it('MANDAT_PROTECTION_FUTURE → toggle mandatPrealableSigne visible', () => {
      component.onRegimeChange('MANDAT_PROTECTION_FUTURE');
      expect(component.showMandatPrealableSigne()).toBe(true);
      expect(component.showFormeMandatProtection()).toBe(false);
    });

    it('MANDAT_PROTECTION_FUTURE + mandatPrealableSigne=true → forme du mandat visible', () => {
      component.onRegimeChange('MANDAT_PROTECTION_FUTURE');
      component.onMandatPrealableSigneChange(true);
      expect(component.showFormeMandatProtection()).toBe(true);
    });

    it('changement régime depuis TUTELLE vers SAUVEGARDE → reset altertationGrave + incapacite', () => {
      component.onRegimeChange('TUTELLE');
      component.onAltertationGraveChange(true);
      component.onIncapaciteGestionQuotidienneChange(true);
      component.onRegimeChange('SAUVEGARDE_JUSTICE');
      expect(component.altertationGrave()).toBe(false);
      expect(component.incapaciteGestionQuotidienne()).toBe(false);
    });

    it('changement régime depuis MANDAT vers TUTELLE → reset mandatPrealableSigne + forme', () => {
      component.onRegimeChange('MANDAT_PROTECTION_FUTURE');
      component.onMandatPrealableSigneChange(true);
      component.onFormeMandatProtectionChange('NOTARIE');
      component.onRegimeChange('TUTELLE');
      expect(component.mandatPrealableSigne()).toBe(false);
      expect(component.formeMandatProtection()).toBeNull();
    });

    it('onMandatPrealableSigneChange(false) reset formeMandatProtection à null', () => {
      component.onRegimeChange('MANDAT_PROTECTION_FUTURE');
      component.onMandatPrealableSigneChange(true);
      component.onFormeMandatProtectionChange('NOTARIE');
      expect(component.formeMandatProtection()).toBe('NOTARIE');
      component.onMandatPrealableSigneChange(false);
      expect(component.formeMandatProtection()).toBeNull();
    });
  });

  describe('SF-FA-25-06 : POST avec nouveaux champs sur les 4 nouveaux régimes', () => {
    function fillBaseRequiredFields(): void {
      component.demandeurFamilial.set('ENFANT_MAJEUR');
      component.actesEnvisages.set(['GESTION_PATRIMOINE', 'DECISIONS_LOGEMENT']);
      component.dateCertificatMedical.set('2026-04-15');
      component.altertationFacultesMentales.set(true);
      component.certificatMedicalCirconstancie.set(true);
    }

    it('POST CURATELLE_RENFORCEE → body inclut incapaciteGestionQuotidienne=true', () => {
      component.onRegimeChange('CURATELLE_RENFORCEE');
      fillBaseRequiredFields();
      component.onIncapaciteGestionQuotidienneChange(true);
      component.calculate();

      const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
      expect(req.request.body.regimeProtectionDemande).toBe('CURATELLE_RENFORCEE');
      expect(req.request.body.incapaciteGestionQuotidienne).toBe(true);
      expect(req.request.body.altertationGrave).toBe(false);
      expect(req.request.body.mandatPrealableSigne).toBe(false);
      expect(req.request.body.formeMandatProtection).toBeNull();
      req.flush(defaultResponse());
    });

    it('POST TUTELLE → body inclut altertationGrave=true et incapacite', () => {
      component.onRegimeChange('TUTELLE');
      fillBaseRequiredFields();
      component.onAltertationGraveChange(true);
      component.onIncapaciteGestionQuotidienneChange(true);
      component.calculate();

      const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
      expect(req.request.body.regimeProtectionDemande).toBe('TUTELLE');
      expect(req.request.body.altertationGrave).toBe(true);
      expect(req.request.body.incapaciteGestionQuotidienne).toBe(true);
      req.flush(defaultResponse());
    });

    it('POST MANDAT_PROTECTION_FUTURE NOTARIE → body inclut formeMandatProtection', () => {
      component.onRegimeChange('MANDAT_PROTECTION_FUTURE');
      fillBaseRequiredFields();
      component.onMandatPrealableSigneChange(true);
      component.onFormeMandatProtectionChange('NOTARIE');
      component.calculate();

      const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
      expect(req.request.body.regimeProtectionDemande).toBe('MANDAT_PROTECTION_FUTURE');
      expect(req.request.body.mandatPrealableSigne).toBe(true);
      expect(req.request.body.formeMandatProtection).toBe('NOTARIE');
      req.flush(defaultResponse());
    });

    it('POST MANDAT_PROTECTION_FUTURE SOUS_SEING_PRIVE → body inclut forme', () => {
      component.onRegimeChange('MANDAT_PROTECTION_FUTURE');
      fillBaseRequiredFields();
      component.onMandatPrealableSigneChange(true);
      component.onFormeMandatProtectionChange('SOUS_SEING_PRIVE');
      component.calculate();

      const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
      expect(req.request.body.formeMandatProtection).toBe('SOUS_SEING_PRIVE');
      req.flush(defaultResponse());
    });

    it('POST MANDAT_PROTECTION_FUTURE sans mandatPrealable → forme null', () => {
      component.onRegimeChange('MANDAT_PROTECTION_FUTURE');
      fillBaseRequiredFields();
      component.calculate();

      const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
      expect(req.request.body.mandatPrealableSigne).toBe(false);
      expect(req.request.body.formeMandatProtection).toBeNull();
      req.flush(defaultResponse());
    });

    it('Régression : POST SAUVEGARDE_JUSTICE continue de fonctionner (4 nouveaux champs à valeur par défaut)', () => {
      component.onRegimeChange('SAUVEGARDE_JUSTICE');
      fillBaseRequiredFields();
      component.calculate();

      const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
      expect(req.request.body.regimeProtectionDemande).toBe('SAUVEGARDE_JUSTICE');
      expect(req.request.body.incapaciteGestionQuotidienne).toBe(false);
      expect(req.request.body.altertationGrave).toBe(false);
      expect(req.request.body.mandatPrealableSigne).toBe(false);
      expect(req.request.body.formeMandatProtection).toBeNull();
      req.flush(defaultResponse());
    });
  });

  describe('SF-FA-25-06 : formValid avec les nouveaux champs', () => {
    function fillBaseRequiredFields(): void {
      component.demandeurFamilial.set('ENFANT_MAJEUR');
      component.actesEnvisages.set(['GESTION_PATRIMOINE']);
      component.dateCertificatMedical.set('2026-04-15');
      component.altertationFacultesMentales.set(true);
    }

    it('formValid TRUE pour CURATELLE_SIMPLE sans champ pivot supplémentaire', () => {
      component.regimeProtectionDemande.set('CURATELLE_SIMPLE');
      fillBaseRequiredFields();
      expect(component.formValid()).toBe(true);
    });

    it('formValid FALSE si MANDAT_PROTECTION_FUTURE + mandatPrealable=true sans forme', () => {
      component.regimeProtectionDemande.set('MANDAT_PROTECTION_FUTURE');
      fillBaseRequiredFields();
      component.mandatPrealableSigne.set(true);
      component.formeMandatProtection.set(null);
      expect(component.formValid()).toBe(false);
    });

    it('formValid TRUE si MANDAT_PROTECTION_FUTURE + mandatPrealable=true + forme renseignée', () => {
      component.regimeProtectionDemande.set('MANDAT_PROTECTION_FUTURE');
      fillBaseRequiredFields();
      component.mandatPrealableSigne.set(true);
      component.formeMandatProtection.set('NOTARIE');
      expect(component.formValid()).toBe(true);
    });

    it('formValid TRUE si MANDAT_PROTECTION_FUTURE sans mandatPrealable (forme inutile)', () => {
      component.regimeProtectionDemande.set('MANDAT_PROTECTION_FUTURE');
      fillBaseRequiredFields();
      component.mandatPrealableSigne.set(false);
      component.formeMandatProtection.set(null);
      expect(component.formValid()).toBe(true);
    });
  });

  describe('SF-FA-25-06 : pré-fill IA des 4 nouveaux champs', () => {
    it('prefillFromAi incapaciteGestionQuotidienneDetected=true → champ + provenance', () => {
      component.aiData = {
        incapaciteGestionQuotidienneDetected: true,
      } as FamilleExtractedData;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      expectSourceExplanationCall();

      expect(component.incapaciteGestionQuotidienne()).toBe(true);
      expect(component.provenanceIncapaciteGestionQuotidienne()).toBe('IA');
    });

    it('prefillFromAi altertationGraveDetected=true → champ + provenance', () => {
      component.aiData = {
        altertationGraveDetected: true,
      } as FamilleExtractedData;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      expectSourceExplanationCall();

      expect(component.altertationGrave()).toBe(true);
      expect(component.provenanceAltertationGrave()).toBe('IA');
    });

    it('prefillFromAi mandatPrealableSigneDetected + formeMandatProtectionDetected', () => {
      component.aiData = {
        mandatPrealableSigneDetected: true,
        formeMandatProtectionDetected: 'NOTARIE',
      } as FamilleExtractedData;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      expectSourceExplanationCall();

      expect(component.mandatPrealableSigne()).toBe(true);
      expect(component.provenanceMandatPrealableSigne()).toBe('IA');
      expect(component.formeMandatProtection()).toBe('NOTARIE');
      expect(component.provenanceFormeMandatProtection()).toBe('IA');
    });

    it('prefillFromAi formeMandatProtection IA hors enum ignorée', () => {
      component.aiData = {
        formeMandatProtectionDetected: 'INEXISTANT',
      } as FamilleExtractedData;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      expectSourceExplanationCall();

      expect(component.formeMandatProtection()).toBeNull();
      expect(component.provenanceFormeMandatProtection()).toBeNull();
    });

    it('handler onIncapaciteGestionQuotidienneChange efface badge IA', () => {
      component.aiData = {
        incapaciteGestionQuotidienneDetected: true,
      } as FamilleExtractedData;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      expectSourceExplanationCall();

      expect(component.provenanceIncapaciteGestionQuotidienne()).toBe('IA');
      component.onIncapaciteGestionQuotidienneChange(false);
      expect(component.provenanceIncapaciteGestionQuotidienne()).toBeNull();
    });

    it('handler onFormeMandatProtectionChange efface badge IA', () => {
      component.aiData = {
        mandatPrealableSigneDetected: true,
        formeMandatProtectionDetected: 'NOTARIE',
      } as FamilleExtractedData;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      expectSourceExplanationCall();

      expect(component.provenanceFormeMandatProtection()).toBe('IA');
      component.onFormeMandatProtectionChange('SOUS_SEING_PRIVE');
      expect(component.formeMandatProtection()).toBe('SOUS_SEING_PRIVE');
      expect(component.provenanceFormeMandatProtection()).toBeNull();
    });
  });

  describe('SF-FA-25-06 : applyPersistedResult reprend les 4 nouveaux champs', () => {
    it('GET 200 avec champs SF-06 → form rempli avec les valeurs persistées', () => {
      const r = defaultResponse();
      r.regimeProtectionDemande = 'MANDAT_PROTECTION_FUTURE';
      r.incapaciteGestionQuotidienne = false;
      r.altertationGrave = false;
      r.mandatPrealableSigne = true;
      r.formeMandatProtection = 'NOTARIE';
      r.eligible = true;
      r.criteresNonRemplis = [];

      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush(r);
      expectSourceExplanationCall();

      expect(component.mandatPrealableSigne()).toBe(true);
      expect(component.formeMandatProtection()).toBe('NOTARIE');
      expect(component.provenanceMandatPrealableSigne()).toBeNull();
      expect(component.provenanceFormeMandatProtection()).toBeNull();
    });

    it('applyPersistedResult formeMandatProtection invalide → null', () => {
      const r = defaultResponse();
      // Force une valeur invalide pour vérifier la défense.
      (r as { formeMandatProtection?: string }).formeMandatProtection = 'BOGUS';

      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush(r);
      expectSourceExplanationCall();

      expect(component.formeMandatProtection()).toBeNull();
    });
  });

  describe('SF-FA-25-06 : éligibilité + criteresNonRemplis dans le résultat', () => {
    it('Réponse eligible=false + criteresNonRemplis exposés dans result()', () => {
      const r = defaultResponse();
      r.regimeProtectionDemande = 'TUTELLE';
      r.eligible = false;
      r.criteresNonRemplis = [
        'Altération grave et durable (art. 440 al. 3) — basculer vers curatelle',
        'Au moins 2 catégories d\'actes envisagés',
      ];

      component.regimeProtectionDemande.set('TUTELLE');
      component.demandeurFamilial.set('ENFANT_MAJEUR');
      component.actesEnvisages.set(['GESTION_PATRIMOINE']);
      component.dateCertificatMedical.set('2026-04-15');
      component.altertationFacultesMentales.set(true);
      component.certificatMedicalCirconstancie.set(true);
      component.calculate();

      const req = httpMock.expectOne((rr) => rr.method === 'POST' && rr.url === BASE_URL);
      req.flush(r);

      expect(component.result()!.eligible).toBe(false);
      expect(component.result()!.criteresNonRemplis?.length).toBe(2);
    });

    it('formeMandatProtectionLabel mappe les 2 codes', () => {
      expect(component.formeMandatProtectionLabel('NOTARIE')).toContain('notarié');
      expect(component.formeMandatProtectionLabel('SOUS_SEING_PRIVE')).toContain('sous seing privé');
    });
  });
});
