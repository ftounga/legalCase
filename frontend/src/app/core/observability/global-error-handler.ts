import { ErrorHandler, Injectable, inject } from '@angular/core';
import { ClientErrorService } from './client-error.service';

/**
 * ErrorHandler Angular qui route toute exception non gérée vers le backend
 * (endpoint /api/v1/logs/client-error → SLF4J → CloudWatch).
 *
 * <p>SF-INFRA-09 — remplace {@code Sentry.createErrorHandler()}.</p>
 */
@Injectable({ providedIn: 'root' })
export class GlobalErrorHandler extends ErrorHandler {
  private readonly clientErrorService = inject(ClientErrorService);

  override handleError(error: unknown): void {
    try {
      const { message, stack } = this.extractErrorInfo(error);
      this.clientErrorService.report({
        message,
        stack,
        url: typeof window !== 'undefined' ? window.location?.href : undefined,
        userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : undefined,
        timestamp: new Date().toISOString()
      });
    } catch {
      // Ne jamais throw depuis un ErrorHandler — fallback silencieux.
    }
    // Préserve le comportement Angular natif (console.error en dev).
    super.handleError(error);
  }

  private extractErrorInfo(error: unknown): { message: string; stack?: string } {
    if (error instanceof Error) {
      return {
        message: error.message || error.name || 'Unknown error',
        stack: error.stack
      };
    }
    if (typeof error === 'string') {
      return { message: error };
    }
    try {
      return { message: JSON.stringify(error) ?? 'Unknown error' };
    } catch {
      return { message: 'Unknown error (unserializable)' };
    }
  }
}
