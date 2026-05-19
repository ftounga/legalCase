import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Workspace } from '../models/workspace.model';
import { SeatsSummary } from '../models/seats-summary.model';
import { SubscriptionState } from '../models/subscription.model';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly apiUrl = '/api/v1/stripe';
  private readonly billingUrl = '/api/v1/billing';
  private bannerShownThisSession = false;

  constructor(private http: HttpClient) {}

  createCheckoutSession(planCode: string): Observable<{ checkoutUrl: string }> {
    return this.http.post<{ checkoutUrl: string }>(`${this.apiUrl}/checkout-session`, { planCode });
  }

  createTopupSession(packCode: string): Observable<{ checkoutUrl: string }> {
    return this.http.post<{ checkoutUrl: string }>(`${this.apiUrl}/topup-session`, { packCode });
  }

  // SF-123-03 : résumé seats + coût mensuel pour header membres + section billing.
  getSeatsSummary(): Observable<SeatsSummary> {
    return this.http.get<SeatsSummary>(`${this.billingUrl}/seats-summary`);
  }

  // SF-247-02 : état d'abonnement (plan, statut, résiliation programmée).
  getSubscription(): Observable<SubscriptionState> {
    return this.http.get<SubscriptionState>(`${this.billingUrl}/subscription`);
  }

  // SF-247-02 : programme la résiliation de l'abonnement en fin de période.
  cancelSubscription(): Observable<SubscriptionState> {
    return this.http.post<SubscriptionState>(`${this.billingUrl}/cancel`, {});
  }

  // SF-247-02 : annule une résiliation programmée.
  resumeSubscription(): Observable<SubscriptionState> {
    return this.http.post<SubscriptionState>(`${this.billingUrl}/resume`, {});
  }

  shouldShowTrialBanner(workspace: Workspace): boolean {
    if (workspace.planCode !== 'FREE') return false;
    if (this.bannerShownThisSession) return false;
    this.bannerShownThisSession = true;
    return true;
  }
}
