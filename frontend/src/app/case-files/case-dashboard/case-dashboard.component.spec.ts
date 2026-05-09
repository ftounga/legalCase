import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { CaseDashboardComponent } from './case-dashboard.component';
import { CaseDashboardService } from '../../core/services/case-dashboard.service';
import { CaseDashboardRefreshService } from './case-dashboard-refresh.service';
import { DashboardResponse, DashboardTile as BackendDashboardTile } from '../../core/models/case-dashboard.model';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { DecisionToolModalService } from '../decisional-tools-panel/decision-tool-modal/decision-tool-modal.service';
import {
  DecisionToolAlignmentsLoader,
  DecisionToolAlignments,
} from '../decision-tools-shared/decision-tool-alignments.loader';
import { RetainedPisteAlignment } from '../../core/models/retained-piste-alignment.model';
import { PieceManquanteAlignment } from '../../core/models/piece-manquante-alignment.model';
import { RisqueAlignment } from '../../core/models/risque-alignment.model';
import { AiQuestionAlignment } from '../../core/models/ai-question-alignment.model';
import { BadgeNavigationService } from '../synthesis-badges/badge-navigation.service';

/**
 * F-229 SF-229-01 — stub réutilisé par tous les describe pour intercepter les
 * appels `BadgeNavigationService.go(key, caseFileId, contextData?)` lancés
 * depuis `openGenericTool` (tiles "résumé").
 */
function makeBadgeNavigationStub(): { go: jest.Mock } {
  return { go: jest.fn() };
}

/**
 * F-228 SF-228-01 — bundle vide par défaut pour les tests qui ne s'occupent
 * pas explicitement des alignements. Évite la dépendance HttpClient qui n'est
 * pas provided dans le TestBed du dashboard.
 */
const EMPTY_ALIGNMENTS: DecisionToolAlignments = {
  retainedPistes: [],
  piecesAlignment: [],
  risquesAlignment: [],
  aiQuestionsAlignment: [],
};

function makeAlignmentsLoaderStub(
  bundle: DecisionToolAlignments = EMPTY_ALIGNMENTS,
): { loadAll: jest.Mock } {
  return { loadAll: jest.fn().mockReturnValue(of(bundle)) };
}

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
        { provide: DecisionToolAlignmentsLoader, useValue: makeAlignmentsLoaderStub() },
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
        { provide: DecisionToolAlignmentsLoader, useValue: makeAlignmentsLoaderStub() },
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
        { provide: DecisionToolAlignmentsLoader, useValue: makeAlignmentsLoaderStub() },
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
        { provide: DecisionToolAlignmentsLoader, useValue: makeAlignmentsLoaderStub() },
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

// F-192 SF-192-02 — tile F-192-retained-pistes-summary rendue + clic navigation
// vers /synthesis#section-pistes (pas d'ouverture modal d'outil).
describe('CaseDashboardComponent — F-192 SF-192-02 F-192-retained-pistes-summary tile', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let modalService: jest.Mocked<DecisionToolModalService>;
  let badgeNavigation: { go: jest.Mock };

  const dashboardWithRetained: DashboardResponse = {
    caseFileId: 'case-1',
    legalDomain: 'IMMIGRATION',
    riskScore: null,
    riskLevel: null,
    tiles: [
      {
        toolId: 'F-192-retained-pistes-summary',
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
    badgeNavigation = makeBadgeNavigationStub();

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: DecisionToolAlignmentsLoader, useValue: makeAlignmentsLoaderStub() },
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: DecisionToolModalService, useValue: modalService },
        { provide: Router, useValue: { navigate: jest.fn() } },
        { provide: BadgeNavigationService, useValue: badgeNavigation },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  it('CA-08 tile F-192-retained-pistes-summary rendue dans le thème DIAGNOSTIC', () => {
    fixture.detectChanges();
    const sections = component.themeSections();
    const diag = sections.find(s => s.key === 'DIAGNOSTIC');
    expect(diag).toBeDefined();
    expect(diag!.tiles[0].toolId).toBe('F-192-retained-pistes-summary');
    expect(diag!.tiles[0].label).toBe('Stratégies retenues');
    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Stratégies retenues');
  });

  it('CA-06 (F-229) clic tile F-192-retained-pistes-summary → BadgeNavigationService.go("pistes", caseFileId)', () => {
    fixture.detectChanges();
    component.openGenericTool('F-192-retained-pistes-summary');
    expect(badgeNavigation.go).toHaveBeenCalledWith('pistes', 'case-1');
    expect(modalService.open).not.toHaveBeenCalled();
  });
});

