import { ApplicationConfig, ErrorHandler, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { paymentRequiredInterceptor } from './core/interceptors/payment-required.interceptor';
import { loadingInterceptor } from './core/interceptors/loading.interceptor';
import { clientErrorHttpInterceptor } from './core/observability/client-error-http.interceptor';
import { GlobalErrorHandler } from './core/observability/global-error-handler';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([
      authInterceptor,
      paymentRequiredInterceptor,
      loadingInterceptor,
      clientErrorHttpInterceptor
    ])),
    provideAnimationsAsync(),
    // SF-INFRA-09 — remplace Sentry.createErrorHandler() par notre handler
    // qui pousse vers /api/v1/logs/client-error → CloudWatch.
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    provideClientHydration(withEventReplay())
  ]
};
