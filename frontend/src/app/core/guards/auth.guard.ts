import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { WorkspaceService } from '../services/workspace.service';
import { switchMap, map, catchError, of } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const workspace = inject(WorkspaceService);
  const router = inject(Router);

  return auth.loadCurrentUser().pipe(
    switchMap(user => {
      if (!user) return of(router.createUrlTree(['/login']));
      return workspace.getCurrentWorkspace().pipe(
        map(() => true),
        catchError(err => {
          if (err.status === 404) return of(router.createUrlTree(['/onboarding']));
          return of(true);
        })
      );
    })
  );
};
