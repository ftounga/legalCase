import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { SuperAdminBacklogComponent } from './super-admin-backlog.component';
import { BacklogFeatureDetailDialogComponent } from './feature-detail/backlog-feature-detail-dialog.component';
import { BacklogAdminService } from '../../core/services/backlog-admin.service';
import { AuthService } from '../../core/services/auth.service';
import { DashboardAuditService } from '../../core/services/dashboard-audit.service';
import {
  BacklogFeatureSummary,
  BacklogFreshness,
  BacklogMarketingTaskSummary,
  BacklogSyncResult,
} from '../../core/models/backlog.model';
import { DashboardAuditReport } from '../../core/models/dashboard-audit.model';
import { PageResponse } from '../../core/models/super-admin.model';

const mockFreshness: BacklogFreshness = {
  lastSyncAt: '2026-05-02T00:55:00Z',
  lastSuccessAt: '2026-05-02T00:55:00Z',
  status: 'OK',
  minutesSinceLastSync: 3,
};

const mockFeature: BacklogFeatureSummary = {
  id: 'fid-1',
  code: 'F-178',
  title: 'Visualiseur de backlog',
  targetVersion: 'V8+',
  status: 'IN_PROGRESS',
  domain: 'TRANSVERSAL',
  priority: 'MEDIUM',
  updatedAt: '2026-05-02T00:00:00Z',
};

const mockMarketing: BacklogMarketingTaskSummary = {
  id: 'mid-1',
  code: 'M-71',
  title: 'Cadrage budget marketing 2026 H2',
  status: 'TERMINE',
  category: 'Cadrage stratégique',
  updatedAt: '2026-04-30T00:00:00Z',
};

const mockSyncResult: BacklogSyncResult = {
  runId: 'run-1', durationMs: 312, featuresCount: 184,
  subfeaturesCount: 612, marketingCount: 76, orphansMarked: 0, success: true,
};

const mockAuditReport: DashboardAuditReport = {
  ranAt: '2026-05-19T18:00:00Z',
  crashedMappers: [
    {
      toolId: 'indemnite-licenciement',
      crashCount: 4,
      lastExceptionClass: 'NullPointerException',
      lastExceptionMessage: 'tile data missing',
      lastOccurredAt: '2026-05-19T17:45:00Z',
    },
    {
      toolId: 'preavis-calculator',
      crashCount: 9,
      lastExceptionClass: 'IllegalStateException',
      lastExceptionMessage: 'bad state',
      lastOccurredAt: '2026-05-18T10:00:00Z',
    },
  ],
  dormantTiles: [
    { tableName: 'rupture_conventionnelle_analyses', rowCount: 0 },
  ],
  activeTiles: [
    { tableName: 'indemnite_licenciement_analyses', rowCount: 12 },
    { tableName: 'preavis_analyses', rowCount: 31 },
  ],
};

const emptyAuditReport: DashboardAuditReport = {
  ranAt: '2026-05-19T18:00:00Z',
  crashedMappers: [],
  dormantTiles: [],
  activeTiles: [],
};

function pageOf<T>(content: T[]): PageResponse<T> {
  return { content, totalElements: content.length, totalPages: 1, size: 50, number: 0 };
}

