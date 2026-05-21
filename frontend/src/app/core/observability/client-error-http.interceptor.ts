import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ClientErrorService } from './client-error.service';

/**
 * Intercepteur HTTP qui remonte les réponses 5xx vers le backend pour
 * observabilité CloudWatch.
 *
 * <p>SF-INFRA-09. Anti-boucle : ignore les requêtes vers l'endpoint
 * /api/v1/logs/client-error lui-même.</p>
 */
export const clientErrorHttpInterceptor: HttpInterceptorFn = (req, next) => {
  const clientErrorService = inject(ClientErrorService);
  const isSelfReportingEndpoint = req.url.includes(ClientErrorService.ENDPOINT);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (!isSelfReportingEndpoint && error.status >= 500 && error.status < 600) {
        clientErrorService.report({
          message: `HTTP ${error.status} on ${req.method} ${req.url}`,
          stack: error.message,
          url: typeof window !== 'undefined' ? window.location?.href : undefined,
          userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : undefined,
          timestamp: new Date().toISOString()
        });
      }
      return throwError(() => error);
    })
  );
};