// F-229 SF-229-03 (2026-05-09) — tile F-193-procedure-checks-summary rendue +
// clic navigation vers /synthesis#section-checklist (pas d'ouverture modal).
// Symétrique des 4 autres tiles résumé F-192/F-194/F-195/F-196.
describe('CaseDashboardComponent — F-229 SF-229-03 F-193-procedure-checks-summary tile', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let modalService: jest.Mocked<DecisionToolModalService>;
  let badgeNavigation: { go: jest.Mock };

  const dashboardWithProcedureChecks: DashboardResponse = {
    caseFileId: 'case-1',
    legalDomain: 'DROIT_DU_TRAVAIL',
    riskScore: null,
    riskLevel: null,
    tiles: [
      {
        toolId: 'F-193-procedure-checks-summary',
        theme: 'DOCUMENTS',
        label: 'Procédure',
        primaryValue: '4 vérifications',
        secondaryValue: '2 OK · 1 en attente · 1 KO',
        alertLevel: 'WARNING',
      },
    ],
  };

  beforeEach(async () => {
    dashboardService = { get: jest.fn().mockReturnValue(of(dashboardWithProcedureChecks)) } as any;
    modalService = { open: jest.fn().mockReturnValue({ close: jest.fn() }) } as any;
    badgeNavigation = makeBadgeNavigationStub();

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: DecisionToolAlignmentsLoader, useValue: makeAlignmentsLoaderStub() },
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: DecisionToolModalService, useValue: modalService },
        { provide: Router, useValue: { navigate: jest.fn() } },
        { provide: BadgeNavigationService, useValue: badgeNavigation },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  it('CA-10 (SF-229-03) clic tile F-193-procedure-checks-summary → BadgeNavigationService.go("checklist", caseFileId)', () => {
    fixture.detectChanges();
    component.openGenericTool('F-193-procedure-checks-summary');
    expect(badgeNavigation.go).toHaveBeenCalledWith('checklist', 'case-1');
    expect(modalService.open).not.toHaveBeenCalled();
  });
});

