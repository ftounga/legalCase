import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

export const paymentRequiredInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);
  const router = inject(Router);

  return next(req).pipe(
    catchError(err => {
      if (err.status === 402) {
        const ref = snackBar.open(
          'Limite de votre plan atteinte. Passez à un plan supérieur pour continuer.',
          'Voir les plans',
          { duration: 8000, panelClass: ['snack-upgrade'] }
        );
        ref.onAction().subscribe(() => router.navigate(['/workspace/billing']));
      }
      return throwError(() => err);
    })
  );
};
