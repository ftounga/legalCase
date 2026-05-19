import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { of, throwError, Observable } from 'rxjs';
import { workspacePlanGuard } from './workspace-plan.guard';
import { WorkspaceService } from '../services/workspace.service';
import { WorkspaceStateService } from '../services/workspace-state.service';
import { Workspace } from '../models/workspace.model';

/**
 * F-156 SF-156-02 — CA11 : OWNER FREE/SOLO sur /workspaces/new → /workspaces/upgrade-required.
 * OWNER TEAM/PRO → /dashboard (la création passe par le dialog du switcher).
 */
describe('workspacePlanGuard (F-156 SF-156-02)', () => {
  let workspaceServiceSpy: jest.Mocked<WorkspaceService>;
  let state: WorkspaceStateService;
  let router: Router;

  function ws(planCode: string): Workspace {
    return { id: 'ws-1', name: 'X', slug: 'x', planCode, status: 'ACTIVE', primary: true };
  }

  function runGuard(): Promise<UrlTree | boolean> {
    return TestBed.runInInjectionContext(() => {
      const result = workspacePlanGuard({} as any, {} as any);
      if (result instanceof Observable) {
        return new Promise<UrlTree | boolean>((resolve) => {
          (result as Observable<UrlTree | boolean>).subscribe(resolve);
        });
      }
      if (result instanceof Promise) return result as Promise<any>;
      return Promise.resolve(result as any);
    });
  }

  beforeEach(() => {
    workspaceServiceSpy = { getCurrentWorkspace: jest.fn() } as any;
    TestBed.configureTestingModule({
      providers: [
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
      ],
    });
    state = TestBed.inject(WorkspaceStateService);
    router = TestBed.inject(Router);
  });

  // CA11 — FREE redirigé
  it('G-01 — plan FREE (cache state) → redirige vers /workspaces/upgrade-required', async () => {
    state.setCurrent(ws('FREE'));
    const result = await runGuard();
    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toContain('/workspaces/upgrade-required');
  });

  // CA11 — SOLO redirigé
  it('G-02 — plan SOLO (cache state) → redirige vers /workspaces/upgrade-required', async () => {
    state.setCurrent(ws('SOLO'));
    const result = await runGuard();
    expect(router.serializeUrl(result as UrlTree)).toContain('/workspaces/upgrade-required');
  });

  // TEAM autorisé (redirigé vers dashboard car création via dialog)
  it('G-03 — plan TEAM (cache state) → redirige vers /dashboard', async () => {
    state.setCurrent(ws('TEAM'));
    const result = await runGuard();
    expect(router.serializeUrl(result as UrlTree)).toContain('/dashboard');
  });

  it('G-04 — plan PRO (cache state) → redirige vers /dashboard', async () => {
    state.setCurrent(ws('PRO'));
    const result = await runGuard();
    expect(router.serializeUrl(result as UrlTree)).toContain('/dashboard');
  });

  // Fallback : state pas chargé → appel HTTP
  it('G-05 — pas de cache, getCurrentWorkspace renvoie FREE → /workspaces/upgrade-required', async () => {
    state.setCurrent(null);
    workspaceServiceSpy.getCurrentWorkspace.mockReturnValue(of(ws('FREE')));
    const result = await runGuard();
    expect(workspaceServiceSpy.getCurrentWorkspace).toHaveBeenCalled();
    expect(router.serializeUrl(result as UrlTree)).toContain('/workspaces/upgrade-required');
  });

  it('G-06 — pas de cache, getCurrentWorkspace renvoie TEAM → /dashboard', async () => {
    state.setCurrent(null);
    workspaceServiceSpy.getCurrentWorkspace.mockReturnValue(of(ws('TEAM')));
    const result = await runGuard();
    expect(router.serializeUrl(result as UrlTree)).toContain('/dashboard');
  });

  it('G-07 — pas de cache, getCurrentWorkspace en erreur → /workspaces/upgrade-required (fail-closed)', async () => {
    state.setCurrent(null);
    workspaceServiceSpy.getCurrentWorkspace.mockReturnValue(throwError(() => ({ status: 500 })));
    const result = await runGuard();
    expect(router.serializeUrl(result as UrlTree)).toContain('/workspaces/upgrade-required');
  });
});
