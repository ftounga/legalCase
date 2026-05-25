import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LiquidationPartageBeSectionComponent } from './liquidation-partage-be-section.component';
import { LiquidationPartageBeResponse } from '../../core/models/liquidation-partage-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ProcedureCheck } from '../../core/models/procedure-check.model';

describe('LiquidationPartageBeSectionComponent', () => {
  let component: LiquidationPartageBeSectionComponent;
  let fixture: ComponentFixture<LiquidationPartageBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/liquidation-partage-be';

  function response(verdict: LiquidationPartageBeResponse['verdict'])
      : LiquidationPartageBeResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs ré-exposé par la réponse (ré-édition du formulaire).
      notaireDesigne: true,
      dateDesignationNotaire: '2026-01-10',
      operationsOuvertes: true,
      dateOuvertureOperations: '2026-01-20',
      inventaireEtabli: true,
      projetLiquidationEtabli: verdict !== 'PROCEDURE_NON_ENGAGEE',
      dateNotificationProjet: verdict !== 'PROCEDURE_NON_ENGAGEE' ? '2026-04-25' : null,
      contreditsDeposes: false,
      procesVerbalDiresEtabli: verdict === 'EN_ATTENTE_HOMOLOGATION' || verdict === 'CLOTUREE',
      homologationDemandee: verdict === 'CLOTUREE',
      dateHomologation: verdict === 'CLOTUREE' ? '2026-05-10' : null,
      commentaire: null,
      // Champs calculés.
      verdict,
      etapes: [{
        code: 'DESIGNATION_NOTAIRE', libelle: 'Désignation du notaire commis',
        statut: 'FAITE', ordre: 1,
        fondement: 'CJ art. 1207', explication: 'Le notaire est désigné par le juge.',
      }],
      delais: [{
        code: 'DELAI_CONTREDITS',
        libelle: 'Délai de contredits sur le projet de liquidation-partage',
        fondement: 'CJ art. 1218',
        dateDepart: verdict !== 'PROCEDURE_NON_ENGAGEE' ? '2026-04-25' : null,
        dateEcheance: verdict !== 'PROCEDURE_NON_ENGAGEE' ? '2026-05-25' : null,
        joursRestants: verdict === 'DELAI_CONTREDITS_CRITIQUE' ? 3 : 30,
        statut: verdict === 'DELAI_CONTREDITS_CRITIQUE' ? 'CRITIQUE' : 'OK',
      }],
      prochaineEtape: verdict === 'CLOTUREE' ? null : 'Déposer les contredits éventuels',
      basesJuridiques: ['CJ art. 1207 et s.', 'CJ art. 1218'],
      messages: ['Vérifier la disponibilité du projet de liquidation-partage au dossier.'],
      country: 'BELGIQUE',
      calculatedAt: '2026-05-17T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        LiquidationPartageBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LiquidationPartageBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ---------------------------------------------------------------------------
  // Chargement (GET)
  // ---------------------------------------------------------------------------

  it('mount BELGIQUE → GET initial déclenché', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it('mount FRANCE → pas d\'appel HTTP', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('GET 200 → result rechargé + showForm=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('EN_COURS'));
    expect(component.result()!.verdict).toBe('EN_COURS');
    expect(component.showForm()).toBe(false);
  });

  it('GET 200 → formulaire ré-hydraté depuis le snapshot d\'inputs', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('EN_COURS'));
    expect(component.notaireDesigne()).toBe(true);
    expect(component.dateDesignationNotaire()).toBe('2026-01-10');
    expect(component.projetLiquidationEtabli()).toBe(true);
    expect(component.dateNotificationProjet()).toBe('2026-04-25');

    component.editMode();
    expect(component.showForm()).toBe(true);
    expect(component.dateNotificationProjet()).toBe('2026-04-25');
  });

  it('GET 404 → reste en mode formulaire, pas de result', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Handlers d'étapes — décocher une étape efface sa date
  // ---------------------------------------------------------------------------

  it('onNotaireChange(false) reset dateDesignationNotaire', () => {
    component.notaireDesigne.set(true);
    component.dateDesignationNotaire.set('2026-01-10');
    component.onNotaireChange(false);
    expect(component.notaireDesigne()).toBe(false);
    expect(component.dateDesignationNotaire()).toBeNull();
  });

  it('onProjetLiquidationChange(false) reset dateNotificationProjet', () => {
    component.projetLiquidationEtabli.set(true);
    component.dateNotificationProjet.set('2026-04-25');
    component.onProjetLiquidationChange(false);
    expect(component.projetLiquidationEtabli()).toBe(false);
    expect(component.dateNotificationProjet()).toBeNull();
  });

  it('onHomologationChange(false) reset dateHomologation', () => {
    component.homologationDemandee.set(true);
    component.dateHomologation.set('2026-05-10');
    component.onHomologationChange(false);
    expect(component.homologationDemandee()).toBe(false);
    expect(component.dateHomologation()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Validité du formulaire
  // ---------------------------------------------------------------------------

  it('formValid faux si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai sur BE même sans aucune étape cochée (procédure non engagée)', () => {
    expect(component.formValid()).toBe(true);
  });

  it('formValid faux si une date est mal formée', () => {
    component.dateDesignationNotaire.set('pas-une-date');
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('calculate() POST → payload conforme + result + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.notaireDesigne.set(true);
    component.dateDesignationNotaire.set('2026-01-10');
    component.operationsOuvertes.set(true);
    component.dateOuvertureOperations.set('2026-01-20');
    component.inventaireEtabli.set(true);
    component.projetLiquidationEtabli.set(true);
    component.dateNotificationProjet.set('2026-04-25');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      notaireDesigne: true,
      dateDesignationNotaire: '2026-01-10',
      operationsOuvertes: true,
      dateOuvertureOperations: '2026-01-20',
      inventaireEtabli: true,
      projetLiquidationEtabli: true,
      dateNotificationProjet: '2026-04-25',
      contreditsDeposes: false,
      procesVerbalDiresEtabli: false,
      homologationDemandee: false,
      dateHomologation: null,
      commentaire: null,
    });
    req.flush(response('EN_COURS'));

    expect(component.result()!.verdict).toBe('EN_COURS');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur backend → snackbar rouge, formulaire conservé', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Date de notification requise' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
    expect(component.showForm()).toBe(true);
  });

  it('calculate() no-op si workspace FRANCE (pas d\'appel POST)', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Verdicts — 5 niveaux, rouge réservé à DELAI_CONTREDITS_CRITIQUE
  // ---------------------------------------------------------------------------

  it('verdict DELAI_CONTREDITS_CRITIQUE → banner rouge --danger', () => {
    expect(component.verdictBannerClass('DELAI_CONTREDITS_CRITIQUE')).toContain('--danger');
    expect(component.verdictBannerIcon('DELAI_CONTREDITS_CRITIQUE')).toBe('error');
  });

  it('verdict EN_ATTENTE_HOMOLOGATION → banner or --medium (pas rouge)', () => {
    expect(component.verdictBannerClass('EN_ATTENTE_HOMOLOGATION')).toContain('--medium');
    expect(component.verdictBannerClass('EN_ATTENTE_HOMOLOGATION')).not.toContain('--danger');
  });

  it('verdict EN_COURS → banner navy --available (pas rouge)', () => {
    expect(component.verdictBannerClass('EN_COURS')).toContain('--available');
    expect(component.verdictBannerClass('EN_COURS')).not.toContain('--danger');
  });

  it('verdict PROCEDURE_NON_ENGAGEE / CLOTUREE → banner navy --available', () => {
    expect(component.verdictBannerClass('PROCEDURE_NON_ENGAGEE')).toContain('--available');
    expect(component.verdictBannerClass('CLOTUREE')).toContain('--available');
  });

  it('delaiStatutClass — rouge pour CRITIQUE et DEPASSE seulement', () => {
    expect(component.delaiStatutClass('CRITIQUE')).toContain('--danger');
    expect(component.delaiStatutClass('DEPASSE')).toContain('--danger');
    expect(component.delaiStatutClass('OK')).not.toContain('--danger');
    expect(component.delaiStatutClass('NON_DEMARRE')).not.toContain('--danger');
  });

  it('GET 200 DELAI_CONTREDITS_CRITIQUE → checklist + délais rendus', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('DELAI_CONTREDITS_CRITIQUE'));
    expect(component.result()!.delais[0].statut).toBe('CRITIQUE');
    expect(component.result()!.etapes.length).toBeGreaterThan(0);
  });

  // ---------------------------------------------------------------------------
  // Bannière FRANCE
  // ---------------------------------------------------------------------------

  it('bannière info affichée si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="lpb-country-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('droit belge');
  });

  it('pas de bannière FR si workspace BELGIQUE', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="lpb-country-banner"]');
    expect(banner).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // SF-246-28 : levée PREFILL_COUNT_ALWAYS_ZERO — 4 dates possibles
  // ---------------------------------------------------------------------------

  it('getPrefillCount() retourne 0 si aiData absent', () => {
    expect(LiquidationPartageBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount() retourne 4 si les 4 dates BE ISO détectées', () => {
    expect(
      LiquidationPartageBeSectionComponent.getPrefillCount({
        aiData: {
          dateDesignationNotaireBeDetectee: '2025-03-10',
          dateOuvertureOperationsBeDetectee: '2025-04-15',
          dateNotificationProjetBeDetectee: '2025-09-01',
          dateHomologationBeDetectee: '2026-02-20',
        },
      }),
    ).toBe(4);
  });

  it('getPrefillCount() retourne 0 si les champs utilisés sont les anciens (non BE)', () => {
    // L'ancien champ dateNotificationProjet (sans suffix BE) ne compte pas
    expect(LiquidationPartageBeSectionComponent.getPrefillCount({
      aiData: { dateNotificationProjet: '2026-04-25' },
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // coherenceAlerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.DATE_NOTIFICATION_PROJET présent si F-96 diverge de la saisie', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Notification du projet',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_NOTIFICATION_PROJET', expectedValue: '2026-03-01',
    }];
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.dateNotificationProjet.set('2026-04-25');
    const alert = component.coherenceAlerts().DATE_NOTIFICATION_PROJET;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
  });

  it('coherenceAlerts vide en standaloneMode', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Notification du projet',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_NOTIFICATION_PROJET', expectedValue: '2026-03-01',
    }];
    component.standaloneMode = true;
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateNotificationProjet.set('2026-04-25');
    expect(Object.keys(component.coherenceAlerts())).toHaveLength(0);
  });

  it('coherenceAlerts masquées après calcul (showForm=false)', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Notification du projet',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_NOTIFICATION_PROJET', expectedValue: '2026-03-01',
    }];
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateNotificationProjet.set('2026-04-25');
    expect(component.coherenceAlerts().DATE_NOTIFICATION_PROJET).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DATE_NOTIFICATION_PROJET).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Toggle / editMode
  // ---------------------------------------------------------------------------

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode ré-affiche le form après calcul', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });
});
