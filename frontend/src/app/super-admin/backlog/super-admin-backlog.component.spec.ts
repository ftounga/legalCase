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
import {
  BacklogFeatureSummary,
  BacklogFreshness,
  BacklogMarketingTaskSummary,
  BacklogSyncResult,
} from '../../core/models/backlog.model';
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

function pageOf<T>(content: T[]): PageResponse<T> {
  return { content, totalElements: content.length, totalPages: 1, size: 50, number: 0 };
}

describe('SuperAdminBacklogComponent', () => {
  let component: SuperAdminBacklogComponent;
  let fixture: ComponentFixture<SuperAdminBacklogComponent>;
  let backlogService: any;
  let snackBar: any;
  let dialog: any;
  let router: Router;

  function setup(isSuperAdmin: boolean) {
    backlogService = jasmine.createSpyObj('BacklogAdminService', [
      'searchFeatures', 'searchMarketingTasks', 'getFreshness',
      'triggerSync', 'getFeatureDetail',
    ]);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj('MatDialog', ['open']);

    backlogService.getFreshness.mockReturnValue(of(mockFreshness));
    backlogService.searchFeatures.mockReturnValue(of(pageOf([mockFeature])));
    backlogService.searchMarketingTasks.mockReturnValue(of(pageOf([mockMarketing])));
    backlogService.triggerSync.mockReturnValue(of(mockSyncResult));

    const currentUser = signal<any>({ id: 'u-sa', email: 'sa@test.com', isSuperAdmin });
    const authService = { currentUser };

    TestBed.configureTestingModule({
      imports: [SuperAdminBacklogComponent, NoopAnimationsModule],
      providers: [
        { provide: BacklogAdminService, useValue: backlogService },
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
});
