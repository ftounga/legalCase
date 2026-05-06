import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { CaseDashboardComponent } from './case-dashboard.component';
import { CaseDashboardService } from '../../core/services/case-dashboard.service';
import { CaseDashboardRefreshService } from './case-dashboard-refresh.service';
import { DashboardResponse, DashboardTile as BackendDashboardTile } from '../../core/models/case-dashboard.model';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { DecisionToolModalService } from '../decisional-tools-panel/decision-tool-modal/decision-tool-modal.service';

/**
 * F-167 SF-167-05 — Tests Jest du tableau de bord décisionnel après fusion
 * des records typés legacy. Le composant ne consomme plus que `tiles[]` +
 * `riskScore`/`riskLevel`. Couverture :
 *   - refresh integration (SF-IA-02-03 conservé)
 *   - groupement par thème + tri par alertLevel (SF-167-05 T-01..T-04)
 *   - état vide (SF-167-05 T-05)
 *   - openGenericTool resolution + warn unknown id (SF-167-01 conservé)
 */
describe('CaseDashboardComponent — refresh integration (SF-IA-02-03)', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let refreshService: CaseDashboardRefreshService;

  const emptyDashboard: DashboardResponse = {
    caseFileId: 'case-1',
    legalDomain: 'TRAVAIL',
    riskScore: null,
    riskLevel: null,
    tiles: [],
  };

  beforeEach(async () => {
    dashboardService = { get: jest.fn().mockReturnValue(of(emptyDashboard)) } as any;
    refreshService = new CaseDashboardRefreshService();

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: CaseDashboardRefreshService, useValue: refreshService },
        { provide: Router, useValue: { navigate: jest.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
  });

  it('loads dashboard once on init', () => {
    fixture.detectChanges();
    expect(dashboardService.get).toHaveBeenCalledTimes(1);
  });

  it('reloads dashboard when refresh service emits (debounced)', fakeAsync(() => {
    fixture.detectChanges();
    refreshService.triggerRefresh();
    tick(300);
    expect(dashboardService.get).toHaveBeenCalledTimes(2);
  }));

  it('coalesces bursts of triggerRefresh() via debounce', fakeAsync(() => {
    fixture.detectChanges();
    refreshService.triggerRefresh();
    refreshService.triggerRefresh();
    refreshService.triggerRefresh();
    tick(300);
    expect(dashboardService.get).toHaveBeenCalledTimes(2);
  }));

  it('does not reload before debounce window elapses', fakeAsync(() => {
    fixture.detectChanges();
    refreshService.triggerRefresh();
    tick(100);
    expect(dashboardService.get).toHaveBeenCalledTimes(1);
    tick(250);
    expect(dashboardService.get).toHaveBeenCalledTimes(2);
  }));
});