// F-194 SF-194-02 — tile F-194-pieces-summary rendue + clic navigation
// vers /synthesis#section-pieces (pas d'ouverture modal d'outil).
describe('CaseDashboardComponent — F-194 SF-194-02 pieces-summary tile', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let modalService: jest.Mocked<DecisionToolModalService>;
  let badgeNavigation: { go: jest.Mock };

  const dashboardWithPieces: DashboardResponse = {
    caseFileId: 'case-1',
    legalDomain: 'DROIT_DU_TRAVAIL',
    riskScore: null,
    riskLevel: null,
    tiles: [
      {
        toolId: 'F-194-pieces-summary',
        theme: 'DOCUMENTS',
        label: 'Pièces',
        primaryValue: '3 à demander',
        secondaryValue: '2 obtenues',
        alertLevel: 'WARNING',
      },
    ],
  };

  beforeEach(async () => {
    dashboardService = { get: jest.fn().mockReturnValue(of(dashboardWithPieces)) } as any;
    modalService = { open: jest.fn().mockReturnValue({ close: jest.fn() }) } as any;
    badgeNavigation = makeBadgeNavigationStub();

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: DecisionToolAlignmentsLoader, useValue: makeAlignmentsLoaderStub() },
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: DecisionToolModalService, useValue: modalService },
        { provide: Router, useValue: { navigate: jest.fn() } },
        { provide: BadgeNavigationService, useValue: badgeNavigation },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  it('CA-08 tile F-194-pieces-summary rendue dans le thème DOCUMENTS', () => {
    fixture.detectChanges();
    const sections = component.themeSections();
    const docs = sections.find(s => s.key === 'DOCUMENTS');
    expect(docs).toBeDefined();
    expect(docs!.tiles[0].toolId).toBe('F-194-pieces-summary');
    expect(docs!.tiles[0].label).toBe('Pièces');
    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Pièces');
  });

  it('CA-06 (F-229) clic tile F-194-pieces-summary → BadgeNavigationService.go("pieces", caseFileId, ctx)', () => {
    component.synthesis = { piecesManquantes: ['Contrat', 'Bulletin'] };
    fixture.detectChanges();
    component.openGenericTool('F-194-pieces-summary');
    expect(badgeNavigation.go).toHaveBeenCalledWith('pieces', 'case-1', {
      popupItems: ['Contrat', 'Bulletin'],
      popupTitle: 'Pièces manquantes',
      popupIcon: 'report_problem',
    });
    expect(modalService.open).not.toHaveBeenCalled();
  });

  it('CA-06 (F-229) clic tile F-194-pieces-summary sans synthesis → popupItems=[] (fail-open)', () => {
    component.synthesis = null;
    fixture.detectChanges();
    component.openGenericTool('F-194-pieces-summary');
    expect(badgeNavigation.go).toHaveBeenCalledWith('pieces', 'case-1', {
      popupItems: [],
      popupTitle: 'Pièces manquantes',
      popupIcon: 'report_problem',
    });
  });
});

// F-195 SF-195-02 — tile F-195-risques-summary rendue + clic navigation
// vers /synthesis#section-risques (pas d'ouverture modal d'outil).
describe('CaseDashboardComponent — F-195 SF-195-02 risques-summary tile', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let modalService: jest.Mocked<DecisionToolModalService>;
  let badgeNavigation: { go: jest.Mock };

  const dashboardWithRisques: DashboardResponse = {
    caseFileId: 'case-1',
    legalDomain: 'DROIT_DU_TRAVAIL',
    riskScore: null,
    riskLevel: null,
    tiles: [
      {
        toolId: 'F-195-risques-summary',
        theme: 'DIAGNOSTIC',
        label: 'Risques',
        primaryValue: '3 risques',
        secondaryValue: '1 validé · 1 écarté · 1 à creuser',
        alertLevel: 'ALERT',
      },
    ],
  };

  beforeEach(async () => {
    dashboardService = { get: jest.fn().mockReturnValue(of(dashboardWithRisques)) } as any;
    modalService = { open: jest.fn().mockReturnValue({ close: jest.fn() }) } as any;
    badgeNavigation = makeBadgeNavigationStub();

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: DecisionToolAlignmentsLoader, useValue: makeAlignmentsLoaderStub() },
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: DecisionToolModalService, useValue: modalService },
        { provide: Router, useValue: { navigate: jest.fn() } },
        { provide: BadgeNavigationService, useValue: badgeNavigation },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  it('CA-05 tile F-195-risques-summary rendue dans le thème DIAGNOSTIC', () => {
    fixture.detectChanges();
    const sections = component.themeSections();
    const diag = sections.find(s => s.key === 'DIAGNOSTIC');
    expect(diag).toBeDefined();
    expect(diag!.tiles[0].toolId).toBe('F-195-risques-summary');
    expect(diag!.tiles[0].label).toBe('Risques');
    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Risques');
  });

  it('CA-06 (F-229) clic tile F-195-risques-summary → BadgeNavigationService.go("risques", caseFileId)', () => {
    fixture.detectChanges();
    component.openGenericTool('F-195-risques-summary');
    expect(badgeNavigation.go).toHaveBeenCalledWith('risques', 'case-1');
    expect(modalService.open).not.toHaveBeenCalled();
  });
});

