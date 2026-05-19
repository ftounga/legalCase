import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SubscriptionStatus {
  optedOut: boolean;
}

/**
 * Service de gestion de l'abonnement aux emails non-transactionnels.
 * Consomme les endpoints publics token-based de SF-248-01.
 */
@Injectable({ providedIn: 'root' })
export class EmailSubscriptionService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/public/email';

  getStatus(token: string): Observable<SubscriptionStatus> {
    return this.http.get<SubscriptionStatus>(`${this.baseUrl}/subscription-status`, {
      params: { token }
    });
  }

  unsubscribe(token: string): Observable<SubscriptionStatus> {
    return this.http.post<SubscriptionStatus>(`${this.baseUrl}/unsubscribe`, { token });
  }

  resubscribe(token: string): Observable<SubscriptionStatus> {
    return this.http.post<SubscriptionStatus>(`${this.baseUrl}/resubscribe`, { token });
  }
}