describe('SuperAdminBacklogComponent', () => {
  let component: SuperAdminBacklogComponent;
  let fixture: ComponentFixture<SuperAdminBacklogComponent>;
  let backlogService: any;
  let auditService: any;
  let snackBar: any;
  let dialog: any;
  let router: Router;

  function setup(isSuperAdmin: boolean) {
    backlogService = jasmine.createSpyObj('BacklogAdminService', [
      'searchFeatures', 'searchMarketingTasks', 'getFreshness',
      'triggerSync', 'getFeatureDetail',
    ]);
    auditService = jasmine.createSpyObj('DashboardAuditService', [
      'getLatest', 'runAudit',
    ]);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj('MatDialog', ['open']);

    backlogService.getFreshness.mockReturnValue(of(mockFreshness));
    backlogService.searchFeatures.mockReturnValue(of(pageOf([mockFeature])));
    backlogService.searchMarketingTasks.mockReturnValue(of(pageOf([mockMarketing])));
    backlogService.triggerSync.mockReturnValue(of(mockSyncResult));
    auditService.getLatest.mockReturnValue(of(mockAuditReport));
    auditService.runAudit.mockReturnValue(of(mockAuditReport));

    const currentUser = signal<any>({ id: 'u-sa', email: 'sa@test.com', isSuperAdmin });
    const authService = { currentUser };

    TestBed.configureTestingModule({
      imports: [SuperAdminBacklogComponent, NoopAnimationsModule],
      providers: [
        { provide: BacklogAdminService, useValue: backlogService },
        { provide: DashboardAuditService, useValue: auditService },
        { provide: AuthService, useValue: authService },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        provideRouter([{ path: 'case-files', component: SuperAdminBacklogComponent }]),
      ],
    });

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(SuperAdminBacklogComponent);
    component = fixture.componentInstance;
  }

  it('redirects non-super-admin users to /case-files', () => {
    setup(false);
    fixture.detectChanges();
    expect(router.navigate).toHaveBeenCalledWith(['/case-files']);
    expect(backlogService.searchFeatures).not.toHaveBeenCalled();
  });

  it('loads freshness + features on init for super-admin', () => {
    setup(true);
    fixture.detectChanges();
    expect(backlogService.getFreshness).toHaveBeenCalledTimes(1);
    expect(backlogService.searchFeatures).toHaveBeenCalledTimes(1);
    expect(component.freshness()?.status).toBe('OK');
    expect(component.features()).toEqual([mockFeature]);
    expect(component.featuresTotal()).toBe(1);
  });

  it('lazy-loads marketing tab on first switch', () => {
    setup(true);
    fixture.detectChanges();
    expect(backlogService.searchMarketingTasks).not.toHaveBeenCalled();
    component.onTabChange(1);
    expect(backlogService.searchMarketingTasks).toHaveBeenCalledTimes(1);
    expect(component.marketing()).toEqual([mockMarketing]);
    expect(component.marketingLoaded()).toBe(true);
  });

  it('reloads features and resets to page 0 when filter changes', () => {
    setup(true);
    fixture.detectChanges();
    backlogService.searchFeatures.mockClear();
    component.featuresPage.set(3);
    component.filterStatus.set('BLOCKED');
    component.onFilterFeatureChange();
    expect(component.featuresPage()).toBe(0);
    expect(backlogService.searchFeatures).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'BLOCKED' }),
      0,
      50,
    );
  });

  it('debounces feature search input by 250ms', fakeAsync(() => {
    setup(true);
    fixture.detectChanges();
    backlogService.searchFeatures.mockClear();

    component.onFeatureSearchInput('immi');
    component.onFeatureSearchInput('immig');
    component.onFeatureSearchInput('immigration');
    tick(100);
    expect(backlogService.searchFeatures).not.toHaveBeenCalled();

    tick(200);
    expect(backlogService.searchFeatures).toHaveBeenCalledTimes(1);
    expect(backlogService.searchFeatures).toHaveBeenCalledWith(
      expect.objectContaining({ search: 'immigration' }),
      0,
      50,
    );
  }));

  it('triggers resync, shows success snackbar and reloads data', () => {
    setup(true);
    fixture.detectChanges();
    backlogService.searchFeatures.mockClear();
    backlogService.getFreshness.mockClear();

    component.triggerResync();

    expect(component.resyncing()).toBe(false);
    expect(backlogService.triggerSync).toHaveBeenCalledTimes(1);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Resync OK'),
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-success'] }),
    );
    expect(backlogService.getFreshness).toHaveBeenCalledTimes(1);
    expect(backlogService.searchFeatures).toHaveBeenCalledTimes(1);
  });

  it('handles resync error with error snackbar without crashing', () => {
    setup(true);
    fixture.detectChanges();
    backlogService.triggerSync.mockReturnValue(throwError(() => new Error('boom')));

    component.triggerResync();

    expect(component.resyncing()).toBe(false);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Échec'),
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] }),
    );
  });

  it('maps freshness status to correct tone and label', () => {
    setup(true);
    fixture.detectChanges();

    component.freshness.set({ ...mockFreshness, status: 'OK', minutesSinceLastSync: 4 });
    expect(component.freshnessTone()).toBe('ok');
    expect(component.freshnessLabel()).toContain('Synchronisé il y a 4');

    component.freshness.set({ ...mockFreshness, status: 'STALE', minutesSinceLastSync: 12 });
    expect(component.freshnessTone()).toBe('stale');
    expect(component.freshnessLabel()).toContain('obsolète');

    component.freshness.set({ ...mockFreshness, status: 'ERROR', minutesSinceLastSync: null });
    expect(component.freshnessTone()).toBe('error');
    expect(component.freshnessLabel()).toContain('erreur');
  });

  it('flags freshnessError when freshness fetch fails (without disrupting feature list)', () => {
    setup(true);
    backlogService.getFreshness.mockReturnValue(throwError(() => new Error('net')));
    fixture.detectChanges();
    expect(component.freshnessError()).toBe(true);
    expect(component.features()).toEqual([mockFeature]);
  });

  it('opens BacklogFeatureDetailDialogComponent when openFeatureDetail is called with a code', () => {
    setup(true);
    fixture.detectChanges();
    (component as any).dialog = dialog;
    component.openFeatureDetail('F-178');
    expect(dialog.open).toHaveBeenCalledWith(
      BacklogFeatureDetailDialogComponent,
      expect.objectContaining({ data: { code: 'F-178' } }),
    );
  });

  it('does nothing when openFeatureDetail receives an empty code', () => {
    setup(true);
    fixture.detectChanges();
    (component as any).dialog = dialog;
    component.openFeatureDetail('');
    expect(dialog.open).not.toHaveBeenCalled();
  });

  // ── SF-178-05 — Vue kanban ─────────────────────────────────────────────

  it('defaults viewMode to list and switches to kanban via onViewModeChange', () => {
    setup(true);
    fixture.detectChanges();
    expect(component.viewMode()).toBe('list');
    component.onViewModeChange('kanban');
    expect(component.viewMode()).toBe('kanban');
  });

  it('reloads features with kanbanPageSize=200 when switching to kanban', () => {
    setup(true);
    fixture.detectChanges();
    backlogService.searchFeatures.mockClear();
    component.onViewModeChange('kanban');
    expect(backlogService.searchFeatures).toHaveBeenCalledTimes(1);
    const call = backlogService.searchFeatures.mock.calls[0];
    expect(call[2]).toBe(200);
  });

  it('groupedFeatures buckets statuses into 5 main columns + OTHER', () => {
    setup(true);
    fixture.detectChanges();
    component.features.set([
      { ...mockFeature, id: '1', code: 'F-100', status: 'READY' },
      { ...mockFeature, id: '2', code: 'F-101', status: 'IN_PROGRESS' },
      { ...mockFeature, id: '3', code: 'F-102', status: 'BLOCKED' },
      { ...mockFeature, id: '4', code: 'F-103', status: 'DONE' },
      { ...mockFeature, id: '5', code: 'F-104', status: 'PLANNED' },
      { ...mockFeature, id: '6', code: 'F-105', status: 'PARTIAL' },
      { ...mockFeature, id: '7', code: 'F-106', status: 'ABSORBED' },
    ]);
    const groups = component.groupedFeatures();
    expect(groups.get('READY')!.length).toBe(1);
    expect(groups.get('IN_PROGRESS')!.length).toBe(1);
    expect(groups.get('BLOCKED')!.length).toBe(1);
    expect(groups.get('DONE')!.length).toBe(1);
    expect(groups.get('PLANNED')!.length).toBe(1);
    expect(groups.get('OTHER')!.length).toBe(2);
  });

  it('showOtherColumn returns true only when OTHER bucket is non-empty', () => {
    setup(true);
    fixture.detectChanges();

    component.features.set([
      { ...mockFeature, id: '1', code: 'F-100', status: 'READY' },
    ]);
    expect(component.showOtherColumn()).toBe(false);

    component.features.set([
      { ...mockFeature, id: '1', code: 'F-100', status: 'READY' },
      { ...mockFeature, id: '2', code: 'F-101', status: 'PARTIAL' },
    ]);
    expect(component.showOtherColumn()).toBe(true);
  });

  it('onViewModeChange is a no-op when called with the current mode', () => {
    setup(true);
    fixture.detectChanges();
    backlogService.searchFeatures.mockClear();
    component.onViewModeChange('list');
    expect(backlogService.searchFeatures).not.toHaveBeenCalled();
  });

  // ── F-180 SF-180-02 — Tab « Audit dashboard » ──────────────────────────

  it('does not load the audit report on init (lazy)', () => {
    setup(true);
    fixture.detectChanges();
    expect(auditService.getLatest).not.toHaveBeenCalled();
    expect(component.auditLoaded()).toBe(false);
  });

  it('lazy-loads the audit report only on first switch to tab index 2', () => {
    setup(true);
    fixture.detectChanges();

    component.onTabChange(2);
    expect(auditService.getLatest).toHaveBeenCalledTimes(1);
    expect(component.auditLoaded()).toBe(true);
    expect(component.auditLoading()).toBe(false);

    // re-opening the tab must not reload (lazy)
    component.onTabChange(0);
    component.onTabChange(2);
    expect(auditService.getLatest).toHaveBeenCalledTimes(1);
  });

  it('renders the 3 panels from a populated audit report', () => {
    setup(true);
    fixture.detectChanges();
    component.onTabChange(2);
    fixture.detectChanges();

    expect(component.auditReport()).toEqual(mockAuditReport);
    expect(component.auditReport()!.crashedMappers.length).toBe(2);
    expect(component.auditReport()!.dormantTiles.length).toBe(1);
    expect(component.auditReport()!.activeTiles.length).toBe(2);

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Mappers en erreur');
    expect(text).toContain('Tiles dormantes');
    expect(text).toContain('Tiles actives');
    expect(text).toContain('indemnite-licenciement');
  });

  it('handles the empty state without crashing (0 crash / 0 dormant / 0 active)', () => {
    setup(true);
    auditService.getLatest.mockReturnValue(of(emptyAuditReport));
    fixture.detectChanges();
    component.onTabChange(2);
    fixture.detectChanges();

    expect(component.auditError()).toBe(false);
    expect(component.sortedCrashedMappers()).toEqual([]);
    expect(component.sortedActiveTiles()).toEqual([]);

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Aucun mapper en erreur');
    expect(text).toContain('Aucune tile dormante');
    expect(text).toContain('Aucune analyse produite');
  });

  it('flags auditError and shows an error snackbar on a network failure', () => {
    setup(true);
    auditService.getLatest.mockReturnValue(throwError(() => ({ status: 500 })));
    fixture.detectChanges();

    component.onTabChange(2);

    expect(component.auditLoading()).toBe(false);
    expect(component.auditError()).toBe(true);
    expect(component.auditReport()).toBeNull();
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('audit'),
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] }),
    );
  });

  it('redirects to /case-files when the audit load returns 403', () => {
    setup(true);
    auditService.getLatest.mockReturnValue(throwError(() => ({ status: 403 })));
    fixture.detectChanges();

    component.onTabChange(2);

    expect(router.navigate).toHaveBeenCalledWith(['/case-files']);
  });

  it('triggerAuditRun posts to /run, toggles auditRunning, refreshes panels and shows success snackbar', () => {
    setup(true);
    fixture.detectChanges();
    component.onTabChange(2);

    const refreshed: DashboardAuditReport = { ...emptyAuditReport, ranAt: '2026-05-19T19:00:00Z' };
    auditService.runAudit.mockReturnValue(of(refreshed));

    component.triggerAuditRun();

    expect(auditService.runAudit).toHaveBeenCalledTimes(1);
    expect(component.auditRunning()).toBe(false);
    expect(component.auditReport()).toEqual(refreshed);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('relancé'),
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-success'] }),
    );
  });

  it('triggerAuditRun does not double-post while a run is already in progress', () => {
    setup(true);
    fixture.detectChanges();
    component.onTabChange(2);

    component.auditRunning.set(true);
    component.triggerAuditRun();

    expect(auditService.runAudit).not.toHaveBeenCalled();
  });

  it('triggerAuditRun handles an error with an error snackbar without crashing', () => {
    setup(true);
    fixture.detectChanges();
    component.onTabChange(2);
    auditService.runAudit.mockReturnValue(throwError(() => ({ status: 500 })));

    component.triggerAuditRun();

    expect(component.auditRunning()).toBe(false);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Échec'),
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] }),
    );
  });

  it('sortedCrashedMappers sorts by crashCount desc by default', () => {
    setup(true);
    fixture.detectChanges();
    component.onTabChange(2);

    const sorted = component.sortedCrashedMappers();
    expect(sorted.map(m => m.crashCount)).toEqual([9, 4]);
  });

  it('onCrashedSortChange re-sorts the crashed mappers table', () => {
    setup(true);
    fixture.detectChanges();
    component.onTabChange(2);

    component.onCrashedSortChange({ active: 'toolId', direction: 'asc' });
    expect(component.sortedCrashedMappers().map(m => m.toolId))
      .toEqual(['indemnite-licenciement', 'preavis-calculator']);
  });

  it('sortedActiveTiles sorts by rowCount desc by default and re-sorts on header click', () => {
    setup(true);
    fixture.detectChanges();
    component.onTabChange(2);

    expect(component.sortedActiveTiles().map(t => t.rowCount)).toEqual([31, 12]);

    component.onActiveSortChange({ active: 'tableName', direction: 'asc' });
    expect(component.sortedActiveTiles().map(t => t.tableName))
      .toEqual(['indemnite_licenciement_analyses', 'preavis_analyses']);
  });

  it('kubectlLogsHint builds a kubectl command scoped to the tool id', () => {
    setup(true);
    fixture.detectChanges();
    const hint = component.kubectlLogsHint('preavis-calculator');
    expect(hint).toContain('kubectl logs');
    expect(hint).toContain('preavis-calculator');
  });
});