// F-196 SF-196-02 — tile F-196-questions-summary rendue + clic navigation
// vers /synthesis#section-questions (pas d'ouverture modal d'outil).
describe('CaseDashboardComponent — F-196 SF-196-02 questions-summary tile', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let modalService: jest.Mocked<DecisionToolModalService>;
  let badgeNavigation: { go: jest.Mock };

  const dashboardWithQuestions: DashboardResponse = {
    caseFileId: 'case-1',
    legalDomain: 'DROIT_DU_TRAVAIL',
    riskScore: null,
    riskLevel: null,
    tiles: [
      {
        toolId: 'F-196-questions-summary',
        theme: 'DOCUMENTS',
        label: 'Questions complémentaires',
        primaryValue: '5 questions',
        secondaryValue: '3 répondues · 2 en attente',
        alertLevel: 'WARNING',
      },
    ],
  };

  beforeEach(async () => {
    dashboardService = { get: jest.fn().mockReturnValue(of(dashboardWithQuestions)) } as any;
    modalService = { open: jest.fn().mockReturnValue({ close: jest.fn() }) } as any;
    badgeNavigation = makeBadgeNavigationStub();

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: DecisionToolAlignmentsLoader, useValue: makeAlignmentsLoaderStub() },
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: DecisionToolModalService, useValue: modalService },
        { provide: Router, useValue: { navigate: jest.fn() } },
        { provide: BadgeNavigationService, useValue: badgeNavigation },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  it('CA-01 tile F-196-questions-summary rendue dans le thème DOCUMENTS', () => {
    fixture.detectChanges();
    const sections = component.themeSections();
    const docs = sections.find(s => s.key === 'DOCUMENTS');
    expect(docs).toBeDefined();
    expect(docs!.tiles[0].toolId).toBe('F-196-questions-summary');
    expect(docs!.tiles[0].label).toBe('Questions complémentaires');
    expect(docs!.tiles[0].primaryValue).toBe('5 questions');
    expect(docs!.tiles[0].secondaryValue).toBe('3 répondues · 2 en attente');
    expect(docs!.tiles[0].alertLevel).toBe('WARNING');
    const html: string = fixture.nativeElement.innerHTML;
    expect(html).toContain('Questions complémentaires');
  });

  it('CA-06 (F-229) clic tile F-196-questions-summary → BadgeNavigationService.go("questions", caseFileId)', () => {
    fixture.detectChanges();
    component.openGenericTool('F-196-questions-summary');
    expect(badgeNavigation.go).toHaveBeenCalledWith('questions', 'case-1');
    expect(modalService.open).not.toHaveBeenCalled();
  });
});

