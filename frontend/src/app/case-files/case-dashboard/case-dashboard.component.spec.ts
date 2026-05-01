import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of } from 'rxjs';
import { CaseDashboardComponent } from './case-dashboard.component';
import { CaseDashboardService } from '../../core/services/case-dashboard.service';
import { CaseDashboardRefreshService } from './case-dashboard-refresh.service';
import { DashboardResponse } from '../../core/models/case-dashboard.model';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { DecisionToolModalService } from '../decisional-tools-panel/decision-tool-modal/decision-tool-modal.service';
import { LicenciementSectionComponent } from '../licenciement-section/licenciement-section.component';

describe('CaseDashboardComponent — refresh integration (SF-IA-02-03)', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let refreshService: CaseDashboardRefreshService;

  const emptyDashboard: DashboardResponse = {
    caseFileId: 'case-1',
    riskScore: null, riskLevel: null,
    licenciement: null, indemnites: null, anciennete: null,
    titleDecision: null, workRight: null, recours: null,
    partage: null, garde: null, divorce: null,
  } as unknown as DashboardResponse;

  beforeEach(async () => {
    dashboardService = { get: jest.fn().mockReturnValue(of(emptyDashboard)) } as any;
    refreshService = new CaseDashboardRefreshService();

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: CaseDashboardRefreshService, useValue: refreshService },
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

describe('CaseDashboardComponent — SF-177-09 cards + modal', () => {
  let fixture: ComponentFixture<CaseDashboardComponent>;
  let component: CaseDashboardComponent;
  let dashboardService: jest.Mocked<CaseDashboardService>;
  let modalService: jest.Mocked<DecisionToolModalService>;

  const fullDashboard: DashboardResponse = {
    caseFileId: 'case-1',
    riskScore: 45,
    riskLevel: 'MOYEN',
    licenciement: { scoreRisque: 70, verdict: 'INVALIDE', criteresNonConformes: 3, criteresTotal: 8 },
    indemnites: { country: 'FRANCE', fourchetteBasse: 2000, fourhetteHaute: 5000, baremeSource: 'Macron' },
    anciennete: { annees: 4, mois: 6, congesTotalJours: 25, ecartsDetectes: 1 },
    titleDecision: { nbRecommandations: 2, premierTitreLabel: 'VPF' },
    workRight: { droitTravail: 'OUI', titreLabel: 'CDS Vie privée et familiale' },
    recours: { recoursLabel: 'OQTF', dateLimite: '2026-06-15', dateLimiteDepassee: false },
    partage: { soulte: 12000, coutTotal: 15000 },
    garde: { gardeLabel: 'Alternée', joursParentA: 180, joursParentB: 185 },
    divorce: { etapesCompletees: 3, etapesTotal: 8, piecesPresentes: 7, piecesTotal: 12, progressionPct: 38 },
  } as unknown as DashboardResponse;

  beforeEach(async () => {
    dashboardService = { get: jest.fn().mockReturnValue(of(fullDashboard)) } as any;
    modalService = { open: jest.fn().mockReturnValue({ close: jest.fn() }) } as any;

    await TestBed.configureTestingModule({
      imports: [CaseDashboardComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CaseDashboardService, useValue: dashboardService },
        { provide: DecisionToolModalService, useValue: modalService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDashboardComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  it('SF-177-09 T-01 : tilesFromDashboard produit riskScore + 9 tiles quand tous champs peuplés', () => {
    fixture.detectChanges();
    const tiles = component.tilesFromDashboard(fullDashboard);
    expect(tiles).toHaveLength(10);
    expect(tiles[0].toolId).toBe('risk-score');
    expect(tiles[0].disabled).toBe(true);
    const toolIds = tiles.slice(1).map((t) => t.toolId);
    expect(toolIds).toEqual([
      'F-DT-08-licenciement-validity',
      'F-DT-09-comparateur-indemnites',
      'F-DT-07-anciennete-conges-prime',
      'F-IM-05-arbre-decisionnel-titre',
      'F-IM-07-droit-au-travail',
      'F-IM-06-recours',
      'F-FA-05-partage-immobilier',
      'F-FA-06-calendrier-garde',
      'F-FA-07-checklist-divorce',
    ]);
  });

  it('SF-177-09 T-02 : tilesFromDashboard filtre les champs null', () => {
    const partialDashboard = {
      ...fullDashboard,
      workRight: null,
      recours: null,
      partage: null,
      garde: null,
      divorce: null,
    } as DashboardResponse;
    fixture.detectChanges();
    const tiles = component.tilesFromDashboard(partialDashboard);
    const toolIds = tiles.map((t) => t.toolId);
    expect(toolIds).not.toContain('F-IM-07-droit-au-travail');
    expect(toolIds).not.toContain('F-IM-06-recours');
    expect(toolIds).not.toContain('F-FA-05-partage-immobilier');
    expect(toolIds).not.toContain('F-FA-06-calendrier-garde');
    expect(toolIds).not.toContain('F-FA-07-checklist-divorce');
    // riskScore + licenciement + indemnites + anciennete + titleDecision = 5
    expect(tiles).toHaveLength(5);
  });

  it('SF-177-09 T-03 : licenciement INVALIDE → metierAlertLevel ALERT', () => {
    fixture.detectChanges();
    const tiles = component.tilesFromDashboard(fullDashboard);
    const lic = tiles.find((t) => t.toolId === 'F-DT-08-licenciement-validity');
    expect(lic?.metierAlertLevel).toBe('ALERT');

    const validDashboard = {
      ...fullDashboard,
      licenciement: { ...fullDashboard.licenciement!, verdict: 'VALIDE' },
    } as DashboardResponse;
    const tilesValid = component.tilesFromDashboard(validDashboard);
    const licValid = tilesValid.find((t) => t.toolId === 'F-DT-08-licenciement-validity');
    expect(licValid?.metierAlertLevel).toBe('OK');
  });

  it('SF-177-09 T-04 : openTile délègue au modalService avec forceExpanded:true et bon component', () => {
    fixture.detectChanges();
    const tiles = component.tilesFromDashboard(fullDashboard);
    const lic = tiles.find((t) => t.toolId === 'F-DT-08-licenciement-validity')!;
    component.openTile(lic);

    expect(modalService.open).toHaveBeenCalledTimes(1);
    const args = modalService.open.mock.calls[0][0];
    expect(args.toolId).toBe('F-DT-08-licenciement-validity');
    expect(args.component).toBe(LicenciementSectionComponent);
    expect(args.inputs['forceExpanded']).toBe(true);
    expect(args.inputs['caseFileId']).toBe('case-1');
    expect(args.inputs['workspaceCountry']).toBe('FRANCE');
    expect(args.onSave).toBeUndefined();
  });

  it('SF-177-09 T-05 : openTile sur tile riskScore (disabled) ne déclenche pas le modal', () => {
    fixture.detectChanges();
    const tiles = component.tilesFromDashboard(fullDashboard);
    const risk = tiles.find((t) => t.toolId === 'risk-score')!;
    expect(risk.disabled).toBe(true);
    expect(risk.component).toBeNull();
    component.openTile(risk);
    expect(modalService.open).not.toHaveBeenCalled();
  });
});