describe('CaseDashboardComponent — SF-167-05 grouping + sort + empty state', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let modalService: jest.Mocked<DecisionToolModalService>;

  /**
   * Dashboard avec tiles couvrant 4 thèmes + un mix de alertLevels pour
   * vérifier groupement et tri.
   */
  const dashboardWithTiles: DashboardResponse = {
    caseFileId: 'case-1',
    legalDomain: 'TRAVAIL',
    riskScore: 45,
    riskLevel: 'MOYEN',
    tiles: [
      // INDEMNITES — 2 tiles, dans cet ordre : OK puis ALERT (le tri doit
      // remonter ALERT en premier).
      {
        toolId: 'F-DT-07-anciennete-conges-prime',
        theme: 'INDEMNITES',
        label: 'Ancienneté & congés',
        primaryValue: '4 an(s) 6 mois',
        secondaryValue: '25 jours congés',
        alertLevel: 'OK',
      },
      {
        toolId: 'F-DT-09-comparateur-indemnites',
        theme: 'INDEMNITES',
        label: 'Indemnités',
        primaryValue: '2 000 – 5 000 €',
        secondaryValue: 'Macron',
        alertLevel: 'ALERT',
      },
      // VALIDITE — 1 tile WARNING.
      {
        toolId: 'F-DT-08-licenciement-validity',
        theme: 'VALIDITE',
        label: 'Validité licenciement',
        primaryValue: 'INVALIDE',
        secondaryValue: '3/8 critères non conformes',
        alertLevel: 'WARNING',
      },
      // DELAIS — 1 tile sans alertLevel.
      {
        toolId: 'F-IM-06-recours',
        theme: 'DELAIS',
        label: 'Recours',
        primaryValue: 'OQTF',
        secondaryValue: 'Limite : 2026-06-15',
        alertLevel: null,
      },
      // DIAGNOSTIC — 1 tile OK.
      {
        toolId: 'F-IM-11-changement-statut',
        theme: 'DIAGNOSTIC',
        label: 'Changement de statut',
        primaryValue: 'ETUDIANT → VPF',
        secondaryValue: '8 mois restants',
        alertLevel: 'OK',
      },
    ],
  };

  beforeEach(async () => {
    dashboardService = { get: jest.fn().mockReturnValue(of(dashboardWithTiles)) } as any;
    modalService = { open: jest.fn().mockReturnValue({ close: jest.fn() }) } as any;

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: DecisionToolModalService, useValue: modalService },
        { provide: Router, useValue: { navigate: jest.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  it('SF-167-05 T-01 : groups tiles by theme in canonical order, skipping empty themes', () => {
    fixture.detectChanges();
    const sections = component.themeSections();
    // Aucun outil DOCUMENTS dans le mock → la section est exclue. Reste 4
    // sections dans l'ordre canonique.
    expect(sections.map((s) => s.key)).toEqual([
      'INDEMNITES',
      'VALIDITE',
      'DELAIS',
      'DIAGNOSTIC',
    ]);
    expect(sections.find((s) => s.key === 'INDEMNITES')?.label)
      .toBe('Indemnités & calculs');
  });

  it('SF-167-05 T-02 : sorts tiles inside a theme by alertLevel (ALERT > WARNING > OK > null)', () => {
    fixture.detectChanges();
    const sections = component.themeSections();
    const indemnites = sections.find((s) => s.key === 'INDEMNITES')!;
    // ALERT remonte avant OK, même si la tile OK est listée en premier dans
    // le payload backend.
    expect(indemnites.tiles.map((t) => t.toolId)).toEqual([
      'F-DT-09-comparateur-indemnites',
      'F-DT-07-anciennete-conges-prime',
    ]);
  });

  it('SF-167-05 T-03 : exposes riskScore tile separately (not inside theme sections)', () => {
    fixture.detectChanges();
    const risk = component.riskScoreTile();
    expect(risk).not.toBeNull();
    expect(risk!.toolId).toBe('risk-score');
    expect(risk!.summary.primaryValue).toBe('45 %');
    expect(risk!.metierAlertLevel).toBe('WARNING');
    // Le riskScore n'apparaît dans aucune section thématique.
    const allSectionTiles = component.themeSections().flatMap((s) => s.tiles);
    expect(allSectionTiles.map((t) => t.toolId)).not.toContain('risk-score');
  });

  it('SF-167-05 T-04 : isEmpty=false when tiles or riskScore present', () => {
    fixture.detectChanges();
    expect(component.isEmpty()).toBe(false);
  });

  it('SF-167-05 T-05 : isEmpty=true + empty-state rendered when no riskScore and no tiles', () => {
    dashboardService.get.mockReturnValue(of({
      caseFileId: 'case-1',
      legalDomain: 'TRAVAIL',
      riskScore: null,
      riskLevel: null,
      tiles: [],
    }));
    fixture.detectChanges();
    expect(component.isEmpty()).toBe(true);
    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Aucun outil exécuté pour ce dossier');
  });

  it('SF-167-05 T-06 : tiles undefined treated as empty list', () => {
    dashboardService.get.mockReturnValue(of({
      caseFileId: 'case-1',
      legalDomain: 'TRAVAIL',
      riskScore: null,
      riskLevel: null,
    } as DashboardResponse));
    fixture.detectChanges();
    expect(component.themeSections()).toEqual([]);
    expect(component.isEmpty()).toBe(true);
  });
});

describe('CaseDashboardComponent — SF-184-01 verdictsCount', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let modalService: jest.Mocked<DecisionToolModalService>;

  const buildTile = (toolId: string): BackendDashboardTile => ({
    toolId,
    theme: 'INDEMNITES',
    label: toolId,
    primaryValue: 'x',
    secondaryValue: null,
    alertLevel: 'OK',
  });

  beforeEach(async () => {
    dashboardService = { get: jest.fn() } as any;
    modalService = { open: jest.fn().mockReturnValue({ close: jest.fn() }) } as any;

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: DecisionToolModalService, useValue: modalService },
        { provide: Router, useValue: { navigate: jest.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  it('SF-184-01 T-01 : verdictsCount = 1 quand riskScore seul (pas de tiles)', () => {
    dashboardService.get.mockReturnValue(of({
      caseFileId: 'case-1',
      legalDomain: 'TRAVAIL',
      riskScore: 75,
      riskLevel: 'ALERT',
      tiles: [],
    }));
    fixture.detectChanges();
    expect(component.verdictsCount()).toBe(1);
  });

  it('SF-184-01 T-02 : verdictsCount = riskScore (1) + tiles (5) = 6', () => {
    dashboardService.get.mockReturnValue(of({
      caseFileId: 'case-1',
      legalDomain: 'TRAVAIL',
      riskScore: 50,
      riskLevel: 'MOYEN',
      tiles: [
        buildTile('t1'),
        buildTile('t2'),
        buildTile('t3'),
        buildTile('t4'),
        buildTile('t5'),
      ],
    }));
    fixture.detectChanges();
    expect(component.verdictsCount()).toBe(6);
  });

  it('SF-184-01 T-03 : verdictsCount = 0 quand pas de riskScore et 0 tiles', () => {
    dashboardService.get.mockReturnValue(of({
      caseFileId: 'case-1',
      legalDomain: 'TRAVAIL',
      riskScore: null,
      riskLevel: null,
      tiles: [],
    }));
    fixture.detectChanges();
    expect(component.verdictsCount()).toBe(0);
  });
});

describe('CaseDashboardComponent — openGenericTool (SF-167-01 / SF-167-05)', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let modalService: jest.Mocked<DecisionToolModalService>;

  const dashboardOneTile: DashboardResponse = {
    caseFileId: 'case-1',
    legalDomain: 'IMMIGRATION',
    riskScore: null,
    riskLevel: null,
    tiles: [
      {
        toolId: 'F-IM-11-changement-statut',
        theme: 'VALIDITE',
        label: 'Changement de statut',
        primaryValue: 'ETUDIANT → VPF',
        secondaryValue: '8 mois restants',
        alertLevel: 'OK',
      },
    ],
  };

  beforeEach(async () => {
    dashboardService = { get: jest.fn().mockReturnValue(of(dashboardOneTile)) } as any;
    modalService = { open: jest.fn().mockReturnValue({ close: jest.fn() }) } as any;

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: DecisionToolModalService, useValue: modalService },
        { provide: Router, useValue: { navigate: jest.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  it('SF-167-01 T-03 : openGenericTool ouvre le modal du composant résolu', () => {
    fixture.detectChanges();
    component.openGenericTool('F-IM-11-changement-statut');
    expect(modalService.open).toHaveBeenCalledTimes(1);
    const args = modalService.open.mock.calls[0][0];
    expect(args.toolId).toBe('F-IM-11-changement-statut');
    expect(args.component).toBeDefined();
    expect(args.inputs['forceExpanded']).toBe(true);
    expect(args.inputs['caseFileId']).toBe('case-1');
  });

  it('SF-167-01 T-04 : openGenericTool no-op + warn sur toolId inconnu', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    fixture.detectChanges();
    component.openGenericTool('unknown-tool-id');
    expect(modalService.open).not.toHaveBeenCalled();
    expect(warnSpy).toHaveBeenCalled();
    warnSpy.mockRestore();
  });
});

// F-192 SF-192-02 — tile RETAINED_PISTES_SUMMARY rendue + clic navigation
// vers /synthesis#section-pistes (pas d'ouverture modal d'outil).
describe('CaseDashboardComponent — F-192 SF-192-02 RETAINED_PISTES_SUMMARY tile', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let modalService: jest.Mocked<DecisionToolModalService>;
  let router: jest.Mocked<Pick<Router, 'navigate'>>;

  const dashboardWithRetained: DashboardResponse = {
    caseFileId: 'case-1',
    legalDomain: 'IMMIGRATION',
    riskScore: null,
    riskLevel: null,
    tiles: [
      {
        toolId: 'RETAINED_PISTES_SUMMARY',
        theme: 'DIAGNOSTIC',
        label: 'Stratégies retenues',
        primaryValue: '3 retenues',
        secondaryValue: '1 en divergence',
        alertLevel: 'WARNING',
      },
    ],
  };

  beforeEach(async () => {
    dashboardService = { get: jest.fn().mockReturnValue(of(dashboardWithRetained)) } as any;
    modalService = { open: jest.fn().mockReturnValue({ close: jest.fn() }) } as any;
    router = { navigate: jest.fn() } as any;

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: DecisionToolModalService, useValue: modalService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  it('CA-08 tile RETAINED_PISTES_SUMMARY rendue dans le thème DIAGNOSTIC', () => {
    fixture.detectChanges();
    const sections = component.themeSections();
    const diag = sections.find(s => s.key === 'DIAGNOSTIC');
    expect(diag).toBeDefined();
    expect(diag!.tiles[0].toolId).toBe('RETAINED_PISTES_SUMMARY');
    expect(diag!.tiles[0].label).toBe('Stratégies retenues');
    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Stratégies retenues');
  });

  it('CA-09 clic tile RETAINED_PISTES_SUMMARY → router.navigate vers /synthesis#section-pistes', () => {
    fixture.detectChanges();
    component.openGenericTool('RETAINED_PISTES_SUMMARY');
    expect(router.navigate).toHaveBeenCalledWith(
      ['/case-files', 'case-1', 'synthesis'],
      { fragment: 'section-pistes' },
    );
    expect(modalService.open).not.toHaveBeenCalled();
  });
});
