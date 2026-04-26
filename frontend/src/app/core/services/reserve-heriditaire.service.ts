import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ReserveHereditaireRequest,
  ReserveHereditaireResponse,
} from '../models/reserve-heriditaire.model';

/**
 * SF-FA-24-08 : wrapper HttpClient pour l'outil décisionnel "Réserve
 * héréditaire et action en réduction" (F-FA-24). FR uniquement —
 * art. 913 + 914-1 + 920-928 Cciv.
 *
 * Consomme l'API figée dans SF-FA-24-07 (backend, mergé PR #672).
 */
@Injectable({ providedIn: 'root' })
export class ReserveHereditaireService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: ReserveHereditaireRequest): Observable<ReserveHereditaireResponse> {
    return this.http.post<ReserveHereditaireResponse>(
      `/api/v1/case-files/${caseFileId}/reserve-heriditaire-analysis`, request);
  }

  get(caseFileId: string): Observable<ReserveHereditaireResponse> {
    return this.http.get<ReserveHereditaireResponse>(
      `/api/v1/case-files/${caseFileId}/reserve-heriditaire-analysis`);
  }
}
