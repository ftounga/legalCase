import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        sessionStorage.setItem('auth.returnUrl', router.url);
        snackBar.open('Votre session a expiré, veuillez vous reconnecter', 'Fermer', {
          duration: 5000,
          panelClass: ['snack-error']
        });
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
