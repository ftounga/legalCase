import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DonationPartageRequest,
  DonationPartageResponse,
} from '../models/donation-partage-fr.model';

/**
 * SF-216-30 : wrapper HttpClient pour l'outil décisionnel "Donation-partage"
 * (F-FA-DONATION-PARTAGE, FR — art. 1075 à 1075-5 Cciv + art. 1078, 1078-1,
 * 1080 + art. 912-928). Consomme l'API figée dans SF-216-29.
 *
 * Endpoints :
 *   POST /api/v1/case-files/{caseFileId}/donation-partage
 *   GET  /api/v1/case-files/{caseFileId}/donation-partage
 */
@Injectable({ providedIn: 'root' })
export class DonationPartageFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse Donation-partage. */
  calculate(
    caseFileId: string,
    request: DonationPartageRequest,
  ): Observable<DonationPartageResponse> {
    return this.http.post<DonationPartageResponse>(
      `/api/v1/case-files/${caseFileId}/donation-partage`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<DonationPartageResponse> {
    return this.http.get<DonationPartageResponse>(
      `/api/v1/case-files/${caseFileId}/donation-partage`,
    );
  }
}
