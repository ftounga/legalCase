/**
 * F-240 SF-240-02 — Service d'acceptation contractuelle (CGU / Privacy / DPA).
 *
 * NE PAS CONFONDRE avec consent.service.ts (cookie-consent GA4).
 * Ce service gère le click-wrap légal via POST /api/v1/consent/accept.
 */
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConsentAcceptanceRequest, ConsentAcceptanceResponse } from '../models/consent.model';

export const CURRENT_CONSENT_VERSION = '2026-05-11';

@Injectable({ providedIn: 'root' })
export class LegalConsentService {

  private readonly endpoint = '/api/v1/consent/accept';

  constructor(private http: HttpClient) {}

  /**
   * Enregistre l'acceptation des documents contractuels (CGU, Privacy, etc.)
   * pour l'utilisateur courant.
   *
   * @returns Observable<ConsentAcceptanceResponse> — 201 avec la liste des acceptations
   */
  acceptConsent(request: ConsentAcceptanceRequest): Observable<ConsentAcceptanceResponse> {
    return this.http.post<ConsentAcceptanceResponse>(this.endpoint, request);
  }
}
