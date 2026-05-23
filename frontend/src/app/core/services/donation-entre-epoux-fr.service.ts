import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DonationEntreEpouxRequest,
  DonationEntreEpouxResponse,
} from '../models/donation-entre-epoux-fr.model';

/**
 * SF-216-24 : wrapper HttpClient pour l'outil décisionnel "Donation entre
 * époux" (F-FA-DONATION-ENTRE-EPOUX, FR — art. 1091-1100 Cciv).
 * Consomme l'API figée dans SF-216-23.
 *
 * Endpoints :
 *   POST /api/v1/case-files/{caseFileId}/donation-entre-epoux
 *   GET  /api/v1/case-files/{caseFileId}/donation-entre-epoux
 */
@Injectable({ providedIn: 'root' })
export class DonationEntreEpouxFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse Donation entre époux. */
  calculate(
    caseFileId: string,
    request: DonationEntreEpouxRequest,
  ): Observable<DonationEntreEpouxResponse> {
    return this.http.post<DonationEntreEpouxResponse>(
      `/api/v1/case-files/${caseFileId}/donation-entre-epoux`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<DonationEntreEpouxResponse> {
    return this.http.get<DonationEntreEpouxResponse>(
      `/api/v1/case-files/${caseFileId}/donation-entre-epoux`,
    );
  }
}
