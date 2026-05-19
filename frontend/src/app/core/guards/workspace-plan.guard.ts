import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { WorkspaceService } from '../services/workspace.service';
import { WorkspaceStateService } from '../services/workspace-state.service';

/**
 * F-156 SF-156-02 — guard pour la route `/workspaces/new`.
 *
 * <p>Comportement (CA11) :
 * <ul>
 *   <li>OWNER TEAM/PRO → autorisé (redirige sur le dashboard car la création se fait via le dialog,
 *       pas via une route dédiée — la route existe uniquement pour bricolage d'URL).</li>
 *   <li>OWNER FREE/SOLO → redirigé vers `/workspaces/upgrade-required` (page d'erreur dédiée).</li>
 * </ul>
 *
 * <p>Source du plan : `WorkspaceStateService.planCode` si déjà chargé, sinon
 * récupération directe via `WorkspaceService.getCurrentWorkspace()`.
 */
export const workspacePlanGuard: CanActivateFn = () => {
  const state = inject(WorkspaceStateService);
  const service = inject(WorkspaceService);
  const router = inject(Router);

  const decide = (planCode: string | null | undefined) => {
    if (planCode && (planCode.toUpperCase() === 'TEAM' || planCode.toUpperCase() === 'PRO')) {
      // OWNER autorisé — on renvoie vers le dashboard puisque la création se fait via le dialog
      // du switcher, pas via cette route. Le guard sert uniquement à protéger l'URL bricolée.
      return router.createUrlTree(['/dashboard']);
    }
    return router.createUrlTree(['/workspaces/upgrade-required']);
  };

  const cached = state.planCode();
  if (cached) {
    return of(decide(cached));
  }
  return service.getCurrentWorkspace().pipe(
    map(ws => {
      state.setCurrent(ws);
      return decide(ws.planCode);
    }),
    catchError(() => of(router.createUrlTree(['/workspaces/upgrade-required'])))
  );
};
