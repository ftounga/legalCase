import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RapportSuccessionRequest,
  RapportSuccessionResponse,
} from '../models/rapport-succession.model';

/**
 * SF-FA-24-14 : wrapper HttpClient pour l'outil décisionnel "Rapport à
 * succession" (F-FA-24). FR uniquement — art. 843-863 + 919 Cciv.
 *
 * Consomme l'API figée dans SF-FA-24-13 (backend, mergé PR #679).
 */
@Injectable({ providedIn: 'root' })
export class RapportSuccessionService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RapportSuccessionRequest): Observable<RapportSuccessionResponse> {
    return this.http.post<RapportSuccessionResponse>(
      `/api/v1/case-files/${caseFileId}/rapport-succession-analysis`, request);
  }

  get(caseFileId: string): Observable<RapportSuccessionResponse> {
    return this.http.get<RapportSuccessionResponse>(
      `/api/v1/case-files/${caseFileId}/rapport-succession-analysis`);
  }
}
