import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, of } from 'rxjs';
import { ClientErrorPayload } from './client-error-payload.model';

/**
 * Service de remontée des erreurs frontend vers le backend.
 *
 * <p>SF-INFRA-09 — remplace Sentry. Dédup au niveau session via un Set
 * en mémoire pour ne pas spammer CloudWatch quand le même bug se reproduit
 * 100x dans la même session avocat.</p>
 */
@Injectable({ providedIn: 'root' })
export class ClientErrorService {
  static readonly ENDPOINT = '/api/v1/logs/client-error';
  static readonly MAX_MESSAGE_LENGTH = 500;
  static readonly MAX_STACK_LENGTH = 4000;
  static readonly MAX_URL_LENGTH = 500;
  static readonly MAX_UA_LENGTH = 200;

  private readonly http = inject(HttpClient);
  private readonly seenHashes = new Set<string>();

  report(payload: ClientErrorPayload): void {
    const message = this.truncateRequired(payload.message, ClientErrorService.MAX_MESSAGE_LENGTH);
    const normalized: ClientErrorPayload = {
      message,
      stack: this.truncate(payload.stack, ClientErrorService.MAX_STACK_LENGTH),
      url: this.truncate(payload.url, ClientErrorService.MAX_URL_LENGTH),
      userAgent: this.truncate(payload.userAgent, ClientErrorService.MAX_UA_LENGTH),
      timestamp: payload.timestamp ?? new Date().toISOString(),
      appVersion: payload.appVersion
    };

    const hash = this.hash(normalized);
    if (this.seenHashes.has(hash)) {
      return;
    }
    this.seenHashes.add(hash);

    this.http
      .post(ClientErrorService.ENDPOINT, normalized)
      .pipe(catchError(() => of(null)))
      .subscribe();
  }

  /** Exposé pour les tests (reset état entre cas). */
  resetDedupForTest(): void {
    this.seenHashes.clear();
  }

  private truncate(value: string | undefined, max: number): string | undefined {
    if (value === undefined || value === null) {
      return undefined;
    }
    return value.length > max ? value.slice(0, max) : value;
  }

  private truncateRequired(value: string, max: number): string {
    if (value === undefined || value === null) {
      return '';
    }
    return value.length > max ? value.slice(0, max) : value;
  }

  private hash(payload: ClientErrorPayload): string {
    const stackHead = (payload.stack ?? '').slice(0, 200);
    return `${payload.message}::${stackHead}`;
  }
}
