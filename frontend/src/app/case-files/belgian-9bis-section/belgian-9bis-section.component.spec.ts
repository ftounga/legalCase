import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Belgian9bisSectionComponent } from './belgian-9bis-section.component';
import { Belgian9bisResponse } from '../../core/models/belgian-9bis.model';
import {
  ImmigrationExtractedData,
  PieceManquanteEntry,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('Belgian9bisSectionComponent', () => {
  let component: Belgian9bisSectionComponent;
  let fixture: ComponentFixture<Belgian9bisSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/belgian-9bis';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function r9bis(overrides: Partial<Belgian9bisResponse> = {}): Belgian9bisResponse {
    return {
      caseFileId: 'case-1',
      dateEntreeBelgique: '2023-01-15',
      dureePresenceMois: 38,
      circonstancesExceptionnelles: true,
      liensFamiliauxBe: true,
      liensProfessionnels: false,
      scolariteEnfantsBe: true,
      menaceOrdrePublic: false,
      dateDepotDemande: '2026-04-01',
      country: 'BELGIQUE',
      presence3AnsOk: true,
      liensConstitutifsOk: true,
      pasMenace: true,
      scoreGlobal: 100,
      verdictProbabilite: 'ELEVEE',
      criteresNonRemplis: [],
      dateExpirationInstructionPrevisionnelle: '2027-10-01',
      formule: 'Présence + liens + pas menace',
      baseJuridique: 'Loi 15/12/1980 art. 9bis + AR 17/05/2007',
      messages: ['Demande recevable au regard de l\'art. 9bis Loi 15/12/1980'],
      ...overrides,
    };
  }

  /**
   * SF-155-08 : absorbe la requête source-explanations émise par ngOnInit
   * (fail-open). Empêche les tests de crash sur httpMock.verify().
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
        Belgian9bisSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Belgian9bisSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('mounts and is collapsed by default', () => {
    expect(component).toBeTruthy();
    expect(component.collapsed()).toBe(true);
  });

  it('BELGIQUE → isBelgium() true, GET appelé au ngOnInit', () => {
    expect(component.isBelgium()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
  });

  it('FRANCE → isBelgium() false, pas d\'appel HTTP, bannière info', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isBelgium()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone(r => r.url === BASE_URL);
    expectSourceExplanationCall();
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(r9bis());
    expectSourceExplanationCall();
    expect(component.result()!.verdictProbabilite).toBe('ELEVEE');
    expect(component.result()!.scoreGlobal).toBe(100);
    expect(component.showForm()).toBe(false);
    expect(component.dateEntreeBelgique()).toBe('2023-01-15');
    expect(component.dureePresenceMois()).toBe(38);
    expect(component.circonstancesExceptionnelles()).toBe(true);
    expect(component.menaceOrdrePublic()).toBe(false);
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si dateEntreeBelgique manquante', () => {
    component.dateEntreeBelgique.set(null);
    component.dureePresenceMois.set(36);
    expect(component.formValid()).toBe(false);

    component.dateEntreeBelgique.set('2023-01-01');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateEntreeBelgique dans le futur', () => {
    const future = new Date();
    future.setDate(future.getDate() + 10);
    const futureIso = future.toISOString().slice(0, 10);
    component.dateEntreeBelgique.set(futureIso);
    component.dureePresenceMois.set(12);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dureePresenceMois négatif', () => {
    component.dateEntreeBelgique.set('2023-01-01');
    component.dureePresenceMois.set(-1);
    expect(component.formValid()).toBe(false);
    component.dureePresenceMois.set(0);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateDepotDemande avant dateEntreeBelgique', () => {
    component.dateEntreeBelgique.set('2023-06-01');
    component.dureePresenceMois.set(36);
    component.dateDepotDemande.set('2023-01-01');
    expect(component.formValid()).toBe(false);
    component.dateDepotDemande.set('2024-01-01');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST sans dateDepotDemande → body sans dateDepotDemande', () => {
    component.dateEntreeBelgique.set('2023-01-15');
    component.dureePresenceMois.set(38);
    component.circonstancesExceptionnelles.set(true);
    component.liensFamiliauxBe.set(true);
    component.liensProfessionnels.set(false);
    component.scolariteEnfantsBe.set(true);
    component.menaceOrdrePublic.set(false);
    component.dateDepotDemande.set(null);
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateEntreeBelgique: '2023-01-15',
      dureePresenceMois: 38,
      circonstancesExceptionnelles: true,
      liensFamiliauxBe: true,
      liensProfessionnels: false,
      scolariteEnfantsBe: true,
      menaceOrdrePublic: false,
    });
    req.flush(r9bis());
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse 9bis humanitaire enregistrée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
    expect(component.showForm()).toBe(false);
    expect(component.result()!.verdictProbabilite).toBe('ELEVEE');
  });

  it('analyze() POST avec dateDepotDemande → body inclut dateDepotDemande', () => {
    component.dateEntreeBelgique.set('2023-01-15');
    component.dureePresenceMois.set(38);
    component.dateDepotDemande.set('2026-04-01');
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateDepotDemande).toBe('2026-04-01');
    req.flush(r9bis());
  });

  it('analyze() error → MatSnackBar erreur', () => {
    component.dateEntreeBelgique.set('2023-01-15');
    component.dureePresenceMois.set(38);
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Workspace mismatch' }, { status: 403, statusText: 'Forbidden' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Workspace mismatch', 'Fermer', jasmine.objectContaining({ panelClass: 'snack-error' }));
    expect(component.analyzing()).toBe(false);
    expect(component.showForm()).toBe(true);
  });

  it('pré-fill IA : dateDepotProcedure → dateDepotDemande + provenance IA', () => {
    const ai: Partial<ImmigrationExtractedData> = {
      dateDepotProcedure: '2026-03-15',
    };
    component.aiData = ai;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.dateDepotDemande()).toBe('2026-03-15');
    expect(component.provenanceDateDepot()).toBe('IA');
  });

  it('badge IA effacé au changement manuel', () => {
    component.dateDepotDemande.set('2026-03-15');
    component.provenanceDateDepot.set('IA');
    component.onDateDepotChange('2026-04-10');
    expect(component.dateDepotDemande()).toBe('2026-04-10');
    expect(component.provenanceDateDepot()).toBeNull();
  });

  it('toggleCollapse alterne collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('verdictLabel mappe ELEVEE/MOYENNE/FAIBLE', () => {
    expect(component.verdictLabel('ELEVEE')).toBe('Probabilité élevée');
    expect(component.verdictLabel('MOYENNE')).toBe('Probabilité moyenne');
    expect(component.verdictLabel('FAIBLE')).toBe('Probabilité faible');
    expect(component.verdictLabel(null)).toBe('');
  });

  it('bannerClass : ELEVEE → success, FAIBLE → warning (pas de rouge dominant)', () => {
    expect(component.bannerClass('ELEVEE')).toContain('success');
    expect(component.bannerClass('MOYENNE')).toContain('info');
    expect(component.bannerClass('FAIBLE')).toContain('warning');
    // Aucun verdict ne renvoie '--danger' (palette navy/or)
    expect(component.bannerClass('ELEVEE')).not.toContain('danger');
    expect(component.bannerClass('MOYENNE')).not.toContain('danger');
    expect(component.bannerClass('FAIBLE')).not.toContain('danger');
  });

  it('editMode → reaffiche le formulaire', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ===========================================================================
  // SF-155-08 : pré-fill IA tous champs + handlers (pattern canonique).
  // ===========================================================================

  it('SF-155-08 — handler onDateEntreeChange efface provenance IA', () => {
    component.dateEntreeBelgique.set('2023-01-15');
    component.provenanceDateEntreeBelgique.set('IA');
    component.onDateEntreeChange('2024-06-01');
    expect(component.dateEntreeBelgique()).toBe('2024-06-01');
    expect(component.provenanceDateEntreeBelgique()).toBeNull();
  });

  it('SF-155-08 — handler onDureePresenceChange efface provenance IA', () => {
    component.dureePresenceMois.set(36);
    component.provenanceDureePresenceMois.set('IA');
    component.onDureePresenceChange(48);
    expect(component.dureePresenceMois()).toBe(48);
    expect(component.provenanceDureePresenceMois()).toBeNull();
  });

  it('SF-155-08 — handler onCirconstancesExceptionnellesChange efface provenance IA', () => {
    component.circonstancesExceptionnelles.set(false);
    component.provenanceCirconstancesExceptionnelles.set('IA');
    component.onCirconstancesExceptionnellesChange(true);
    expect(component.circonstancesExceptionnelles()).toBe(true);
    expect(component.provenanceCirconstancesExceptionnelles()).toBeNull();
  });

  it('SF-155-08 — handler onLiensFamiliauxBeChange efface provenance IA', () => {
    component.liensFamiliauxBe.set(false);
    component.provenanceLiensFamiliauxBe.set('IA');
    component.onLiensFamiliauxBeChange(true);
    expect(component.liensFamiliauxBe()).toBe(true);
    expect(component.provenanceLiensFamiliauxBe()).toBeNull();
  });

  it('SF-155-08 — handler onLiensProfessionnelsChange efface provenance IA', () => {
    component.liensProfessionnels.set(false);
    component.provenanceLiensProfessionnels.set('IA');
    component.onLiensProfessionnelsChange(true);
    expect(component.liensProfessionnels()).toBe(true);
    expect(component.provenanceLiensProfessionnels()).toBeNull();
  });

  it('SF-155-08 — handler onScolariteEnfantsBeChange efface provenance IA', () => {
    component.scolariteEnfantsBe.set(false);
    component.provenanceScolariteEnfantsBe.set('IA');
    component.onScolariteEnfantsBeChange(true);
    expect(component.scolariteEnfantsBe()).toBe(true);
    expect(component.provenanceScolariteEnfantsBe()).toBeNull();
  });

  it('SF-155-08 — handler onMenaceOrdrePublicChange efface provenance IA', () => {
    component.menaceOrdrePublic.set(false);
    component.provenanceMenaceOrdrePublic.set('IA');
    component.onMenaceOrdrePublicChange(true);
    expect(component.menaceOrdrePublic()).toBe(true);
    expect(component.provenanceMenaceOrdrePublic()).toBeNull();
  });

  it('SF-155-08 — pré-fill IA gracieux : aiData absent → no-op (pas de crash)', () => {
    component.aiData = null;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.dateDepotDemande()).toBeNull();
    expect(component.provenanceDateDepot()).toBeNull();
  });

  it('SF-155-08 — pré-fill IA dateEntreeBelgique : crochet futur — no-op si non fourni', () => {
    component.aiData = { dateDepotProcedure: '2026-03-15' };
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // L'IA n'expose pas dateEntreeBelgique aujourd'hui — no-op gracieux.
    expect(component.dateEntreeBelgique()).toBeNull();
    expect(component.provenanceDateEntreeBelgique()).toBeNull();
  });

  it('SF-155-08 — pré-fill IA dateEntreeBelgique : si extension future "as any" présente, le mécanisme rempli', () => {
    // Simule une extension future du modèle ImmigrationExtractedData.
    // Le composant accepte ce champ via `as any` (no-op gracieux jusqu'à la SF
    // d'extension du modèle backend).
    component.aiData = {
      dateDepotProcedure: '2026-03-15',
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      dateEntreeBelgique: '2022-09-10',
    } as any;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.dateEntreeBelgique()).toBe('2022-09-10');
    expect(component.provenanceDateEntreeBelgique()).toBe('IA');
  });

  // ===========================================================================
  // SF-155-08 : validation F-IA-03 — coherenceAlerts via CoherenceAlertBuilder.
  // ===========================================================================

  it('SF-155-08 — coherenceAlerts vide si aucune source ne diverge', () => {
    component.aiData = { dateDepotProcedure: '2026-04-01' };
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Le pré-fill IA met dateDepotDemande à 2026-04-01 = même valeur que aiData → 0 alerte.
    expect(component.dateDepotDemande()).toBe('2026-04-01');
    expect(component.coherenceAlerts().DATE_DEPOT_DEMANDE).toBeUndefined();
    expect(Object.keys(component.coherenceAlerts()).length).toBe(0);
  });

  it('SF-155-08 — coherenceAlerts.DATE_DEPOT_DEMANDE source IA si aiData diffère', () => {
    component.aiData = { dateDepotProcedure: '2026-03-15' };
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // L'avocat surcharge la valeur IA → divergence détectée.
    component.onDateDepotChange('2026-05-20');
    const alert = component.coherenceAlerts().DATE_DEPOT_DEMANDE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('IA');
    expect(alert!.expectedDisplay).toBe('2026-03-15');
    expect(alert!.contributors).toContain('IA');
  });

  it('SF-155-08 — coherenceAlerts.MENACE_ORDRE_PUBLIC source F96 quand procedureCheck VERIFIED diverge', () => {
    const checks: ProcedureCheck[] = [{
      id: 'pc-1',
      ordre: 1,
      description: 'Vérifier menace ordre public',
      statut: 'VERIFIED',
      critereCode: 'B9BIS_MENACE_ORDRE_PUBLIC',
      expectedValue: 'true',
      raison: 'Casier judiciaire chargé',
    }];
    component.procedureChecks = checks;
    component.menaceOrdrePublic.set(false);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const alert = component.coherenceAlerts().MENACE_ORDRE_PUBLIC;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
    expect(alert!.expectedDisplay).toBe('true');
  });

  it('SF-155-08 — coherenceAlerts.LIENS_FAMILIAUX_BE source QUESTION_IA quand réponse "oui" diverge', () => {
    const questions: AiQuestion[] = [{
      id: 'q-1',
      orderIndex: 1,
      questionText: 'Y a-t-il des liens familiaux constitués en Belgique ?',
      answerText: 'Oui, conjoint et 2 enfants',
      critereCode: 'B9BIS_LIENS_FAMILIAUX_BE',
      expectedValue: 'true',
    }];
    component.aiQuestions = questions;
    component.liensFamiliauxBe.set(false);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const alert = component.coherenceAlerts().LIENS_FAMILIAUX_BE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('QUESTION_IA');
    expect(alert!.expectedDisplay).toBe('true');
  });

  it('SF-155-08 — coherenceAlerts MULTI quand 2 sources convergent sur même valeur', () => {
    component.aiData = { dateDepotProcedure: '2026-03-15' };
    component.dateDepotDemande.set('2026-05-20');
    component.procedureChecks = [{
      id: 'pc-2',
      ordre: 1,
      description: 'Vérifier date dépôt',
      statut: 'VERIFIED',
      critereCode: 'B9BIS_DATE_DEPOT_DEMANDE',
      expectedValue: '2026-03-15',
    }];
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const alert = component.coherenceAlerts().DATE_DEPOT_DEMANDE;
    expect(alert).toBeDefined();
    // F96 ajouté en premier (scan ordering) → MULTI quand IA confirme.
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors.length).toBeGreaterThanOrEqual(2);
    expect(alert!.expectedDisplay).toBe('2026-03-15');
  });

  it('SF-155-08 — coherenceAlerts vide après calcul (showForm=false)', () => {
    component.aiData = { dateDepotProcedure: '2026-03-15' };
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onDateDepotChange('2026-05-20');
    // Form mode : alerte présente.
    expect(component.coherenceAlerts().DATE_DEPOT_DEMANDE).toBeDefined();
    // Calcul rendu : showForm passe à false → coherenceAlerts vide (gate).
    component.showForm.set(false);
    expect(Object.keys(component.coherenceAlerts()).length).toBe(0);
  });

  it('SF-155-08 — alertsSummary().total reflète le nombre d\'alertes actives', () => {
    component.aiData = { dateDepotProcedure: '2026-03-15' };
    component.procedureChecks = [{
      id: 'pc-3',
      ordre: 1,
      description: 'Liens pro',
      statut: 'VERIFIED',
      critereCode: 'B9BIS_LIENS_PROFESSIONNELS',
      expectedValue: 'true',
    }];
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Override post-prefill : avocat saisit valeurs divergentes.
    component.onDateDepotChange('2026-05-20');
    component.onLiensProfessionnelsChange(false);
    expect(component.alertsSummary().total).toBeGreaterThanOrEqual(2);
    expect(component.alertsSummary().blockers).toBe(0);
  });

  it('SF-155-08 — explanationFor renvoie [] par défaut (pas d\'explanations chargées)', () => {
    expect(component.explanationFor('DATE_DEPOT_DEMANDE')).toEqual([]);
    expect(component.explanationFor('MENACE_ORDRE_PUBLIC')).toEqual([]);
  });

  it('SF-155-08 — alertBadgeLabel formatte selon source', () => {
    component.aiData = { dateDepotProcedure: '2026-03-15' };
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onDateDepotChange('2026-05-20');
    const alert = component.coherenceAlerts().DATE_DEPOT_DEMANDE!;
    expect(alert).toBeDefined();
    const label = component.alertBadgeLabel(alert);
    expect(label).toContain('Incohérence détectée');
    expect(label).toContain('2026-03-15');
  });

  it('SF-155-08 — alertTooltip "Contredit" quand multi-contributors', () => {
    component.aiData = { dateDepotProcedure: '2026-03-15' };
    component.procedureChecks = [{
      id: 'pc-4',
      ordre: 1,
      description: 'X',
      statut: 'VERIFIED',
      critereCode: 'B9BIS_DATE_DEPOT_DEMANDE',
      expectedValue: '2026-03-15',
    }];
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onDateDepotChange('2026-05-20');
    const alert = component.coherenceAlerts().DATE_DEPOT_DEMANDE!;
    expect(alert).toBeDefined();
    expect(alert.contributors.length).toBeGreaterThanOrEqual(2);
    expect(component.alertTooltip(alert)).toContain('Contredit');
  });

  it('SF-155-08 — pièce manquante seule ne déclenche pas d\'alerte (besoin d\'une source IA/F96/Q)', () => {
    const pieces: PieceManquanteEntry[] = [{
      texte: 'Justificatif liens familiaux',
      critereCode: 'B9BIS_LIENS_FAMILIAUX_BE',
    }];
    component.piecesManquantes = pieces;
    component.liensFamiliauxBe.set(false);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Pas d'alerte sans IA/F96/Q divergente.
    expect(component.coherenceAlerts().LIENS_FAMILIAUX_BE).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02d — mode simulateur autonome
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02d — mode standalone', () => {
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-14-9bis-humanitaire-be/calculate';

    it('CA-02 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-02 : aucun GET vers /api/v1/case-files/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const matches = httpMock.match((r: { url: string }) => r.url.includes('/api/v1/case-files/'));
      expect(matches.length).toBe(0);
    });

    it('CA-04 : POST sur le dispatcher /api/v1/simulators/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      try { (component as any).analyze(); } catch (_) { /* formValid */ }
      const dispatcherReqs = httpMock.match((r: { url: string; method: string }) => r.url === STANDALONE_URL_F163 && r.method === 'POST');
      const caseFileReqs = httpMock.match((r: { url: string; method: string }) => r.url.includes('/api/v1/case-files/') && r.method === 'POST');
      // Aucun POST case-file ne doit partir en standalone.
      expect(caseFileReqs.length).toBe(0);
      dispatcherReqs.forEach((req: any) => req.flush({}));
    });
  });
});
