import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DonationRequest,
  DonationResponse,
} from '../models/donation.model';

/**
 * SF-FA-24-06 : wrapper HttpClient pour l'outil décisionnel "Validité d'une
 * donation entre vifs" (F-FA-24). FR uniquement — art. 893-958, 902-906, 920+,
 * 931, 953-958 Cciv. Consomme l'API figée dans SF-FA-24-05 (PR #671).
 */
@Injectable({ providedIn: 'root' })
export class DonationService {

  constructor(private http: HttpClient) {}

  calculate(
    caseFileId: string,
    request: DonationRequest,
  ): Observable<DonationResponse> {
    return this.http.post<DonationResponse>(
      `/api/v1/case-files/${caseFileId}/donation-analysis`, request);
  }

  get(caseFileId: string): Observable<DonationResponse> {
    return this.http.get<DonationResponse>(
      `/api/v1/case-files/${caseFileId}/donation-analysis`);
  }
}