// F-195 SF-195-02 — tile riskScore étendue avec scoreRisqueAvocat
describe('CaseDashboardComponent — F-195 SF-195-02 dual riskScore', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;

  beforeEach(async () => {
    dashboardService = { get: jest.fn() } as any;
    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: DecisionToolAlignmentsLoader, useValue: makeAlignmentsLoaderStub() },
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: DecisionToolModalService, useValue: { open: jest.fn() } },
        { provide: Router, useValue: { navigate: jest.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
  });

  it('CA-06 dual score affiché quand scoreRisqueAvocat présent (score IA brut + validé avocat)', () => {
    dashboardService.get.mockReturnValue(of({
      caseFileId: 'case-1',
      legalDomain: 'TRAVAIL',
      riskScore: 75,
      riskLevel: 'ELEVE',
      scoreRisqueAvocat: 35,
      tiles: [],
    }));
    fixture.detectChanges();
    const risk = component.riskScoreTile();
    expect(risk).not.toBeNull();
    expect(risk!.summary.primaryValue).toContain('IA brut : 75 %');
    expect(risk!.summary.primaryValue).toContain('Validé avocat : 35 %');
    // La couleur suit le score validé avocat (35 < 60 → WARNING).
    expect(risk!.metierAlertLevel).toBe('WARNING');
  });

  it('CA-06 fallback comportement original quand scoreRisqueAvocat absent (1 score)', () => {
    dashboardService.get.mockReturnValue(of({
      caseFileId: 'case-1',
      legalDomain: 'TRAVAIL',
      riskScore: 75,
      riskLevel: 'ELEVE',
      tiles: [],
    }));
    fixture.detectChanges();
    const risk = component.riskScoreTile();
    expect(risk).not.toBeNull();
    expect(risk!.summary.primaryValue).toBe('75 %');
    expect(risk!.metierAlertLevel).toBe('ALERT');
  });

  it('CA-06 scoreRisqueAvocat null → fallback comportement original', () => {
    dashboardService.get.mockReturnValue(of({
      caseFileId: 'case-1',
      legalDomain: 'TRAVAIL',
      riskScore: 50,
      riskLevel: 'MOYEN',
      scoreRisqueAvocat: null,
      tiles: [],
    }));
    fixture.detectChanges();
    const risk = component.riskScoreTile();
    expect(risk!.summary.primaryValue).toBe('50 %');
  });
});

