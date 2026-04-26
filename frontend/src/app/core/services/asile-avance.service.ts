import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AsileAvanceRequest, AsileAvanceResponse } from '../models/asile-avance.model';

/**
 * SF-IM-12-02 : wrapper HttpClient pour l'outil décisionnel "Asile avancé"
 * (F-IM-12). FR uniquement (CESEDA Livre V).
 * Consomme l'API figée dans SF-IM-12-01 (backend, mergé PR #644).
 */
@Injectable({ providedIn: 'root' })
export class AsileAvanceService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AsileAvanceRequest): Observable<AsileAvanceResponse> {
    return this.http.post<AsileAvanceResponse>(
      `/api/v1/case-files/${caseFileId}/asile-avance-analysis`, request);
  }

  get(caseFileId: string): Observable<AsileAvanceResponse> {
    return this.http.get<AsileAvanceResponse>(
      `/api/v1/case-files/${caseFileId}/asile-avance-analysis`);
  }
}
