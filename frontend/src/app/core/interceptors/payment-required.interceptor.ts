import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { QuotaErrorStateService } from '../services/quota-error-state.service';

/**
 * SF-171-02 — Intercepteur 402.
 *
 * Plus de snackbar direct (qui se chassait avec les handlers locaux et donnait un
 * message hardcodé trompeur). Toute réponse 402 est poussée dans `QuotaErrorState`.
 * Le bandeau partagé `<app-quota-error-banner>` lit ce state et affiche le motif
 * exact issu du backend (champ `code` + `message`, contrat SF-171-01).
 */
export const paymentRequiredInterceptor: HttpInterceptorFn = (req, next) => {
  const quotaErrorState = inject(QuotaErrorStateService);

  return next(req).pipe(
    catchError(err => {
      if (err.status === 402) {
        quotaErrorState.set({
          code: err.error?.code ?? null,
          message: err.error?.message ?? 'Limite atteinte',
          sourceUrl: req.url,
          receivedAt: Date.now(),
        });
      }
      return throwError(() => err);
    })
  );
};