// F-228 SF-228-01 — ctx d'ouverture des outils enrichi avec les 4 alignements
// IA (pistes retenues, pièces obtenues, risques validés, questions). Symétrie
// stricte avec le panel F-IA-04 — bug staging Immigration Chen 17 du
// 2026-05-08 : popup `<app-immigration-title-decision-section>` perdait ses
// pistes stratégiques entre le 1er et le 2nd clic.
describe('CaseDashboardComponent — F-228 SF-228-01 ctx alignements', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let modalService: jest.Mocked<DecisionToolModalService>;

  const dashboardWithImmigrationTile: DashboardResponse = {
    caseFileId: 'case-1',
    legalDomain: 'IMMIGRATION',
    riskScore: null,
    riskLevel: null,
    tiles: [
      {
        toolId: 'F-IM-05-arbre-decisionnel-titre',
        theme: 'DIAGNOSTIC',
        label: 'Titre de séjour recommandé',
        primaryValue: 'VPF',
        secondaryValue: null,
        alertLevel: 'OK',
      },
    ],
  };

  // Bundle représentatif : 1 piste pertinente pour l'outil cible + 1 piste
  // pour un autre outil (ne doit PAS être propagée par le filtre toolIdCible).
  const piste1: RetainedPisteAlignment = {
    pisteId: 'p-1',
    texte: 'Demander un titre VPF',
    conditions: [],
    matchStatus: 'ALIGNED',
    toolIdCible: 'F-IM-05-arbre-decisionnel-titre',
  };
  const pisteOtherTool: RetainedPisteAlignment = {
    pisteId: 'p-2',
    texte: 'Vie privée et familiale',
    conditions: [],
    matchStatus: 'ALIGNED',
    toolIdCible: 'F-IM-06-recours',
  };
  const pieceObtenue: PieceManquanteAlignment = {
    pieceLibelle: 'Justificatif de domicile',
    statut: 'OBTENUE',
    toolIdsCibles: ['F-IM-05-arbre-decisionnel-titre'],
  };
  const pieceManquanteOther: PieceManquanteAlignment = {
    pieceLibelle: 'Acte de naissance',
    statut: 'A_DEMANDER',
    toolIdsCibles: ['F-IM-05-arbre-decisionnel-titre'],
  };
  const risqueOqtf: RisqueAlignment = {
    risqueLibelle: 'Risque OQTF',
    statut: 'VALIDE',
    toolIdsCibles: ['F-IM-08-oqtf-avec-delai-fr'],
  };
  const aiQuestion: AiQuestionAlignment = {
    questionId: 'q-1',
    answerText: 'Marié à une Française',
    statutDeduction: 'PIECE_OBTENUE',
    pieceLibelleDeduit: 'Acte de mariage',
  };

  const fullBundle: DecisionToolAlignments = {
    retainedPistes: [piste1, pisteOtherTool],
    piecesAlignment: [pieceObtenue, pieceManquanteOther],
    risquesAlignment: [risqueOqtf],
    aiQuestionsAlignment: [aiQuestion],
  };

  let loaderStub: { loadAll: jest.Mock };
  let badgeNavigation: { go: jest.Mock };

  beforeEach(async () => {
    dashboardService = {
      get: jest.fn().mockReturnValue(of(dashboardWithImmigrationTile)),
    } as any;
    modalService = { open: jest.fn().mockReturnValue({ close: jest.fn() }) } as any;
    loaderStub = makeAlignmentsLoaderStub(fullBundle);
    badgeNavigation = makeBadgeNavigationStub();

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: DecisionToolAlignmentsLoader, useValue: loaderStub },
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: DecisionToolModalService, useValue: modalService },
        { provide: Router, useValue: { navigate: jest.fn() } },
        { provide: BadgeNavigationService, useValue: badgeNavigation },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    component.synthesis = {
      immigrationExtractedData: { nationalite: 'CN' },
    };
  });

  it('CA-07 charge les 4 alignements au mount via DecisionToolAlignmentsLoader', () => {
    fixture.detectChanges();
    expect(loaderStub.loadAll).toHaveBeenCalledWith('case-1');
    expect(component.retainedPistes()).toEqual([piste1, pisteOtherTool]);
    expect(component.piecesAlignment()).toEqual([pieceObtenue, pieceManquanteOther]);
    expect(component.risquesAlignment()).toEqual([risqueOqtf]);
    expect(component.aiQuestionsAlignment()).toEqual([aiQuestion]);
  });

  it('CA-07-A openGenericTool propage pistesRetenues filtrées par toolIdCible', () => {
    fixture.detectChanges();
    component.openGenericTool('F-IM-05-arbre-decisionnel-titre');
    expect(modalService.open).toHaveBeenCalledTimes(1);
    const args = modalService.open.mock.calls[0][0];
    // Filtre toolIdCible : seule piste1 doit remonter, pisteOtherTool est exclue.
    expect(args.inputs['pistesRetenues']).toEqual([piste1]);
  });

  it('CA-07-B openGenericTool propage piecesObtenues filtrées (statut OBTENUE + toolId)', () => {
    fixture.detectChanges();
    component.openGenericTool('F-IM-05-arbre-decisionnel-titre');
    const args = modalService.open.mock.calls[0][0];
    // Filtre statut=OBTENUE + toolId : seul pieceObtenue (statut OBTENUE) ;
    // pieceManquanteOther (A_DEMANDER) ne remonte pas.
    expect(args.inputs['piecesObtenues']).toEqual(['Justificatif de domicile']);
  });

  it('CA-07-C openGenericTool propage aiData (immigrationExtractedData) depuis synthesis', () => {
    fixture.detectChanges();
    component.openGenericTool('F-IM-05-arbre-decisionnel-titre');
    const args = modalService.open.mock.calls[0][0];
    expect(args.inputs['aiData']).toEqual({ nationalite: 'CN' });
  });

  it('CA-07-D openGenericTool propage aiQuestions (input direct) + procedureChecks', () => {
    component.aiQuestions = [{ id: 'q-1', text: 'Date arrivée ?' }];
    component.procedureChecks = [{ code: 'CHECK_X', status: 'OK' }];
    fixture.detectChanges();
    component.openGenericTool('F-IM-05-arbre-decisionnel-titre');
    const args = modalService.open.mock.calls[0][0];
    expect(args.inputs['aiQuestions']).toEqual([{ id: 'q-1', text: 'Date arrivée ?' }]);
    expect(args.inputs['procedureChecks']).toEqual([{ code: 'CHECK_X', status: 'OK' }]);
  });

  it('CA-08 close + ré-ouverture passe les mêmes inputs (signals d\'alignement non resetés)', () => {
    fixture.detectChanges();
    component.openGenericTool('F-IM-05-arbre-decisionnel-titre');
    const firstCall = modalService.open.mock.calls[0][0];

    // Simule fermeture + ré-ouverture (le composant est toujours monté ; les
    // signals d'alignement ne sont pas resetés entre les ouvertures).
    component.openGenericTool('F-IM-05-arbre-decisionnel-titre');
    const secondCall = modalService.open.mock.calls[1][0];

    expect(secondCall.inputs['pistesRetenues']).toEqual(firstCall.inputs['pistesRetenues']);
    expect(secondCall.inputs['piecesObtenues']).toEqual(firstCall.inputs['piecesObtenues']);
    expect(secondCall.inputs['aiData']).toEqual(firstCall.inputs['aiData']);
    // Pas de re-fetch loader entre les ouvertures (loader appelé 1 fois au mount).
    expect(loaderStub.loadAll).toHaveBeenCalledTimes(1);
  });

  it('CA-09 (F-229) tile résumé F-192-retained-pistes-summary délègue à BadgeNavigationService.go("pistes")', () => {
    const badge = TestBed.inject(BadgeNavigationService) as unknown as { go: jest.Mock };
    fixture.detectChanges();
    component.openGenericTool('F-192-retained-pistes-summary');
    expect(badge.go).toHaveBeenCalledWith('pistes', 'case-1');
    expect(modalService.open).not.toHaveBeenCalled();
  });

  it('CA-09 (F-229) tile résumé F-194-pieces-summary délègue à BadgeNavigationService.go("pieces", ctx)', () => {
    const badge = TestBed.inject(BadgeNavigationService) as unknown as { go: jest.Mock };
    // synthesis fixé dans le beforeEach (immigrationExtractedData) — n'a pas
    // de piecesManquantes, donc on attend [] dans contextData.
    fixture.detectChanges();
    component.openGenericTool('F-194-pieces-summary');
    expect(badge.go).toHaveBeenCalledWith('pieces', 'case-1', {
      popupItems: [],
      popupTitle: 'Pièces manquantes',
      popupIcon: 'report_problem',
    });
    expect(modalService.open).not.toHaveBeenCalled();
  });
});

