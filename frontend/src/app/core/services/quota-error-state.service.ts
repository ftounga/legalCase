import { Injectable, Signal, WritableSignal, computed, signal } from '@angular/core';

/**
 * SF-171-02 — état partagé d'erreur quota / budget tokens.
 *
 * Le `paymentRequiredInterceptor` pousse ici toute réponse 402, et les composants
 * abonnés (banner, boutons en `disabled-quota`) lisent le signal pour adapter l'UI.
 */
export interface QuotaError {
  /** Code machine-readable du body 402 (SF-171-01). `null` si legacy / inconnu. */
  code: string | null;
  /** Message brut backend, utilisé en fallback ou complément du titre mappé. */
  message: string;
  /** URL de la requête qui a renvoyé 402 (debug / source). */
  sourceUrl: string;
  /** `Date.now()` au moment de la réception. */
  receivedAt: number;
}

@Injectable({ providedIn: 'root' })
export class QuotaErrorStateService {
  private readonly _error: WritableSignal<QuotaError | null> = signal(null);

  readonly error: Signal<QuotaError | null> = this._error.asReadonly();

  set(err: QuotaError): void {
    this._error.set(err);
  }

  clear(): void {
    this._error.set(null);
  }

  /**
   * Helper pour les boutons : `[disabled]="quotaErrorState.matches('TOKEN_BUDGET_EXCEEDED')()"`.
   * Retourne un signal qui vaut `true` tant que l'erreur courante a le code donné.
   */
  matches(code: string): Signal<boolean> {
    return computed(() => this._error()?.code === code);
  }
}
