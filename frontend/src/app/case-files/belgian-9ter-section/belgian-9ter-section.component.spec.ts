import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { SimpleChange } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Belgian9terSectionComponent } from './belgian-9ter-section.component';
import { Belgian9terResponse } from '../../core/models/belgian-9ter.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';

describe('Belgian9terSectionComponent', () => {
  let component: Belgian9terSectionComponent;
  let fixture: ComponentFixture<Belgian9terSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/belgian-9ter';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  /**
   * SF-155-09 : absorbe la requête source-explanations émise par ngOnInit
   * pour éviter les crashs sur httpMock.verify().
   */
  function flushSourceExplanationCalls(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  function beResponse(overrides: Partial<Belgian9terResponse> = {}): Belgian9terResponse {
    return {
      caseFileId: 'case-1',
      dateDebutSymptomes: '2026-01-10',
      maladieGraveCertifiee: true,
      soinsNecessairesDisponiblesBe: true,
      soinsInaccessiblesPaysOrigine: true,
      menaceOrdrePublic: false,
      dateDepotDemande: '2026-02-15',
      country: 'BELGIQUE',
      certificatMedicalType1Ok: true,
      soinsRequisOk: true,
      inaccessibiliteOk: true,
      pasMenace: true,
      scoreGlobal: 100,
      verdictProbabiliteAcceptation: 'ELEVEE',
      criteresNonRemplis: [],
      dateExpirationInstruction: '2027-02-15',
      formule: 'Régularisation 9ter médical : score 100/100, probabilité ELEVEE.',
      baseJuridique: 'Loi 15/12/1980 art. 9ter + AR 17/05/2007 art. 7-8',
      messages: ['Procédure 9ter — instruction OE 6-24 mois.'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        Belgian9terSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Belgian9terSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    flushSourceExplanationCalls();
    httpMock.verify();
  });

  it('mount : composant créé, signaux à leur défaut', () => {
    expect(component).toBeTruthy();
    expect(component.collapsed()).toBe(true);
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
    expect(component.maladieGraveCertifiee()).toBe(false);
    expect(component.soinsNecessairesDisponiblesBe()).toBe(false);
    expect(component.soinsInaccessiblesPaysOrigine()).toBe(false);
    expect(component.menaceOrdrePublic()).toBe(false);
    expect(component.dateDebutSymptomes()).toBeNull();
    expect(component.dateDepotDemande()).toBeNull();
  });

  it('BELGIQUE → isBelgium() true, GET appelé au ngOnInit', () => {
    expect(component.isBelgium()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('FRANCE → isBelgium() false, pas d\'appel HTTP au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isBelgium()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('charge l\'analyse existante si présente (GET 200) → result + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(beResponse());
    expect(component.result()!.scoreGlobal).toBe(100);
    expect(component.result()!.verdictProbabiliteAcceptation).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(component.dateDebutSymptomes()).toBe('2026-01-10');
    expect(component.maladieGraveCertifiee()).toBe(true);
    expect(component.soinsNecessairesDisponiblesBe()).toBe(true);
    expect(component.soinsInaccessiblesPaysOrigine()).toBe(true);
    expect(component.menaceOrdrePublic()).toBe(false);
    expect(component.dateDepotDemande()).toBe('2026-02-15');
  });

  it('GET 404 → reste en mode formulaire', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid : tous booléens à false → valide (le backend retournera FAIBLE)', () => {
    expect(component.formValid()).toBe(true);
  });

  it('formValid : refuse dateDebutSymptomes dans le futur', () => {
    const future = new Date();
    future.setDate(future.getDate() + 10);
    const futureIso = future.toISOString().slice(0, 10);
    component.dateDebutSymptomes.set(futureIso);
    expect(component.formValid()).toBe(false);
  });

  it('formValid : refuse dateDepotDemande < dateDebutSymptomes', () => {
    component.dateDebutSymptomes.set('2026-02-01');
    component.dateDepotDemande.set('2026-01-15');
    expect(component.formValid()).toBe(false);

    component.dateDepotDemande.set('2026-02-15');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST → body complet, snack OK, refresh triggered, showForm=false', () => {
    component.dateDebutSymptomes.set('2026-01-10');
    component.maladieGraveCertifiee.set(true);
    component.soinsNecessairesDisponiblesBe.set(true);
    component.soinsInaccessiblesPaysOrigine.set(true);
    component.menaceOrdrePublic.set(false);
    component.dateDepotDemande.set('2026-02-15');
    component.analyze();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateDebutSymptomes: '2026-01-10',
      maladieGraveCertifiee: true,
      soinsNecessairesDisponiblesBe: true,
      soinsInaccessiblesPaysOrigine: true,
      menaceOrdrePublic: false,
      dateDepotDemande: '2026-02-15',
    });
    req.flush(beResponse());

    expect(component.result()!.scoreGlobal).toBe(100);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Régularisation 9ter analysée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() ignoré si formValid=false (date dans le futur)', () => {
    const future = new Date();
    future.setDate(future.getDate() + 5);
    component.dateDebutSymptomes.set(future.toISOString().slice(0, 10));
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() erreur backend → snack-error, analyzing reset', () => {
    component.maladieGraveCertifiee.set(true);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.analyzing()).toBe(false);
  });

  it('bannerClass : ELEVEE=success, MOYENNE=info, FAIBLE=warning', () => {
    expect(component.bannerClass('ELEVEE')).toContain('belgian-9ter-banner--success');
    expect(component.bannerClass('MOYENNE')).toContain('belgian-9ter-banner--info');
    expect(component.bannerClass('FAIBLE')).toContain('belgian-9ter-banner--warning');
    expect(component.bannerClass(null)).toBe('belgian-9ter-banner');
  });

  it('bannerIcon : ELEVEE=check_circle, MOYENNE=info_outline, FAIBLE=warning', () => {
    expect(component.bannerIcon('ELEVEE')).toBe('check_circle');
    expect(component.bannerIcon('MOYENNE')).toBe('info_outline');
    expect(component.bannerIcon('FAIBLE')).toBe('warning');
  });

  it('verdictLabel renvoie un libellé humain', () => {
    expect(component.verdictLabel('ELEVEE')).toBe('Probabilité élevée');
    expect(component.verdictLabel('MOYENNE')).toBe('Probabilité moyenne');
    expect(component.verdictLabel('FAIBLE')).toBe('Probabilité faible');
    expect(component.verdictLabel(null)).toBe('');
  });

  it('hasCriticalMenace : true si pasMenace=false (menace présente)', () => {
    expect(component.hasCriticalMenace(beResponse({ pasMenace: false }))).toBe(true);
    expect(component.hasCriticalMenace(beResponse({ pasMenace: true }))).toBe(false);
    expect(component.hasCriticalMenace(null)).toBe(false);
  });

  it('toggleCollapse() inverse l\'état collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode() → showForm=true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('pré-fill IA gracieux : aiData absent → no-op, pas d\'exception', () => {
    component.aiData = undefined;
    expect(() => {
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    }).not.toThrow();
    expect(component.maladieGraveCertifiee()).toBe(false);
    expect(component.provenanceMaladieGrave()).toBeNull();
  });

  it('pré-fill IA gracieux : aiData fourni mais sans champ médical → no-op', () => {
    component.aiData = { dateNotificationOqtf: '2026-01-01' } as any;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.maladieGraveCertifiee()).toBe(false);
    expect(component.dateDebutSymptomes()).toBeNull();
    expect(component.provenanceMaladieGrave()).toBeNull();
    expect(component.provenanceDateDebutSymptomes()).toBeNull();
  });

  it('pré-fill IA si champs médicaux présents (clés tolérées) → signaux + provenance IA', () => {
    component.aiData = {
      // ImmigrationExtractedData ne déclare pas ces clés ; le composant
      // les consomme défensivement (cast any).
      dateDebutSymptomes: '2026-01-15',
      maladieGrave: true,
      soinsNecessairesDisponiblesBe: true,
    } as any;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateDebutSymptomes()).toBe('2026-01-15');
    expect(component.provenanceDateDebutSymptomes()).toBe('IA');
    expect(component.maladieGraveCertifiee()).toBe(true);
    expect(component.provenanceMaladieGrave()).toBe('IA');
    expect(component.soinsNecessairesDisponiblesBe()).toBe(true);
    expect(component.provenanceSoinsBe()).toBe('IA');
  });

  it('onMaladieGraveChange efface la provenance IA', () => {
    component.provenanceMaladieGrave.set('IA');
    component.onMaladieGraveChange(true);
    expect(component.maladieGraveCertifiee()).toBe(true);
    expect(component.provenanceMaladieGrave()).toBeNull();
  });

  it('onDateDebutSymptomesChange efface la provenance IA', () => {
    component.provenanceDateDebutSymptomes.set('IA');
    component.onDateDebutSymptomesChange('2026-03-01');
    expect(component.dateDebutSymptomes()).toBe('2026-03-01');
    expect(component.provenanceDateDebutSymptomes()).toBeNull();
  });

  it('ngOnChanges(aiData) en mode formulaire sans result → re-prefill appliqué', () => {
    component.aiData = undefined;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceMaladieGrave()).toBeNull();

    component.aiData = { maladieGrave: true } as any;
    component.ngOnChanges({
      aiData: new SimpleChange(undefined, component.aiData, false),
    });

    expect(component.maladieGraveCertifiee()).toBe(true);
    expect(component.provenanceMaladieGrave()).toBe('IA');
  });

  it('ngOnChanges(aiData) firstChange → pas de re-prefill (ngOnInit s\'en charge)', () => {
    component.aiData = { maladieGrave: true } as any;
    component.ngOnChanges({
      aiData: new SimpleChange(undefined, component.aiData, true),
    });
    // En firstChange ngOnChanges ne lance pas prefillFromAi (évite double appel).
    expect(component.maladieGraveCertifiee()).toBe(false);
    expect(component.provenanceMaladieGrave()).toBeNull();
  });

  it('FRANCE : prefillFromAi non appelé (gate isBelgium)', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = { maladieGrave: true } as any;
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.maladieGraveCertifiee()).toBe(false);
  });

  it('résultat : score 50 (MOYENNE) → bannière info', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({
      scoreGlobal: 50,
      verdictProbabiliteAcceptation: 'MOYENNE',
      certificatMedicalType1Ok: true,
      soinsRequisOk: true,
      inaccessibiliteOk: false,
      pasMenace: true,
      criteresNonRemplis: ['Inaccessibilité des soins au pays d\'origine non prouvée'],
    }));
    expect(component.result()!.scoreGlobal).toBe(50);
    expect(component.bannerClass('MOYENNE')).toContain('--info');
  });

  it('résultat : score 0 (FAIBLE) avec menace ordre public → hasCriticalMenace=true', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({
      scoreGlobal: 0,
      verdictProbabiliteAcceptation: 'FAIBLE',
      maladieGraveCertifiee: false,
      certificatMedicalType1Ok: false,
      soinsNecessairesDisponiblesBe: false,
      soinsRequisOk: false,
      soinsInaccessiblesPaysOrigine: false,
      inaccessibiliteOk: false,
      menaceOrdrePublic: true,
      pasMenace: false,
      criteresNonRemplis: ['Certificat médical non produit', 'Menace ordre public présumée'],
    }));
    expect(component.result()!.scoreGlobal).toBe(0);
    expect(component.hasCriticalMenace(component.result())).toBe(true);
    expect(component.bannerClass('FAIBLE')).toContain('--warning');
  });

  // -----------------------------------------------------------------------
  // SF-155-09 — Validation F-IA-03 : coherenceAlerts + popover trigger.
  // -----------------------------------------------------------------------

  it('SF-155-09 : coherenceAlerts vide par défaut (aucune source IA/F96/Q/PM)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.coherenceAlerts()).toEqual({});
    expect(component.alertsSummary().total).toBe(0);
  });

  it('SF-155-09 : coherenceAlerts reste vide après showForm=false (mode résultat)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse());
    // En mode résultat, pas d'alertes affichées (pattern SF-IA-03-12).
    expect(component.showForm()).toBe(false);
    expect(component.coherenceAlerts()).toEqual({});
  });

  it('SF-155-09 : source IA — divergence sur dateDebutSymptomes déclenche alerte', () => {
    component.aiData = { dateDebutSymptomes: '2026-01-15' } as any;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // IA pré-remplit à '2026-01-15' → provenance IA. Avocat modifie à autre date.
    expect(component.dateDebutSymptomes()).toBe('2026-01-15');
    component.onDateDebutSymptomesChange('2026-03-01');
    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_DEBUT_SYMPTOMES).toBeTruthy();
    expect(alerts.DATE_DEBUT_SYMPTOMES!.source).toBe('IA');
    expect(alerts.DATE_DEBUT_SYMPTOMES!.expectedDisplay).toBe('2026-01-15');
    expect(component.alertsSummary().total).toBe(1);
  });

  it('SF-155-09 : source F96 — maladieGraveCertifiee attendue TRUE, avocat saisit FALSE', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    const checks: ProcedureCheck[] = [
      {
        id: 'c1', ordre: 1, description: 'Vérifier CM1+CM2', statut: 'VERIFIED',
        critereCode: 'BE_9TER_MALADIE_GRAVE', expectedValue: 'TRUE',
        raison: 'CM1 daté du 10/01',
      },
    ];
    component.procedureChecks = checks;
    component.ngOnChanges({ procedureChecks: new SimpleChange(null, checks, false) });
    // Touch + conserver FALSE (désaccord avec F96).
    component.onMaladieGraveChange(false);
    const alerts = component.coherenceAlerts();
    expect(alerts.MALADIE_GRAVE).toBeTruthy();
    expect(alerts.MALADIE_GRAVE!.source).toBe('F96');
    expect(alerts.MALADIE_GRAVE!.expectedDisplay).toBe('Maladie grave certifiée');
    expect(alerts.MALADIE_GRAVE!.contributors).toContain('F96');
  });

  it('SF-155-09 : source QUESTION_IA — soinsNecessairesDisponiblesBe attendu FALSE, avocat saisit TRUE', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    const questions: AiQuestion[] = [
      {
        id: 'q1', orderIndex: 1,
        questionText: 'Les soins sont-ils disponibles en Belgique ?',
        answerText: 'oui',
        critereCode: 'BE_9TER_SOINS_BE', expectedValue: 'FALSE',
      },
    ];
    component.aiQuestions = questions;
    component.ngOnChanges({ aiQuestions: new SimpleChange(null, questions, false) });
    component.onSoinsBeChange(true);
    const alerts = component.coherenceAlerts();
    expect(alerts.SOINS_BE).toBeTruthy();
    expect(alerts.SOINS_BE!.source).toBe('QUESTION_IA');
    expect(alerts.SOINS_BE!.expectedDisplay).toBe('Soins indisponibles en Belgique');
  });

  it('SF-155-09 : source PIECE_MANQUANTE (seule) — n\'émet pas d\'alerte sans divergence IA/F96/Q', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // PIECE_MANQUANTE seule ne déclenche pas l'alerte (addPieceManquante ignore
    // si aucune autre source n'a initié l'alerte — pattern builder).
    const pieces: PieceManquanteEntry[] = [
      { texte: 'Certificat médical CM2 manquant', critereCode: 'BE_9TER_MALADIE_GRAVE' },
    ];
    component.piecesManquantes = pieces;
    component.ngOnChanges({ piecesManquantes: new SimpleChange(null, pieces, false) });
    component.onMaladieGraveChange(false);
    const alerts = component.coherenceAlerts();
    expect(alerts.MALADIE_GRAVE).toBeUndefined();
  });

  it('SF-155-09 : source MULTI — IA + F96 convergent, contributors=2, source=MULTI', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { soinsInaccessiblesPaysOrigine: true } as any;
    component.ngOnChanges({ aiData: new SimpleChange(undefined, component.aiData, false) });
    const checks: ProcedureCheck[] = [
      {
        id: 'c2', ordre: 2, description: 'Accessibilité pays origine', statut: 'VERIFIED',
        critereCode: 'BE_9TER_SOINS_INACCESSIBLES', expectedValue: 'TRUE',
      },
    ];
    component.procedureChecks = checks;
    component.ngOnChanges({ procedureChecks: new SimpleChange(null, checks, false) });
    // L'IA avait pré-rempli à `true` (soinsInaccessibles). L'avocat désactive.
    component.onSoinsInaccessiblesChange(false);
    const alerts = component.coherenceAlerts();
    expect(alerts.SOINS_INACCESSIBLES).toBeTruthy();
    expect(alerts.SOINS_INACCESSIBLES!.source).toBe('MULTI');
    expect(alerts.SOINS_INACCESSIBLES!.contributors.length).toBeGreaterThanOrEqual(2);
    expect(alerts.SOINS_INACCESSIBLES!.contributors).toContain('F96');
    expect(alerts.SOINS_INACCESSIBLES!.contributors).toContain('IA');
  });

  it('SF-155-09 : alertTooltip / alertBadgeLabel — libellés cohérents', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { dateDebutSymptomes: '2026-01-15' } as any;
    component.ngOnChanges({ aiData: new SimpleChange(undefined, component.aiData, false) });
    component.onDateDebutSymptomesChange('2026-03-01');
    const alert = component.coherenceAlerts().DATE_DEBUT_SYMPTOMES!;
    expect(alert).toBeTruthy();
    expect(component.alertBadgeLabel(alert)).toContain('2026-01-15');
    expect(component.alertBadgeLabel(alert)).toContain('Incohérence');
    expect(component.alertTooltip(alert)).toContain('Analyse du dossier');
  });

  it('SF-155-09 : explanationFor mapping → clés sourceExplanations', () => {
    const map = new Map<string, any[]>();
    map.set('date_debut_symptomes', [{ actionType: 'SYNTHESIS' }]);
    map.set('BE_9TER_MALADIE_GRAVE', [{ actionType: 'F96' }]);
    map.set('BE_9TER_SOINS_BE', [{ actionType: 'QUESTION_IA' }]);
    map.set('BE_9TER_SOINS_INACCESSIBLES', [{ actionType: 'SYNTHESIS' }]);
    component.sourceExplanations.set(map);
    expect(component.explanationFor('DATE_DEBUT_SYMPTOMES').length).toBe(1);
    expect(component.explanationFor('MALADIE_GRAVE').length).toBe(1);
    expect(component.explanationFor('SOINS_BE').length).toBe(1);
    expect(component.explanationFor('SOINS_INACCESSIBLES').length).toBe(1);
    expect(component.explanationFor('MENACE_ORDRE_PUBLIC').length).toBe(0);
  });

  it('SF-155-09 : FRANCE → coherenceAlerts vide (gate pays BE-only)', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = { dateDebutSymptomes: '2026-01-15' } as any;
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    component.onDateDebutSymptomesChange('2026-03-01');
    expect(component.coherenceAlerts()).toEqual({});
  });
});
