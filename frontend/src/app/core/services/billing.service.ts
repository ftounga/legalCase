import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Workspace } from '../models/workspace.model';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly apiUrl = '/api/v1/stripe';
  private bannerShownThisSession = false;

  constructor(private http: HttpClient) {}

  createCheckoutSession(planCode: string): Observable<{ checkoutUrl: string }> {
    return this.http.post<{ checkoutUrl: string }>(`${this.apiUrl}/checkout-session`, { planCode });
  }

  shouldShowTrialBanner(workspace: Workspace): boolean {
    if (workspace.planCode !== 'FREE') return false;
    if (this.bannerShownThisSession) return false;
    this.bannerShownThisSession = true;
    return true;
  }
}
