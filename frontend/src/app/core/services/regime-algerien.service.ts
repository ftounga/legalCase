import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RegimeAlgerienRequest,
  RegimeAlgerienResponse,
} from '../models/regime-algerien.model';

/**
 * SF-IM-17-02 : wrapper HttpClient pour l'outil décisionnel "Régime
 * algérien" (F-IM-17). FR uniquement (Accord franco-algérien
 * du 27/12/1968 modifié).
 * Consomme l'API figée dans SF-IM-17-01 (backend, mergé PR #653).
 */
@Injectable({ providedIn: 'root' })
export class RegimeAlgerienService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RegimeAlgerienRequest): Observable<RegimeAlgerienResponse> {
    return this.http.post<RegimeAlgerienResponse>(
      `/api/v1/case-files/${caseFileId}/regime-algerien-analysis`, request);
  }

  get(caseFileId: string): Observable<RegimeAlgerienResponse> {
    return this.http.get<RegimeAlgerienResponse>(
      `/api/v1/case-files/${caseFileId}/regime-algerien-analysis`);
  }
}