// F-228 SF-228-01 — re-fetch des alignements à chaque emission
// `CaseDashboardRefreshService.refresh$` (debounce 300ms).
describe('CaseDashboardComponent — F-228 SF-228-01 refresh alignements', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let refreshService: CaseDashboardRefreshService;
  let loaderStub: { loadAll: jest.Mock };

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
    loaderStub = makeAlignmentsLoaderStub();

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: DecisionToolAlignmentsLoader, useValue: loaderStub },
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: CaseDashboardRefreshService, useValue: refreshService },
        { provide: DecisionToolModalService, useValue: { open: jest.fn() } },
        { provide: Router, useValue: { navigate: jest.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    fixture.componentInstance.caseFileId = 'case-1';
  });

  it('re-fetch alignements à chaque emission refresh$ (debounce 300ms)', fakeAsync(() => {
    fixture.detectChanges();
    expect(loaderStub.loadAll).toHaveBeenCalledTimes(1);

    refreshService.triggerRefresh();
    tick(300);
    expect(loaderStub.loadAll).toHaveBeenCalledTimes(2);
  }));

  it('coalesces bursts of triggerRefresh() via debounce (1 re-fetch après 3 emissions rapprochées)', fakeAsync(() => {
    fixture.detectChanges();
    refreshService.triggerRefresh();
    refreshService.triggerRefresh();
    refreshService.triggerRefresh();
    tick(300);
    // 1 mount + 1 debounced re-fetch = 2 appels (pas 4).
    expect(loaderStub.loadAll).toHaveBeenCalledTimes(2);
  }));
});
